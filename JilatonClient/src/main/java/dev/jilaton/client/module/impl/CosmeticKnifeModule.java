package dev.jilaton.client.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.mixin.ItemInHandRendererInvoker;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.EffectPalette;
import dev.jilaton.client.render.fx.FxEmitter;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.model.AssetMesh;
import dev.jilaton.client.render.model.BodyAnchor;
import dev.jilaton.client.render.model.HeldModelRenderer;
import dev.jilaton.client.render.model.KnifeMotion;
import dev.jilaton.client.render.model.KnifeThirdPerson;
import dev.jilaton.client.render.model.Material;
import dev.jilaton.client.render.model.Mesh;
import dev.jilaton.client.render.model.ModelBatch;
import dev.jilaton.client.render.model.Transform;
import dev.jilaton.client.render.model.asset.ModelAssets;
import dev.jilaton.client.render.knife.CsKnifeEngine;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import dev.jilaton.client.setting.NumberSetting;
import dev.jilaton.client.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * First-person knife viewmodel. The main-hand pass draws the player's own arm through vanilla (skin, slim or wide
 * arms, 3D characters all apply) and attaches the knife model at that hand, so the blade moves with the arm instead
 * of floating in front of the camera. On top sits a CS-style motion set ({@link KnifeMotion}): a twirling draw when
 * the knife comes out, idle breathing, camera-turn inertia, alternating slashes on attack and an inspect (key V).
 *
 * <p>The knife meshes come from {@link ModelAssets}; every asset part carries a material tag and is drawn with its
 * own recipe — steel face, bright edge bevel, dark grip, accent-lit fittings.
 */
public final class CosmeticKnifeModule extends ClientModule implements FxEmitter {
    /** Jilaton's own knives: each is a registered {@link ModelAssets} knife id, title-cased. */
    public static final List<String> JILATON_STYLES = List.of("Bayonet", "Karambit", "Butterfly", "Tanto");
    /** Skins drawn by the ported Anubis engine ({@link CsKnifeEngine}): its own clips, two-handed katana, trails. */
    public static final String CS_PREFIX = "CS ";
    public static final List<String> CS_STYLES = List.of("CS Butterfly", "CS Karambit", "CS Katana");
    private static final List<String> STYLES = java.util.stream.Stream.concat(JILATON_STYLES.stream(), CS_STYLES.stream()).toList();
    /** Longest dimension of the drawn knife at scale 1, in blocks. */
    private static final double DRAWN_LENGTH = 0.34;
    /** The arm's rest offsets that the side/height settings are measured from. */
    private static final double REST_SIDE = 0.26, REST_HEIGHT = -0.16;

    /**
     * Where the grip sits in the vanilla arm's model space (before the arm part's own transform): the centre of the
     * fist at the bottom of the right arm, and the rotation that lays the blade forward out of the fist.
     */
    public record Grip(double x, double y, double z, double rx, double ry, double rz) { }
    public static volatile Grip grip = new Grip(-0.36, 0.70, -0.02, -15, 175, 0);
    /**
     * The first-person arm sits a hand's width from the camera, so a knife at its world length would read as a toy;
     * this matches the drawn blade to the arm's length, like vanilla's held sword.
     */
    public static volatile double handScale = 2.1;
    /** Dev harness only: draw the knife at a fixed spot in view space, skipping the arm, to check the submit path. */
    public static volatile boolean debugViewSpace;
    /** Dev harness only: hand-pass frames this module has drawn. */
    public static volatile long framesDrawn;

