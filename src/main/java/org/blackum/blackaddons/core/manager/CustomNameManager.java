package org.blackum.blackaddons.core.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
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
        HttpRequest githubRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.GITHUB_NAMES_URL))
                .header("User-Agent", Constants.BOT_USER_AGENT)
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();

        httpClient.sendAsync(githubRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 && response.body() != null) {
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            parseAndApplyNames(json);
                            return;
                        } catch (Exception e) {
                            Blackaddons.LOGGER.warn("Failed to parse GitHub custom names, falling back to bot.", e);
                        }
                    } else {
                        Blackaddons.LOGGER.warn("GitHub custom names returned status " + response.statusCode()
                                + ", falling back to bot.");
                    }

                    fetchFromBotFallback();
                })
                .exceptionally(ex -> {
                    Blackaddons.LOGGER.warn("Error fetching GitHub custom names, falling back to bot.", ex);
                    fetchFromBotFallback();
                    return null;
                });
    }

    private void fetchFromBotFallback() {
        if (ConfigManager.data == null || ConfigManager.data.botUrl == null || ConfigManager.data.botUrl.isEmpty()) {
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
                .thenAccept(response -> {
                    if (response.statusCode() == 200 && response.body() != null) {
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            if (json.has("names")) {
                                parseAndApplyNames(json.getAsJsonObject("names"));
                            }
                        } catch (Exception e) {
                            Blackaddons.LOGGER.error("Failed to parse bot custom names.", e);
                        }
                    }
                })
                .exceptionally(ex -> {
                    Blackaddons.LOGGER.error("Error fetching bot custom names.", ex);
                    return null;
                });
    }

    private void parseAndApplyNames(JsonObject namesObj) {
        customNames.clear();
        for (Map.Entry<String, JsonElement> entry : namesObj.entrySet()) {
            JsonObject data = entry.getValue().getAsJsonObject();
            String displayName = data.has("display") ? data.get("display").getAsString() : entry.getKey();
            String color = data.has("color") ? data.get("color").getAsString() : "";
            boolean animated = data.has("animated") && data.get("animated").getAsBoolean();
            boolean chroma = data.has("chroma") && data.get("chroma").getAsBoolean();
            float speed = data.has("speed") ? data.get("speed").getAsFloat() : 1.0f;

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
                    boolean isLinear = gradientStr.startsWith("linear-gradient");
                    boolean isRadial = gradientStr.startsWith("radial-gradient");

                    if (isLinear || isRadial) {
                        Matcher m = Pattern.compile(
                                "rgba\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*[^)]*\\)\\s*(\\d+)%")
                                .matcher(gradientStr);
                        while (m.find()) {
                            int r = Integer.parseInt(m.group(1));
                            int g = Integer.parseInt(m.group(2));
                            int b = Integer.parseInt(m.group(3));
                            float fraction = Float.parseFloat(m.group(4)) / 100.0f;

                            int rgb = (r << 16) | (g << 8) | b;

                            if (isRadial) {
                                float leftFraction = 0.5f - (fraction / 2.0f);
                                float rightFraction = 0.5f + (fraction / 2.0f);

                                gradientStops.add(new ChatUtils.ColorStop(rgb, leftFraction));
                                if (leftFraction != rightFraction) {
                                    gradientStops.add(new ChatUtils.ColorStop(rgb, rightFraction));
                                }
                            } else {
                                gradientStops.add(new ChatUtils.ColorStop(rgb, fraction));
                            }
                        }
                        Collections.sort(gradientStops,
                                (a, b) -> Float.compare(a.fraction(), b.fraction()));
                    }
                }
            }

            customNames.put(entry.getKey().toLowerCase(),
                    new CustomName(displayName, color, gradientStops, animated, chroma, speed));
        }
        Blackaddons.LOGGER.info("Successfully fetched " + customNames.size() + " custom names.");
    }

    public Component replaceNames(Component component) {
        if (customNames.isEmpty()) {
            return component;
        }

        return processComponent(component);
    }

    public String replaceInString(String text) {
        if (customNames.isEmpty() || text == null || text.isEmpty()) return text;
        String lower = text.toLowerCase();
        if (!containsAnyIgn(lower)) return text;
        for (String ign : customNames.keySet()) {
            if (lower.contains(ign)) {
                CustomName custom = customNames.get(ign);
                text = text.replaceAll("(?i)" + Pattern.quote(ign), custom.display());
            }
        }
        return text;
    }

    public FormattedCharSequence replaceInSequence(FormattedCharSequence sequence) {
        if (customNames.isEmpty() || sequence == null) return sequence;
        StringBuilder sb = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        String plain = sb.toString();
        if (!containsAnyIgn(plain.toLowerCase())) return sequence;

        MutableComponent rebuilt = Component.literal("");
        Style[] currentStyle = new Style[]{null};
        StringBuilder buffer = new StringBuilder();

        sequence.accept((index, style, codePoint) -> {
            if (!java.util.Objects.equals(style, currentStyle[0])) {
                if (buffer.length() > 0) {
                    rebuilt.append(Component.literal(buffer.toString()).withStyle(currentStyle[0] != null ? currentStyle[0] : Style.EMPTY));
                    buffer.setLength(0);
                }
                currentStyle[0] = style;
            }
            buffer.appendCodePoint(codePoint);
            return true;
        });

        if (buffer.length() > 0) {
            rebuilt.append(Component.literal(buffer.toString()).withStyle(currentStyle[0] != null ? currentStyle[0] : Style.EMPTY));
        }

        return net.minecraft.locale.Language.getInstance().getVisualOrder(replaceNames(rebuilt));
    }

    private boolean containsAnyIgn(String lowerText) {
        for (String ign : customNames.keySet()) {
            if (lowerText.contains(ign)) return true;
        }
        return false;
    }

    private Component processComponent(Component component) {
        MutableComponent newComponent = Component.empty();
        newComponent.setStyle(component.getStyle());

        if (component.getContents() instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component argComponent) {
                    newArgs[i] = processComponent(argComponent);
                } else {
                    newArgs[i] = args[i];
                }
            }
            MutableComponent rebuilt = MutableComponent.create(
                    new TranslatableContents(translatable.getKey(), translatable.getFallback(), newArgs));
            rebuilt.setStyle(component.getStyle());
            for (Component sibling : component.getSiblings()) {
                rebuilt.append(processComponent(sibling));
            }
            return rebuilt;
        } else if (component.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents literal) {
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

        if (custom.chroma()) {
            return ChatUtils.BuildChroma(custom.display(), custom.speed());
        }

        if (custom.gradientStops() != null && !custom.gradientStops().isEmpty()) {
            try {
                if (custom.animated()) {
                    return ChatUtils.BuildAnimatedMultiGradient(custom.display(), custom.gradientStops(),
                            custom.speed());
                }
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

    public record CustomName(String display, String color, List<ChatUtils.ColorStop> gradientStops, boolean animated,
            boolean chroma, float speed) {
    }
}
