/*
 * Adapts the generic Creative flight mode controls from LiquidBounce.
 * Copyright (c) 2015 - 2026 CCBlueX. LiquidBounce is licensed under GPL-3.0-or-later.
 * Source: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654
 */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.mixin.LocalPlayerFlightAccessor;
import dev.jilaton.client.mixin.ServerboundMovePlayerPacketAccessor;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.AABB;

public final class FlightModule extends ClientModule {
    private final NumberSetting speed;
    private final NumberSetting maxVelocity;
    private final BooleanSetting forceFlight;
    private final BooleanSetting bypassVanillaCheck;
    private LocalPlayer owner;
    private boolean previousMayfly;
    private boolean previousFlying;
    private float previousFlyingSpeed;
    private float appliedFlyingSpeed;
    private boolean forcedFlying;
    private int evaluatedOpenAirTick = Integer.MIN_VALUE;
    private boolean openAirHeartbeat;
    private boolean sendingHeartbeat;

    public FlightModule() {
        super("flight", "Flight", "Creative-style flight controls with configurable speed and velocity cap.",
                ModuleCategory.MOVEMENT);
        speed = addSetting(new NumberSetting("speed", "Flight speed", 0.1, 5.0, 0.1, 0.1));
        maxVelocity = addSetting(new NumberSetting("max_velocity", "Maximum velocity", 1.0, 20.0, 0.5, 4.0));
        forceFlight = addSetting(new BooleanSetting("force_flight", "Keep flight active", true));
        bypassVanillaCheck = addSetting(new BooleanSetting("bypass_vanilla_check", "Open-air heartbeat", true));
    }

    @Override
    protected void onEnable(Minecraft client) {
        apply(client.player);
    }

    @Override
    protected void onDisable(Minecraft client) {
        restore();
    }

    @Override
    protected void onTick(Minecraft client) {
        if (client.player == null || client.level == null || !client.player.isAlive()) {
            restore();
            return;
        }
        apply(client.player);
        sendOpenAirHeartbeat(client, client.player);
    }

    public NumberSetting speed() {
        return speed;
    }

    public NumberSetting maxVelocity() {
        return maxVelocity;
    }

    public BooleanSetting forceFlight() {
        return forceFlight;
    }

    public BooleanSetting bypassVanillaCheck() {
        return bypassVanillaCheck;
    }

    @Override
    protected void onOutgoingPacket(Packet<?> packet) {
        if (sendingHeartbeat || !bypassVanillaCheck.value() || owner == null
                || !(packet instanceof ServerboundMovePlayerPacket movement)
                || !movement.hasPosition() || !isOpenAirHeartbeatTick(owner)) {
            return;
        }
        double priorY = ((LocalPlayerFlightAccessor) owner).jilaton$getYLast();
        ((ServerboundMovePlayerPacketAccessor) movement).jilaton$setY(priorY - 0.04);
    }

    private void apply(LocalPlayer player) {
        if (player == null) {
            return;
        }
        if (owner != player) {
            restore();
            owner = player;
            Abilities abilities = player.getAbilities();
            previousMayfly = abilities.mayfly;
            previousFlying = abilities.flying;
            previousFlyingSpeed = abilities.getFlyingSpeed();
            abilities.mayfly = true;
            forcedFlying = false;
        }

        Abilities abilities = player.getAbilities();
        appliedFlyingSpeed = (float) speed.value();
        abilities.setFlyingSpeed(appliedFlyingSpeed);
        if (forceFlight.value()) {
            abilities.flying = true;
            forcedFlying = true;
        } else if (forcedFlying) {
            abilities.flying = previousFlying;
            forcedFlying = false;
        }
        player.setDeltaMovement(FlightPolicy.capVelocity(player.getDeltaMovement(), maxVelocity.value()));
    }

    private void restore() {
        LocalPlayer previousOwner = owner;
        if (previousOwner == null) {
            return;
        }
        Abilities abilities = previousOwner.getAbilities();
        if (Float.compare(abilities.getFlyingSpeed(), appliedFlyingSpeed) == 0) {
            abilities.setFlyingSpeed(previousFlyingSpeed);
        }
        if (abilities.mayfly) {
            abilities.mayfly = previousMayfly;
        }
        if (forcedFlying && abilities.flying) {
            abilities.flying = previousFlying;
        }
        owner = null;
        forcedFlying = false;
        evaluatedOpenAirTick = Integer.MIN_VALUE;
        openAirHeartbeat = false;
    }

    private void sendOpenAirHeartbeat(Minecraft client, LocalPlayer player) {
        if (!bypassVanillaCheck.value() || !isOpenAirHeartbeatTick(player)
                || client.getConnection() == null || client.getConnection().getConnection() == null) {
            return;
        }
        sendingHeartbeat = true;
        try {
            client.getConnection().getConnection().send(new ServerboundMovePlayerPacket.Pos(
                    player.getX(), player.getY(), player.getZ(), player.onGround(), player.horizontalCollision));
        } finally {
            sendingHeartbeat = false;
        }
    }

    private boolean isOpenAirHeartbeatTick(LocalPlayer player) {
        if (player.tickCount % 40 != 0) {
            return false;
        }
        if (player.tickCount != evaluatedOpenAirTick) {
            evaluatedOpenAirTick = player.tickCount;
            AABB checkBox = player.getBoundingBox().move(0.0, -0.55, 0.0);
            boolean hasBlockBelow = BlockPos.betweenClosedStream(checkBox)
                    .anyMatch(position -> !player.level().getBlockState(position).isAir());
            openAirHeartbeat = FlightPolicy.shouldSendOpenAirHeartbeat(player.tickCount, hasBlockBelow);
        }
        return openAirHeartbeat;
    }
}
