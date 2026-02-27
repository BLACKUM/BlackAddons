package org.blackum.blackaddons.gui.screen.tabs;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.widget.Button;
import org.blackum.blackaddons.gui.widget.ItemGridWidget;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.core.util.ItemDeserializer;
import org.blackum.blackaddons.core.model.SkyblockItem;
import java.util.ArrayList;
import java.util.List;

public class InventoryTabController extends ProfileTabController {
    private PetDetailWidget petDetailWidget;
    private SkyblockItem selectedPet;

    private enum SubTab {
        INVENTORY("Inventory"),
        PETS("Pets"),
        WARDROBE("Wardrobe"),
        ENDER_CHEST("Ender Chest"),
        BACKPACKS("Backpacks"),
        VAULT("Personal Vault");

        final String label;

        SubTab(String label) {
            this.label = label;
        }
    }

    private SubTab currentSubTab = SubTab.INVENTORY;
    private final List<Button> subTabButtons = new ArrayList<>();
    private final List<Widget> currentWidgets = new ArrayList<>();
    private final List<Integer> pagedOffsets = new ArrayList<>();
    private TabPanel.Tab tab;
    private ListView pagedListView;

    public InventoryTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        this.tab = tab;
        int startX = tab.getParent().getContentX();
        int startY = tab.getParent().getContentY();
        int width = tab.getParent().getContentWidth();

        subTabButtons.clear();
        int numTabs = SubTab.values().length;
        int btnWidth = (width - 40) / numTabs;
        int btnX = startX + 10;

        for (SubTab st : SubTab.values()) {
            Button btn = new Button(btnX, startY, btnWidth, 20, st.label, () -> switchSubTab(st));
            subTabButtons.add(btn);
            tab.addWidget(btn);
            btnX += btnWidth + 5;
        }

