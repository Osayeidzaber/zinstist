package dev.jilaton.client.setting;

import java.util.Objects;
import java.util.function.UnaryOperator;

/** Validated text exposed by the native editor, commands and configuration profiles. */
public final class TextSetting implements Setting {
    private final String id;
    private final String label;
    private final int maximumLength;
    private final UnaryOperator<String> validator;
    private String value;

    public TextSetting(String id, String label, int maximumLength, String defaultValue, UnaryOperator<String> validator) {
        this.id = Objects.requireNonNull(id);
        this.label = Objects.requireNonNull(label);
        if (maximumLength <= 0) throw new IllegalArgumentException("Text length must be positive");
        this.maximumLength = maximumLength;
        this.validator = Objects.requireNonNull(validator);
        setValue(defaultValue);
    }

    @Override public String id() { return id; }
    @Override public String label() { return label; }
    public int maximumLength() { return maximumLength; }
    public String value() { return value; }

    public void setValue(String value) {
        Objects.requireNonNull(value);
        if (value.length() > maximumLength) throw new IllegalArgumentException("Text exceeds " + maximumLength + " characters");
        String validated = Objects.requireNonNull(validator.apply(value));
        if (validated.length() > maximumLength) throw new IllegalArgumentException("Validated text is too long");
        this.value = validated;
    }
}
