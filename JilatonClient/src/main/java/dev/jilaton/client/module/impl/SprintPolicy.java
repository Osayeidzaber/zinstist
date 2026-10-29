package dev.jilaton.client.module.impl;

public final class SprintPolicy {
    private SprintPolicy() {
    }

    public static boolean shouldSprint(boolean forwardOnly, boolean forward, boolean backward,
                                       boolean left, boolean right, boolean requireFood,
                                       boolean enoughFood, boolean stopAtCollision, boolean colliding,
                                       boolean stopWhileSneaking, boolean sneaking,
                                       boolean stopWhileUsingItem, boolean usingItem) {
        if (requireFood && !enoughFood || stopAtCollision && colliding
                || stopWhileSneaking && sneaking || stopWhileUsingItem && usingItem) {
            return false;
        }
        boolean moving = forwardOnly ? forward : forward || backward || left || right;
        return moving && (!requireFood || enoughFood);
    }
}
