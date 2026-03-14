package org.blackum.blackaddons.core.manager;

import org.blackum.blackaddons.core.util.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.blackum.blackaddons.Blackaddons;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

public class LocalTeammateManager {
    private static LocalTeammateManager instance;
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME).resolve(Constants.DATA_DIR_NAME);
    private static final File DATA_FILE = DATA_DIR.resolve(Constants.TEAMMATES_FILE_NAME).toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("§.");

    private JsonObject data = new JsonObject();

    private LocalTeammateManager() {
        migrate();
        load();
    }

    public static synchronized LocalTeammateManager getInstance() {
        if (instance == null) {
            instance = new LocalTeammateManager();
        }
        return instance;
    }

    private void load() {
        if (!DATA_FILE.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(DATA_FILE)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                data = parsed.getAsJsonObject();
            }
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to load local teammates data", e);
        }
    }

    private void migrate() {
        File oldFile = FabricLoader.getInstance().getConfigDir()
                .resolve(Constants.CONFIG_DIR_NAME)
                .resolve(Constants.TEAMMATES_FILE_NAME).toFile();

        if (oldFile.exists() && !DATA_FILE.exists()) {
            try {
                if (!DATA_DIR.toFile().exists()) {
                    DATA_DIR.toFile().mkdirs();
                }
                if (oldFile.renameTo(DATA_FILE)) {
                    Blackaddons.LOGGER.info("Successfully migrated teammates.json to data folder");
                }
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to migrate teammates.json", e);
            }
        }
    }

    private void save() {
        try {
            if (!DATA_FILE.getParentFile().exists()) {
                DATA_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            Blackaddons.LOGGER.error("Failed to save local teammates data", e);
        }
    }

    public void processRuns(String userUuid, JsonArray runs) {
        if (runs == null || runs.isEmpty())
            return;

        if (!data.has(userUuid)) {
            data.add(userUuid, new JsonObject());
        }
        JsonObject userData = data.getAsJsonObject(userUuid);

        if (!userData.has("_meta")) {
            userData.add("_meta", new JsonObject());
        }
        JsonObject meta = userData.getAsJsonObject("_meta");
        long lastScan = meta.has("last_scan_ts") ? meta.get("last_scan_ts").getAsLong() : 0;
        long newScanTs = lastScan;

        boolean updated = false;

        List<JsonObject> sortedRuns = new ArrayList<>();
        for (JsonElement runElem : runs) {
            if (runElem.isJsonObject()) {
                sortedRuns.add(runElem.getAsJsonObject());
            }
        }
        sortedRuns.sort(Comparator.comparingLong(r -> r.has("completion_ts") ? r.get("completion_ts").getAsLong() : 0));

        for (JsonObject run : sortedRuns) {
            long ts = run.has("completion_ts") ? run.get("completion_ts").getAsLong() : 0;
            long tsSeconds = ts / 1000;

            if (tsSeconds <= lastScan)
                continue;

            if (tsSeconds > newScanTs) {
                newScanTs = tsSeconds;
            }

            String dType = run.has("dungeon_type") ? run.get("dungeon_type").getAsString() : "catacombs";
            int tier = run.has("dungeon_tier") ? run.get("dungeon_tier").getAsInt() : 0;
            boolean isMaster = dType.contains("master");
            String floorPrefix = isMaster ? "M" : "F";
            String floorName = floorPrefix + tier;
            if (tier == 0 && !isMaster)
                floorName = "Entrance";

            if (run.has("participants")) {
                JsonArray participants = run.getAsJsonArray("participants");
                for (JsonElement pElem : participants) {
                    if (!pElem.isJsonObject())
                        continue;
                    JsonObject p = pElem.getAsJsonObject();

                    String pUuid = p.has("player_uuid") ? p.get("player_uuid").getAsString() : null;
                    if (pUuid == null || pUuid.equals(userUuid))
                        continue;

                    if (!userData.has(pUuid)) {
                        JsonObject newTm = new JsonObject();
                        newTm.addProperty("ign", "Unknown");
                        newTm.addProperty("count", 0);
                        newTm.addProperty("last_floor", floorName);
                        newTm.addProperty("last_ts", tsSeconds);
                        newTm.addProperty("last_class", "Unknown");
                        newTm.addProperty("last_class_level", 0);
                        userData.add(pUuid, newTm);
                    }

                    JsonObject tm = userData.getAsJsonObject(pUuid);
                    tm.addProperty("count", tm.get("count").getAsInt() + 1);
                    tm.addProperty("last_floor", floorName);
                    tm.addProperty("last_ts", tsSeconds);

                    String rawName = p.has("display_name") ? p.get("display_name").getAsString() : "Unknown";
                    String cleanName = cleanMcFormatting(rawName);

                    String ign = cleanName.split(":")[0].trim();
                    tm.addProperty("ign", ign);

                    if (cleanName.contains(":")) {
                        String[] parts = cleanName.split(":");
                        if (parts.length > 1) {
                            String classPart = parts[1].trim();
                            if (classPart.contains("(")) {
                                String className = classPart.split("\\(")[0].trim();
                                tm.addProperty("last_class", className);
                                try {
                                    String lvlStr = classPart.split("\\(")[1].replace(")", "").trim();
                                    int lvl = Integer.parseInt(lvlStr);
                                    tm.addProperty("last_class_level", lvl);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                    updated = true;
                }
            }
        }

        if (updated) {
            meta.addProperty("last_scan_ts", newScanTs);
            save();
        }
    }

    public JsonArray getTeammates(String userUuid) {
        if (!data.has(userUuid))
            return new JsonArray();

        JsonObject userData = data.getAsJsonObject(userUuid);
        List<JsonObject> teammates = new ArrayList<>();

        for (String key : userData.keySet()) {
            if (key.equals("_meta"))
                continue;
            teammates.add(userData.getAsJsonObject(key));
        }

        teammates.sort((a, b) -> Integer.compare(b.get("count").getAsInt(), a.get("count").getAsInt()));

        JsonArray result = new JsonArray();
        for (JsonObject tm : teammates) {
            result.add(tm);
        }
        return result;
    }

    private long localRunWindowStart = 0;

    public void setLocalRunWindowStart(long ts) {
        this.localRunWindowStart = ts;
    }

    public JsonArray mergeTeammates(JsonArray localList, JsonArray botList) {
        if (botList == null || botList.isEmpty())
            return localList != null ? localList : new JsonArray();
        if (localList == null || localList.isEmpty())
            return botList;

        Map<String, JsonObject> mergedMap = new HashMap<>();

        for (JsonElement elem : localList) {
            if (!elem.isJsonObject())
                continue;
            JsonObject obj = elem.getAsJsonObject();
            if (!obj.has("ign"))
                continue;
            mergedMap.put(obj.get("ign").getAsString().toLowerCase(), obj.deepCopy());
        }

        for (JsonElement elem : botList) {
            String ign = null;
            JsonObject botData = null;

            if (elem.isJsonArray()) {
                JsonArray tuple = elem.getAsJsonArray();
                if (tuple.size() >= 2) {
                    ign = tuple.get(0).getAsString();
                    botData = tuple.get(1).getAsJsonObject();
                }
            } else if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.has("ign")) {
                    ign = obj.get("ign").getAsString();
                    botData = obj;
                }
            }

            if (ign == null || botData == null)
                continue;

            String key = ign.toLowerCase();
            int botCount = botData.has("count") ? botData.get("count").getAsInt() : 0;
            long botLastTs = botData.has("last_ts") ? botData.get("last_ts").getAsLong() : 0;

            if (mergedMap.containsKey(key)) {
                JsonObject localData = mergedMap.get(key);
                int localCount = localData.has("count") ? localData.get("count").getAsInt() : 0;

                int finalCount;
                if (localRunWindowStart > 0 && botLastTs < localRunWindowStart) {
                    finalCount = localCount + botCount;
                } else {
                    finalCount = Math.max(localCount, botCount);
                }

                localData.addProperty("count", finalCount);

                long localLastTs = localData.has("last_ts") ? localData.get("last_ts").getAsLong() : 0;
                if (botLastTs > localLastTs) {
                    localData.addProperty("last_ts", botLastTs);
                    if (botData.has("last_floor"))
                        localData.addProperty("last_floor", botData.get("last_floor").getAsString());
                    if (botData.has("last_class"))
                        localData.addProperty("last_class", botData.get("last_class").getAsString());
                    if (botData.has("last_class_level"))
                        localData.addProperty("last_class_level", botData.get("last_class_level").getAsInt());
                }
            } else {
                JsonObject newData = botData.deepCopy();
                if (!newData.has("ign"))
                    newData.addProperty("ign", ign);
                mergedMap.put(key, newData);
            }
        }

        List<JsonObject> resultList = new ArrayList<>(mergedMap.values());
        resultList.sort((a, b) -> {
            int c1 = a.has("count") ? a.get("count").getAsInt() : 0;
            int c2 = b.has("count") ? b.get("count").getAsInt() : 0;
            return Integer.compare(c2, c1);
        });

        JsonArray result = new JsonArray();
        for (JsonObject tm : resultList) {
            result.add(tm);
        }
        return result;
    }

    private String cleanMcFormatting(String text) {
        if (text == null)
            return "";
        return FORMATTING_CODE_PATTERN.matcher(text).replaceAll("").trim();
    }
}
