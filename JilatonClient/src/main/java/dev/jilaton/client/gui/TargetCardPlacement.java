package dev.jilaton.client.gui;

import dev.jilaton.client.config.HudStyle;
import java.util.Objects;

/**
 * Ephemeral projected placement; never writes over the user's saved HUD layout. The card sits beside the
 * target's body, left of it by preference, and its near edge clears the body's projected half-width so it
 * never covers the opponent however close they are.
 */
public final class TargetCardPlacement {
    private static final int GAP = 8;
    private final AnimatedValue x = new AnimatedValue(), y = new AnimatedValue();
    private Object identity;
    private int viewportWidth, viewportHeight;
    private boolean right;

    public HudLayout place(Object target, double anchorX, double anchorY, double bodyHalfWidth, int width, int height,
                           int viewportWidth, int viewportHeight, HudStyle style, long now, boolean immediate) {
        if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY)) throw new IllegalArgumentException("Invalid target anchor");
        if (!Objects.equals(identity, target) || this.viewportWidth != viewportWidth || this.viewportHeight != viewportHeight) {
            reset(); identity = target;
            this.viewportWidth = viewportWidth; this.viewportHeight = viewportHeight;
        }
        HudStyle free = new HudStyle(style.scale(), style.panelOpacity(), style.accent(), HudStyle.Alignment.FREE, true);
        HudLayout dimensions = HudLayout.resolve(width, height, viewportWidth, viewportHeight, 0, 0, null, free);
        double half = Double.isFinite(bodyHalfWidth) ? Math.max(0, bodyHalfWidth) : 0;
        double leftX = anchorX - half - GAP - dimensions.width();
        // Hysteresis prevents the card alternating sides at the left screen edge.
        if (!right && leftX < 4) right = true;
        else if (right && leftX > 24) right = false;
        double desiredX = right ? anchorX + half + GAP : leftX;
        double desiredY = anchorY - dimensions.height() * 0.5;
        int travelX = viewportWidth - dimensions.width(), travelY = viewportHeight - dimensions.height();
        double smoothX = x.update(clamp(desiredX, travelX), now, immediate);
        double smoothY = y.update(clamp(desiredY, travelY), now, immediate);
        return new HudLayout((int) Math.round(clamp(smoothX, travelX)), (int) Math.round(clamp(smoothY, travelY)),
                dimensions.width(), dimensions.height(), dimensions.scale());
    }

    private static double clamp(double value, int maximum) { return Math.max(0, Math.min(maximum, value)); }
    public void reset() { x.reset(); y.reset(); identity = null; right = false; }
}
