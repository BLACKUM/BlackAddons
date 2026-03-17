package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ActionManager;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;

public class WaypointActionEditScreen extends BaseScreen {
    private final Waypoint waypoint;
    private final ConfigManager.WaypointAction action;
    private ListView stepsList;

    public WaypointActionEditScreen(Screen parent, Waypoint waypoint, ConfigManager.WaypointAction action) {
        super(Component.literal("Edit Waypoint Action"), parent);
        this.waypoint = waypoint;
        this.action = action;
    }

    @Override
    protected int getContentHeight() {
        return 0;
    }

    @Override
    protected void initWidgets() {
        int fieldX = containerX + Theme.PADDING;
        int fieldWidth = containerWidth - Theme.PADDING * 2;
        int currentY = containerY + 60;

        widgets.add(new Label(fieldX, currentY, "Triggers:", Label.Style.CAPTION));
        currentY += 15;
        
        ToggleSwitch entryToggle = new ToggleSwitch(fieldX, currentY, fieldWidth, "Trigger on Entry", "Fire actions when entering radius", action.triggerOnEntry, val -> {
            action.triggerOnEntry = val;
            WaypointManager.getInstance().save();
        });
        widgets.add(entryToggle);
        currentY += 25;

        ToggleSwitch exitToggle = new ToggleSwitch(fieldX, currentY, fieldWidth, "Trigger on Exit", "Fire actions when leaving radius", action.triggerOnExit, val -> {
            action.triggerOnExit = val;
            WaypointManager.getInstance().save();
        });
        widgets.add(exitToggle);
        currentY += 35;

        widgets.add(new Label(fieldX, currentY, "Title:", Label.Style.CAPTION));
        currentY += 15;

        TextField titleField = new TextField(fieldX, currentY, (fieldWidth - Theme.PADDING) / 2, Theme.TEXTFIELD_HEIGHT, "Title");
        titleField.setText(action.title);
        titleField.setOnValueChange(val -> {
            action.title = val;
            WaypointManager.getInstance().save();
        });
        widgets.add(titleField);

        TextField subtitleField = new TextField(fieldX + (fieldWidth + Theme.PADDING) / 2, currentY, (fieldWidth - Theme.PADDING) / 2, Theme.TEXTFIELD_HEIGHT, "Subtitle");
        subtitleField.setText(action.subtitle);
        subtitleField.setOnValueChange(val -> {
            action.subtitle = val;
            WaypointManager.getInstance().save();
        });
        widgets.add(subtitleField);
        currentY += 35;

        ToggleSwitch notifyToggle = new ToggleSwitch(fieldX, currentY, fieldWidth, "Show Notification",
                "Show a custom notification when triggered", action.showNotification, val -> {
            action.showNotification = val;
            WaypointManager.getInstance().save();
            this.init();
        });
        widgets.add(notifyToggle);
        currentY += 25;

        if (action.showNotification) {
            TextField nTitleField = new TextField(fieldX, currentY, (fieldWidth - Theme.PADDING) / 2, Theme.TEXTFIELD_HEIGHT, "Notification Title");
            nTitleField.setText(action.notificationTitle != null ? action.notificationTitle : "");
            nTitleField.setOnValueChange(val -> {
                action.notificationTitle = val;
                WaypointManager.getInstance().save();
            });
            widgets.add(nTitleField);

            TextField nMsgField = new TextField(fieldX + (fieldWidth + Theme.PADDING) / 2, currentY, (fieldWidth - Theme.PADDING) / 2, Theme.TEXTFIELD_HEIGHT, "Notification Message");
            nMsgField.setText(action.notificationMessage != null ? action.notificationMessage : "");
            nMsgField.setOnValueChange(val -> {
                action.notificationMessage = val;
                WaypointManager.getInstance().save();
            });
            widgets.add(nMsgField);
            currentY += 35;

            List<String> typeOptions = java.util.stream.Stream.of(NotificationType.values()).map(Enum::name).toList();
            Dropdown typeDropdown = new Dropdown(fieldX, currentY, fieldWidth - 54, Theme.TEXTFIELD_HEIGHT, "Notification Type", typeOptions, selected -> {
                action.notificationType = NotificationType.valueOf(selected);
                WaypointManager.getInstance().save();
            });
            typeDropdown.setSelectedIndex(action.notificationType.ordinal());
            widgets.add(typeDropdown);

            Button testNotifyBtn = new Button(fieldX + fieldWidth - 50, currentY, 50, Theme.TEXTFIELD_HEIGHT, "Test", () -> {
                NotificationManager.addNotification(
                        org.blackum.blackaddons.core.util.FormatUtils.formatColor(action.notificationTitle != null && !action.notificationTitle.isEmpty() ? action.notificationTitle : "Test Title"),
                        org.blackum.blackaddons.core.util.FormatUtils.formatColor(action.notificationMessage != null && !action.notificationMessage.isEmpty() ? action.notificationMessage : "Test Message"),
                        action.notificationType
                );
            });
            widgets.add(testNotifyBtn);
            currentY += 35;
        }

        widgets.add(new Label(fieldX, currentY, "Action Steps:", Label.Style.CAPTION));
        currentY += 15;

        int listHeight = containerHeight - (currentY - containerY) - 40;
        stepsList = new ListView(fieldX, currentY, fieldWidth, listHeight);
        rebuildSteps();
        widgets.add(stepsList);

        Button addBtn = new Button(fieldX, currentY + listHeight + 5, fieldWidth / 2 - 2, 20, "Add Step", () -> {
            action.actions.add(new ConfigManager.ActionStep(ConfigManager.ActionStepType.SEND_MESSAGE, 0, "", 0, 0));
            WaypointManager.getInstance().save();
            int currentScroll = stepsList.getScrollOffset();
            rebuildSteps();
            stepsList.setScrollOffset(currentScroll);
        });
        widgets.add(addBtn);

        Button backBtn = new Button(fieldX + fieldWidth / 2 + 2, currentY + listHeight + 5, fieldWidth / 2 - 2, 20, "Back", () -> {
            minecraft.setScreen(parent);
        });
        widgets.add(backBtn);
    }

