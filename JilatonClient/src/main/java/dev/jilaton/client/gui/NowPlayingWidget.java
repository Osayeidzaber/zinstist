package dev.jilaton.client.gui;

import dev.jilaton.client.music.NowPlaying;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Now Playing panel shared by the HUD and the Music Center screen. Three densities, one visual language. */
public final class NowPlayingWidget {
    public enum Style { MINIMAL, COMPACT, EXPANDED }

    private NowPlayingWidget() { }

    public static int width(Style style) {
        return switch (style) { case MINIMAL -> 150; case COMPACT -> 176; case EXPANDED -> 220; };
    }

    public static int height(Style style) {
        return switch (style) { case MINIMAL -> 18; case COMPACT -> 40; case EXPANDED -> 70; };
    }

    public static void render(GuiGraphics graphics, Font font, NowPlaying np, Style style, int x, int y, int accent,
                              boolean showArt, boolean showDevice, long now) {
        int w = width(style), h = height(style);
        JilatonUi.fillRounded(graphics, x, y, w, h, 4, 0xE6070707);
        JilatonUi.strokeRounded(graphics, x, y, w, h, 4, JilatonUi.HAIRLINE);
        boolean has = np.hasTrack();
        String title = has ? np.title() : np.status();
        String artist = has ? np.artist() : np.source();
        double progress = np.progress(now);
        int accentColor = 0xFF000000 | accent;
        if (style == Style.MINIMAL) {
            drawPlayState(graphics, x + 7, y + 5, np.playing());
            String line = has && !artist.isEmpty() ? title + "  ·  " + artist : title;
            graphics.drawString(font, clip(font, line, w - 26), x + 19, y + 5, JilatonUi.TEXT, false);
            if (has) graphics.fill(x + 4, y + h - 2, x + 4 + (int) ((w - 8) * progress), y + h - 1, accentColor);
            return;
        }
        int art = style == Style.EXPANDED ? 52 : 28;
        int textX = x + 8;
        if (showArt) {
            int ax = x + 6, ay = y + 6;
            if (np.artwork() != null) graphics.blit(np.artwork(), ax, ay, ax + art, ay + art, 0, 1, 0, 1);
            else {
                JilatonUi.fillRounded(graphics, ax, ay, art, art, 3, 0xFF141414);
                graphics.drawCenteredString(font, "♪", ax + art / 2, ay + art / 2 - 4, JilatonUi.TEXT_3);
            }
            textX = ax + art + 8;
        }
        int textWidth = x + w - 8 - textX;
        graphics.drawString(font, clip(font, title, textWidth), textX, y + 7, JilatonUi.TEXT, false);
        graphics.drawString(font, clip(font, artist, textWidth), textX, y + 18, JilatonUi.TEXT_2, false);
        if (style == Style.EXPANDED) {
            String device = showDevice ? (np.device().isEmpty() ? np.status() : np.device()) : np.status();
            graphics.drawString(font, clip(font, device.toUpperCase(java.util.Locale.ROOT), textWidth), textX, y + 30, JilatonUi.TEXT_3, false);
            int barY = y + h - 14;
            graphics.fill(textX, barY, x + w - 8, barY + 2, 0xFF262626);
            graphics.fill(textX, barY, textX + (int) ((x + w - 8 - textX) * progress), barY + 2, accentColor);
            String times = NowPlaying.time(np.positionNow(now)) + " / " + NowPlaying.time(np.durationMs());
            graphics.drawString(font, times, x + w - 8 - font.width(times), barY + 4, JilatonUi.TEXT_3, false);
            drawPlayState(graphics, textX, barY + 4, np.playing());
        } else {
            graphics.fill(textX, y + h - 6, x + w - 8, y + h - 5, 0xFF262626);
            graphics.fill(textX, y + h - 6, textX + (int) ((x + w - 8 - textX) * progress), y + h - 5, accentColor);
        }
    }

    private static void drawPlayState(GuiGraphics graphics, int x, int y, boolean playing) {
        if (playing) {
            graphics.fill(x, y, x + 2, y + 7, JilatonUi.TEXT);
            graphics.fill(x + 4, y, x + 6, y + 7, JilatonUi.TEXT);
        } else for (int i = 0; i < 4; i++) graphics.fill(x + i, y + i, x + i + 1, y + 7 - i, JilatonUi.TEXT);
    }

    static String clip(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        String ellipsis = "…";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }
}
