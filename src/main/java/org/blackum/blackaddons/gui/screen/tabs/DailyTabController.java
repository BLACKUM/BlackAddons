package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.core.manager.ProfileStateManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.integration.BotIntegration;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.model.DungeonFloor;
import org.blackum.blackaddons.core.util.FormatUtils;
import org.blackum.blackaddons.core.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class DailyTabController extends ProfileTabController {

    private String dailyMode = "leaderboard";
    private String dailyPeriod = "daily";
    private String dailyMetric = "xp";
    private int dailyPage = 1;
    private int dailyTotalPages = 1;
    private DungeonFloor dailyFloorValue = DungeonFloor.M7;

    private ListView dailyLeaderboardList;
    private ListView dailyPersonalList;
    private Button[] dailyModeButtons = new Button[4];
    private Dropdown dailySearchTypeDropdown;
    private TextField dailySearchField;
    private Button dailyShowMeBtn;
    private Dropdown dailyFloorDropdown;
    private PaginationWidget paginationWidget;

    private boolean dailyDataLoaded = false;

    public DailyTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int w = tab.getParent().getContentWidth();
        int cx = tab.getParent().getContentX();
        int cy = tab.getParent().getContentY();

        int btnW = (w - 34) / 4;
        int btnH = 20;
        int gap = 5;

        dailyModeButtons[0] = new Button(cx, cy, btnW, btnH, "Daily", () -> setDailyMode("leaderboard", "daily"));
        dailyModeButtons[1] = new Button(cx + btnW + gap, cy, btnW, btnH, "Monthly",
                () -> setDailyMode("leaderboard", "monthly"));
        dailyModeButtons[2] = new Button(cx + (btnW + gap) * 2, cy, btnW, btnH, "Personal",
                () -> setDailyMode("personal", "daily"));
        dailyModeButtons[3] = new Button(cx + (btnW + gap) * 3, cy, btnW, btnH, "Runs", this::toggleDailyMetric);

        int searchY = cy + 25;

        dailySearchTypeDropdown = new Dropdown(cx, searchY, 60, 20, "Search By", List.of("IGN", "Page"),
                (val) -> {
                    if (val.equals("Page")) {
                        dailySearchField.setPlaceholder("Page #");
                        dailySearchField.setCharFilter(Character::isDigit);
                        String txt = dailySearchField.getText();
                        if (!txt.matches("\\d*")) {
                            dailySearchField.setText(txt.replaceAll("\\D", ""));
                        }
                    } else {
                        dailySearchField.setPlaceholder("IGN...");
                        dailySearchField.setCharFilter(c -> true);
                    }
                });
        dailySearchTypeDropdown.setSelectedIndex(0);

        int showMeW = 70;
        int showMeX = cx + w - showMeW - 20;
        dailyShowMeBtn = new Button(showMeX, searchY, showMeW, 20, "Show Me", () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                if (!dailySearchTypeDropdown.isExpanded()) {
                    performSearch(mc.player.getName().getString(), "IGN", false);
                }
            }
        });

        int searchFieldX = cx + 65;
        int searchFieldW = showMeX - searchFieldX - 10;

        dailySearchField = new TextField(searchFieldX, searchY, searchFieldW, 20, "IGN...");
        dailySearchField.setDebounceDelay(500);
        dailySearchField.setOnValueChange(query -> {
            if (!query.isEmpty()) {
                String type = (dailySearchTypeDropdown != null && dailySearchTypeDropdown.getSelectedIndex() == 1)
                        ? "Page"
                        : "IGN";
                performSearch(query, type, false);
            } else {
                fetchDailyData();
            }
        });

        dailyFloorDropdown = new Dropdown(cx, cy + 50, w - 20, 20, "Floor: M7", DungeonFloor.getDisplayNames(),
                (val) -> {
                    dailyFloorValue = DungeonFloor.fromDisplayName(val);
                    if (dailyMetric.startsWith("runs")) {
                        this.dailyMetric = "runs_" + dailyFloorValue.getKey();
                        fetchDailyData();
                    }
                });
        dailyFloorDropdown.setVisible(false);
        dailyFloorDropdown.setSelectedOption("M7");
        dailyFloorDropdown.setSelectedOption("M7");

        int listY = cy + 75;
        int listH = tab.getParent().getMaxContentHeight() - 75;

        dailyLeaderboardList = new ListView(cx, listY, w - 20, listH - 105);
        dailyPersonalList = new ListView(cx, listY, w - 20, listH);
        dailyPersonalList.setVisible(false);

        paginationWidget = new PaginationWidget(cx, cy + listH - 25, w - 20, dailyPage, dailyTotalPages, page -> {
            this.dailyPage = page;
            fetchDailyData();
        });

        tab.addWidget(dailyLeaderboardList);
        tab.addWidget(dailyPersonalList);

        for (Button b : dailyModeButtons)
            tab.addWidget(b);

        tab.addWidget(dailySearchField);
        tab.addWidget(dailyShowMeBtn);
        tab.addWidget(paginationWidget);

        tab.addWidget(dailySearchTypeDropdown);
        tab.addWidget(dailyFloorDropdown);

        updateDailyButtons();
    }

    @Override
    public boolean isLoaded() {
        return dailyDataLoaded;
    }

    @Override
    public void onSelected() {
        if (!dailyDataLoaded) {
            fetchDailyData();
        }
    }

    public void tick() {
    }

    private void performSearch(String query, String type, boolean force) {
        if (query == null || query.trim().isEmpty())
            return;
        query = query.trim();

        if (type.equals("Page")) {
            if (query.matches("\\d+")) {
                int page = Integer.parseInt(query);
                this.dailyPage = page;
                if (this.dailyPage < 1)
                    this.dailyPage = 1;
                fetchDailyData();
            }
        } else {
            dailyLeaderboardList.clearItems();
            addInfoRow(dailyLeaderboardList, "Searching...", "");

            ProfileStateManager.getInstance().getLeaderboardWithPlayer(dailyPeriod, dailyMetric, query)
                    .thenAccept(result -> {
                        Minecraft.getInstance().execute(() -> {
                            if (result == null || result.hasError()) {
                                dailyLeaderboardList.clearItems();
                                if (result != null && result.hasError()) {
                                    addInfoRow(dailyLeaderboardList, result.getError(), "");
                                } else {
                                    addInfoRow(dailyLeaderboardList, "Not found.", "");
                                }
                                return;
                            }

                            JsonObject json = result.getData();
                            if (json.has("page")) {
                                this.dailyPage = json.get("page").getAsInt();
                            }
                            if (json.has("total_pages")) {
                                this.dailyTotalPages = json.get("total_pages").getAsInt();
                            }

                            dailyLeaderboardList.clearItems();
                            renderLeaderboard(json);
                        });
                    });
        }
    }

    private void setDailyMode(String mode, String period) {
        this.dailyMode = mode;
        if (period != null)
            this.dailyPeriod = period;
        this.dailyPage = 1;

        if (mode.equals("personal")) {
            dailyLeaderboardList.setVisible(false);
            dailyPersonalList.setVisible(true);
        } else {
            dailyLeaderboardList.setVisible(true);
            dailyPersonalList.setVisible(false);
        }
        updateDailyButtons();
        fetchDailyData();
    }

    private void toggleDailyMetric() {
        if (this.dailyMetric.equals("xp")) {
            this.dailyMetric = "runs_" + dailyFloorValue.getKey();
        } else {
            this.dailyMetric = "xp";
        }
        updateDailyButtons();
        fetchDailyData();
    }

    private void updateDailyButtons() {
        boolean isLb = dailyMode.equals("leaderboard");
        boolean isRuns = dailyMetric.startsWith("runs");

        dailyModeButtons[0].setEnabled(!isLb || !dailyPeriod.equals("daily"));
        dailyModeButtons[1].setEnabled(!isLb || !dailyPeriod.equals("monthly"));
        dailyModeButtons[2].setEnabled(!dailyMode.equals("personal"));

        dailyModeButtons[3].setText(isRuns ? "Show XP" : "Runs");

        if (dailyFloorDropdown != null) {
            dailyFloorDropdown.setVisible(isRuns && isLb);
            if (isRuns) {
                dailyFloorDropdown.setSelectedOption(dailyFloorValue.getDisplayName());
            }
        }

        if (dailySearchField != null)
            dailySearchField.setVisible(isLb);
        if (dailyShowMeBtn != null)
            dailyShowMeBtn.setVisible(isLb);
        if (dailySearchTypeDropdown != null)
            dailySearchTypeDropdown.setVisible(isLb);
        if (paginationWidget != null)
            paginationWidget.setVisible(isLb);
    }

    private void fetchDailyData() {
        if (dailyMode.equals("leaderboard")) {
            dailyLeaderboardList.clearItems();
            addInfoRow(dailyLeaderboardList, "Loading...", "");

            ProfileStateManager.getInstance().getLeaderboard(dailyPeriod, dailyMetric, dailyPage).thenAccept(result -> {
                Minecraft.getInstance().execute(() -> {
                    dailyLeaderboardList.clearItems();
                    if (result == null || result.hasError()) {
                        addInfoRow(dailyLeaderboardList, "Error fetching data.", "");
                        return;
                    }

                    JsonObject json = result.getData();
                    if (json.has("total_pages")) {
                        dailyTotalPages = json.get("total_pages").getAsInt();
                    } else {
                        dailyTotalPages = 1;
                    }
                    if (paginationWidget != null) {
                        paginationWidget.update(dailyPage, dailyTotalPages);
                    }

                    renderLeaderboard(json);
                });
            });
        } else {
            renderPersonalStats();
        }
    }

    private void renderLeaderboard(JsonObject json) {
        if (!json.has("data") || json.get("data").isJsonNull()) {
            addInfoRow(dailyLeaderboardList, "No data found.", "");
            return;
        }

        JsonArray data = json.getAsJsonArray("data");
        if (data.size() == 0) {
            addInfoRow(dailyLeaderboardList, "No entries yet.", "");
            return;
        }

        int rank = (dailyPage - 1) * 10 + 1;
        String viewPlayer = screen.getPlayer();
        for (JsonElement e : data) {
            JsonObject entry = e.getAsJsonObject();
            String ign = entry.get("ign").getAsString();
            double val = entry.get("gained").getAsDouble();

            LeaderboardRow row = new LeaderboardRow(dailyLeaderboardList.getWidth(), rank++, ign, val,
                    dailyMetric.startsWith("runs"));
            if (ign.equalsIgnoreCase(viewPlayer)) {
                row.setCurrentPlayer(true);
            }
            dailyLeaderboardList.addItem(row);
        }

        if (json.has("last_updated") && !json.get("last_updated").isJsonNull()) {
            long lastUpdatedTs = json.get("last_updated").getAsLong();
            long now = System.currentTimeMillis() / 1000;
            long elapsed = now - lastUpdatedTs;

            String lastUpdatedStr = FormatUtils.formatTime(elapsed) + " ago";
            long nextUpdate = 86400 - elapsed;
            String nextUpdateStr = nextUpdate > 0 ? " (Next update in " + FormatUtils.formatTime(nextUpdate) + ")"
                    : " (Updating soon...)";
            addInfoRow(dailyLeaderboardList, "Last Updated: " + lastUpdatedStr + nextUpdateStr, "");
        }

        addDiscordLinkButton(dailyLeaderboardList);
        dailyDataLoaded = true;
    }

    private void addDiscordLinkButton(ListView list) {
        Button linkBtn = new Button(0, 0, list.getWidth() - 20, 20,
                ChatFormatting.AQUA + "Want to be on leaderboard? Link Discord", () -> {
                    String url = Constants.DISCORD_AUTH_URL;
                    Util.getPlatform().openUri(url);
                });

        Widget wrapper = new Widget(0, 0, list.getWidth(), 30) {
            @Override
            public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                linkBtn.setX(this.x + 10);
                linkBtn.setY(this.y + 5);
                linkBtn.setWidth(this.width - 20);
                linkBtn.render(g, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return linkBtn.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public void updateHoverState(int mouseX, int mouseY) {
                linkBtn.updateHoverState(mouseX, mouseY);
            }

            @Override
            public void tick() {
                linkBtn.tick();
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                return linkBtn.mouseReleased(mouseX, mouseY, button);
            }
        };
        list.addItem(wrapper);
    }

    private void renderPersonalStats() {
        dailyPersonalList.clearItems();

        if (profileData == null) {
            addInfoRow(dailyPersonalList, "No profile data loaded.", "");
            return;
        }

        JsonObject daily = JsonUtils.getObject(profileData, "daily_stats");
        JsonObject monthly = JsonUtils.getObject(profileData, "monthly_stats");

        if (daily.size() == 0 && monthly.size() == 0) {
            addInfoRow(dailyPersonalList, "No personal data available.", "Link Discord with /link to track.");
            return;
        }

        final String playerName = screen.getPlayer();

        Widget header = new Widget(0, 0, dailyPersonalList.getWidth(), 30) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.drawCenteredString(Minecraft.getInstance().font,
                        ChatFormatting.BOLD.toString() + "📊 Personal Stats: " + playerName,
                        x + width / 2, y + 10,
                        Theme.ACCENT);
            }
        };
        dailyPersonalList.addItem(header);

        renderStatGroup(dailyPersonalList, "Catacombs", daily, monthly, null);

        addSectionHeader(dailyPersonalList, "Class Progress");
        String[] classes = { "archer", "berserk", "healer", "mage", "tank" };
        for (String cls : classes) {
            renderStatGroup(dailyPersonalList, cls.substring(0, 1).toUpperCase() + cls.substring(1), daily, monthly,
                    cls);
        }

        addSectionHeader(dailyPersonalList, "Runs Gained");
        renderRunsGroup(dailyPersonalList, daily, monthly);

        dailyPersonalList.addItem(new Widget(0, 0, 0, 40) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            }
        });
    }

    private void renderStatGroup(ListView list, String title, JsonObject dailyRoot, JsonObject monthlyRoot,
            String classKey) {
        double dGained = 0, dStart = 0, dEnd = 0;
        double mGained = 0, mStart = 0, mEnd = 0;
        boolean hasDaily = false, hasMonthly = false;

        if (classKey == null) {
            if (dailyRoot != null && dailyRoot.has("cata_gained")) {
                dGained = getDouble(dailyRoot, "cata_gained");
                dStart = getDouble(dailyRoot, "cata_start_lvl");
                dEnd = getDouble(dailyRoot, "cata_current_lvl");
                hasDaily = true;
            }
        } else {
            if (dailyRoot != null && dailyRoot.has("classes") && dailyRoot.getAsJsonObject("classes").has(classKey)) {
                JsonObject cls = dailyRoot.getAsJsonObject("classes").getAsJsonObject(classKey);
                dGained = getDouble(cls, "gained");
                dStart = getDouble(cls, "start_lvl");
                dEnd = getDouble(cls, "current_lvl");
                hasDaily = true;
            }
        }

        if (classKey == null) {
            if (monthlyRoot != null && monthlyRoot.has("cata_gained")) {
                mGained = getDouble(monthlyRoot, "cata_gained");
                mStart = getDouble(monthlyRoot, "cata_start_lvl");
                mEnd = getDouble(monthlyRoot, "cata_current_lvl");
                hasMonthly = true;
            }
        } else {
            if (monthlyRoot != null && monthlyRoot.has("classes")
                    && monthlyRoot.getAsJsonObject("classes").has(classKey)) {
                JsonObject cls = monthlyRoot.getAsJsonObject("classes").getAsJsonObject(classKey);
                mGained = getDouble(cls, "gained");
                mStart = getDouble(cls, "start_lvl");
                mEnd = getDouble(cls, "current_lvl");
                hasMonthly = true;
            }
        }

        if (dGained <= 0 && mGained <= 0)
            return;

        final double fdGained = dGained, fdStart = dStart, fdEnd = dEnd;
        final double fmGained = mGained, fmStart = mStart, fmEnd = mEnd;
        final boolean fHasDaily = hasDaily && dGained > 0;
        final boolean fHasMonthly = hasMonthly && mGained > 0;

        Widget w = new Widget(0, 0, list.getWidth(), 55) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                RenderHelper.renderRoundedRect(graphics, x, y, width - 4, height - 2,
                        3, Theme.BACKGROUND_SECONDARY);
                Minecraft mc = Minecraft.getInstance();
                graphics.drawString(mc.font, ChatFormatting.AQUA + title, x + 5, y + 5, Theme.TEXT_PRIMARY);

                int rowY = y + 18;
                if (fHasDaily) {
                    String dStr = "Day: " + ChatFormatting.GREEN + "+" + FormatUtils.formatNumber(fdGained) + " XP "
                            + ChatFormatting.GRAY + "("
                            + String.format("%.2f", fdStart) + " ➤ " + String.format("%.2f", fdEnd) + ")";
                    graphics.drawString(mc.font, dStr, x + 10, rowY, Theme.TEXT_PRIMARY);
                    rowY += 12;
                } else {
                    graphics.drawString(mc.font, "Day: " + ChatFormatting.GRAY + "No Gain", x + 10, rowY,
                            Theme.TEXT_SECONDARY);
                    rowY += 12;
                }

                if (fHasMonthly) {
                    String mStr = "Month: " + ChatFormatting.GREEN + "+" + FormatUtils.formatNumber(fmGained) + " XP "
                            + ChatFormatting.GRAY + "("
                            + String.format("%.2f", fmStart) + " ➤ " + String.format("%.2f", fmEnd) + ")";
                    graphics.drawString(mc.font, mStr, x + 10, rowY, Theme.TEXT_PRIMARY);
                } else {
                    graphics.drawString(mc.font, "Month: " + ChatFormatting.GRAY + "No Gain", x + 10, rowY,
                            Theme.TEXT_SECONDARY);
                }
            }
        };
        list.addItem(w);
    }

    private void renderRunsGroup(ListView list, JsonObject daily, JsonObject monthly) {
        StringBuilder dailyRuns = new StringBuilder();
        StringBuilder monthlyRuns = new StringBuilder();

        if (daily != null && daily.has("runs")) {
            appendRuns(dailyRuns, daily.getAsJsonObject("runs"));
        }
        if (monthly != null && monthly.has("runs")) {
            appendRuns(monthlyRuns, monthly.getAsJsonObject("runs"));
        }

        if (dailyRuns.length() == 0 && monthlyRuns.length() == 0) {
            addInfoRow(list, "No runs recorded recently.", "");
            return;
        }

        final String dText = dailyRuns.length() > 0 ? dailyRuns.toString() : "None";
        final String mText = monthlyRuns.length() > 0 ? monthlyRuns.toString() : "None";

        Widget w = new Widget(0, 0, list.getWidth(), 45) {
            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                RenderHelper.renderRoundedRect(graphics, x, y, width - 4, height - 2,
                        3, Theme.BACKGROUND_SECONDARY);
                Minecraft mc = Minecraft.getInstance();
                graphics.drawString(mc.font, "Daily: " + dText, x + 5, y + 8, Theme.TEXT_PRIMARY);
                graphics.drawString(mc.font, "Monthly: " + mText, x + 5, y + 25, Theme.TEXT_PRIMARY);
            }
        };
        list.addItem(w);
    }

    private void appendRuns(StringBuilder sb, JsonObject runsObj) {
        List<String> parts = new ArrayList<>();

        int totalSum = 0;
        if (runsObj.has("total")) {
            totalSum += runsObj.get("total").getAsInt();
        } else {
            if (runsObj.has("master") && runsObj.getAsJsonObject("master").has("total")) {
                totalSum += runsObj.getAsJsonObject("master").get("total").getAsInt();
            }
            if (runsObj.has("normal") && runsObj.getAsJsonObject("normal").has("total")) {
                totalSum += runsObj.getAsJsonObject("normal").get("total").getAsInt();
            }
        }

        if (totalSum > 0) {
            parts.add("Total (+" + totalSum + ")");
        }

        if (runsObj.has("master")) {
            JsonObject m = runsObj.getAsJsonObject("master");
            for (int i = 7; i >= 1; i--) {
                String key = String.valueOf(i);
                if (m.has(key)) {
                    int val = m.get(key).getAsInt();
                    if (val > 0) {
                        parts.add(ChatFormatting.GOLD + "M" + key + " (+" + val + ")" + ChatFormatting.RESET);
                    }
                }
            }
        }

        if (runsObj.has("normal")) {
            JsonObject n = runsObj.getAsJsonObject("normal");
            for (int i = 7; i >= 1; i--) {
                String key = String.valueOf(i);
                if (n.has(key)) {
                    int val = n.get(key).getAsInt();
                    if (val > 0) {
                        parts.add(ChatFormatting.WHITE + "F" + key + " (+" + val + ")" + ChatFormatting.RESET);
                    }
                }
            }
        }

        if (runsObj.has("normal")) {
            JsonObject n = runsObj.getAsJsonObject("normal");
            if (n.has("0")) {
                int val = n.get("0").getAsInt();
                if (val > 0) {
                    parts.add(ChatFormatting.GRAY + "Entrance (+" + val + ")" + ChatFormatting.RESET);
                }
            }
        }

        for (int i = 0; i < parts.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(parts.get(i));
        }
    }

}
