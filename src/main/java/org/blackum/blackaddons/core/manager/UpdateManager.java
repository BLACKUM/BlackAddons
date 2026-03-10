package org.blackum.blackaddons.core.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.util.HttpUtils;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.gui.render.Theme;

import static org.blackum.blackaddons.core.util.MinecraftInstance.mc;

public class UpdateManager {

    public static void check() {
        HttpUtils.sendGetRequest(Constants.GITHUB_LATEST_RELEASE_API).thenAccept(response -> {
            if (response == null || response.statusCode() != 200) return;

            try {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String latestTag = json.get("tag_name").getAsString();
                String currentVersion = FabricLoader.getInstance().getModContainer(Constants.MOD_ID).get()
                        .getMetadata().getVersion().getFriendlyString();

                if (isNewer(latestTag, currentVersion)) {
                    mc.execute(() -> {
                        if (mc.player != null) {
                            MutableComponent message = ChatUtils.getPrefix()
                                    .append(Component.literal("Update for BlackAddons is available! ").withStyle(ChatFormatting.GRAY))
                                    .append(Component.literal("[Open]")
                                            .withStyle(Style.EMPTY
                                                    .withColor(TextColor.fromRgb(Theme.ACCENT))
                                                    .withBold(true)
                                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(Constants.GITHUB_RELEASES_URL)))
                                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open GitHub releases")))));
                            mc.gui.getChat().addMessage(message);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static boolean isNewer(String latest, String current) {
        String v1 = latest.replaceAll("[^0-9.]", "");
        String v2 = current.replaceAll("[^0-9.]", "");

        String[] latestParts = v1.split("\\.");
        String[] currentParts = v2.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int lp = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int cp = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (lp > cp) return true;
            if (lp < cp) return false;
        }
        return false;
    }
}