    private final ChoiceSetting style = addSetting(new ChoiceSetting("style", "Blade", STYLES, "Bayonet"));
    private final ChoiceSetting holdOn = addSetting(new ChoiceSetting("hold_on", "Replace",
            List.of("Swords", "Swords & axes", "Any item"), "Swords"));
    private final NumberSetting side = addSetting(new NumberSetting("side", "Side offset", 0.18, 0.34, 0.01, REST_SIDE));
    private final NumberSetting height = addSetting(new NumberSetting("height", "Height", -0.34, 0.10, 0.01, REST_HEIGHT));
    private final NumberSetting scale = addSetting(new NumberSetting("scale", "Scale", 0.70, 1.30, 0.05, 1.0));
    private final NumberSetting sway = addSetting(new NumberSetting("sway", "Idle sway & inertia", 0, 2, 0.05, 1.0));
    private final NumberSetting bob = addSetting(new NumberSetting("bob", "Walk bob", 0, 2, 0.05, 1.2));
    private final NumberSetting equipSeconds = addSetting(new NumberSetting("equip_seconds", "Draw time", 0.12, 0.80, 0.02, 0.55));
    private final BooleanSetting slashes = addSetting(new BooleanSetting("slashes", "Slash animation", true));
    private final ChoiceSetting palette = addSetting(EffectPalette.modeSetting("palette", "Accent"));
    private final TextSetting custom = addSetting(EffectPalette.hexSetting("custom", "Custom accent", "C9D2E8"));
    private final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Opacity", 0.20, 1, 0.05, 1.0));
    private final BooleanSetting showArm = addSetting(new BooleanSetting("show_arm", "CS skins: show arms", true));
    private final BooleanSetting deploy = addSetting(new BooleanSetting("deploy", "CS skins: deploy animation", true));
    private final NumberSetting inspectSpeed = addSetting(new NumberSetting("inspect_speed", "CS skins: animation speed", 0.5, 2, 0.05, 1));
    private final NumberSetting trailLength = addSetting(new NumberSetting("trail_length", "CS skins: slash trail length", 0.05, 0.35, 0.01, 0.14));
    private final BooleanSetting sparkles = addSetting(new BooleanSetting("sparkles", "CS skins: blade sparkle", true));
    private final BooleanSetting thirdPerson = addSetting(new BooleanSetting("third_person", "Show in third person", true));
    private final BooleanSetting others = addSetting(new BooleanSetting("others", "Show on other players", true));
    /** Built on first use so constructing the module never touches the engine's meshes. */
    private CsKnifeEngine engine;

    private final KnifeMotion motion = new KnifeMotion();
    private final ModelBatch batch = new ModelBatch();
    /** One motion per visible player: the first-person hand pass only covers the local player. */
    private final Map<Integer, KnifeMotion> worldMotions = new LinkedHashMap<>();
    private final Map<Integer, Boolean> worldSwings = new LinkedHashMap<>();
    private final Map<String, Material> materials = new LinkedHashMap<>();
    private AssetMesh.Fitted fitted;
    private String fittedKey = "";
    private int materialAccent = Integer.MIN_VALUE;
    private int heldSlot = -1;
    /** Null until the first frame: touching ItemStack at construction would need Minecraft's registries. */
    private ItemStack heldItem;
    private float lastSwing;
    private long lastFrame;

