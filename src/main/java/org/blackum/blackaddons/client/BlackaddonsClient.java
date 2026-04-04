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
import net.minecraft.resources.Identifier;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.CommandManager;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.blackum.blackaddons.core.manager.DebugOverlayManager;
import org.blackum.blackaddons.core.manager.PartyFinderManager;
import org.blackum.blackaddons.core.manager.UpdateManager;
import org.blackum.blackaddons.gui.render.DebugBoxRenderer;
import org.blackum.blackaddons.gui.render.WaypointRenderer;
import org.blackum.blackaddons.feature.chat.ChatImageHandler;
import org.blackum.blackaddons.feature.chat.ChatActionManager;
import org.blackum.blackaddons.feature.chat.IrcClient;
import org.blackum.blackaddons.feature.chat.IrcPrefixManager;
import org.blackum.blackaddons.feature.cheat.AutoBM;
import org.blackum.blackaddons.feature.cheat.AutoSS;
import org.blackum.blackaddons.feature.cheat.AutoTNT;
import org.blackum.blackaddons.feature.cheat.FastLeap;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.blackum.blackaddons.feature.cheat.Perspective;
import org.blackum.blackaddons.feature.cheat.RelicLook;
import org.blackum.blackaddons.core.util.LocationUtils;
import org.blackum.blackaddons.core.util.AlignUtils;
import org.blackum.blackaddons.core.util.Scheduler;
import org.blackum.blackaddons.feature.dungeon.DungeonJoinHandler;
import org.blackum.blackaddons.feature.dungeon.SoloClearsTracker;
import org.blackum.blackaddons.feature.rng.RngTracker;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.DemoScreen;
import org.blackum.blackaddons.gui.screen.TestMenuScreen;
import org.blackum.blackaddons.integration.BotIntegration;

public class BlackaddonsClient implements ClientModInitializer {
    private static boolean internalChatMsg = false;
    private static Screen pendingScreen = null;

    @Override
    public void onInitializeClient() {
        Blackaddons.LOGGER.info("Initializing client...");
        IrcPrefixManager.getPrefix(); 
        AutoTNT.register();
        FastLeap.register();
        RelicLook.register();
        AutoSS.register();
        AutoBM.register();
        Freecam.register();
        Perspective.register();
        Scheduler.register();
        LocationUtils.register();
        AlignUtils.register();

        ConfigManager.load();
        BotIntegration.authenticateWithMojang();
        CustomNameManager.getInstance().fetch();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IrcPrefixManager.resetCache();
            IrcClient.getInstance().connect();
            UpdateManager.check();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            IrcClient.getInstance().disconnect();
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String prefix = IrcPrefixManager.getPrefix();
            if (message.startsWith(prefix)) {
                String content = message.substring(prefix.length()).trim();
                if (!content.isEmpty()) {
                    IrcClient.getInstance().sendMessage(content);
                }
                return false;
            }
            return true;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            ChatActionManager.getInstance().onChatMessage(message);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            ChatActionManager.getInstance().onChatMessage(message);
        });

        Blackaddons.guiOpener = () -> {
            pendingScreen = new DemoScreen();
        };

        Blackaddons.testMenuOpener = () -> {
            pendingScreen = new TestMenuScreen();
        };

        Blackaddons.screenOpener = (screen) -> {
            pendingScreen = screen;
        };

        Blackaddons.mainGuiOpener = () -> {
            pendingScreen = new BlackAddonsGUI();
        };

        Blackaddons.notificationTrigger = (message) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> NotificationManager.addNotification("Notification", message, NotificationType.INFO));
        };

        DebugOverlayManager.register();
        CommandManager.register();

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("blackaddons", "notifications"), (graphics, deltaTracker) -> {
            if (!(Minecraft.getInstance().screen instanceof BaseScreen)) {
                NotificationManager.getInstance().render(graphics);
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> AlignUtils.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            org.blackum.blackaddons.gui.notification.NotificationManager.getInstance().tick();
            if (pendingScreen != null) {
                client.setScreen(pendingScreen);
                pendingScreen = null;
            }
        });

        Blackaddons.LOGGER.info("Client initialization completed");
    }
}
