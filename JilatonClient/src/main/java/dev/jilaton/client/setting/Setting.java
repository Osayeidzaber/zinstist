package dev.jilaton.client.setting;

public sealed interface Setting permits BooleanSetting, NumberSetting, ChoiceSetting, TextSetting {
    String id();

    String label();

    /** Puts the value back to the one the setting was created with. */
    void reset();

    /** True while the value equals the one the setting was created with. */
    boolean isDefault();
}
