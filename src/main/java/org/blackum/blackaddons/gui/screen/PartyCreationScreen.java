package org.blackum.blackaddons.gui.screen;

import org.blackum.blackaddons.core.util.MinecraftInstance;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.core.manager.PartyFinderManager;

import java.util.List;

public class PartyCreationScreen extends BaseScreen {

    private String selectedFloor = "M7";

    public PartyCreationScreen(Screen parent) {
        super(Component.literal("Create Dungeon Party"), parent);
    }

    @Override
    public void init() {
        this.width = MinecraftInstance.mc.getWindow().getGuiScaledWidth();
        this.height = MinecraftInstance.mc.getWindow().getGuiScaledHeight();

        this.containerWidth = 200;
        this.containerHeight = 150;
        this.containerX = (this.width - this.containerWidth) / 2;
        this.containerY = (this.height - this.containerHeight) / 2;

        widgets.clear();
        initWidgets();

        int maxWidgetY = 0;
        for (Widget w : widgets) {
            int relativeBottom = (w.getY() + w.getHeight()) - this.containerY;
            if (relativeBottom > maxWidgetY) {
                maxWidgetY = relativeBottom;
            }
        }
        this.baseContentHeight = Math.max(0, maxWidgetY + 20);
        this.contentHeight = this.baseContentHeight;
    }

    @Override
    protected void initWidgets() {
        int contentX = containerX + Theme.PADDING_LARGE;
        int contentY = containerY + Theme.PADDING_LARGE;

        widgets.add(new Label(contentX, contentY, "Create Party", Label.Style.TITLE));

        int labelY = contentY + 25;
        widgets.add(new Label(contentX, labelY, "Select Floor:", Label.Style.BODY));

        int dropdownY = labelY + 15;
        List<String> floors = List.of("M7", "M6", "M5", "M4", "M3", "M2", "M1", "F7", "F6", "F5", "F4", "F3", "F2",
                "F1", "Entrance");
        Dropdown floorDropdown = new Dropdown(contentX, dropdownY, 170, Theme.BUTTON_HEIGHT, "Select Floor", floors,
                floor -> {
                    this.selectedFloor = floor;
                });
        floorDropdown.setSelectedOption(selectedFloor);

        int buttonsY = dropdownY + Theme.BUTTON_HEIGHT + Theme.PADDING_LARGE;
        Button createBtn = new Button(contentX, buttonsY, 80, Theme.BUTTON_HEIGHT, "Create", () -> {
            PartyFinderManager.getInstance().createParty(selectedFloor, new JsonObject());
            onClose();
        });
        widgets.add(createBtn);

        Button cancelBtn = new Button(contentX + 80 + Theme.PADDING, buttonsY, 80, Theme.BUTTON_HEIGHT, "Cancel",
                this::onClose);
        widgets.add(cancelBtn);

        widgets.add(floorDropdown);
    }
}
