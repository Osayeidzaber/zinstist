package dev.jilaton.client.setting;

public sealed interface Setting permits BooleanSetting, NumberSetting {
    String id();

    String label();
}
