package dev.jilaton.client.module.impl;

import dev.jilaton.client.gui.NowPlayingWidget;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;

import java.util.List;

/** Draggable Now Playing HUD panel (position, scale and opacity are edited in the HUD editor as "music"). */
public final class NowPlayingModule extends ClientModule {
    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", List.of("Minimal", "Compact", "Expanded"), "Compact"));
    private final BooleanSetting artwork = addSetting(new BooleanSetting("artwork", "Album art", true));
    private final BooleanSetting device = addSetting(new BooleanSetting("device", "Show device", true));
    private final BooleanSetting hideIdle = addSetting(new BooleanSetting("hide_idle", "Hide when nothing plays", true));

    public NowPlayingModule() {
        super("now_playing", "Now Playing", "Spotify-style now playing panel for local music or your desktop Spotify.", ModuleCategory.RENDER);
    }

    public NowPlayingWidget.Style style() {
        return switch (style.value()) { case "Minimal" -> NowPlayingWidget.Style.MINIMAL; case "Expanded" -> NowPlayingWidget.Style.EXPANDED; default -> NowPlayingWidget.Style.COMPACT; };
    }

    public boolean artwork() { return artwork.value(); }
    public boolean device() { return device.value(); }
    public boolean hideIdle() { return hideIdle.value(); }
}
