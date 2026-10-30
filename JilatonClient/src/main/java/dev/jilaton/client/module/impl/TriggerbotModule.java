package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.concurrent.ThreadLocalRandom;

public final class TriggerbotModule extends ClientModule {
    private final NumberSetting reactionDelay;
    private final NumberSetting reactionVariance;
    private final NumberSetting targetGrace;
    private final BooleanSetting playersOnly;
    private final BooleanSetting hostiles;
    private final BooleanSetting includeInvisible;
    private final BooleanSetting weaponsOnly;
    private final BooleanSetting holdAttack;
    private int targetId = -1;
    private long reactionReadyTick;
    private long lastSeenTick;
    private long chargedReadyTick = -1L;
    private net.minecraft.world.level.Level trackingLevel;

    public TriggerbotModule() {
        super("triggerbot", "Triggerbot", "Attacks a living target under the crosshair after a short delay.", ModuleCategory.COMBAT);
        reactionDelay = addSetting(new NumberSetting("reaction_delay", "Reaction delay (ticks)", 0.0, 10.0, 1.0, 2.0));
        reactionVariance = addSetting(new NumberSetting("reaction_variance", "Random reaction spread (ticks)",
                0.0, 5.0, 1.0, 2.0));
        targetGrace = addSetting(new NumberSetting("target_grace", "Target tracking grace (ticks)",
                0.0, 8.0, 1.0, 4.0));
        playersOnly = addSetting(new BooleanSetting("players_only", "Players only", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostile mobs", false));
        includeInvisible = addSetting(new BooleanSetting("include_invisible", "Include invisible targets", false));
        weaponsOnly = addSetting(new BooleanSetting("weapons_only", "Melee weapons only", true));
        holdAttack = addSetting(new BooleanSetting("hold_attack", "Require attack key", false));
    }

    @Override
    protected void onEnable(Minecraft client) {
        resetTarget();
        trackingLevel = client.level;
    }

    @Override
    protected void onDisable(Minecraft client) {
        resetTarget();
        trackingLevel = null;
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.level != trackingLevel) {
            resetTarget();
            trackingLevel = client.level;
        }
        if (client.player == null || client.level == null || client.gameMode == null || client.screen != null
                || client.player.isSpectator() || client.player.isUsingItem()) {
            resetTarget();
            return;
        }

        long gameTick = client.level.getGameTime();
        updateCharge(client.player, gameTick);
        ClientModule criticals = JilatonClient.moduleManager().get("criticals");
        if (criticals instanceof CriticalsModule criticalsModule && criticalsModule.hasPendingAttack()) {
            return;
        }
        if (!(client.hitResult instanceof EntityHitResult hit)) {
            if (targetId >= 0 && TriggerbotPolicy.shouldForgetTarget(gameTick, lastSeenTick,
                    (int) targetGrace.value())) {
                resetTarget();
            }
            return;
        }
        if (!(hit.getEntity() instanceof LivingEntity target) || !isValidTarget(client, target)) {
            resetTarget();
            return;
        }

        if (target.getId() != targetId) {
            targetId = target.getId();
            reactionReadyTick = TriggerbotPolicy.reactionReadyAt(gameTick, (int) reactionDelay.value(),
                    randomExtraTicks());
        }
        lastSeenTick = gameTick;
        if (!TriggerbotPolicy.mayAttack(gameTick, reactionReadyTick, chargedReadyTick,
                client.player.getAttackStrengthScale(0.0F), holdAttack.value(),
                client.options.keyAttack.isDown(), weaponsOnly.value(), isMeleeWeapon(client.player.getMainHandItem()))
                || isShieldProtected(client, target)) {
            return;
        }

        client.gameMode.attack(client.player, target);
        if (criticals instanceof CriticalsModule criticalsModule && criticalsModule.hasPendingAttack()) {
            chargedReadyTick = -1L;
            return;
        }
        client.player.swing(InteractionHand.MAIN_HAND);
        chargedReadyTick = -1L;
    }

    private void resetTarget() {
        targetId = -1;
        reactionReadyTick = 0L;
        lastSeenTick = 0L;
        chargedReadyTick = -1L;
    }

    private void updateCharge(Player player, long gameTick) {
        if (player.getAttackStrengthScale(0.0F) < 1.0F) {
            chargedReadyTick = -1L;
        } else if (chargedReadyTick < 0L) {
            chargedReadyTick = gameTick + ThreadLocalRandom.current().nextInt(2);
        }
    }

    private int randomExtraTicks() {
        int maximum = (int) reactionVariance.value();
        return maximum == 0 ? 0 : ThreadLocalRandom.current().nextInt(maximum + 1);
    }

    private boolean isValidTarget(Minecraft client, LivingEntity target) {
        Player self = client.player;
        if (target == self || !target.isAlive() || target.isSpectator()
                || (target.isInvisible() && !includeInvisible.value())) {
            return false;
        }
        if (target instanceof Player candidate) {
            return !candidate.isCreative() && PlayerTargetFilters.allowsPlayer(client, self, candidate);
        }
        return !playersOnly.value() && hostiles.value() && target instanceof Monster monster
                && !self.isAlliedTo(target) && (!(monster instanceof NeutralMob) || monster.isAggressive());
    }

    private boolean isShieldProtected(Minecraft client, LivingEntity target) {
        if (!(target instanceof Player other) || !other.isBlocking()
                || !ShieldTargetPolicy.shieldFacesAttacker(client.player.getX(), client.player.getZ(),
                other.getX(), other.getZ(), other.getYHeadRot())
                || client.player.getMainHandItem().is(ItemTags.AXES)) {
            return false;
        }
        ClientModule autoWeapon = JilatonClient.moduleManager().get("auto_weapon");
        return !(autoWeapon instanceof AutoWeaponModule module && module.canSelectShieldBreaker(client, client.player));
    }

    private static boolean isMeleeWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT) || stack.is(Items.MACE);
    }
}
