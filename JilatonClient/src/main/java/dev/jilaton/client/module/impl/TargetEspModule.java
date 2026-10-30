/* Presence lifecycle (appear/vanish timing, memory window, crossfade between targets, hit flash) adapted
 * from Anubis 0.9.9.3 TargetEspModule (Horcus / 4ldenz), GPL-3.0-only. Marker styles are original Jilaton
 * geometry (see TargetEspGeometry). See ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.Motion;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.CombatTargetTracker;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.TargetEspGeometry;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class TargetEspModule extends ClientModule {
    private static final double APPEAR_SECONDS = 0.30;
    private static final double VANISH_SECONDS = 0.38;
    private static final double MAX_DISTANCE_SQR = 128 * 128;
    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Style",
            List.of(TargetEspGeometry.HALO, TargetEspGeometry.ORBIT, TargetEspGeometry.HELIX, TargetEspGeometry.BRACKETS),
            TargetEspGeometry.HALO));
    private final ChoiceSetting source = addSetting(new ChoiceSetting("source", "Target source",
            List.of("Combat", "Last hit", "Crosshair"), "Combat"));
    private final ChoiceSetting colorMode = addSetting(EffectPalette.modeSetting("color_mode", "Color"));
    private final TextSetting color = addSetting(EffectPalette.hexSetting("color", "Custom RGB hex", "F2F2F2"));
    private final BooleanSetting hitFlash = addSetting(new BooleanSetting("hit_flash", "Hit flash", true));
    private final TextSetting flashColor = addSetting(EffectPalette.hexSetting("flash_color", "Flash RGB hex", "FFFFFF"));
    private final NumberSetting scale = addSetting(new NumberSetting("scale", "Scale", 0.6, 1.8, 0.1, 1.0));
    private final NumberSetting speed = addSetting(new NumberSetting("speed", "Speed", 0.2, 2.5, 0.1, 1.0));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.2, 1.0, 0.05, 1.0));
    private final NumberSetting memory = addSetting(new NumberSetting("memory", "Memory (seconds)", 0.5, 15, 0.5, 4));
    private final BooleanSetting playersOnly = addSetting(new BooleanSetting("players_only", "Players only", false));
    private final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Through walls", true));
    private Mark current = new Mark();
    private Mark previous = new Mark();
    private Object boundLevel;
    private long lastFrame;
    private double phase;

    public TargetEspModule() {
        super("target_esp", "TargetESP", "Animated 3D marker on your combat target: halo, orbit, helix or brackets.",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable(Minecraft client) {
        reset();
    }

    private void reset() {
        current = new Mark();
        previous = new Mark();
        boundLevel = null;
        lastFrame = 0L;
    }

    /** Called once per frame during world extraction; draws nothing when no target has been seen. */
    public void emit(EffectBatch batch, Minecraft client, float partial, Vec3 camera, int accent, long now, boolean reduced) {
        if (!enabled()) return;
        if (client.level == null || client.player == null) {
            if (boundLevel != null) reset();
            return;
        }
        if (client.level != boundLevel) {
            reset();
            boundLevel = client.level;
        }
        double dt = lastFrame == 0L ? 0 : Math.min(0.1, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        if (!reduced) phase += dt * speed.value();
        retarget(pick(client, now));
        current.presence = current.entity != null ? Math.min(1, current.presence + dt / APPEAR_SECONDS) : 0;
        previous.presence = Math.max(0, previous.presence - dt / VANISH_SECONDS);
        if (previous.presence <= 0) previous.entity = null;
        current.follow(partial, client);
        previous.follow(partial, client);
        int rgb = EffectPalette.resolve(colorMode.value(), accent, color.value());
        int flashRgb = EffectPalette.resolve(EffectPalette.CUSTOM, accent, flashColor.value());
        if (previous.entity != null) draw(batch, previous, true, camera, rgb, flashRgb);
        if (current.entity != null) draw(batch, current, false, camera, rgb, flashRgb);
    }

    private void retarget(LivingEntity wanted) {
        if (wanted == current.entity) return;
        if (wanted != null && wanted == previous.entity) {
            Mark swap = current;
            current = previous;
            previous = swap;
            return;
        }
        if (current.entity != null && current.presence > 0.02) {
            Mark swap = previous;
            previous = current;
            current = swap;
        }
        current.assign(wanted);
    }

    private LivingEntity pick(Minecraft client, long now) {
        LivingEntity target = switch (source.value()) {
            case "Crosshair" -> client.crosshairPickEntity instanceof LivingEntity living ? living
                    : CombatTargetTracker.lastHit(memory.value(), now);
            case "Last hit" -> CombatTargetTracker.lastHit(memory.value(), now);
            default -> {
                LivingEntity aura = JilatonClient.moduleManager().get("kill_aura") instanceof KillAuraModule module
                        && module.enabled() ? module.currentTarget() : null;
                yield aura != null ? aura : CombatTargetTracker.lastHit(memory.value(), now);
            }
        };
        if (target == null || target == client.player || !target.isAlive() || target.isSpectator()
                || target instanceof ArmorStand || target.level() != client.level
                || target.distanceToSqr(client.player) > MAX_DISTANCE_SQR) return null;
        if (playersOnly.value() && !(target instanceof Player)) return null;
        return target;
    }

    private void draw(EffectBatch batch, Mark mark, boolean leaving, Vec3 camera, int rgb, int flashRgb) {
        double p = mark.presence;
        double visible = leaving ? p * p : 1 - Math.pow(1 - p, 3);
        double grow = leaving ? 1 + 0.3 * (1 - p) : 0.55 + 0.45 * Motion.SETTLE.apply(p);
        double flash = hitFlash.value() ? mark.hurt : 0;
        double size = scale.value() * grow * (1 + 0.1 * flash);
        double radius = (mark.width * 0.5 + 0.4) * size;
        int tint = EffectBatch.mixRgb(rgb, flashRgb, flash * 0.8);
        TargetEspGeometry.emit(batch, style.value(), new TargetEspGeometry.Marker(
                mark.x - camera.x, mark.y - camera.y, mark.z - camera.z, mark.height * Math.min(1.2, size),
                radius, phase, tint, opacity.value() * visible, flash, throughWalls.value()));
    }

    private static final class Mark {
        private LivingEntity entity;
        private double presence;
        private double x, y, z, width, height, hurt;

        void assign(LivingEntity target) {
            entity = target;
            presence = 0;
            hurt = 0;
        }

        void follow(float partial, Minecraft client) {
            Entity target = entity;
            if (target == null) return;
            if (target.isRemoved() || target.level() != client.level) {
                entity = null;
                return;
            }
            Vec3 position = target.getPosition(partial);
            x = position.x;
            y = position.y;
            z = position.z;
            width = target.getBbWidth();
            height = target.getBbHeight();
            hurt = entity.hurtTime > 0 ? Math.max(0, Math.min(1, (entity.hurtTime - partial) / 10.0)) : 0;
        }
    }
}
