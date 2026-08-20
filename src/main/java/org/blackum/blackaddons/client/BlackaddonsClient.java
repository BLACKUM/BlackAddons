package org.blackum.blackaddons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.Identifier;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.ModuleManager;
import org.blackum.blackaddons.feature.customname.CustomNameManager;
import org.blackum.blackaddons.feature.party.PartyFinderManager;
import org.blackum.blackaddons.feature.update.UpdateManager;
import org.blackum.blackaddons.client.render.DebugBoxRenderer;
import org.blackum.blackaddons.client.render.WaypointRenderer;
import org.blackum.blackaddons.feature.chat.ChatImageHandler;
import org.blackum.blackaddons.feature.chat.ChatActionManager;
import org.blackum.blackaddons.feature.chat.IrcClient;
import org.blackum.blackaddons.feature.chat.IrcPrefixManager;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.common.util.mc.ServerUtils;
import org.blackum.blackaddons.feature.TabListToggleHandler;
import org.blackum.blackaddons.feature.waypoint.AlignUtils;
import org.blackum.blackaddons.feature.dungeon.listener.DungeonJoinHandler;
import org.blackum.blackaddons.feature.dungeon.listener.DungeonListener;
import org.blackum.blackaddons.feature.dungeon.tracker.SoloClearTimer;
import org.blackum.blackaddons.feature.dungeon.tracker.SoloClearsTracker;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.blackum.blackaddons.feature.dungeon.solver.puzzle.waterboard.WaterBoardSolver;
import org.blackum.blackaddons.feature.dungeon.solver.puzzle.tpmaze.TpMazeSolver;
import org.blackum.blackaddons.gui.hud.HudRegistry;
import org.blackum.blackaddons.feature.dungeon.map.DungeonScoreboard;
import org.blackum.blackaddons.feature.dungeon.map.DungeonWorldScanner;
import org.blackum.blackaddons.feature.dungeon.map.RoomData;
import org.blackum.blackaddons.feature.rng.RngTracker;
import org.blackum.blackaddons.feature.profile.ProfileStateManager;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.render.font.CustomFontRenderer;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.debug.DemoScreen;
import org.blackum.blackaddons.gui.screen.debug.TestMenuScreen;
import org.blackum.blackaddons.service.BotIntegration;

public class BlackaddonsClient implements ClientModInitializer {
    private static boolean internalChatMsg = false;
    private static Screen pendingScreen = null;

    @Override
    public void onInitializeClient() {
        Blackaddons.LOGGER.info("Initializing client...");
        IrcPrefixManager.getPrefix(); 

        ConfigManager.load();
        if (ConfigManager.data.customTextEnabled) {
            CustomFontRenderer.getInstance().init();
        }
        RoomData.loadRooms();
        BotIntegration.authenticateWithBot();
        CustomNameManager.getInstance().fetch();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IrcPrefixManager.resetCache();
            IrcClient.getInstance().connect();
            UpdateManager.check();
            ProfileStateManager.getInstance().getRngData(client.getUser().getName());
            DungeonMap.reset();
            DungeonWorldScanner.reset();
            DungeonScoreboard.reset();
            WaterBoardSolver.reset();
            TpMazeSolver.reset();
            DungeonListener.resetKeyTimer();
            SoloClearTimer.reset();
            ServerUtils.reset();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            IrcClient.getInstance().disconnect();
            DungeonMap.reset();
            DungeonWorldScanner.reset();
            DungeonScoreboard.reset();
            WaterBoardSolver.reset();
            TpMazeSolver.reset();
            DungeonListener.resetKeyTimer();
            SoloClearTimer.reset();
            ServerUtils.reset();
        });

        Blackaddons.guiOpener = () -> {
            pendingScreen = new DemoScreen();
        };

        Blackaddons.testMenuOpener = () -> {
            pendingScreen = new TestMenuScreen();
        };

        Blackaddons.screenOpener = (screen) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> McCompat.setScreen(client, screen));
        };

        Blackaddons.mainGuiOpener = () -> {
            pendingScreen = new BlackAddonsGUI();
        };

        Blackaddons.notificationTrigger = (message) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> NotificationManager.addNotification("Notification", message, NotificationType.INFO));
        };

        ModuleManager.registerAutoModules();
        HudRegistry.install();

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("blackaddons", "notifications"),
                (graphics, tracker) -> {
                    if (!(McCompat.getScreen(Minecraft.getInstance()) instanceof BaseScreen)) {
                        NotificationManager.getInstance().render(graphics);
                    }
                }
        );

//? if >=26.2 {

        /*LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            WaypointRenderer.render(context.poseStack(), context.submitNodeCollector(), 0.0f);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;
            DebugBoxRenderer.render(
                    context.poseStack(),
                    context.submitNodeCollector(),
                    McCompat.getCamera(mc.gameRenderer).position(),
                    LocationUtils.getDebugBoxes()
            );
        });

*///?} else {
        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(context -> {
            WaypointRenderer.render(context.poseStack().last().pose(), context.bufferSource(), 0.0f);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;
            DebugBoxRenderer.render(
                    context.poseStack().last().pose(),
                    context.bufferSource(),
                    mc.gameRenderer.getMainCamera().position(),
                    LocationUtils.getDebugBoxes()
            );
        });
//?}

        ClientTickEvents.START_CLIENT_TICK.register(client -> AlignUtils.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ++DungeonListener.currentTime;
            DungeonListener.tick();
            NotificationManager.getInstance().tick();
            SoloClearsTracker.tick();
            SoloClearTimer.tick();
            ServerUtils.tick();
            TabListToggleHandler.tick();
            if (pendingScreen != null) {
                McCompat.setScreen(client, pendingScreen);
                pendingScreen = null;
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigManager.save();
            IrcClient.getInstance().disconnect();
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (internalChatMsg) {
                internalChatMsg = false;
                return true;
            }

            if (ConfigManager.data.ircChatMode) {
                IrcClient.getInstance().sendMessage(message.trim());
                return false;
            }

            String prefix = IrcPrefixManager.getPrefix();

            if (prefix.equals("#") && message.startsWith("##")) {
                internalChatMsg = true;
                Minecraft.getInstance().player.connection.sendChat(message.substring(1));
                return false;
            }

            if (message.startsWith(prefix)) {
                IrcClient.getInstance().sendMessage(message.substring(prefix.length()).trim());
                return false;
            }
            return true;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Component handled = ChatImageHandler
                    .handleMessage(message);
            RngTracker.onChatMessage(handled);
            DungeonJoinHandler.onChatMessage(handled);
            DungeonScoreboard.onChatMessage(handled);
            PartyFinderManager.getInstance().onChatMessage(handled);
            ChatActionManager.getInstance().onChatMessage(handled);
            SoloClearsTracker.onChatMessage(handled);
            SoloClearTimer.onChatMessage(handled);
            DungeonListener.onChatMessage(handled);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            Component handled = ChatImageHandler
                    .handleMessage(message);
            RngTracker.onChatMessage(handled);
            DungeonJoinHandler.onChatMessage(handled);
            DungeonScoreboard.onChatMessage(handled);
            PartyFinderManager.getInstance().onChatMessage(handled);
            ChatActionManager.getInstance().onChatMessage(handled);
            SoloClearsTracker.onChatMessage(handled);
            SoloClearTimer.onChatMessage(handled);
            DungeonListener.onChatMessage(handled);
        });

        Blackaddons.LOGGER.info("Client initialization completed");
    }
}
