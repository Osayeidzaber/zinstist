package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.FxShapes;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A pharaoh headdress drawn over players' heads as world-space geometry: a striped nemes, a tiered
 * khepresh or a nemes crowned with a sun disc, plus an optional broad collar. The mesh follows the
 * interpolated head yaw and pitch through the shared world-effect pipeline.
 */
public final class PharaohHeaddressModule extends ClientModule implements FxEmitter {
    private static volatile PharaohHeaddressModule active;
    private static final int MAX_PLAYERS = 16;
    private static final double MAX_DISTANCE_SQR = 64 * 64;
    private static final float UNIT = 0.0625F;
    private static final List<String> STYLES = List.of("Nemes", "Khepresh", "Sun Disc");

    /*
     * Head-space clearance geometry (1/16 block, origin at the eye-height anchor). Vanilla is drawn at
     * 0.9375 scale: the torso top sits at the neck (eyes 1.62 - head 0.46875/2 = 1.40625 world) and the
     * chestplate adds one unit of deformation (1.4648 world = -0.5625 head units; HumanoidModel.java:122).
     * The cape pivots on the neck (PlayerCapeModel.java:33) and its top edge can only rise by the
     * 10-degree roll lift, 0.293 * sin(10) = 0.0509 world (PlayerCapeModel.java:44, AvatarRenderer.java:139-141),
     * i.e. -0.68 head units. Everything below stays under those two lines.
     */
    static final double COLLAR_Y = 1.0;          // 1.5625 world: 0.098 above the chestplate top
    static final double COLLAR_RADIUS = 6.0;     // 0.375 world: clears the 0.3315 head corner by 0.0435
    static final double BACK_LAPPET_Y0 = -0.4;   // 1.475 world: above the lifted cape corner (-0.68)
    static final double BACK_LAPPET_Y1 = 7.2;
    static final double BACK_LAPPET_Z0 = -6.2;
    static final double BACK_LAPPET_Z1 = -4.1;

