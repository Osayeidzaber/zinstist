package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.impl.PharaohHeaddressModule.Viewer;
import dev.jilaton.client.setting.BooleanSetting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One row per gate case for the shared visibility rule. Rows that need a live player are given as a
 * {@link Viewer}; the mixin and the emit path both resolve a {@link net.minecraft.world.entity.player.Player}
 * to this record before asking, so these rows are exactly the decision both paths make.
 */
class PharaohHeaddressVisibilityTest {
    private static final double AT_LIMIT = 64 * 64;

    private PharaohHeaddressModule module;

    @BeforeEach
    void enable() {
        module = new PharaohHeaddressModule();
        module.setEnabled(true);
    }

    @AfterEach
    void disable() {
        module.setEnabled(false);
    }

    private static Viewer self(boolean firstPerson) {
        return new Viewer(true, firstPerson, true, false, false, 0);
    }

    private static Viewer other(double distanceSqr, boolean friend) {
        return new Viewer(false, false, true, false, friend, distanceSqr);
    }

    private void setting(String id, boolean value) {
        module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(setting -> (BooleanSetting) setting)
                .findFirst().orElseThrow().setValue(value);
    }

    @Test
    void moduleOffShowsNobody() {
        module.setEnabled(false);
        assertFalse(module.shows(other(0, false)));
        assertFalse(module.shows(self(false)));
    }

    @Test
    void ownPlayerShowsInThirdPersonOnly() {
        assertTrue(module.shows(self(false)));
        assertFalse(module.shows(self(true)));
        setting("own", false);
        assertFalse(module.shows(self(false)));
    }

    @Test
    void otherPlayersShowInsideSixtyFourBlocks() {
        assertTrue(module.shows(other(0, false)));
        assertTrue(module.shows(other(AT_LIMIT, false)));
        assertFalse(module.shows(other(AT_LIMIT + 1, false)));
        setting("others", false);
        assertFalse(module.shows(other(0, false)));
    }

    @Test
    void friendFilterOnlyAdmitsFriends() {
        setting("friends", true);
        assertFalse(module.shows(other(0, false)));
        assertTrue(module.shows(other(0, true)));
    }

    @Test
    void deadInvisibleAndMissingAvatarsAreNeverShown() {
        assertFalse(module.shows(new Viewer(false, false, false, false, false, 0)));
        assertFalse(module.shows(new Viewer(false, false, true, true, false, 0)));
        assertFalse(module.shows((Viewer) null));
    }

    @Test
    void emitAndTheHelmetGateAskTheSamePredicate() throws IOException {
        String moduleSource = source("src/main/java/dev/jilaton/client/module/impl/PharaohHeaddressModule.java");
        String mixinSource = source("src/main/java/dev/jilaton/client/render/character/PharaohHelmetPolicy.java")
                + source("src/main/java/dev/jilaton/client/mixin/PharaohHelmetMixin.java");
        String emit = region(moduleSource, "public void emit(FxFrame f)", "private int[] palette");

        assertTrue(emit.contains("shows(player)"), "emit goes through the shared predicate");
        assertFalse(emit.contains("isAlive"), "emit no longer filters on its own");
        assertFalse(emit.contains("isInvisible"), "emit no longer filters on its own");
        assertFalse(emit.contains("own.value"), "emit no longer filters on its own");
        assertFalse(emit.contains("others.value"), "emit no longer filters on its own");
        assertFalse(emit.contains("friends.value"), "emit no longer filters on its own");
        assertFalse(emit.contains("MAX_DISTANCE_SQR"), "emit no longer filters on its own");
        assertFalse(emit.contains("distanceToSqr"), "emit no longer filters on its own");

        assertTrue(mixinSource.contains("PharaohHeaddressModule.shows(player)"), "gate goes through the shared predicate");
        assertTrue(mixinSource.contains("PharaohHelmetPolicy.hidesHelmet(slot, state, PharaohHelmetPolicy.playerOf(state))"), "single gate call site");
        // Resolution guards; the non-player row cannot be instantiated headless (no Level), so it is source-pinned.
        assertTrue(mixinSource.contains("avatar.id == 0"), "id 0 is unresolvable");
        assertTrue(mixinSource.contains("level == null"), "a missing level is unresolvable");
        assertTrue(mixinSource.contains("getEntity(avatar.id) instanceof Player player"), "only real players resolve");
    }

