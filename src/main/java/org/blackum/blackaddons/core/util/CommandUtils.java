package org.blackum.blackaddons.core.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.chat.ChatUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.blackum.blackaddons.core.util.MinecraftInstance.mc;

public class CommandUtils {

    private static CommandDispatcher<FabricClientCommandSource> activeDispatcher;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        activeDispatcher = dispatcher;
        ClientTickEvents.END_CLIENT_TICK.register(CommandUtils::onEndTick);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> syncAliasesToChat());

        ConfigManager.data.knownAliases.forEach((name, command) -> registerAlias(dispatcher, name, command));
    }

    private static void syncAliasesToChat() {
        if (mc.player == null || mc.player.connection == null) {
            return;
        }

        CommandDispatcher<ClientSuggestionProvider> chatDispatcher = mc.player.connection.getCommands();
        if (chatDispatcher == null) {
            return;
        }

        ConfigManager.data.knownAliases.forEach((name, command) -> {
            String cleanedCommand = command.startsWith("/") ? command.substring(1) : command;
            String usage = getUsageHint(cleanedCommand);
            
            chatDispatcher.register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal(name)
                    .then(RequiredArgumentBuilder.<ClientSuggestionProvider, String>argument(usage, StringArgumentType.greedyString())
                            .suggests((context, builder) -> getAliasSuggestions(cleanedCommand, builder, context.getSource()))));
        });
    }

    private static void registerAlias(CommandDispatcher<FabricClientCommandSource> dispatcher, String name,
            String command) {
        String cleanedCommand = command.startsWith("/") ? command.substring(1) : command;
        String usage = getUsageHint(cleanedCommand);

        dispatcher.register(ClientCommandManager.literal(name)
                .executes(context -> {
                    if (!ConfigManager.data.knownAliases.containsKey(name)) {
                        return 0;
                    }
                    executeUnified(cleanedCommand, context.getSource());
                    return 1;
                })
                .then(ClientCommandManager.argument(usage, StringArgumentType.greedyString())
                        .suggests((context, builder) -> getAliasSuggestions(cleanedCommand, builder, context.getSource()))
                        .executes(context -> {
                            if (!ConfigManager.data.knownAliases.containsKey(name)) {
                                return 0;
                            }
                            String args = StringArgumentType.getString(context, usage);
                            executeUnified(cleanedCommand + " " + args, context.getSource());
                            return 1;
                        })));
    }

    private static String getUsageHint(String cleanedCommand) {
        if (mc.player == null || mc.player.connection == null) {
            return "args";
        }

        try {
            CommandDispatcher<?> dispatcher;
            Object source;

            String firstWord = cleanedCommand.split("\\s+")[0];
            if (activeDispatcher != null && activeDispatcher.getRoot().getChild(firstWord) != null) {
                dispatcher = activeDispatcher;
                source = mc.player.connection.getSuggestionsProvider(); 
            } else {
                dispatcher = mc.player.connection.getCommands();
                source = mc.player.connection.getSuggestionsProvider();
            }

            if (dispatcher == null) return "args";

            @SuppressWarnings("unchecked")
            CommandDispatcher<Object> casted = (CommandDispatcher<Object>) dispatcher;
            ParseResults<Object> parse = casted.parse(cleanedCommand, source);
            List<ParsedCommandNode<Object>> nodes = parse.getContext().getNodes();
            
            if (nodes.isEmpty()) return "args";
            
            CommandNode<Object> lastNode = nodes.get(nodes.size() - 1).getNode();
            Map<CommandNode<Object>, String> usageMap = casted.getSmartUsage(lastNode, source);
            
            if (usageMap.isEmpty()) return "args";
            
            return String.join(" ", usageMap.values());
        } catch (Exception e) {
            return "args";
        }
    }

    private static CompletableFuture<Suggestions> getAliasSuggestions(String cleanedCommand, SuggestionsBuilder builder, Object source) {
        if (mc.player == null || mc.player.connection == null) {
            return builder.buildFuture();
        }

        String remaining = builder.getRemaining();
        String virtualCommand = cleanedCommand + " " + remaining;

        CommandDispatcher<?> dispatcher = null;
        Object dispatcherSource = null;

        String firstWord = cleanedCommand.split("\\s+")[0];
        if (activeDispatcher != null && activeDispatcher.getRoot().getChild(firstWord) != null) {
            dispatcher = activeDispatcher;
            dispatcherSource = (source instanceof FabricClientCommandSource fs) ? fs : (FabricClientCommandSource) mc.player.connection.getSuggestionsProvider();
        } else {
            dispatcher = mc.player.connection.getCommands();
            dispatcherSource = mc.player.connection.getSuggestionsProvider();
        }

        if (dispatcher == null || dispatcherSource == null) {
            return builder.buildFuture();
        }

        try {
            @SuppressWarnings("unchecked")
            CommandDispatcher<Object> castedDispatcher = (CommandDispatcher<Object>) dispatcher;

            int virtualCursor = virtualCommand.length();

            return castedDispatcher.getCompletionSuggestions(
                    castedDispatcher.parse(virtualCommand, dispatcherSource),
                    virtualCursor).thenApply(s -> {
                        for (Suggestion suggestion : s.getList()) {
                            builder.suggest(suggestion.getText(), suggestion.getTooltip());
                        }
                        return builder.build();
                    });
        } catch (Exception ignored) {
            return builder.buildFuture();
        }
    }

    private static void executeUnified(String command, FabricClientCommandSource source) {
        if (mc.player == null) {
            return;
        }

        String cleaned = command.startsWith("/") ? command.substring(1) : command;

        if (activeDispatcher != null) {
            String firstWord = cleaned.split("\\s+")[0];
            if (activeDispatcher.getRoot().getChild(firstWord) != null) {
                try {
                    activeDispatcher.execute(cleaned, source);
                    return;
                } catch (Exception ignored) {
                }
            }
        }

        mc.player.connection.sendCommand(cleaned);
    }

    static void onEndTick(Minecraft client) {
    }

    private static void removeCommandNode(CommandNode<?> root, String name) {
        if (root == null) {
            return;
        }
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

                                syncAliasesToChat();

                                ChatUtils.send_debug("Added: " + name + " -> " + desc);

                                return 1;
                            })));

    static ArgumentBuilder<FabricClientCommandSource, ?> del = ClientCommandManager.literal("del")
            .then(ClientCommandManager.argument("alias", StringArgumentType.string())
                    .suggests((ctx, builder) -> {
                        List<String> existing = new ArrayList<>();

                        ConfigManager.data.knownAliases.forEach((alias, command) -> existing.add(alias));
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
                ConfigManager.data.knownAliases.forEach((alias, command) -> ChatUtils.send_debug(alias + " -> " + command));

                return 1;
            });

    public static LiteralArgumentBuilder<FabricClientCommandSource> subcommand = ClientCommandManager
            .literal("commandaliases")
            .then(add)
            .then(del)
            .then(list);
}
