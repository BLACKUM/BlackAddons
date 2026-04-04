package org.blackum.blackaddons.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.integration.BotIntegration;
import org.blackum.blackaddons.core.util.JsonUtils;
import org.blackum.blackaddons.core.util.MinecraftInstance;

import java.util.List;

public class SoloLeaderboardScreen extends BaseScreen {

    private TabPanel floorTabs;
    private String selectedFloor;

    private static final List<String> FLOORS = List.of("F7", "M7");

    private static final int GOLD = 0xFFFFD700;
    private static final int SILVER = 0xFFC0C0C0;
    private static final int BRONZE = 0xFFCD7F32;

    public SoloLeaderboardScreen(String initialFloor) {
        this(initialFloor, null);
    }

    public SoloLeaderboardScreen(String initialFloor, Screen parent) {
        super(Component.literal("Solo Clear Leaderboard"), parent);
        this.selectedFloor = initialFloor != null ? initialFloor.toUpperCase() : "F7";
    }

    @Override
    public void initWidgets() {
        int contentX = containerX + 16;
        int contentY = containerY + 16;
        int contentW = containerWidth - 32;
        int contentH = containerHeight - 32;

        widgets.add(new Label(contentX, contentY, "⚔  Solo Clear Leaderboard", Label.Style.TITLE));

        Button refreshBtn = new Button(contentX + contentW - 90, contentY - 2, 85, 22, "⟳  Refresh",
                this::refreshLeaderboard);
        widgets.add(refreshBtn);

        int tabsY = contentY + 30;
        floorTabs = new TabPanel(contentX, tabsY, contentW, contentH - 34);

        floorTabs.setOnTabChange(index -> {
            selectedFloor = FLOORS.get(index);
            refreshLeaderboard();
        });

        for (String floor : FLOORS) {
            TabPanel.Tab tab = floorTabs.addTab(floor);
            int w = floorTabs.getContentWidth() - 8;
            ListView list = new ListView(
                    floorTabs.getContentX(), floorTabs.getContentY(),
                    w, floorTabs.getMaxContentHeight());
            tab.addWidget(list);
        }

        int startIndex = FLOORS.indexOf(selectedFloor);
        if (startIndex < 0)
            startIndex = 0;
        floorTabs.selectTab(startIndex);

        widgets.add(floorTabs);
        refreshLeaderboard();
    }

    private void refreshLeaderboard() {
        if (floorTabs == null)
            return;

        TabPanel.Tab currentTab = floorTabs.getTab(floorTabs.getSelectedTabIndex());
        if (currentTab == null || currentTab.widgets.isEmpty())
            return;

        ListView list = (ListView) currentTab.widgets.get(0);
        list.clearItems();
        list.addItem(new Label(0, 0, ChatFormatting.GRAY + "Loading...", Label.Style.BODY));

        BotIntegration.getSoloLeaderboard(selectedFloor).thenAccept(response -> {
            MinecraftInstance.mc.execute(() -> {
                if (MinecraftInstance.mc.screen != this)
                    return;
                list.clearItems();

                if (response == null || !response.has("runs")) {
                    list.addItem(new Label(0, 0,
                            ChatFormatting.RED + "Failed to load  —  is the bot URL configured?",
                            Label.Style.BODY));
                    return;
                }

                JsonArray runs = response.getAsJsonArray("runs");
                if (runs.isEmpty()) {
                    list.addItem(new Label(0, 0,
                            ChatFormatting.GRAY + "No runs on this floor yet.",
                            Label.Style.BODY));
                    return;
                }

                for (JsonElement el : runs) {
                    JsonObject run = el.getAsJsonObject();
                    list.addItem(new RunWidget(0, 0, list.getWidth(), run));
                }
            });
        });
    }

    @Override
    protected void extractScrolledContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    private static class RunWidget extends Widget {
        private final int rank;
        private final String ign;
        private final String timeStr;
        private final int secrets;
        private final boolean prince;
        private final boolean mimic;
        private final JsonArray puzzles;

        public RunWidget(int x, int y, int width, JsonObject data) {
            super(x, y, width, 44);
            this.rank = JsonUtils.getInt(data, "rank");
            this.ign = JsonUtils.getString(data, "ign", "Unknown");
            String rawTime = JsonUtils.getString(data, "time_str", "--:--");
            int dotIndex = rawTime.indexOf('.');
            this.timeStr = dotIndex != -1 ? rawTime.substring(0, dotIndex) : rawTime;
            this.secrets = JsonUtils.getInt(data, "secrets");
            this.prince = data.has("prince") && data.get("prince").getAsBoolean();
            this.mimic = data.has("mimic") && data.get("mimic").getAsBoolean();
            this.puzzles = data.has("puzzles") ? data.getAsJsonArray("puzzles") : new JsonArray();
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
            boolean hov = mx >= x && mx <= x + width && my >= y && my <= y + height;
            RenderHelper.renderSurface(g, x, y, width, height, Theme.BORDER_RADIUS_SMALL, hov);

            int rankColor = rank == 1 ? GOLD : rank == 2 ? SILVER : rank == 3 ? BRONZE : Theme.TEXT_SECONDARY;
            String rankStr = rank <= 3 ? (rank == 1 ? "🥇" : rank == 2 ? "🥈" : "🥉") : ("#" + rank);
            g.text(MinecraftInstance.mc.font, rankStr, x + 8, y + 7, rankColor);

            g.text(MinecraftInstance.mc.font,
                    ChatFormatting.AQUA + "" + ChatFormatting.BOLD + ign,
                    x + 38, y + 7, Theme.TEXT_PRIMARY);

            String timeLabel = "⏱ " + timeStr;
            int timeLabelW = MinecraftInstance.mc.font.width(timeLabel);
            g.text(MinecraftInstance.mc.font,
                    ChatFormatting.YELLOW + timeStr,
                    x + width - timeLabelW - 8, y + 7,
                    Theme.TEXT_PRIMARY);

            java.util.List<String> puzzleNames = new java.util.ArrayList<>();
            for (JsonElement e : puzzles) {
                puzzleNames.add(e.getAsString());
            }
            String puzzlesStr = puzzleNames.isEmpty() ? "None" : String.join(", ", puzzleNames);

            String stats = ChatFormatting.GRAY + "Secrets: " + ChatFormatting.WHITE + secrets
                    + ChatFormatting.GRAY + "  Puzzles: " + ChatFormatting.LIGHT_PURPLE + "[" + puzzlesStr + "]"
                    + ChatFormatting.GRAY + "  Prince: "
                    + (prince ? ChatFormatting.GREEN + "✔" : ChatFormatting.RED + "✘")
                    + ChatFormatting.GRAY + "  Mimic: "
                    + (mimic ? ChatFormatting.GREEN + "✔" : ChatFormatting.RED + "✘");
            g.text(MinecraftInstance.mc.font, stats, x + 38, y + 26, Theme.TEXT_SECONDARY);
        }
    }
}