    @Test
    void theDrawingUsesTheMeasuredConstants() throws IOException {
        String moduleSource = source("src/main/java/dev/jilaton/client/module/impl/PharaohHeaddressModule.java");
        assertTrue(moduleSource.contains("point(0, COLLAR_Y, 0)"));
        assertTrue(moduleSource.contains("BACK_LAPPET_Y0, BACK_LAPPET_Z0"));
    }

    private record Box(double x0, double y0, double z0, double x1, double y1, double z1) {
        boolean intersects(Box other) {
            return x0 <= other.x1 && other.x0 <= x1
                    && y0 <= other.y1 && other.y0 <= y1
                    && z0 <= other.z1 && other.z0 <= z1;
        }
    }

    /*
     * Head-space (1/16 block from the 1.50 eye-height anchor) vanilla boxes, model space drawn at 0.9375:
     * - torso body cube (-4,0,-2)-(4,12,2) on the body part pivot (HumanoidModel.java:122), plus one unit
     *   of outer-armour deformation on every face -> top face at -0.5625;
     * - cape cube (-5,0,-1)-(5,16,1) on a neck pivot offset (0,0,2) at 180 degrees
     *   (PlayerCapeModel.java:32-33), rotated in setupAnim (PlayerCapeModel.java:43-47); the +/-10 degree
     *   roll (AvatarRenderer.java:244-250) lifts a top corner by 5*sin(10) model units, putting it at
     *   -0.685, and the walk-bob flap term (AvatarRenderer.java:254) only sweeps it back, never higher.
     */
    private static final Box TORSO = new Box(-4.6875, -13.7, -2.8125, 4.6875, -0.5625, 2.8125);
    private static final Box CAPE = new Box(-4.69, -17.3, -16.9, 4.69, -0.685, -1.8);

    private static Box collar(double y, double radius) {
        return new Box(-radius, y, -radius, radius, y, radius);
    }

    private static Box backLappet() {
        return new Box(-5.8, PharaohHeaddressModule.BACK_LAPPET_Y0, PharaohHeaddressModule.BACK_LAPPET_Z0,
                5.8, PharaohHeaddressModule.BACK_LAPPET_Y1, PharaohHeaddressModule.BACK_LAPPET_Z1);
    }

    @Test
    void collarClearsTheTorsoAndTheCape() {
        Box collar = collar(PharaohHeaddressModule.COLLAR_Y, PharaohHeaddressModule.COLLAR_RADIUS);
        assertFalse(collar.intersects(TORSO), "collar must sit above the chestplate");
        assertFalse(collar.intersects(CAPE), "collar must sit above the cape's lifted edge");
    }

    @Test
    void backLappetClearsTheCapeThroughItsSway() {
        assertFalse(backLappet().intersects(CAPE), "back lappet must stay above the cape's pivoting edge");
        assertFalse(backLappet().intersects(TORSO), "back lappet must stay above the chestplate");
    }

    @Test
    void theOldCollarWouldHaveClippedTheTorso() {
        assertTrue(collar(-1.2, 5.4).intersects(TORSO), "this test would catch the old geometry");
    }

    @Test
    void collarRingClearsTheHeadCorners() {
        double headHalf = 3.75;
        assertTrue(PharaohHeaddressModule.COLLAR_RADIUS > Math.hypot(headHalf, headHalf));
    }

    private static String source(String relative) throws IOException {
        Path path = Path.of(relative);
        if (!Files.exists(path)) path = Path.of("JilatonClient").resolve(relative);
        return Files.readString(path);
    }

    private static String region(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        if (from < 0 || to < 0) throw new IllegalArgumentException("region markers not found");
        return source.substring(from, to);
    }
}