        updateContent();
    }

    private void switchSubTab(SubTab newTab) {
        this.currentSubTab = newTab;
        updateContent();
    }

    private void updateContent() {
        if (tab == null)
            return;

        for (Widget w : currentWidgets) {
            tab.widgets.remove(w);
        }
        currentWidgets.clear();
        pagedOffsets.clear();

        int startX = tab.getParent().getContentX();
        int startY = tab.getParent().getContentY() + 40;
        int contentWidth = tab.getParent().getContentWidth();
        int contentHeight = tab.getParent().getContentHeight();

        List<SkyblockItem> equipmentItems = new ArrayList<>();
        List<SkyblockItem> armorItems = new ArrayList<>();
        List<SkyblockItem> inventoryItems = new ArrayList<>();
        List<SkyblockItem> petItems = new ArrayList<>();

        JsonObject inventory = profileData.getAsJsonObject("inventory");

        if (inventory != null) {
            if (currentSubTab == SubTab.INVENTORY) {
                if (inventory.has("equipment_contents")) {
                    equipmentItems.addAll(ItemDeserializer.deserializeList(
                            inventory.getAsJsonObject("equipment_contents").get("data").getAsString()));
                }
                while (equipmentItems.size() < 4)
                    equipmentItems
                            .add(new SkyblockItem(net.minecraft.world.item.ItemStack.EMPTY, "EMPTY_EQUIP", "COMMON"));

                if (inventory.has("inv_armor")) {
                    List<SkyblockItem> armor = ItemDeserializer
                            .deserializeList(inventory.getAsJsonObject("inv_armor").get("data").getAsString());
                    java.util.Collections.reverse(armor);
                    armorItems.addAll(armor);
                }
                while (armorItems.size() < 4)
                    armorItems.add(new SkyblockItem(net.minecraft.world.item.ItemStack.EMPTY, "EMPTY_ARMOR", "COMMON"));

                if (inventory.has("inv_contents")) {
                    List<SkyblockItem> inv = ItemDeserializer
                            .deserializeList(inventory.getAsJsonObject("inv_contents").get("data").getAsString());
                    if (inv.size() >= 36) {
                        List<SkyblockItem> hotbar = inv.subList(0, 9);
                        List<SkyblockItem> mainInv = inv.subList(9, 36);
                        inventoryItems.addAll(mainInv);
                        inventoryItems.addAll(hotbar);
                    } else {
                        inventoryItems.addAll(inv);
                    }
                }
                while (inventoryItems.size() < 36)
                    inventoryItems.add(new SkyblockItem(net.minecraft.world.item.ItemStack.EMPTY, "EMPTY", "COMMON"));

                int totalGridWidth = (1 * 36 + 12) + 20 + (1 * 36 + 12) + 20 + (9 * 36 + 12);
                int gridX = startX + (contentWidth - totalGridWidth) / 2;

                ItemGridWidget equipGrid = new ItemGridWidget(gridX, startY, 1, equipmentItems);
                tab.addWidget(equipGrid);
                currentWidgets.add(equipGrid);

                gridX += equipGrid.getWidth() + 20;
                ItemGridWidget armorGrid = new ItemGridWidget(gridX, startY, 1, armorItems);
                tab.addWidget(armorGrid);
                currentWidgets.add(armorGrid);

                gridX += armorGrid.getWidth() + 20;
                ItemGridWidget invGrid = new ItemGridWidget(gridX, startY, 9, inventoryItems);
                tab.addWidget(invGrid);
                currentWidgets.add(invGrid);

            } else if (currentSubTab == SubTab.ENDER_CHEST || currentSubTab == SubTab.BACKPACKS) {
                List<List<SkyblockItem>> sections = new ArrayList<>();
                List<String> labels = new ArrayList<>();

                if (currentSubTab == SubTab.ENDER_CHEST && inventory.has("ender_chest_contents")) {
                    List<SkyblockItem> ecItems = ItemDeserializer.deserializeList(
                            inventory.getAsJsonObject("ender_chest_contents").get("data").getAsString());
                    int numPages = (int) Math.ceil(ecItems.size() / 54.0);
                    for (int i = 0; i < numPages; i++) {
                        int start = i * 54;
                        int end = Math.min(start + 54, ecItems.size());
                        sections.add(new ArrayList<>(ecItems.subList(start, end)));
                        labels.add("Page " + (i + 1));
                    }
                } else if (currentSubTab == SubTab.BACKPACKS && inventory.has("backpack_contents")) {
                    JsonObject backpacks = inventory.getAsJsonObject("backpack_contents");
                    int bpIdx = 1;
                    for (String key : backpacks.keySet()) {
                        JsonObject bp = backpacks.getAsJsonObject(key);
                        if (bp.has("data")) {
                            List<SkyblockItem> bpItems = ItemDeserializer.deserializeList(bp.get("data").getAsString());
                            if (!bpItems.isEmpty()) {
                                sections.add(bpItems);
                                labels.add("Backpack " + bpIdx++);
                            }
                        }
                    }
                }

                if (!sections.isEmpty()) {
                    int btnWidth = 30;
                    int btnGap = 5;
                    int maxRowWidth = contentWidth - 20;
                    int buttonsPerRow = Math.max(1, maxRowWidth / (btnWidth + btnGap));
                    int numRows = (int) Math.ceil((double) labels.size() / buttonsPerRow);

                    for (int row = 0; row < numRows; row++) {
                        int startIdx = row * buttonsPerRow;
                        int endIdx = Math.min(startIdx + buttonsPerRow, labels.size());
                        int rowBtnCount = endIdx - startIdx;
                        int rowWidth = rowBtnCount * btnWidth + (rowBtnCount - 1) * btnGap;
                        int btnX = startX + (contentWidth - rowWidth) / 2;
                        int btnY = startY + (row * 25);

                        for (int i = startIdx; i < endIdx; i++) {
                            final int idx = i;
                            Button btn = new Button(btnX, btnY, btnWidth, 20, String.valueOf(i + 1), () -> {
                                if (pagedListView != null && idx < pagedOffsets.size()) {
                                    pagedListView.scrollTo(pagedOffsets.get(idx));
                                }
                            });
                            tab.addWidget(btn);
                            currentWidgets.add(btn);
                            btnX += btnWidth + btnGap;
                        }
                    }

                    int buttonsTotalHeight = numRows * 25;
                    int listTopOffset = buttonsTotalHeight + 10;
                    pagedListView = new ListView(startX, startY + listTopOffset, contentWidth, contentHeight - (40 + listTopOffset + 20));
                    tab.addWidget(pagedListView);
                    currentWidgets.add(pagedListView);

                    int currentOffset = 0;

                    for (int i = 0; i < sections.size(); i++) {
                        pagedOffsets.add(currentOffset);

                        Label pageLabel = new Label(0, 0, labels.get(i), Label.Style.TITLE, Label.Alignment.CENTER);
                        pagedListView.addItem(pageLabel);
                        currentOffset += pageLabel.getHeight() + pagedListView.getItemSpacing();

                        List<SkyblockItem> pageItems = sections.get(i);
                        int paddedSize = ((pageItems.size() + 8) / 9) * 9;
                        if (paddedSize == 0)
                            paddedSize = 54;
                        while (pageItems.size() < paddedSize) {
                            pageItems
                                    .add(new SkyblockItem(net.minecraft.world.item.ItemStack.EMPTY, "EMPTY", "COMMON"));
                        }

                        ItemGridWidget grid = new ItemGridWidget(0, 0, 9, pageItems);
                        grid.setAlignment(ItemGridWidget.Alignment.CENTER);
                        pagedListView.addItem(grid);
                        currentOffset += grid.getHeight() + pagedListView.getItemSpacing();

                        Widget spacer = new Widget(0, 0, 0, 20) {
                            @Override
                            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
                            }
                        };
                        pagedListView.addItem(spacer);
                        currentOffset += 20 + pagedListView.getItemSpacing();
                    }
                }
            } else {
                List<SkyblockItem> otherItems = new ArrayList<>();
                if (currentSubTab == SubTab.VAULT && inventory.has("personal_vault_contents")) {
                    otherItems.addAll(ItemDeserializer.deserializeList(
                            inventory.getAsJsonObject("personal_vault_contents").get("data").getAsString()));
                } else if (currentSubTab == SubTab.WARDROBE && inventory.has("wardrobe_contents")) {
                    otherItems.addAll(ItemDeserializer
                            .deserializeList(inventory.getAsJsonObject("wardrobe_contents").get("data").getAsString()));
                }

                if (!otherItems.isEmpty()) {
                    ItemGridWidget grid = new ItemGridWidget(startX + (contentWidth - (9 * 36 + 12)) / 2, startY, 9,
                            otherItems);
                    tab.addWidget(grid);
                    currentWidgets.add(grid);
                }
            }
        }

        if (currentSubTab == SubTab.PETS) {
            if (profileData.has("pets")) {
                JsonElement petsEl = profileData.get("pets");
                if (petsEl.isJsonArray()) {
                    com.google.gson.JsonArray pets = petsEl.getAsJsonArray();
                    for (com.google.gson.JsonElement p : pets) {
                        if (p.isJsonObject()) {
                            petItems.add(ItemDeserializer.deserializePet(p.getAsJsonObject()));
                        }
                    }
                } else if (petsEl.isJsonObject()) {
                    JsonObject petsObj = petsEl.getAsJsonObject();
                    for (String key : petsObj.keySet()) {
                        JsonElement p = petsObj.get(key);
                        if (p.isJsonObject()) {
                            petItems.add(ItemDeserializer.deserializePet(p.getAsJsonObject()));
                        }
                    }
                }
            }

            if (!petItems.isEmpty()) {
                int leftWidth = (int) (contentWidth * 0.6);
                int rightWidth = contentWidth - leftWidth - 30;
                int gridX = startX + 10;

                ItemGridWidget grid = new ItemGridWidget(gridX, startY, 10, petItems);
                grid.setSelectedItem(selectedPet);
                grid.setOnClick(item -> {
                    this.selectedPet = item;
                    if (petDetailWidget != null) {
                        petDetailWidget.setPet(item);
                    }
                    grid.setSelectedItem(item);
                });
                tab.addWidget(grid);
                currentWidgets.add(grid);

                if (selectedPet == null && !petItems.isEmpty()) {
                    selectedPet = petItems.get(0);
                    grid.setSelectedItem(selectedPet);
                }

                petDetailWidget = new PetDetailWidget(gridX + leftWidth + 10, startY, rightWidth, grid.getHeight());
                petDetailWidget.setPet(selectedPet);
                tab.addWidget(petDetailWidget);
                currentWidgets.add(petDetailWidget);
            }
        }
    }
}