    public CosmeticKnifeModule() {
        super("cosmetic_knife", "Knife", "CS-style knives in your hand (incl. two-handed katana): draw, slash, inspect (V).",
                ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable(Minecraft client) {
        heldSlot = -1;
        worldMotions.clear();
        worldSwings.clear();
        motion.startDraw(System.nanoTime());
    }

    @Override
    protected void onDisable(Minecraft client) {
        motion.startUnequip(System.nanoTime());
        heldSlot = -1;
        lastFrame = 0;
        worldMotions.clear();
        worldSwings.clear();
        batch.reset();
        if (engine != null) engine.deactivate();
    }

    /**
     * Third-person pass: the same {@link KnifeMotion} state machine drives the blade at each player's wrist, so a draw,
     * slash or inspect reads identically from outside. The CS skins keep their own ported clips and hand pass.
     */
    @Override
    public void emit(FxFrame f) {
        Minecraft client = f.client;
        if (client.level == null || csSkin()) return;
        if (!thirdPerson.value() && !others.value()) return;
        worldMotions.keySet().removeIf(id -> client.level.getEntity(id) == null);
        worldSwings.keySet().removeIf(id -> client.level.getEntity(id) == null);

        AssetMesh.Fitted model = fitted(style.value());
        double drawScale = model.scaleFor(scale.value());
        int accent = EffectPalette.resolve(palette.value(),
                dev.jilaton.client.gui.ClientTheme.resolveAccent(JilatonClient.clientConfig().themeId(),
                        JilatonClient.clientConfig().customAccent()), custom.value());
        batch.reset();
        boolean drew = false;
        for (Player player : client.level.players()) {
            boolean self = player == client.player;
            if (self ? f.firstPerson || !thirdPerson.value() : !others.value()) continue;
            if (!player.isAlive() || player.isSpectator() || player.isInvisible()) continue;
            // Only the bare hand: with an item held, vanilla already draws that item in third person and this pass
            // must not put a second blade on top of it.
            if (!player.getMainHandItem().isEmpty()) continue;
            KnifeMotion state = worldState(player, f);
            KnifeMotion.Pose pose = state.pose(f.now, equipSeconds.value(), sway.value(), bob.value(),
                    player.walkAnimation.position(f.partial) * 0.6662 / Math.PI,
                    Mth.clamp(player.walkAnimation.speed(f.partial), 0, 1),
                    player.isSprinting(), player.isCrouching(), player.onGround(),
                    player.getDeltaMovement().y, f.reducedMotion);
            BodyAnchor.Part wristPart = player.getMainArm() == HumanoidArm.RIGHT
                    ? BodyAnchor.Part.RIGHT_HAND : BodyAnchor.Part.LEFT_HAND;
            Transform knife = KnifeThirdPerson.hand(BodyAnchor.forPart(player, wristPart, f.partial), pose);
            for (Map.Entry<String, Mesh> part : model.parts().entrySet()) {
                batch.add(f, part.getValue(), material(part.getKey(), accent), knife, drawScale,
                        opacity.value(), false);
            }
            drew = true;
        }
        if (drew) batch.flush(f);
    }

    private KnifeMotion worldState(Player player, FxFrame f) {
        KnifeMotion state = worldMotions.get(player.getId());
        if (state == null) {
            state = new KnifeMotion();
            state.startDraw(f.now);
            worldMotions.put(player.getId(), state);
        }
        boolean swing = player.swinging;
        if (swing && !worldSwings.getOrDefault(player.getId(), false) && slashes.value()) state.startSlash(f.now);
        worldSwings.put(player.getId(), swing);
        state.look(player.getViewYRot(f.partial), player.getViewXRot(f.partial), f.dt, sway.value());
        return state;
    }

    /** Whether the selected blade is one of the ported CS skins rather than a Jilaton knife. */
    public boolean csSkin() { return style.value().startsWith(CS_PREFIX); }

    @Override
    protected void onTick(Minecraft client) {
        if (!csSkin()) {
            if (engine != null) engine.deactivate();
            return;
        }
        if (engine == null) engine = new CsKnifeEngine();
        engine.configure(new CsKnifeEngine.Config(style.value().substring(CS_PREFIX.length()), csItems(),
                (float) inspectSpeed.value(), (float) scale.value(), showArm.value(), deploy.value(), slashes.value(),
                (float) trailLength.value(), sparkles.value()), JilatonClient::inspectHeld);
        if (CsKnifeEngine.active != engine) engine.activate();
        engine.setBare(client.player != null && client.player.getMainHandItem().isEmpty());
        engine.tick();
    }

    private String csItems() {
        return switch (holdOn.value()) {
            case "Swords & axes" -> "Weapons";
            case "Any item" -> "Any Item";
            default -> "Swords";
        };
    }

    /**
     * Whether the knife occupies this main-hand pass. An empty hand always gets the blade — that is the module's
     * home pose; the Replace setting only decides which held items it also takes over.
     */
    public boolean replaces(ItemStack item) {
        return occupies(item.isEmpty(), item.is(ItemTags.SWORDS), item.is(ItemTags.AXES),
                holdOn.value(), csSkin());
    }

    /** The replace decision without an {@link ItemStack}, so it can be tested headless. */
    static boolean occupies(boolean heldEmpty, boolean sword, boolean axe, String mode, boolean csSkin) {
        if (heldEmpty) return true;
        if (csSkin) return false;
        return switch (mode) {
            case "Swords & axes" -> sword || axe;
            case "Any item" -> true;
            default -> sword;
        };
    }

    /** Starts the inspect animation; ignored mid-slash or mid-draw. */
    public void inspect() {
        if (!enabled()) return;
        if (csSkin()) {
            if (engine != null) engine.inspect();
            return;
        }
        long now = System.nanoTime();
        motion.startInspect(now, equipSeconds.value());
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            KnifeMotion world = worldMotions.get(client.player.getId());
            if (world != null) world.startInspect(now, equipSeconds.value());
        }
    }

    /** Harness hook: selects a blade without going through the GUI. */
    public void setStyle(String value) { style.setValue(value); }

    /** Harness hook: picks what the knife replaces without going through the GUI. */
    public void setHoldOn(String value) { holdOn.setValue(value); }

    /** Test and harness hook: the animation state this module drives. */
    public KnifeMotion motion() { return motion; }

    /** Draws the arm and the attached knife for this frame's main-hand pass. */
    public void renderInHand(ItemInHandRendererInvoker hands, AbstractClientPlayer player, float partial, float swing,
                             PoseStack pose, SubmitNodeCollector collector, int light) {
        framesDrawn++;
        if (csSkin()) {
            // The ported engine draws the skin and its arm(s) in an empty hand; fall back to the vanilla arm if it
            // has no mesh for this skin so cancelling the pass never leaves the hand invisible.
            if (engine == null || !engine.renderBare(pose, collector, light)) {
                hands.jilaton$renderPlayerArm(pose, collector, light, 0, swing, player.getMainArm());
            }
            return;
        }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        boolean reduced = JilatonClient.clientConfig().reducedMotion();

        int slot = player.getInventory().getSelectedSlot();
        ItemStack held = player.getMainHandItem();
        if (slot != heldSlot || heldItem == null || !ItemStack.isSameItem(held, heldItem)) {
            if (heldSlot < 0) motion.startDraw(now);
            else motion.startSwitch(now);
        }
        heldSlot = slot;
        heldItem = held.copy();
        if (slashes.value() && swing > 0 && (lastSwing == 0 || swing + 0.3F < lastSwing)) motion.startSlash(now);
        lastSwing = swing;
        motion.look(player.getViewYRot(partial), player.getViewXRot(partial), dt, sway.value());

        // Same limb-swing phase the vanilla arms use (cos(position * 0.6662)), in half-steps.
        double walk = player.walkAnimation.position(partial) * 0.6662 / Math.PI;
        double moving = Mth.clamp(player.walkAnimation.speed(partial), 0, 1);
        KnifeMotion.Pose m = motion.pose(now, equipSeconds.value(), sway.value(), bob.value(), walk, moving,
                player.isSprinting(), player.isCrouching(), player.onGround(), player.getDeltaMovement().y, reduced);
        HumanoidArm arm = player.getMainArm();
        float sideSign = arm == HumanoidArm.RIGHT ? 1 : -1;

        pose.pushPose();
        pose.translate(sideSign * (m.x() + side.value() - REST_SIDE), m.y() + height.value() - REST_HEIGHT, m.z());
        // Rotate the whole rig about the hand, not the eye, so slashes sweep the blade rather than orbit the view.
        float pivotX = sideSign * 0.45F, pivotY = -0.45F, pivotZ = -0.8F;
        pose.translate(pivotX, pivotY, pivotZ);
        pose.mulPose(Axis.YP.rotationDegrees((float) (sideSign * m.yaw())));
        pose.mulPose(Axis.XP.rotationDegrees((float) m.pitch()));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (sideSign * m.roll())));
        pose.translate(-pivotX, -pivotY, -pivotZ);

