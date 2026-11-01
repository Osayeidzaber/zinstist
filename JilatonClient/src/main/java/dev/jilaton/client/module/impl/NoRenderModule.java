package dev.jilaton.client.module.impl;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;

/**
 * Hides screen effects that block your view. The option set follows LiquidBounce's AntiBlind (GPL-3.0, CCBlueX);
 * the hooks are Jilaton's own mixins on the 1.21.11 renderers ({@code NoRenderMixin}).
 */
public final class NoRenderModule extends ClientModule {
    public enum Part { HURT_CAM, PUMPKIN, POWDER_SNOW, PORTAL, NAUSEA, VIGNETTE, SPYGLASS, BOSS_BARS, TITLES, UNDERWATER, IN_WALL, TOTEM }

    private final BooleanSetting hurtCam = addSetting(new BooleanSetting("hurt_cam", "Hurt camera shake", true));
    private final BooleanSetting pumpkin = addSetting(new BooleanSetting("pumpkin", "Pumpkin overlay", true));
    private final BooleanSetting powderSnow = addSetting(new BooleanSetting("powder_snow", "Powder snow overlay", true));
    private final BooleanSetting portal = addSetting(new BooleanSetting("portal", "Nether portal overlay", true));
    private final BooleanSetting nausea = addSetting(new BooleanSetting("nausea", "Nausea overlay", true));
    private final BooleanSetting vignette = addSetting(new BooleanSetting("vignette", "Vignette", false));
    private final BooleanSetting spyglass = addSetting(new BooleanSetting("spyglass", "Spyglass scope", false));
    private final BooleanSetting bossBars = addSetting(new BooleanSetting("boss_bars", "Boss bars", false));
    private final BooleanSetting titles = addSetting(new BooleanSetting("titles", "Titles", false));
    private final BooleanSetting underwater = addSetting(new BooleanSetting("underwater", "Underwater overlay", true));
    private final BooleanSetting inWall = addSetting(new BooleanSetting("in_wall", "In-wall overlay", true));
    private final BooleanSetting totem = addSetting(new BooleanSetting("totem", "Totem pop animation", false));

    public NoRenderModule() {
        super("no_render", "NoRender", "Hides hurt shake, pumpkin, portal, nausea and other view-blocking overlays.",
                ModuleCategory.RENDER);
    }

    /** True when the part should be hidden this frame. */
    public static boolean hides(Part part) {
        if (JilatonClient.moduleManager() == null
                || !(JilatonClient.moduleManager().get("no_render") instanceof NoRenderModule m) || !m.enabled()) return false;
        return switch (part) {
            case HURT_CAM -> m.hurtCam.value();
            case PUMPKIN -> m.pumpkin.value();
            case POWDER_SNOW -> m.powderSnow.value();
            case PORTAL -> m.portal.value();
            case NAUSEA -> m.nausea.value();
            case VIGNETTE -> m.vignette.value();
            case SPYGLASS -> m.spyglass.value();
            case BOSS_BARS -> m.bossBars.value();
            case TITLES -> m.titles.value();
            case UNDERWATER -> m.underwater.value();
            case IN_WALL -> m.inWall.value();
            case TOTEM -> m.totem.value();
        };
    }
}
