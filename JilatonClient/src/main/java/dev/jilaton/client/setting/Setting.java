package dev.jilaton.client.setting;

public sealed interface Setting permits BooleanSetting, NumberSetting, ChoiceSetting {
    String id();

    String label();
}
