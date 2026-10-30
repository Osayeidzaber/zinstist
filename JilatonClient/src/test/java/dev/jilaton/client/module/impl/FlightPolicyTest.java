package dev.jilaton.client.module.impl;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightPolicyTest {
    @Test
    void preservesVelocityBelowTheConfiguredCap() {
        Vec3 velocity = new Vec3(0.3, 0.4, 0.0);

        assertEquals(velocity, FlightPolicy.capVelocity(velocity, 1.0));
    }

    @Test
    void normalizesVelocityToTheMaximumMagnitude() {
        Vec3 capped = FlightPolicy.capVelocity(new Vec3(3.0, 4.0, 0.0), 2.0);

        assertEquals(1.2, capped.x, 1.0E-9);
        assertEquals(1.6, capped.y, 1.0E-9);
        assertEquals(2.0, capped.length(), 1.0E-9);
    }

    @Test
    void invalidMaximumProducesZeroVelocity() {
        assertEquals(Vec3.ZERO, FlightPolicy.capVelocity(new Vec3(1.0, 2.0, 3.0), 0.0));
    }

    @Test
    void openAirHeartbeatRunsOnlyEveryFortiethTick() {
        assertEquals(true, FlightPolicy.shouldSendOpenAirHeartbeat(80, false));
        assertEquals(false, FlightPolicy.shouldSendOpenAirHeartbeat(81, false));
    }

    @Test
    void openAirHeartbeatIsSuppressedWhenABlockIsBelow() {
        assertEquals(false, FlightPolicy.shouldSendOpenAirHeartbeat(80, true));
    }

    @Test
    void restoreReturnsTheReplacedFlyingSpeedOnlyWhileTheAppliedValueIsStillThere() {
        assertEquals(0.05F, FlightPolicy.restoredFlyingSpeed(1.0F, 1.0F, 0.05F));
        assertEquals(0.2F, FlightPolicy.restoredFlyingSpeed(0.2F, 1.0F, 0.05F));
    }

    @Test
    void restoreNeverResurrectsAbilityFlagsTheGameAlreadyCleared() {
        assertEquals(true, FlightPolicy.restoredFlag(true, true));
        assertEquals(false, FlightPolicy.restoredFlag(true, false));
        assertEquals(false, FlightPolicy.restoredFlag(false, true));
    }
}
