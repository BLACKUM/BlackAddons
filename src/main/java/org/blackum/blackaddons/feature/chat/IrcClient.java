package org.blackum.blackaddons.feature.chat;

import org.blackum.blackaddons.core.util.MinecraftInstance;
import org.blackum.blackaddons.feature.chat.EmojiUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IrcClient implements WebSocket.Listener {
    private static IrcClient instance;
    private WebSocket webSocket;
    private final HttpClient client;
    private boolean connecting = false;
    private boolean isAdmin = false;

    private ScheduledExecutorService keepAliveExecutor;
    private long lastMessageTime = System.currentTimeMillis();

    private final List<IrcMessageListener> listeners = new ArrayList<>();
    private final List<IrcMessage> messageBuffer = new ArrayList<>();
    private static final int BUFFER_SIZE = 50;

    private IrcClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static IrcClient getInstance() {
        if (instance == null) {
            instance = new IrcClient();
        }
        return instance;
    }

    public void connect() {
        if (webSocket != null || connecting || !ConfigManager.data.ircEnabled) {
            return;
        }

        String botUrl = ConfigManager.data.botUrl;
        if (botUrl.isEmpty()) {
            return;
        }

        String wsUrl = botUrl.replace("http://", "ws://").replace("https://", "wss://") + "/v1/irc";
        if (!ConfigManager.data.developerKey.isEmpty()) {
            wsUrl += "?key=" + URLEncoder.encode(ConfigManager.data.developerKey, StandardCharsets.UTF_8);
        }

        synchronized (messageBuffer) {
            messageBuffer.clear();
        }
        for (IrcMessageListener listener : listeners) {
            listener.onMessageReceived(null);
        }

        connecting = true;
        lastMessageTime = System.currentTimeMillis();

        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), this)
                .whenComplete((ws, ex) -> {
                    connecting = false;
                    if (ex != null) {
                        Blackaddons.LOGGER.error("Failed to connect to IRC: " + ex.getMessage());
                        scheduleReconnect();
                    } else {
                        this.webSocket = ws;
                        Blackaddons.LOGGER.info("Connected to IRC");
                        startKeepAlive();
                    }
                });
    }

    public void disconnect() {
        stopKeepAlive();
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Disconnecting");
            } catch (Exception ignored) {
            }
            webSocket = null;
        }
    }

    private void startKeepAlive() {
        stopKeepAlive();
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor();
        keepAliveExecutor.scheduleAtFixedRate(() -> {
            if (webSocket != null) {
                if (System.currentTimeMillis() - lastMessageTime > 30000) {
                    Blackaddons.LOGGER.warn("IRC connection timed out, reconnecting...");
                    disconnect();
                    scheduleReconnect();
                    return;
                }
                try {
                    webSocket.sendPing(ByteBuffer.allocate(0)).exceptionally(ex -> {
                        Blackaddons.LOGGER.error("Failed to send ping", ex);
                        disconnect();
                        scheduleReconnect();
                        return null;
                    });
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Ping error", e);
                    disconnect();
                    scheduleReconnect();
                }
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    private void stopKeepAlive() {
        if (keepAliveExecutor != null && !keepAliveExecutor.isShutdown()) {
            keepAliveExecutor.shutdownNow();
            keepAliveExecutor = null;
        }
    }

    public void addListener(IrcMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Blackaddons.LOGGER.info("IRC Listener added: " + listener.getClass().getSimpleName() + " (Total: "
                    + listeners.size() + ")");
        }
    }

    public void removeListener(IrcMessageListener listener) {
        listeners.remove(listener);
    }

    public List<IrcMessage> getMessageBuffer() {
        synchronized (messageBuffer) {
            return new ArrayList<>(messageBuffer);
        }
    }

    public void sendMessage(String message, String channel) {
        if (webSocket == null || !ConfigManager.data.ircEnabled) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            String processedMessage = processMessageText(message);
            String username = MinecraftInstance.mc.getUser().getName();

            IrcMessage ircMsg = new IrcMessage(username, processedMessage, channel);
            synchronized (messageBuffer) {
                if (messageBuffer.size() >= BUFFER_SIZE) {
                    messageBuffer.remove(0);
                }
                messageBuffer.add(ircMsg);
            }
            for (IrcMessageListener listener : listeners) {
                listener.onMessageReceived(ircMsg);
            }
            displayMessage(username, processedMessage, channel);

            JsonObject json = new JsonObject();
            json.addProperty("user", username);
            json.addProperty("uuid", MinecraftInstance.mc.getUser().getProfileId().toString());
            json.addProperty("message", processedMessage);
            json.addProperty("channel", channel);
            json.addProperty("timestamp", ircMsg.timestamp());

            webSocket.sendText(json.toString(), true).exceptionally(ex -> {
                Blackaddons.LOGGER.error("Failed to send IRC message", ex);
                return null;
            });
        });
    }

    public void sendMessage(String message) {
        sendMessage(message, "general");
    }

    private final StringBuilder incomingMessageBuffer = new StringBuilder();

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        lastMessageTime = System.currentTimeMillis();
        incomingMessageBuffer.append(data);
        if (!last) {
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        String fullMessage = incomingMessageBuffer.toString();
        incomingMessageBuffer.setLength(0);

        try {
            JsonObject json = JsonParser.parseString(fullMessage).getAsJsonObject();
            String type = json.get("type").getAsString();

            if (type.equals("chat")) {
                String user = json.get("user").getAsString();
                String message = processMessageText(json.get("message").getAsString());
                String channel = json.has("channel") ? json.get("channel").getAsString() : "general";
                long timestamp = json.has("timestamp") ? json.get("timestamp").getAsLong() : System.currentTimeMillis();

                IrcMessage ircMsg = new IrcMessage(user, message, channel, timestamp);

                synchronized (messageBuffer) {
                    if (messageBuffer.size() >= BUFFER_SIZE) {
                        messageBuffer.remove(0);
                    }
                    messageBuffer.add(ircMsg);
                }

                for (IrcMessageListener listener : listeners) {
                    try {
                        listener.onMessageReceived(ircMsg);
                    } catch (Exception e) {
                        Blackaddons.LOGGER.error("Error notifying IRC listener: " + e.getMessage());
                    }
                }

                displayMessage(user, message, channel);
            } else if (type.equals("history")) {
                String channel = json.get("channel").getAsString();
                JsonArray messages = json.get("messages").getAsJsonArray();

                synchronized (messageBuffer) {
                    for (JsonElement el : messages) {
                        JsonObject msgObj = el.getAsJsonObject();
                        String user = msgObj.get("user").getAsString();
                        String message = processMessageText(msgObj.get("message").getAsString());
                        long timestamp = msgObj.has("timestamp") ? msgObj.get("timestamp").getAsLong()
                                : System.currentTimeMillis();
                        messageBuffer.add(new IrcMessage(user, message, channel, timestamp));
                    }

                    while (messageBuffer.size() > BUFFER_SIZE * 5) {
                        messageBuffer.remove(0);
                    }
                }

                for (IrcMessageListener listener : listeners) {
                    listener.onMessageReceived(null);
                }
            } else if (type.equals("auth")) {
                this.isAdmin = json.get("is_admin").getAsBoolean();
                Blackaddons.LOGGER.info("IRC Auth status received: Admin=" + isAdmin);
                for (IrcMessageListener listener : listeners) {
                    if (listener instanceof IrcAuthListener) {
                        ((IrcAuthListener) listener).onAuthStatusChanged(isAdmin);
                    }
                }
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Error parsing IRC message: " + e.getMessage());
            e.printStackTrace();
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    private void displayMessage(String user, String message, String channel) {
        if (user == null || message == null || message.isEmpty())
            return;

        MinecraftInstance.mc.execute(() -> {
            if (MinecraftInstance.mc.player == null)
                return;

            String[] lines = message.split("\\n");
            boolean firstLine = true;

            for (String rawLine : lines) {
                String line = rawLine;
                while (!line.isEmpty()) {
                    int chunkSize = Math.min(line.length(), 256);
                    String chunk = line.substring(0, chunkSize);
                    line = line.substring(chunkSize);

                    MutableComponent component = Component.literal("");
                    if (firstLine) {
                        component.append(Component.literal("§d[IRC-" + channel.toUpperCase() + "] "))
                                .append(Component.literal(user).withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
                        firstLine = false;
                    } else {
                        component.append(Component.literal("  ").withStyle(ChatFormatting.WHITE));
                    }
                    component.append(Component.literal(chunk).withStyle(ChatFormatting.WHITE));
                    MinecraftInstance.mc.gui.getChat().addClientSystemMessage(component);
                }
            }
        });
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        stopKeepAlive();
        this.webSocket = null;
        Blackaddons.LOGGER.info("IRC connection closed: " + reason);
        scheduleReconnect();
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        stopKeepAlive();
        this.webSocket = null;
        Blackaddons.LOGGER.error("IRC WebSocket error: " + error.getMessage());
        scheduleReconnect();
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        lastMessageTime = System.currentTimeMillis();
        return WebSocket.Listener.super.onPong(webSocket, message);
    }

    private void scheduleReconnect() {
        if (ConfigManager.data.ircEnabled) {
            CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS)
                    .execute(this::connect);
        }
    }

    public boolean isConnected() {
        return webSocket != null;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public static record IrcMessage(String user, String message, String channel, long timestamp) {
        public IrcMessage(String user, String message, String channel) {
            this(user, message, channel, System.currentTimeMillis());
        }
    }

    public interface IrcMessageListener {
        void onMessageReceived(IrcMessage message);
    }

    public interface IrcAuthListener extends IrcMessageListener {
        void onAuthStatusChanged(boolean isAdmin);
    }

    private String processMessageText(String message) {
        String processed = EmojiUtils.replaceEmojis(message);
        if (processed.length() > 2000) {
            processed = processed.substring(0, 2000) + " [truncated]";
        }

        if (processed.length() > 200) {
            StringBuilder spacedText = new StringBuilder();
            int currentWordLength = 0;
            boolean inUrl = false;

            for (int i = 0; i < processed.length(); i++) {
                char c = processed.charAt(i);
                spacedText.append(c);
                if (c == ' ') {
                    currentWordLength = 0;
                    inUrl = false;
                } else {
                    currentWordLength++;

                    if (currentWordLength == 7) {
                        String currentWord = processed.substring(i - 6, i + 1);
                        if (currentWord.equals("http://")) {
                            inUrl = true;
                        }
                    } else if (currentWordLength == 8) {
                        String currentWord = processed.substring(i - 7, i + 1);
                        if (currentWord.equals("https://")) {
                            inUrl = true;
                        }
                    }

                    if (currentWordLength >= 60 && !inUrl) {
                        spacedText.append(" ");
                        currentWordLength = 0;
                    }
                }
            }
            return spacedText.toString();
        }
        return processed;
    }
}
