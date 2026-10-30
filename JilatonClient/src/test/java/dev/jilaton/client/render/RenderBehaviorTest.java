package dev.jilaton.client.render;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderBehaviorTest {
    @Test void freeLookUsesVanillaMouseUnitsAndClampsPitch() {
        FreeLookRotation rotation = new FreeLookRotation();
        rotation.reset(30, 10);
        rotation.turn(20, -40, 2, true);
        assertEquals(36, rotation.yaw(), 0.001);
        assertEquals(-2, rotation.pitch(), 0.001);
        rotation.turn(0, 10000, 1, true);
        assertEquals(90, rotation.pitch());
        rotation.turn(0, -20000, 1, true);
        assertEquals(-90, rotation.pitch());
    }

    @Test void unlimitedFreeLookWrapsAndRejectsInvalidMouseValues() {
        FreeLookRotation rotation = new FreeLookRotation();
        rotation.reset(179, 0);
        rotation.turn(20, 2400, 1, false);
        assertEquals(-178, rotation.yaw());
        assertEquals(0, rotation.pitch());
        rotation.turn(Double.NaN, 1, 1, false);
        assertEquals(-178, rotation.yaw());
        assertEquals(0, rotation.pitch());
    }

    @Test void healthHistoryDoesNotInventInitialDamageOrReuseDespawnedSamples() {
        HealthChangeTracker tracker = new HealthChangeTracker(2);
        tracker.beginTick();
        assertEquals(0, tracker.observe(7, 20));
        tracker.endTick();
        tracker.beginTick();
        assertEquals(-4, tracker.observe(7, 16));
        assertEquals(2, tracker.observe(7, 18));
        assertEquals(0, tracker.observe(7, Float.NaN));
        tracker.endTick();
        tracker.beginTick();
        tracker.endTick();
        assertEquals(0, tracker.size());
        assertEquals(0, tracker.observe(7, 5));
    }

    @Test void healthTrackingIsBoundedAndClearDropsOldWorldState() {
        HealthChangeTracker tracker = new HealthChangeTracker(2);
        tracker.beginTick();
        tracker.observe(1, 20);
        tracker.observe(2, 20);
        tracker.observe(3, 20);
        assertEquals(2, tracker.size());
        assertEquals(0, tracker.observe(1, 19.99F));
        tracker.clear();
        assertEquals(0, tracker.size());
        assertEquals(0, tracker.observe(1, 10));
    }

    @Test void worldProjectionUsesPerspectiveDivideAndRejectsBehindCamera() {
        WorldProjection projection = new WorldProjection();
        assertFalse(projection.project(0, 0, -3, 800, 600));
        projection.update(new Matrix4f().perspective((float) Math.toRadians(90), 4F / 3, 0.1F, 100), new Matrix4f());
        assertTrue(projection.project(0, 0, -3, 800, 600));
        assertEquals(400, projection.x(800), 0.001);
        assertEquals(300, projection.y(600), 0.001);
        assertTrue(projection.project(1, 1, -3, 800, 600));
        assertTrue(projection.x(800) > 400);
        assertTrue(projection.y(600) < 300);
        assertFalse(projection.project(0, 0, 3, 800, 600));
        assertFalse(projection.project(0, 0, -0.01, 800, 600));
        assertFalse(projection.project(Double.NaN, 0, -3, 800, 600));
        projection.clear();
        assertFalse(projection.project(0, 0, -3, 800, 600));
    }

    @Test void worldProjectionComposesViewBeforeProjection() {
        WorldProjection projection = new WorldProjection();
        projection.update(new Matrix4f().perspective((float) Math.toRadians(90), 1, 0.1F, 100),
                new Matrix4f().rotateY((float) Math.PI));
        assertTrue(projection.project(0, 0, 3, 600, 600));
        assertFalse(projection.project(0, 0, -3, 600, 600));
    }
}
