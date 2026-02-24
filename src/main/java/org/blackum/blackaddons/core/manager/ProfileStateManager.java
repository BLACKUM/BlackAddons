package org.blackum.blackaddons.core.manager;

import org.blackum.blackaddons.core.model.BotResult;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.integration.BotIntegration;
import org.blackum.blackaddons.integration.ProfileService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;

import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProfileStateManager {
    private static ProfileStateManager instance;
    private final Map<String, CacheEntry<JsonObject>> profileCache = new HashMap<>();
    private final Map<String, CacheEntry<JsonObject>> rngCache = new HashMap<>();
    private final Map<String, CacheEntry<JsonObject>> leaderboardCache = new HashMap<>();

    private ProfileStateManager() {
    }

    public static synchronized ProfileStateManager getInstance() {
        if (instance == null) {
            instance = new ProfileStateManager();
        }
        return instance;
    }

    public CompletableFuture<BotResult<JsonObject>> getProfile(String player, String profileName, boolean force) {
        String cacheKey = player.toLowerCase() + ":" + (profileName != null ? profileName.toLowerCase() : "default");

        if (force) {
            clearCache(player);
        } else if (profileCache.containsKey(cacheKey)) {
            CacheEntry<JsonObject> entry = profileCache.get(cacheKey);
            if (!entry.isExpired()) {
                return CompletableFuture.completedFuture(BotResult.success(entry.data));
            }
        }

        CompletableFuture<JsonObject> future = ProfileService.getProfileStats(player, profileName, force)
                .thenCompose(local -> {
                    if (local != null) {
                        return CompletableFuture.completedFuture(local);
                    }
                    return getSafeBotProfile(player, profileName, force);
                });

        return future.thenApply(json -> {
            if (json == null)
                return BotResult.error("API Unavailable");
            if (json.has("error"))
                return BotResult.error(json.get("error").getAsString());

            JsonObject data = json;
            if (json.has("data")) {
                data = json.getAsJsonObject("data");
            }

            if (data.has("catacombs") || data.has("members") || data.has("profiles")) {
                profileCache.put(cacheKey, new CacheEntry<>(data));

                if (data.has("profiles")) {
                    JsonArray profiles = data.getAsJsonArray("profiles");
                    for (JsonElement p : profiles) {
                        JsonObject prof = p.getAsJsonObject();
                        if (prof.has("stats") && prof.has("name")) {
                            String pName = prof.get("name").getAsString();
                            JsonObject pStats = prof.getAsJsonObject("stats");

                            pStats.add("profiles", profiles);

                            String pKey = player.toLowerCase() + ":" + pName.toLowerCase();
                            profileCache.put(pKey, new CacheEntry<>(pStats));
                        }
                    }
                }

                return BotResult.success(data);
            }
            return BotResult.error("Invalid data");
        });
    }

    private CompletableFuture<JsonObject> getSafeBotProfile(String player, String profileName, boolean force) {
        return BotIntegration.getProfileStats(player, profileName, force)
                .exceptionally(e -> null);
    }

    public CompletableFuture<BotResult<JsonObject>> getRngData(String player) {
        if (rngCache.containsKey(player.toLowerCase())) {
            CacheEntry<JsonObject> entry = rngCache.get(player.toLowerCase());
            if (!entry.isExpired(15)) {
                return CompletableFuture.completedFuture(BotResult.success(entry.data));
            }
        }

        CompletableFuture<JsonObject> botFuture = BotIntegration.getRngData(player).exceptionally(e -> null);

        return botFuture.thenApply(json -> {
            JsonObject botData = null;
            if (json != null && !json.has("error") && json.has("data")) {
                botData = json.getAsJsonObject("data");
            }

            JsonObject localData = LocalRngManager.getInstance().getRngData();
            JsonObject finalData;
            String currentUser = Minecraft.getInstance().getUser().getName();
            boolean isSelf = player.equalsIgnoreCase(currentUser);

            if (botData == null) {
                if (isSelf) {
                    finalData = localData;
                } else {
                    return BotResult.error("API Unavailable");
                }
            } else {
                if (isSelf) {
                    if (localData.has("drops")) {
                        JsonObject localDrops = localData.getAsJsonObject("drops");
                        JsonObject botDrops = botData.has("drops") ? botData.getAsJsonObject("drops")
                                : new JsonObject();

                        for (String cat : localDrops.keySet()) {
                            JsonObject localCat = localDrops.getAsJsonObject(cat);
                            JsonObject botCat = botDrops.has(cat) ? botDrops.getAsJsonObject(cat) : new JsonObject();

                            for (String item : localCat.keySet()) {
                                int localCount = localCat.get(item).getAsInt();
                                int botCount = botCat.has(item) ? botCat.get(item).getAsInt() : 0;

                                if (localCount > botCount) {
                                    botCat.addProperty(item, localCount);
                                }
                            }
                            botDrops.add(cat, botCat);
                        }
                        botData.add("drops", botDrops);
                    }
                }
                finalData = botData;
            }

            if (finalData != null) {
                rngCache.put(player.toLowerCase(), new CacheEntry<>(finalData));
                return BotResult.success(finalData);
            }
            return BotResult.error("Invalid data");
        });
    }

    public CompletableFuture<Integer> updateRngCount(String player, String category, String item, String action,
            Integer count) {
        String currentUser = Minecraft.getInstance().getUser().getName();
        boolean isDev = ConfigManager.data.developerKey != null
                && !ConfigManager.data.developerKey.isEmpty();

        if (!isDev && !player.equalsIgnoreCase(currentUser)) {
            return CompletableFuture.completedFuture(null);
        }

        final int fallbackCount;
        if ("set".equals(action) && count != null) {
            fallbackCount = count;
        } else if ("increment".equals(action)) {
            int current = LocalRngManager.getInstance().getDropCount(category, item);
            fallbackCount = current + 1;
        } else if ("decrement".equals(action)) {
            int current = LocalRngManager.getInstance().getDropCount(category, item);
            fallbackCount = Math.max(0, current - 1);
        } else {
            fallbackCount = -1;
        }

        if ("set".equals(action) && count != null) {
            LocalRngManager.getInstance().setDropCount(category, item, count);
        } else if ("increment".equals(action)) {
            LocalRngManager.getInstance().addDrop(category, item, 1);
        } else if ("decrement".equals(action)) {
            LocalRngManager.getInstance().addDrop(category, item, -1);
        }

        rngCache.remove(player.toLowerCase());

        return BotIntegration.updateRngDrop(player, category, item, action, count)
                .thenApply(newCount -> {
                    if (newCount == null) {
                        Blackaddons.LOGGER
                                .warn("Failed to sync RNG drop with bot. Using local value.");
                        NotificationManager.addNotification(
                                "Rng Sync Failed",
                                "Saved locally. Bot unreachable.",
                                NotificationType.WARNING);
                        return fallbackCount != -1 ? fallbackCount : null;
                    }

                    if (newCount != -1) {
                        LocalRngManager.getInstance().setDropCount(category, item,
                                newCount);
                        return newCount;
                    }

                    return null;
                })
                .exceptionally(e -> {
                    Blackaddons.LOGGER
                            .error("Error syncing RNG drop with bot: " + e.getMessage());
                    NotificationManager.addNotification(
                            "Rng Sync Error",
                            "Saved locally. Error: " + e.getMessage(),
                            NotificationType.ERROR);
                    return fallbackCount != -1 ? fallbackCount : null;
                });
    }

    public CompletableFuture<BotResult<JsonObject>> getLeaderboard(String period, String metric, int page) {
        String cacheKey = "lb:" + period + ":" + metric + ":" + page;
        if (leaderboardCache.containsKey(cacheKey)) {
            CacheEntry<JsonObject> entry = leaderboardCache.get(cacheKey);
            if (!entry.isExpired(15)) {
                return CompletableFuture.completedFuture(BotResult.success(entry.data));
            }
        }

        return BotIntegration.getLeaderboard(period, metric, page).thenApply(json -> {
            if (json == null)
                return BotResult.error("API Unavailable");
            if (json.has("error"))
                return BotResult.error(json.get("error").getAsString());

            leaderboardCache.put(cacheKey, new CacheEntry<>(json));
            return BotResult.success(json);
        });
    }

    public CompletableFuture<BotResult<JsonObject>> getLeaderboardWithPlayer(String period, String metric,
            String player) {
        return BotIntegration.getLeaderboardWithPlayer(period, metric, player).thenApply(json -> {
            if (json == null)
                return BotResult.error("API Unavailable");
            if (json.has("error"))
                return BotResult.error(json.get("error").getAsString());
            return BotResult.success(json);
        });
    }

    public void clearCache(String player) {
        profileCache.entrySet().removeIf(entry -> entry.getKey().startsWith(player.toLowerCase() + (":")));
        rngCache.remove(player.toLowerCase());
        leaderboardCache.clear();
    }

    public void clearLeaderboardCache() {
        leaderboardCache.clear();
    }

    public void loadProfileAndOpen(String player, String profileName, boolean force) {
        loadProfileAndOpen(player, profileName, force, false);
    }

    @SuppressWarnings("null")
    public void loadProfileAndOpen(String player, String profileName, boolean force, boolean quiet) {
        Minecraft mc = Minecraft.getInstance();
        if (!quiet) {
            mc.gui.getChat()
                    .addMessage(
                            ChatUtils.getMessage("Loading stats for " + player + (force ? " (Forced)" : "") + "..."));
        }

        getProfile(player, profileName, force).thenAccept(result -> {
            if (result == null) {
                mc.gui.getChat().addMessage(ChatUtils.error("Failed to fetch data."));
                NotificationManager.addNotification("Profile Error", "Failed to fetch data from API.",
                        NotificationType.ERROR);
                return;
            }

            if (result.hasError()) {
                String err = result.getError();
                mc.gui.getChat().addMessage(ChatUtils.error(err));
                NotificationManager.addNotification("Profile Error", err, NotificationType.ERROR);
                return;
            }

            JsonObject data = result.getData();
            if (data == null) {
                mc.gui.getChat().addMessage(ChatUtils.error("Invalid response format."));
                NotificationManager.addNotification("Profile Error", "Invalid response format.",
                        NotificationType.ERROR);
                return;
            }

            final JsonObject finalData = data;
            mc.execute(() -> {
                org.blackum.blackaddons.client.BlackaddonsClient.openScreen(
                        new org.blackum.blackaddons.gui.screen.ProfileViewerScreen(null, player, profileName, force,
                                finalData));
            });
        }).exceptionally(e -> {
            mc.gui.getChat().addMessage(ChatUtils.error("Exception: " + e.getMessage()));
            NotificationManager.addNotification("Profile Exception", e.getMessage(), NotificationType.ERROR);
            return null;
        });
    }

    private static class CacheEntry<T> {
        final T data;
        final long timestamp;

        CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return isExpired(ConfigManager.data.cacheDurationMinutes);
        }

        boolean isExpired(int minutes) {
            long durationMs = minutes * 60 * 1000L;
            return System.currentTimeMillis() - timestamp > durationMs;
        }
    }
}