        if (debugViewSpace) {
            pose.popPose();
            pose.pushPose();
            pose.translate(0.25, -0.25, -0.9);
            pose.mulPose(Axis.YP.rotationDegrees(150));
        } else hands.jilaton$renderPlayerArm(pose, collector, light, 0, 0, arm);

        Grip g = debugViewSpace ? new Grip(0, 0, 0, 0, 0, 0) : grip;
        pose.translate(sideSign * g.x(), g.y(), g.z());
        pose.mulPose(Axis.XP.rotationDegrees((float) g.rx()));
        pose.mulPose(Axis.YP.rotationDegrees((float) (sideSign * g.ry())));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (sideSign * g.rz())));
        pose.mulPose(Axis.ZP.rotationDegrees((float) m.spin()));
        AssetMesh.Fitted model = fitted(style.value());
        int accent = EffectPalette.resolve(palette.value(),
                dev.jilaton.client.gui.ClientTheme.resolveAccent(JilatonClient.clientConfig().themeId(),
                        JilatonClient.clientConfig().customAccent()), custom.value());
        HeldModelRenderer.submit(pose, collector, light, model.parts(), tag -> material(tag, accent),
                model.scaleFor(scale.value()) * (debugViewSpace ? 1 : handScale), opacity.value());
        pose.popPose();
    }

    private AssetMesh.Fitted fitted(String styleName) {
        if (fitted == null || !fittedKey.equals(styleName)) {
            fitted = AssetMesh.fit(ModelAssets.knife(styleName.toLowerCase(Locale.ROOT)), DRAWN_LENGTH);
            fittedKey = styleName;
        }
        return fitted;
    }

    /** Tag-to-colour mapping owned here, per {@code ModelAssets}: steel, edge bevel, grip, fittings. */
    private Material material(String tag, int accent) {
        if (materialAccent != accent) {
            materials.clear();
            materialAccent = accent;
        }
        return materials.computeIfAbsent(tag, key -> switch (key) {
            case "blade" -> new Material(EffectBatch.mixRgb(accent, 0x2E333C, 0.78),
                    EffectBatch.mixRgb(accent, 0xD8DFEA, 0.86), accent, 0.04);
            case "edge" -> new Material(EffectBatch.mixRgb(accent, 0x9AA5B4, 0.55),
                    EffectBatch.mixRgb(accent, 0xFFFFFF, 0.92), accent, 0.10);
            case "handle" -> new Material(EffectBatch.mixRgb(accent, 0x0A0B0F, 0.88),
                    EffectBatch.mixRgb(accent, 0x24272F, 0.78), accent, 0.0);
            default -> new Material(EffectBatch.mixRgb(accent, 0x2C3139, 0.62),
                    EffectBatch.mixRgb(accent, 0x9EA7B4, 0.62), accent, 0.28);
        });
    }
}