    private void rebuildSteps() {
        stepsList.clearItems();
        int itemWidth = stepsList.getWidth() - 16;

        for (int i = 0; i < action.actions.size(); i++) {
            final int index = i;
            ConfigManager.ActionStep step = action.actions.get(i);

            SectionHeader header = new SectionHeader(itemWidth, "Step " + (i + 1) + ": " + step.type.getDisplayName());
            ExpandableGroup group = new ExpandableGroup(0, 0, itemWidth, header, !step.collapsed);
            
            header.setCollapsed(step.collapsed);
            header.setToggleCallback(() -> {
                step.collapsed = !step.collapsed;
                group.setExpanded(!step.collapsed);
                WaypointManager.getInstance().save();
            });

            java.util.List<String> typeOptions = java.util.stream.Stream.of(ConfigManager.ActionStepType.values())
                    .map(ConfigManager.ActionStepType::getDisplayName)
                    .toList();
            Dropdown typeDropdown = new Dropdown(0, 0, itemWidth, "Step Type", typeOptions, selected -> {
                step.type = ConfigManager.ActionStepType.fromDisplayName(selected);
                WaypointManager.getInstance().save();
                int currentScroll = stepsList.getScrollOffset();
                rebuildSteps();
                stepsList.setScrollOffset(currentScroll);
            });
            typeDropdown.setSelectedIndex(step.type.ordinal());
            group.addChild(new SettingWrapper(0, 0, itemWidth, "Step Type", "What this step does", typeDropdown));

            SettingWrapper delayWrap = new SettingWrapper(0, 0, itemWidth, "Delay (Ticks)", "Wait (Ticks) before this step", null);
            Slider delaySlider = new Slider(0, 0, itemWidth, 0, 100, step.delayTicks, val -> {
                step.delayTicks = Math.round(val);
                delayWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", step.delayTicks, step.delayTicks / 20.0));
                WaypointManager.getInstance().save();
            });
            delayWrap.setControl(delaySlider);
            delayWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", step.delayTicks, step.delayTicks / 20.0));
            group.addChild(delayWrap);

            if (step.type == ConfigManager.ActionStepType.USE_ITEM || step.type == ConfigManager.ActionStepType.ATTACK || step.type == ConfigManager.ActionStepType.PRESS_KEYBIND) {
                SettingWrapper durationWrap = new SettingWrapper(0, 0, itemWidth, "Duration (Ticks)", "How long to hold (0 = click)", null);
                Slider durationSlider = new Slider(0, 0, itemWidth, 0, 100, step.durationTicks, val -> {
                    step.durationTicks = Math.round(val);
                    durationWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", step.durationTicks, step.durationTicks / 20.0));
                    WaypointManager.getInstance().save();
                });
                durationWrap.setControl(durationSlider);
                durationWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", step.durationTicks, step.durationTicks / 20.0));
                group.addChild(durationWrap);
            }

            if (step.type == ConfigManager.ActionStepType.SWITCH_SLOT) {
                Slider slotSlider = new Slider(0, 0, itemWidth, 0, 8, step.slotIndex, null);
                SettingWrapper slotWrap = new SettingWrapper(0, 0, itemWidth, "Hotbar Slot", "Slot to select (0-8)", null);
                slotWrap.setRightLabel("Slot " + (step.slotIndex + 1));
                slotSlider.onValueChange(val -> {
                    step.slotIndex = Math.round(val);
                    slotWrap.setRightLabel("Slot " + (step.slotIndex + 1));
                    WaypointManager.getInstance().save();
                });
                slotWrap.setControl(slotSlider);
                slotWrap.setRightLabel("Slot " + (step.slotIndex + 1));
                group.addChild(slotWrap);
            } else if (step.type == ConfigManager.ActionStepType.SEND_MESSAGE) {
                TextField msgField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Message");
                msgField.setText(step.message);
                msgField.setOnValueChange(val -> {
                    step.message = val;
                    WaypointManager.getInstance().save();
                });
                group.addChild(new SettingWrapper(0, 0, itemWidth, "Message", "Chat message or /command", msgField));
            } else if (step.type == ConfigManager.ActionStepType.PRESS_KEYBIND) {
                AutocompleteTextField keyField = new AutocompleteTextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "key.keyboard.f5", () -> {
                    ArrayList<String> keys = new ArrayList<>();
                    for (net.minecraft.client.KeyMapping km : minecraft.options.keyMappings) {
                        keys.add(km.getName());
                    }
                    return keys;
                });
                keyField.setText(step.message);
                keyField.setOnValueChange(val -> {
                    step.message = val;
                    WaypointManager.getInstance().save();
                });
                group.addChild(new SettingWrapper(0, 0, itemWidth, "Keybind Name", "Internal name (e.g. key.jump)", keyField));
            } else if (step.type == ConfigManager.ActionStepType.ROTATE) {
                if (!step.useCoordinates) {
                    SettingWrapper yawWrap = new SettingWrapper(0, 0, itemWidth, "Yaw", "Target horizontal rotation", null);
                    Slider yawSlider = new Slider(0, 0, itemWidth, -180, 180, step.yaw, val -> {
                        step.yaw = val;
                        yawWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", step.yaw));
                        WaypointManager.getInstance().save();
                    });
                    yawWrap.setControl(yawSlider);
                    yawWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", step.yaw));
                    group.addChild(yawWrap);

                    SettingWrapper pitchWrap = new SettingWrapper(0, 0, itemWidth, "Pitch", "Target vertical rotation", null);
                    Slider pitchSlider = new Slider(0, 0, itemWidth, -90, 90, step.pitch, val -> {
                        step.pitch = val;
                        pitchWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", step.pitch));
                        WaypointManager.getInstance().save();
                    });
                    pitchWrap.setControl(pitchSlider);
                    pitchWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", step.pitch));
                    group.addChild(pitchWrap);
                }

                ToggleSwitch coordToggle = new ToggleSwitch(0, 0, itemWidth, "Use Coordinates",
                        "Rotate to specific X, Y, Z instead of Yaw/Pitch", step.useCoordinates, val -> {
                    step.useCoordinates = val;
                    WaypointManager.getInstance().save();
                    rebuildSteps();
                });
                group.addChild(coordToggle);

                if (step.useCoordinates) {
                    TextField xField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "X");
                    xField.setText(String.valueOf(step.targetX));
                    xField.setOnValueChange(val -> {
                        try { step.targetX = Double.parseDouble(val); ConfigManager.save(); } catch (Exception ignored) {}
                    });
                    group.addChild(new SettingWrapper(0, 0, itemWidth, "Target X", "X Coordinate", xField));

                    TextField yField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Y");
                    yField.setText(String.valueOf(step.targetY));
                    yField.setOnValueChange(val -> {
                        try { step.targetY = Double.parseDouble(val); ConfigManager.save(); } catch (Exception ignored) {}
                    });
                    group.addChild(new SettingWrapper(0, 0, itemWidth, "Target Y", "Y Coordinate", yField));

                    TextField zField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Z");
                    zField.setText(String.valueOf(step.targetZ));
                    zField.setOnValueChange(val -> {
                        try { step.targetZ = Double.parseDouble(val); ConfigManager.save(); } catch (Exception ignored) {}
                    });
                    group.addChild(new SettingWrapper(0, 0, itemWidth, "Target Z", "Z Coordinate", zField));
                }
            }

