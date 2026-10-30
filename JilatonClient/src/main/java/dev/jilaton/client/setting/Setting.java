package dev.jilaton.client.setting;

public sealed interface Setting permits BooleanSetting, NumberSetting, ChoiceSetting, TextSetting {
    String id();

    String label();
}
