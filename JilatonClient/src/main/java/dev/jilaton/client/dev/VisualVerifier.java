package dev.jilaton.client.dev;

import com.mojang.logging.LogUtils;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.config.ClientConfig;
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
    private double spiderStartY;
    private double autoWalkStartZ, autoWalkTurnZ;
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
        // ClickGUI restart round trip: what the previous run wrote at its end must be what this boot loaded.
        ClientConfig bootConfig = JilatonClient.clientConfig();
        String bootCategory = bootConfig.guiCategory(), bootExpanded = bootConfig.guiExpandedModule();
        LOGGER.info("[JilatonVerify] gui-restore loaded category={} expanded={}", bootCategory, bootExpanded);
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
        then(2, () -> {
            JilatonClient.clientConfig().setGuiScale(0.8);
            openGui("MODULES", "Visuals", "");
        });
        shot(12, "17b-clickgui-size-80");
        then(2, () -> JilatonClient.clientConfig().setGuiScale(1.0));
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

        // Crystal PvP look from the reference clip: glass-cube crystals, outlined target, smooth third-person camera.
        then(2, () -> {
            client.player.setYRot(0);
            client.player.setXRot(10);
            summonAhead(client, "zombie", 3.5);
            command(client, "execute as @a at @s rotated 0 0 run summon end_crystal ^1.6 ^ ^3 {ShowBottom:0b}");
            enable("explosive_highlights");
            enable("target_esp");
            setting("target_esp", "source", "Last hit");
            setting("target_esp", "memory", 15.0);
            enable("smooth_camera");
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        // Wait out the previous scene's death animation so the recorded target is the living zombie.
        then(30, () -> {
            LivingEntity zombie = nearestZombie(client);
            if (zombie != null) CombatTargetTracker.record(zombie, System.nanoTime());
        });
        then(10, () -> {
            var cam = client.gameRenderer.getMainCamera().position();
            LivingEntity zombie = nearestZombie(client);
            var esp = (dev.jilaton.client.module.impl.TargetEspModule) dev.jilaton.client.JilatonClient.moduleManager().get("target_esp");
            LOGGER.info("[JilatonVerify] target esp enabled={} shown={} tracked={} zombie={}", esp.enabled(), esp.shownTarget(),
                    CombatTargetTracker.lastHit(15, System.nanoTime()), zombie);
            LOGGER.info("[JilatonVerify] target outline color={} appearsGlowing={}",
                    zombie == null ? "no zombie" : dev.jilaton.client.render.EntityGlowSupport.color(zombie),
                    zombie != null && client.shouldEntityAppearGlowing(zombie));
            LOGGER.info("[JilatonVerify] smooth camera distance={} (setting 5)",
                    String.format(java.util.Locale.ROOT, "%.2f", cam.distanceTo(client.player.getEyePosition())));
        });
        shot(2, "36-crystal-pvp");
        then(3, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            disable("smooth_camera");
            disable("explosive_highlights");
            disable("target_esp");
            command(client, "kill @e[type=minecraft:zombie]");
            command(client, "kill @e[type=minecraft:end_crystal]");
        });

        // Video UI: info bar watermark, module spotlight card, shield cooldown pill, hexagram target marker.
        then(2, () -> {
            client.player.setYRot(0);
            client.player.setXRot(20);
            summonAhead(client, "zombie", 3.0);
            command(client, "item replace entity @p weapon.offhand with minecraft:shield");
            enable("target_esp");
            setting("target_esp", "source", "Last hit");
            setting("target_esp", "memory", 15.0);
            setting("target_esp", "style", "Hexagram");
        });
        then(30, () -> {
            LivingEntity zombie = nearestZombie(client);
            if (zombie != null) CombatTargetTracker.record(zombie, System.nanoTime());
            // Stand-in for an axe hit on the shield: the same item cooldown the server applies.
            client.player.getCooldowns().addCooldown(client.player.getOffhandItem(), 100);
            enable("fullbright");
        });
        shot(8, "37-video-ui");
        then(3, () -> {
            disable("fullbright");
            disable("target_esp");
            command(client, "item replace entity @p weapon.offhand with minecraft:air");
            command(client, "kill @e[type=minecraft:zombie]");
        });

        // Finisher: a zombie you hit dies, a 3D character appears beside it and lands a ground slam (first person),
        // then a second kill gets a spin kick seen from third person.
        then(2, () -> {
            client.player.setYRot(0);
            client.player.setXRot(15);
            enable("finisher");
            setting("finisher", "move", "Ground slam");
            setting("finisher", "kill_effect", "Lightning");
            setting("finisher", "crowd", 3.0);
            enable("fullbright");
            summonAhead(client, "zombie", 3.5);
        });
        then(30, () -> {
            LivingEntity zombie = nearestZombie(client);
            if (zombie != null) CombatTargetTracker.record(zombie, System.nanoTime());
            command(client, "kill @e[type=minecraft:zombie]");
        });
        shot(14, "38-finisher-leap");
        shot(6, "38-finisher-impact");
        then(30, () -> {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            setting("finisher", "move", "Spin kick");
            setting("finisher", "kill_effect", "Black hole");
            summonAhead(client, "zombie", 3.5);
        });
        then(30, () -> {
            LivingEntity zombie = nearestZombie(client);
            if (zombie != null) CombatTargetTracker.record(zombie, System.nanoTime());
            command(client, "kill @e[type=minecraft:zombie]");
        });
        shot(19, "38-finisher-spin");
        shot(10, "38-finisher-crowd");
        then(40, () -> {
            client.options.setCameraType(CameraType.FIRST_PERSON);
            disable("finisher");
            disable("fullbright");
        });

        // Storage ESP and Breadcrumbs: containers placed on the studio platform, then a short walked path.
        then(2, () -> {
            studio(client);
            client.player.setYRot(0);
            client.player.setXRot(25);
            command(client, at("setblock", 2, 0, 5) + " minecraft:chest");
            command(client, at("setblock", -2, 0, 6) + " minecraft:barrel");
            command(client, at("setblock", 0, 0, 8) + " minecraft:white_shulker_box");
            command(client, at("setblock", 3, 0, 9) + " minecraft:ender_chest");
            command(client, at("setblock", -3, 0, 9) + " minecraft:diamond_ore");
            enable("breadcrumbs");
            enable("fullbright");
        });
        for (int step = 0; step < 8; step++) {
            final int k = step;
            then(2, () -> command(client, "tp @a " + (studioX + (k % 2 == 0 ? 0.6 : -0.6)) + " " + studioY + " "
                    + (studioZ - 0.5 * (k + 1)) + " 0 25"));
        }
        then(5, () -> {
            enable("storage_esp");
            enable("block_esp");
        });
        shot(25, "39-storage-breadcrumbs");
        then(2, () -> {
            disable("storage_esp");
            disable("block_esp");
            disable("breadcrumbs");
            disable("fullbright");
            for (int[] o : new int[][]{{2, 0, 5}, {-2, 0, 6}, {0, 0, 8}, {3, 0, 9}, {-3, 0, 9}}) {
                command(client, at("setblock", o[0], o[1], o[2]) + " minecraft:air");
            }
            studio(client);
        });

        // Waypoints and InventoryCleaner: a waypoint where we stand, seen from 12 blocks back; junk thrown out.
        then(2, () -> {
            studio(client);
            enable("waypoints");
            enable("fullbright");
            var wp = dev.jilaton.client.module.impl.WaypointsModule.get();
            if (wp != null) wp.add(client, "Verify Base");
            command(client, "item replace entity @p inventory.5 with minecraft:rotten_flesh 12");
            command(client, "item replace entity @p inventory.6 with minecraft:diamond 3");
            setting("inventory_cleaner", "when", "Always");
            enable("inventory_cleaner");
        });
        then(4, () -> command(client, "tp @a " + studioX + " " + studioY + " " + (studioZ - 12) + " 0 10"));
        shot(20, "40-waypoint");
        then(2, () -> {
            var inv = client.player.getInventory();
            LOGGER.info("[JilatonVerify] cleaner slot14={} slot15={}", inv.getItem(14), inv.getItem(15));
            disable("inventory_cleaner");
            disable("fullbright");
            var wp = dev.jilaton.client.module.impl.WaypointsModule.get();
            if (wp != null) wp.store().remove(dev.jilaton.client.module.impl.WaypointsModule.worldKey(client),
                    dev.jilaton.client.module.impl.WaypointsModule.dimensionKey(client), "Verify Base");
            command(client, "clear @p minecraft:diamond");
            studio(client);
        });

        // ChestStealer: open a real chest holding diamonds and junk; the diamonds move over, the junk stays,
        // the screen closes. AutoRespawn: die and come back without touching the death screen.
        then(2, () -> {
            studio(client);
            command(client, at("setblock", 0, 0, 2) + " minecraft:chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:5},"
                    + "{Slot:1b,id:\"minecraft:rotten_flesh\",count:3}]}");
            setting("inventory_cleaner", "when", "Inventory open");
            enable("chest_stealer");
            enable("auto_respawn");
        });
        then(10, () -> {
            net.minecraft.core.BlockPos chest = net.minecraft.core.BlockPos.containing(studioX, studioY, studioZ + 2);
            client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chest),
                            net.minecraft.core.Direction.UP, chest, false));
        });
        then(30, () -> {
            int diamonds = client.player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND);
            LOGGER.info("[JilatonVerify] stealer diamonds={} screen={}", diamonds,
                    client.screen == null ? "closed" : client.screen.getClass().getSimpleName());
            command(client, "kill @p");
        });
        then(40, () -> {
            LOGGER.info("[JilatonVerify] respawn alive={} screen={}", client.player != null && client.player.isAlive(),
                    client.screen == null ? "none" : client.screen.getClass().getSimpleName());
            disable("chest_stealer");
            disable("auto_respawn");
            command(client, at("setblock", 0, 0, 2) + " minecraft:air");
            command(client, "clear @p minecraft:diamond");
            studio(client);
        });

        // XRay (Anubis port): looking down through the studio floor at a diamond ore buried beneath it.
        then(2, () -> {
            studio(client);
            command(client, at("setblock", 1, -3, 3) + " minecraft:diamond_ore");
            command(client, at("setblock", -1, -2, 3) + " minecraft:gold_ore");
            client.player.setYRot(0);
            client.player.setXRot(60);
            setting("xray", "opacity", 10.0);
            enable("xray");
        });
        shot(40, "42-xray");
        then(2, () -> disable("xray"));
        // NoRender: a carved pumpkin on the head normally blurs the whole screen.
        then(20, () -> {
            client.player.setXRot(10);
            command(client, "item replace entity @p armor.head with minecraft:carved_pumpkin");
            enable("no_render");
        });
        shot(10, "42b-norender-pumpkin");
        then(2, () -> {
            disable("no_render");
            command(client, "item replace entity @p armor.head with minecraft:air");
            command(client, at("setblock", 1, -3, 3) + " minecraft:air");
            command(client, at("setblock", -1, -2, 3) + " minecraft:air");
            command(client, at("setblock", 0, 0, 2) + " minecraft:air");
        });

        // Anubis ports: Time Changer forces noon, Sneak (Vanilla) crouches, Spider climbs a wall.
        then(2, () -> {
            studio(client);
            client.player.setYRot(0);
            client.player.setXRot(-10);
            setting("time_changer", "time", 6000.0);
            enable("time_changer");
            setting("sneak", "mode", "Vanilla");
            enable("sneak");
        });
        then(10, () -> LOGGER.info("[JilatonVerify] time_changer dayTime={} sneak crouching={}",
                client.level.getDayTime() % 24000, client.player.isCrouching()));
        shot(4, "43-time-changer-noon");
        then(2, () -> {
            disable("sneak");
            disable("time_changer");
            command(client, "fill " + (int) Math.floor(studioX - 1) + " " + (int) Math.floor(studioY) + " " + (int) Math.floor(studioZ + 1)
                    + " " + (int) Math.floor(studioX + 1) + " " + (int) Math.floor(studioY + 5) + " " + (int) Math.floor(studioZ + 1) + " minecraft:stone");
            enable("spider");
        });
        then(4, () -> {
            spiderStartY = client.player.getY();
            client.options.keyUp.setDown(true);
        });
        then(20, () -> {
            client.options.keyUp.setDown(false);
            LOGGER.info("[JilatonVerify] spider climbed={}", String.format(java.util.Locale.ROOT, "%.2f", client.player.getY() - spiderStartY));
            disable("spider");
            command(client, "fill " + (int) Math.floor(studioX - 1) + " " + (int) Math.floor(studioY) + " " + (int) Math.floor(studioZ + 1)
                    + " " + (int) Math.floor(studioX + 1) + " " + (int) Math.floor(studioY + 5) + " " + (int) Math.floor(studioZ + 1) + " minecraft:air");
            studio(client);
        });

        // Redesign tour: full-screen menus captured over the live world.
        then(2, () -> {
            JilatonClient.clientConfig().setMenuBackground("Dark Veil");
            client.setScreen(new dev.jilaton.client.gui.JilatonMainMenuScreen());
        });
        shot(40, "50-home");
        then(2, () -> JilatonClient.clientConfig().setMenuBackground("Aurora"));
        shot(10, "50b-home-aurora");
        then(2, () -> JilatonClient.clientConfig().setMenuBackground("Threads"));
        shot(10, "50c-home-threads");
        then(2, () -> JilatonClient.clientConfig().setMenuBackground("Galaxy"));
        shot(10, "50d-home-galaxy");
        then(2, () -> JilatonClient.clientConfig().setMenuBackground("Liquid Chrome"));
        shot(10, "50e-home-chrome");
        then(2, () -> JilatonClient.clientConfig().setMenuBackground("Dark Veil"));
        then(2, () -> client.setScreen(null));
        then(2, () -> client.setScreen(new dev.jilaton.client.gui.JilatonPauseScreen()));
        shot(30, "51-pause");
        then(2, () -> client.setScreen(null));
        then(2, () -> {
            var list = new net.minecraft.client.multiplayer.ServerList(client);
            list.load();
            if (list.get("127.0.0.1") == null) {
                list.add(new net.minecraft.client.multiplayer.ServerData("Verify server", "127.0.0.1",
                        net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);
                list.save();
            }
            client.setScreen(new dev.jilaton.client.gui.JilatonServersScreen(null));
        });
        shot(30, "52-servers");
        then(2, () -> client.setScreen(new dev.jilaton.client.gui.JilatonWorldsScreen(null)));
        shot(30, "53-worlds");
        then(2, () -> client.setScreen(null));

        // Hand-pose port: linked mirrored hands, real held items, shared map, knife and ease-out.
        then(2, () -> {
            studio(client);
            client.options.hideGui = false;
            client.options.setCameraType(CameraType.FIRST_PERSON);
            disable("cosmetic_knife");
            command(client, "time set 6000");
            command(client, "item replace entity @p weapon.mainhand with minecraft:diamond_sword");
            command(client, "item replace entity @p weapon.offhand with minecraft:shield");
            JilatonClient.moduleManager().get("view_model").resetSettings();
            setting("view_model", "pivot", "Hand");
            setting("view_model", "link_hands", true);
            setting("view_model", "glide", 0.4);
            setting("view_model", "main_x", 0.15);
            setting("view_model", "main_y", 0.15);
            setting("view_model", "main_ry", 20.0);
            setting("view_model", "main_scale", 0.8);
            enable("view_model");
        });
        shot(30, "44-hand-pose-linked");
        then(2, () -> {
            enable("cosmetic_knife");
            enable("swing_animations");
            client.player.swing(InteractionHand.MAIN_HAND);
        });
        shot(2, "44b-hand-pose-knife-swing");
        then(8, () -> {
            disable("cosmetic_knife");
            disable("swing_animations");
            command(client, "item replace entity @p weapon.offhand with minecraft:air");
            command(client, "item replace entity @p weapon.mainhand with minecraft:filled_map[minecraft:map_id=0]");
        });
        shot(20, "44c-hand-pose-shared-map");
        then(2, () -> disable("view_model"));
        then(35, () -> {
            var model = (dev.jilaton.client.module.impl.ViewModelModule) JilatonClient.moduleManager().get("view_model");
            LOGGER.info("[JilatonVerify] hand-pose disabled={} renderActive={}", !model.enabled(), model.renderActive());
            if (model.renderActive()) throw new IllegalStateException("Hand pose did not return to rest");
        });
        shot(2, "44d-hand-pose-restored");
        then(2, () -> {
            JilatonClient.moduleManager().get("view_model").resetSettings();
            command(client, "item replace entity @p weapon.mainhand with minecraft:air");
        });

        // AutoWalk changes direction without leaving either physical key held, then yields to manual input.
        then(2, () -> {
            studio(client);
            client.options.keyUp.setDown(false);
            client.options.keyDown.setDown(false);
            JilatonClient.moduleManager().get("auto_walk").resetSettings();
            setting("auto_walk", "safe", false);
            setting("auto_walk", "auto_jump", false);
        });
        then(5, () -> {
            autoWalkStartZ = client.player.getZ();
            enable("auto_walk");
        });
        then(10, () -> {
            autoWalkTurnZ = client.player.getZ();
            setting("auto_walk", "direction", "Backward");
        });
        then(12, () -> {
            double forward = autoWalkTurnZ - autoWalkStartZ;
            double backward = autoWalkTurnZ - client.player.getZ();
            disable("auto_walk");
            LOGGER.info("[JilatonVerify] auto-walk forward={} backward={} forwardKey={} backwardKey={}",
                    forward, backward, client.options.keyUp.isDown(), client.options.keyDown.isDown());
            if (forward < 0.2 || backward < 0.2 || client.options.keyUp.isDown() || client.options.keyDown.isDown())
                throw new IllegalStateException("AutoWalk direction/physical-key regression");
        });
        shot(2, "45-auto-walk-restored");
        then(2, () -> {
            setting("auto_walk", "disable_on_input", true);
            // This test expects the module to disable itself, so don't add it to the stay-enabled set.
            JilatonClient.moduleManager().get("auto_walk").setEnabled(true);
            client.options.keyLeft.setDown(true);
        });
        then(3, () -> {
            boolean yielded = !JilatonClient.moduleManager().get("auto_walk").enabled();
            client.options.keyLeft.setDown(false);
            disable("auto_walk");
            LOGGER.info("[JilatonVerify] auto-walk manual input yielded={}", yielded);
            if (!yielded) throw new IllegalStateException("AutoWalk did not yield to manual input");
            JilatonClient.moduleManager().get("auto_walk").resetSettings();
            studio(client);
        });

        // AutoTool must select the real diamond pick, yield to a manual slot change, and restore on disable.
        then(2, () -> {
            studio(client);
            client.player.setYRot(0);
            client.player.setXRot(0);
            command(client, "gamemode survival @p");
            command(client, at("setblock", 0, 1, 3) + " minecraft:obsidian");
            command(client, "item replace entity @p hotbar.0 with minecraft:stick");
            command(client, "item replace entity @p hotbar.1 with minecraft:wooden_pickaxe");
            command(client, "item replace entity @p hotbar.2 with minecraft:diamond_pickaxe");
            command(client, "item replace entity @p hotbar.3 with minecraft:air");
            JilatonClient.moduleManager().get("auto_tool").resetSettings();
            setting("auto_tool", "drill_instabreak", false);
        });
        then(8, () -> {
            client.player.getInventory().setSelectedSlot(0);
            enable("auto_tool");
            client.options.keyAttack.setDown(true);
        });
        then(8, () -> {
            int slot = client.player.getInventory().getSelectedSlot();
            LOGGER.info("[JilatonVerify] auto-tool selected={}", slot);
            if (slot != 2) throw new IllegalStateException("AutoTool did not select the diamond pickaxe");
            client.options.keyAttack.setDown(false);
            client.player.getInventory().setSelectedSlot(3);
        });
        then(5, () -> {
            int slot = client.player.getInventory().getSelectedSlot();
            LOGGER.info("[JilatonVerify] auto-tool manual slot={}", slot);
            if (slot != 3) throw new IllegalStateException("AutoTool overwrote a manual slot selection");
            client.player.getInventory().setSelectedSlot(0);
            client.options.keyAttack.setDown(true);
        });
        then(8, () -> {
            disable("auto_tool");
            client.options.keyAttack.setDown(false);
            int slot = client.player.getInventory().getSelectedSlot();
            LOGGER.info("[JilatonVerify] auto-tool disabled restored={}", slot);
            if (slot != 0) throw new IllegalStateException("AutoTool did not restore on disable");
        });
        shot(2, "46-auto-tool-restored");
        then(2, () -> {
            command(client, "gamemode creative @p");
            command(client, at("setblock", 0, 1, 3) + " minecraft:air");
            JilatonClient.moduleManager().get("auto_tool").resetSettings();
        });

        // A client-only player fixture verifies cancellation before attack FX; no fake-player packet is sent.
        then(2, () -> {
            studio(client);
            client.player.setYRot(0);
            client.player.setXRot(0);
            var friends = (dev.jilaton.client.module.impl.FriendsModule) JilatonClient.moduleManager().get("friends");
            var names = dev.jilaton.client.module.impl.FriendManager.names();
            boolean wasEnabled = friends.enabled();
            var bots = JilatonClient.moduleManager().get("anti_bot");
            boolean botsEnabled = bots.enabled();
            var assist = (dev.jilaton.client.module.impl.AimAssistModule) JilatonClient.moduleManager().get("aim_assist");
            boolean assistEnabled = assist.enabled();
            var friend = new net.minecraft.client.player.RemotePlayer(client.level,
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "VerifyFriend"));
            friend.setId(2_000_000_000);
            friend.setPos(client.player.getX(), client.player.getY(), client.player.getZ() + 1.8);
            client.level.addEntity(friend);
            try {
                friends.resetSettings();
                friends.setEnabled(true);
                dev.jilaton.client.module.impl.FriendManager.add("VerifyFriend");
                boolean melee = friends.intercepts(client.player, friend);
                boolean spear = friends.interceptsStab(client.player);
                if (!melee || !spear) throw new IllegalStateException("Friend guard did not recognize its in-range fixture");
                CombatTargetTracker.clear();
                client.gameMode.attack(client.player, friend);
                boolean canceled = CombatTargetTracker.lastTarget() == null;
                LOGGER.info("[JilatonVerify] friend-guard melee={} spear={} canceled-before-fx={}", melee, spear, canceled);
                if (!canceled) throw new IllegalStateException("Friend attack reached normal attack effects");

                // A visible, client-only target validates target selection and the real mouse-count port.
                dev.jilaton.client.module.impl.FriendManager.remove("VerifyFriend");
                bots.setEnabled(false); // The fixture intentionally has no server tab-list entry.
                friend.setPos(client.player.getX() - .8, client.player.getY(), client.player.getZ() + 3);
                assist.resetSettings();
                setting("aim_assist", "mode", "Mouse assist");
                setting("aim_assist", "weapons_only", false);
                assist.setEnabled(true);
                assist.tick(client);
                var nudge = assist.nudge(client.player, 1, 0, .016);
                var rotations = JilatonClient.moduleManager().rotationManager();
                rotations.request("friends", 70, client.player.getYRot(), client.player.getXRot(), 4);
                var yielded = assist.nudge(client.player, 1, 0, .016);
                rotations.cancelAll();
                LOGGER.info("[JilatonVerify] aim-assist mouse-yaw={} higher-priority-yield={}", nudge.yaw(), yielded.yaw() == 0);
                if (!(nudge.yaw() > 0) || yielded.yaw() != 0)
                    throw new IllegalStateException("AimAssist mouse target/correction arbitration failed");

                // Keep aim off the target so no item is used; exercise two real hotbar acquisitions.
                var shoot = (dev.jilaton.client.module.impl.AutoShootModule) JilatonClient.moduleManager().get("auto_shoot");
                var inventory = client.player.getInventory();
                var savedHotbar = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
                for (int i = 0; i < 9; i++) savedHotbar.add(inventory.getItem(i).copy());
                var savedOffhand = client.player.getOffhandItem().copy();
                int selectedBefore = inventory.getSelectedSlot();
                boolean shootEnabled = shoot.enabled();
                try {
                    for (int i = 0; i < 9; i++) inventory.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                    client.player.setItemInHand(InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                    inventory.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
                    inventory.setItem(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SNOWBALL));
                    inventory.setItem(4, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EGG));
                    inventory.setSelectedSlot(0);
                    shoot.resetSettings();
                    setting("auto_shoot", "aim_mode", "Direct");
                    setting("auto_shoot", "aim_tolerance", .5);
                    shoot.setEnabled(true);
                    shoot.tick(client);
                    int first = inventory.getSelectedSlot();
                    inventory.setItem(2, net.minecraft.world.item.ItemStack.EMPTY);
                    shoot.tick(client);
                    int second = inventory.getSelectedSlot();
                    shoot.setEnabled(false);
                    int restored = inventory.getSelectedSlot();
                    LOGGER.info("[JilatonVerify] auto-shoot first-slot={} second-slot={} restored={}", first, second, restored);
                    if (first != 2 || second != 4 || restored != 0)
                        throw new IllegalStateException("AutoShoot lost the original hotbar slot across projectile stacks");
                } finally {
                    shoot.setEnabled(false);
                    for (int i = 0; i < 9; i++) inventory.setItem(i, savedHotbar.get(i));
                    client.player.setItemInHand(InteractionHand.OFF_HAND, savedOffhand);
                    inventory.setSelectedSlot(selectedBefore);
                    shoot.resetSettings();
                    shoot.setEnabled(shootEnabled);
                    rotations.cancelAll();
                }

                var hitbox = (dev.jilaton.client.module.impl.HitboxModule) JilatonClient.moduleManager().get("hitbox");
                boolean hitboxEnabled = hitbox.enabled();
                hitbox.setEnabled(false);
                var physicalBox = friend.getBoundingBox();
                float basePick = friend.getPickRadius();
                var attackRange = client.player.entityAttackRange();
                var beyondRange = client.player.getEyePosition().add(client.player.getViewVector(1).scale(
                        attackRange.effectiveMaxRange(client.player) + attackRange.hitboxMargin() + .25));
                boolean baseRange = attackRange.isInRange(client.player, beyondRange);
                try {
                    hitbox.size().setValue(.5);
                    setting("hitbox", "apply_to_component", true);
                    hitbox.setEnabled(true);
                    float expandedPick = friend.getPickRadius();
                    boolean component = attackRange.isInRange(client.player, beyondRange);
                    setting("hitbox", "apply_to_component", false);
                    boolean optedOut = attackRange.isInRange(client.player, beyondRange);
                    hitbox.setEnabled(false);
                    boolean restored = friend.getPickRadius() == basePick;
                    LOGGER.info("[JilatonVerify] hitbox pick={} component={} base={} opt-out={} collision-unchanged={} restored={}",
                            expandedPick, component, baseRange, optedOut, physicalBox.equals(friend.getBoundingBox()), restored);
                    if (expandedPick != .5f || !component || baseRange || optedOut || !restored
                            || !physicalBox.equals(friend.getBoundingBox()))
                        throw new IllegalStateException("Hitbox native pick/component/lifecycle regression");
                } finally {
                    hitbox.resetSettings();
                    hitbox.setEnabled(hitboxEnabled);
                }
            } finally {
                friend.discard();
                dev.jilaton.client.module.impl.FriendManager.replace(names);
                friends.setEnabled(wasEnabled);
                assist.setEnabled(assistEnabled);
                assist.resetSettings();
                bots.setEnabled(botsEnabled);
            }
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
        // Lyrics offline: online/Spotify lyrics cannot resolve here, but user-provided synced lyrics can. A
        // silent WAV plus an .lrc of placeholder lines (written by the harness, no third-party text) is played
        // through the Local files source and shown in three placements.
        then(1, () -> studio(client));
        then(5, () -> {
            client.options.hideGui = false;
            client.setScreen(null);
            client.options.setCameraType(CameraType.FIRST_PERSON);
            command(client, "time set 13000");
            client.player.setYRot(0);
            client.player.setXRot(-4);
            java.nio.file.Path folder = client.gameDirectory.toPath().resolve("verify-music");
            try {
                writeVerifyTrack(folder);
            } catch (IOException failure) {
                LOGGER.error("[JilatonVerify] could not write the verify track: {}", failure.getMessage());
                return;
            }
            setting("music_center", "source", "Local files");
            setting("music_center", "folder", folder.toString());
            dev.jilaton.client.music.MusicCenter center = dev.jilaton.client.music.MusicCenter.get();
            center.setFolder(folder);
            center.rescan();
            center.player().setQueue(center.library().all(), 0);
            setting("spatial_lyrics", "third_person_only", false);
            setting("spatial_lyrics", "mode", "Curved Arc");
            enable("spatial_lyrics");
        });
        then(80, () -> logMusic());
        shot(10, "35-lyrics-local-arc");
        then(2, () -> setting("spatial_lyrics", "mode", "World Trail"));
        then(80, () -> logMusic());
        shot(10, "35b-lyrics-local-trail");
        then(2, () -> {
            setting("spatial_lyrics", "mode", "Sky Lyrics");
            client.player.setXRot(-35);
        });
        then(80, () -> logMusic());
        shot(10, "35c-lyrics-local-sky");
        then(3, () -> {
            disable("spatial_lyrics");
            dev.jilaton.client.music.MusicCenter.get().player().stop();
            setting("music_center", "source", "Off");
            client.player.setXRot(0);
        });

        then(5, () -> {
            // Alternate the markers so every run checks a value the previous run (a real restart) wrote.
            ClientConfig config = JilatonClient.clientConfig();
            String category = "Movement".equals(bootCategory) ? "Combat" : "Movement";
            String expanded = "auto_clicker".equals(bootExpanded) ? "aim_assist" : "auto_clicker";
            config.setGuiCategory(category);
            config.setGuiExpandedModule(expanded);
            config.save(JilatonClient.moduleManager());
            LOGGER.info("[JilatonVerify] gui-restore wrote category={} expanded={}", category, expanded);
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

    /** A 30 s silent 8 kHz mono WAV and a synced .lrc beside it: one new line every 3 seconds. */
    private static void writeVerifyTrack(java.nio.file.Path folder) throws IOException {
        java.nio.file.Files.createDirectories(folder);
        int rate = 8000, seconds = 30, samples = rate * seconds;
        java.nio.ByteBuffer wav = java.nio.ByteBuffer.allocate(44 + samples * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(36 + samples * 2)
                .put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1)
                .putShort((short) 1).putInt(rate).putInt(rate * 2).putShort((short) 2).putShort((short) 16)
                .put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(samples * 2);
        java.nio.file.Files.write(folder.resolve("Jilaton Verify - Test Pattern.wav"), wav.array());
        StringBuilder lrc = new StringBuilder("[ti:Test Pattern]\n[ar:Jilaton Verify]\n");
        for (int line = 0; line < 10; line++) {
            lrc.append(String.format(java.util.Locale.ROOT, "[00:%02d.00]Verify line %d of ten\n", line * 3, line + 1));
        }
        java.nio.file.Files.writeString(folder.resolve("Jilaton Verify - Test Pattern.lrc"), lrc);
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
                || name.startsWith("20-") || name.startsWith("33") || name.startsWith("35") || name.startsWith("37")
                || name.startsWith("44");
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

    /** A command verb followed by absolute block coordinates offset from the studio centre. */
    private String at(String verb, int dx, int dy, int dz) {
        return verb + " " + (int) Math.floor(studioX + dx) + " " + (int) Math.floor(studioY + dy) + " " + (int) Math.floor(studioZ + dz);
    }

    private static void command(Minecraft client, String command) {
        IntegratedServer server = client.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
}
