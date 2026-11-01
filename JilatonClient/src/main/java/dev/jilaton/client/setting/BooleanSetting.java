package dev.jilaton.client.setting;

public final class BooleanSetting implements Setting {
    private final String id;
    private final String label;
    private boolean value;
    private final boolean initial;

    public BooleanSetting(String id, String label, boolean defaultValue) {
        this.id = id;
        this.label = label;
        this.value = defaultValue;
        this.initial = defaultValue;
    }

    @Override
    public void reset() {
        value = initial;
    }

    @Override
    public boolean isDefault() {
        return value == initial;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean value() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public void toggle() {
        value = !value;
    }
}
