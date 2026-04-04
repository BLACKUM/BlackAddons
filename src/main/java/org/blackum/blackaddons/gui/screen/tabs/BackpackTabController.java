package org.blackum.blackaddons.gui.screen.tabs;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.core.util.ItemDeserializer;
import org.blackum.blackaddons.core.model.SkyblockItem;

import java.util.ArrayList;
import java.util.List;

public class BackpackTabController extends ProfileTabController {
    private ListView listView;
    private final List<Integer> backpackOffsets = new ArrayList<>();

    public BackpackTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int startX = tab.getParent().getContentX();
        int startY = tab.getParent().getContentY();
        int width = tab.getParent().getContentWidth();
        int height = tab.getParent().getContentHeight();

        JsonObject inventory = profileData.getAsJsonObject("inventory");
        if (inventory == null || !inventory.has("backpack_contents")) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "No Backpack data", Label.Style.TITLE));
            return;
        }

        JsonObject backpacks = inventory.getAsJsonObject("backpack_contents");
        if (backpacks.keySet().isEmpty()) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "No backpacks found", Label.Style.TITLE));
            return;
        }

        int btnWidth = 30;
        int btnHeight = 20;
        int btnGap = 5;
        int numBackpacks = backpacks.keySet().size();
        int totalBtnWidth = numBackpacks * btnWidth + (numBackpacks - 1) * btnGap;
        int btnX = startX + (width - totalBtnWidth) / 2;

        for (int i = 0; i < numBackpacks; i++) {
            final int bpIdx = i;
            Button btn = new Button(btnX, startY, btnWidth, btnHeight, String.valueOf(i + 1), () -> {
                if (listView != null && bpIdx < backpackOffsets.size()) {
                    listView.scrollTo(backpackOffsets.get(bpIdx));
                }
            });
            tab.addWidget(btn);
            btnX += btnWidth + btnGap;
        }

        listView = new ListView(startX, startY + btnHeight + 10, width, height - btnHeight - 10);
        tab.addWidget(listView);

        backpackOffsets.clear();
        int currentOffset = 0;

        int idx = 0;
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : backpacks.entrySet()) {
            JsonObject bp = entry.getValue().getAsJsonObject();
            if (!bp.has("data"))
                continue;

            List<SkyblockItem> items = ItemDeserializer.deserializeList(bp.get("data").getAsString());
            if (items.isEmpty())
                continue;

            backpackOffsets.add(currentOffset);

            Label bpLabel = new Label(0, 0, "Backpack " + (idx + 1), Label.Style.TITLE);
            listView.addItem(bpLabel);
            currentOffset += bpLabel.getHeight() + listView.getItemSpacing();

            ItemGridWidget grid = new ItemGridWidget(0, 0, 9, items);
            listView.addItem(grid);
            currentOffset += grid.getHeight() + listView.getItemSpacing();

            listView.addItem(new Widget(0, 0, 0, 20) {
                @Override
                public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                        float partialTick) {
                }
            });
            currentOffset += 20 + listView.getItemSpacing();
            idx++;
        }
    }
}
