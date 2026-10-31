package dev.jilaton.client.gui.design;

import dev.jilaton.client.gui.JilatonType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws and measures text for Jilaton's Inter {@link Font} with {@link TextRaster}: every call to
 * {@code GuiGraphics.drawString} or {@code Font.width} on that font lands here ({@code JilatonTextMixin}), as does
 * {@link JilatonType#draw}. Text is placed on whole physical pixels and blitted 1:1, so it is never scaled or filtered.
 * Geometry matches Minecraft's TTF provider at size 9: the em is 0.826 of the line size and the baseline sits at
 * 0.856 of it, so existing layouts keep their alignment.
 */
public final class JilatonText {
    public static final float EM = 0.826F, BASELINE = 0.856F;
    /** On-screen em below which Regular and Mono draw one weight heavier, so thin strokes keep contrast. */
    private static final float HEAVIER_BELOW_PX = 11.5F;

    /** A run of text in one face and colour. */
    public record Segment(String text, JilatonType.Face face, int argb) { }

    private JilatonText() { }

    /** Whether {@code font} is Jilaton's Inter font (and the Inter option is on). */
    public static boolean handles(Font font) {
        return font != null && font == JilatonType.interIfCreated() && JilatonType.enabled();
    }

    private static double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    static TextRaster.Face raster(JilatonType.Face face, float emPx) {
        JilatonType.Face shown = emPx >= HEAVIER_BELOW_PX ? face : switch (face) {
            case REGULAR -> JilatonType.Face.MEDIUM;
            case MONO -> JilatonType.Face.MONO_MEDIUM;
            default -> face;
        };
        return TextRaster.Face.valueOf(shown.name());
    }

    /** Splits styled text into segments: face from the style's font, colour from the style or {@code color}. */
    public static List<Segment> segments(FormattedCharSequence sequence, int color) {
        List<Segment> out = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        JilatonType.Face[] face = {null};
        int[] argb = {0};
        sequence.accept((index, style, codePoint) -> {
            JilatonType.Face f = JilatonType.faceOf(style.getFont(), style.isBold());
            int c = style.getColor() == null ? color : (color & 0xFF000000) | (style.getColor().getValue() & 0xFFFFFF);
            if (text.length() > 0 && (f != face[0] || c != argb[0])) {
                out.add(new Segment(text.toString(), face[0], argb[0]));
                text.setLength(0);
            }
            face[0] = f;
            argb[0] = c;
            text.appendCodePoint(codePoint);
            return true;
        });
        if (text.length() > 0) out.add(new Segment(text.toString(), face[0], argb[0]));
        return out;
    }

    public static List<Segment> segments(FormattedText formatted) {
        List<Segment> out = new ArrayList<>();
        formatted.visit((style, text) -> {
            out.add(new Segment(text, JilatonType.faceOf(style.getFont(), style.isBold()), 0));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** Width in GUI units of {@code segments} drawn at line size {@code size}. */
    public static float width(List<Segment> segments, float size) {
        float em = EM * size;
        double emPx = em * guiScale();
        float width = 0;
        for (Segment segment : segments) width += TextRaster.advance(segment.text(), raster(segment.face(), (float) emPx), em);
        return width;
    }

    public static float width(String text, JilatonType.Face face, float size) {
        return width(List.of(new Segment(text, face, 0)), size);
    }

    /** Draws {@code segments} with the line's top-left at ({@code x}, {@code y}), line size {@code size} units. */
    public static void draw(GuiGraphics graphics, List<Segment> segments, float x, float y, float size, boolean shadow) {
        if (segments.isEmpty()) return;
        if (shadow) {
            List<Segment> dark = new ArrayList<>(segments.size());
            for (Segment s : segments) dark.add(new Segment(s.text(), s.face(), (s.argb() & 0xFF000000) | ((s.argb() & 0xFCFCFC) >> 2)));
            drawOnce(graphics, dark, x + size / 9F, y + size / 9F, size);
        }
        drawOnce(graphics, segments, x, y, size);
    }

    private static void drawOnce(GuiGraphics graphics, List<Segment> segments, float x, float y, float size) {
        org.joml.Matrix3x2fStack pose = graphics.pose();
        double gs = guiScale();
        double ppu = Math.abs(pose.m00()) * gs;
        if (ppu <= 0) return;
        float emPx = (float) (EM * size * ppu);
        if (emPx < 2) return;
        double penX = (pose.m00() * x + pose.m20()) * gs;
        double baseline = (pose.m11() * (y + BASELINE * size) + pose.m21()) * gs;
        int baseY = (int) Math.round(baseline);
        float inv = (float) (1 / gs);
        pose.pushMatrix();
        pose.identity();
        pose.scale(inv, inv);
        for (Segment segment : segments) {
            if (segment.text().isEmpty()) continue;
            TextRaster.Face face = raster(segment.face(), emPx);
            TextRaster.Image image = TextRaster.gui(segment.text(), face, emPx);
            int left = (int) Math.round(penX) - image.padLeft();
            int top = baseY - Math.round(image.baseline());
            if ((segment.argb() >>> 24) != 0)
                graphics.blit(RenderPipelines.GUI_TEXTURED, image.id(), left, top, 0, 0, image.width(), image.height(),
                        image.width(), image.height(), segment.argb());
            penX += TextRaster.advance(segment.text(), face, emPx);
        }
        pose.popMatrix();
    }
}
