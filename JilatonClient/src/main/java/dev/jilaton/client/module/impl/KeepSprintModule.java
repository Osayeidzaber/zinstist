package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ThreadLocalRandom;

/** Preserves the local sprint flag and adjusts the attack knockback slowdown. */
public final class KeepSprintModule extends ClientModule {
    private final NumberSetting motionMin;
    private final NumberSetting motionMax;
    private final NumberSetting motionWhenHurtMin;
    private final NumberSetting motionWhenHurtMax;
    private final NumberSetting hurtTimeMin;
    private final NumberSetting hurtTimeMax;
    private final NumberSetting chance;

    public KeepSprintModule() {
        super("keep_sprint", "KeepSprint", "Keeps sprint after an attack and tunes horizontal slowdown.",
                ModuleCategory.COMBAT);
        motionMin = addSetting(new NumberSetting("motion_min", "Motion minimum", 0.0, 100.0, 1.0, 100.0));
        motionMax = addSetting(new NumberSetting("motion_max", "Motion maximum", 0.0, 100.0, 1.0, 100.0));
        motionWhenHurtMin = addSetting(new NumberSetting("motion_when_hurt_min", "Hurt motion minimum",
                0.0, 100.0, 1.0, 100.0));
        motionWhenHurtMax = addSetting(new NumberSetting("motion_when_hurt_max", "Hurt motion maximum",
                0.0, 100.0, 1.0, 100.0));
        hurtTimeMin = addSetting(new NumberSetting("hurt_time_min", "Hurt-time minimum", 1.0, 10.0, 1.0, 1.0));
        hurtTimeMax = addSetting(new NumberSetting("hurt_time_max", "Hurt-time maximum", 1.0, 10.0, 1.0, 10.0));
        chance = addSetting(new NumberSetting("chance", "Motion chance", 0.0, 100.0, 1.0, 100.0));
    }

    public double knockbackMultiplier(Player player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return KeepSprintPolicy.motionFactor(player.hurtTime,
                motionMin.value(), motionMax.value(), motionWhenHurtMin.value(), motionWhenHurtMax.value(),
                (int) hurtTimeMin.value(), (int) hurtTimeMax.value(), chance.value(),
                random.nextDouble(0.0, 100.0), random.nextDouble());
    }

    public NumberSetting motionMin() {
        return motionMin;
    }

    public NumberSetting motionMax() {
        return motionMax;
    }

    public NumberSetting motionWhenHurtMin() {
        return motionWhenHurtMin;
    }

    public NumberSetting motionWhenHurtMax() {
        return motionWhenHurtMax;
    }

    public NumberSetting hurtTimeMin() {
        return hurtTimeMin;
    }

    public NumberSetting hurtTimeMax() {
        return hurtTimeMax;
    }

    public NumberSetting chance() {
        return chance;
    }
}
