package dev.jilaton.client.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffhandRefillControllerTest {
    private static final long FIRST_SWAP_TIME = 20_000_000_000L;

    @Test
    void waitsForMatchingServerConfirmationAndDoesNotDuplicatePredictedSwap() {
        FakeDriver driver = new FakeDriver();
        OffhandRefillController refill = new OffhandRefillController(driver, new Random(1));
        beginRefill(refill);

        assertEquals(1, driver.swaps);
        assertTrue(refill.busy());
        refill.observe(2, 12, OffhandRefillController.Offhand.TOTEM, FIRST_SWAP_TIME + 1L);
        refill.step(FIRST_SWAP_TIME + 10_000_000L, 1);
        assertEquals(1, driver.swaps);

        refill.observe(0, 11, OffhandRefillController.Offhand.TOTEM, FIRST_SWAP_TIME + 20_000_000L);
        assertTrue(refill.busy(), "A stale container update must not confirm the transaction");
        refill.observe(0, 13, OffhandRefillController.Offhand.TOTEM, FIRST_SWAP_TIME + 30_000_000L);
        assertFalse(refill.busy());
    }

    @Test
    void retriesOnlyAfterServerReportsEmptyAndLatencyPauseExpires() {
        FakeDriver driver = new FakeDriver();
        OffhandRefillController refill = new OffhandRefillController(driver, new Random(2));
        beginRefill(refill);
        refill.observe(0, 13, OffhandRefillController.Offhand.EMPTY, FIRST_SWAP_TIME + 1L);

        refill.step(FIRST_SWAP_TIME + 99_000_000L, 1);
        assertEquals(1, driver.swaps);
        refill.step(FIRST_SWAP_TIME + 101_000_000L, 1);
        assertEquals(2, driver.swaps);
        assertTrue(refill.busy());
    }

    @Test
    void timesOutUnconfirmedSwapThenWaitsBeforeRetrying() {
        FakeDriver driver = new FakeDriver();
        OffhandRefillController refill = new OffhandRefillController(driver, new Random(4));
        beginRefill(refill);

        refill.step(FIRST_SWAP_TIME + 750_000_001L, 1);
        assertEquals(1, driver.swaps);
        refill.step(FIRST_SWAP_TIME + 850_000_002L, 1);
        assertEquals(2, driver.swaps);
    }

    @Test
    void stopsAfterThreeRejectedInventoryTransactions() {
        FakeDriver driver = new FakeDriver();
        OffhandRefillController refill = new OffhandRefillController(driver, new Random(3));
        beginRefill(refill);
        long now = FIRST_SWAP_TIME;
        for (int attempt = 1; attempt <= 3; attempt++) {
            refill.observe(0, 13 + attempt, OffhandRefillController.Offhand.EMPTY, now + 1L);
            if (attempt < 3) {
                now += 101_000_000L;
                refill.step(now, 1);
                assertEquals(attempt + 1, driver.swaps);
            }
        }
        refill.step(now + 1_000_000_000L, 1);
        assertEquals(3, driver.swaps);
    }

    private static void beginRefill(OffhandRefillController refill) {
        refill.step(0L, 3);
        refill.step(10_000_000_000L, 3);
        refill.step(FIRST_SWAP_TIME, 3);
    }

    private static final class FakeDriver implements OffhandRefillController.Driver {
        private int swaps;

        @Override
        public boolean needsTotem() {
            return true;
        }

        @Override
        public boolean canInteract() {
            return true;
        }

        @Override
        public int findSource() {
            return 10;
        }

        @Override
        public boolean validSource(int slot) {
            return slot == 10;
        }

        @Override
        public void swap(int slot) {
            swaps++;
        }

        @Override
        public int containerId() {
            return 0;
        }

        @Override
        public int stateId() {
            return 12;
        }

        @Override
        public int latencyMillis() {
            return 100;
        }
    }
}
