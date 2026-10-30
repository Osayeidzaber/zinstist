package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
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
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * A translucent ka (soul) rises from a living entity when it pops a totem. The figure is six boxes posed
 * like a player, drawn with additive-looking glass fills and bright wire edges, then dissolved upward.
 * Events come from {@code TotemPopMixin}; nothing here changes gameplay.
 */
public final class TotemSpiritModule extends ClientModule implements FxEmitter {
    private static final int MAX_SPIRITS = 12;
    private static final double MAX_DISTANCE_SQR = 96 * 96;
    private static final float UNIT = 0.05859375F;
    private static final float HIP = 12F;
    private static final float CROUCH_LEAN = 0.45F;
    private static final float CROUCH_DROP = 3F;

    /** Offset x/y/z, then box min x/y/z and max x/y/z in model units, then the dissolve tier. */
    private static final float[][] PARTS = {
            {-1.9F, 12F, 0F, -1.5F, -12F, -1.5F, 1.5F, 0F, 1.5F, 0.0F},
            {1.9F, 12F, 0F, -1.5F, -12F, -1.5F, 1.5F, 0F, 1.5F, 0.0F},
            {0F, 12F, 0F, -3.5F, 0F, -1.75F, 3.5F, 11F, 1.75F, 0.4F},
            {-5F, 22F, 0F, -2.5F, -9F, -1.75F, 0.5F, 1.5F, 1.75F, 0.6F},
            {5F, 22F, 0F, -0.5F, -9F, -1.75F, 2.5F, 1.5F, 1.75F, 0.6F},
            {0F, 24F, 0F, -3.5F, 0F, -3.5F, 3.5F, 7F, 3.5F, 1.0F}
    };
    /** Corner indices per face, four per face. */
    private static final int[] FACES = {1, 3, 7, 5, 0, 4, 6, 2, 2, 6, 7, 3, 0, 1, 5, 4, 4, 5, 7, 6, 0, 2, 3, 1};
    private static final int[] EDGES = {0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};

    private final NumberSetting kaTime = addSetting(new NumberSetting("ka_time", "Rise duration", 0.6, 6, 0.1, 2.4));
    private final NumberSetting ascent = addSetting(new NumberSetting("ascent", "Ascent", 0, 4, 0.1, 1.6));
    private final ChoiceSetting look = addSetting(new ChoiceSetting("look", "Look", List.of("Glass", "Wire"), "Glass"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.15, 1, 0.05, 0.65));
    private final BooleanSetting kaPose = addSetting(new BooleanSetting("ka_pose", "Arms raised", true));
    private final BooleanSetting crest = addSetting(new BooleanSetting("crest", "Floating ankh", true));
    private final BooleanSetting seal = addSetting(new BooleanSetting("seal", "Ground seal", true));
    private final BooleanSetting own = addSetting(new BooleanSetting("own", "Show own pops", false));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Through walls", false));
    private final ChoiceSetting colorMode = addSetting(new ChoiceSetting("color_mode", "Color", List.of("Theme", "Manual"), "Theme"));
    private final TextSetting bodyColor = addSetting(EffectPalette.hexSetting("body_color", "Body color", "4B4030"));
    private final TextSetting auraColor = addSetting(EffectPalette.hexSetting("aura_color", "Aura color", "2B3F5C"));

    private final Spirit[] spirits = new Spirit[MAX_SPIRITS];
    private final double[] corners = new double[24];
    private final float[] pitch = new float[PARTS.length];
    private final float[] yaw = new float[PARTS.length];
    private final float[] roll = new float[PARTS.length];
    private int head;
    private int count;

    // Per-spirit transform, resolved once per draw so the box math stays allocation-free.
    private double originX, originY, originZ;
    private float leftX, leftZ, forwardX, forwardZ, unit, leanSin, leanCos, drop;
    private boolean walls;

