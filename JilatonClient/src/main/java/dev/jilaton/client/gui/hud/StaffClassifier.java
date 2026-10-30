package dev.jilaton.client.gui.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Heuristic staff matching over a player's tab-list name, team tag and team colour.
 * Server names and teams differ, so a match is a hint, never a claim about a player's rank.
 */
public final class StaffClassifier {
    public enum Kind { NAME_PREFIX, NAME_CONTAINS, NAME_GLOB, TEAM_TAG, TEAM_COLOR }

    public record Rule(Kind kind, String value) {
        public String label() {
            return value.toUpperCase(Locale.ROOT);
        }
    }

    public record Candidate(String name, String teamPrefix, String teamSuffix,
                            String teamColorName, Integer teamColorRgb) {
    }

    public record Match(Rule rule) {
        public String label() {
            return rule.label();
        }
    }

    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("black", 0x000000), Map.entry("dark_blue", 0x0000AA),
            Map.entry("dark_green", 0x00AA00), Map.entry("dark_aqua", 0x00AAAA),
            Map.entry("dark_red", 0xAA0000), Map.entry("dark_purple", 0xAA00AA),
            Map.entry("gold", 0xFFAA00), Map.entry("gray", 0xAAAAAA),
            Map.entry("dark_gray", 0x555555), Map.entry("blue", 0x5555FF),
            Map.entry("green", 0x55FF55), Map.entry("aqua", 0x55FFFF),
            Map.entry("red", 0xFF5555), Map.entry("light_purple", 0xFF55FF),
            Map.entry("yellow", 0xFFFF55), Map.entry("white", 0xFFFFFF));

    private StaffClassifier() {
    }

    /** Comma-separated: plain = name prefix, contains:, glob:, tag: = team prefix/suffix, color: = team colour. */
    public static List<Rule> parseRules(String setting) {
        List<Rule> rules = new ArrayList<>();
        if (setting == null) {
            return rules;
        }
        for (String token : setting.split(",")) {
            String trimmed = token.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            Kind kind = Kind.NAME_PREFIX;
            String value = trimmed;
            int colon = trimmed.indexOf(':');
            if (colon > 0) {
                String head = trimmed.substring(0, colon).strip().toLowerCase(Locale.ROOT);
                String tail = trimmed.substring(colon + 1).strip();
                if (tail.isEmpty()) {
                    continue;
                }
                switch (head) {
                    case "contains" -> {
                        kind = Kind.NAME_CONTAINS;
                        value = tail;
                    }
                    case "glob" -> {
                        kind = Kind.NAME_GLOB;
                        value = tail;
                    }
                    case "tag" -> {
                        kind = Kind.TEAM_TAG;
                        value = tail;
                    }
                    case "color", "colour" -> {
                        kind = Kind.TEAM_COLOR;
                        value = tail;
                    }
                    default -> {
                    }
                }
            }
            rules.add(new Rule(kind, value));
        }
        return List.copyOf(rules);
    }

    public static Optional<Match> classify(Candidate candidate, List<Rule> rules) {
        if (candidate == null || candidate.name() == null || candidate.name().isBlank() || rules == null) {
            return Optional.empty();
        }
        String name = candidate.name().toLowerCase(Locale.ROOT);
        String prefix = candidate.teamPrefix() == null ? "" : candidate.teamPrefix().toLowerCase(Locale.ROOT);
        String suffix = candidate.teamSuffix() == null ? "" : candidate.teamSuffix().toLowerCase(Locale.ROOT);
        for (Rule rule : rules) {
            String value = rule.value();
            String lower = value.toLowerCase(Locale.ROOT);
            boolean matches = switch (rule.kind()) {
                case NAME_PREFIX -> name.startsWith(lower);
                case NAME_CONTAINS -> name.contains(lower);
                case NAME_GLOB -> globMatches(value, candidate.name());
                case TEAM_TAG -> prefix.contains(lower) || suffix.contains(lower);
                case TEAM_COLOR -> colorMatches(value, candidate.teamColorName(), candidate.teamColorRgb());
            };
            if (matches) {
                return Optional.of(new Match(rule));
            }
        }
        return Optional.empty();
    }

    private static boolean globMatches(String pattern, String value) {
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < pattern.length(); index++) {
            char character = pattern.charAt(index);
            if (character == '*') {
                regex.append(".*");
            } else if (character == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(character)));
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE).matcher(value).matches();
    }

    private static boolean colorMatches(String rule, String colorName, Integer colorRgb) {
        String normalized = rule.strip();
        Integer wanted = null;
        if (normalized.startsWith("#") && normalized.length() == 7) {
            try {
                wanted = Integer.parseInt(normalized.substring(1), 16);
            } catch (NumberFormatException ignored) {
                return false;
            }
        } else {
            wanted = COLORS.get(normalized.toLowerCase(Locale.ROOT));
        }
        if (wanted != null && colorRgb != null) {
            return (wanted & 0xFFFFFF) == (colorRgb & 0xFFFFFF);
        }
        return colorName != null && colorName.equalsIgnoreCase(normalized);
    }
}
