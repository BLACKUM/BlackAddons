package org.blackum.blackaddons.core.manager;

import org.blackum.blackaddons.core.manager.PartyFinderManager;
import com.mojang.brigadier.Command;
import net.minecraft.ChatFormatting;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.feature.dungeon.DungeonJoinHandler;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.CommandUtils;
import org.blackum.blackaddons.feature.rng.RngTracker;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.integration.BotIntegration;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.manager.ProfileStateManager;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.feature.chat.ChatActionExecutor;
import org.blackum.blackaddons.gui.screen.PartyFinderScreen;
import org.blackum.blackaddons.gui.screen.IrcScreen;
import org.blackum.blackaddons.gui.screen.ImagePreviewScreen;
import org.blackum.blackaddons.feature.chat.ChatActionManager;
import org.blackum.blackaddons.feature.chat.IrcClient;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.core.manager.RotationManager;
import java.util.UUID;

public class CommandManager {

        private static int handleIrcChatMode(FabricClientCommandSource source, boolean enabled) {
                ConfigManager.data.ircChatMode = enabled;
                ConfigManager.save();
                source.sendFeedback(enabled
                                ? ChatUtils.success("IRC chat mode enabled. Normal chat now goes to IRC.")
                                : ChatUtils.error("IRC chat mode disabled. Use the IRC prefix to chat in IRC."));
                return 1;
        }

        private static int sendIrcChatModeStatus(FabricClientCommandSource source) {
                boolean enabled = ConfigManager.data.ircChatMode;
                source.sendFeedback(enabled
                                ? ChatUtils.success("IRC chat mode is enabled.")
                                : ChatUtils.error("IRC chat mode is disabled."));
                return 1;
        }

        private static int handleActionTriggerMode(FabricClientCommandSource source, boolean enabled) {
                ConfigManager.data.actionTriggersEnabled = enabled;
                ConfigManager.save();
                if (!enabled) {
                        ChatActionExecutor.getInstance().clearPendingActions();
                }
                source.sendFeedback(enabled ? ChatUtils.success("Action triggers enabled.")
                                : ChatUtils.error("Action triggers disabled."));
                return 1;
        }

        private static int sendActionTriggerModeStatus(FabricClientCommandSource source) {
                boolean enabled = ConfigManager.data.actionTriggersEnabled;
                source.sendFeedback(enabled ? ChatUtils.success("Action triggers are enabled.")
                                : ChatUtils.error("Action triggers are disabled."));
                return 1;
        }

        private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> createActionTriggerModeNode(
                        String name) {
                return ClientCommandManager.literal(name)
                                .executes(ctx -> handleActionTriggerMode(ctx.getSource(),
                                                !ConfigManager.data.actionTriggersEnabled))
                                .then(ClientCommandManager.literal("on")
                                                .executes(ctx -> handleActionTriggerMode(ctx.getSource(), true)))
                                .then(ClientCommandManager.literal("off")
                                                .executes(ctx -> handleActionTriggerMode(ctx.getSource(), false)))
                                .then(ClientCommandManager.literal("toggle")
                                                .executes(ctx -> handleActionTriggerMode(ctx.getSource(),
                                                                !ConfigManager.data.actionTriggersEnabled)))
                                .then(ClientCommandManager.literal("status")
                                                .executes(ctx -> sendActionTriggerModeStatus(ctx.getSource())));
        }

