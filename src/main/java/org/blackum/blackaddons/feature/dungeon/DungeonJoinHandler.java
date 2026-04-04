package org.blackum.blackaddons.feature.dungeon;

import org.blackum.blackaddons.core.manager.ProfileStateManager;
import org.blackum.blackaddons.core.util.JsonUtils;
import org.blackum.blackaddons.core.util.FormatUtils;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.util.DungeonUtils;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.blackum.blackaddons.core.config.ConfigManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.blackum.blackaddons.core.util.MinecraftInstance.mc;

public class DungeonJoinHandler {
    private static final Pattern JOIN_PATTERN = Pattern
            .compile("^Party Finder > (.+) joined the dungeon group! \\((.+) Level (\\d+)\\)$");

    public static void onChatMessage(Component message) {
        String content = message.getString();
        Matcher matcher = JOIN_PATTERN.matcher(content);

        if (matcher.find() && ConfigManager.data.partyFinderShowStatsOnJoin) {
            String nickname = matcher.group(1);
            if (nickname.equalsIgnoreCase(mc.getUser().getName())) {
                return;
            }
            String className = matcher.group(2);
            String classLevel = matcher.group(3);

            fetchAndShowStats(nickname, className, classLevel);
        }
    }

    public static void fetchAndShowStats(String nickname, String className, String classLevel) {
        ProfileStateManager.getInstance().getProfile(nickname, null, false).thenAccept(result -> {
            if (result == null || result.hasError() || result.getData() == null) {
                return;
            }

            JsonObject data = result.getData();
            mc.execute(() -> displayStats(nickname, data));
        });
    }

    private static void displayStats(String nickname, JsonObject data) {
        double cataXp = JsonUtils.getDouble(data, "catacombs");
        double cataLvl = DungeonUtils.getCataLevel(cataXp);
        int secrets = JsonUtils.getInt(data, "secrets");

        int totalRuns = 0;
        JsonObject floors = data.has("floors") ? data.getAsJsonObject("floors") : new JsonObject();
        for (String key : floors.keySet()) {
            totalRuns += JsonUtils.getInt(floors.getAsJsonObject(key), "runs");
        }

        double secretsPerRun = totalRuns > 0 ? (double) secrets / totalRuns : 0;

        String m7Runs = "0";
        String m7PbSPlus = "None";
        String m7PbS = "None";

        if (floors.has("M7")) {
            JsonObject m7 = floors.getAsJsonObject("M7");
            m7Runs = String.valueOf(JsonUtils.getInt(m7, "runs"));
            m7PbSPlus = FormatUtils.formatMs(JsonUtils.getInt(m7, "fastest_s_plus"));
            m7PbS = FormatUtils.formatMs(JsonUtils.getInt(m7, "fastest_s"));
        }

        MutableComponent response = Component.empty()
                .append(ChatUtils.getPrefix())
                .append(Component.literal("Stats for ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(nickname).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(":\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Cata: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.2f", cataLvl)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("\nSecrets average: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.2f/per", secretsPerRun)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("\nM7 runs: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(m7Runs).withStyle(ChatFormatting.RED))
                .append(Component.literal("\nM7 pb S+: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(m7PbSPlus).withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" S: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(m7PbS).withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal("\n"));

        MutableComponent openPv = Component.literal("[Open PV]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/" + Constants.BASE_COMMAND + " pv " + nickname))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to open full profile viewer for " + nickname))));

        MutableComponent kickBtn = Component.literal(" [Kick]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/party kick " + nickname))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("Click to kick " + nickname + " from the party"))));

        response.append(openPv).append(kickBtn);

        mc.gui.getChat().addClientSystemMessage(response);
    }
}
