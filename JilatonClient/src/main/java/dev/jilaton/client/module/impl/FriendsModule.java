package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;

public final class FriendsModule extends ClientModule {
    public FriendsModule() {
        super("friends", "Friends",
                "Middle-click a player to add or remove them. Friends wear a blue hat and are protected from combat targeting.",
                ModuleCategory.COMBAT);
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

}
