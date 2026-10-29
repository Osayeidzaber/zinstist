package dev.jilaton.client.module.impl;

import dev.jilaton.client.module.ClientModule;
import dev.jilaton.client.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class FriendsModule extends ClientModule {
    public FriendsModule() {
        super("friends", "Friends", "Protects players in your friend list from combat targeting.", ModuleCategory.COMBAT);
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    protected void onTick(Minecraft client) {
        FriendHatRenderer.update(client);
    }

    @Override
    protected void onDisable(Minecraft client) {
        FriendHatRenderer.clear();
    }
}