    public TotemSpiritModule() {
        super("totem_spirit", "Totem Spirit", "A ghostly ka rises where a totem popped.", ModuleCategory.RENDER);
        for (int i = 0; i < MAX_SPIRITS; i++) spirits[i] = new Spirit();
    }

    /** Called from the packet hook for every entity that pops a totem. */
    public static void onPop(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        if (JilatonClient.moduleManager().get("totem_spirit") instanceof TotemSpiritModule module) module.capture(player);
    }

    @Override
    protected void onDisable(Minecraft client) {
        head = 0;
        count = 0;
    }

    private void capture(Player player) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled() || client.level == null || player.level() != client.level || player.isInvisible()) return;
        boolean mine = player == client.player;
        if (mine && !own.value()) return;
        if (count == MAX_SPIRITS) {
            head = (head + 1) % MAX_SPIRITS;
            count--;
        }
        Spirit spirit = spirits[(head + count) % MAX_SPIRITS];
        count++;
        spirit.x = player.getX();
        spirit.y = player.getY();
        spirit.z = player.getZ();
        spirit.bodyYaw = player.getYRot();
        spirit.headYaw = player.yHeadRot - player.getYRot();
        spirit.headPitch = player.getXRot();
        spirit.walk = player.walkAnimation.position();
        spirit.stride = Math.min(1F, player.walkAnimation.speed());
        spirit.scale = player.getScale();
        spirit.crouch = player.isCrouching();
        spirit.mine = mine;
        spirit.born = System.nanoTime();
    }

    @Override
    public void emit(FxFrame f) {
        if (f.client.level == null) return;
        long life = (long) (kaTime.value() * 1_000_000_000.0);
        while (count > 0 && f.now - spirits[head].born >= life) {
            head = (head + 1) % MAX_SPIRITS;
            count--;
        }
        if (count == 0) return;
        walls = throughWalls.value();
        int body = colorMode.value().equals("Manual") ? EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, bodyColor.value()) : f.accent;
        int aura = colorMode.value().equals("Manual") ? EffectPalette.resolve(EffectPalette.CUSTOM, f.accent, auraColor.value())
                : EffectBatch.mixRgb(f.accent, 0x6E7BFF, 0.55);
        int light = EffectBatch.mixRgb(body, 0xFFFFFF, 0.62);
        boolean glass = look.value().equals("Glass");
        for (int i = 0; i < count; i++) {
            Spirit spirit = spirits[(head + i) % MAX_SPIRITS];
            if (spirit.mine && !own.value()) continue;
            if (f.distanceSqr(spirit.x, spirit.y, spirit.z) > MAX_DISTANCE_SQR) continue;
            draw(f, spirit, Math.min(1F, (float) (f.now - spirit.born) / life), glass, body, aura, light);
        }
    }

    private void draw(FxFrame f, Spirit s, float t, boolean glass, int body, int aura, int light) {
        float alpha = (float) (opacity.value() * Math.min(1F, t / 0.06F));
        if (alpha <= 0.01F) return;
        float ka = kaPose.value() ? smooth((t - 0.08F) / 0.45F) : 0F;
        float lift = (float) (ascent.value() * ascend(t));
        if (seal.value() && t < 0.6F) groundSeal(f, s, t / 0.6F, alpha, body, aura, light);

        double yawRad = Math.toRadians(s.bodyYaw);
        float cos = (float) Math.cos(yawRad), sin = (float) Math.sin(yawRad);
        leftX = cos;
        leftZ = sin;
        forwardX = -sin;
        forwardZ = cos;
        originX = s.x;
        originY = s.y + lift;
        originZ = s.z;
        unit = UNIT * s.scale * (1F + 0.08F * t);
        float lean = s.crouch ? CROUCH_LEAN * (1F - ka) : 0F;
        leanSin = (float) Math.sin(lean);
        leanCos = (float) Math.cos(lean);
        drop = s.crouch ? CROUCH_DROP * (1F - ka) : 0F;
        pose(s, ka);

        for (int i = 0; i < PARTS.length; i++) {
            float fade = alpha * dissolve(t, PARTS[i][9]);
            if (fade < 0.01F) continue;
            place(i, 0.7F);
            fill(f, FxShapes.argb(aura, fade * (glass ? 0.16F : 0.1F)));
        }
        for (int i = 0; i < PARTS.length; i++) {
            float tier = PARTS[i][9];
            float fade = alpha * dissolve(t, tier);
            if (fade < 0.01F) continue;
            place(i, 0F);
            if (glass) fill(f, FxShapes.argb(EffectBatch.mixRgb(body, aura, (1F - tier) * 0.45F), fade * 0.32F));
            edges(f, FxShapes.argb(light, fade * (glass ? 0.8F : 0.95F)), glass ? 1.5F : 2F);
        }
        if (crest.value()) ankh(f, s, t, lift, alpha * dissolve(t, 1.1F), aura, light);
    }

    private void pose(Spirit s, float ka) {
        float swing = (float) Math.cos(s.walk * 0.6662F) * s.stride;
        float settle = 1F - ka;
        float crouchArms = s.crouch ? 0.35F : 0F;
        pitch[0] = swing * 1.4F * settle - 0.05F * ka;
        pitch[1] = -swing * 1.4F * settle - 0.15F * ka;
        pitch[2] = 0F;
        pitch[3] = lerp(ka, -swing + crouchArms, 2.75F);
        pitch[4] = lerp(ka, swing + crouchArms, 2.75F);
        pitch[5] = (float) (-s.headPitch * (Math.PI / 180.0) * settle + 0.3F * ka);
        yaw[5] = (float) (s.headYaw * (Math.PI / 180.0) * settle);
        roll[3] = 0.32F * ka;
        roll[4] = -0.32F * ka;
    }

    /** Rotates and places one box's eight corners into {@link #corners}, scaled and offset to world space. */
    private void place(int index, float inflate) {
        float[] part = PARTS[index];
        boolean torso = index >= 2;
        float cp = (float) Math.cos(pitch[index]), sp = (float) Math.sin(pitch[index]);
        float cy = (float) Math.cos(yaw[index]), sy = (float) Math.sin(yaw[index]);
        float cr = (float) Math.cos(roll[index]), sr = (float) Math.sin(roll[index]);
        for (int c = 0; c < 8; c++) {
            float lx = (c & 1) == 0 ? part[3] - inflate : part[6] + inflate;
            float ly = (c & 2) == 0 ? part[4] - inflate : part[7] + inflate;
            float lz = (c & 4) == 0 ? part[5] - inflate : part[8] + inflate;
            float ay = ly * cp + lz * sp;
            float az = lz * cp - ly * sp;
            float bx = lx * cy - az * sy;
            float bz = lx * sy + az * cy;
            float px = bx * cr - ay * sr + part[0];
            float py = bx * sr + ay * cr + part[1];
            float pz = bz + part[2];
            if (torso && leanSin != 0F) {
                float hy = py - HIP;
                float ny = hy * leanCos - pz * leanSin;
                pz = hy * leanSin + pz * leanCos;
                py = ny + HIP;
            }
            py -= drop;
            corners[c * 3] = originX + (px * leftX + pz * forwardX) * unit;
            corners[c * 3 + 1] = originY + py * unit;
            corners[c * 3 + 2] = originZ + (px * leftZ + pz * forwardZ) * unit;
        }
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
        double[] v = corners;
        for (int i = 0; i < EDGES.length; i += 2) {
            int a = EDGES[i] * 3, b = EDGES[i + 1] * 3;
            f.batch.line(v[a] - f.camX, v[a + 1] - f.camY, v[a + 2] - f.camZ,
                    v[b] - f.camX, v[b + 1] - f.camY, v[b + 2] - f.camZ, color, width, walls);
        }
    }

    /** Three expanding rings plus four small ankh marks, gone before the body finishes rising. */
    private void groundSeal(FxFrame f, Spirit s, float st, float alpha, int body, int aura, int light) {
        float open = 1F - (1F - st) * (1F - st) * (1F - st);
        double radius = (0.35F + 1.15F * open) * s.scale;
        double a = alpha * (1F - st) * 0.9F;
        double y = s.y + 0.02;
        f.batch.ring(s.x - f.camX, y - f.camY, s.z - f.camZ, radius * 0.93F, 40, FxShapes.argb(body, a), 1.6F, walls, 0, 0);
        f.batch.ring(s.x - f.camX, y - f.camY, s.z - f.camZ, radius * 1.12F, 40, FxShapes.argb(body, a * 0.25F), 1.2F, walls, 0, 0);
        f.batch.ring(s.x - f.camX, y - f.camY, s.z - f.camZ, radius * 0.55F, 32, FxShapes.argb(aura, a * 0.7F), 1.4F, walls, 0, 0);
        double base = Math.toRadians(s.bodyYaw) + open * 0.6;
        for (int i = 0; i < 4; i++) {
            double angle = base + i * Math.PI / 2;
            double px = s.x + Math.cos(angle) * radius * 0.78F;
            double pz = s.z + Math.sin(angle) * radius * 0.78F;
            FxShapes.billboardRing(f, px, y + 0.004, pz, 0.1F * s.scale, 0.03F * s.scale,
                    light, a, walls);
        }
    }

    /** Floating ankh above the head: a camera-facing loop with a stem and crossbar, blooming on spawn. */
    private void ankh(FxFrame f, Spirit s, float t, float lift, float alpha, int aura, int light) {
        if (alpha < 0.01F) return;
        float pop = Math.min(1F, t / 0.2F);
        float bloom = 1F + 1.7F * (pop - 1F) * (pop - 1F) * (pop - 1F) + 0.7F * (pop - 1F) * (pop - 1F);
        float size = 0.2F * s.scale * Math.max(0F, bloom);
        if (size <= 0.001F) return;
        double y = originY + (32F - drop) * unit + 0.35F * s.scale + lift * 0.3F;
        FxShapes.billboardRing(f, s.x, y + size * 0.45F, s.z, size * 0.34F, size * 0.1F, aura, alpha * 0.55F, walls);
        double cx = s.x - f.camX, cy = y - f.camY, cz = s.z - f.camZ;
        int stem = FxShapes.argb(light, alpha);
        f.batch.line(cx + f.upX * size * 0.12F, cy + f.upY * size * 0.12F, cz + f.upZ * size * 0.12F,
                cx - f.upX * size * 0.62F, cy - f.upY * size * 0.62F, cz - f.upZ * size * 0.62F, stem, 2F, walls);
        f.batch.line(cx - f.rightX * size * 0.42F, cy - f.rightY * size * 0.42F, cz - f.rightZ * size * 0.42F,
                cx + f.rightX * size * 0.42F, cy + f.rightY * size * 0.42F, cz + f.rightZ * size * 0.42F, stem, 2F, walls);
    }

    /** Per-part dissolve: upper parts wait longer, so the figure fades from the feet up. */
    private static float dissolve(float t, float tier) {
        return 1F - clamp01((t - 0.45F - tier * 0.22F) / 0.3F);
    }

    private static float ascend(float t) {
        float k = Math.max(0F, (t - 0.12F) / 0.88F);
        return k * k * (2.2F - 1.2F * k);
    }

    private static float smooth(float t) {
        float k = clamp01(t);
        return k * k * (3F - 2F * k);
    }

    private static float lerp(float k, float a, float b) {
        return a + (b - a) * clamp01(k);
    }

    private static float clamp01(float v) {
        return v < 0F ? 0F : Math.min(v, 1F);
    }

    private static final class Spirit {
        double x, y, z;
        float bodyYaw, headYaw, headPitch, walk, stride, scale;
        boolean crouch, mine;
        long born;
    }
}
