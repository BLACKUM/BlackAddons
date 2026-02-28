package org.blackum.blackaddons.core.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.chat.ChatUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.blackum.blackaddons.core.util.MinecraftInstance.mc;

public class CommandUtils {

    private static CommandDispatcher<FabricClientCommandSource> activeDispatcher;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        activeDispatcher = dispatcher;
        ClientTickEvents.END_CLIENT_TICK.register(CommandUtils::onEndTick);

        ConfigManager.data.knownAliases.forEach((name, command) -> {
            registerAlias(dispatcher, name, command);
        });
    }

    private static void registerAlias(CommandDispatcher<FabricClientCommandSource> dispatcher, String name,
            String command) {
        String cleanedCommand = command.startsWith("/") ? command.substring(1) : command;
        String[] parts = cleanedCommand.trim().split("\\s+");
        CommandNode<FabricClientCommandSource> targetNode = dispatcher.getRoot();
        boolean simpleLiteral = true;

        for (String part : parts) {
            CommandNode<FabricClientCommandSource> child = targetNode.getChild(part);
            if (child instanceof LiteralCommandNode) {
                targetNode = child;
            } else {
                simpleLiteral = false;
                break;
            }
        }

        if (simpleLiteral && targetNode != dispatcher.getRoot() && parts.length > 0) {
            dispatcher.register(ClientCommandManager.literal(name)
                    .redirect(targetNode));
        } else {
            dispatcher.register(ClientCommandManager.literal(name)
                    .executes(context -> {
                        if (!ConfigManager.data.knownAliases.containsKey(name)) {
                            return 0;
                        }
                        assert mc.player != null;
                        mc.player.connection.sendCommand(cleanedCommand);
                        return 1;
                    })
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                if (activeDispatcher == null || mc.player == null)
                                    return builder.buildFuture();
                                String remaining = builder.getRemaining();
                                String fullCommand = cleanedCommand + " " + remaining;
                                return activeDispatcher.getCompletionSuggestions(
                                        activeDispatcher.parse(fullCommand, context.getSource()),
                                        fullCommand.length() - remaining.length());
                            })
                            .executes(context -> {
                                if (!ConfigManager.data.knownAliases.containsKey(name)) {
                                    return 0;
                                }
                                String args = StringArgumentType.getString(context, "args");
                                assert mc.player != null;
                                mc.player.connection.sendCommand(cleanedCommand + " " + args);
                                return 1;
                            })));
        }
    }

    static void onEndTick(Minecraft client) {
    }

    private static void removeCommandNode(CommandNode<?> root, String name) {
        if (root == null)
            return;
        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            Map<?, ?> children = (Map<?, ?>) childrenField.get(root);
            children.remove(name);

            Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            Map<?, ?> literals = (Map<?, ?>) literalsField.get(root);
            literals.remove(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static ArgumentBuilder<FabricClientCommandSource, ?> add = ClientCommandManager.literal("add")
            .then(ClientCommandManager.argument("alias", StringArgumentType.string())
                    .then(ClientCommandManager.argument("real_command", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "alias");
                                String desc = StringArgumentType.getString(ctx, "real_command");

                                ConfigManager.data.knownAliases.put(name, desc);
                                ConfigManager.save();

                                if (activeDispatcher != null) {
                                    registerAlias(activeDispatcher, name, desc);
                                }

                                if (mc.player != null && mc.player.connection != null) {
                                    CommandDispatcher<ClientSuggestionProvider> chatDispatcher = mc.player.connection
                                            .getCommands();
                                    if (chatDispatcher != null) {
                                        chatDispatcher.register(
                                                LiteralArgumentBuilder.<ClientSuggestionProvider>literal(name));
                                    }
                                }

                                ChatUtils.send_debug("Added: " + name + " -> " + desc);

                                return 1;
                            })));

    static ArgumentBuilder<FabricClientCommandSource, ?> del = ClientCommandManager.literal("del")
            .then(ClientCommandManager.argument("alias", StringArgumentType.string())
                    .suggests((ctx, builder) -> {
                        List<String> existing = new ArrayList<>();

                        ConfigManager.data.knownAliases.forEach((alias, command) -> {
                            existing.add(alias);
                        });
                        ConfigManager.save();

                        return SharedSuggestionProvider.suggest(existing, builder);
                    })
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "alias");

                        ConfigManager.data.knownAliases.remove(name);
                        ConfigManager.save();

                        if (activeDispatcher != null) {
                            removeCommandNode(activeDispatcher.getRoot(), name);
                        }

                        if (mc.player != null && mc.player.connection != null) {
                            CommandDispatcher<ClientSuggestionProvider> chatDispatcher = mc.player.connection
                                    .getCommands();
                            if (chatDispatcher != null) {
                                removeCommandNode(chatDispatcher.getRoot(), name);
                            }
                        }

                        ChatUtils.send_debug("Removed: " + name);
                        return 1;
                    }));

    static ArgumentBuilder<FabricClientCommandSource, ?> list = ClientCommandManager.literal("list")
            .executes(ctx -> {
                ChatUtils.send_debug("Aliases: ");
                ConfigManager.data.knownAliases.forEach((alias, command) -> {
                    ChatUtils.send_debug(alias + " -> " + command);
                });

                return 1;
            });

    public static LiteralArgumentBuilder<FabricClientCommandSource> subcommand = ClientCommandManager
            .literal("commandaliases")
            .then(add)
            .then(del)
            .then(list);
}
