package dev.jilaton.client.setting;

import java.util.List;
import java.util.Objects;

public final class ChoiceSetting implements Setting {
    private final String id;
    private final String label;
    private final List<String> choices;
    private String value;

    public ChoiceSetting(String id, String label, List<String> choices, String defaultValue) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.choices = List.copyOf(choices);
        if (this.choices.isEmpty() || this.choices.stream().anyMatch(choice -> choice == null || choice.isBlank())) {
            throw new IllegalArgumentException("A choice setting requires non-empty choices");
        }
        setValue(defaultValue);
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

    public void setValue(String value) {
        if (!choices.contains(value)) {
            throw new IllegalArgumentException("Unsupported choice for " + id + ": " + value);
        }
        this.value = value;
    }

    public void cycle(int direction) {
        int index = choices.indexOf(value);
        int next = Math.floorMod(index + Integer.signum(direction), choices.size());
        value = choices.get(next);
    }
}
