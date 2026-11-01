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

    /** LiquidBounce Teams PREFIX predicate, rejecting empty prefixes from leading spaces. */
    public static boolean samePrefix(String selfName, String candidateName) {
        if (selfName == null || candidateName == null) return false;
        String[] self = selfName.replaceAll("(?i)§[0-9a-fk-orx]", "").split(" ", 2);
        String[] other = candidateName.replaceAll("(?i)§[0-9a-fk-orx]", "").split(" ", 2);
        return self.length > 1 && other.length > 1 && !self[0].isBlank() && self[0].equals(other[0]);
    }

}