        public static void register() {
                ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                        Command<FabricClientCommandSource> openGui = ctx -> {
                                if (Blackaddons.mainGuiOpener != null)
                                        Blackaddons.mainGuiOpener.run();
                                return 1;
                        };

                        var testNode = ClientCommandManager.literal("test");

                        testNode.then(ClientCommandManager.literal("DebugGui")
                                        .executes(ctx -> {
                                                if (Blackaddons.guiOpener != null)
                                                        Blackaddons.guiOpener.run();
                                                return 1;
                                        }));

                        testNode.then(ClientCommandManager.literal("TestMenu")
                                        .executes(ctx -> {
                                                if (Blackaddons.testMenuOpener != null)
                                                        Blackaddons.testMenuOpener.run();
                                                return 1;
                                        }));

                        testNode.then(ClientCommandManager.literal("rotate")
                                        .then(ClientCommandManager
                                                        .argument("yaw", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(-180, 180))
                                                        .then(ClientCommandManager
                                                                        .argument("pitch", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(-90, 90))
                                                                        .executes(ctx -> {
                                                                                float yaw = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "yaw");
                                                                                float pitch = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "pitch");
                                                                                RotationManager.getInstance().rotateTo(yaw, pitch);
                                                                                ctx.getSource().sendFeedback(Component.literal("Rotating to yaw=" + yaw + " pitch=" + pitch));
                                                                                return 1;
                                                                        }))));

                        testNode.then(ClientCommandManager.literal("rotateTo")
                                        .then(ClientCommandManager
                                                        .argument("x", com.mojang.brigadier.arguments.FloatArgumentType.floatArg())
                                                        .then(ClientCommandManager
                                                                        .argument("y", com.mojang.brigadier.arguments.FloatArgumentType.floatArg())
                                                                        .then(ClientCommandManager
                                                                                        .argument("z", com.mojang.brigadier.arguments.FloatArgumentType.floatArg())
                                                                                        .executes(ctx -> {
                                                                                                float x = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "x");
                                                                                                float y = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "y");
                                                                                                float z = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "z");
                                                                                                RotationManager.getInstance().rotateToBlock(x, y, z);
                                                                                                ctx.getSource().sendFeedback(Component.literal("Rotating to block " + x + " " + y + " " + z));
                                                                                                return 1;
                                                                                        })))));

                        testNode.then(ClientCommandManager.literal("rng")
                                        .then(ClientCommandManager
                                                        .argument(Constants.CMD_ARG_TYPE, StringArgumentType.string())
                                                        .suggests((context, builder) -> SharedSuggestionProvider
                                                                        .suggest(new String[] {
                                                                                        Constants.DROP_TYPE_RARE,
                                                                                        Constants.DROP_TYPE_CRAZY,
                                                                                        Constants.DROP_TYPE_PRAY },
                                                                                        builder))
                                                        .then(ClientCommandManager
                                                                        .argument(Constants.CMD_ARG_MAGIC_FIND,
                                                                                        IntegerArgumentType.integer(0))
                                                                        .then(ClientCommandManager
                                                                                        .argument(Constants.CMD_ARG_ITEM,
                                                                                                        StringArgumentType
                                                                                                                        .greedyString())
                                                                                        .executes(context -> {
                                                                                                String typeArg = StringArgumentType
                                                                                                                .getString(context,
                                                                                                                                Constants.CMD_ARG_TYPE)
                                                                                                                .toLowerCase();
                                                                                                int mf = IntegerArgumentType
                                                                                                                .getInteger(context,
                                                                                                                                Constants.CMD_ARG_MAGIC_FIND);
                                                                                                String item = StringArgumentType
                                                                                                                .getString(context,
                                                                                                                                Constants.CMD_ARG_ITEM);

                                                                                                String typePrefix = ChatFormatting.GOLD
                                                                                                                + ""
                                                                                                                + ChatFormatting.BOLD
                                                                                                                + "RARE";
                                                                                                if (typeArg.equals(
                                                                                                                Constants.DROP_TYPE_CRAZY))
                                                                                                        typePrefix = ChatFormatting.LIGHT_PURPLE
                                                                                                                        + ""
                                                                                                                        + ChatFormatting.BOLD
                                                                                                                        + "CRAZY RARE";
                                                                                                else if (typeArg.equals(
                                                                                                                Constants.DROP_TYPE_PRAY))
                                                                                                        typePrefix = ChatFormatting.DARK_PURPLE
                                                                                                                        + ""
                                                                                                                        + ChatFormatting.BOLD
                                                                                                                        + "PRAY TO RNGESUS";

                                                                                                String fakeMessage = typePrefix
                                                                                                                + " DROP! "
                                                                                                                + ChatFormatting.RESET
                                                                                                                + ""
                                                                                                                + ChatFormatting.WHITE
                                                                                                                + item
                                                                                                                + " "
                                                                                                                + ChatFormatting.RESET
                                                                                                                + ""
                                                                                                                + ChatFormatting.AQUA
                                                                                                                + "(+"
                                                                                                                + ChatFormatting.RESET
                                                                                                                + ""
                                                                                                                + ChatFormatting.AQUA
                                                                                                                + mf
                                                                                                                + "% "
                                                                                                                + ChatFormatting.RESET
                                                                                                                + ""
                                                                                                                + ChatFormatting.AQUA
                                                                                                                + "✯ Magic Find"
                                                                                                                + ChatFormatting.RESET
                                                                                                                + ""
                                                                                                                + ChatFormatting.AQUA
                                                                                                                + ")";

                                                                                                Minecraft.getInstance().gui
                                                                                                                .getChat()
                                                                                                                .addMessage(Component
                                                                                                                                .literal(fakeMessage));
                                                                                                RngTracker.onChatMessage(
                                                                                                                Component.literal(
                                                                                                                                fakeMessage));

                                                                                                NotificationManager
                                                                                                                .addNotification(
                                                                                                                                "RNG Drop Tested",
                                                                                                                                item + " (" + typeArg
                                                                                                                                                + ")",
                                                                                                                                NotificationType.SUCCESS);
                                                                                                return 1;
                                                                                        })))));

                        testNode.then(ClientCommandManager.literal("GiveTNT")
                                        .executes(ctx -> {
                                                Minecraft client = Minecraft.getInstance();
                                                var player = client.player;
                                                if (player != null) {
                                                        var connection = player.connection;
                                                        if (connection != null) {
                                                                connection.sendCommand(
                                                                                "give @s tnt[custom_name='\"Superboom TNT\"'] 64");
                                                                connection.sendCommand(
                                                                                "give @s tnt[custom_name='\"Infinityboom TNT\"'] 64");
                                                                connection.sendCommand(
                                                                                "give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ cracked_stone_bricks\",auto:1b}] 1");
                                                                connection.sendCommand(
                                                                                "give @s repeating_command_block[block_entity_data={id:\"minecraft:command_block\",Command:\"setblock ~ ~1 ~ smooth_stone_slab\",auto:1b}] 1");
                                                        }
                                                }
                                                return 1;
                                        }));

                        testNode.then(ClientCommandManager.literal("allnames")
                                        .executes(ctx -> {
                                                Minecraft client = Minecraft.getInstance();
                                                if (client.level != null && client.getConnection() != null) {
                                                        client.execute(() -> {
                                                                net.minecraft.world.scores.Scoreboard scoreboard = client.level
                                                                                .getScoreboard();
                                                                net.minecraft.world.scores.Objective obj = scoreboard
                                                                                .getObjective("allNamesObj");
                                                                if (obj == null) {
                                                                        obj = scoreboard.addObjective(
                                                                                        "allNamesObj",
                                                                                        net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                                                                                        Component.literal(
                                                                                                        "Test"),
                                                                                        net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER,
                                                                                        true, null);
                                                                }
                                                                scoreboard.setDisplayObjective(
                                                                                net.minecraft.world.scores.DisplaySlot.SIDEBAR,
                                                                                obj);

                                                                int score = 0;
                                                                for (net.minecraft.client.multiplayer.PlayerInfo info : client
                                                                                .getConnection().getOnlinePlayers()) {
                                                                        String name = info.getProfile().name();
                                                                        scoreboard.getOrCreatePlayerScore(() -> name,
                                                                                        obj).set(score++);
                                                                }

                                                                NotificationManager.addNotification(
                                                                                "Test",
                                                                                "Created scoreboard with all online players.",
                                                                                NotificationType.SUCCESS);
                                                        });
                                                }
                                                return 1;
                                        }));

                        testNode.then(ClientCommandManager.literal("dungeonjoin")
                                        .then(ClientCommandManager
                                                        .argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                                                        .executes(ctx -> {
                                                                String ign = StringArgumentType.getString(ctx,
                                                                                Constants.CMD_ARG_IGN);
                                                                String fakeMessage = "Party Finder > " + ign
                                                                                + " joined the dungeon group! (Berserk Level 1)";
                                                                net.minecraft.network.chat.Component component = net.minecraft.network.chat.Component
                                                                                .literal(fakeMessage);
                                                                Minecraft.getInstance().gui.getChat()
                                                                                .addMessage(component);
                                                                DungeonJoinHandler
                                                                                .onChatMessage(component);
                                                                ChatActionManager
                                                                                .getInstance()
                                                                                .onChatMessage(component);
                                                                return 1;
                                                        })));

                        testNode.then(ClientCommandManager.literal("testinvite")
                                        .then(ClientCommandManager
                                                        .argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                                                        .executes(ctx -> {
                                                                String ign = StringArgumentType.getString(ctx,
                                                                                Constants.CMD_ARG_IGN);
                                                                String fakeMessage = "[VIP] " + ign
                                                                                + " has invited you to join their party!\nYou have 60 seconds to accept. Click here to join!";
                                                                net.minecraft.network.chat.Component component = net.minecraft.network.chat.Component
                                                                                .literal(fakeMessage);
                                                                Minecraft.getInstance().gui.getChat()
                                                                                .addMessage(component);
                                                                ChatActionManager.getInstance()
                                                                                .onChatMessage(component);
                                                                return 1;
                                                        })));

                        testNode.then(ClientCommandManager.literal("testjoin")
                                        .then(ClientCommandManager
                                                        .argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                                                        .executes(ctx -> {
                                                                String ign = StringArgumentType.getString(ctx,
                                                                                Constants.CMD_ARG_IGN);
                                                                String fakeMessage = ign
                                                                                + " whispers to you: [BlackAddons] join party request - id:e1bc825d";
                                                                net.minecraft.network.chat.Component component = net.minecraft.network.chat.Component
                                                                                .literal(fakeMessage);
                                                                Minecraft.getInstance().gui.getChat()
                                                                                .addMessage(component);
                                                                PartyFinderManager.getInstance()
                                                                                .onChatMessage(component);
                                                                return 1;
                                                        })
                                                        .then(ClientCommandManager
                                                                        .argument("id", StringArgumentType.string())
                                                                        .executes(ctx -> {
                                                                                String ign = StringArgumentType
                                                                                                .getString(
                                                                                                                ctx,
                                                                                                                Constants.CMD_ARG_IGN);
                                                                                String id = StringArgumentType
                                                                                                .getString(
                                                                                                                ctx,
                                                                                                                "id");
                                                                                String fakeMessage = ign
                                                                                                + " whispers to you: [BlackAddons] join party request - id:"
                                                                                                + id;
                                                                                net.minecraft.network.chat.Component component = net.minecraft.network.chat.Component
                                                                                                .literal(fakeMessage);
                                                                                Minecraft.getInstance().gui.getChat()
                                                                                                .addMessage(component);
                                                                                PartyFinderManager.getInstance()
                                                                                                .onChatMessage(component);
                                                                                return 1;
                                                                        }))));

                        testNode.then(ClientCommandManager.literal("setid")
                                        .then(ClientCommandManager.argument("id", StringArgumentType.string())
                                                        .executes(ctx -> {
                                                                String id = StringArgumentType.getString(ctx, "id");
                                                                try {
                                                                        java.lang.reflect.Field field = PartyFinderManager
                                                                                        .getInstance().getClass()
                                                                                        .getDeclaredField(
                                                                                                        "currentPartyId");
                                                                        field.setAccessible(true);
                                                                        field.set(PartyFinderManager.getInstance(), id);
                                                                        NotificationManager.addNotification("Test",
                                                                                        "Set party ID to " + id,
                                                                                        NotificationType.SUCCESS);
                                                                } catch (Exception e) {
                                                                        e.printStackTrace();
                                                                }
                                                                return 1;
                                                        })));

                        var pvNode = ClientCommandManager.literal("pv")
                                        .executes(ctx -> {
                                                String player = Minecraft.getInstance().getUser().getName();
                                                ProfileStateManager.getInstance().loadProfileAndOpen(player, null,
                                                                false);
                                                return 1;
                                        })
                                        .then(ClientCommandManager
                                                        .argument(Constants.CMD_ARG_IGN, StringArgumentType.string())
                                                        .executes(ctx -> {
                                                                String ign = StringArgumentType.getString(ctx,
                                                                                Constants.CMD_ARG_IGN);
                                                                ProfileStateManager.getInstance()
                                                                                .loadProfileAndOpen(ign, null, false);
                                                                return 1;
                                                        })
                                                        .then(ClientCommandManager.literal("force")
                                                                        .executes(ctx -> {
                                                                                String ign = StringArgumentType
                                                                                                .getString(ctx, Constants.CMD_ARG_IGN);
                                                                                ProfileStateManager.getInstance()
                                                                                                .loadProfileAndOpen(ign,
                                                                                                                null,
                                                                                                                true);
                                                                                return 1;
                                                                        })));

                        var dailyNode = ClientCommandManager.literal("daily")
                                        .executes(ctx -> {
                                                String player = Minecraft.getInstance().getUser().getName();
                                                NotificationManager.addNotification("Daily Sync",
                                                                "Syncing stats with bot...",
                                                                NotificationType.INFO);

                                                BotIntegration.sendDailySync(player).thenAccept(success -> {
                                                        if (success) {
                                                                NotificationManager.addNotification("Daily Sync",
                                                                                "Stats synced successfully!",
                                                                                NotificationType.SUCCESS);
                                                        } else {
                                                                NotificationManager.addNotification("Daily Sync",
                                                                                "Failed to sync stats.",
                                                                                NotificationType.ERROR);
                                                        }
                                                });
                                                return 1;
                                        });

                        var ircNode = ClientCommandManager.literal("irc")
                                        .then(ClientCommandManager.literal("on")
                                                        .executes(ctx -> handleIrcChatMode(ctx.getSource(), true)))
                                        .then(ClientCommandManager.literal("off")
                                                        .executes(ctx -> handleIrcChatMode(ctx.getSource(), false)))
                                        .then(ClientCommandManager.literal("toggle")
                                                        .executes(ctx -> handleIrcChatMode(ctx.getSource(),
                                                                        !ConfigManager.data.ircChatMode)))
                                        .then(ClientCommandManager.literal("status")
                                                        .executes(ctx -> sendIrcChatModeStatus(ctx.getSource())))
                                        .then(ClientCommandManager
                                                        .argument("message", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                                String messageArg = StringArgumentType.getString(ctx,
                                                                                "message");
                                                                if (messageArg != null) {
                                                                        IrcClient.getInstance().sendMessage(messageArg);
                                                                }
                                                                return 1;
                                                        }))
                                        .executes(ctx -> {
                                                Minecraft.getInstance().execute(() -> {
                                                        org.blackum.blackaddons.client.BlackaddonsClient
                                                                        .openScreen(new IrcScreen());
                                                });
                                                return 1;
                                        });

                        for (String alias : new String[] { Constants.BASE_COMMAND, "black", "blackaddons" }) {
                                var cmd = ClientCommandManager.literal(alias).executes(openGui);

                                if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                                        cmd.then(testNode);
                                        cmd.then(dailyNode);
                                }
                                cmd.then(pvNode);
                                cmd.then(ircNode);
                                cmd.then(createActionTriggerModeNode("em"));
                                cmd.then(createActionTriggerModeNode("editmode"));
                                cmd.then(CommandUtils.subcommand);
                                cmd.then(ClientCommandManager.literal("pf").executes(ctx -> {
                                        Minecraft.getInstance().execute(() -> {
                                                org.blackum.blackaddons.client.BlackaddonsClient
                                                                .openScreen(new PartyFinderScreen());
                                        });
                                        return 1;
                                }));
                                cmd.then(ClientCommandManager.literal("preview")
                                                .then(ClientCommandManager
                                                                .argument("url", StringArgumentType
                                                                                .greedyString())
                                                                .executes(ctx -> {
                                                                        String url = StringArgumentType
                                                                                        .getString(ctx, "url");
                                                                        Minecraft.getInstance().execute(() -> {
                                                                                Minecraft.getInstance()
                                                                                                .setScreen(
                                                                                                                new ImagePreviewScreen(
                                                                                                                                url,
                                                                                                                                Minecraft.getInstance().screen));
                                                                        });
                                                                        return 1;
                                                                })));

                                if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
                                }

                                dispatcher.register(cmd);
                        }
                        CommandUtils.register(dispatcher);
                });
        }
}
