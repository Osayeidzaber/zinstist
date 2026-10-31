package dev.jilaton.client.render.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A named ViewModel transform: both hands' offsets, rotations, scale and enable flag. Presets are persisted as text
 * inside a module setting, so they travel through {@code ClientConfig} with no schema change.
 *
 * <p>The wire form is {@code name:hand|hand;name:hand|hand}, one hand being
 * {@code enabled,x,y,z,rx,ry,rz,scale}. Anything malformed is dropped rather than thrown: a corrupt line must not
 * stop the client from loading its other presets.
 */
public record ViewModelPreset(String name, Hand main, Hand off) {
    public static final int MAX_NAME = 24;

    public record Hand(boolean enabled, double x, double y, double z, double rx, double ry, double rz, double scale) {
        public static final Hand NONE = new Hand(true, 0, 0, 0, 0, 0, 0, 1);
    }

    public ViewModelPreset {
        name = cleanName(name);
        if (main == null || off == null) throw new IllegalArgumentException("A preset needs both hands");
    }

    /** Trims and bounds a user-supplied name so it can round-trip through the wire form. */
    public static String cleanName(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (cleaned.length() > MAX_NAME) cleaned = cleaned.substring(0, MAX_NAME).trim();
        return cleaned.isEmpty() ? "Preset" : cleaned;
    }

    public String encode() {
        return name + ":" + encode(main) + "|" + encode(off);
    }

    private static String encode(Hand hand) {
        return (hand.enabled() ? "1" : "0") + "," + hand.x() + "," + hand.y() + "," + hand.z() + ","
                + hand.rx() + "," + hand.ry() + "," + hand.rz() + "," + hand.scale();
    }

    public static String encodeAll(List<ViewModelPreset> presets) {
        StringBuilder text = new StringBuilder();
        for (ViewModelPreset preset : presets) {
            if (text.length() > 0) text.append(';');
            text.append(preset.encode());
        }
        return text.toString();
    }

    public static List<ViewModelPreset> decodeAll(String text) {
        List<ViewModelPreset> presets = new ArrayList<>();
        if (text == null || text.isBlank()) return presets;
        for (String entry : text.split(";")) {
            ViewModelPreset preset = decode(entry);
            if (preset != null) presets.add(preset);
        }
        return presets;
    }

    public static ViewModelPreset decode(String entry) {
        if (entry == null) return null;
        int split = entry.indexOf(':');
        if (split <= 0 || split == entry.length() - 1) return null;
        String[] hands = entry.substring(split + 1).split("\\|", -1);
        Hand main = decodeHand(hands, 0);
        Hand off = decodeHand(hands, 1);
        if (main == null || off == null) return null;
        return new ViewModelPreset(entry.substring(0, split), main, off);
    }

    private static Hand decodeHand(String[] hands, int index) {
        if (index >= hands.length) return null;
        String[] fields = hands[index].split(",", -1);
        if (fields.length != 8) return null;
        try {
            Hand hand = new Hand(
                    "1".equals(fields[0]),
                    Double.parseDouble(fields[1]), Double.parseDouble(fields[2]), Double.parseDouble(fields[3]),
                    Double.parseDouble(fields[4]), Double.parseDouble(fields[5]), Double.parseDouble(fields[6]),
                    Double.parseDouble(fields[7]));
            return finite(hand) ? hand : null;
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    private static boolean finite(Hand hand) {
        return Double.isFinite(hand.x()) && Double.isFinite(hand.y()) && Double.isFinite(hand.z())
                && Double.isFinite(hand.rx()) && Double.isFinite(hand.ry()) && Double.isFinite(hand.rz())
                && Double.isFinite(hand.scale()) && hand.scale() > 0;
    }

    /** Case-insensitive lookup, because the setting stores what the choice list shows. */
    public static ViewModelPreset find(List<ViewModelPreset> presets, String name) {
        for (ViewModelPreset preset : presets) {
            if (preset.name().equalsIgnoreCase(name)) return preset;
        }
        return null;
    }

    public String key() {
        return name.toLowerCase(Locale.ROOT);
    }
}