    /** Corner order matches {@link #FACES}: x low/high, y low/high, z low/high. */
    private static final int[] FACES = {1, 3, 7, 5, 0, 4, 6, 2, 2, 6, 7, 3, 0, 1, 5, 4, 4, 5, 7, 6, 0, 2, 3, 1};
    private static final int[] EDGES = {0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};

    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style", List.of("Nemes", "Khepresh", "Sun Disc"), "Nemes"));
    private final NumberSetting stripes = addSetting(new NumberSetting("stripes", "Stripes", 3, 9, 1, 5));
    private final BooleanSetting collar = addSetting(new BooleanSetting("collar", "Broad collar", true));
    private final NumberSetting size = addSetting(new NumberSetting("size", "Size", 0.8, 1.3, 0.05, 1.0));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.2, 1, 0.05, 0.85));
    private final ChoiceSetting colorMode = addSetting(new ChoiceSetting("color_mode", "Color", List.of("Theme", "Manual"), "Manual"));
    private final TextSetting goldColor = addSetting(EffectPalette.hexSetting("gold", "Gold", "C8A24A"));
    private final TextSetting inlayColor = addSetting(EffectPalette.hexSetting("inlay", "Inlay", "243B6B"));
    private final TextSetting gemColor = addSetting(EffectPalette.hexSetting("gem", "Gem", "5AA6E8"));
    private final BooleanSetting gemGlow = addSetting(new BooleanSetting("gem_glow", "Gem glow", true));
    private final BooleanSetting glint = addSetting(new BooleanSetting("glint", "Glint", true));
    private final BooleanSetting own = addSetting(new BooleanSetting("own", "Show in third person", true));
    private final BooleanSetting others = addSetting(new BooleanSetting("others", "Show on others", true));
    private final BooleanSetting friends = addSetting(new BooleanSetting("friends", "Others: friends only", false));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Through walls", false));

    private final double[] corners = new double[24];
    private double ox, oy, oz;
    private float cy, sy, cp, sp;
    private double unit;
    private boolean walls;

    public PharaohHeaddressModule() {
        super("pharaoh_headdress", "Pharaoh Headdress", "A striped nemes, khepresh or sun-disc crown over players' heads.", ModuleCategory.RENDER);
        active = this;
    }

    /** The enabled headdress, or null; read by the helmet-hiding mixin during the armor pass. */
    public static PharaohHeaddressModule active() {
        PharaohHeaddressModule module = active;
        return module != null && module.enabled() ? module : null;
    }

    /** The per-avatar facts the visibility rule needs, resolved from a live player by {@link #shows(Player)}. */
    public record Viewer(boolean self, boolean firstPerson, boolean alive, boolean invisible, boolean friend,
                         double distanceSqr) {
    }

    /**
     * The single visibility rule shared by the emit path and the helmet gate, so the two cannot drift:
     * a player is shown when the module is on, they are alive and visible, the own/others/friends filters
     * admit them, they are within 64 blocks of the camera and the camera is not in first person for them.
     */
    public static boolean shows(Player player) {
        PharaohHeaddressModule module = active;
        if (module == null || player == null) return false;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gameRenderer == null) return false;
        Camera camera = client.gameRenderer.getMainCamera();
        return module.shows(new Viewer(player == client.player, !camera.isDetached(), player.isAlive(),
                player.isInvisible(), FriendManager.contains(player), player.position().distanceToSqr(camera.position())));
    }

    public boolean shows(Viewer viewer) {
        if (viewer == null || !enabled()) return false;
        if (!viewer.alive() || viewer.invisible()) return false;
        if (viewer.self() && (viewer.firstPerson() || !own.value())) return false;
        if (!viewer.self() && !others.value()) return false;
        if (!viewer.self() && friends.value() && !viewer.friend()) return false;
        return viewer.distanceSqr() <= MAX_DISTANCE_SQR;
    }

    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || client.player == null) return;
        double a = opacity.value() * f.intensity;
        if (a <= 0.01) return;
        walls = throughWalls.value();
        int index = Math.max(0, STYLES.indexOf(style.value()));
        int[] palette = palette(f);
        double time = f.seconds() * (f.reducedMotion ? 0.3 : 1);
        int drawn = 0;
        for (Player player : client.level.players()) {
            if (!shows(player)) continue;
            if (++drawn > MAX_PLAYERS) break;
            draw(f, player, player.getPosition(f.partial), index, palette, a, time);
        }
    }

    /** Theme builds metal, inlay and gem from the accent; Manual uses the three hex fields. */
    private int[] palette(FxFrame f) {
        int gold, inlay, gem;
        if (colorMode.value().equals("Manual")) {
            gold = EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, goldColor.value());
            inlay = EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, inlayColor.value());
            gem = EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, gemColor.value());
        } else {
            gold = EffectBatch.mixRgb(f.accent, 0xFFFFFF, 0.35);
            inlay = EffectBatch.mixRgb(f.accent, 0x0A0F1E, 0.72);
            gem = EffectBatch.mixRgb(f.accent, 0xFFFFFF, 0.6);
        }
        int light = EffectBatch.mixRgb(gold, 0xFFFFFF, 0.55);
        return new int[]{gold, inlay, gem, light};
    }

    private void draw(FxFrame f, Player player, Vec3 pos, int look, int[] palette, double a, double time) {
        float yaw = Mth.rotLerp(f.partial, player.yHeadRotO, player.yHeadRot);
        float pitch = Mth.lerp(f.partial, player.xRotO, player.getXRot());
        cy = (float) Math.cos(Math.toRadians(yaw));
        sy = (float) Math.sin(Math.toRadians(yaw));
        cp = (float) Math.cos(Math.toRadians(pitch));
        sp = (float) Math.sin(Math.toRadians(pitch));
        unit = UNIT * size.value();
        ox = pos.x;
        oy = pos.y + player.getEyeHeight() - 0.12;
        oz = pos.z;

        int gold = palette[0], inlay = palette[1], gem = palette[2], light = palette[3];
        int bands = Math.max(3, (int) Math.round(stripes.value()));
        if (f.quality.ordinal() == 0) bands = Math.min(bands, 4);
        float edgeWidth = 1.3F;

        if (look == 1) khepresh(f, bands, gold, inlay, gem, light, edgeWidth, a, time);
        else nemes(f, bands, gold, inlay, gem, light, edgeWidth, a, look == 2, time);
        if (collar.value()) collar(f, gold, inlay, gem, a, time);
        if (glint.value() && f.quality.ordinal() > 0) glint(f, look, gold, light, a, time);
    }

    private void nemes(FxFrame f, int bands, int gold, int inlay, int gem, int light, float width, double a, boolean sunDisc, double time) {
        striped(f, -5, 7, -5, 5, 10.4, 5, bands, gold, inlay, a * 0.3, light, a * 0.75, width);
        striped(f, -5.4, 6.3, 4.1, 5.4, 7.3, 5.6, 1, gold, gold, a * 0.4, light, a * 0.95, width);
        striped(f, -6.3, -2.6, 2.0, -4.3, 7.0, 4.8, bands, gold, inlay, a * 0.28, light, a * 0.7, width);
        striped(f, 4.3, -2.6, 2.0, 6.3, 7.0, 4.8, bands, gold, inlay, a * 0.28, light, a * 0.7, width);
        striped(f, -5.8, BACK_LAPPET_Y0, BACK_LAPPET_Z0, 5.8, BACK_LAPPET_Y1, BACK_LAPPET_Z1,
                bands, gold, inlay, a * 0.28, light, a * 0.7, width);

        double[] brow = point(0, 7.7, 5.7);
        FxShapes.crystal(f, brow[0], brow[1], brow[2], 0.055 * size.value(), 0.07 * size.value(), time, gem, a * 0.9, walls);
        if (gemGlow.value()) {
            FxShapes.softDot(f, brow[0], brow[1], brow[2], 0.12 * size.value(), gem, a * 0.55, walls);
        }
        if (!sunDisc) return;

        double[] base = point(0, 10.4, 0);
        double[] disc = point(0, 16.2, 0);
        f.batch.line(base[0] - f.camX, base[1] - f.camY, base[2] - f.camZ,
                disc[0] - f.camX, disc[1] - f.camY, disc[2] - f.camZ, FxShapes.argb(gold, a * 0.8), width, walls);
        FxShapes.billboardRing(f, disc[0], disc[1], disc[2], 0.22 * size.value(), 0.035 * size.value(), gold, a * 0.9, walls);
        FxShapes.softDot(f, disc[0], disc[1], disc[2], 0.1 * size.value(), gem, a * 0.85, walls);
        double[] hornL = point(-3.4, 10.9, 2.4), hornLm = point(-4.6, 13.2, 1.4), hornLt = point(-4.2, 15.4, 0.6);
        double[] hornR = point(3.4, 10.9, 2.4), hornRm = point(4.6, 13.2, 1.4), hornRt = point(4.2, 15.4, 0.6);
        int hornColor = FxShapes.argb(gold, a * 0.8);
        f.batch.line(hornL[0] - f.camX, hornL[1] - f.camY, hornL[2] - f.camZ,
                hornLm[0] - f.camX, hornLm[1] - f.camY, hornLm[2] - f.camZ, hornColor, width, walls);
        f.batch.line(hornLm[0] - f.camX, hornLm[1] - f.camY, hornLm[2] - f.camZ,
                hornLt[0] - f.camX, hornLt[1] - f.camY, hornLt[2] - f.camZ, hornColor, width, walls);
        f.batch.line(hornR[0] - f.camX, hornR[1] - f.camY, hornR[2] - f.camZ,
                hornRm[0] - f.camX, hornRm[1] - f.camY, hornRm[2] - f.camZ, hornColor, width, walls);
        f.batch.line(hornRm[0] - f.camX, hornRm[1] - f.camY, hornRm[2] - f.camZ,
                hornRt[0] - f.camX, hornRt[1] - f.camY, hornRt[2] - f.camZ, hornColor, width, walls);
    }

    private void khepresh(FxFrame f, int bands, int gold, int inlay, int gem, int light, float width, double a, double time) {
        striped(f, -5, 6.4, -5, 5, 8.2, 5, bands, inlay, gold, a * 0.32, light, a * 0.9, width);
        striped(f, -4.7, 8.2, -4.7, 4.7, 11.2, 4.7, bands, inlay, inlay, a * 0.3, light, a * 0.8, width);
        striped(f, -3.7, 11.2, -3.7, 3.7, 13.0, 3.7, bands, inlay, inlay, a * 0.28, light, a * 0.75, width);
        striped(f, -2.4, 13.0, -2.4, 2.4, 14.2, 2.4, 1, gold, gold, a * 0.35, light, a * 0.9, width);
        for (int i = -2; i <= 2; i++) {
            double[] low = point(i * 1.5, 8.4, 4.9);
            double[] high = point(i * 0.9, 12.6, 3.4);
            f.batch.line(low[0] - f.camX, low[1] - f.camY, low[2] - f.camZ,
                    high[0] - f.camX, high[1] - f.camY, high[2] - f.camZ, FxShapes.argb(gold, a * 0.45), 1F, walls);
        }
        double[] gemAt = point(0, 7.3, 5.2);
        FxShapes.crystal(f, gemAt[0], gemAt[1], gemAt[2], 0.05 * size.value(), 0.065 * size.value(), time, gem, a * 0.9, walls);
        if (gemGlow.value()) FxShapes.softDot(f, gemAt[0], gemAt[1], gemAt[2], 0.1 * size.value(), gem, a * 0.5, walls);
    }

    private void collar(FxFrame f, int gold, int inlay, int gem, double a, double time) {
        double[] base = point(0, COLLAR_Y, 0);
        double radius = COLLAR_RADIUS * unit;
        int segments = f.quality.circleSegments();
        f.batch.ring(base[0] - f.camX, base[1] - f.camY, base[2] - f.camZ, radius, segments,
                FxShapes.argb(gold, a * 0.75), 1.4F, walls, 0, 0);
        f.batch.ring(base[0] - f.camX, base[1] - f.camY, base[2] - f.camZ, radius * 1.16, segments,
                FxShapes.argb(inlay, a * 0.55), 1.1F, walls, 0, 0);
        for (int i = 0; i < 6; i++) {
            double angle = time * 0.4 + i * Math.PI / 3;
            double dx = Math.cos(angle) * radius * 1.08, dz = Math.sin(angle) * radius * 1.08;
            FxShapes.softDot(f, base[0] + dx, base[1], base[2] + dz, 0.035 * size.value(), gem, a * 0.6, walls);
        }
    }

    /** A narrow highlight travelling across the crown face. */
    private void glint(FxFrame f, int look, int gold, int light, double a, double time) {
        double phase = (time % 3.6) / 3.6;
        double sweep = Math.sin(phase * Math.PI * 2.4);
        double envelope = Math.sin(phase * Math.PI);
        double gain = 0.65 * envelope * envelope;
        if (gain < 0.02) return;
        double x = sweep * (look == 1 ? 4.2 : 4.8);
        double top = look == 1 ? 12.4 : 10.0;
        double[] low = point(x, 6.6, 5.75);
        double[] high = point(x * 0.7, top, look == 1 ? 4.9 : 5.1);
        f.batch.line(low[0] - f.camX, low[1] - f.camY, low[2] - f.camZ,
                high[0] - f.camX, high[1] - f.camY, high[2] - f.camZ,
                FxShapes.argb(light, a * gain), 2.2F, walls);
    }

    // ---- geometry ------------------------------------------------------------------------------

    private void striped(FxFrame f, double x0, double y0, double z0, double x1, double y1, double z1,
                         int bands, int colorA, int colorB, double fillAlpha, int edge, double edgeAlpha, float width) {
        int count = Math.max(1, bands);
        double step = (y1 - y0) / count;
        for (int i = 0; i < count; i++) {
            placeBox(x0, y0 + step * i, z0, x1, y0 + step * (i + 1), z1);
            fill(f, FxShapes.argb((i & 1) == 0 ? colorA : colorB, fillAlpha));
        }
        placeBox(x0, y0, z0, x1, y1, z1);
        edges(f, FxShapes.argb(edge, edgeAlpha), width);
    }

    private void placeBox(double x0, double y0, double z0, double x1, double y1, double z1) {
        for (int c = 0; c < 8; c++) {
            double lx = (c & 1) == 0 ? x0 : x1;
            double ly = (c & 2) == 0 ? y0 : y1;
            double lz = (c & 4) == 0 ? z0 : z1;
            double[] p = point(lx, ly, lz);
            corners[c * 3] = p[0];
            corners[c * 3 + 1] = p[1];
            corners[c * 3 + 2] = p[2];
        }
    }

    private double[] point(double lx, double ly, double lz) {
        return transform(lx, ly, lz, ox, oy, oz, cy, sy, cp, sp, unit);
    }

    /** Head-space to world-space: pitch about X, then yaw about Y, then the head anchor and scale. */
    static double[] transform(double lx, double ly, double lz,
                              double ox, double oy, double oz,
                              double cy, double sy, double cp, double sp, double unit) {
        double ay = ly * cp + lz * sp;
        double az = lz * cp - ly * sp;
        double bx = lx * cy - az * sy;
        double bz = lx * sy + az * cy;
        return new double[]{ox + bx * unit, oy + ay * unit, oz + bz * unit};
    }

    private void fill(FxFrame f, int color) {
        if ((color >>> 24) == 0) return;
        double[] v = corners;
        for (int i = 0; i < FACES.length; i += 4) {
            int a = FACES[i] * 3, b = FACES[i + 1] * 3, c = FACES[i + 2] * 3, d = FACES[i + 3] * 3;
            f.batch.quad(v[a] - f.camX, v[a + 1] - f.camY, v[a + 2] - f.camZ,
                    v[b] - f.camX, v[b + 1] - f.camY, v[b + 2] - f.camZ,
                    v[c] - f.camX, v[c + 1] - f.camY, v[c + 2] - f.camZ,
                    v[d] - f.camX, v[d + 1] - f.camY, v[d + 2] - f.camZ,
                    color, color, color, color, walls);
        }
    }

    private void edges(FxFrame f, int color, float width) {
        if ((color >>> 24) == 0) return;
        double[] v = corners;
        for (int i = 0; i < EDGES.length; i += 2) {
            int a = EDGES[i] * 3, b = EDGES[i + 1] * 3;
            f.batch.line(v[a] - f.camX, v[a + 1] - f.camY, v[a + 2] - f.camZ,
                    v[b] - f.camX, v[b + 1] - f.camY, v[b + 2] - f.camZ, color, width, walls);
        }
    }
}
