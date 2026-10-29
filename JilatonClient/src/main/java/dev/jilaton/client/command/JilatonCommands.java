package dev.jilaton.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.module.impl.FriendManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class JilatonCommands {
    private JilatonCommands() {
    }

    public static void register(ClientConfig config) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher, config));
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, ClientConfig config) {
        dispatcher.register(ClientCommandManager.literal("jilaton")
                .then(ClientCommandManager.literal("friend")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(context -> addFriend(config, context.getSource(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(context -> removeFriend(config, context.getSource(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(ClientCommandManager.literal("list").executes(context -> {
                            String friends = String.join(", ", FriendManager.names());
                            context.getSource().sendFeedback(Component.literal(
                                    friends.isEmpty() ? "Jilaton friends: none" : "Jilaton friends: " + friends));
                            return 1;
                        }))));
    }

    private static int addFriend(ClientConfig config, FabricClientCommandSource source, String name) {
        try {
            boolean added = FriendManager.add(name);
            config.save(dev.jilaton.client.JilatonClient.moduleManager());
            source.sendFeedback(Component.literal(added ? "Added " + name + " to friends"
                    : name + " is already a friend"));
            return 1;
        } catch (IllegalArgumentException exception) {
            source.sendFeedback(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int removeFriend(ClientConfig config, FabricClientCommandSource source, String name) {
        try {
            boolean removed = FriendManager.remove(name);
            config.save(dev.jilaton.client.JilatonClient.moduleManager());
            source.sendFeedback(Component.literal(removed
                    ? "Removed " + name + " from friends" : name + " was not in your friends"));
            return 1;
        } catch (IllegalArgumentException exception) {
            source.sendFeedback(Component.literal(exception.getMessage()));
            return 0;
        }
    }
}
