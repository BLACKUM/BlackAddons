package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;

public class TriggerActionEditScreen extends BaseScreen {
    private final ConfigManager.ChatTrigger trigger;
    private ListView actionsList;

    public TriggerActionEditScreen(Screen parent, ConfigManager.ChatTrigger trigger) {
        super(Component.literal("Edit Trigger Actions"), parent);
        this.trigger = trigger;
    }

    @Override
    protected int getContentHeight() {
        return 0;
    }

    @Override
    protected void initWidgets() {
        int listWidth = containerWidth - Theme.PADDING * 2;
        int listHeight = containerHeight - 110;
        int listX = containerX + Theme.PADDING;
        int listY = containerY + 60;

        actionsList = new ListView(listX, listY, listWidth, listHeight);
        rebuildActions();
        widgets.add(actionsList);

        Button addBtn = new Button(listX, listY - 25, listWidth, 20, "Add New Action", () -> {
            trigger.actions.add(new ConfigManager.TriggerAction(ConfigManager.TriggerActionType.SWITCH_SLOT, 0, "", 0, 0));
            ConfigManager.save();
            int currentScroll = actionsList.getScrollOffset();
            rebuildActions();
            actionsList.setScrollOffset(currentScroll);
        });
        widgets.add(addBtn);

        Button backBtn = new Button(listX, containerY + containerHeight - 30, listWidth, 20, "Back", () -> {
            if (parent != null) {
                minecraft.setScreen(parent);
            } else {
                onClose();
            }
        });
        widgets.add(backBtn);
    }

    private void rebuildActions() {
        actionsList.clearItems();
        int itemWidth = actionsList.getWidth() - 16;

        for (int i = 0; i < trigger.actions.size(); i++) {
            final int index = i;
            ConfigManager.TriggerAction action = trigger.actions.get(i);

            SectionHeader header = new SectionHeader(itemWidth, "Action " + (i + 1) + ": " + action.type.getDisplayName());
            ExpandableGroup group = new ExpandableGroup(0, 0, itemWidth, header, !action.collapsed);
            
            header.setCollapsed(action.collapsed);
            header.setToggleCallback(() -> {
                action.collapsed = !action.collapsed;
                group.setExpanded(!action.collapsed);
                ConfigManager.save();
            });
            List<String> typeOptions = java.util.stream.Stream.of(ConfigManager.TriggerActionType.values())
                    .map(ConfigManager.TriggerActionType::getDisplayName)
                    .toList();
            Dropdown typeDropdown = new Dropdown(0, 0, itemWidth, "Action Type", typeOptions, selected -> {
                action.type = ConfigManager.TriggerActionType.fromDisplayName(selected);
                ConfigManager.save();
                int currentScroll = actionsList.getScrollOffset();
                rebuildActions();
                actionsList.setScrollOffset(currentScroll);
            });
            typeDropdown.setSelectedIndex(action.type.ordinal());
            group.addChild(new SettingWrapper(0, 0, itemWidth, "Action Type", "What this action does", typeDropdown));

            SettingWrapper delayWrap = new SettingWrapper(0, 0, itemWidth, "Delay (Ticks)", "Wait (Ticks) before this action", null);
            Slider delaySlider = new Slider(0, 0, itemWidth, 0, 100, action.delayTicks, val -> {
                action.delayTicks = Math.round(val);
                delayWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", action.delayTicks, action.delayTicks / 20.0));
                ConfigManager.save();
            });
            delayWrap.setControl(delaySlider);
            delayWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", action.delayTicks, action.delayTicks / 20.0));
            group.addChild(delayWrap);

            if (action.type == ConfigManager.TriggerActionType.USE_ITEM || action.type == ConfigManager.TriggerActionType.ATTACK || action.type == ConfigManager.TriggerActionType.PRESS_KEYBIND) {
                SettingWrapper durationWrap = new SettingWrapper(0, 0, itemWidth, "Duration (Ticks)", "How long to hold (0 = click)", null);
                Slider durationSlider = new Slider(0, 0, itemWidth, 0, 100, action.durationTicks, val -> {
                    action.durationTicks = Math.round(val);
                    durationWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", action.durationTicks, action.durationTicks / 20.0));
                    ConfigManager.save();
                });
                durationWrap.setControl(durationSlider);
                durationWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", action.durationTicks, action.durationTicks / 20.0));
                group.addChild(durationWrap);
            }

            if (action.type == ConfigManager.TriggerActionType.SWITCH_SLOT) {
                Slider slotSlider = new Slider(0, 0, itemWidth, 0, 8, action.slotIndex, null);
                SettingWrapper slotWrap = new SettingWrapper(0, 0, itemWidth, "Hotbar Slot", "Slot to select (0-8)", null);
                slotWrap.setRightLabel("Slot " + (action.slotIndex + 1));
                slotSlider.onValueChange(val -> {
                    action.slotIndex = Math.round(val);
                    slotWrap.setRightLabel("Slot " + (action.slotIndex + 1));
                    ConfigManager.save();
                });
                slotWrap.setControl(slotSlider);
                slotWrap.setRightLabel("Slot " + (action.slotIndex + 1));
                group.addChild(slotWrap);
            } else if (action.type == ConfigManager.TriggerActionType.SEND_MESSAGE) {
                TextField msgField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Message...");
                msgField.setText(action.message);
                msgField.setOnValueChange(val -> {
                    action.message = val;
                    ConfigManager.save();
                });
                group.addChild(new SettingWrapper(0, 0, itemWidth, "Message", "Chat message or /command", msgField));
            } else if (action.type == ConfigManager.TriggerActionType.PRESS_KEYBIND) {
                AutocompleteTextField keyField = new AutocompleteTextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "key.keyboard.f5", () -> {
                    List<String> keys = new ArrayList<>();
                    for (net.minecraft.client.KeyMapping km : minecraft.options.keyMappings) {
                        keys.add(km.getName());
                    }
                    return keys;
                });
                keyField.setText(action.message);
                keyField.setOnValueChange(val -> {
                    action.message = val;
                    ConfigManager.save();
                });
                group.addChild(new SettingWrapper(0, 0, itemWidth, "Keybind Name", "Internal name (e.g. key.jump)", keyField));
            } else if (action.type == ConfigManager.TriggerActionType.ROTATE) {
                if (!action.useCoordinates) {
                    SettingWrapper yawWrap = new SettingWrapper(0, 0, itemWidth, "Yaw", "Target horizontal rotation", null);
                    Slider yawSlider = new Slider(0, 0, itemWidth, -180, 180, action.yaw, val -> {
                        action.yaw = val;
                        yawWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", action.yaw));
                        ConfigManager.save();
                    });
                    yawWrap.setControl(yawSlider);
                    yawWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", action.yaw));
                    group.addChild(yawWrap);

                    SettingWrapper pitchWrap = new SettingWrapper(0, 0, itemWidth, "Pitch", "Target vertical rotation", null);
                    Slider pitchSlider = new Slider(0, 0, itemWidth, -90, 90, action.pitch, val -> {
                        action.pitch = val;
                        pitchWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", action.pitch));
                        ConfigManager.save();
                    });
                    pitchWrap.setControl(pitchSlider);
                    pitchWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", action.pitch));
                    group.addChild(pitchWrap);
                }

                ToggleSwitch coordToggle = new ToggleSwitch(0, 0, itemWidth, "Use Coordinates",
                        "Rotate to specific X, Y, Z instead of Yaw/Pitch", action.useCoordinates, val -> {
                    action.useCoordinates = val;
                    ConfigManager.save();
                    rebuildActions();
                });
                group.addChild(coordToggle);

                if (action.useCoordinates) {
                    TextField xField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "X...");
                    xField.setText(String.valueOf(action.targetX));
                    xField.setOnValueChange(val -> {
                        try { action.targetX = Double.parseDouble(val); ConfigManager.save(); } catch (Exception e) {}
                    });
                    group.addChild(new SettingWrapper(0, 0, itemWidth, "Target X", "X Coordinate", xField));

                    TextField yField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Y...");
                    yField.setText(String.valueOf(action.targetY));
                    yField.setOnValueChange(val -> {
                        try { action.targetY = Double.parseDouble(val); ConfigManager.save(); } catch (Exception e) {}
                    });
                    group.addChild(new SettingWrapper(0, 0, itemWidth, "Target Y", "Y Coordinate", yField));

                    TextField zField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Z...");
                    zField.setText(String.valueOf(action.targetZ));
                    zField.setOnValueChange(val -> {
                        try { action.targetZ = Double.parseDouble(val); ConfigManager.save(); } catch (Exception e) {}
                    });
                    group.addChild(new SettingWrapper(0, 0, itemWidth, "Target Z", "Z Coordinate", zField));
                }
            }

