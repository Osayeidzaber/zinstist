package dev.jilaton.client.render.character;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.module.impl.FreecamModule;
import dev.jilaton.client.module.impl.FriendManager;
import dev.jilaton.client.render.character.kit.CharacterDesign;
import dev.jilaton.client.render.character.kit.CharacterModel;
import dev.jilaton.client.render.character.kit.Detail;
import dev.jilaton.client.setting.BooleanSetting;
import dev.jilaton.client.setting.ChoiceSetting;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class CharacterModule extends ClientModule {
    private static final long FRIEND_REFRESH_NANOS = 1000000000L;
    private static final float MODEL_PIXEL = 0.05859375F;
    private static volatile CharacterModule active;
    private final ChoiceSetting character = this.addSetting(characterSetting());
    private final BooleanSetting friends = this.addSetting(new BooleanSetting("friends", "On Friends", false));
    private final ChoiceSetting detail = this.addSetting(
            new ChoiceSetting("detail", "Detail", List.of(Detail.LOW.label, Detail.MEDIUM.label, Detail.HIGH.label), Detail.MEDIUM.label)
    );
    private final BooleanSetting heldItems = this.addSetting(new BooleanSetting("held-items", "Held Items", false));
    private final Int2BooleanOpenHashMap friendCache = new Int2BooleanOpenHashMap();
    private long friendsCheckedAt;
    private int scanTicks;
    private String preparedCharacter;
    private String preparedDetail;
    private WeakReference<ClientLevel> level = new WeakReference<>(null);

    public CharacterModule() {
        super("character-3d", "3D Character", "Replaces your player model with a 3D character.", ModuleCategory.RENDER);
    }

    private static ChoiceSetting characterSetting() {
        List<String> names = CharacterCatalog.names();
        // Custom .glb characters are only discovered by refresh(), after saved settings load, so a saved
        // custom name is held until the scan offers it instead of being rejected and overwritten.
        return new ChoiceSetting("character", "Character", names, names.get(0)).deferUnknownValues();
    }

    private void refreshCharacters() {
        refreshCharacters(CustomCharacters.folder());
    }

    /**
     * Tick-path catalogue refresh with an explicit drop folder. Package-private so tests can prove the whole
     * selection chain without touching FabricLoader's config directory.
     */
    boolean refreshCharacters(java.nio.file.Path root) {
        boolean catalogChanged = CharacterCatalog.refresh(root);
        List<String> names = CharacterCatalog.names();
        if (catalogChanged || !names.equals(this.character.choices())) {
            this.character.setChoices(names);
            return true;
        }
        return false;
    }

    public static void ensureDropFolder() {
        CustomCharacters.ensureFolder();
    }

    private void prepare() {
        if (this.enabled()) {
            CharacterCache.prepare(CharacterCatalog.byName(this.character.value()), this.effectiveDetail());
        }
    }

    private void prepareIfChanged() {
        String name = this.character.value();
        String level = this.detail.value();
        if (!name.equals(this.preparedCharacter) || !level.equals(this.preparedDetail)) {
            this.preparedCharacter = name;
            this.preparedDetail = level;
            this.prepare();
        }
    }

    private Detail effectiveDetail() {
        return Detail.byLabel(this.detail.value());
    }

    @Override
    protected void onEnable(Minecraft client) {
        ClientModule headdress = JilatonClient.moduleManager().get("pharaoh_headdress");
        if (headdress != null) {
            headdress.setEnabled(false);
        }

        CharacterCache.listen();
        this.reset();
        ensureDropFolder();
        this.refreshCharacters();
        this.prepare();
        active = this;
    }

    @Override
    protected void onDisable(Minecraft client) {
        if (active == this) {
            active = null;
        }

        this.reset();
        CharacterCache.clear();
    }

    @Override
    protected void onTick(Minecraft client) {
        this.watchLevel();
        if (++this.scanTicks % 40 == 0) {
            this.refreshCharacters();
        }
        this.prepareIfChanged();
    }

    private void reset() {
        this.friendCache.clear();
        CharacterMotion.clear();
        this.level = new WeakReference<>(Minecraft.getInstance().level);
    }

    private void watchLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (this.level.get() != current) {
            this.reset();
            CharacterCache.releaseTextures();
        }
    }

    public static void extract(Entity entity, AvatarRenderState state) {
        CharacterModule module = active;
        WornCharacter worn = module == null ? null : module.resolve(state);
        CharacterHolder holder = (CharacterHolder) state;
        holder.anubis$setCharacter(worn);
        if (worn != null) {
            Entity vehicle = entity.getVehicle();
            holder.anubis$setSeat(vehicle == null ? 0.0F : (vehicle instanceof LivingEntity ? 1.35F : 0.3F));
            if (state.nameTagAttachment != null) {
                CharacterModel model = worn.model;
                float over = model.maxY * model.scale - 32.0F;
                if (over > 0.0F) {
                    state.nameTagAttachment = state.nameTagAttachment.add(0.0, over * MODEL_PIXEL * state.scale, 0.0);
                }
            }
        }
    }

    static void baking(String file) {
        CharacterModule module = active;
        if (module != null) {
            send("Preparing " + file + ", big models take a few seconds the first time");
        }
    }

    static void notice(String file, List<String> limbs) {
        CharacterModule module = active;
        if (module != null) {
            String message = limbs.isEmpty()
                    ? file + " has no skeleton the client recognises, so it moves as one piece"
                    : file + " only moves its " + String.join(" and ", limbs) + ", the rest of its skeleton wasn't recognised";
            send(message);
        }
    }

    /** One chat line for a drop-folder file the importer cannot read at all (.gltf scene files). */
    static void unsupportedModel(String file) {
        if (active != null) {
            send(file + " is a .gltf scene file; only self-contained .glb models are supported. Export it as a single .glb");
        }
    }

    static void failed(CharacterDesign design, Throwable failure) {
        CharacterModule module = active;
        if (module != null) {
            Throwable cause = failure instanceof UncheckedIOException && failure.getCause() != null ? failure.getCause() : failure;
            String reason = !(cause instanceof IOException) && !(cause instanceof IllegalStateException)
                    ? "the file couldn't be read, see the log"
                    : cause.getMessage();
            send("Couldn't load " + design.name() + ": " + reason);
        }
    }

    private static void send(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) {
            client.gui.getChat().addMessage(Component.literal("[Jilaton] " + message));
        }
    }

    static WornCharacter worn(EntityRenderState state) {
        return state instanceof CharacterHolder holder ? holder.anubis$character() : null;
    }

    static float seat(EntityRenderState state) {
        return state instanceof CharacterHolder holder ? holder.anubis$seat() : 0.0F;
    }

    public static boolean hidesModel(EntityRenderState state) {
        return worn(state) != null;
    }

    public static boolean hidesLayer(RenderLayer<?, ?> layer, EntityRenderState state) {
        return worn(state) == null
                ? false
                : layer instanceof HumanoidArmorLayer
                || layer instanceof CapeLayer
                || layer instanceof WingsLayer
                || layer instanceof StuckInBodyLayer
                || layer instanceof CustomHeadLayer
                || layer instanceof ParrotOnShoulderLayer
                || layer instanceof Deadmau5EarsLayer
                || layer instanceof ItemInHandLayer && !showsHeldItems();
    }

    private static boolean showsHeldItems() {
        CharacterModule module = active;
        return module != null && module.heldItems.value();
    }

    static WornCharacter firstPerson() {
        CharacterModule module = active;
        if (module == null) {
            return null;
        } else {
            LocalPlayer self = Minecraft.getInstance().player;
            if (self != null && !self.isSpectator()) {
                WornCharacter worn = CharacterCache.ready(CharacterCatalog.byName(module.character.value()), module.effectiveDetail());
                return worn != null && worn.model.firstPersonArms ? worn : null;
            } else {
                return null;
            }
        }
    }

    private WornCharacter resolve(AvatarRenderState state) {
        if (state.isSpectator) {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        return !this.wears(mc, state) ? null : CharacterCache.ready(CharacterCatalog.byName(this.character.value()), this.effectiveDetail());
    }

    private boolean wears(Minecraft mc, AvatarRenderState state) {
        LocalPlayer self = mc.player;
        if (self != null && mc.level != null) {
            if (state.id != self.getId()) {
                return this.friends.value() && this.isFriend(mc, state.id);
            } else {
                return !freecam(mc) && mc.options.getCameraType().isFirstPerson() ? mc.screen != null && !self.isSleeping() : true;
            }
        } else {
            return false;
        }
    }

    private static boolean freecam(Minecraft mc) {
        return JilatonClient.moduleManager().get("freecam") instanceof FreecamModule camera && camera.live(mc);
    }

    private boolean isFriend(Minecraft mc, int id) {
        long now = System.nanoTime();
        if (now - this.friendsCheckedAt > FRIEND_REFRESH_NANOS) {
            this.friendCache.clear();
            this.friendsCheckedAt = now;
        }

        if (this.friendCache.containsKey(id)) {
            return this.friendCache.get(id);
        }

        boolean friend = mc.level.getEntity(id) instanceof Player player && FriendManager.contains(player.getGameProfile().name());
        this.friendCache.put(id, friend);
        return friend;
    }
}
