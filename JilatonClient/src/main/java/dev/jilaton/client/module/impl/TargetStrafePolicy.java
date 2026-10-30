package dev.jilaton.client.module.impl;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/** Pure direction, eligibility and range decisions for the TargetStrafe module. */
final class TargetStrafePolicy {
    private static final double MIN_DISTANCE = 1.0E-5;
    private static final float MIN_INPUT_SQUARED = 1.0E-6F;

    private TargetStrafePolicy() {
    }

    /** A jump edge flips the orbit; a held left/right key then pins the side. */
    static int directionAfterInput(int direction, boolean jump, boolean jumpWasDown, boolean left, boolean right) {
        int next = jump && !jumpWasDown ? -direction : direction;
        return left != right ? (left ? -1 : 1) : next;
    }

    /** Maps the client's current screen to {@link #eligible}'s {@code screenOpen}: no screen means normal play. */
    static boolean screenOpen(Object currentScreen) {
        return currentScreen != null;
    }

    static boolean eligible(boolean hasLevel, boolean screenOpen, boolean requireGround, boolean onGround,
                            boolean requireMovement, boolean movementInput, boolean sprintTapActive) {
        return hasLevel && !screenOpen
                && (!requireGround || onGround)
                && (!requireMovement || movementInput)
                && !sprintTapActive;
    }

    /** Orbit input is undefined on top of the target and only meaningful inside the follow range. */
    static boolean canOrbit(double distance, double followRange) {
        return distance >= MIN_DISTANCE && distance <= followRange;
    }

    static boolean inFollowRange(double distance, double followRange) {
        return distance <= followRange;
    }

    /** A zero or near-zero local input must not overwrite the player's movement. */
    static boolean meaningfulInput(Vec2 movement) {
        return movement.x * movement.x + movement.y * movement.y > MIN_INPUT_SQUARED;
    }

    static boolean hasMovementInput(Input input) {
        return input.forward() || input.backward() || input.left() || input.right();
    }
}
