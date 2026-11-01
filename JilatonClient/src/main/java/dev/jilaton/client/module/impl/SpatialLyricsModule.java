package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.TextRaster;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.music.Lyrics;
import dev.jilaton.client.music.MusicCenter;
import dev.jilaton.client.music.NowPlaying;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.render.fx.LyricLayout;
import dev.jilaton.client.render.fx.LyricSky;
import dev.jilaton.client.render.fx.LyricTrail;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Synchronized lyrics placed in the world around the local player. Lines come from a local .lrc file when
 * one matches, otherwise from LRCLIB for whatever the music source reports playing (at the user's request).
 * Each stack of lines gets one anchor, chosen where the player is looking when the stack is born, and then
 * stays fixed in world space while lines change. Only a stack that stays out of view or out of reach is
 * moved, as a single eased glide, so the text reads as an object in the air instead of a camera overlay; the
 * active line fades and scales in, with sung words lit as they are sung.
 */
public final class SpatialLyricsModule extends ClientModule implements FxEmitter {
    private static final int OWNER = FxParticles.ownerId("spatial_lyrics");
    /** "World Trail" places each line where you look and leaves it there; the rest are anchored stacks. */
    static final String WORLD_TRAIL = "World Trail";
    /** "Sky Lyrics" pins each line to its own compass point in the sky, like the sun and stars. */
    static final String SKY = "Sky Lyrics";
    private final ChoiceSetting mode = addSetting(new ChoiceSetting("mode", "Position mode", modes(), WORLD_TRAIL));
    private final NumberSetting radius = addSetting(new NumberSetting("radius", "Radius (blocks)", 1.5, 4, 0.05, 2.3));
    private final NumberSetting height = addSetting(new NumberSetting("height", "Height", -1, 2, 0.05, 0.3));
    private final NumberSetting side = addSetting(new NumberSetting("side", "Horizontal angle (°)", -90, 90, 1, 26));
    private final NumberSetting spread = addSetting(new NumberSetting("spread", "Line spacing on arc (°)", 6, 40, 1, 16));
    private final NumberSetting fontSize = addSetting(new NumberSetting("font_size", "Font size", 0.5, 2, 0.05, 1));
    private final ChoiceSetting align = addSetting(new ChoiceSetting("align", "Text alignment", List.of("Center", "Left", "Right"), "Center"));
    private final NumberSetting lines = addSetting(new NumberSetting("lines", "Visible lines", 1, 5, 1, 3));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1, 0.05, 0.95));
    private final NumberSetting dim = addSetting(new NumberSetting("dim", "Other lines opacity", 0.05, 1, 0.05, 0.4));
    private final NumberSetting glow = addSetting(new NumberSetting("glow", "Glow", 0, 1, 0.05, 0.35));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Accent color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom accent hex", "F2F2F2"));
    private final ChoiceSetting transition = addSetting(new ChoiceSetting("transition", "Transition", List.of("Slide", "Fade", "Scale"), "Slide"));
    private final NumberSetting speed = addSetting(new NumberSetting("speed", "Transition time (s)", 0.1, 1.2, 0.05, 0.45));
    private final ChoiceSetting facing = addSetting(new ChoiceSetting("facing", "Rotation", List.of("Face camera", "Follow arc"), "Face camera"));
    private final NumberSetting offset = addSetting(new NumberSetting("offset", "Timing offset (ms)", -3000, 3000, 10, 0));
    private final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Max render distance", 4, 32, 1, 12));
    private final BooleanSetting thirdPersonOnly = addSetting(new BooleanSetting("third_person_only", "Only in third person", false));
    private final BooleanSetting occlusion = addSetting(new BooleanSetting("occlusion", "Hidden behind blocks", true));
    private final BooleanSetting panel = addSetting(new BooleanSetting("panel", "Background panel", false));
    private final BooleanSetting floating = addSetting(new BooleanSetting("floating", "Floating motion", true));
    private final BooleanSetting accents = addSetting(new BooleanSetting("accents", "Particle accents on new line", true));
    private final BooleanSetting titleFallback = addSetting(new BooleanSetting("title_fallback", "Show track title when no lyrics", true));
    private final NumberSetting trailLines = addSetting(new NumberSetting("trail_lines", "Lines kept (trail & sky)", 1, 8, 1, 5));
    private final NumberSetting linger = addSetting(new NumberSetting("linger", "Linger (trail & sky, s)", 3, 40, 1, 14));
    private final NumberSetting depth = addSetting(new NumberSetting("depth", "3D depth layers", 0, 6, 1, 0));
    private final LyricTrail trail = new LyricTrail();
    /** Font widths of BMP characters below U+3000, filled on first use; stored +1 so 0 means unknown. */
    private final int[] glyphWidths = new int[0x3000];
    /** Line fitting asks for every character's width every frame; this answers without a String per call. */
    private final IntUnaryOperator advance = this::glyphWidth;
    /** Runs for one sky line; the raster consumes them immediately, so one list serves every line. */
    private final List<TextRaster.Run> skyRuns = new ArrayList<>();
    private final LyricSky sky = new LyricSky();
    private long lastTrailNanos;

    private LyricLayout.Anchor anchor;
    private StackKey stackKey;
    private int lastIndex = Integer.MIN_VALUE;
    private long changedAt;
    private double previousSlotShift;

    /** Everything that starts a new anchor when it changes: the track, its lyrics, and the stack layout. */
    record StackKey(String source, String title, String artist, Lyrics lyrics, String mode, double radius,
                    double spread) { }

    public SpatialLyricsModule() {
        super("spatial_lyrics", "Spatial Lyrics", "Synchronized LRC lyrics floating in 3D around your character.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        // A resource reload can swap the font while the module is off; widths are re-measured on next use.
        java.util.Arrays.fill(glyphWidths, 0);
        resetStack();
        FxParticles.get().clearOwner(OWNER);
    }

    private static List<String> modes() {
        List<String> all = new ArrayList<>();
        all.add(WORLD_TRAIL);
        all.add(SKY);
        all.addAll(LyricLayout.MODES);
        return List.copyOf(all);
    }

    private void resetStack() {
        trail.clear();
        sky.clear();
        lastTrailNanos = 0;
        anchor = null;
        stackKey = null;
        lastIndex = Integer.MIN_VALUE;
        previousSlotShift = 0;
    }

    /** Settings handles for the Music Center positioning preview. */
    public NumberSetting radiusSetting() { return radius; }
    public NumberSetting heightSetting() { return height; }
    public NumberSetting sideSetting() { return side; }
    public ChoiceSetting modeSetting() { return mode; }
    public NumberSetting offsetSetting() { return offset; }
    public NumberSetting spreadSetting() { return spread; }
    public NumberSetting trailLinesSetting() { return trailLines; }
    public NumberSetting lingerSetting() { return linger; }

    public LyricLayout.Params params(boolean firstPerson, double time, double bodyYaw) {
        return new LyricLayout.Params(mode.value(), radius.value(), height.value(), side.value(), spread.value(),
                firstPerson, floating.value() ? time : 0, bodyYaw);
    }

    @Override
    public void emit(FxFrame f) {
        Player player = f.client.player;
        if (player == null || f.client.options.hideGui) return;
        if (thirdPersonOnly.value() && f.firstPerson) return;
        MusicCenter center = MusicCenter.get();
        NowPlaying playing = center.nowPlaying();
        if (!playing.hasTrack()) {
            resetStack();
            return;
        }
        Lyrics lyrics = center.lyrics();
        long position = playing.positionNow(f.now) + (long) offset.value();
        int index;
        Lyrics.Line active = null;
        if (lyrics.isEmpty()) {
            // Music Visuals already draws the track name in this view; a second copy would double-draw it.
            if (!titleFallback.value() || musicVisualsShowsTrack(f)) {
                resetStack();
                return;
            }
            index = 0;
        } else {
            index = lyrics.indexAt(position);
            if (index >= 0) active = lyrics.lines().get(index);
        }
        double viewYaw = Math.atan2(-f.forwardX, f.forwardZ);
        double now = f.now / 1e9;
        StackKey key = new StackKey(playing.source(), playing.title(), playing.artist(), lyrics, mode.value(),
                radius.value(), spread.value());
        if (mode.value().equals(WORLD_TRAIL)) {
            sky.clear();
            emitTrail(f, player, playing, lyrics, index, active, position, key);
            return;
        }
        if (mode.value().equals(SKY)) {
            emitSky(f, playing, lyrics, index, active, position, key);
            return;
        }
        trail.clear();
        sky.clear();
        // One anchor per stack: it is placed on the gaze when the stack is born (track, lyrics or layout
        // change) and then never follows the camera. Only a stack that stays out of view or out of reach is
        // moved, as a single eased glide.
        if (anchor == null || !key.equals(stackKey)) {
            stackKey = key;
            lastIndex = Integer.MIN_VALUE;
            Vec3 home = anchorPoint(player, f, viewYaw, f.firstPerson);
            anchor = LyricLayout.Anchor.birth(home.x, home.y, home.z, viewYaw, now);
        } else if (anchor.wantsReanchor(now, viewYaw, player.getX(), player.getEyeY(), player.getZ())) {
            Vec3 home = anchorPoint(player, f, viewYaw, f.firstPerson);
            anchor = anchor.reanchor(home.x, home.y, home.z, viewYaw, now);
        }
        boolean changed = index != lastIndex;
        if (changed) {
            previousSlotShift = lastIndex == Integer.MIN_VALUE ? 0 : Math.max(-2, Math.min(2, index - lastIndex));
            lastIndex = index;
            changedAt = f.now;
        }
        double transitionT = Math.min(1, (f.now - changedAt) / 1e9 / speed.value());
        double eased = 1 - Math.pow(1 - transitionT, 3);
        if (f.reducedMotion) eased = 1;

        double ax = anchor.x(now), ay = anchor.y(now), az = anchor.z(now);
        double bearing = anchor.bearing(now);
        // The arc is laid out around the anchor's frozen bearing, never the live gaze, and "Follow arc"
        // uses the same bearing, so nothing about the stack rotates with the camera.
        LyricLayout.Params params = params(f.firstPerson, f.seconds() * (f.reducedMotion ? 0 : 1), bearing);
        double shift = LyricLayout.verticalShift(params, bearing);
        if (changed && accents.value() && active != null && !active.text().isEmpty())
            spawnAccents(f, ax, ay, az, bearing, params, shift);

        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        int visible = mode.value().equals("Minimal") ? 1 : (int) lines.value();
        int before = (visible - 1) / 2, after = visible - 1 - before;
        double maxSlot = Math.max(before, after);
        double baseAlpha = opacity.value() * f.intensity;
        int alignValue = switch (align.value()) { case "Left" -> -1; case "Right" -> 1; default -> 0; };
        for (int slot = -before - 1; slot <= after + 1; slot++) {
            int lineIndex = index + slot;
            String text;
            List<FxFrame.Run> runs = new ArrayList<>();
            if (lyrics.isEmpty()) {
                if (slot != 0) continue;
                String fallback = playing.artist() == null || playing.artist().isEmpty()
                        ? playing.title() : playing.title() + "  ·  " + playing.artist();
                text = fallback.isEmpty() ? "" : LyricLayout.fitText(fallback, advance, MAX_LINE_PIXELS);
            } else {
                text = fittedLine(lyrics, index, slot, advance);
                if (text == null) continue;
            }
            if (text.isEmpty()) continue;
            // Lines slide from the slot they occupied before the change into their new slot.
            double from = slot + previousSlotShift;
            double k2 = transition.value().equals("Slide") ? from + (slot - from) * eased : slot;
            if (Math.abs(k2) > maxSlot + 0.999) continue;
            double alpha = baseAlpha * LyricLayout.opacity(k2, dim.value());
            if (transition.value().equals("Fade") && slot == 0) alpha *= eased;
            double scale = LyricLayout.scale(k2);
            if (transition.value().equals("Scale") && slot == 0) scale *= 0.85 + 0.15 * eased;
            if (Math.abs(slot) > maxSlot) alpha *= 1 - eased;
            // Only lines that were not on screen before fade in; steady lines keep their brightness, so a
            // line change animates the new line instead of pulsing the whole stack.
            double appear = appearEase(f.reducedMotion, f.now - changedAt);
            if (slot == 0) {
                alpha *= 0.15 + 0.85 * appear;
                scale *= 0.88 + 0.12 * appear;
            } else if (fadesIn(from, maxSlot, previousSlotShift == 0)) alpha *= appear;
            LyricLayout.Slot placement = LyricLayout.slot(params, k2, bearing);
            double x = ax + placement.dx(), y = ay + placement.dy() + shift, z = az + placement.dz();
            y = lift(f, x, z, y, 0.4);
            double distance = Math.sqrt(f.distanceSqr(x, y, z));
            double maxD = maxDistance.value();
            if (distance > maxD) continue;
            alpha *= Math.min(1, Math.max(0, (maxD - distance) / (maxD * 0.3)));
            alpha *= clearance(f, x, y, z);
            if (alpha < 0.02) continue;
            if (slot == 0) {
                boolean timed = active != null && active.timedWords() && lineIndex == index
                        && active.text().equals(text);
                shine(text, timed ? active : null, timed ? Lyrics.wordsSung(active, position) : 0, rgb, alpha,
                        f.seconds(), runs);
            } else runs.add(new FxFrame.Run(text, FxShapes.argb(0xDCDCDC, alpha)));
            // The text is already fitted to MAX_LINE_PIXELS, so glyphs keep one size: an overlong line loses
            // whole words to an ellipsis instead of shrinking below the readable size.
            float size = (float) (0.020 * fontSize.value() * scale);
            int glowColor = FxShapes.argb(rgb, Math.max(0.05, alpha * Math.max(glow.value(), 0.2) * 0.35));
            int background = panel.value() ? FxShapes.argb(0x000000, alpha * 0.45) : 0;
            boolean billboard = facing.value().equals("Face camera");
            f.text(new FxFrame.WorldText(x, y, z, runs, size, background, glowColor, billboard,
                    (float) (placement.facingYaw() + 180), !occlusion.value(), alignValue));
        }
    }

    /**
     * World Trail: a line change places the new line along the current gaze (in front of any block the gaze hits)
     * and leaves every earlier line where it was born. Lines are drawn with fixed facing, so the player can walk
     * around them, and with stacked depth layers behind the face so they read as solid lettering.
     */
    private void emitTrail(FxFrame f, Player player, NowPlaying playing, Lyrics lyrics, int index, Lyrics.Line active,
                           long position, StackKey key) {
        if (!key.equals(stackKey)) {
            stackKey = key;
            trail.clear();
            lastIndex = Integer.MIN_VALUE;
        }
        if (index != lastIndex) {
            lastIndex = index;
            changedAt = f.now;
            String text;
            if (lyrics.isEmpty()) {
                String fallback = playing.artist() == null || playing.artist().isEmpty()
                        ? playing.title() : playing.title() + "  ·  " + playing.artist();
                text = fallback.isEmpty() ? null : LyricLayout.fitText(fallback, advance, MAX_LINE_PIXELS);
            } else text = index < 0 ? null : fittedLine(lyrics, index, 0, advance);
            if (text != null && !text.isEmpty()) {
                double reach = LyricLayout.effectiveRadius(radius.value(), f.firstPerson) + 0.6;
                Vec3 eye = new Vec3(f.camX, f.camY, f.camZ);
                double[] ray = LyricLayout.turned(f.forwardX, f.forwardY, f.forwardZ, side.value());
                Vec3 end = eye.add(ray[0] * reach, ray[1] * reach, ray[2] * reach);
                double hit = -1;
                if (f.client.level != null) {
                    var result = f.client.level.clip(new ClipContext(eye, end, ClipContext.Block.VISUAL,
                            ClipContext.Fluid.NONE, player));
                    if (result.getType() != HitResult.Type.MISS) hit = result.getLocation().distanceTo(eye);
                }
                // Beside the gaze by the side angle, never on the crosshair where it would cover the fight.
                double[] dir = LyricLayout.turned(f.forwardX, f.forwardY, f.forwardZ, side.value());
                LyricTrail.Entry placed = trail.place(text, index, eye.x, eye.y + height.value() * 0.25, eye.z,
                        dir[0], dir[1], dir[2], LyricTrail.placementDistance(reach, hit), f.now,
                        (int) trailLines.value());
                if (accents.value()) spawnTrailAccents(f, placed);
            }
        }
        double dt = lastTrailNanos == 0 ? 0 : Math.min(0.1, (f.now - lastTrailNanos) / 1e9);
        lastTrailNanos = f.now;
        trail.step(dt, f.now, linger.value(), index);
        LyricTrail.Entry current = trail.active(index);

        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        int alignValue = switch (align.value()) { case "Left" -> -1; case "Right" -> 1; default -> 0; };
        double maxD = maxDistance.value();
        int layers = (int) depth.value();
        for (LyricTrail.Entry e : trail.entries()) {
            boolean isActive = e == current;
            double alpha = opacity.value() * f.intensity * LyricTrail.alpha(e, f.now, linger.value(), isActive)
                    * (isActive ? 1 : dim.value());
            double y = e.drawY();
            double distance = Math.sqrt(f.distanceSqr(e.x, y, e.z));
            if (distance > maxD) continue;
            alpha *= Math.min(1, Math.max(0, (maxD - distance) / (maxD * 0.3)));
            alpha *= clearance(f, e.x, y, e.z);
            if (alpha < 0.02) continue;
            double appear = f.reducedMotion ? 1 : LyricTrail.appear(e, f.now);
            y -= 0.12 * (1 - appear);
            float size = (float) (0.020 * fontSize.value() * (0.86 + 0.14 * appear));
            // Depth layers first, back to front, so see-through mode still paints the face last. Only the lit line
            // is extruded: on dimmed lines the layers smeared the letters into the terrain.
            for (int layer = isActive ? layers : 0; layer >= 1; layer--) {
                double back = layer * 0.012 * fontSize.value();
                int shade = shaded(isActive ? rgb : 0x6A6A70, 0x0B0B10, 0.45 + 0.5 * layer / Math.max(1, layers));
                f.text(new FxFrame.WorldText(e.x + e.dirX * back, y + e.dirY * back, e.z + e.dirZ * back,
                        List.of(new FxFrame.Run(e.text, FxShapes.argb(shade, alpha))), size, 0, 0, false, e.yaw,
                        !occlusion.value(), alignValue));
            }
            List<FxFrame.Run> runs = new ArrayList<>();
            if (isActive) {
                boolean timed = active != null && active.timedWords() && active.text().equals(e.text);
                shine(e.text, timed ? active : null, timed ? Lyrics.wordsSung(active, position) : 0, rgb, alpha,
                        f.seconds(), runs);
            } else runs.add(new FxFrame.Run(e.text, FxShapes.argb(0xDCDCDC, alpha)));
            // Every line is outlined (a non-zero glow marks it for the renderer's 8-way outline) so it stays legible.
            int glowColor = FxShapes.argb(rgb, Math.max(0.05, alpha * Math.max(glow.value(), 0.2) * 0.35));
            int background = panel.value() ? FxShapes.argb(0x000000, alpha * 0.45) : 0;
            f.text(new FxFrame.WorldText(e.x, y, e.z, runs, size, background, glowColor, false, e.yaw,
                    !occlusion.value(), alignValue));
        }
    }

    /** Raster em of the sky text: large enough that a sky line reads crisply at {@link LyricSky#RADIUS}. */
    private static final float SKY_TEXT_EM = 40;
    /**
     * World blocks per raster pixel. At {@link #SKY_TEXT_EM} the face averages ≈12 px per character, so a short
     * 20-character line (≈240 px) spans ≈12 blocks, about 10° at {@link LyricSky#RADIUS}, and a 60-character line
     * about a quarter of the view. The earlier 0.018 drew that short line at ≈70 px of a 1080p screen in runtime
     * scene 35c, too small to read.
     */
    private static final double SKY_UNITS_PER_TEXT_PIXEL = 0.05;
    /** The sky has room for long lines, so it fits wider than a world-space slot before ellipsising. */
    static final int SKY_MAX_LINE_PIXELS = 360;
    private static final float[] QUAD_UVS = {0, 1, 1, 1, 1, 0, 0, 0};

    /**
     * Sky Lyrics: each line is its own fixed point on the celestial sphere, placed along the gaze when it starts
     * and left there, so turning the camera sweeps past the lines like stars instead of carrying them along. The
     * text is rasterised into a texture and emitted as sky-layer quads, and it shares the {@link LyricSky}
     * lifecycle (incoming, settling, active, transitioning, fading, expired) with the other placements.
     */
    private void emitSky(FxFrame f, NowPlaying playing, Lyrics lyrics, int index, Lyrics.Line active, long position,
                         StackKey key) {
        if (!key.equals(stackKey)) {
            stackKey = key;
            sky.clear();
            trail.clear();
            lastIndex = Integer.MIN_VALUE;
        }
        if (index != lastIndex) {
            lastIndex = index;
            changedAt = f.now;
            String text;
            if (lyrics.isEmpty()) {
                String fallback = playing.artist() == null || playing.artist().isEmpty()
                        ? playing.title() : playing.title() + "  ·  " + playing.artist();
                text = fallback.isEmpty() ? null : LyricLayout.fitText(fallback, advance, SKY_MAX_LINE_PIXELS);
            } else text = index < 0 ? null : fittedLine(lyrics, index, 0, advance, SKY_MAX_LINE_PIXELS);
            if (text != null && !text.isEmpty())
                sky.place(text, index, f.forwardX, f.forwardY, f.forwardZ, f.now, (int) trailLines.value());
        }
        sky.step(f.now, linger.value(), index);
        LyricSky.Entry current = sky.active(index);
        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        for (LyricSky.Entry e : sky.entries()) {
            boolean isActive = e == current;
            double alpha = opacity.value() * f.intensity * LyricSky.alpha(e, f.now, linger.value(), isActive)
                    * (isActive ? 1 : dim.value());
            // Sky lines are scenery, so they only give way to the crosshair during a fight.
            if (dev.jilaton.client.render.CombatTargetTracker.targetId() >= 0) {
                double h = Math.cos(e.elevation);
                alpha *= LyricLayout.centerClearance(-Math.sin(e.azimuth) * h, Math.sin(e.elevation),
                        Math.cos(e.azimuth) * h, f.forwardX, f.forwardY, f.forwardZ, true);
            }
            if (alpha < 0.02) continue;
            double appear = f.reducedMotion ? 1 : LyricSky.appear(e, f.now);
            List<TextRaster.Run> runs = skyRuns;
            runs.clear();
            if (isActive) {
                boolean timed = active != null && active.timedWords() && active.text().equals(e.text);
                shineRaster(e.text, timed ? active : null, timed ? Lyrics.wordsSung(active, position) : 0, rgb, runs);
            } else runs.add(new TextRaster.Run(e.text, 0xFFB4B4B4));
            if (runs.isEmpty()) continue;
            TextRaster.Image image = TextRaster.world(runs, TextRaster.Face.SEMIBOLD, SKY_TEXT_EM, SKY_TEXT_EM * 0.09F);
            double units = SKY_UNITS_PER_TEXT_PIXEL * fontSize.value() * (0.92 + 0.08 * appear);
            float[] corners = LyricSky.corners(e.azimuth, e.elevation, LyricSky.RADIUS,
                    image.width() * units, image.height() * units);
            // The sky pipeline ignores vertex alpha (screen blend), so the fade rides on the vertex brightness.
            int tone = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
            f.sky().add(new FxFrame.SkyQuad(image.id(), corners, QUAD_UVS, tone << 24 | tone << 16 | tone << 8 | tone));
        }
    }

    private int glyphWidth(int codePoint) {
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        if (codePoint < 0 || codePoint >= glyphWidths.length) return font.width(String.valueOf((char) codePoint));
        int cached = glyphWidths[codePoint];
        if (cached != 0) return cached - 1;
        int width = font.width(String.valueOf((char) codePoint));
        glyphWidths[codePoint] = width + 1;
        return width;
    }

    /** Opaque runs for sky text: colours are baked into the raster, brightness comes from the quad. */
    private static void shineRaster(String text, Lyrics.Line timed, int sungWords, int accent, List<TextRaster.Run> runs) {
        if (timed == null) {
            runs.add(new TextRaster.Run(text, 0xFFF5F5F5));
            return;
        }
        StringBuilder sung = new StringBuilder(), rest = new StringBuilder();
        for (int w = 0; w < timed.words().size(); w++) (w < sungWords ? sung : rest).append(timed.words().get(w).text());
        if (sung.length() > 0) runs.add(new TextRaster.Run(sung.toString(), 0xFF000000 | (accent & 0xFFFFFF)));
        if (rest.length() > 0) runs.add(new TextRaster.Run(rest.toString(), 0xFFDCDCDC));
    }

    private static boolean musicVisualsShowsTrack(FxFrame f) {
        return JilatonClient.moduleManager().get("music_visuals") instanceof MusicVisualsModule visuals
                && visuals.showsWorldTrackLabel(f.firstPerson);
    }

    private void spawnTrailAccents(FxFrame f, LyricTrail.Entry placed) {
        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        FxParticles.Spec spec = new FxParticles.Spec();
        for (int i = 0; i < 10; i++) {
            spec.reset();
            spec.x = placed.x + (Math.random() - 0.5) * 0.9;
            spec.y = placed.y - 0.08;
            spec.z = placed.z + (Math.random() - 0.5) * 0.9;
            spec.vy = 0.12 + Math.random() * 0.2;
            spec.drag = 0.9; spec.life = 1.0; spec.size0 = 0.022; spec.size1 = 0;
            spec.color = rgb; spec.alpha = 0.7 * opacity.value(); spec.glow = true; spec.walls = true; spec.owner = OWNER;
            FxParticles.get().spawn(spec);
        }
    }

    /**
     * World-space home for a stack in front of the player's gaze, lifted above whatever terrain is under it.
     * First person never lets the arc come closer than {@link LyricLayout#MIN_FIRST_PERSON_RADIUS} to the eye.
     */
    /** Fades text that sits on the crosshair; the clear zone widens while you are in a fight. */
    static double clearance(FxFrame f, double x, double y, double z) {
        return LyricLayout.centerClearance(x - f.camX, y - f.camY, z - f.camZ, f.forwardX, f.forwardY, f.forwardZ,
                dev.jilaton.client.render.CombatTargetTracker.targetId() >= 0);
    }

    private Vec3 anchorPoint(Player player, FxFrame f, double yaw, boolean firstPerson) {
        double r = LyricLayout.effectiveRadius(radius.value(), firstPerson);
        double x = player.getX() - Math.sin(yaw) * r;
        double z = player.getZ() + Math.cos(yaw) * r;
        double y = firstPerson ? player.getEyeY() : player.getY() + player.getEyeHeight() * 0.92;
        return new Vec3(x, lift(f, x, z, y, 1.2), z);
    }

    /**
     * Raises a point above the ground under it, but only for ground near the camera. A heightmap hit far
     * above the eyes is a ceiling (a bridge, a floating platform), and climbing onto it would put the text
     * out of view.
     */
    private static double lift(FxFrame f, double x, double z, double y, double margin) {
        double floor = ground(f, x, z);
        return floor <= f.camY + 1.5 ? Math.max(y, floor + margin) : y;
    }

    private static double ground(FxFrame f, double x, double z) {
        if (f.client.level == null) return Double.NEGATIVE_INFINITY;
        return f.client.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }

    /**
     * The lit line: sung words in the accent and the rest in a readable grey (word timing from {@link Lyrics}), or
     * the whole line bright when the lyric has no word timing. Colours change only when a word is sung, so the world
     * text renderer keeps one cached image per state instead of re-rasterising every frame.
     */
    private static void shine(String text, Lyrics.Line timed, int sungWords, int accent, double alpha, double time,
                              List<FxFrame.Run> runs) {
        if (timed == null) {
            runs.add(new FxFrame.Run(text, FxShapes.argb(0xF5F5F5, alpha)));
            return;
        }
        StringBuilder sung = new StringBuilder(), rest = new StringBuilder();
        for (int w = 0; w < timed.words().size(); w++) (w < sungWords ? sung : rest).append(timed.words().get(w).text());
        if (sung.length() > 0) runs.add(new FxFrame.Run(sung.toString(), FxShapes.argb(accent, alpha)));
        if (rest.length() > 0) runs.add(new FxFrame.Run(rest.toString(), FxShapes.argb(0xA8A8A8, alpha)));
    }

    /**
     * Widest drawn line in font pixels; at 0.020 units per pixel this is 4 world units per font-size unit, enough
     * for a typical full lyric line so it reads as a sentence instead of a few words and an ellipsis.
     */
    static final int MAX_LINE_PIXELS = 200;

    /**
     * Text drawn in {@code slot} relative to the active {@code index}, fitted on a word boundary to
     * {@link #MAX_LINE_PIXELS}. Returns null when the slot holds nothing to draw — outside the lyric file or a
     * blank line — so the caller skips it and never indexes out of range.
     */
    static String fittedLine(Lyrics lyrics, int index, int slot, IntUnaryOperator advance) {
        return fittedLine(lyrics, index, slot, advance, MAX_LINE_PIXELS);
    }

    /** Same fit at a caller-chosen width; Sky Lyrics has room for longer lines than a world-space slot. */
    static String fittedLine(Lyrics lyrics, int index, int slot, IntUnaryOperator advance, int maxWidth) {
        if (lyrics == null || lyrics.isEmpty()) return null;
        int lineIndex = index + slot;
        if (lineIndex < 0 || lineIndex >= lyrics.lines().size()) return null;
        String text = lyrics.lines().get(lineIndex).text();
        return text.isEmpty() ? null : LyricLayout.fitText(text, advance, maxWidth);
    }

    /** Cubic ease-out over 350 ms for a line appearing; immediate when motion is reduced. */
    static double appearEase(boolean reducedMotion, long elapsedNanos) {
        if (reducedMotion) return 1;
        double t = Math.min(1, Math.max(0, elapsedNanos / 3.5e8));
        return 1 - Math.pow(1 - t, 3);
    }

    /** Whether a neighbouring line is new to the stack: outside the band before, or the first frame shown. */
    static boolean fadesIn(double previousSlot, double maxSlot, boolean firstShow) {
        return firstShow || Math.abs(previousSlot) > maxSlot + 0.999;
    }

    private static int shaded(int base, int highlight, double amount) {
        amount = Math.max(0, Math.min(1, amount));
        int r = (int) (((base >> 16) & 0xFF) * (1 - amount) + ((highlight >> 16) & 0xFF) * amount);
        int g = (int) (((base >> 8) & 0xFF) * (1 - amount) + ((highlight >> 8) & 0xFF) * amount);
        int b = (int) ((base & 0xFF) * (1 - amount) + (highlight & 0xFF) * amount);
        return r << 16 | g << 8 | b;
    }

    private void spawnAccents(FxFrame f, double ax, double ay, double az, double bearing, LyricLayout.Params params,
                              double shift) {
        LyricLayout.Slot placement = LyricLayout.slot(params, 0, bearing);
        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        FxParticles.Spec spec = new FxParticles.Spec();
        for (int i = 0; i < 8; i++) {
            spec.reset();
            spec.x = ax + placement.dx() + (Math.random() - 0.5) * 0.8;
            spec.y = ay + placement.dy() + shift - 0.05;
            spec.z = az + placement.dz() + (Math.random() - 0.5) * 0.8;
            spec.vy = 0.15 + Math.random() * 0.2;
            spec.drag = 0.9; spec.life = 1.1; spec.size0 = 0.025; spec.size1 = 0;
            spec.color = rgb; spec.alpha = 0.7 * opacity.value(); spec.glow = true; spec.walls = true; spec.owner = OWNER;
            FxParticles.get().spawn(spec);
        }
    }
}
