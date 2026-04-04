package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.model.DungeonFloor;
import org.blackum.blackaddons.core.waypoint.WaypointGroup;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;

public class WaypointGroupEditScreen extends BaseScreen {

    private static final String FILTER_ANY = "Any";
    private static final String DUNGEON_YES = "In Dungeon";
    private static final String DUNGEON_NO = "Not in Dungeon";
    private static final String BOSS_YES = "In Boss";
    private static final String BOSS_NO = "Not in Boss";

    private final WaypointGroup group;
    private final Runnable onSave;

    public WaypointGroupEditScreen(Screen parent, WaypointGroup group, Runnable onSave) {
        super(Component.literal("Edit Group"), parent);
        this.group = group;
        this.onSave = onSave;
    }

    @Override
    protected int getContentHeight() {
        return 0;
    }

    @Override
    public void initWidgets() {
        int listWidth = containerWidth - Theme.PADDING * 2;
        ListView list = new ListView(containerX + Theme.PADDING, containerY + 40, listWidth, containerHeight - 50);
        int itemWidth = list.getWidth() - 16;

        list.addItem(new Label(0, 0, "Group Settings", Label.Style.CAPTION));
        
        TextField nameField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Group name");
        nameField.setText(group.name != null ? group.name : "");
        nameField.setOnValueChange(val -> group.name = val);
        list.addItem(new SettingWrapper(0, 0, itemWidth, "Name", "Visible label for this group", nameField));

        ToggleSwitch enabledToggle = new ToggleSwitch(0, 0, itemWidth, "Enabled", "Whether this group and its waypoints are active", group.enabled, val -> group.enabled = val);
        list.addItem(enabledToggle);

        list.addItem(new Label(0, 0, "Conditions", Label.Style.CAPTION));
        list.addItem(new Label(0, 0, "Leave conditions as 'Any' to ignore them.", Label.Style.BODY));

        boolean isF7orM7 = "F7".equals(group.floorFilter) || "M7".equals(group.floorFilter);

        List<String> dungeonOptions = List.of(FILTER_ANY, DUNGEON_YES, DUNGEON_NO);
        Dropdown dungeonDropdown = new Dropdown(0, 0, itemWidth, "In Dungeon", dungeonOptions, selected -> {
            if (DUNGEON_YES.equals(selected)) group.inDungeonFilter = true;
            else if (DUNGEON_NO.equals(selected)) group.inDungeonFilter = false;
            else group.inDungeonFilter = null;
        });
        String currentDungeon = group.inDungeonFilter == null ? FILTER_ANY : (group.inDungeonFilter ? DUNGEON_YES : DUNGEON_NO);
        dungeonDropdown.setSelectedOption(currentDungeon);
        list.addItem(new SettingWrapper(0, 0, itemWidth, "In Dungeon", "Only show when in / not in a dungeon", dungeonDropdown));

        List<String> floorOptions = new ArrayList<>();
        floorOptions.add(FILTER_ANY);
        for (DungeonFloor floor : DungeonFloor.values()) {
            floorOptions.add(floor.getDisplayName());
        }

        SettingWrapper phaseWrapper = new SettingWrapper(0, 0, itemWidth, "F7/M7 Phase",
                "Only show during a specific F7 or M7 boss phase", null);
        List<String> phaseOptions = new ArrayList<>();
        phaseOptions.add(FILTER_ANY);
        for (int i = 1; i <= 5; i++) phaseOptions.add("Phase " + i);
        Dropdown phaseDropdown = new Dropdown(0, 0, itemWidth, "Phase", phaseOptions, selected -> {
            if (FILTER_ANY.equals(selected)) group.phaseFilter = null;
            else {
                try { group.phaseFilter = Integer.parseInt(selected.replace("Phase ", "")); }
                catch (NumberFormatException ignored) { group.phaseFilter = null; }
            }
        });
        String currentPhase = (group.phaseFilter != null && group.phaseFilter > 0) ? "Phase " + group.phaseFilter : FILTER_ANY;
        phaseDropdown.setSelectedOption(currentPhase);
        phaseWrapper.setControl(phaseDropdown);
        phaseWrapper.setVisible(isF7orM7);

        Dropdown floorDropdown = new Dropdown(0, 0, itemWidth, "Dungeon Floor", floorOptions, selected -> {
            group.floorFilter = FILTER_ANY.equals(selected) ? null : selected;
            boolean f7m7 = "F7".equals(group.floorFilter) || "M7".equals(group.floorFilter);
            phaseWrapper.setVisible(f7m7);
            if (!f7m7) group.phaseFilter = null;
        });
        floorDropdown.setSelectedOption(group.floorFilter != null ? group.floorFilter : FILTER_ANY);
        list.addItem(new SettingWrapper(0, 0, itemWidth, "Dungeon Floor", "Only show on this floor", floorDropdown));

        List<String> bossOptions = List.of(FILTER_ANY, BOSS_YES, BOSS_NO);
        Dropdown bossDropdown = new Dropdown(0, 0, itemWidth, "Boss Room", bossOptions, selected -> {
            if (BOSS_YES.equals(selected)) group.inBossFilter = true;
            else if (BOSS_NO.equals(selected)) group.inBossFilter = false;
            else group.inBossFilter = null;
        });
        String currentBoss = group.inBossFilter == null ? FILTER_ANY : (group.inBossFilter ? BOSS_YES : BOSS_NO);
        bossDropdown.setSelectedOption(currentBoss);
        list.addItem(new SettingWrapper(0, 0, itemWidth, "Boss Room", "Only show in / not in boss room", bossDropdown));

        list.addItem(phaseWrapper);

        list.addItem(new Widget(0, 0, itemWidth, 10) {
            @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {}
        });

        GridRow btnRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
        Button saveBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, Theme.BUTTON_HEIGHT, "Save", () -> {
            WaypointManager.getInstance().save();
            onSave.run();
            minecraft.setScreen(parent);
        });
        Button cancelBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, Theme.BUTTON_HEIGHT, "Cancel",
                () -> minecraft.setScreen(parent));
        btnRow.addChild(saveBtn, 0);
        btnRow.addChild(cancelBtn, (itemWidth + Theme.PADDING) / 2);
        list.addItem(btnRow);

        widgets.add(list);
    }

    @Override
    protected void extractScrolledContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        RenderHelper.drawCenteredString(graphics, font, "Edit Group", containerX + containerWidth / 2, containerY + 20, Theme.TEXT_PRIMARY);
    }
}
