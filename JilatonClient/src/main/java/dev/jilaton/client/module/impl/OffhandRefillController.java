package dev.jilaton.client.module.impl;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Server-confirmed offhand refill lifecycle adapted from Anubis's OffhandRefill. */
final class OffhandRefillController {
    private static final int MAX_ATTEMPTS = 3;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final Driver driver;
    private final RandomGenerator random;
    private Phase phase = Phase.IDLE;
    private Status status = Status.IDLE;
    private long due;
    private int source = -1;
    private double tempo = 1.0;
    private boolean occupiedSinceSwap;
    private int attempts;
    private int sentContainer = -1;
    private int seenState = -1;
    private long startedAt;
    private long deadline;
    private long retryAt;
    private long retryPause;

    OffhandRefillController(Driver driver, RandomGenerator random) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.random = Objects.requireNonNull(random, "random");
    }

    boolean busy() {
        return phase != Phase.IDLE || status == Status.WAITING;
    }

    void reset() {
        cancelSchedule();
        resetConfirmation();
        occupiedSinceSwap = false;
    }

    void step(long nowNanos, int pace) {
        if (!driver.canInteract()) {
            reset();
            return;
        }
        expire(nowNanos);
        if (!driver.needsTotem()) {
            occupiedSinceSwap = true;
            cancelSchedule();
            return;
        }
        if (occupiedSinceSwap) {
            if (status != Status.EMPTY && status != Status.GAVE_UP) {
                resetConfirmation();
            }
            occupiedSinceSwap = false;
        }
        if (!canAttempt(nowNanos)) {
            cancelSchedule();
            return;
        }

        int boundedPace = Math.max(1, Math.min(10, pace));
        if (boundedPace == 1) {
            int instant = driver.findSource();
            if (instant >= 0 && driver.validSource(instant)) {
                equip(nowNanos, instant);
            }
            return;
        }
        if (nowNanos < due) {
            return;
        }
        switch (phase) {
            case IDLE -> {
                if (driver.findSource() < 0) {
                    return;
                }
                tempo = 0.94 + random.nextDouble() * 0.12;
                phase = Phase.NOTICE;
                due = nowNanos + sample(10.0 + (boundedPace - 1) * 8.0,
                        20.0 + (boundedPace - 1) * 13.0);
            }
            case NOTICE -> {
                source = driver.findSource();
                if (source < 0) {
                    cancelSchedule();
                    return;
                }
                phase = Phase.TARGET;
                due = nowNanos + sample(6.0 + (boundedPace - 1) * 2.5,
                        12.0 + (boundedPace - 1) * 4.5);
            }
            case TARGET -> {
                if (!driver.validSource(source)) {
                    phase = Phase.NOTICE;
                    due = nowNanos + sample(6.0, 14.0);
                    return;
                }
                equip(nowNanos, source);
            }
        }
    }

    void observe(int containerId, int stateId, Offhand offhand, long receivedAtNanos) {
        if (attempts == 0 || receivedAtNanos < startedAt
                || containerId != -1 && containerId != sentContainer
                || stateId != -1 && stateId < seenState
                || status != Status.WAITING && status != Status.UNCONFIRMED
                && status != Status.EMPTY && status != Status.GAVE_UP) {
            return;
        }
        if (stateId != -1) {
            seenState = stateId;
        }
        status = switch (offhand) {
            case EMPTY -> attempts >= MAX_ATTEMPTS ? Status.GAVE_UP : Status.EMPTY;
            case TOTEM -> Status.CONFIRMED;
            case OTHER -> Status.OCCUPIED;
        };
        retryAt = receivedAtNanos + retryPause;
    }

    private void equip(long nowNanos, int selectedSource) {
        if (!(driver.needsTotem() && driver.canInteract() && driver.validSource(selectedSource))) {
            cancelSchedule();
            return;
        }
        begin(nowNanos, driver.containerId(), driver.stateId(), driver.latencyMillis());
        cancelSchedule();
        driver.swap(selectedSource);
        occupiedSinceSwap = !driver.needsTotem();
    }

    private long sample(double lowMillis, double highMillis) {
        return Math.round((lowMillis + (highMillis - lowMillis)
                * (random.nextDouble() + random.nextDouble()) / 2.0) * tempo * NANOS_PER_MILLI);
    }

    private void cancelSchedule() {
        phase = Phase.IDLE;
        source = -1;
        due = 0L;
    }

    private void resetConfirmation() {
        status = Status.IDLE;
        attempts = 0;
        sentContainer = -1;
        seenState = -1;
        retryPause = 0L;
        retryAt = 0L;
        deadline = 0L;
        startedAt = 0L;
    }

    private void begin(long nowNanos, int containerId, int stateId, int latencyMillis) {
        attempts++;
        sentContainer = containerId;
        seenState = stateId;
        startedAt = nowNanos;
        long latency = Math.max(0, latencyMillis);
        deadline = nowNanos + Math.min(5_000L, Math.max(750L, latency * 3L + 250L)) * NANOS_PER_MILLI;
        retryPause = Math.min(500L, Math.max(100L, latency)) * NANOS_PER_MILLI;
        status = Status.WAITING;
    }

    private void expire(long nowNanos) {
        if (status == Status.WAITING && nowNanos >= deadline) {
            status = Status.UNCONFIRMED;
            retryAt = nowNanos + retryPause;
        }
    }

    private boolean canAttempt(long nowNanos) {
        expire(nowNanos);
        if (status == Status.IDLE) {
            return true;
        }
        if (status != Status.EMPTY && status != Status.UNCONFIRMED) {
            return false;
        }
        if (attempts >= MAX_ATTEMPTS) {
            status = Status.GAVE_UP;
            return false;
        }
        return nowNanos >= retryAt;
    }

    enum Offhand {
        EMPTY,
        TOTEM,
        OTHER
    }

    interface Driver {
        boolean needsTotem();

        boolean canInteract();

        int findSource();

        boolean validSource(int slot);

        void swap(int slot);

        int containerId();

        int stateId();

        int latencyMillis();
    }

    private enum Phase {
        IDLE,
        NOTICE,
        TARGET
    }

    private enum Status {
        IDLE,
        WAITING,
        CONFIRMED,
        EMPTY,
        OCCUPIED,
        UNCONFIRMED,
        GAVE_UP
    }
}
