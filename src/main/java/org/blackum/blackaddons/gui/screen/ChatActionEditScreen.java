package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ActionManager;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatActionEditScreen extends BaseScreen {
    private static final int CUSTOM_INPUT_WIDTH = 90;
    private static final int ANGLE_INPUT_WIDTH = 62;
    private static final float SAFE_ACTION_TIME_SECONDS = 5.0f;
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
    public void initWidgets() {
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

            action.normalizeTiming();

            SettingWrapper delayWrap = new SettingWrapper(0, 0, itemWidth, "Delay (Seconds)", "Wait before this step", null);
            GridRow delayRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
            final Slider[] delaySliderRef = new Slider[1];
            final TextField[] delayFieldRef = new TextField[1];
            TextField delayField = createNonNegativeSecondsField(action.delaySeconds, value -> {
                action.setDelaySeconds(value);
                delaySliderRef[0].setValue(Math.min(value, SAFE_ACTION_TIME_SECONDS));
                delayWrap.setRightLabel(formatSeconds(action.delaySeconds));
                updateTextField(delayFieldRef[0], action.delaySeconds);
                ActionManager.getInstance().save();
            });
            delayFieldRef[0] = delayField;
            Slider delaySlider = new Slider(0, 0, itemWidth - CUSTOM_INPUT_WIDTH - Theme.PADDING_SMALL, 0, SAFE_ACTION_TIME_SECONDS,
                    Math.max(0.0f, Math.min(action.delaySeconds, SAFE_ACTION_TIME_SECONDS)), val -> {
                action.setDelaySeconds(roundToMillis(val));
                delayWrap.setRightLabel(formatSeconds(action.delaySeconds));
                updateTextField(delayFieldRef[0], action.delaySeconds);
                ActionManager.getInstance().save();
            });
            delaySliderRef[0] = delaySlider;
            delayRow.addChild(delaySlider, 0);
            delayRow.addChild(delayField, itemWidth - CUSTOM_INPUT_WIDTH);
            delayWrap.setControl(delayRow);
            delayWrap.setRightLabel(formatSeconds(action.delaySeconds));
            group.addChild(delayWrap);

            if (action.type == ConfigManager.ActionStepType.USE_ITEM
                    || action.type == ConfigManager.ActionStepType.ATTACK
                    || action.type == ConfigManager.ActionStepType.PRESS_KEYBIND
                    || action.type == ConfigManager.ActionStepType.MOVE_KEYBINDS
                    || action.type == ConfigManager.ActionStepType.ALIGN) {
                SettingWrapper durationWrap = new SettingWrapper(0, 0, itemWidth, 
                        action.type == ConfigManager.ActionStepType.ALIGN ? "Timeout (Seconds)" : "Duration (Seconds)", 
                        action.type == ConfigManager.ActionStepType.ALIGN ? "Max time to wait for alignment" : "How long to hold. 0 = click.", null);
                GridRow durationRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                final Slider[] durationSliderRef = new Slider[1];
                final TextField[] durationFieldRef = new TextField[1];
                TextField durationField = createNonNegativeSecondsField(action.durationSeconds, value -> {
                    action.setDurationSeconds(value);
                    durationSliderRef[0].setValue(Math.min(value, SAFE_ACTION_TIME_SECONDS));
                    durationWrap.setRightLabel(formatSeconds(action.durationSeconds));
                    updateTextField(durationFieldRef[0], action.durationSeconds);
                    ActionManager.getInstance().save();
                });
                durationFieldRef[0] = durationField;
                Slider durationSlider = new Slider(0, 0, itemWidth - CUSTOM_INPUT_WIDTH - Theme.PADDING_SMALL, 0, SAFE_ACTION_TIME_SECONDS,
                        Math.max(0.0f, Math.min(action.durationSeconds, SAFE_ACTION_TIME_SECONDS)), val -> {
                    action.setDurationSeconds(roundToMillis(val));
                    durationWrap.setRightLabel(formatSeconds(action.durationSeconds));
                    updateTextField(durationFieldRef[0], action.durationSeconds);
                    ActionManager.getInstance().save();
                });
                durationSliderRef[0] = durationSlider;
                durationRow.addChild(durationSlider, 0);
                durationRow.addChild(durationField, itemWidth - CUSTOM_INPUT_WIDTH);
                durationWrap.setControl(durationRow);
                durationWrap.setRightLabel(formatSeconds(action.durationSeconds));
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
            } else if (action.type == ConfigManager.ActionStepType.MOVE_KEYBINDS) {
                action.movementKeybinds = MovementKeybindSelector.sanitizeSelection(action.movementKeybinds);
                SettingWrapper movementWrap = new SettingWrapper(0, 0, itemWidth, "Movement Keys",
                        "Toggle any movement keys to press together during this step.", null);
                MovementKeybindSelector selector = new MovementKeybindSelector(0, 0, itemWidth, action.movementKeybinds, values -> {
                    action.movementKeybinds = MovementKeybindSelector.sanitizeSelection(values);
                    movementWrap.setRightLabel(MovementKeybindSelector.summarize(action.movementKeybinds));
                    ActionManager.getInstance().save();
                });
                movementWrap.setControl(selector);
                movementWrap.setRightLabel(MovementKeybindSelector.summarize(action.movementKeybinds));
                group.addChild(movementWrap);
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
                GridRow lookAtRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                final TextField[] lookAtFieldRef = new TextField[1];
                Slider lookAtSlider = new Slider(0, 0, itemWidth - CUSTOM_INPUT_WIDTH - Theme.PADDING_SMALL, 0, 10,
                        Math.max(0.0f, Math.min(action.lookAtSeconds, 10.0f)), val -> {
                    action.lookAtSeconds = roundToMillis(val);
                    lookAtWrap.setRightLabel(formatSeconds(action.lookAtSeconds));
                    updateTextField(lookAtFieldRef[0], action.lookAtSeconds);
                    ActionManager.getInstance().save();
                });
                TextField lookAtField = createNonNegativeDecimalField(action.lookAtSeconds, value -> {
                    action.lookAtSeconds = value;
                    lookAtSlider.setValue(Math.min(value, 10.0f));
                    lookAtWrap.setRightLabel(formatSeconds(action.lookAtSeconds));
                    updateTextField(lookAtFieldRef[0], action.lookAtSeconds);
                    ActionManager.getInstance().save();
                });
                lookAtFieldRef[0] = lookAtField;
                lookAtRow.addChild(lookAtSlider, 0);
                lookAtRow.addChild(lookAtField, itemWidth - CUSTOM_INPUT_WIDTH);
                lookAtWrap.setControl(lookAtRow);
                lookAtWrap.setRightLabel(formatSeconds(action.lookAtSeconds));
                group.addChild(lookAtWrap);

                if (!action.useCoordinates) {
                    GridRow angleRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT + 15);
                    SettingWrapper yawWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Yaw", null, null);
                    GridRow yawRow = new GridRow((itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT);
                    final TextField[] yawFieldRef = new TextField[1];
                    Slider yawSlider = new Slider(0, 0, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH - Theme.PADDING_SMALL, -180, 180,
                            normalizeYaw(action.yaw), val -> {
                        action.yaw = normalizeYaw(val);
                        yawWrap.setRightLabel(formatAngle(action.yaw));
                        updateAngleField(yawFieldRef[0], action.yaw);
                        ActionManager.getInstance().save();
                    });
                    TextField yawField = createYawField(action.yaw, value -> {
                        action.yaw = value;
                        yawSlider.setValue(action.yaw);
                        yawWrap.setRightLabel(formatAngle(action.yaw));
                        updateAngleField(yawFieldRef[0], action.yaw);
                        ActionManager.getInstance().save();
                    });
                    yawFieldRef[0] = yawField;
                    yawRow.addChild(yawSlider, 0);
                    yawRow.addChild(yawField, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH);
                    yawWrap.setControl(yawRow);
                    yawWrap.setRightLabel(formatAngle(normalizeYaw(action.yaw)));
                    angleRow.addChild(yawWrap, 0);

                    SettingWrapper pitchWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Pitch", null, null);
                    GridRow pitchRow = new GridRow((itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT);
                    final TextField[] pitchFieldRef = new TextField[1];
                    Slider pitchSlider = new Slider(0, 0, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH - Theme.PADDING_SMALL, -90, 90,
                            clampPitch(action.pitch), val -> {
                        action.pitch = clampPitch(val);
                        pitchWrap.setRightLabel(formatAngle(action.pitch));
                        updateAngleField(pitchFieldRef[0], action.pitch);
                        ActionManager.getInstance().save();
                    });
                    TextField pitchField = createPitchField(action.pitch, value -> {
                        action.pitch = value;
                        pitchSlider.setValue(action.pitch);
                        pitchWrap.setRightLabel(formatAngle(action.pitch));
                        updateAngleField(pitchFieldRef[0], action.pitch);
                        ActionManager.getInstance().save();
                    });
                    pitchFieldRef[0] = pitchField;
                    pitchRow.addChild(pitchSlider, 0);
                    pitchRow.addChild(pitchField, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH);
                    pitchWrap.setControl(pitchRow);
                    pitchWrap.setRightLabel(formatAngle(clampPitch(action.pitch)));
                    angleRow.addChild(pitchWrap, (itemWidth / 2) + 2);
                    group.addChild(angleRow);

                    Button captureBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Capture Current Rotation", () -> {
                        if (minecraft.player != null) {
                            action.yaw = normalizeYaw(minecraft.player.getYRot());
                            action.pitch = clampPitch(minecraft.player.getXRot());
                            ActionManager.getInstance().save();
                            rebuildActions();
                        }
                    });
                    group.addChild(captureBtn);
                } else {
                    GridRow coordRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                    TextField xField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "X");
                    xField.setText(formatOptionalCoord(action.targetX));
                    xField.setOnValueChange(val -> { try { action.targetX = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(xField, 0);

                    TextField yField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Y");
                    yField.setText(formatOptionalCoord(action.targetY));
                    yField.setOnValueChange(val -> { try { action.targetY = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(yField, (itemWidth / 3) + 1);

                    TextField zField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Z");
                    zField.setText(formatOptionalCoord(action.targetZ));
                    zField.setOnValueChange(val -> { try { action.targetZ = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
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
            } else if (action.type == ConfigManager.ActionStepType.ALIGN) {
                GridRow topRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
                topRow.addChild(new ToggleSwitch(0, 0, (itemWidth / 2) - 2, "Use Coords", action.useCoordinates, val -> {
                    action.useCoordinates = val;
                    ActionManager.getInstance().save();
                    rebuildActions();
                }), 0);
                topRow.addChild(new ToggleSwitch(0, 0, (itemWidth / 2) - 2, "Look After", action.lookAfterAlign, val -> {
                    action.lookAfterAlign = val;
                    ActionManager.getInstance().save();
                    rebuildActions();
                }), (itemWidth / 2) + 2);
                group.addChild(topRow);

                if (action.useCoordinates) {
                    GridRow coordRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                    TextField xField = new TextField(0, 0, (itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT, "X");
                    xField.setText(formatOptionalCoord(action.targetX));
                    xField.setOnValueChange(val -> { try { action.targetX = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(xField, 0);

                    TextField zField = new TextField(0, 0, (itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT, "Z");
                    zField.setText(formatOptionalCoord(action.targetZ));
                    zField.setOnValueChange(val -> { try { action.targetZ = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(zField, (itemWidth / 2) + 2);
                    group.addChild(coordRow);

                    GridRow captureRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
                    Button captureBtn = new Button(0, 0, (itemWidth / 2) - 2, Theme.BUTTON_HEIGHT, "Capture Looking At", () -> {
                        net.minecraft.world.phys.HitResult hr = minecraft.hitResult;
                        if (hr instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                            net.minecraft.core.BlockPos pos = bhr.getBlockPos();
                            action.targetX = pos.getX() + 0.5D;
                            action.targetZ = pos.getZ() + 0.5D;
                            ActionManager.getInstance().save();
                            rebuildActions();
                        }
                    });
                    captureRow.addChild(captureBtn, 0);

                    Button capturePosBtn = new Button(0, 0, (itemWidth / 2) - 2, Theme.BUTTON_HEIGHT, "Capture Current Pos", () -> {
                        if (minecraft.player != null) {
                            action.targetX = minecraft.player.getX();
                            action.targetZ = minecraft.player.getZ();
                            ActionManager.getInstance().save();
                            rebuildActions();
                        }
                    });
                    captureRow.addChild(capturePosBtn, (itemWidth / 2) + 2);
                    group.addChild(captureRow);
                }

                if (action.lookAfterAlign) {
                    ToggleSwitch useLookCoordsToggle = new ToggleSwitch(0, 0, itemWidth, "Look At Coordinates", "Look at specific XYZ", action.useLookAfterCoords, val -> {
                        action.useLookAfterCoords = val;
                        ActionManager.getInstance().save();
                        rebuildActions();
                    });
                    group.addChild(useLookCoordsToggle);

                    if (action.useLookAfterCoords) {
                        GridRow lookCoordRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                        TextField lxField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "X");
                        lxField.setText(formatOptionalCoord(action.alignLookAtX));
                        lxField.setOnValueChange(val -> { try { action.alignLookAtX = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                        lookCoordRow.addChild(lxField, 0);

                        TextField lyField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Y");
                        lyField.setText(formatOptionalCoord(action.alignLookAtY));
                        lyField.setOnValueChange(val -> { try { action.alignLookAtY = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                        lookCoordRow.addChild(lyField, (itemWidth / 3) + 1);

                        TextField lzField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Z");
                        lzField.setText(formatOptionalCoord(action.alignLookAtZ));
                        lzField.setOnValueChange(val -> { try { action.alignLookAtZ = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); ActionManager.getInstance().save(); } catch (Exception ignored) {} });
                        lookCoordRow.addChild(lzField, (itemWidth * 2 / 3) + 2);
                        group.addChild(lookCoordRow);

                        GridRow lookCaptureRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
                        Button captureLookBtn = new Button(0, 0, (itemWidth / 2) - 2, Theme.BUTTON_HEIGHT, "Capture Looking At", () -> {
                            net.minecraft.world.phys.HitResult hr = minecraft.hitResult;
                            if (hr instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                                net.minecraft.core.BlockPos pos = bhr.getBlockPos();
                                action.alignLookAtX = pos.getX() + 0.5D;
                                action.alignLookAtY = pos.getY() + 0.5D;
                                action.alignLookAtZ = pos.getZ() + 0.5D;
                                ActionManager.getInstance().save();
                                rebuildActions();
                            }
                        });
                        lookCaptureRow.addChild(captureLookBtn, 0);

                        Button captureLookPosBtn = new Button(0, 0, (itemWidth / 2) - 2, Theme.BUTTON_HEIGHT, "Capture Current Pos", () -> {
                            if (minecraft.player != null) {
                                action.alignLookAtX = minecraft.player.getX();
                                action.alignLookAtY = minecraft.player.getY();
                                action.alignLookAtZ = minecraft.player.getZ();
                                ActionManager.getInstance().save();
                                rebuildActions();
                            }
                        });
                        lookCaptureRow.addChild(captureLookPosBtn, (itemWidth / 2) + 2);
                        group.addChild(lookCaptureRow);
                    } else {
                        GridRow angleRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT + 15);
                        SettingWrapper yawWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Post Yaw", null, null);
                        GridRow yawRow = new GridRow((itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT);
                        final TextField[] yawFieldRef = new TextField[1];
                        Slider yawSlider = new Slider(0, 0, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH - Theme.PADDING_SMALL, -180, 180,
                                normalizeYaw(action.alignPostYaw), val -> {
                            action.alignPostYaw = normalizeYaw(val);
                            yawWrap.setRightLabel(formatAngle(action.alignPostYaw));
                            updateAngleField(yawFieldRef[0], action.alignPostYaw);
                            ActionManager.getInstance().save();
                        });
                        TextField yawField = createYawField(action.alignPostYaw, value -> {
                            action.alignPostYaw = value;
                            yawSlider.setValue(action.alignPostYaw);
                            yawWrap.setRightLabel(formatAngle(action.alignPostYaw));
                            updateAngleField(yawFieldRef[0], action.alignPostYaw);
                            ActionManager.getInstance().save();
                        });
                        yawFieldRef[0] = yawField;
                        yawRow.addChild(yawSlider, 0);
                        yawRow.addChild(yawField, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH);
                        yawWrap.setControl(yawRow);
                        yawWrap.setRightLabel(formatAngle(normalizeYaw(action.alignPostYaw)));
                        angleRow.addChild(yawWrap, 0);

                        SettingWrapper pitchWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Post Pitch", null, null);
                        GridRow pitchRow = new GridRow((itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT);
                        final TextField[] pitchFieldRef = new TextField[1];
                        Slider pitchSlider = new Slider(0, 0, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH - Theme.PADDING_SMALL, -90, 90,
                                clampPitch(action.alignPostPitch), val -> {
                            action.alignPostPitch = clampPitch(val);
                            pitchWrap.setRightLabel(formatAngle(action.alignPostPitch));
                            updateAngleField(pitchFieldRef[0], action.alignPostPitch);
                            ActionManager.getInstance().save();
                        });
                        TextField pitchField = createPitchField(action.alignPostPitch, value -> {
                            action.alignPostPitch = value;
                            pitchSlider.setValue(action.alignPostPitch);
                            pitchWrap.setRightLabel(formatAngle(action.alignPostPitch));
                            updateAngleField(pitchFieldRef[0], action.alignPostPitch);
                            ActionManager.getInstance().save();
                        });
                        pitchFieldRef[0] = pitchField;
                        pitchRow.addChild(pitchSlider, 0);
                        pitchRow.addChild(pitchField, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH);
                        pitchWrap.setControl(pitchRow);
                        pitchWrap.setRightLabel(formatAngle(clampPitch(action.alignPostPitch)));
                        angleRow.addChild(pitchWrap, (itemWidth / 2) + 2);
                        group.addChild(angleRow);

                        Button captureRotBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Capture Current Rotation", () -> {
                            if (minecraft.player != null) {
                                action.alignPostYaw = normalizeYaw(minecraft.player.getYRot());
                                action.alignPostPitch = clampPitch(minecraft.player.getXRot());
                                ActionManager.getInstance().save();
                                rebuildActions();
                            }
                        });
                        group.addChild(captureRotBtn);
                    }
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
    protected void extractScrolledContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        RenderHelper.drawCenteredString(graphics, font, "Editing Steps for: " + trigger.pattern, containerX + containerWidth / 2, containerY + 20, Theme.TEXT_PRIMARY);
    }

    private String formatOptionalCoord(double value) {
        if (value == 0.0) return "";
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String formatSeconds(float seconds) {
        return String.format(java.util.Locale.ROOT, "%.3fs", seconds);
    }

    private static float roundToMillis(float value) {
        return Math.round(Math.max(0.0f, value) * 1000.0f) / 1000.0f;
    }

    private static float roundAngle(float value) {
        return Math.round(value * 10.0f) / 10.0f;
    }

    private static float normalizeYaw(float yaw) {
        yaw %= 360.0f;
        if (yaw > 180.0f) {
            yaw -= 360.0f;
        }
        if (yaw < -180.0f) {
            yaw += 360.0f;
        }
        return roundAngle(yaw);
    }

    private static float clampPitch(float pitch) {
        return roundAngle(Math.max(-90.0f, Math.min(90.0f, pitch)));
    }

    private static String formatAngle(float angle) {
        return String.format(java.util.Locale.ROOT, "%.1f°", angle);
    }

    private void updateTextField(TextField field, float value) {
        String formatted = String.format(java.util.Locale.ROOT, "%.3f", roundToMillis(value));
        if (!formatted.equals(field.getText())) {
            field.setText(formatted);
        }
    }

    private void updateAngleField(TextField field, float value) {
        String formatted = String.format(java.util.Locale.ROOT, "%.1f", roundAngle(value));
        if (!formatted.equals(field.getText())) {
            field.setText(formatted);
        }
    }

    private TextField createNonNegativeSecondsField(float initialValue, Consumer<Float> onValidValue) {
        TextField field = createNonNegativeDecimalField(initialValue, onValidValue);
        updateTextField(field, initialValue);
        return field;
    }

    private TextField createNonNegativeDecimalField(float initialValue, Consumer<Float> onValidValue) {
        TextField field = new TextField(0, 0, CUSTOM_INPUT_WIDTH, Theme.TEXTFIELD_HEIGHT, "Custom");
        field.setMaxLength(10);
        field.setCharFilter(c -> Character.isDigit(c) || c == '.');
        updateTextField(field, initialValue);
        field.setOnValueChange(val -> {
            if (val == null || val.isEmpty() || ".".equals(val)) {
                return;
            }
            try {
                onValidValue.accept(roundToMillis(Float.parseFloat(val)));
            } catch (NumberFormatException ignored) {
            }
        });
        return field;
    }

    private TextField createYawField(float initialValue, Consumer<Float> onValidValue) {
        return createAngleField(initialValue, true, onValidValue);
    }

    private TextField createPitchField(float initialValue, Consumer<Float> onValidValue) {
        return createAngleField(initialValue, false, onValidValue);
    }

    private TextField createAngleField(float initialValue, boolean yaw, Consumer<Float> onValidValue) {
        TextField field = new TextField(0, 0, ANGLE_INPUT_WIDTH, Theme.TEXTFIELD_HEIGHT, yaw ? "Yaw" : "Pitch");
        field.setMaxLength(10);
        field.setCharFilter(c -> Character.isDigit(c) || c == '.' || c == '-');
        updateAngleField(field, yaw ? normalizeYaw(initialValue) : clampPitch(initialValue));
        field.setOnValueChange(val -> {
            if (val == null || val.isEmpty() || ".".equals(val) || "-".equals(val) || "-.".equals(val)) {
                return;
            }
            try {
                float parsed = Float.parseFloat(val.replace(',', '.'));
                onValidValue.accept(yaw ? normalizeYaw(parsed) : clampPitch(parsed));
            } catch (NumberFormatException ignored) {
            }
        });
        return field;
    }
}
