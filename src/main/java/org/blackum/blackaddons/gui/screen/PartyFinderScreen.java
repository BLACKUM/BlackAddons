package org.blackum.blackaddons.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.core.manager.PartyFinderManager;
import org.blackum.blackaddons.integration.BotIntegration;
import org.blackum.blackaddons.core.util.JsonUtils;
import org.blackum.blackaddons.core.util.MinecraftInstance;
import org.blackum.blackaddons.Blackaddons;

import java.util.List;

public class PartyFinderScreen extends BaseScreen {

    private TabPanel floorTabs;
    private String selectedFloor = "All";
    private long lastRefreshTime = 0;
    private static final long REFRESH_INTERVAL_MS = 15000;

    public PartyFinderScreen() {
        this(null);
    }

    public PartyFinderScreen(Screen parent) {
        super(Component.literal("Dungeon Party Finder"), parent);
    }

    @Override
    public void initWidgets() {
        int contentX = containerX + 20;
        int contentY = containerY + 20;
        int contentWidth = containerWidth - 40;
        int contentHeight = containerHeight - 40;

        widgets.add(new Label(contentX, contentY, "Dungeon Party Finder", Label.Style.TITLE));

        int btnY = contentY + 25;
        int btnHeight = 25;
        Button refreshBtn = new Button(contentX, btnY, 80, btnHeight, "Refresh", this::refreshParties);
        widgets.add(refreshBtn);

        Button createBtn = new Button(contentX + 90, btnY, 100, btnHeight, "Create Party", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new PartyCreationScreen(this));
            }
        });

        widgets.add(createBtn);

        Button unqueueBtn = new Button(contentX + 200, btnY, 80, btnHeight, "Unqueue", () -> {
            PartyFinderManager.getInstance().unqueue();
        });
        widgets.add(unqueueBtn);

        int tabsY = btnY + 35;
        floorTabs = new TabPanel(contentX, tabsY, contentWidth, contentHeight - (tabsY - contentY));
        floorTabs.setOnTabChange(index -> {
            if (index == 0)
                selectedFloor = "All";
            else {
                List<String> floors = List.of("M7", "M6", "M5", "M4", "M3", "M2", "M1", "F7", "F6", "F5", "F4", "F3",
                        "F2", "F1", "Entrance");
                selectedFloor = floors.get(index - 1);
            }
            refreshParties();
        });

        addFloorTab("All Floors");
        for (String f : List.of("M7", "M6", "M5", "M4", "M3", "M2", "M1", "F7", "F6", "F5", "F4", "F3", "F2", "F1",
                "Entrance")) {
            addFloorTab(f);
        }

        widgets.add(floorTabs);
        refreshParties();
    }

    private void addFloorTab(String name) {
        TabPanel.Tab tab = floorTabs.addTab(name);
        int w = floorTabs.getContentWidth() - 10;
        ListView list = new ListView(floorTabs.getContentX(), floorTabs.getContentY(), w,
                floorTabs.getMaxContentHeight());
        tab.addWidget(list);
    }

    private void refreshParties() {
        if (floorTabs == null)
            return;

        TabPanel.Tab currentTab = floorTabs.getTab(floorTabs.getSelectedTabIndex());
        if (currentTab.widgets.isEmpty())
            return;

        ListView list = (ListView) currentTab.widgets.get(0);
        lastRefreshTime = System.currentTimeMillis();

        BotIntegration.getParties(selectedFloor).thenAccept(response -> {
            if (response != null && response.has("parties")) {
                JsonArray parties = response.getAsJsonArray("parties");
                MinecraftInstance.mc.execute(() -> {
                    if (MinecraftInstance.mc.screen == this) {
                        list.clearItems();
                        for (JsonElement el : parties) {
                            JsonObject p = el.getAsJsonObject();
                            list.addItem(new PartyWidget(0, 0, list.getWidth(), p));
                        }
                    }
                });
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (System.currentTimeMillis() - lastRefreshTime > REFRESH_INTERVAL_MS) {
            refreshParties();
        }
    }

    @Override
    protected void extractScrolledContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    private static class PartyWidget extends Widget {
        private final String id;
        private final String leader;
        private final String note;
        private final int memberCount;
        private final int maxSize;

        public PartyWidget(int x, int y, int width, JsonObject data) {
            super(x, y, width, 50);
            this.id = JsonUtils.getString(data, "id", "");
            this.leader = JsonUtils.getString(data, "leader_name", "Unknown");
            this.note = JsonUtils.getString(data, "note", "");
            this.memberCount = JsonUtils.getInt(data, "member_count");
            this.maxSize = JsonUtils.getInt(data, "max_size");
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
            RenderHelper.renderSurface(g, x, y, width, height, Theme.BORDER_RADIUS_SMALL, false);

            g.text(MinecraftInstance.mc.font, leader, x + 10, y + 10, Theme.ACCENT);
            g.text(MinecraftInstance.mc.font, ChatFormatting.GRAY + note, x + 10, y + 25,
                    Theme.TEXT_SECONDARY);

            String countText = memberCount + "/" + maxSize;
            int countWidth = MinecraftInstance.mc.font.width(countText);
            g.text(MinecraftInstance.mc.font, countText, x + width - countWidth - 80, y + 20,
                    Theme.TEXT_PRIMARY);

            boolean hovered = mx >= x + width - 70 && mx <= x + width - 10 && my >= y + 15 && my <= y + 35;
            RenderHelper.renderRoundedRect(g, x + width - 70, y + 15, 60, 20, Theme.BORDER_RADIUS_SMALL,
                    hovered ? Theme.withAlpha(Theme.ACCENT, 0.4f) : Theme.withAlpha(Theme.ACCENT, 0.2f));

            int joinWidth = MinecraftInstance.mc.font.width("Join");
            g.text(MinecraftInstance.mc.font, "Join", x + width - 70 + (60 - joinWidth) / 2, y + 21,
                    Theme.TEXT_PRIMARY);
        }

        @Override
        public boolean mouseClicked(double mx, double my, MouseButtonEvent event) {
            int b = event.button();
            if (mx >= x + width - 70 && mx <= x + width - 10 && my >= y + 15 && my <= y + 35) {
                PartyFinderManager.getInstance().sendJoinRequest(leader, id);
                return true;
            }
            return false;
        }
    }
}
