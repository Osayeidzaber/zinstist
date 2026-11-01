package dev.jilaton.client.gui;

import java.util.List;

/**
 * Snapping for the HUD editor. A dragged widget's left edge, centre and right edge (and the same vertically) snap
 * to the screen edges, the screen centre and the edges/centres of the other widgets when within {@link #RANGE}
 * pixels. Returns the snapped position and the guide line that caused it (NaN when nothing snapped).
 */
public final class HudSnap {
    public static final double RANGE = 6;

    public record Box(double x, double y, double w, double h) { }

    public record Result(double x, double y, double guideX, double guideY) { }

    private HudSnap() {
    }

    public static Result snap(Box moving, List<Box> others, double screenW, double screenH) {
        double[] targetsX = targets(others, screenW, true);
        double[] targetsY = targets(others, screenH, false);
        double[] sx = axis(moving.x(), moving.w(), targetsX);
        double[] sy = axis(moving.y(), moving.h(), targetsY);
        return new Result(sx[0], sy[0], sx[1], sy[1]);
    }

    private static double[] targets(List<Box> others, double screen, boolean horizontal) {
        double[] t = new double[3 + others.size() * 3];
        t[0] = 0;
        t[1] = screen / 2;
        t[2] = screen;
        int i = 3;
        for (Box b : others) {
            double start = horizontal ? b.x() : b.y(), size = horizontal ? b.w() : b.h();
            t[i++] = start;
            t[i++] = start + size / 2;
            t[i++] = start + size;
        }
        return t;
    }

    /** {snapped start, guide} for one axis. */
    private static double[] axis(double start, double size, double[] targets) {
        double[] anchors = {0, size / 2, size};
        double best = RANGE + 1, snapped = start, guide = Double.NaN;
        for (double anchor : anchors) {
            for (double target : targets) {
                double d = Math.abs(start + anchor - target);
                if (d <= RANGE && d < best) {
                    best = d;
                    snapped = target - anchor;
                    guide = target;
                }
            }
        }
        return new double[]{snapped, guide};
    }
}
