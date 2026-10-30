package dev.jilaton.client.setting;

import java.util.List;
import java.util.Objects;

public final class ChoiceSetting implements Setting {
    private final String id;
    private final String label;
    private volatile List<String> choices;
    private String value;
    private boolean deferUnknown;
    /** A saved value that is not offered yet (e.g. a custom model not scanned at load time). */
    private String pending;

    public ChoiceSetting(String id, String label, List<String> choices, String defaultValue) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.choices = validated(choices);
        setValue(defaultValue);
    }

    /**
     * Lets {@link #setValue} accept a value the current choices do not contain yet: it is remembered, kept for
     * saving, and selected as soon as {@link #setChoices} offers it. For settings whose options are discovered
     * at runtime.
     */
    public ChoiceSetting deferUnknownValues() {
        deferUnknown = true;
        return this;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String label() {
        return label;
    }

    public List<String> choices() {
        return choices;
    }

    public String value() {
        return value;
    }

    /** The value to persist: a deferred value outlives a session in which it was never offered. */
    public String persistedValue() {
        return pending != null ? pending : value;
    }

    public void setValue(String value) {
        if (!choices.contains(value)) {
            if (deferUnknown && value != null && !value.isBlank()) {
                pending = value;
                return;
            }
            throw new IllegalArgumentException("Unsupported choice for " + id + ": " + value);
        }
        this.value = value;
        pending = null;
    }

    /** Replaces the offered choices, keeping the current value when it survives and resolving a deferred one. */
    public void setChoices(List<String> next) {
        List<String> updated = validated(next);
        choices = updated;
        if (pending != null && updated.contains(pending)) {
            value = pending;
            pending = null;
        } else if (!updated.contains(value)) {
            value = updated.get(0);
        }
    }

    public void cycle(int direction) {
        List<String> current = choices;
        int index = current.indexOf(value);
        int next = Math.floorMod(index + Integer.signum(direction), current.size());
        value = current.get(next);
        pending = null;
    }

    private static List<String> validated(List<String> choices) {
        List<String> copy = List.copyOf(choices);
        if (copy.isEmpty() || copy.stream().anyMatch(choice -> choice == null || choice.isBlank())) {
            throw new IllegalArgumentException("A choice setting requires non-empty choices");
        }
        return copy;
    }
}
