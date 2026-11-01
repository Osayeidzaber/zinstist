package dev.jilaton.client.dev;

import com.mojang.logging.LogUtils;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.design.Canvas;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.impl.AmbienceModule;
import dev.jilaton.client.module.impl.HudModule;
import dev.jilaton.client.render.CombatTargetTracker;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.Setting;
import dev.jilaton.client.setting.TextSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Development-only visual test harness. Active only when the JVM is started with
 * {@code -Djilaton.visualVerify}. It stages scenes in a loaded singleplayer world (a target dummy, real
 * attacks through the game mode, jumps, movement, sky times), toggles each visual module, saves in-game
 * screenshots under {@code screenshots/verify-*} and logs any module that disabled itself, then quits.
 * It never reads or synthesises OS input.
 */
public final class VisualVerifier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> VISUALS = Set.of("target_esp", "jump_circles", "trails", "hit_particles", "hit_effects",
            "damage_particles", "player_effects", "custom_sky", "ambience", "visual_engine", "explosive_highlights",
            "custom_crosshair", "nametags", "player_esp", "tracers", "item_esp", "kill_aura", "fullbright");
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int settle = -1;
    private boolean scripted;
    /** HUD preset in force before the harness touches it, restored before the client stops. */
    private String hudPresetBefore = "Minimal";
    /** Centre of the studio platform built at script start; every world scene re-enters from here. */
    private double studioX, studioY = Double.NaN, studioZ;
    private int outlineX, outlineY, outlineZ;

    private record Step(int delay, Runnable action) { }

    public static void registerIfRequested() {
        if (System.getProperty("jilaton.visualVerify") == null) return;
        VisualVerifier verifier = new VisualVerifier();
        ClientTickEvents.END_CLIENT_TICK.register(verifier::tick);
        LOGGER.info("[JilatonVerify] visual verification harness armed");
    }

    private void tick(Minecraft client) {
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (!scripted) {
            if (settle < 0) settle = 100;
            if (--settle > 0) return;
            script(client);
            scripted = true;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = steps.poll();
        if (step == null) return;
        try {
            step.action().run();
        } catch (RuntimeException failure) {
            LOGGER.error("[JilatonVerify] step failed", failure);
        }
        Step next = steps.peek();
        wait = next == null ? 0 : next.delay();
    }

    private void then(int delayTicks, Runnable action) { steps.add(new Step(delayTicks, action)); }

    /** Music-only pass (`-Djilaton.verify.music=true`): lyrics, the Music Center and the lyric pipeline log. */
    private void musicScript(Minecraft client) {
        then(10, () -> {
            command(client, "gamerule advance_time false");
            command(client, "gamerule advance_weather false");
            command(client, "weather clear");
            command(client, "time set 6000");
            client.options.hideGui = false;
            client.options.pauseOnLostFocus = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.setScreen(null);
            setting("music_center", "source", "Spotify app (no setup)");
            setting("spatial_lyrics", "mode", "Floating Side");
            setting("spatial_lyrics", "third_person_only", false);
            enable("spatial_lyrics");
            enable("now_playing");
        });
        then(0, () -> waitForLyrics(0));
        shot(40, "11-spatial-lyrics");
        then(20, () -> client.setScreen(new dev.jilaton.client.gui.MusicCenterScreen(null)));
        shot(40, "12-music-center");
        then(20, () -> client.setScreen(null));
        then(5, this::report);
        then(10, client::stop);
    }

    /**
     * Holds the scene until the asynchronous lyric lookup resolves (or ~12 s pass) so the screenshot can never
     * land mid-search. The retry re-queues itself ahead of the queued shots.
     */
    private void waitForLyrics(int waited) {
        dev.jilaton.client.music.MusicCenter center = dev.jilaton.client.music.MusicCenter.get();
        String status = center.lyricsStatus();
        boolean ready = !center.lyrics().isEmpty() || !status.startsWith("Searching");
        if (!ready && waited < 240) {
            steps.addFirst(new Step(5, () -> waitForLyrics(waited + 5)));
            return;
        }
        LOGGER.info("[JilatonVerify] lyrics wait ended after {} ticks ({})", waited, ready ? "resolved" : "timeout, " + status);
        logMusic();
    }

    private void script(Minecraft client) {
        if (Boolean.getBoolean("jilaton.verify.music")) {
            musicScript(client);
            return;
        }
        then(0, () -> {
            for (ClientModule module : JilatonClient.moduleManager().all()) if (VISUALS.contains(module.id())) module.setEnabled(false);
            if (JilatonClient.moduleManager().get("hud") instanceof HudModule hud) hudPresetBefore = hud.preset();
            command(client, "gamerule advance_time false");
            command(client, "gamerule advance_weather false");
            command(client, "gamerule spawn_mobs false");
            command(client, "weather clear");
            command(client, "difficulty easy");
            command(client, "gamemode survival @a");
            command(client, "effect give @a minecraft:resistance infinite 255 true");
            command(client, "time set 13000");
            command(client, "kill @e[type=!player]");
            client.options.hideGui = true;
            client.options.pauseOnLostFocus = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            // Build a fixed studio platform high above the terrain so framing, lighting and sky are identical
            // on every run; then stand at its center facing +Z with the target dummy three blocks ahead.
            int px = Mth.floor(client.player.getX()), pz = Mth.floor(client.player.getZ());
            int py = 140;
            command(client, "fill " + (px - 24) + " " + (py - 1) + " " + (pz - 24) + " " + (px + 24) + " " + (py - 1) + " " + (pz + 24)
                    + " minecraft:polished_deepslate");
            command(client, "fill " + (px - 24) + " " + py + " " + (pz - 24) + " " + (px + 24) + " " + (py + 6) + " " + (pz + 24) + " minecraft:air");
            studioX = px + 0.5;
            studioY = py;
            studioZ = pz + 0.5;
            command(client, "tp @a " + (px + 0.5) + " " + py + " " + (pz + 0.5) + " 0 8");
            command(client, "summon husk " + (px + 0.5) + " " + py + " " + (pz + 3.5) + " {NoAI:1b,PersistenceRequired:1b,Silent:1b,"
                    + "Rotation:[180f,0f],Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000.0}]}");
            client.player.setYRot(0);
            client.player.yRotO = 0;
            client.player.setXRot(8);
            client.player.xRotO = 8;
        });
        then(3, () -> aimAtDummy(client));
        shot(40, "01-baseline");

        // Depth probe: the same effects with through-walls off, to check the depth-tested overlay layer.
        then(5, () -> {
            CombatTargetTracker.record(dummy(client), System.nanoTime());
            setting("target_esp", "source", "Last hit");
            setting("target_esp", "memory", 15.0);
            setting("target_esp", "through_walls", false);
            setting("target_esp", "style", "Helix");
            setting("player_esp", "through_walls", false);
            setting("player_esp", "hostiles", true);
            enable("target_esp");
            enable("player_esp");
        });
        shot(30, "01b-depth-probe");
        then(2, () -> {
            setting("target_esp", "through_walls", true);
            setting("player_esp", "through_walls", true);
        });
        shot(5, "01c-walls-probe");
        then(2, () -> {
            disable("target_esp");
            disable("player_esp");
        });

        then(5, () -> {
            CombatTargetTracker.record(dummy(client), System.nanoTime());
            setting("target_esp", "source", "Last hit");
            setting("target_esp", "memory", 15.0);
            enable("target_esp");
        });
        for (String style : List.of("Halo", "Orbit", "Helix", "Brackets")) {
            then(2, () -> {
                CombatTargetTracker.record(dummy(client), System.nanoTime());
                setting("target_esp", "style", style);
            });
            shot(30, "02-target_esp-" + style.toLowerCase());
        }
        then(2, () -> disable("target_esp"));

        then(5, () -> {
            enable("hit_particles");
            enable("hit_effects");
            enable("damage_particles");
        });
        then(10, () -> attack(client));
        shot(2, "03-hit-early");
        shot(6, "03-hit-late");
        then(10, () -> attack(client));
        shot(4, "03-hit-second");
        then(30, () -> {
            disable("hit_particles");
            disable("hit_effects");
            disable("damage_particles");
        });

        then(10, () -> {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            client.player.setXRot(25);
            enable("jump_circles");
        });
        then(20, () -> client.player.jumpFromGround());
        shot(6, "04-jump-air");
        shot(10, "04-jump-landed");
        then(30, () -> disable("jump_circles"));

        then(5, () -> enable("trails"));
        for (int i = 0; i < 24; i++) then(1, () -> client.player.setDeltaMovement(0.28, client.player.getDeltaMovement().y, 0));
        shot(1, "05-trails");
        then(10, () -> disable("trails"));

        then(5, () -> {
            command(client, "time set 18000");
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            client.player.setYRot(0);
            client.player.yRotO = 0;
            client.player.setXRot(2);
            client.player.xRotO = 2;
            setting("player_effects", "halo", "Crown");
            setting("player_effects", "halo_tilt", 18.0);
            setting("player_effects", "relics", "Blades");
            setting("player_effects", "relic_count", 5.0);
            setting("player_effects", "orbit_radius", 1.05);
            setting("player_effects", "orbit_speed", 0.7);
            setting("player_effects", "wings", "Dark Angel");
            setting("player_effects", "wing_size", 1.4);
            setting("player_effects", "wing_spread", 1.1);
            setting("player_effects", "companion", "Wisp");
            setting("player_effects", "aura", "Radiance");
            setting("player_effects", "opacity", 1.0);
            enable("player_effects");
        });
        shot(40, "06-player_effects");
        then(5, () -> disable("player_effects"));

        then(5, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.player.setXRot(-18);
            client.player.xRotO = -18;
            setting("custom_sky", "follow_day", false);
            setting("custom_sky", "color_strength", 1.0);
            setting("custom_sky", "star_brightness", 1.0);
            setting("custom_sky", "dome", true);
            setting("custom_sky", "dome_brightness", 1.0);
            setting("custom_sky", "clouds", 0.6);
            setting("custom_sky", "hide_clouds", true);
            setting("custom_sky", "fog_tint", 0.75);
            // The pack replaces every theme layer, so theme shots must run with it off or the filename lies.
            setting("custom_sky", "dramatic_skys", false);
            enable("custom_sky");
        });
        ChoiceSetting themes = choice("custom_sky", "theme");
        List<String> themeNames = themes == null ? List.of("default") : themes.choices();
        for (String time : List.of("13000", "18000", "23000")) {
            then(2, () -> command(client, "time set " + time));
            for (String theme : themeNames) {
                then(2, () -> {
                    if (themes != null) themes.setValue(theme);
                    logSky("theme=" + theme + " @" + time);
                });
                shot(25, "07-sky-" + time + "-" + theme.toLowerCase().replace(' ', '_'));
            }
        }
        then(2, () -> setting("custom_sky", "dramatic_skys", true));
        for (String time : List.of("13000", "18000", "23000")) {
            then(2, () -> command(client, "time set " + time));
            then(2, () -> logSky("pack @" + time));
            shot(25, "07p-sky-" + time + "-dramatic_skys");
        }
        then(5, () -> disable("custom_sky"));

        then(5, () -> {
            command(client, "time set 18000");
            client.player.setXRot(8);
            enable("ambience");
        });
        shot(60, "08-ambience");
        then(5, () -> disable("ambience"));

        then(5, () -> {
            command(client, "time set 6000");
            command(client, "execute as @a at @s rotated 0 0 run summon end_crystal ^1.5 ^ ^4 {ShowBottom:0b}");
            enable("explosive_highlights");
        });
        shot(40, "09-explosive_highlights");
        then(5, () -> disable("explosive_highlights"));

        then(5, () -> {
            client.options.hideGui = false;
            enable("custom_crosshair");
        });
        shot(20, "10-crosshair");
        then(5, () -> {
            client.options.hideGui = false;
            client.setScreen(null);
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.player.setXRot(8);
            setting("music_center", "source", "Spotify app (no setup)");
            setting("spatial_lyrics", "mode", "Curved Arc");
            setting("spatial_lyrics", "third_person_only", false);
            enable("spatial_lyrics");
            enable("now_playing");
        });
        then(220, () -> logMusic());
        shot(40, "11-spatial-lyrics");
        // Screenshot readback is asynchronous; leave the world on screen long enough that it cannot bleed into the next shot.
        then(20, () -> client.setScreen(new dev.jilaton.client.gui.MusicCenterScreen(null)));
        shot(40, "12-music-center");
        then(5, () -> client.setScreen(null));
        then(5, () -> {
            disable("spatial_lyrics");
            disable("now_playing");
            client.options.hideGui = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            setting("custom_sky", "theme", "Ringed Planet");
            setting("player_effects", "halo", "Crown");
            setting("player_effects", "wings", "Dark Angel");
            setting("player_effects", "relics", "Blades");
            setting("player_effects", "aura", "Radiance");
            setting("player_effects", "companion", "Wisp");
            enable("player_effects");
        });
        then(5, () -> client.setScreen(new dev.jilaton.client.gui.VisualStudioScreen(null)));
        shot(60, "13-visual_studio");
        then(5, () -> client.setScreen(null));
        then(5, () -> disable("player_effects"));

        // ClickGUI: the shipping board screen, each state restored from the config the GUI itself persists.
        then(5, () -> {
            client.options.hideGui = false;
            client.setScreen(null);
            createProfiles();
            openGui("MODULES", "Combat", "");
        });
        shot(12, "14-clickgui-combat");
        then(2, () -> openGui("MODULES", "Visuals", ""));
        shot(12, "15-clickgui-visuals");
        then(2, () -> openGui("MODULES", "Combat", firstCombatWithSettings()));
        shot(12, "16-clickgui-settings-open");
        then(2, () -> openGui("THEME", "Combat", ""));
        shot(12, "17-clickgui-theme");
        then(2, () -> openGui("CONFIGS", "Combat", ""));
        shot(12, "18-clickgui-configs");

        // The switch is the only part of a card that may toggle; the body opens the settings drawer.
        then(2, () -> {
            openGui("MODULES", "Combat", "");
            Screen gui = client.screen;
            if (gui == null) return;
            Set<String> before = enabledIds();
            clickAt(gui, CARD_SWITCH_X, CARD_SWITCH_Y);
            Set<String> after = enabledIds();
            Set<String> toggled = new java.util.HashSet<>(after);
            toggled.removeAll(before);
            Set<String> untoggled = new java.util.HashSet<>(before);
            untoggled.removeAll(after);
            LOGGER.info("[JilatonVerify] switch click: toggled={} untoggled={} others={}",
                    toggled, untoggled, after.size() - toggled.size());
            if (toggled.size() + untoggled.size() != 1) {
                LOGGER.error("[JilatonVerify] switch click did not toggle exactly one module");
            }
        });
        shot(12, "18b-clickgui-switch");
        then(2, () -> {
            Screen gui = client.screen;
            if (gui == null) return;
            Set<String> before = enabledIds();
            clickAt(gui, CARD_BODY_X, CARD_BODY_Y);
            if (!before.equals(enabledIds())) {
                LOGGER.error("[JilatonVerify] clicking the card body toggled a module");
            } else {
                LOGGER.info("[JilatonVerify] card body click enabled nothing (drawer path)");
            }
        });
        shot(12, "18c-clickgui-drawer");
        then(5, () -> {
            client.setScreen(null);
            deleteProfiles();
        });

        then(1, () -> studio(client));
        // HUD target card: a live mob in front of the camera, Minimal then the Full preset. Night keeps the
        // summoned zombie from burning in the sun, which would otherwise fill the frame with fire particles.
        then(5, () -> {
            client.options.hideGui = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "kill @e[type=minecraft:husk]");
            command(client, "kill @e[type=minecraft:zombie]");
            command(client, "time set 13000");
            client.player.setYRot(0);
            client.player.yRotO = 0;
            client.player.setXRot(0);
            client.player.xRotO = 0;
            summonAhead(client, "zombie");
        });
        then(6, () -> aimAt(client, nearestZombie(client)));
        then(4, () -> {
            LivingEntity zombie = nearestZombie(client);
            if (zombie != null) CombatTargetTracker.record(zombie, System.nanoTime());
        });
        shot(10, "19-hud-minimal");
        then(4, () -> preset("Full"));
        shot(12, "20-hud-full");
        then(4, () -> preset("Minimal"));

        then(1, () -> studio(client));
        // Block Outline: a two-block wall gives the crosshair a deterministic block, then the glide.
        then(5, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "kill @e[type=minecraft:zombie]");
            client.player.setXRot(0);
            client.player.xRotO = 0;
            double yaw = Math.toRadians(client.player.getYRot());
            int bx = Mth.floor(client.player.getX() - Math.sin(yaw) * 3);
            int bz = Mth.floor(client.player.getZ() + Math.cos(yaw) * 3);
            int by = Mth.floor(client.player.getY()) + 1;
            command(client, "setblock " + bx + " " + by + " " + bz + " minecraft:polished_deepslate");
            command(client, "setblock " + bx + " " + (by + 1) + " " + bz + " minecraft:polished_deepslate");
            outlineX = bx;
            outlineY = by;
            outlineZ = bz;
            enable("block_outline");
        });
        shot(10, "21-block_outline");
        then(2, () -> {
            client.player.setXRot(-25);
            client.player.xRotO = -25;
        });
        shot(3, "21b-block_outline-glide");
        then(3, () -> command(client, "execute as @p at @s run tp @s ~1.4 ~ ~"));
        then(3, () -> aimAtBlock(client, outlineX, outlineY, outlineZ));
        shot(8, "21c-block_outline-oblique");
        then(3, () -> disable("block_outline"));

        then(1, () -> studio(client));
        // Low fire: the local player burns for the overlay; cleared again before the next scene.
        then(5, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            client.player.setRemainingFireTicks(400);
            client.player.setSharedFlagOnFire(true);
            enable("low_fire");
        });
        shot(10, "22-low_fire");
        then(3, () -> {
            client.player.clearFire();
            client.player.setSharedFlagOnFire(false);
            disable("low_fire");
        });

        then(1, () -> studio(client));
        // Radiance: soft shell on the player and the mob standing in front, seen from behind.
        then(5, () -> {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            client.player.setYRot(90);
            client.player.yRotO = 90;
            client.player.setXRot(10);
            client.player.xRotO = 10;
            setting("radiance", "color_mode", "Custom");
            setting("radiance", "color", "A98CFF");
            setting("radiance", "strength", 0.34);
            setting("radiance", "thickness", 0.24);
            setting("radiance", "pulse", false);
            // On the camera axis the player would hide the zombie; step it sideways so both auras read.
            summonAhead(client, "zombie", 1.6);
            enable("radiance");
        });
        shot(20, "23-radiance");
        then(3, () -> disable("radiance"));

        then(1, () -> studio(client));
        // Afterglow: a fresh mob dies in front of the camera and dissolves.
        then(5, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "kill @e[type=minecraft:zombie]");
            summonAhead(client, "zombie");
        });
        then(8, () -> {
            setting("afterglow", "color_mode", "Custom");
            setting("afterglow", "color", "C9A7FF");
            setting("afterglow", "count", 48);
            setting("afterglow", "glow", true);
            enable("afterglow");
        });
        then(4, () -> command(client, "kill @e[type=minecraft:zombie]"));
        shot(10, "24-afterglow");
        shot(8, "24b-afterglow-late");
        then(3, () -> disable("afterglow"));

        then(1, () -> studio(client));
        // Weather: the three selectable modes, one after the other.
        then(5, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            setting("weather", "mode", "Glassy Rain");
            enable("weather");
        });
        shot(30, "25-weather-rain");
        then(2, () -> setting("weather", "mode", "Settling Snow"));
        shot(30, "25b-weather-snow");
        then(2, () -> setting("weather", "mode", "Ember Shower"));
        shot(30, "25c-weather-ember");
        then(3, () -> {
            setting("weather", "mode", "Glassy Rain");
            disable("weather");
        });

        then(1, () -> studio(client));
        // Fullbright: the lightmap gamma override at midnight. The saved gamma option must be unchanged
        // before and after, which separates this from the old implementation that wrote options.gamma().
        then(5, () -> {
            client.options.hideGui = true;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "time set 18000");
            command(client, "weather clear");
            setting("fullbright", "gamma", 8.0);
            disable("fullbright");
            LOGGER.info("[JilatonVerify] fullbright scene: gamma option before={}", client.options.gamma().get());
        });
        shot(12, "26-fullbright-off");
        then(3, () -> enable("fullbright"));
        shot(8, "26b-fullbright-on");
        then(3, () -> {
            LOGGER.info("[JilatonVerify] fullbright scene: gamma option after={}", client.options.gamma().get());
            disable("fullbright");
            command(client, "time set 13000");
        });

        then(1, () -> studio(client));
        // M41 swing: the Anubis pose captured mid-arc (shot 2 ticks into a ~6 tick swing).
        then(5, () -> {
            // F1 (hideGui) also skips the first-person hand pass, which is exactly what this scene must show.
            client.options.hideGui = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "time set 6000");
            command(client, "weather clear");
            command(client, "item replace entity @p weapon.mainhand with minecraft:diamond_sword");
            setting("swing_animations", "mode", "Slash");
            setting("swing_animations", "scale", 1.2);
            setting("swing_animations", "tempo", 1.2);
            enable("swing_animations");
        });
        // Let the vanilla equip animation raise the freshly replaced sword before swinging.
        then(14, () -> client.player.swing(InteractionHand.MAIN_HAND));
        shot(2, "27-swing-slash");
        then(2, () -> setting("swing_animations", "mode", "Spin"));
        then(2, () -> client.player.swing(InteractionHand.MAIN_HAND));
        shot(2, "27b-swing-spin");
        then(3, () -> disable("swing_animations"));

        then(1, () -> studio(client));
        // M41 terrain shockwave: Custom HitEffects with only the world wave, seen from behind.
        then(5, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "kill @e[type=minecraft:zombie]");
            double yaw = Math.toRadians(client.player.getYRot());
            command(client, "summon minecraft:zombie " + (client.player.getX() - Math.sin(yaw) * 2.2) + " "
                    + client.player.getY() + " " + (client.player.getZ() + Math.cos(yaw) * 2.2)
                    + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
            setting("hit_effects", "preset", "Custom");
            setting("hit_effects", "trigger", "Attack attempts");
            setting("hit_effects", "style_world_wave", true);
            setting("hit_effects", "world_wave_radius", 21.0);
            setting("hit_effects", "style_sparks", false);
            setting("hit_effects", "style_ripple", false);
            enable("hit_effects");
        });
        then(3, () -> aimAt(client, nearestZombie(client)));
        then(2, () -> {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            client.player.setXRot(25);
            client.player.xRotO = 25;
            LivingEntity zombie = nearestZombie(client);
            if (zombie != null && client.gameMode != null) client.gameMode.attack(client.player, zombie);
            client.player.swing(InteractionHand.MAIN_HAND);
        });
        shot(6, "28-terrain-wave");
        shot(4, "28b-terrain-wave-wide");
        then(3, () -> {
            disable("hit_effects");
            command(client, "kill @e[type=minecraft:zombie]");
            setting("hit_effects", "preset", "Minimal");
            setting("hit_effects", "style_world_wave", false);
        });

        then(1, () -> studio(client));
        // M41 ambience precedence: Theme colour with no owner, then CustomSky owns it.
        then(5, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "time set 13000");
            // Fireflies glow and are drawn large: dim Dust in the near-white client accent barely shows on the dark
            // plaza, so the colour hand-over to the sky accent could not be judged from the screenshots.
            setting("ambience", "style", "Fireflies");
            setting("ambience", "density", 1.0);
            setting("ambience", "radius", 10.0);
            setting("ambience", "size", 2.5);
            setting("ambience", "opacity", 1.0);
            setting("ambience", "color_mode", "Theme");
            disable("custom_sky");
            enable("ambience");
            logAmbience("29-theme");
        });
        shot(40, "29-ambience-theme");
        then(2, () -> {
            enable("custom_sky");
            logAmbience("29b-sky");
        });
        shot(40, "29b-ambience-sky");
        then(3, () -> {
            disable("ambience");
            disable("custom_sky");
            command(client, "time set 13000");
        });

        // Pharaoh headdress over a worn helmet, seen from the front: the helmet must vanish under the
        // headdress while the module is on and come back the moment it is switched off.
        then(1, () -> studio(client));
        then(5, () -> {
            command(client, "time set 6000");
            command(client, "item replace entity @p armor.head with minecraft:iron_helmet");
            client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            setting("pharaoh_headdress", "style", "Nemes");
            setting("pharaoh_headdress", "color_mode", "Manual");
            enable("pharaoh_headdress");
        });
        shot(20, "30-pharaoh-helmet-on");
        then(2, () -> disable("pharaoh_headdress"));
        shot(10, "30b-pharaoh-helmet-off");
        then(3, () -> {
            command(client, "item replace entity @p armor.head with minecraft:air");
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });

        // Screenshot [Copy]: drive the real custom-click handler on an earlier shot; the feedback line and the
        // OS clipboard (checked outside the game) are the evidence.
        then(5, () -> {
            client.gui.getChat().clearMessages(false);
            dev.jilaton.client.screenshot.ScreenshotChatActions.handleCopy(client,
                    new net.minecraft.network.chat.ClickEvent.Custom(
                            dev.jilaton.client.screenshot.ScreenshotChatActions.COPY_ID,
                            java.util.Optional.of(net.minecraft.nbt.StringTag.valueOf("verify-17-clickgui-theme.png"))));
        });
        // The "Copied screenshot" / "Copy failed" line lands in chat and therefore in latest.log.
        shot(60, "31-screenshot-copy-chat");

        // FreeLook + KillAura: the camera stays where FreeLook holds it while the body turns 90 degrees to a
        // target. Every tick's body yaw/pitch and camera yaw are logged so the motion profile can be checked.
        then(1, () -> studio(client));
        then(5, () -> {
            command(client, "kill @e[type=minecraft:zombie]");
            command(client, "time set 18000");
            command(client, "summon minecraft:zombie " + (studioX + 3.0) + " " + studioY + " " + studioZ
                    + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
            // The hold-key manager revokes a hold module every tick the physical key is up, so the
            // verifier cannot keep FreeLook alive by enabling it alone.
            setting("free_look", "hold", false);
            setting("kill_aura", "require_attack_key", false);
            setting("kill_aura", "turn_speed", 12.0);
        });
        then(3, () -> {
            client.player.setYRot(0);
            client.player.setXRot(0);
            enable("free_look");
            enable("kill_aura");
        });
        for (int i = 0; i < 24; i++) {
            int tick = i;
            then(1, () -> {
                float camera = JilatonClient.moduleManager().get("free_look") instanceof dev.jilaton.client.module.impl.FreeLookModule look
                        ? look.yaw() : Float.NaN;
                LOGGER.info("[JilatonVerify] aura-freelook t={} bodyYaw={} pitch={} headYaw={} cameraYaw={}", tick,
                        client.player.getYRot(), client.player.getXRot(), client.player.getYHeadRot(), camera);
            });
        }
        shot(1, "32-freelook-killaura");
        then(3, () -> {
            disable("kill_aura");
            disable("free_look");
            command(client, "kill @e[type=minecraft:zombie]");
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });

        // Target card pinned beside the opponent: the card must sit left of the zombie's body without covering
        // it, at normal range (33) and point-blank (33b), where the projected body is several times wider.
        then(1, () -> studio(client));
        then(5, () -> {
            client.options.hideGui = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "time set 13000");
            client.player.setYRot(0);
            client.player.setXRot(0);
            setting("hud", "target_placement", "Follow target");
            summonAhead(client, "zombie", 2.5);
        });
        then(6, () -> aimAt(client, nearestZombie(client)));
        then(4, () -> {
            LivingEntity zombie = nearestZombie(client);
            if (zombie != null) CombatTargetTracker.record(zombie, System.nanoTime());
        });
        shot(10, "33-target-follow");
        then(2, () -> {
            LivingEntity zombie = nearestZombie(client);
            if (zombie != null) {
                net.minecraft.world.phys.Vec3 look = client.player.getLookAngle();
                double flat = Math.max(1e-6, Math.hypot(look.x, look.z));
                // A server-side teleport: moving the client copy alone is overwritten by the next position packet.
                command(client, "tp @e[type=minecraft:zombie,limit=1,sort=nearest] " + (client.player.getX() + look.x / flat * 1.6)
                        + " " + zombie.getY() + " " + (client.player.getZ() + look.z / flat * 1.6));
                CombatTargetTracker.record(zombie, System.nanoTime());
            }
        });
        then(4, () -> aimAt(client, nearestZombie(client)));
        shot(10, "33b-target-follow-near");
        then(3, () -> {
            setting("hud", "target_placement", "HUD");
            command(client, "kill @e[type=minecraft:zombie]");
        });

        // AutoArmor from the hotbar: a hotbar chestplate must equip through the inventory container path just
        // like a main-inventory piece, and the server must agree (items are read back after the round trip).
        then(2, () -> {
            command(client, "item replace entity @p armor.chest with minecraft:air");
            command(client, "item replace entity @p armor.feet with minecraft:air");
            command(client, "item replace entity @p hotbar.3 with minecraft:diamond_chestplate");
            command(client, "item replace entity @p inventory.11 with minecraft:iron_boots");
        });
        then(5, () -> enable("auto_armor"));
        then(30, () -> {
            LOGGER.info("[JilatonVerify] auto_armor chest={} feet={} hotbar3={} inv20={}",
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem()),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).getItem()),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(client.player.getInventory().getItem(3).getItem()),
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(client.player.getInventory().getItem(20).getItem()));
            disable("auto_armor");
            command(client, "item replace entity @p armor.chest with minecraft:air");
            command(client, "item replace entity @p armor.feet with minecraft:air");
        });

        then(5, () -> {
            preset(hudPresetBefore);
            client.options.hideGui = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
        });
        then(5, () -> report());
        then(10, client::stop);
    }

    /** Logs what the music pipeline actually resolved, so a screenshot can be interpreted afterwards. */
    private void logMusic() {
        dev.jilaton.client.music.MusicCenter center = dev.jilaton.client.music.MusicCenter.get();
        dev.jilaton.client.music.NowPlaying playing = center.nowPlaying();
        dev.jilaton.client.music.Lyrics lyrics = center.lyrics();
        int index = lyrics.isEmpty() ? -1 : lyrics.indexAt(playing.positionNow(System.nanoTime()));
        LOGGER.info("[JilatonVerify] music source={} track='{}' artist='{}' playing={} pos={}ms dur={}ms lines={} active={} status='{}'",
                center.source(), playing.title(), playing.artist(), playing.playing(), playing.positionMs(),
                playing.durationMs(), lyrics.lines().size(), index, center.lyricsStatus());
        if (index >= 0) LOGGER.info("[JilatonVerify] active lyric: {}", lyrics.lines().get(index).text());
    }

    private void attack(Minecraft client) {
        Entity target = dummy(client);
        if (target == null || client.gameMode == null) {
            LOGGER.warn("[JilatonVerify] no dummy to attack");
            return;
        }
        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
    }

    private static LivingEntity dummy(Minecraft client) {
        LivingEntity best = null;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Husk husk && (best == null || husk.distanceToSqr(client.player) < best.distanceToSqr(client.player))) best = husk;
        }
        return best;
    }

    /** Points the camera at the dummy's chest so composition is deterministic between runs. */
    private static void aimAtDummy(Minecraft client) {
        aimAt(client, dummy(client));
    }

    private static void aimAt(Minecraft client, LivingEntity target) {
        if (target == null) return;
        double tx = target.getX(), ty = target.getY() + target.getBbHeight() * 0.62, tz = target.getZ();
        double ex = client.player.getX(), ey = client.player.getEyeY(), ez = client.player.getZ();
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(0.001, flat)));
        client.player.setYRot(yaw);
        client.player.setXRot(pitch);
        client.player.yRotO = yaw;
        client.player.xRotO = pitch;
    }

    /** Aims at the centre of a specific block, so an oblique stance still selects the intended block. */
    private static void aimAtBlock(Minecraft client, int bx, int by, int bz) {
        double dx = bx + 0.5 - client.player.getX();
        double dy = by + 0.5 - client.player.getEyeY();
        double dz = bz + 0.5 - client.player.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(0.001, flat)));
        client.player.setYRot(yaw);
        client.player.setXRot(pitch);
        client.player.yRotO = yaw;
        client.player.xRotO = pitch;
    }

    /** Summons a mob roughly three blocks along the player's current facing, in view of a level camera. */
    private static void summonAhead(Minecraft client, String type) {
        summonAhead(client, type, 0.0);
    }

    /** As {@link #summonAhead(Minecraft, String)}, shifted sideways so it does not sit behind the player. */
    private static void summonAhead(Minecraft client, String type, double lateral) {
        double yaw = Math.toRadians(client.player.getYRot());
        double x = client.player.getX() - Math.sin(yaw) * 3.0 + Math.cos(yaw) * lateral;
        double z = client.player.getZ() + Math.cos(yaw) * 3.0 + Math.sin(yaw) * lateral;
        command(client, "summon minecraft:" + type + " " + x + " " + client.player.getY() + " " + z
                + " {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
    }

    private static LivingEntity nearestZombie(Minecraft client) {
        LivingEntity best = null;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Zombie zombie && (best == null || zombie.distanceToSqr(client.player) < best.distanceToSqr(client.player))) {
                best = zombie;
            }
        }
        return best;
    }

    private void preset(String value) {
        if (JilatonClient.moduleManager().get("hud") instanceof HudModule hud) {
            hud.selectPreset(value);
            LOGGER.info("[JilatonVerify] HUD preset -> {}", value);
        } else {
            LOGGER.warn("[JilatonVerify] hud module is not a HudModule");
        }
    }

    /** First card of the 2-column module list while no drawer is open, in Canvas board coordinates. */
    private static final double CARD_SWITCH_X = 705, CARD_SWITCH_Y = 201;
    private static final double CARD_BODY_X = 360, CARD_BODY_Y = 190;

    /** Reopens the shipping ClickGUI with a state restored from the config the GUI itself persists. */
    private static void openGui(String page, String category, String drawer) {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(null); // the closed GUI persists its own state in removed(), so close before writing
        JilatonClient.clientConfig().setGuiPage(page);
        JilatonClient.clientConfig().setGuiCategory(category);
        JilatonClient.clientConfig().setGuiExpandedModule(drawer);
        JilatonClient.clientConfig().setCategoryScroll(category, 0);
        JilatonClient.openClickGui(null);
    }

    private static String firstCombatWithSettings() {
        for (ClientModule module : JilatonClient.moduleManager().all()) {
            if (module.category() == ModuleCategory.COMBAT && !module.settings().isEmpty()) return module.id();
        }
        return "";
    }

    private static Set<String> enabledIds() {
        Set<String> out = new java.util.HashSet<>();
        for (ClientModule module : JilatonClient.moduleManager().all()) if (module.enabled()) out.add(module.id());
        return out;
    }

    /** Sends a real click through the screen at a point given in the 1280x720 board space. */
    private static void clickAt(Screen screen, double boardX, double boardY) {
        double scale = Math.min(screen.width / Canvas.W, screen.height / Canvas.H);
        double x = (screen.width - Canvas.W * scale) / 2 + boardX * scale;
        double y = (screen.height - Canvas.H * scale) / 2 + boardY * scale;
        screen.mouseMoved(x, y);
        screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
    }

    /** Configs page needs rows to show, so three throwaway profiles are written and removed again. */    private static void createProfiles() {
        try {
            for (String name : List.of("verify-a", "verify-b", "verify-c")) {
                JilatonClient.clientConfig().saveProfile(name, JilatonClient.moduleManager());
            }
        } catch (IOException failure) {
            LOGGER.warn("[JilatonVerify] could not create profiles: {}", failure.getMessage());
        }
    }

    private static void deleteProfiles() {
        try {
            for (String name : List.of("verify-a", "verify-b", "verify-c")) {
                JilatonClient.clientConfig().deleteProfile(name);
            }
        } catch (IOException failure) {
            LOGGER.warn("[JilatonVerify] could not delete profiles: {}", failure.getMessage());
        }
    }

    private void shot(int delayTicks, String name) {
        // HUD and in-world text effects both bail out when hideGui is set, so scenes that exercise them keep it off.
        then(Math.max(0, delayTicks - 1), () -> Minecraft.getInstance().options.hideGui = !keepsHud(name));
        then(1, () -> {
            Minecraft client = Minecraft.getInstance();
            Screenshot.grab(client.gameDirectory, "verify-" + name + ".png", client.getMainRenderTarget(), 1,
                    message -> LOGGER.info("[JilatonVerify] shot {}: {}", name, message.getString()));
            logDisabled();
        });
    }

    /** Scenes whose subject is drawn by the HUD or the ClickGUI shell rather than the world. */
    private static boolean keepsHud(String name) {
        return name.startsWith("10-") || name.startsWith("27") || name.startsWith("31") || name.startsWith("11-") || name.startsWith("12-")
                || name.startsWith("14-") || name.startsWith("15-") || name.startsWith("16-")
                || name.startsWith("17-") || name.startsWith("18-") || name.startsWith("19-")
                || name.startsWith("20-") || name.startsWith("33");
    }

    private void logDisabled() {
        for (String id : VISUALS) {
            ClientModule module = JilatonClient.moduleManager().get(id);
            if (module == null) LOGGER.warn("[JilatonVerify] module {} is not registered", id);
        }
    }

    private final java.util.Set<String> expected = new java.util.HashSet<>();

    private void enable(String id) {
        ClientModule module = JilatonClient.moduleManager().get(id);
        if (module == null) {
            LOGGER.warn("[JilatonVerify] missing module {}", id);
            return;
        }
        module.setEnabled(true);
        expected.add(id);
        LOGGER.info("[JilatonVerify] enabled {} -> {}", id, module.enabled());
    }

    private void disable(String id) {
        ClientModule module = JilatonClient.moduleManager().get(id);
        if (module == null) return;
        if (expected.remove(id) && !module.enabled())
            LOGGER.error("[JilatonVerify] module {} disabled itself while it was being tested", id);
        module.setEnabled(false);
    }

    private void report() {
        for (String id : expected) {
            ClientModule module = JilatonClient.moduleManager().get(id);
            if (module != null && !module.enabled()) LOGGER.error("[JilatonVerify] module {} disabled itself", id);
        }
        LOGGER.info("[JilatonVerify] finished");
    }

    private static ChoiceSetting choice(String moduleId, String settingId) {
        Setting setting = find(moduleId, settingId);
        return setting instanceof ChoiceSetting choice ? choice : null;
    }

    /** Logs the values the next sky shot will actually render, so the filename can be checked against them. */
    private static void logSky(String label) {
        ChoiceSetting theme = choice("custom_sky", "theme");
        Setting pack = find("custom_sky", "dramatic_skys");
        LOGGER.info("[JilatonVerify] sky {}: theme={} dramatic_skys={}", label,
                theme == null ? "?" : theme.value(), pack instanceof BooleanSetting bool ? bool.value() : "?");
    }

    /** Proves the Ambience precedence chain from the run log, not only from pixels. */
    private static void logAmbience(String label) {
        ClientModule module = JilatonClient.moduleManager().get("ambience");
        if (!(module instanceof AmbienceModule ambience)) {
            LOGGER.warn("[JilatonVerify] ambience {}: module missing", label);
            return;
        }
        ChoiceSetting mode = choice("ambience", "color_mode");
        // SLF4J has no format specifiers, so the hex is formatted here; the accent and owners stay separate fields.
        LOGGER.info("[JilatonVerify] ambience {}: mode={} accent=#{} owners={}", label,
                mode == null ? "?" : mode.value(), String.format("%06X", ambience.resolvedAccent(0xA98CFF)), ownerList());
    }

    private static String ownerList() {
        StringBuilder owners = new StringBuilder();
        for (String id : List.of("custom_sky", "cyberspace", "glass_material", "fullbright")) {
            ClientModule module = JilatonClient.moduleManager().get(id);
            if (module != null && module.enabled()) owners.append(owners.length() == 0 ? "" : ",").append(id);
        }
        return owners.length() == 0 ? "none" : owners.toString();
    }

    private static void setting(String moduleId, String settingId, Object value) {
        Setting setting = find(moduleId, settingId);
        if (setting == null) {
            LOGGER.warn("[JilatonVerify] setting {}.{} is missing", moduleId, settingId);
            return;
        }
        try {
            if (setting instanceof ChoiceSetting choice) choice.setValue((String) value);
            else if (setting instanceof BooleanSetting bool) bool.setValue((Boolean) value);
            else if (setting instanceof NumberSetting number) number.setValue(((Number) value).doubleValue());
            else if (setting instanceof TextSetting text) text.setValue((String) value);
            else LOGGER.warn("[JilatonVerify] setting {}.{} not found", moduleId, settingId);
        } catch (RuntimeException failure) {
            LOGGER.warn("[JilatonVerify] could not set {}.{} = {}: {}", moduleId, settingId, value, failure.getMessage());
        }
    }

    private static Setting find(String moduleId, String settingId) {
        ClientModule module = JilatonClient.moduleManager().get(moduleId);
        if (module == null) return null;
        for (Setting setting : module.settings()) if (setting.id().equals(settingId)) return setting;
        return null;
    }

    /** Back to the studio platform centre facing +Z: earlier scenes walk the player off its edge. */
    private void studio(Minecraft client) {
        if (Double.isNaN(studioY)) return;
        command(client, "tp @a " + studioX + " " + studioY + " " + studioZ + " 0 8");
        client.player.setDeltaMovement(0, 0, 0);
    }

    private static void command(Minecraft client, String command) {
        IntegratedServer server = client.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
}
