package dev.jilaton.client.gui;

import java.util.Arrays;

public enum ClientTheme {
    OBSIDIAN("obsidian", "Obsidian", 0xFFF2F2F2),
    GRAPHITE("graphite", "Graphite", 0xFF9C9C9C),
    VIOLET("violet", "Violet", 0xFFA855F7),
    BLUE("blue", "Glacier", 0xFF69AFFF),
    CYAN("cyan", "Tide", 0xFF50D5C8),
    ROSE("rose", "Rose", 0xFFFF78AD),
    AMBER("amber", "Ember", 0xFFFFB957);

    private final String id;
    private final String label;
    private final int accent;

    ClientTheme(String id, String label, int accent) {
        this.id = id;
        this.label = label;
        this.accent = accent;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public int accent() {
        return accent;
    }

    public static ClientTheme fromId(String id) {
        return Arrays.stream(values())
                .filter(theme -> theme.id.equals(id))
                .findFirst()
                .orElse(OBSIDIAN);
    }

    public static int resolveAccent(String themeId, String customAccent) {
        if (!"custom".equals(themeId)) {
            return fromId(themeId).accent();
        }
        if (customAccent == null || !customAccent.matches("[0-9A-Fa-f]{6}")) {
            return OBSIDIAN.accent();
        }
        try {
            return 0xFF000000 | Integer.parseInt(customAccent, 16);
        } catch (IllegalArgumentException exception) {
            return OBSIDIAN.accent();
        }
    }
}
