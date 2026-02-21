package org.blackum.blackaddons.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.manager.LocalTeammateManager;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.util.HttpUtils;

import java.util.concurrent.CompletableFuture;

public class ProfileService {

    public static CompletableFuture<JsonObject> getProfileStats(String player, String profileName, boolean force) {
        return getUuid(player)
                .thenCompose(uuid -> {
                    if (uuid == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return getProfileData(uuid).thenApply(profileData -> {
                        if (profileData == null)
                            return null;
                        return processProfileData(uuid, profileData, profileName);
                    });
                });
    }

    private static CompletableFuture<String> getUuid(String name) {
        String url = Constants.PLAYER_DB_API + name;
        return HttpUtils.sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                    return json.get("data").getAsJsonObject().get("player").getAsJsonObject().get("raw_id")
                            .getAsString();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse UUID for " + name + ": " + e.getMessage());
                }
            }
            return null;
        });
    }

    private static CompletableFuture<JsonObject> getProfileData(String uuid) {
        String url = Constants.ADJECTILS_PROFILE_API + uuid;
        return HttpUtils.sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse profile data: " + e.getMessage());
                }
            }
            return null;
        });
    }

    private static JsonObject processProfileData(String uuid, JsonObject profileData, String profileName) {
        try {
            if (!profileData.has("profiles") || profileData.get("profiles").isJsonNull())
                return null;

            JsonArray profiles = profileData.getAsJsonArray("profiles");
            if (profiles.isEmpty())
                return null;

            JsonArray profilesList = new JsonArray();
            for (JsonElement p : profiles) {
                JsonObject obj = p.getAsJsonObject();
                JsonObject pData = new JsonObject();
                pData.addProperty("name", obj.get("cute_name").getAsString());
                pData.addProperty("id", obj.get("profile_id").getAsString());
                pData.addProperty("selected", obj.has("selected") && obj.get("selected").getAsBoolean());
                profilesList.add(pData);
            }

            JsonObject selectedProfile = getSelectedProfile(profiles, profileName);
            if (selectedProfile == null)
                return null;

            JsonObject members = selectedProfile.getAsJsonObject("members");

            if (!members.has(uuid))
                return null;

            JsonObject member = members.getAsJsonObject(uuid);
            JsonObject dungeons = member.has("dungeons") ? member.getAsJsonObject("dungeons") : new JsonObject();

            JsonObject result = new JsonObject();
            result.addProperty("catacombs", extractCatacombsXp(dungeons));
            result.addProperty("secrets", extractSecrets(dungeons, member));
            result.addProperty("blood_mob_kills", extractBloodMobKills(member));
            result.add("classes", extractClassXp(dungeons));
            result.add("accessory_bag_storage",
                    member.has("accessory_bag_storage") ? member.getAsJsonObject("accessory_bag_storage")
                            : new JsonObject());
            result.add("floors", extractFloorStats(dungeons));

            JsonArray recentRuns = extractRecentRuns(dungeons, uuid);
            result.add("recent_runs", recentRuns);
            result.add("teammates", LocalTeammateManager.getInstance().getTeammates(uuid));
            result.add("daily_stats", new JsonObject());
            result.add("monthly_stats", new JsonObject());
            result.add("profiles", profilesList);

            return result;
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Error processing profile data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static JsonObject getSelectedProfile(JsonArray profiles, String profileName) {
        JsonObject bestProfile = null;

        if (profileName != null) {
            for (JsonElement p : profiles) {
                JsonObject obj = p.getAsJsonObject();
                if (obj.has("cute_name") && obj.get("cute_name").getAsString().equalsIgnoreCase(profileName)) {
                    return obj;
                }
            }
        }

        for (JsonElement p : profiles) {
            JsonObject obj = p.getAsJsonObject();
            if (obj.has("selected") && obj.get("selected").getAsBoolean()) {
                bestProfile = obj;
                break;
            }
        }
        return (bestProfile != null) ? bestProfile : profiles.get(0).getAsJsonObject();
    }

    private static double extractCatacombsXp(JsonObject dungeons) {
        JsonObject dungeonTypes = dungeons.has("dungeon_types") ? dungeons.getAsJsonObject("dungeon_types")
                : new JsonObject();
        JsonObject catacombs = dungeonTypes.has("catacombs") ? dungeonTypes.getAsJsonObject("catacombs")
                : new JsonObject();
        return catacombs.has("experience") ? catacombs.get("experience").getAsDouble() : 0.0;
    }

    private static int extractSecrets(JsonObject dungeons, JsonObject member) {
        if (dungeons.has("secrets")) {
            return dungeons.get("secrets").getAsInt();
        } else if (member.has("achievements")) {
            JsonObject achievements = member.getAsJsonObject("achievements");
            if (achievements.has("skyblock_treasure_hunter")) {
                return achievements.get("skyblock_treasure_hunter").getAsInt();
            }
        }
        return 0;
    }

    private static int extractBloodMobKills(JsonObject member) {
        if (member.has("player_stats")) {
            JsonObject stats = member.getAsJsonObject("player_stats");
            if (stats.has("kills")) {
                JsonObject kills = stats.getAsJsonObject("kills");
                if (kills.has("watcher_summon_undead")) {
                    return kills.get("watcher_summon_undead").getAsInt();
                }
            }
        }
        return 0;
    }

    private static JsonObject extractClassXp(JsonObject dungeons) {
        JsonObject playerClasses = dungeons.has("player_classes") ? dungeons.getAsJsonObject("player_classes")
                : new JsonObject();
        JsonObject classXp = new JsonObject();
        String[] classes = { "archer", "berserk", "healer", "mage", "tank" };
        for (String cls : classes) {
            double xp = 0;
            if (playerClasses.has(cls)) {
                xp = playerClasses.getAsJsonObject(cls).get("experience").getAsDouble();
            }
            String capitalized = cls.substring(0, 1).toUpperCase() + cls.substring(1);
            classXp.addProperty(capitalized, xp);
        }
        return classXp;
    }

    private static JsonObject extractFloorStats(JsonObject dungeons) {
        JsonObject dungeonTypes = dungeons.has("dungeon_types") ? dungeons.getAsJsonObject("dungeon_types")
                : new JsonObject();
        JsonObject catacombs = dungeonTypes.has("catacombs") ? dungeonTypes.getAsJsonObject("catacombs")
                : new JsonObject();
        JsonObject masterCatacombs = dungeonTypes.has("master_catacombs")
                ? dungeonTypes.getAsJsonObject("master_catacombs")
                : new JsonObject();

        JsonObject floors = new JsonObject();
        processTier(catacombs, "F", floors);
        processTier(masterCatacombs, "M", floors);
        return floors;
    }

    private static JsonArray extractRecentRuns(JsonObject dungeons, String uuid) {
        JsonArray recentRuns = new JsonArray();
        if (dungeons.has("treasures")) {
            JsonObject treasures = dungeons.getAsJsonObject("treasures");
            if (treasures.has("runs")) {
                recentRuns = treasures.getAsJsonArray("runs");
                long minTs = Long.MAX_VALUE;
                for (JsonElement run : recentRuns) {
                    if (run.isJsonObject() && run.getAsJsonObject().has("completion_ts")) {
                        long ts = run.getAsJsonObject().get("completion_ts").getAsLong();
                        if (ts < minTs)
                            minTs = ts;
                    }
                }
                if (minTs != Long.MAX_VALUE) {
                    LocalTeammateManager.getInstance()
                            .setLocalRunWindowStart(minTs / 1000);
                }
                LocalTeammateManager.getInstance().processRuns(uuid, recentRuns);
            }
        }
        return recentRuns;
    }

    private static void processTier(JsonObject tierData, String prefix, JsonObject floors) {
        if (tierData == null)
            return;

        JsonObject timesSPlus = tierData.has("fastest_time_s_plus") ? tierData.getAsJsonObject("fastest_time_s_plus")
                : new JsonObject();
        JsonObject timesS = tierData.has("fastest_time_s") ? tierData.getAsJsonObject("fastest_time_s")
                : new JsonObject();
        JsonObject runs = tierData.has("tier_completions") ? tierData.getAsJsonObject("tier_completions")
                : new JsonObject();
        JsonObject bestScore = tierData.has("best_score") ? tierData.getAsJsonObject("best_score") : new JsonObject();

        for (String tier : runs.keySet()) {
            if ("total".equals(tier))
                continue;

            String floorName;
            if ("0".equals(tier)) {
                floorName = "F".equals(prefix) ? "Entrance" : "M0";
            } else {
                floorName = prefix + tier;
            }

            int runsVal = runs.has(tier) ? runs.get(tier).getAsInt() : 0;
            int bestScoreVal = bestScore.has(tier) ? bestScore.get(tier).getAsInt() : 0;
            int fastSPlus = timesSPlus.has(tier) ? timesSPlus.get(tier).getAsInt() : 0;
            int fastS = timesS.has(tier) ? timesS.get(tier).getAsInt() : 0;

            JsonObject floorObj = new JsonObject();
            floorObj.addProperty("runs", runsVal);
            floorObj.addProperty("best_score", bestScoreVal);
            floorObj.addProperty("fastest_s_plus", fastSPlus);
            floorObj.addProperty("fastest_s", fastS);

            floors.add(floorName, floorObj);
        }
    }
}
