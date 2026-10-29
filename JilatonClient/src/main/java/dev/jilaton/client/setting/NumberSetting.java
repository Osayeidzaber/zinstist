package dev.jilaton.client.setting;

public final class NumberSetting implements Setting {
    private final String id;
    private final String label;
    private final double minimum;
    private final double maximum;
    private final double step;
    private double value;

    public NumberSetting(String id, String label, double minimum, double maximum, double step, double defaultValue) {
        if (minimum > maximum || step <= 0.0) {
            throw new IllegalArgumentException("Invalid numeric setting range");
        }
        this.id = id;
        this.label = label;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
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

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    public double value() {
        return value;
    }

    public void setValue(double value) {
        double bounded = Math.max(minimum, Math.min(maximum, value));
        this.value = minimum + Math.round((bounded - minimum) / step) * step;
        this.value = Math.max(minimum, Math.min(maximum, this.value));
    }
}
