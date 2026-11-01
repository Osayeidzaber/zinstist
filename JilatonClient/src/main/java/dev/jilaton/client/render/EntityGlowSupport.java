package dev.jilaton.client.render;

import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.ClientTheme;
import dev.jilaton.client.module.impl.ItemEspModule;
import dev.jilaton.client.module.impl.PlayerEspModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Visual-only native glow selection; never modifies server-synchronized entity flags or team data. */
public final class EntityGlowSupport {
    private EntityGlowSupport() { }

    public static Integer color(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || entity.level() != client.level) return null;
        var config = JilatonClient.clientConfig();
        int accent = ClientTheme.resolveAccent(config.themeId(), config.customAccent());
        var camera = client.gameRenderer.getMainCamera().position();
        double seconds = System.nanoTime() / 1_000_000_000.0;
        if (entity instanceof LivingEntity living && JilatonClient.moduleManager().get("player_esp") instanceof PlayerEspModule esp
                && esp.enabled() && esp.glow() && esp.options().targets().contains(living)
                && esp.options().matches(entity, client, camera)
                && (esp.options().throughWalls() || client.player.hasLineOfSight(entity))) {
            return esp.options().color(living, accent, camera, seconds);
        }
        if (JilatonClient.moduleManager().get("target_esp") instanceof dev.jilaton.client.module.impl.TargetEspModule targetEsp) {
            Integer outline = targetEsp.outlineColor(entity, accent);
            if (outline != null) return outline;
        }
        if (JilatonClient.moduleManager().get("item_esp") instanceof ItemEspModule items && items.enabled() && items.glow()
                && items.targets().contains(entity) && items.matches(entity, client, camera)
                && (items.throughWalls() || client.player.hasLineOfSight(entity))) return items.color(accent, seconds);
        return null;
    }
}
