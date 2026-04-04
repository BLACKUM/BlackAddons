package org.blackum.blackaddons.gui.screen.tabs;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.core.util.ItemDeserializer;
import org.blackum.blackaddons.core.model.SkyblockItem;

import java.util.ArrayList;
import java.util.List;

public class EnderChestTabController extends ProfileTabController {
    private ListView listView;
    private final List<Integer> pageOffsets = new ArrayList<>();

    public EnderChestTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int startX = tab.getParent().getContentX();
        int startY = tab.getParent().getContentY();
        int width = tab.getParent().getContentWidth();
        int height = tab.getParent().getContentHeight();

        JsonObject inventory = profileData.getAsJsonObject("inventory");
        if (inventory == null || !inventory.has("ender_chest_contents")) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "No Ender Chest data", Label.Style.TITLE));
            return;
        }

        List<SkyblockItem> allItems = ItemDeserializer.deserializeList(
                inventory.getAsJsonObject("ender_chest_contents").get("data").getAsString());

        if (allItems.isEmpty()) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "Ender Chest is empty", Label.Style.TITLE));
            return;
        }

        int btnWidth = 30;
        int btnHeight = 20;
        int btnGap = 5;
        int maxPages = (int) Math.ceil(allItems.size() / 54.0);
        int totalBtnWidth = maxPages * btnWidth + (maxPages - 1) * btnGap;
        int btnX = startX + (width - totalBtnWidth) / 2;

        for (int i = 0; i < maxPages; i++) {
            final int pageIdx = i;
            Button btn = new Button(btnX, startY, btnWidth, btnHeight, String.valueOf(i + 1), () -> {
                if (listView != null && pageIdx < pageOffsets.size()) {
                    listView.scrollTo(pageOffsets.get(pageIdx));
                }
            });
            tab.addWidget(btn);
            btnX += btnWidth + btnGap;
        }

        listView = new ListView(startX, startY + btnHeight + 10, width, height - btnHeight - 10);
        tab.addWidget(listView);

        pageOffsets.clear();
        int currentOffset = 0;

        for (int i = 0; i < maxPages; i++) {
            int start = i * 54;
            int end = Math.min(start + 54, allItems.size());
            List<SkyblockItem> pageItems = new ArrayList<>(allItems.subList(start, end));
            while (pageItems.size() < 54) {
                pageItems.add(new SkyblockItem(net.minecraft.world.item.ItemStack.EMPTY, "EMPTY", "COMMON"));
            }

            pageOffsets.add(currentOffset);

            Label pageLabel = new Label(0, 0, "Page " + (i + 1), Label.Style.TITLE);
            listView.addItem(pageLabel);
            currentOffset += pageLabel.getHeight() + listView.getItemSpacing();

            ItemGridWidget grid = new ItemGridWidget(0, 0, 9, pageItems);
            listView.addItem(grid);
            currentOffset += grid.getHeight() + listView.getItemSpacing();

            listView.addItem(new Widget(0, 0, 0, 20) {
                @Override
                public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                        float partialTick) {
                }
            });
            currentOffset += 20 + listView.getItemSpacing();
        }
    }
}
