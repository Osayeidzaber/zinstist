/* Adapts recovered Anubis 0.9.9.3 misc/FreecamModule.java and its supporting hooks.
 * Horcus / 4ldenz, GPL-3.0-only. See ATTRIBUTION.md for implementation differences. */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.MovementInputEvent;
import dev.jilaton.client.render.FreecamMotion;
import dev.jilaton.client.setting.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class FreecamModule extends ClientModule {
    private static final Input IDLE = new Input(false, false, false, false, false, false, false);
    private final NumberSetting speed = addSetting(new NumberSetting("speed", "Speed", 0.1, 50, 0.1, 1));
    private final NumberSetting vertical = addSetting(new NumberSetting("vertical_speed", "Vertical speed", 0.2, 5, 0.05, 1));
    private final BooleanSetting smooth = addSetting(new BooleanSetting("smooth", "Smooth camera", true));
    private final BooleanSetting body = addSetting(new BooleanSetting("body", "Show own body", true));
    private final BooleanSetting hands = addSetting(new BooleanSetting("hands", "Show hands", true));
    private final BooleanSetting bodyKeys = addSetting(new BooleanSetting("body_keys", "Keys also move body", false));
    private final BooleanSetting keepWalking = addSetting(new BooleanSetting("keep_walking", "Keep prior walking", true));
    private final BooleanSetting wheel = addSetting(new BooleanSetting("scroll_speed", "Scroll adjusts speed", true));
    private final BooleanSetting resetWheel = addSetting(new BooleanSetting("reset_scroll_speed", "Reset scroll speed on disable", true));
    private final NumberSetting chunks = addSetting(new NumberSetting("chunks", "Client chunk distance", 2, 32, 1, 12));
    private final FreecamMotion motion = new FreecamMotion();
    private final FreecamBodyInput carried = new FreecamBodyInput();
    private LocalPlayer player;
    private ClientLevel world;
    private CameraType perspectiveBefore;
    private boolean perspectivePushed, cullBefore, flyingBefore;
    private FreecamMotion.ChunkWindow chunkWindow;
    private double temporarySpeed = 1, configuredSpeed = 1;
    private int learn;

    public FreecamModule() {
        super("freecam", "Freecam", "Fly a detached camera; optionally carry prior body walking or pass movement keys to it.", ModuleCategory.RENDER);
        setKeyCode(GLFW.GLFW_KEY_F6);
    }

    @Override public boolean persistEnabled() { return false; }
    @Override protected void onEnable(Minecraft client) { temporarySpeed = configuredSpeed = speed.value(); carried.clear(); }

    private void open(Minecraft client) {
        ClientModule look = JilatonClient.moduleManager().get("free_look");
        if (look != null) look.setEnabled(false);
        player = client.player; world = client.level;
        flyingBefore = player.getAbilities().flying;
        cullBefore = client.smartCull;
        client.smartCull = false;
        motion.reset(player.position().add(0, player.getEyeHeight(net.minecraft.world.entity.Pose.STANDING), 0), player.getYRot(), player.getXRot());
        updatePerspective(client);
        learn = 40;
        captureBody(client, true);
        followChunks(client);
    }

    @Override protected void onTick(Minecraft client) {
        if (client == null) return;
        if (player == null) {
            if (client.player == null || client.level == null) return;
            open(client);
        }
        if (client.player != player || client.level != world || !player.isAlive()) { setEnabled(false); return; }
        updatePerspective(client);
        if (speed.value() != configuredSpeed) temporarySpeed = configuredSpeed = speed.value();
        if (client.screen != null || !client.isWindowActive()) { motion.pause(); return; }
        if (learn > 0) { --learn; captureBody(client, false); }
        if (player.getAbilities().mayfly) player.getAbilities().flying = false;
        var keys = keys(client);
        motion.step((keys.forward() ? 1 : 0) - (keys.backward() ? 1 : 0),
                (keys.left() ? 1 : 0) - (keys.right() ? 1 : 0),
                (keys.jump() ? 1 : 0) - (keys.shift() ? 1 : 0),
                resetWheel.value() ? temporarySpeed : speed.value(), vertical.value(), smooth.value());
        followChunks(client);
    }

    private void captureBody(Minecraft client, boolean opening) {
        if (keepWalking.value() && (opening || bodyKeys.value())) {
            carried.learn(keys(client));
            carried.learn(player.input.keyPresses);
        } else if (player.isCrouching()) carried.learn(new Input(false, false, false, false, false, true, false));
        if (!keepWalking.value()) return;
        carried.learn(new Input(player.zza > 0.02, player.zza < -0.02, player.xxa > 0.02, player.xxa < -0.02, false, false, false));
        // Infer a carried direction when input had not caught up with existing motion at activation.
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.horizontalDistanceSqr() < 1.0E-8 || opening && (player.zza != 0 || player.xxa != 0)) return;
        double radians = Math.toRadians(player.getYRot());
        double along = velocity.x * -Math.sin(radians) + velocity.z * Math.cos(radians);
        double across = velocity.x * Math.cos(radians) + velocity.z * Math.sin(radians);
        boolean flat = Math.abs(along) >= Math.abs(across);
        carried.learn(new Input(flat && along > 0.01, flat && along < -0.01,
                !flat && across > 0.01, !flat && across < -0.01, false, false, false));
    }

    private void updatePerspective(Minecraft client) {
        if (body.value() && !perspectivePushed && client.options.getCameraType().isFirstPerson()) {
            perspectiveBefore = client.options.getCameraType();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            perspectivePushed = true;
        } else if (!body.value()) releasePerspective(client);
    }

    private void releasePerspective(Minecraft client) {
        if (perspectivePushed && client.options.getCameraType() == CameraType.THIRD_PERSON_BACK)
            client.options.setCameraType(perspectiveBefore);
        perspectivePushed = false; perspectiveBefore = null;
    }

    private void followChunks(Minecraft client) {
        var feet = player.chunkPosition();
        Vec3 position = motion.current();
        var next = FreecamMotion.chunkWindow(position.x, position.z, feet.x, feet.z, (int) chunks.value());
        if (next.equals(chunkWindow)) return;
        var cache = world.getChunkSource();
        if (chunkWindow == null || next.radius() != chunkWindow.radius()) cache.updateViewRadius(next.radius());
        cache.updateViewCenter(next.x(), next.z());
        chunkWindow = next;
        client.levelRenderer.needsUpdate();
    }

    @Override protected void onDisable(Minecraft client) {
        try {
            if (client != null) {
                releasePerspective(client);
                if (player != null) {
                    client.smartCull = cullBefore;
                    if (client.player == player) {
                        player.getAbilities().flying = flyingBefore;
                        writeInput(player, client.screen == null && client.isWindowActive() ? keys(client) : IDLE);
                        if (client.gameMode != null) client.gameMode.stopDestroyBlock();
                    }
                    if (world == client.level && world != null) {
                        if (client.player != null) {
                            var feet = client.player.chunkPosition();
                            world.getChunkSource().updateViewCenter(feet.x, feet.z);
                        }
                        world.getChunkSource().updateViewRadius(client.options.getEffectiveRenderDistance());
                        client.levelRenderer.needsUpdate();
                    }
                }
            }
        } finally {
            player = null; world = null; chunkWindow = null; learn = 0; carried.clear();
            perspectivePushed = false; perspectiveBefore = null;
            temporarySpeed = speed.value();
        }
    }

    @Override protected void onMovementInput(MovementInputEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (!live(client)) return;
        Input bodyInput = client.screen == null && client.isWindowActive() ? carried.apply(keys(client), bodyKeys.value()) : IDLE;
        event.setInput(bodyInput);
        event.setMoveVectorOverride(new Vec2((bodyInput.left() ? 1 : 0) - (bodyInput.right() ? 1 : 0),
                (bodyInput.forward() ? 1 : 0) - (bodyInput.backward() ? 1 : 0)).normalized());
    }

    public void driveBeforeSend(Minecraft client) {
        if (!live(client)) return;
        Input input = client.screen == null && client.isWindowActive() ? carried.apply(keys(client), bodyKeys.value()) : IDLE;
        writeInput(player, input);
    }

    private static void writeInput(LocalPlayer player, Input input) {
        player.input.keyPresses = input;
        ((dev.jilaton.client.mixin.ClientInputAccessor) player.input).jilaton$setMoveVector(new Vec2(
                (input.left() ? 1 : 0) - (input.right() ? 1 : 0), (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0)).normalized());
        player.setShiftKeyDown(input.shift());
    }

    private static Input keys(Minecraft client) {
        var options = client.options;
        return new Input(options.keyUp.isDown(), options.keyDown.isDown(), options.keyLeft.isDown(), options.keyRight.isDown(),
                options.keyJump.isDown(), options.keyShift.isDown(), options.keySprint.isDown());
    }

    public boolean live(Minecraft client) { return enabled() && player != null && client != null && client.player == player && client.level == world; }
    public boolean turn(Minecraft client, double x, double y) {
        if (!live(client) || client.screen != null || !client.isWindowActive()) return false;
        motion.turn(x, y); return true;
    }
    public boolean scroll(Minecraft client, double amount) {
        if (!live(client) || !wheel.value() || !Double.isFinite(amount) || amount == 0) return false;
        if (resetWheel.value()) temporarySpeed = FreecamMotion.scrollSpeed(temporarySpeed, amount, speed.minimum(), speed.maximum());
        else speed.setValue(FreecamMotion.scrollSpeed(speed.value(), amount, speed.minimum(), speed.maximum()));
        return true;
    }
    public Vec3 cameraPosition(float partial) { return motion.position(partial); }
    public float yaw() { return motion.yaw(); }
    public float pitch() { return motion.pitch(); }
    public boolean showBody() { return body.value(); }
    public boolean showHands() { return hands.value(); }
}
