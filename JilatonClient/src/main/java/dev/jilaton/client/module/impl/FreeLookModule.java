/* Java adaptation of LiquidBounce ModuleFreeLook, Copyright (c) 2015-2026 CCBlueX.
 * GPL-3.0-or-later. See ATTRIBUTION.md for source revision and adaptation limits. */
package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.FreeLookRotation;
import dev.jilaton.client.setting.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import java.util.List;
import org.lwjgl.glfw.GLFW;

public final class FreeLookModule extends ClientModule {
    /** Degrees per second the camera closes on the body rotation after release. */
    private static final double RELEASE_DEGREES_PER_SECOND = 540.0;
    /** Camera rejoin finishes this close to the body, below one eye movement at normal sensitivity. */
    private static final float RELEASE_EPSILON_DEGREES = 0.05F;
    private final ChoiceSetting perspective = addSetting(new ChoiceSetting("perspective", "Perspective", List.of("Back", "Front"), "Back"));
    private final NumberSetting sensitivity = addSetting(new NumberSetting("sensitivity", "Camera sensitivity", 0.1, 2, 0.1, 1));
    private final BooleanSetting pitchLimit = addSetting(new BooleanSetting("limit_pitch", "Limit pitch", true));
    private final BooleanSetting hold = addSetting(new BooleanSetting("hold", "Hold key", true));
    private final FreeLookRotation rotation = new FreeLookRotation();
    private Object player;
    private CameraType previousPerspective;
    private CameraType ownedPerspective;
    private boolean releasing;
    private long releaseLastNanos;

    public FreeLookModule() {
        super("free_look", "FreeLook", "Orbit the camera without rotating your player; restores the previous perspective.", ModuleCategory.RENDER);
        setKeyCode(GLFW.GLFW_KEY_LEFT_ALT);
    }

    @Override public boolean holdKey() { return hold.value(); }

    @Override protected void onEnable(Minecraft client) {
        if (client == null || client.player == null) return;
        if (releasing && player == client.player) {
            // Re-enabled mid-rejoin: keep the original perspective so the next release still restores it.
            releasing = false;
            applyPerspective(client);
            return;
        }
        capture(client);
    }

    private void capture(Minecraft client) {
        player = client.player;
        rotation.reset(client.player.getYRot(), client.player.getXRot());
        previousPerspective = client.options.getCameraType();
        applyPerspective(client);
    }

    private void applyPerspective(Minecraft client) {
        ownedPerspective = perspective.value().equals("Front") ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK;
        client.options.setCameraType(ownedPerspective);
    }

    @Override protected void onTick(Minecraft client) {
        if (client == null) return;
        if (client.player == null || client.level == null
                || (player != null && player != client.player) || client.player.isSleeping()) {
            setEnabled(false);
            return;
        }
        if (client.screen != null || !client.isWindowActive()) { release(client); return; }
        if (player == null) capture(client);
        // Respect a deliberate vanilla perspective change rather than fighting F5 every tick.
        if (client.options.getCameraType() != ownedPerspective) { setEnabled(false); return; }
        applyPerspective(client);
    }

    @Override protected void onDisable(Minecraft client) {
        if (canRejoin(client)) {
            releasing = true;
            releaseLastNanos = System.nanoTime();
            return;
        }
        release(client);
    }

    /** Freecam and detached camera modes own the camera, so they get an immediate hand-off instead of a blend. */
    private boolean canRejoin(Minecraft client) {
        if (client == null || client.player == null || client.player != player) {
            return false;
        }
        ClientModule freecam = JilatonClient.moduleManager().get("freecam");
        return !(freecam instanceof FreecamModule camera && camera.enabled());
    }

    private void release(Minecraft client) {
        releasing = false;
        if (client != null && previousPerspective != null && client.options.getCameraType() == ownedPerspective)
            client.options.setCameraType(previousPerspective);
        player = null;
        previousPerspective = ownedPerspective = null;
    }

    public boolean active(Minecraft client) {
        if (client.player == null) return false;
        if (enabled()) {
            return player != null && client.player == player && client.screen == null
                    && client.isWindowActive() && !client.player.isSleeping();
        }
        return releasing && advanceRejoin(client);
    }

    private boolean advanceRejoin(Minecraft client) {
        if (client.player != player) {
            release(client);
            return false;
        }
        ClientModule freecam = JilatonClient.moduleManager().get("freecam");
        if (freecam instanceof FreecamModule camera && camera.enabled()) {
            release(client);
            return false;
        }
        long now = System.nanoTime();
        double seconds = Math.max(0, Math.min(0.1, (now - releaseLastNanos) / 1_000_000_000.0));
        releaseLastNanos = now;
        float remaining = rotation.stepToward(client.player.getYRot(), client.player.getXRot(),
                (float) (RELEASE_DEGREES_PER_SECOND * seconds));
        if (remaining <= RELEASE_EPSILON_DEGREES) {
            release(client);
            return false;
        }
        return true;
    }

    public boolean turn(Minecraft client, double x, double y) {
        if (!enabled() || !active(client)) return false;
        rotation.turn(x, y, sensitivity.value(), pitchLimit.value());
        return true;
    }

    public float yaw() { return rotation.yaw(); }
    public float pitch() { return rotation.pitch(); }
}
