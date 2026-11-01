package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.music.MusicCenter;
import dev.jilaton.client.music.MusicDisplay;
import dev.jilaton.client.music.NowPlaying;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxParticles;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Optional music-themed cosmetics that sit alongside the Music Center: a floating album-art panel, a
 * compact track label, drifting note particles, a ground visualizer and track-change notifications. The
 * visualizer and note pulse react only to the local player's own audio; with Spotify they stay static.
 */
public final class MusicVisualsModule extends ClientModule implements FxEmitter {
    private static final int OWNER = FxParticles.ownerId("music_visuals");
    private final BooleanSetting albumPanel = addSetting(new BooleanSetting("album_panel", "Floating album art", true));
    private final NumberSetting panelSize = addSetting(new NumberSetting("panel_size", "Album art size", 0.3, 1.5, 0.05, 0.6));
    private final NumberSetting panelAngle = addSetting(new NumberSetting("panel_angle", "Album art angle (°)", -90, 90, 1, -42));
    private final BooleanSetting trackLabel = addSetting(new BooleanSetting("track_label", "Track name in world", true));
    private final ChoiceSetting notes = addSetting(new ChoiceSetting("notes", "Floating symbols", List.of("Off", "Stars", "Orbs"), "Stars"));
    private final BooleanSetting visualizer = addSetting(new BooleanSetting("visualizer", "Ground visualizer (local files only)", true));
    private final BooleanSetting notify = addSetting(new BooleanSetting("notify", "Now Playing notifications", true));
    private final ChoiceSetting display = addSetting(new ChoiceSetting("display", "First-person display",
            MusicDisplay.MODES, MusicDisplay.WORLD_SPACE));
    private final BooleanSetting thirdPersonOnly = addSetting(new BooleanSetting("third_person_only", "Only in third person (Custom)", true));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.1, 1, 0.05, 0.85));
    private final FxParticles.Spec spec = new FxParticles.Spec();
    private final Random random = new Random();
    private String lastTrack = "";
    private Identifier previousArt, currentArt;
    private long artChangedAt;
    private double level;

    public MusicVisualsModule() {
        super("music_visuals", "Music Visuals", "Floating album art, track label, music particles and a ground visualizer.", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        lastTrack = "";
        previousArt = currentArt = null;
        FxParticles.get().clearOwner(OWNER);
    }

    @Override
    protected void onTick(Minecraft client) {
        NowPlaying playing = MusicCenter.get().nowPlaying();
        String key = playing.title() + "\u0000" + playing.artist();
        if (playing.hasTrack() && !key.equals(lastTrack)) {
            lastTrack = key;
            if (notify.value())
                JilatonClient.moduleManager().notifications().push("music", "Now playing  " + (playing.artist().isEmpty()
                        ? playing.title() : playing.title() + " — " + playing.artist()), true, System.nanoTime());
        }
    }

    @Override
    public void emit(FxFrame f) {
        Player player = f.client.player;
        if (player == null || f.client.options.hideGui) return;
        NowPlaying playing = MusicCenter.get().nowPlaying();
        if (!playing.hasTrack()) return;
        // Explicit modes keep the card on screen in first person; only Custom keeps the legacy third-person gate.
        boolean cameraCard = MusicDisplay.cameraAnchored(display.value(), f.firstPerson);
        boolean hideAttached = !cameraCard && thirdPersonOnly.value() && f.firstPerson;
        int rgb = EffectPalette.resolve(colorMode.value(), f.accent, color.value());
        double a = opacity.value() * f.intensity;
        double amplitude = playing.visualSyncAllowed() ? MusicCenter.get().amplitude() : 0;
        level += (amplitude - level) * Math.min(1, f.dt * 14);
        Vec3 pos = player.getPosition(f.partial);
        double t = f.seconds() * (f.reducedMotion ? 0.2 : 1);
        double viewYaw = Math.atan2(-f.forwardX, f.forwardZ);

        if (playing.artwork() != currentArt) { previousArt = currentArt; currentArt = playing.artwork(); artChangedAt = f.now; }
        if (!hideAttached && (albumPanel.value() || trackLabel.value())) {
            if (cameraCard) emitCard(f, playing, display.value(), rgb, a, t);
            else emitWorldPanel(f, player, playing, rgb, a, t, viewYaw);
        }
        if (playing.playing() && !notes.value().equals("Off")) {
            double rate = (0.8 + level * 6) * f.quality.density();
            if (random.nextDouble() < rate * f.dt) {
                double angle = random.nextDouble() * Math.PI * 2;
                spec.reset();
                spec.x = pos.x + Math.cos(angle) * 0.7; spec.y = pos.y + 1.1 + random.nextDouble() * 0.6; spec.z = pos.z + Math.sin(angle) * 0.7;
                spec.vx = Math.cos(angle) * 0.15; spec.vz = Math.sin(angle) * 0.15; spec.vy = 0.35;
                spec.drag = 0.96; spec.life = 2.2; spec.color = rgb; spec.alpha = a * 0.8; spec.owner = OWNER; spec.walls = true;
                if (notes.value().equals("Stars")) { spec.shape = FxParticles.Shape.STAR; spec.size0 = 0.08; spec.size1 = 0.02; spec.spin = 1.5; }
                else { spec.size0 = 0.05; spec.size1 = 0.01; spec.glow = true; }
                FxParticles.get().spawn(spec);
            }
        }
        if (visualizer.value() && playing.visualSyncAllowed() && playing.playing()) {
            double gx = pos.x - f.camX, gy = pos.y + 0.03 - f.camY, gz = pos.z - f.camZ;
            int bars = 48;
            for (int i = 0; i < bars; i++) {
                double angle = i * Math.PI * 2 / bars + t * 0.2;
                double wobble = 0.5 + 0.5 * Math.sin(i * 1.7 + t * 3);
                double len = 0.08 + level * (0.35 + 0.35 * wobble);
                double r0 = 0.9;
                f.batch.line(gx + Math.cos(angle) * r0, gy, gz + Math.sin(angle) * r0,
                        gx + Math.cos(angle) * (r0 + len), gy, gz + Math.sin(angle) * (r0 + len), FxShapes.argb(rgb, a * (0.35 + level)), 2F, false);
            }
        }
    }

    /**
     * Camera-pinned card for the explicit first-person modes: artwork and track text sit at fixed offsets along the
     * view basis, so looking around or walking never loses the track. Sizes come from {@link MusicDisplay}.
     */
    private void emitCard(FxFrame f, NowPlaying playing, String mode, int rgb, double a, double t) {
        MusicDisplay.Placement p = MusicDisplay.placement(mode, panelSize.value());
        double bob = Math.sin(t * 0.9) * 0.02;
        double cx = f.camX + f.forwardX * p.forward() + f.rightX * p.right() + f.upX * (p.up() + bob);
        double cy = f.camY + f.forwardY * p.forward() + f.rightY * p.right() + f.upY * (p.up() + bob);
        double cz = f.camZ + f.forwardZ * p.forward() + f.rightZ * p.right() + f.upZ * (p.up() + bob);
        double fade = Math.min(1, (f.now - artChangedAt) / 4e8);
        if (albumPanel.value() && p.showArt()) {
            float size = (float) p.artSize();
            if (previousArt != null && fade < 1)
                f.image(new FxFrame.WorldImage(cx, cy, cz, previousArt, size, (float) (a * (1 - fade)), true, 0));
            if (currentArt != null) f.image(new FxFrame.WorldImage(cx, cy, cz, currentArt, size, (float) (a * fade), true, 0));
            else FxShapes.softDot(f, cx, cy, cz, size * 0.45, rgb, a * 0.25, true);
        }
        if (!trackLabel.value()) return;
        double titleDy = MusicDisplay.titleOffset(p);
        double artistDy = titleDy - p.artistSize() * 16;
        f.text(new FxFrame.WorldText(cx + f.upX * titleDy, cy + f.upY * titleDy, cz + f.upZ * titleDy,
                List.of(new FxFrame.Run(playing.title(), FxShapes.argb(0xFFFFFF, a))), p.titleSize(), 0, 0, true, 0, false));
        if (!playing.artist().isEmpty())
            f.text(new FxFrame.WorldText(cx + f.upX * artistDy, cy + f.upY * artistDy, cz + f.upZ * artistDy,
                    List.of(new FxFrame.Run(playing.artist(), FxShapes.argb(0xA6A6A6, a))), p.artistSize(), 0, 0, true, 0, false));
        if (p.progressWidth() <= 0) return;
        double barDy = artistDy - p.artistSize() * 16;
        double w = p.progressWidth();
        double bx = f.forwardX * p.forward() + f.rightX * p.right() + f.upX * (p.up() + bob + barDy);
        double by = f.forwardY * p.forward() + f.rightY * p.right() + f.upY * (p.up() + bob + barDy);
        double bz = f.forwardZ * p.forward() + f.rightZ * p.right() + f.upZ * (p.up() + bob + barDy);
        double progress = playing.progress(f.now);
        f.batch.line(bx - f.rightX * w / 2, by - f.rightY * w / 2, bz - f.rightZ * w / 2,
                bx + f.rightX * w / 2, by + f.rightY * w / 2, bz + f.rightZ * w / 2, FxShapes.argb(0x3A3A3A, a), 2F, true);
        f.batch.line(bx - f.rightX * w / 2, by - f.rightY * w / 2, bz - f.rightZ * w / 2,
                bx + f.rightX * (w * progress - w / 2), by + f.rightY * (w * progress - w / 2),
                bz + f.rightZ * (w * progress - w / 2), FxShapes.argb(rgb, a), 2F, true);
    }

    /** The original world-anchored album panel, used in third person and by the Custom mode. */
    private void emitWorldPanel(FxFrame f, Player player, NowPlaying playing, int rgb, double a, double t, double viewYaw) {
        double angle = viewYaw + Math.toRadians(panelAngle.value());
        double px = player.getX() - Math.sin(angle) * 1.9, pz = player.getZ() + Math.cos(angle) * 1.9;
        double py = player.getY() + player.getEyeHeight() * 0.85 + Math.sin(t * 0.9) * 0.05;
        // In third person the panel can land over the crosshair; it gives way like the lyrics do.
        a *= SpatialLyricsModule.clearance(f, px, py, pz);
        if (a < 0.02) return;
        if (albumPanel.value()) {
            double fade = Math.min(1, (f.now - artChangedAt) / 4e8);
            float size = (float) panelSize.value();
            if (previousArt != null && fade < 1) f.image(new FxFrame.WorldImage(px, py, pz, previousArt, size, (float) (a * (1 - fade)), true, 0));
            if (currentArt != null) f.image(new FxFrame.WorldImage(px, py, pz, currentArt, size, (float) (a * fade), true, 0));
            else FxShapes.softDot(f, px, py, pz, size * 0.45, rgb, a * 0.25, true);
        }
        if (trackLabel.value()) {
            double below = albumPanel.value() ? panelSize.value() * 0.62 : 0;
            f.text(new FxFrame.WorldText(px, py - below, pz, List.of(new FxFrame.Run(playing.title(), FxShapes.argb(0xFFFFFF, a))),
                    0.012F, 0, 0, true, 0, false));
            if (!playing.artist().isEmpty())
                f.text(new FxFrame.WorldText(px, py - below - 0.14, pz, List.of(new FxFrame.Run(playing.artist(), FxShapes.argb(0xA6A6A6, a))),
                        0.009F, 0, 0, true, 0, false));
            double progress = playing.progress(f.now);
            double w = 0.5;
            double bx = px - f.camX, by = py - below - 0.24 - f.camY, bz = pz - f.camZ;
            f.batch.line(bx - f.rightX * w / 2, by, bz - f.rightZ * w / 2, bx + f.rightX * w / 2, by, bz + f.rightZ * w / 2,
                    FxShapes.argb(0x3A3A3A, a), 2F, true);
            f.batch.line(bx - f.rightX * w / 2, by, bz - f.rightZ * w / 2, bx + f.rightX * (w * progress - w / 2), by,
                    bz + f.rightZ * (w * progress - w / 2), FxShapes.argb(rgb, a), 2F, true);
        }
    }

    /** Whether this module draws the track title in the given view, so lyrics do not draw a second copy. */
    public boolean showsWorldTrackLabel(boolean firstPerson) {
        return enabled() && trackLabel.value()
                && MusicDisplay.showsTrackTitle(display.value(), firstPerson, thirdPersonOnly.value());
    }
}
