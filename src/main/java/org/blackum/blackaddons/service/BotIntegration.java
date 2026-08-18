package org.blackum.blackaddons.service;

import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.util.mc.MinecraftInstance;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.io.HttpUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.blackum.blackaddons.common.util.io.EncryptionUtils;

public class BotIntegration {
    static final HttpClient client = HttpUtils.client;

    public static void sendRngDrop(String player, String item, String rarity, String floor, String category) {
        if (ConfigManager.data.botUrl.isEmpty())
            return;

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("item", item);
        json.addProperty("rarity", rarity);
        json.addProperty("floor", floor);
        json.addProperty("category", category);
        json.addProperty("action", "increment");
        json.addProperty("timestamp", System.currentTimeMillis() / 1000);

        sendMojangAuthedPostRequest(Constants.BOT_API_RNG, json);
    }

    public static CompletableFuture<Boolean> sendDailySync(String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);

        return sendPostRequest(Constants.BOT_API_DAILY, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() >= 200 && res.statusCode() < 300;
        });
    }

    public static CompletableFuture<JsonObject> getProfileStats(String player, String profileName, boolean force) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_PROFILE + "?player=" + player;
        if (profileName != null) {
            url += "&profile=" + profileName;
        }
        if (force) {
            url += "&force=true";
        }

        return sendGetRequest(url).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse profile stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRtcaStats(String player, String profileName, String floor,
            Map<String, Double> bonuses) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        if (profileName != null) {
            json.addProperty("profile", profileName);
        }
        json.addProperty("floor", floor);

        if (bonuses != null && !bonuses.isEmpty()) {
            JsonObject bonusJson = new JsonObject();
            for (Map.Entry<String, Double> entry : bonuses.entrySet()) {
                bonusJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("bonuses", bonusJson);
        }

        return sendPostRequest(Constants.BOT_API_RTCA, json.toString()).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RTCA stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRngData(String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        return sendGetRequest(Constants.BOT_API_RNG + "?player=" + player).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RNG data response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Integer> updateRngDrop(String player, String category, String item, String action,
            Integer count) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("action", action);
        json.addProperty("category", category);
        json.addProperty("item", item);
        if (count != null) {
            json.addProperty("count", count);
        }

        return sendMojangAuthedPostRequest(Constants.BOT_API_RNG, json).thenApply(res -> {
            if (res != null && res.statusCode() >= 200 && res.statusCode() < 300) {
                try {
                    JsonObject responseJson = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (responseJson.has("count")) {
                        return responseJson.get("count").getAsInt();
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RNG update response: " + e.getMessage());
                }
                return -1;
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getLeaderboard(String period, String metric, int page) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format(Constants.BOT_API_LEADERBOARD + "?period=%s&metric=%s&page=%d", period, metric,
                page);
        return sendGetRequest(endpoint).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse leaderboard stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getLeaderboardWithPlayer(String period, String metric, String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format(Constants.BOT_API_LEADERBOARD + "?period=%s&metric=%s&find_player=%s", period,
                metric, player);
        return sendGetRequest(endpoint).thenApply(res -> {
            if (res != null && (res.statusCode() == 200 || res.statusCode() == 404)) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> createParty(String floor, String note, JsonObject reqs, int maxSize) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());
        json.addProperty("floor", floor);
        json.addProperty("note", note);
        json.add("reqs", reqs);
        json.addProperty("max_size", maxSize);

        return sendPostRequest(Constants.BOT_API_PARTY_CREATE, json.toString()).thenApply(res -> {
            if (res != null && (res.statusCode() == 200 || res.statusCode() == 400)) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Boolean> unqueueParty() {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());

        return sendPostRequest(Constants.BOT_API_PARTY_UNQUEUE, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() == 200;
        });
    }

    public static CompletableFuture<Boolean> updateParty(int memberCount, String note) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());
        json.addProperty("member_count", memberCount);
        if (note != null) {
            json.addProperty("note", note);
        }

        return sendPostRequest(Constants.BOT_API_PARTY_UPDATE, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() == 200;
        });
    }

    public static CompletableFuture<JsonObject> getParties(String floor) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_PARTY_LIST + (floor != null ? "?floor=" + floor : "");
        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getSoloLeaderboard(String floor) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_SOLO_LEADERBOARD + "?floor=" + floor;
        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse leaderboard response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Boolean> preVerifyMojang(String ign, String uuid, String serverId) {
        if (ConfigManager.data.botUrl.isEmpty() || serverId == null || serverId.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("ign", ign);
        json.addProperty("server_id", serverId);
        json.addProperty("uuid", uuid);

        return sendPostRequest(Constants.BOT_API_AUTH_VERIFY, json.toString()).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject body = JsonParser.parseString(res.body()).getAsJsonObject();
                    return body.has("ok") && body.get("ok").getAsBoolean();
                } catch (Exception e) {
                    Blackaddons.LOGGER.warn("[BotIntegration] preVerifyMojang parse error: {}", e.getMessage());
                }
            }
            return false;
        });
    }

    public static CompletableFuture<JsonObject> sendSoloClear(String player, String playerUuid, String floor,
            String time,
            int secrets, int deaths, int crypts,
            List<String> puzzles, boolean prince, boolean mimic, boolean needsVerification,
            List<String> scoreboardLines, List<String> tablistLines,
            Map<String, Integer> scoreComponents,
            long dungeonEnterTick, long clearTriggerTick,
            long clientClockEnter, long clientClockClear,
            String mojangServerId, JsonObject mapData) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        if (playerUuid != null && !playerUuid.isEmpty()) {
            json.addProperty("uuid", playerUuid);
        }
        json.addProperty("floor", floor);
        json.addProperty("time", time);
        json.addProperty("secrets", secrets);
        json.addProperty("deaths", deaths);
        json.addProperty("crypts", crypts);
        json.addProperty("prince", prince);
        json.addProperty("mimic", mimic);
        json.addProperty("needs_verification", needsVerification);

        JsonArray puzzleArray = new JsonArray();
        if (puzzles != null) {
            for (String p : puzzles) {
                puzzleArray.add(p);
            }
        }
        json.add("puzzles", puzzleArray);

        JsonArray sidebarArray = new JsonArray();
        if (scoreboardLines != null) {
            for (String line : scoreboardLines) {
                sidebarArray.add(line);
            }
        }
        json.add("scoreboard_lines", sidebarArray);

        JsonArray tabArray = new JsonArray();
        if (tablistLines != null) {
            for (String line : tablistLines) {
                tabArray.add(line);
            }
        }
        json.add("tablist_lines", tabArray);

        if (scoreComponents != null && !scoreComponents.isEmpty()) {
            JsonObject components = new JsonObject();
            for (Map.Entry<String, Integer> e : scoreComponents.entrySet()) {
                components.addProperty(e.getKey(), e.getValue());
            }
            json.add("score_components", components);
        }

        json.addProperty("dungeon_enter_tick", dungeonEnterTick);
        json.addProperty("clear_trigger_tick", clearTriggerTick);
        json.addProperty("client_clock_enter", clientClockEnter);
        json.addProperty("client_clock_clear", clientClockClear);

        if (mojangServerId != null && !mojangServerId.isEmpty()) {
            json.addProperty("mojang_server_id", mojangServerId);
        }

        if (mapData != null) {
            json.add("map_data", mapData);
        }

        // If we have a server clock offset, include server clock times and ticks for enter/clear
        try {
            if (serverClockOffsetMs != null) {
                long scEnter = clientClockEnter + serverClockOffsetMs;
                long scClear = clientClockClear + serverClockOffsetMs;
                json.addProperty("server_clock_enter", scEnter);
                json.addProperty("server_clock_clear", scClear);
                json.addProperty("server_tick_enter", scEnter / 50);
                json.addProperty("server_tick_clear", scClear / 50);
            }
        } catch (Exception ignored) {
        }

        return sendPostRequest(Constants.BOT_API_SOLO_CLEAR, json.toString()).thenApply(res -> {
            if (res != null && res.statusCode() >= 200 && res.statusCode() < 300) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse solo clear response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    private static boolean authKeyFetched = false;
    private static CompletableFuture<String> activeAuthKeyFuture = null;
    // Offset (server_clock_ms - client_system_time_ms) discovered from /v1/key
    private static Long serverClockOffsetMs = null;

    public static synchronized CompletableFuture<String> getAuthKey() {
        if (authKeyFetched && EncryptionUtils.isKeySet()) {
            return CompletableFuture.completedFuture("fetched_key_present");
        }
        if (activeAuthKeyFuture != null && !activeAuthKeyFuture.isDone()) {
            return activeAuthKeyFuture;
        }

        net.minecraft.client.Minecraft mc = MinecraftInstance.mc;
        if (mc == null || mc.getUser() == null) {
            return CompletableFuture.completedFuture(null);
        }

        String serverId = MojangAuthService.generateServerId();
        activeAuthKeyFuture = MojangAuthService.joinServer(serverId).thenCompose(joined -> {
            if (!joined) {
                Blackaddons.LOGGER.warn("Mojang joinServer failed; refusing to fetch key.");
                return CompletableFuture.completedFuture(null);
            }

            String uuidStr = mc.getUser().getProfileId().toString();
            String username = mc.getUser().getName();
            String url = ConfigManager.data.botUrl + "/v1/key?uuid=" + URLEncoder.encode(uuidStr, StandardCharsets.UTF_8)
                    + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                    + "&mojang_server_id=" + URLEncoder.encode(serverId, StandardCharsets.UTF_8);

            try {
                HttpRequest.Builder keyBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", Constants.BOT_USER_AGENT)
                        .timeout(Duration.ofSeconds(10))
                        .GET();
                applyBotToken(keyBuilder);
                HttpRequest request = keyBuilder.build();

                return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(response -> {
                            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                try {
                                    JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                                    // Read independently of the key. A backend that hands out no key
                                    // still tells us its clock, and that is the half the solo-clear
                                    // report needs; nesting this inside the key branch meant no key,
                                    // no offset, and silently no server timestamps on any clear.
                                    if (responseJson.has("server_clock_ms")) {
                                        try {
                                            long serverClock = responseJson.get("server_clock_ms").getAsLong();
                                            serverClockOffsetMs = serverClock - System.currentTimeMillis();
                                            Blackaddons.LOGGER.info("Captured server clock offset: {} ms", serverClockOffsetMs);
                                        } catch (Exception ignored) {
                                        }
                                    }
                                    if (responseJson.has("key")) {
                                        String key = responseJson.get("key").getAsString();
                                        EncryptionUtils.setKeyBase64(key);
                                        authKeyFetched = true;
                                        return "fetched_key_present";
                                    }
                                } catch (Exception e) {
                                    Blackaddons.LOGGER.error("Failed to parse bot key JSON: " + e.getMessage());
                                }
                            } else {
                                Blackaddons.LOGGER.warn("Bot /v1/key failed. Status: " + response.statusCode() + " Body: " + response.body());
                            }
                            return null;
                        })
                        .exceptionally(e -> {
                            Blackaddons.LOGGER.error("Failed to fetch bot key: " + e.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to build key request: " + e.getMessage());
                return CompletableFuture.completedFuture(null);
            }
        });

        return activeAuthKeyFuture;
    }

    public static CompletableFuture<Void> authenticateWithBot() {
        return getAuthKey().thenAccept(key -> {
            if (key != null) {
                Blackaddons.LOGGER.info("Successfully fetched developer authentication key.");
            } else {
                Blackaddons.LOGGER.warn("Failed to get developer authentication key.");
            }
        });
    }

    private static CompletableFuture<HttpResponse<String>> sendPostRequest(String endpoint, String jsonBody) {
        return sendRequest("POST", endpoint, jsonBody, true);
    }

    private static CompletableFuture<HttpResponse<String>> sendMojangAuthedPostRequest(String endpoint,
            JsonObject json) {
        if (ConfigManager.data.developerKey != null && !ConfigManager.data.developerKey.isEmpty()) {
            return sendPostRequest(endpoint, json.toString());
        }

        String serverId = MojangAuthService.generateServerId();
        return MojangAuthService.joinServer(serverId).thenCompose(joined -> {
            if (!joined) {
                Blackaddons.LOGGER.warn("Mojang joinServer failed; refusing authenticated bot POST to {}", endpoint);
                return CompletableFuture.completedFuture(null);
            }
            json.addProperty("mojang_server_id", serverId);
            return sendPostRequest(endpoint, json.toString());
        });
    }

    private static CompletableFuture<HttpResponse<String>> sendGetRequest(String endpoint) {
        return sendRequest("GET", endpoint, null, true);
    }

    /**
     * Adds the bearer token to a bot request, if one is configured.
     *
     * A self-hosted botUrl may refuse anonymous requests outright — the identity headers below say
     * who a player is, never that the caller may ask at all. Empty by default, so the official
     * backend sees exactly the request it saw before.
     */
    private static void applyBotToken(HttpRequest.Builder builder) {
        String token = ConfigManager.data.botToken;
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    private static CompletableFuture<HttpResponse<String>> sendRequest(String method, String endpoint, String jsonBody,
            boolean allowRetry) {
        return getAuthKey().thenCompose(key -> {
            String url = ConfigManager.data.botUrl + endpoint;

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", Constants.BOT_USER_AGENT);
            applyBotToken(builder);

            if (ConfigManager.data.developerKey != null && !ConfigManager.data.developerKey.isEmpty()) {
                builder.header(Constants.HEADER_DEVELOPER_KEY, ConfigManager.data.developerKey);
            } else {
                if (EncryptionUtils.isKeySet()) {
                    try {
                        if (MinecraftInstance.mc.getUser() != null) {
                            String uuidStr = MinecraftInstance.mc.getUser().getProfileId().toString();
                            String encryptedId = EncryptionUtils.encryptIdentity(uuidStr);
                            if (encryptedId != null) {
                                builder.header("X-Encrypted-Identity", encryptedId);
                            }
                            builder.header("X-Player-Name", MinecraftInstance.mc.getUser().getName());
                            builder.header("X-Player-UUID", MinecraftInstance.mc.getUser().getProfileId().toString());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (method.equalsIgnoreCase("POST") && jsonBody != null) {
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.GET();
            }

            return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenCompose(res -> {
                        if (res.statusCode() == 403 && allowRetry) {
                            Blackaddons.LOGGER.info("Authentication failed. Regenerating key and retrying...");
                            authKeyFetched = false;
                            return sendRequest(method, endpoint, jsonBody, false);
                        }

                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            Blackaddons.LOGGER.info("Successfully communicated with bot (" + method + "): " + endpoint);
                        } else {
                            Blackaddons.LOGGER.warn(
                                    "Bot communication failed. Status: " + res.statusCode() + " Body: " + res.body());
                        }
                        return CompletableFuture.completedFuture(res);
                    })
                    .exceptionally(e -> {
                        Blackaddons.LOGGER.error("Error communicating with bot: " + e.getMessage());
                        return null;
                    });
        });
    }

}
