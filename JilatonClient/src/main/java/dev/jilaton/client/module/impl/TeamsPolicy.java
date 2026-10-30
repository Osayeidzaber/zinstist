package dev.jilaton.client.module.impl;

/** Pure teammate matching rules for the Teams module. */
public final class TeamsPolicy {
    private TeamsPolicy() {
    }

    public static boolean isTeammate(boolean allied, Integer selfNameColor, Integer candidateNameColor,
                                     boolean matchNameColor) {
        if (allied) {
            return true;
        }
        return matchNameColor && sameExplicitColor(selfNameColor, candidateNameColor);
    }

    public static boolean sameExplicitColor(Integer selfColor, Integer candidateColor) {
        return selfColor != null && selfColor.equals(candidateColor);
    }

}
