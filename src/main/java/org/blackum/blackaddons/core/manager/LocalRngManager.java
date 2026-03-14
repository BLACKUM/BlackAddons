package org.blackum.blackaddons.core.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.util.Constants;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class LocalRngManager {
    private static LocalRngManager instance;
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME).resolve(Constants.DATA_DIR_NAME);
    private static final File DATA_FILE = DATA_DIR.resolve(Constants.RNG_DATA_FILE_NAME).toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonObject data = new JsonObject();

    private LocalRngManager() {
        migrate();
        load();
    }

    public static synchronized LocalRngManager getInstance() {
        if (instance == null) {
            instance = new LocalRngManager();
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
            Blackaddons.LOGGER.error("Failed to load local RNG data", e);
        }
    }

    private void migrate() {
        File oldFile = FabricLoader.getInstance().getConfigDir()
                .resolve(Constants.CONFIG_DIR_NAME)
                .resolve(Constants.RNG_DATA_FILE_NAME).toFile();

        if (oldFile.exists() && !DATA_FILE.exists()) {
            try {
                if (!DATA_DIR.toFile().exists()) {
                    DATA_DIR.toFile().mkdirs();
                }
                if (oldFile.renameTo(DATA_FILE)) {
                    Blackaddons.LOGGER.info("Successfully migrated rng_data.json to data folder");
                }
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to migrate rng_data.json", e);
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
            Blackaddons.LOGGER.error("Failed to save local RNG data", e);
        }
    }

    public void addDrop(String category, String item, int count) {
        if (!data.has("drops")) {
            data.add("drops", new JsonObject());
        }
        JsonObject drops = data.getAsJsonObject("drops");

        if (!drops.has(category)) {
            drops.add(category, new JsonObject());
        }
        JsonObject catDrops = drops.getAsJsonObject(category);

        int current = catDrops.has(item) ? catDrops.get(item).getAsInt() : 0;
        catDrops.addProperty(item, current + count);

        save();
    }

    public void setDropCount(String category, String item, int count) {
        if (!data.has("drops")) {
            data.add("drops", new JsonObject());
        }
        JsonObject drops = data.getAsJsonObject("drops");

        if (!drops.has(category)) {
            drops.add(category, new JsonObject());
        }
        JsonObject catDrops = drops.getAsJsonObject(category);

        catDrops.addProperty(item, count);

        save();
    }

    public int getDropCount(String category, String item) {
        if (data.has("drops")) {
            JsonObject drops = data.getAsJsonObject("drops");
            if (drops.has(category)) {
                JsonObject catDrops = drops.getAsJsonObject(category);
                if (catDrops.has(item)) {
                    return catDrops.get(item).getAsInt();
                }
            }
        }
        return 0;
    }

    public JsonObject getRngData() {
        return data;
    }
}
