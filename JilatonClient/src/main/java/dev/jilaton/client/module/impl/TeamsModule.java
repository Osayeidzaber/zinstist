package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DyedItemColor;

public final class TeamsModule extends ClientModule {
    private final BooleanSetting ignoreTeammates;
    private final BooleanSetting matchNameColor;
    private final BooleanSetting matchScoreboard;
    private final BooleanSetting matchPrefix;
    private final BooleanSetting armorHead;
    private final BooleanSetting armorChest;
    private final BooleanSetting armorLegs;
    private final BooleanSetting armorFeet;

    public TeamsModule() {
        super("teams", "Teams", "Filters scoreboard allies and matching name colors, prefixes or dyed armor.",
                ModuleCategory.COMBAT);
        ignoreTeammates = addSetting(new BooleanSetting("ignore_teammates", "Ignore teammates", true));
        matchNameColor = addSetting(new BooleanSetting("match_name_color", "Match same name color", false));
        matchScoreboard = addSetting(new BooleanSetting("match_scoreboard", "Match scoreboard team", true));
        matchPrefix = addSetting(new BooleanSetting("match_prefix", "Match name prefix", false));
        armorHead = addSetting(new BooleanSetting("armor_head", "Match helmet color", true));
        armorChest = addSetting(new BooleanSetting("armor_chest", "Match chestplate color", false));
        armorLegs = addSetting(new BooleanSetting("armor_legs", "Match leggings color", false));
        armorFeet = addSetting(new BooleanSetting("armor_feet", "Match boots color", false));
    }

    @Override
    public boolean enabledByDefault() {
        // Off by default: on most servers every player shares a rank colour and a tab-sorting scoreboard team,
        // so an always-on Teams filter marks everyone as a teammate and combat modules find no targets.
        return false;
    }

    public boolean ignoreTeammates() {
        return ignoreTeammates.value();
    }

    public boolean matches(Player self, Player candidate) {
        if (self == null || candidate == null || self == candidate) {
            return false;
        }
        TextColor selfColor = self.getDisplayName().getStyle().getColor();
        TextColor candidateColor = candidate.getDisplayName().getStyle().getColor();
        if (TeamsPolicy.isTeammate(matchScoreboard.value() && self.isAlliedTo(candidate),
                selfColor == null ? null : selfColor.getValue(),
                candidateColor == null ? null : candidateColor.getValue(), matchNameColor.value())) {
            return true;
        }
        if (matchPrefix.value() && TeamsPolicy.samePrefix(self.getDisplayName().getString(), candidate.getDisplayName().getString())) {
            return true;
        }
        return (armorHead.value() && sameArmorColor(self, candidate, EquipmentSlot.HEAD))
                || (armorChest.value() && sameArmorColor(self, candidate, EquipmentSlot.CHEST))
                || (armorLegs.value() && sameArmorColor(self, candidate, EquipmentSlot.LEGS))
                || (armorFeet.value() && sameArmorColor(self, candidate, EquipmentSlot.FEET));
    }

    public BooleanSetting matchNameColor() {
        return matchNameColor;
    }

    private static boolean sameArmorColor(Player self, Player candidate, EquipmentSlot slot) {
        return TeamsPolicy.sameExplicitColor(armorColor(self, slot), armorColor(candidate, slot));
    }

    private static Integer armorColor(Player player, EquipmentSlot slot) {
        DyedItemColor color = player.getItemBySlot(slot).get(DataComponents.DYED_COLOR);
        return color == null ? null : color.rgb();
    }
}
