package org.blackum.blackaddons.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.util.HttpUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class PriceService {

    private static Map<String, Double> priceCache = new ConcurrentHashMap<>();
    private static final AtomicLong priceCacheExpiry = new AtomicLong(0);
    private static final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private static final AtomicBoolean hasLoadedLocalCache = new AtomicBoolean(false);

    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME).resolve(Constants.DATA_DIR_NAME);
    private static final File PRICES_FILE = DATA_DIR.resolve(Constants.PRICES_FILE_NAME).toFile();

    private static class PricesData {
        long timestamp;
        Map<String, Double> prices;
    }

    public static Double getPrice(String itemId) {
        if (System.currentTimeMillis() > priceCacheExpiry.get()) {
            checkAndRefreshPrices();
        }
        return priceCache.getOrDefault(itemId, 0.0);
    }

    private static void checkAndRefreshPrices() {
        if (hasLoadedLocalCache.compareAndSet(false, true)) {
            loadPrices();
        }
        if (System.currentTimeMillis() > priceCacheExpiry.get()) {
            refreshPrices();
        }
    }

    private static void refreshPrices() {
        if (!isRefreshing.compareAndSet(false, true)) {
            return;
        }
        Blackaddons.LOGGER.info("Refreshing local prices...");
        CompletableFuture<Map<String, Double>> bzFuture = getBazaarPrices();
        CompletableFuture<Map<String, Double>> ahFuture = getAhPrices();
        CompletableFuture<Map<String, Double>> specialFuture = getSpecialPrices();

        CompletableFuture.allOf(bzFuture, ahFuture, specialFuture).thenRun(() -> {
            Map<String, Double> newPrices = new HashMap<>();
            try {
                newPrices.putAll(bzFuture.get());
                newPrices.putAll(ahFuture.get());
                newPrices.putAll(specialFuture.get());

                if (newPrices.containsKey("SKELETON_MASTER_CHESTPLATE")) {
                    newPrices.put("SKELETON_MASTER_CHESTPLATE_50", newPrices.get("SKELETON_MASTER_CHESTPLATE"));
                }

                priceCache = new ConcurrentHashMap<>(newPrices);
                priceCacheExpiry.set(System.currentTimeMillis() + Constants.PRICE_CACHE_DURATION_MS);
                savePrices(newPrices);
                Blackaddons.LOGGER.info("Local prices refreshed. Total items: " + newPrices.size());
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to merge prices: " + e.getMessage());
            } finally {
                isRefreshing.set(false);
            }
        });
    }

    private static CompletableFuture<Map<String, Double>> getBazaarPrices() {
        return HttpUtils.sendGetRequest(Constants.HYPIXEL_BAZAAR_API).thenApply(res -> {
            Map<String, Double> prices = new HashMap<>();
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                    JsonObject products = json.getAsJsonObject("products");
                    for (String key : products.keySet()) {
                        JsonObject product = products.getAsJsonObject(key);
                        double sellPrice = product.getAsJsonObject("quick_status").get("sellPrice").getAsDouble();
                        prices.put(key, sellPrice);
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Error parsing Bazaar prices: " + e.getMessage());
                }
            }
            return prices;
        });
    }

    private static CompletableFuture<Map<String, Double>> getAhPrices() {
        return HttpUtils.sendGetRequest(Constants.MOULBERRY_AH_API).thenApply(res -> {
            Map<String, Double> prices = new HashMap<>();
            if (res != null && res.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                    for (String key : json.keySet()) {
                        prices.put(key, json.get(key).getAsDouble());
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Error parsing AH prices: " + e.getMessage());
                }
            }
            return prices;
        });
    }

    private static CompletableFuture<Map<String, Double>> getSpecialPrices() {
        Map<String, String[]> specials = new HashMap<>();
        specials.put("SHINY_NECRON_HANDLE", new String[] { Constants.COFL_SHINY_NECRON_HANDLE });
        specials.put("SKELETON_MASTER_CHESTPLATE", new String[] {
                Constants.COFL_SKELETON_MASTER_CHESTPLATE_MAX,
                Constants.COFL_SKELETON_MASTER_CHESTPLATE_BASE
        });

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<String, Double> results = new ConcurrentHashMap<>();

        for (Map.Entry<String, String[]> entry : specials.entrySet()) {
            CompletableFuture<Void> itemFuture = CompletableFuture.completedFuture(null);

            for (String url : entry.getValue()) {
                itemFuture = itemFuture.thenCompose(v -> {
                    if (results.containsKey(entry.getKey()) && results.get(entry.getKey()) > 0) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return HttpUtils.sendGetRequest(url).thenAccept(res -> {
                        if (res != null && res.statusCode() == 200) {
                            try {
                                JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
                                double price = json.has("median") ? json.get("median").getAsDouble()
                                        : (json.has("min") ? json.get("min").getAsDouble() : 0);
                                if (price > 0)
                                    results.put(entry.getKey(), price);
                            } catch (Exception e) {
                            }
                        }
                    });
                });
            }
            futures.add(itemFuture);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> new HashMap<>(results));
    }

    private static void savePrices(Map<String, Double> prices) {
        if (!DATA_DIR.toFile().exists()) {
            DATA_DIR.toFile().mkdirs();
        }

        PricesData data = new PricesData();
        data.timestamp = System.currentTimeMillis();
        data.prices = prices;

        try (FileWriter writer = new FileWriter(PRICES_FILE)) {
            Constants.GSON.toJson(data, writer);
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Failed to save prices: " + e.getMessage());
        }
    }

    private static void loadPrices() {
        migrate();
        if (!PRICES_FILE.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(PRICES_FILE)) {
            PricesData data = Constants.GSON.fromJson(reader, PricesData.class);
            if (data != null && data.prices != null) {
                long age = System.currentTimeMillis() - data.timestamp;
                if (age < Constants.PRICE_CACHE_DURATION_MS) {
                    priceCache = new ConcurrentHashMap<>(data.prices);
                    priceCacheExpiry.set(data.timestamp + Constants.PRICE_CACHE_DURATION_MS);
                    Blackaddons.LOGGER.info("Loaded prices from local cache. Age: " + (age / 1000 / 60) + "m");
                } else {
                    Blackaddons.LOGGER.info("Local price cache expired.");
                }
            }
        } catch (Exception e) {
            Blackaddons.LOGGER.error("Failed to load prices: " + e.getMessage());
        }
    }

    private static void migrate() {
        File oldFile = FabricLoader.getInstance().getConfigDir()
                .resolve(Constants.CONFIG_DIR_NAME)
                .resolve(Constants.PRICES_FILE_NAME).toFile();

        if (oldFile.exists() && !PRICES_FILE.exists()) {
            try {
                if (!DATA_DIR.toFile().exists()) {
                    DATA_DIR.toFile().mkdirs();
                }
                if (oldFile.renameTo(PRICES_FILE)) {
                    Blackaddons.LOGGER.info("Successfully migrated prices.json to data folder");
                }
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to migrate prices.json", e);
            }
        }
    }
}
