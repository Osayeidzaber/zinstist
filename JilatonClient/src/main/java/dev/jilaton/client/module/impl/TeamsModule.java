package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import dev.jilaton.client.setting.BooleanSetting;

public final class TeamsModule extends ClientModule {
    private final BooleanSetting ignoreTeammates;

    public TeamsModule() {
        super("teams", "Teams", "Filters allied players from combat targeting.", ModuleCategory.COMBAT);
        ignoreTeammates = addSetting(new BooleanSetting("ignore_teammates", "Ignore teammates", true));
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    public boolean ignoreTeammates() {
        return ignoreTeammates.value();
    }
}
