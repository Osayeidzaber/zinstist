package dev.jilaton.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.jilaton.client.config.ClientConfig;
import dev.jilaton.client.JilatonClient;
import dev.jilaton.client.gui.HudEditorScreen;
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
                .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.argument("module", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    JilatonClient.moduleManager().all().stream().filter(module -> module.id().startsWith(builder.getRemainingLowerCase()))
                                            .forEach(module -> builder.suggest(module.id()));
                                    return builder.buildFuture();
                                })
                                .then(ClientCommandManager.argument("setting", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            var module = JilatonClient.moduleManager().get(StringArgumentType.getString(context, "module"));
                                            if (module != null) module.settings().stream().filter(setting -> setting.id().startsWith(builder.getRemainingLowerCase()))
                                                    .forEach(setting -> builder.suggest(setting.id()));
                                            return builder.buildFuture();
                                        })
                                        .then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    var module = JilatonClient.moduleManager().get(StringArgumentType.getString(context, "module"));
                                                    String id = StringArgumentType.getString(context, "setting");
                                                    var setting = module == null ? null : module.settings().stream().filter(candidate -> candidate.id().equals(id)).findFirst().orElse(null);
                                                    if (setting == null) { context.getSource().sendError(Component.literal("Unknown module or setting")); return 0; }
                                                    try {
                                                        String actual = dev.jilaton.client.setting.SettingValueEditor.apply(setting, StringArgumentType.getString(context, "value"));
                                                        config.save(JilatonClient.moduleManager());
                                                        context.getSource().sendFeedback(Component.literal(module.name() + " / " + setting.label() + " = " + actual));
                                                        return 1;
                                                    } catch (IllegalArgumentException failure) {
                                                        context.getSource().sendError(Component.literal(failure.getMessage()));
                                                        return 0;
                                                    }
                                                })))))
                .then(ClientCommandManager.literal("waypoint")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            var module = dev.jilaton.client.module.impl.WaypointsModule.get();
                                            var client = context.getSource().getClient();
                                            if (module == null || client.player == null) return 0;
                                            var w = module.add(client, StringArgumentType.getString(context, "name"));
                                            context.getSource().sendFeedback(Component.literal("Waypoint " + w.name() + " set at "
                                                    + w.x() + " " + w.y() + " " + w.z()));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            var module = dev.jilaton.client.module.impl.WaypointsModule.get();
                                            var client = context.getSource().getClient();
                                            if (module != null) module.store().in(dev.jilaton.client.module.impl.WaypointsModule.worldKey(client),
                                                    dev.jilaton.client.module.impl.WaypointsModule.dimensionKey(client)).forEach(w -> builder.suggest(w.name()));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            var module = dev.jilaton.client.module.impl.WaypointsModule.get();
                                            var client = context.getSource().getClient();
                                            String name = StringArgumentType.getString(context, "name");
                                            boolean removed = module != null && module.store().remove(
                                                    dev.jilaton.client.module.impl.WaypointsModule.worldKey(client),
                                                    dev.jilaton.client.module.impl.WaypointsModule.dimensionKey(client), name);
                                            if (removed) context.getSource().sendFeedback(Component.literal("Removed waypoint " + name));
                                            else context.getSource().sendError(Component.literal("No waypoint named " + name + " here"));
                                            return removed ? 1 : 0;
                                        })))
                        .then(ClientCommandManager.literal("list").executes(context -> {
                            var module = dev.jilaton.client.module.impl.WaypointsModule.get();
                            var client = context.getSource().getClient();
                            if (module == null) return 0;
                            var list = module.store().in(dev.jilaton.client.module.impl.WaypointsModule.worldKey(client),
                                    dev.jilaton.client.module.impl.WaypointsModule.dimensionKey(client));
                            if (list.isEmpty()) context.getSource().sendFeedback(Component.literal("No waypoints in this dimension"));
                            for (var w : list) context.getSource().sendFeedback(Component.literal(w.name() + ": " + w.x() + " " + w.y() + " " + w.z()));
                            return list.size();
                        })))
                .then(ClientCommandManager.literal("gui").executes(context -> {
                    context.getSource().getClient().execute(() -> JilatonClient.openClickGui(null));
                    return 1;
                }))
                .then(ClientCommandManager.literal("music").executes(context -> {
                    var client = net.minecraft.client.Minecraft.getInstance();
                    client.execute(() -> client.setScreen(new dev.jilaton.client.gui.MusicCenterScreen(null)));
                    return 1;
                }))
                .then(ClientCommandManager.literal("hud").executes(context -> {
                    context.getSource().getClient().execute(() -> context.getSource().getClient().setScreen(
                            new HudEditorScreen(JilatonClient.moduleManager(), config, null)));
                    return 1;
                }))
                .then(ClientCommandManager.literal("toggle")
                        .then(ClientCommandManager.argument("module", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String remaining = builder.getRemainingLowerCase();
                                    JilatonClient.moduleManager().all().stream()
                                            .filter(module -> module.id().startsWith(remaining))
                                            .forEach(module -> builder.suggest(module.id()));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    var module = JilatonClient.moduleManager().get(StringArgumentType.getString(context, "module"));
                                    if (module == null) {
                                        context.getSource().sendError(Component.literal("Unknown Jilaton module"));
                                        return 0;
                                    }
                                    module.toggle();
                                    config.save(JilatonClient.moduleManager());
                                    context.getSource().sendFeedback(Component.literal(module.name()
                                            + (module.enabled() ? " enabled" : " disabled")));
                                    return 1;
                                })))
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
