package dev.jilaton.client.setting;

/** One validated conversion path for command editing; settings retain their own bounds/validation. */
public final class SettingValueEditor {
    private SettingValueEditor() { }

    public static String apply(Setting setting, String value) {
        if (setting instanceof BooleanSetting flag) {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) throw new IllegalArgumentException("Use true or false");
            flag.setValue(Boolean.parseBoolean(value));
            return Boolean.toString(flag.value());
        }
        if (setting instanceof NumberSetting number) {
            double parsed;
            try { parsed = Double.parseDouble(value); } catch (NumberFormatException failure) { throw new IllegalArgumentException("Use a finite number"); }
            if (!Double.isFinite(parsed)) throw new IllegalArgumentException("Use a finite number");
            number.setValue(parsed);
            return Double.toString(number.value());
        }
        if (setting instanceof ChoiceSetting choice) {
            String actual = choice.choices().stream().filter(option -> option.equalsIgnoreCase(value)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Choose: " + String.join(", ", choice.choices())));
            choice.setValue(actual);
            return choice.value();
        }
        if (setting instanceof TextSetting text) { text.setValue(value); return text.value(); }
        throw new IllegalArgumentException("Unsupported setting");
    }
}
