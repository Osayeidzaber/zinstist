package dev.jilaton.client.account;

import com.mojang.authlib.yggdrasil.ProfileResult;
import dev.jilaton.client.mixin.MinecraftSessionAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Switches the game session between the account Minecraft was launched with and offline names. It only switches
 * while no world is loaded (the title screen), and it remembers the launch session so it can always go back to it.
 */
public final class SessionSwitcher {
    private static User launchUser;
    private static CompletableFuture<ProfileResult> launchProfile;
    private static OfflineAccounts offline;

    private SessionSwitcher() { }

    public static OfflineAccounts offline() {
        if (offline == null)
            offline = new OfflineAccounts(FabricLoader.getInstance().getConfigDir().resolve("jilaton").resolve("accounts.txt"));
        return offline;
    }

    private static void rememberLaunch(Minecraft minecraft) {
        if (launchUser != null) return;
        launchUser = minecraft.getUser();
        launchProfile = ((MinecraftSessionAccessor) minecraft).jilaton$profileFuture();
    }

    /** The account the game was launched with. */
    public static User launchUser(Minecraft minecraft) {
        rememberLaunch(minecraft);
        return launchUser;
    }

    /** Whether the session can change now: only with no world loaded. */
    public static boolean canSwitch(Minecraft minecraft) {
        return minecraft.level == null && minecraft.getSingleplayerServer() == null;
    }

    /** Whether the launch account is the one in use. */
    public static boolean usingLaunch(Minecraft minecraft) {
        return launchUser == null || minecraft.getUser() == launchUser;
    }

    /** Switches to an offline {@code name}. */
    public static boolean useOffline(Minecraft minecraft, String name) {
        if (!canSwitch(minecraft) || !OfflineAccounts.valid(name)) return false;
        rememberLaunch(minecraft);
        var accessor = (MinecraftSessionAccessor) minecraft;
        accessor.jilaton$setUser(new User(name, UUIDUtil.createOfflinePlayerUUID(name), "", Optional.empty(), Optional.empty()));
        accessor.jilaton$setProfileFuture(CompletableFuture.completedFuture(new ProfileResult(UUIDUtil.createOfflineProfile(name))));
        return true;
    }

    /** Switches back to the launch account. */
    public static boolean useLaunch(Minecraft minecraft) {
        if (!canSwitch(minecraft) || launchUser == null) return false;
        var accessor = (MinecraftSessionAccessor) minecraft;
        accessor.jilaton$setUser(launchUser);
        accessor.jilaton$setProfileFuture(launchProfile);
        return true;
    }
}