            GridRow moveRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
            Button upBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, Theme.BUTTON_HEIGHT, "Move Up", () -> {
                if (index > 0) {
                    ConfigManager.TriggerAction prev = trigger.actions.remove(index);
                    trigger.actions.add(index - 1, prev);
                    ConfigManager.save();
                    int currentScroll = actionsList.getScrollOffset();
                    rebuildActions();
                    actionsList.setScrollOffset(currentScroll);
                }
            });
            Button downBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, Theme.BUTTON_HEIGHT, "Move Down", () -> {
                if (index < trigger.actions.size() - 1) {
                    ConfigManager.TriggerAction next = trigger.actions.remove(index);
                    trigger.actions.add(index + 1, next);
                    ConfigManager.save();
                    int currentScroll = actionsList.getScrollOffset();
                    rebuildActions();
                    actionsList.setScrollOffset(currentScroll);
                }
            });
            moveRow.addChild(upBtn, 0);
            moveRow.addChild(downBtn, (itemWidth + Theme.PADDING) / 2);
            group.addChild(moveRow);

            Button deleteBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Delete Action", () -> {
                trigger.actions.remove(index);
                ConfigManager.save();
                int currentScroll = actionsList.getScrollOffset();
                rebuildActions();
                actionsList.setScrollOffset(currentScroll);
            });
            group.addChild(deleteBtn);

            actionsList.addItem(group);
        }
    }

    @Override
    protected void renderScrolledContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderHelper.drawCenteredString(graphics, font, "Editing Actions for: " + trigger.pattern, containerX + containerWidth / 2, containerY + 20, Theme.TEXT_PRIMARY);
    }
}
