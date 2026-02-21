package org.blackum.blackaddons.core.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.feature.chat.ChatUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class CustomNameManager {
    private static CustomNameManager instance;
    private final Map<String, CustomName> customNames = new ConcurrentHashMap<>();
    private final HttpClient httpClient;

    private CustomNameManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .build();
    }

    public static CustomNameManager getInstance() {
        if (instance == null) {
            instance = new CustomNameManager();
        }
        return instance;
    }

    public void fetch() {
        if (ConfigManager.data.botUrl.isEmpty()) {
            return;
        }

        String url = ConfigManager.data.botUrl + Constants.BOT_API_NAMES;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", Constants.BOT_USER_AGENT)
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    }
                    return null;
                })
                .thenAccept(body -> {
                    if (body != null) {
                        try {
                            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                            if (json.has("names")) {
                                JsonObject namesObj = json.getAsJsonObject("names");
                                customNames.clear();
                                for (Map.Entry<String, JsonElement> entry : namesObj.entrySet()) {
                                    JsonObject data = entry.getValue().getAsJsonObject();
                                    String displayName = data.get("display").getAsString();
                                    String color = data.has("color") ? data.get("color").getAsString() : "";

                                    List<ChatUtils.ColorStop> gradientStops = new ArrayList<>();
                                    if (data.has("gradient")) {
                                        JsonElement gradientElement = data.get("gradient");

                                        if (gradientElement.isJsonArray()) {
                                            JsonArray gradient = gradientElement.getAsJsonArray();
                                            if (gradient.size() >= 2) {
                                                try {
                                                    int start = Integer.parseInt(
                                                            gradient.get(0).getAsString().replace("#", ""), 16);
                                                    int end = Integer.parseInt(
                                                            gradient.get(1).getAsString().replace("#", ""), 16);
                                                    gradientStops.add(new ChatUtils.ColorStop(start, 0.0f));
                                                    gradientStops.add(new ChatUtils.ColorStop(end, 1.0f));
                                                } catch (NumberFormatException ignored) {
                                                }
                                            }
                                        } else if (gradientElement.isJsonPrimitive()) {
                                            String gradientStr = gradientElement.getAsString();
                                            if (gradientStr.startsWith("linear-gradient")) {
                                                Matcher m = Pattern.compile(
                                                        "rgba\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*[^)]*\\)\\s*(\\d+)%")
                                                        .matcher(gradientStr);
                                                while (m.find()) {
                                                    int r = Integer.parseInt(m.group(1));
                                                    int g = Integer.parseInt(m.group(2));
                                                    int b = Integer.parseInt(m.group(3));
                                                    float fraction = Float.parseFloat(m.group(4)) / 100.0f;

                                                    int rgb = (r << 16) | (g << 8) | b;
                                                    gradientStops.add(new ChatUtils.ColorStop(rgb, fraction));
                                                }
                                                Collections.sort(gradientStops,
                                                        (a, b) -> Float.compare(a.fraction(), b.fraction()));
                                            }
                                        }
                                    }

                                    customNames.put(entry.getKey().toLowerCase(),
                                            new CustomName(displayName, color, gradientStops));
                                }
                                Blackaddons.LOGGER
                                        .info("Successfully fetched " + customNames.size() + " custom names.");
                            }
                        } catch (Exception e) {
                            Blackaddons.LOGGER.error("Failed to parse custom names: " + e.getMessage());
                        }
                    }
                })
                .exceptionally(ex -> {
                    Blackaddons.LOGGER.error("Error fetching custom names: " + ex.getMessage());
                    return null;
                });
    }

    public Component replaceNames(Component component) {
        if (customNames.isEmpty()) {
            return component;
        }

        return processComponent(component);
    }

    private Component processComponent(Component component) {
        MutableComponent newComponent = Component.empty();
        newComponent.setStyle(component.getStyle());

        if (component.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents literal) {
            String text = literal.text();
            String lowerText = text.toLowerCase();
            boolean found = false;

            for (String ign : customNames.keySet()) {
                if (lowerText.contains(ign)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                newComponent = component.plainCopy();
                newComponent.setStyle(component.getStyle());
            } else {
                int currentPos = 0;
                while (currentPos < text.length()) {
                    String earliestIgn = null;
                    int earliestIdx = -1;

                    for (String ign : customNames.keySet()) {
                        int idx = lowerText.indexOf(ign, currentPos);
                        if (idx != -1 && (earliestIdx == -1 || idx < earliestIdx)) {
                            earliestIdx = idx;
                            earliestIgn = ign;
                        }
                    }

                    if (earliestIgn == null) {
                        newComponent.append(Component.literal(text.substring(currentPos)));
                        break;
                    }

                    if (earliestIdx > currentPos) {
                        newComponent.append(Component.literal(text.substring(currentPos, earliestIdx)));
                    }

                    newComponent.append(applyCustomName(earliestIgn, Component.literal(earliestIgn)));
                    currentPos = earliestIdx + earliestIgn.length();
                }
            }
        } else {
            newComponent = component.plainCopy();
            newComponent.setStyle(component.getStyle());
        }

        for (Component sibling : component.getSiblings()) {
            newComponent.append(processComponent(sibling));
        }

        return newComponent;
    }

    public Component applyCustomName(String username, Component originalComponent) {
        CustomName custom = customNames.get(username.toLowerCase());
        if (custom == null) {
            return originalComponent;
        }

        if (custom.gradientStops() != null && !custom.gradientStops().isEmpty()) {
            try {
                return ChatUtils.BuildMultiGradient(custom.display(), custom.gradientStops());
            } catch (Exception e) {
            }
        }

        if (custom.display().contains("§")) {
            return Component.literal(custom.display());
        }

        if (custom.color() != null && !custom.color().isEmpty()) {
            try {
                TextColor color = TextColor.parseColor(custom.color()).getOrThrow();
                return Component.literal(custom.display()).withStyle(Style.EMPTY.withColor(color));
            } catch (Exception e) {
            }
        }

        return Component.literal(custom.display());
    }

    public CustomName getCustomName(String username) {
        return customNames.get(username.toLowerCase());
    }

    public record CustomName(String display, String color, List<ChatUtils.ColorStop> gradientStops) {
    }
}
