package dev.jilaton.client.account;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Offline accounts: names only, stored one per line in {@code config/jilaton/accounts.txt}. Offline names carry no
 * secret, so nothing here is sensitive; Microsoft accounts are not stored by this class.
 */
public final class OfflineAccounts {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private final Path file;
    private final List<String> names = new ArrayList<>();

    public OfflineAccounts(Path file) {
        this.file = file;
        load();
    }

    /** A name Minecraft accepts: 3 to 16 letters, digits or underscores. */
    public static boolean valid(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** Why {@code name} cannot be added, or empty when it can. */
    public String problem(String name) {
        if (name == null || name.isEmpty()) return "Type a name";
        if (name.length() < 3) return "At least 3 characters";
        if (name.length() > 16) return "At most 16 characters";
        if (!valid(name)) return "Letters, digits and _ only";
        if (contains(name)) return "Already added";
        return "";
    }

    public List<String> names() {
        return List.copyOf(names);
    }

    public boolean contains(String name) {
        for (String existing : names) if (existing.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    public boolean add(String name) {
        if (!problem(name).isEmpty()) return false;
        names.add(name);
        save();
        return true;
    }

    public boolean rename(String from, String to) {
        int index = names.indexOf(from);
        if (index < 0 || !valid(to) || (!from.equalsIgnoreCase(to) && contains(to))) return false;
        names.set(index, to);
        save();
        return true;
    }

    public void remove(String name) {
        if (names.remove(name)) save();
    }

    private void load() {
        names.clear();
        try {
            if (!Files.isRegularFile(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String name = line.strip();
                if (valid(name) && !contains(name)) names.add(name);
            }
        } catch (IOException ignored) {
            // An unreadable list starts empty; it is rewritten on the next change.
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, names, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Kept in memory for this session.
        }
    }
}
