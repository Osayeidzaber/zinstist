package dev.jilaton.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Jilaton chrome on vanilla menu screens: the mark plus a numbered section label in the corner, and a
 * fade in from black whenever a menu screen opens, so navigation between screens feels continuous.
 */
public final class MenuChrome {
    private static final double FADE_SECONDS = 0.32;
    private static final Map<Screen, Long> OPENED = new WeakHashMap<>();
    private static final Map<Screen, Integer> GENERATION = new WeakHashMap<>();

    private MenuChrome() { }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (client.level != null || screen instanceof JilatonMainMenuScreen || screen instanceof ClickGuiScreen || screen instanceof JilatonClickGui
                    || screen instanceof HudEditorScreen || screen instanceof TextSettingScreen
                    || screen instanceof JilatonCardScreen) return;
            OPENED.putIfAbsent(screen, System.nanoTime());
            // Re-initialisation (resizing) may or may not reset screen events; a generation guard keeps one painter.
            int generation = GENERATION.merge(screen, 1, Integer::sum);
            ScreenEvents.afterRender(screen).register((current, graphics, mouseX, mouseY, delta) -> {
                if (GENERATION.getOrDefault(current, -1) == generation) render(client, current, graphics);
            });
        });
    }

    private static void render(Minecraft client, Screen screen, GuiGraphics graphics) {
        if (client.level != null) return;
        String section = section(screen);
        int x = 10, y = 8;
        JilatonLogo.draw(graphics, x, y, 9, JilatonUi.TEXT_2);
        graphics.drawString(client.font, "JILATON", x + 14, y + 1, JilatonUi.TEXT_2, false);
        int labelX = x + 18 + client.font.width("JILATON");
        graphics.drawString(client.font, "— " + section, labelX, y + 1, JilatonUi.TEXT_4, false);

        boolean reduced = JilatonWidgets.reducedMotion();
        long opened = OPENED.getOrDefault(screen, 0L);
        double p = reduced ? 1 : Motion.SETTLE.apply((System.nanoTime() - opened) / 1_000_000_000.0 / FADE_SECONDS);
        if (p < 1) graphics.fill(0, 0, screen.width, screen.height, JilatonUi.alpha(0xFF000000, 1 - p));
    }

    static String section(Screen screen) {
        if (screen instanceof SelectWorldScreen) return "01  WORLDS";
        if (screen instanceof CreateWorldScreen) return "01  NEW WORLD";
        if (screen instanceof JoinMultiplayerScreen) return "02  SERVERS";
        if (screen instanceof OptionsScreen || screen instanceof OptionsSubScreen) return "04  OPTIONS";
        String title = screen.getTitle().getString().toUpperCase(Locale.ROOT);
        return title.isBlank() ? "MENU" : title.length() > 28 ? title.substring(0, 27) + "…" : title;
    }
}
