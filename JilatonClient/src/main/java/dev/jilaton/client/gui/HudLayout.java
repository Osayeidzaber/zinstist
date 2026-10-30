package dev.jilaton.client.gui;

import dev.jilaton.client.config.ClientConfig.HudPosition;
import dev.jilaton.client.config.HudStyle;

/** Shared screen-space geometry for rendering and editor input. */
public record HudLayout(int x, int y, int width, int height, float scale) {
    public static HudLayout resolve(int contentWidth, int contentHeight, int viewportWidth, int viewportHeight,
                                    int defaultX, int defaultY, HudPosition position, HudStyle style) {
        if (contentWidth <= 0 || contentHeight <= 0 || viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("HUD dimensions must be positive");
        }
        float scale = (float) Math.min(style.scale(), Math.min(viewportWidth / (double) contentWidth,
                viewportHeight / (double) contentHeight));
        int width = Math.min(viewportWidth, (int) Math.ceil(contentWidth * (double) scale));
        int height = Math.min(viewportHeight, (int) Math.ceil(contentHeight * (double) scale));
        int travelX = viewportWidth - width;
        int travelY = viewportHeight - height;
        int x = position == null ? defaultX : (int) Math.round(travelX * position.x());
        int y = position == null ? defaultY : (int) Math.round(travelY * position.y());
        x = switch (style.alignment()) {
            case FREE -> x;
            case LEFT -> 8;
            case CENTER -> travelX / 2;
            case RIGHT -> travelX - 8;
        };
        return new HudLayout(Math.max(0, Math.min(travelX, x)), Math.max(0, Math.min(travelY, y)),
                width, height, scale);
    }
}
