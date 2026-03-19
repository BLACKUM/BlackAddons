package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ActionManager;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;

public class ChatActionEditScreen extends BaseScreen {
    private final ConfigManager.ChatAction trigger;
    private ListView actionsList;

    public ChatActionEditScreen(Screen parent, ConfigManager.ChatAction trigger) {
        super(Component.literal("Edit Action Steps"), parent);
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

        Button addBtn = new Button(listX, listY - 25, listWidth, 20, "Add New Step", () -> {
            trigger.actions.add(new ConfigManager.ActionStep(ConfigManager.ActionStepType.SWITCH_SLOT, 0, "", 0, 0));
            ActionManager.getInstance().save();
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
            ConfigManager.ActionStep action = trigger.actions.get(i);

            SectionHeader header = new SectionHeader(itemWidth, "Step " + (i + 1) + ": " + action.type.getDisplayName());
            ExpandableGroup group = new ExpandableGroup(0, 0, itemWidth, header, !action.collapsed);
            
            header.setCollapsed(action.collapsed);
            header.setToggleCallback(() -> {
                action.collapsed = !action.collapsed;
                group.setExpanded(!action.collapsed);
                ActionManager.getInstance().save();
            });
            List<String> typeOptions = java.util.stream.Stream.of(ConfigManager.ActionStepType.values())
                    .map(ConfigManager.ActionStepType::getDisplayName)
                    .toList();
            Dropdown typeDropdown = new Dropdown(0, 0, itemWidth, "Step Type", typeOptions, selected -> {
                action.type = ConfigManager.ActionStepType.fromDisplayName(selected);
                ActionManager.getInstance().save();
                int currentScroll = actionsList.getScrollOffset();
                rebuildActions();
                actionsList.setScrollOffset(currentScroll);
            });
            typeDropdown.setSelectedIndex(action.type.ordinal());
            group.addChild(new SettingWrapper(0, 0, itemWidth, "Step Type", "What this step does", typeDropdown));

            SettingWrapper delayWrap = new SettingWrapper(0, 0, itemWidth, "Delay (Ticks)", "Wait (Ticks) before this step", null);
            Slider delaySlider = new Slider(0, 0, itemWidth, 0, 100, action.delayTicks, val -> {
                action.delayTicks = Math.round(val);
                delayWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", action.delayTicks, action.delayTicks / 20.0));
                ActionManager.getInstance().save();
            });
            delayWrap.setControl(delaySlider);
            delayWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", action.delayTicks, action.delayTicks / 20.0));
            group.addChild(delayWrap);

            if (action.type == ConfigManager.ActionStepType.USE_ITEM || action.type == ConfigManager.ActionStepType.ATTACK || action.type == ConfigManager.ActionStepType.PRESS_KEYBIND) {
                SettingWrapper durationWrap = new SettingWrapper(0, 0, itemWidth, "Duration (Ticks)", "How long to hold (0 = click)", null);
                Slider durationSlider = new Slider(0, 0, itemWidth, 0, 100, action.durationTicks, val -> {
                    action.durationTicks = Math.round(val);
                    durationWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", action.durationTicks, action.durationTicks / 20.0));
                    ActionManager.getInstance().save();
                });
                durationWrap.setControl(durationSlider);
                durationWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%dt (%.2fs)", action.durationTicks, action.durationTicks / 20.0));
                group.addChild(durationWrap);
            }

            if (action.type == ConfigManager.ActionStepType.SWITCH_SLOT) {
                Slider slotSlider = new Slider(0, 0, itemWidth, 0, 8, action.slotIndex, null);
                SettingWrapper slotWrap = new SettingWrapper(0, 0, itemWidth, "Hotbar Slot", "Slot to select (0-8)", null);
                slotWrap.setRightLabel("Slot " + (action.slotIndex + 1));
                slotSlider.onValueChange(val -> {
                    action.slotIndex = Math.round(val);
                    slotWrap.setRightLabel("Slot " + (action.slotIndex + 1));
                    ActionManager.getInstance().save();
                });
                slotWrap.setControl(slotSlider);
                slotWrap.setRightLabel("Slot " + (action.slotIndex + 1));
                group.addChild(slotWrap);
            } else if (action.type == ConfigManager.ActionStepType.SEND_MESSAGE) {
                TextField msgField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Message...");
                msgField.setText(action.message);
                msgField.setOnValueChange(val -> {
                    action.message = val;
                    ActionManager.getInstance().save();
                });
                group.addChild(new SettingWrapper(0, 0, itemWidth, "Message", "Chat message or /command", msgField));
            } else if (action.type == ConfigManager.ActionStepType.PRESS_KEYBIND) {
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
                    ActionManager.getInstance().save();
                });
                group.addChild(new SettingWrapper(0, 0, itemWidth, "Keybind Name", "Internal name (e.g. key.jump)", keyField));
            } else if (action.type == ConfigManager.ActionStepType.ROTATE) {
                GridRow topRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
                topRow.addChild(new ToggleSwitch(0, 0, (itemWidth / 2) - 2, "Insta Snap", action.instaSnap, val -> {
                    action.instaSnap = val;
                    ActionManager.getInstance().save();
                    rebuildActions();
                }), 0);
                topRow.addChild(new ToggleSwitch(0, 0, (itemWidth / 2) - 2, "Use Coords", action.useCoordinates, val -> {
                    action.useCoordinates = val;
                    ActionManager.getInstance().save();
                    rebuildActions();
                }), (itemWidth / 2) + 2);
                group.addChild(topRow);

                if (!action.instaSnap) {
                    SettingWrapper speedWrap = new SettingWrapper(0, 0, itemWidth, "Rotation Speed", "Override speed (0 = use global)", null);
                    Slider speedSlider = new Slider(0, 0, itemWidth, 0, 100, action.rotationSpeed, val -> {
                        action.rotationSpeed = val;
                        speedWrap.setRightLabel(val == 0 ? "Global" : String.format(java.util.Locale.ROOT, "%.1f", val));
                        ActionManager.getInstance().save();
                    });
                    speedWrap.setControl(speedSlider);
                    speedWrap.setRightLabel(action.rotationSpeed == 0 ? "Global" : String.format(java.util.Locale.ROOT, "%.1f", action.rotationSpeed));
                    group.addChild(speedWrap);
                }

                SettingWrapper lookAtWrap = new SettingWrapper(0, 0, itemWidth, "Look At Time", "Keep aiming at the target after rotation completes", null);
                Slider lookAtSlider = new Slider(0, 0, itemWidth, 0, 10, action.lookAtSeconds, val -> {
                    action.lookAtSeconds = Math.round(val * 1000.0f) / 1000.0f;
                    lookAtWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.3fs", action.lookAtSeconds));
                    ActionManager.getInstance().save();
                });
                lookAtWrap.setControl(lookAtSlider);
                lookAtWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.3fs", action.lookAtSeconds));
                group.addChild(lookAtWrap);

                if (!action.useCoordinates) {
                    GridRow angleRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT + 15);
                    SettingWrapper yawWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Yaw", null, null);
                    Slider yawSlider = new Slider(0, 0, (itemWidth / 2) - 2, -180, 180, action.yaw, val -> {
                        action.yaw = val;
                        yawWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", action.yaw));
                        ActionManager.getInstance().save();
                    });
                    yawWrap.setControl(yawSlider);
                    yawWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", action.yaw));
                    angleRow.addChild(yawWrap, 0);

                    SettingWrapper pitchWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Pitch", null, null);
                    Slider pitchSlider = new Slider(0, 0, (itemWidth / 2) - 2, -90, 90, action.pitch, val -> {
                        action.pitch = val;
                        pitchWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", action.pitch));
                        ActionManager.getInstance().save();
                    });
                    pitchWrap.setControl(pitchSlider);
                    pitchWrap.setRightLabel(String.format(java.util.Locale.ROOT, "%.1f°", action.pitch));
                    angleRow.addChild(pitchWrap, (itemWidth / 2) + 2);
                    group.addChild(angleRow);

                    Button captureBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Capture Current Rotation", () -> {
                        if (minecraft.player != null) {
                            action.yaw = minecraft.player.getYRot();
                            action.pitch = minecraft.player.getXRot();
                            ActionManager.getInstance().save();
                            rebuildActions();
                        }
                    });
                    group.addChild(captureBtn);
                } else {
                    GridRow coordRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                    TextField xField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "X");
                    xField.setText(String.format(java.util.Locale.ROOT, "%.1f", action.targetX));
                    xField.setOnValueChange(val -> { try { action.targetX = Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(xField, 0);

                    TextField yField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Y");
                    yField.setText(String.format(java.util.Locale.ROOT, "%.1f", action.targetY));
                    yField.setOnValueChange(val -> { try { action.targetY = Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(yField, (itemWidth / 3) + 1);

                    TextField zField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Z");
                    zField.setText(String.format(java.util.Locale.ROOT, "%.1f", action.targetZ));
                    zField.setOnValueChange(val -> { try { action.targetZ = Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(zField, (itemWidth * 2 / 3) + 2);
                    group.addChild(coordRow);

                    Button captureBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Capture Looking At", () -> {
                        net.minecraft.world.phys.HitResult hr = minecraft.hitResult;
                        if (hr instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                            net.minecraft.core.BlockPos pos = bhr.getBlockPos();
                            action.targetX = pos.getX();
                            action.targetY = pos.getY();
                            action.targetZ = pos.getZ();
                            ActionManager.getInstance().save();
                            rebuildActions();
                        }
                    });
                    group.addChild(captureBtn);
                }
            }

            GridRow moveRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
            Button upBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, Theme.BUTTON_HEIGHT, "Move Up", () -> {
                if (index > 0) {
                    ConfigManager.ActionStep prev = trigger.actions.remove(index);
                    trigger.actions.add(index - 1, prev);
                    ActionManager.getInstance().save();
                    int currentScroll = actionsList.getScrollOffset();
                    rebuildActions();
                    actionsList.setScrollOffset(currentScroll);
                }
            });
            Button downBtn = new Button(0, 0, (itemWidth - Theme.PADDING) / 2, Theme.BUTTON_HEIGHT, "Move Down", () -> {
                if (index < trigger.actions.size() - 1) {
                    ConfigManager.ActionStep next = trigger.actions.remove(index);
                    trigger.actions.add(index + 1, next);
                    ActionManager.getInstance().save();
                    int currentScroll = actionsList.getScrollOffset();
                    rebuildActions();
                    actionsList.setScrollOffset(currentScroll);
                }
            });
            moveRow.addChild(upBtn, 0);
            moveRow.addChild(downBtn, (itemWidth + Theme.PADDING) / 2);
            group.addChild(moveRow);

            Button deleteBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Delete Step", () -> {
                trigger.actions.remove(index);
                ActionManager.getInstance().save();
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
        RenderHelper.drawCenteredString(graphics, font, "Editing Steps for: " + trigger.pattern, containerX + containerWidth / 2, containerY + 20, Theme.TEXT_PRIMARY);
    }
}