            GridRow moveRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
            Button upBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, Theme.BUTTON_HEIGHT, "Move Up", () -> {
                if (index > 0) {
                    ConfigManager.ActionStep prev = action.actions.remove(index);
                    action.actions.add(index - 1, prev);
                    WaypointManager.getInstance().save();
                    int currentScroll = stepsList.getScrollOffset();
                    rebuildSteps();
                    stepsList.setScrollOffset(currentScroll);
                }
            });
            Button downBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, Theme.BUTTON_HEIGHT, "Move Down", () -> {
                if (index < action.actions.size() - 1) {
                    ConfigManager.ActionStep next = action.actions.remove(index);
                    action.actions.add(index + 1, next);
                    WaypointManager.getInstance().save();
                    int currentScroll = stepsList.getScrollOffset();
                    rebuildSteps();
                    stepsList.setScrollOffset(currentScroll);
                }
            });
            moveRow.addChild(upBtn, 0);
            moveRow.addChild(downBtn, (itemWidth + Theme.PADDING) / 2);
            group.addChild(moveRow);

            Button deleteBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Delete Step", () -> {
                action.actions.remove(index);
                ConfigManager.save();
                int currentScroll = stepsList.getScrollOffset();
                rebuildSteps();
                stepsList.setScrollOffset(currentScroll);
            });
            group.addChild(deleteBtn);

            stepsList.addItem(group);
        }
    }

    @Override
    protected void renderScrolledContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        String title = "Editing Actions for: " + (waypoint.name != null && !waypoint.name.isEmpty() ? waypoint.name : "Unnamed Waypoint");
        if (minecraft.player != null) {
            double dist = Math.sqrt(Math.pow(waypoint.x - minecraft.player.getX(), 2) +
                    Math.pow(waypoint.y - minecraft.player.getY(), 2) +
                    Math.pow(waypoint.z - minecraft.player.getZ(), 2));
            title += String.format(java.util.Locale.ROOT, " (%.1fm)", dist);
        }
        RenderHelper.drawCenteredString(graphics, font, title, containerX + containerWidth / 2, containerY + 20, Theme.TEXT_PRIMARY);
    }
}
