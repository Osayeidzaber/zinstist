package dev.jilaton.client.setting;

public final class BooleanSetting implements Setting {
    private final String id;
    private final String label;
    private boolean value;

    public BooleanSetting(String id, String label, boolean defaultValue) {
        this.id = id;
        this.label = label;
        this.value = defaultValue;
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
