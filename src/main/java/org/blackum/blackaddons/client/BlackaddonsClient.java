package org.blackum.blackaddons.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.feature.cheat.AutoTNT;
import org.blackum.blackaddons.feature.cheat.FastLeap;
import org.blackum.blackaddons.feature.cheat.BloodBlink;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.feature.rng.RngTracker;
import org.blackum.blackaddons.core.manager.CommandManager;
import org.blackum.blackaddons.core.manager.DebugOverlayManager;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.screen.DemoScreen;
import org.blackum.blackaddons.gui.screen.TestMenuScreen;
import org.blackum.blackaddons.integration.BotIntegration;
import org.blackum.blackaddons.feature.chat.IrcClient;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.feature.chat.ChatImageHandler;
import org.blackum.blackaddons.feature.chat.ChatSoundAlertManager;
import org.blackum.blackaddons.feature.dungeon.DungeonJoinHandler;
import org.blackum.blackaddons.core.manager.PartyFinderManager;
import org.blackum.blackaddons.core.manager.CustomNameManager;
import org.blackum.blackaddons.Blackaddons;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.gui.screens.Screen;

public class BlackaddonsClient implements ClientModInitializer {
    public static Screen pendingScreen = null;

    public static void openScreen(Screen screen) {
        pendingScreen = screen;
    }

    @Override
    public void onInitializeClient() {
        Blackaddons.LOGGER.info("Initializing client...");
        AutoTNT.register();
        FastLeap.register();
        BloodBlink.register();

        ConfigManager.load();
        BotIntegration.fetchVerificationKey();
        CustomNameManager.getInstance().fetch();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IrcClient.getInstance().connect();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            IrcClient.getInstance().disconnect();
        });

        Blackaddons.guiOpener = () -> {
            BlackaddonsClient.openScreen(new DemoScreen());
        };

        Blackaddons.testMenuOpener = () -> {
            BlackaddonsClient.openScreen(new TestMenuScreen());
        };

        Blackaddons.mainGuiOpener = () -> {
            BlackaddonsClient.openScreen(new BlackAddonsGUI());
        };

        Blackaddons.notificationTrigger = (message) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> NotificationManager.addNotification("Notification", message, NotificationType.INFO));
        };

        DebugOverlayManager.register();
        CommandManager.register();

        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            if (!(Minecraft.getInstance().screen instanceof BaseScreen)) {
                NotificationManager.getInstance().render(graphics);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            NotificationManager.getInstance().tick();
            if (BlackaddonsClient.pendingScreen != null) {
                client.setScreen(BlackaddonsClient.pendingScreen);
                BlackaddonsClient.pendingScreen = null;
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith("#")) {
                IrcClient.getInstance().sendMessage(message.substring(1).trim());
                return false;
            }
            return true;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Component handled = ChatImageHandler
                    .handleMessage(message);
            RngTracker.onChatMessage(handled);
            DungeonJoinHandler.onChatMessage(handled);
            PartyFinderManager.getInstance().onChatMessage(handled);
            ChatSoundAlertManager.getInstance().onChatMessage(handled);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            Component handled = ChatImageHandler
                    .handleMessage(message);
            RngTracker.onChatMessage(handled);
            DungeonJoinHandler.onChatMessage(handled);
            PartyFinderManager.getInstance().onChatMessage(handled);
            ChatSoundAlertManager.getInstance().onChatMessage(handled);
        });

        Blackaddons.LOGGER.info("Client initialization completed");
    }
}
