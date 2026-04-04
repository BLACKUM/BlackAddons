package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import java.util.function.Consumer;

public class WaypointActionEditScreen extends BaseScreen {
    private static final int CUSTOM_INPUT_WIDTH = 90;
    private static final int ANGLE_INPUT_WIDTH = 62;
    private static final float SAFE_ACTION_TIME_SECONDS = 5.0f;
    private final Waypoint waypoint;
    private final ConfigManager.WaypointAction action;
    private boolean entryDescriptionExpanded;
    private boolean exitDescriptionExpanded;
    private boolean guiExitDescriptionExpanded;
    private boolean notificationDescriptionExpanded;
    private ToggleSwitch entryToggle;
    private ToggleSwitch exitToggle;
    private ToggleSwitch guiExitToggle;
    private ToggleSwitch notifyToggle;
    private Label titleLabel;
    private Label stepsLabel;
    private TextField titleField;
    private TextField subtitleField;
    private TextField notificationTitleField;
    private TextField notificationMessageField;
    private Dropdown notificationTypeDropdown;
    private Button testNotificationButton;
    private Button addStepButton;
    private Button backButton;
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
    public void initWidgets() {
        int fieldX = containerX + Theme.PADDING;
        int fieldWidth = containerWidth - Theme.PADDING * 2;
        int currentY = containerY + 60;

        widgets.add(new Label(fieldX, currentY, "Triggers:", Label.Style.CAPTION));
        currentY += 15;
        
        entryToggle = new ToggleSwitch(fieldX, currentY, fieldWidth, "Trigger on Entry", "Fire actions when entering radius", action.triggerOnEntry, val -> {
            action.triggerOnEntry = val;
            WaypointManager.getInstance().save();
        });
        entryToggle.setExpanded(entryDescriptionExpanded);
        entryToggle.setOnExpandChange(() -> {
            entryDescriptionExpanded = entryToggle.isExpanded();
            layoutWidgets();
        });
        widgets.add(entryToggle);
        currentY += entryToggle.getHeight() + 5;

        exitToggle = new ToggleSwitch(fieldX, currentY, fieldWidth, "Trigger on Exit", "Fire actions when leaving radius", action.triggerOnExit, val -> {
            action.triggerOnExit = val;
            WaypointManager.getInstance().save();
        });
        exitToggle.setExpanded(exitDescriptionExpanded);
        exitToggle.setOnExpandChange(() -> {
            exitDescriptionExpanded = exitToggle.isExpanded();
            layoutWidgets();
        });
        widgets.add(exitToggle);
        currentY += exitToggle.getHeight() + 5;

        guiExitToggle = new ToggleSwitch(fieldX, currentY, fieldWidth, "Trigger on GUI Exit", "Fire actions when closing any GUI while inside this waypoint", action.triggerOnGuiExit, val -> {
            action.triggerOnGuiExit = val;
            WaypointManager.getInstance().save();
        });
        guiExitToggle.setExpanded(guiExitDescriptionExpanded);
        guiExitToggle.setOnExpandChange(() -> {
            guiExitDescriptionExpanded = guiExitToggle.isExpanded();
            layoutWidgets();
        });
        widgets.add(guiExitToggle);
        currentY += guiExitToggle.getHeight() + 15;

        titleLabel = new Label(fieldX, currentY, "Title:", Label.Style.CAPTION);
        widgets.add(titleLabel);
        currentY += 15;

        titleField = new TextField(fieldX, currentY, (fieldWidth - Theme.PADDING) / 2, Theme.TEXTFIELD_HEIGHT, "Title");
        titleField.setText(action.title);
        titleField.setOnValueChange(val -> {
            action.title = val;
            WaypointManager.getInstance().save();
        });
        widgets.add(titleField);

        subtitleField = new TextField(fieldX + (fieldWidth + Theme.PADDING) / 2, currentY, (fieldWidth - Theme.PADDING) / 2, Theme.TEXTFIELD_HEIGHT, "Subtitle");
        subtitleField.setText(action.subtitle);
        subtitleField.setOnValueChange(val -> {
            action.subtitle = val;
            WaypointManager.getInstance().save();
        });
        widgets.add(subtitleField);
        currentY += 35;

        notifyToggle = new ToggleSwitch(fieldX, currentY, fieldWidth, "Show Notification",
                "Show a custom notification when triggered", action.showNotification, val -> {
            action.showNotification = val;
            WaypointManager.getInstance().save();
            rebuildLayoutPreservingScroll();
        });
        notifyToggle.setExpanded(notificationDescriptionExpanded);
        notifyToggle.setOnExpandChange(() -> {
            notificationDescriptionExpanded = notifyToggle.isExpanded();
            layoutWidgets();
        });
        widgets.add(notifyToggle);
        currentY += notifyToggle.getHeight() + 5;

        if (action.showNotification) {
            notificationTitleField = new TextField(fieldX, currentY, (fieldWidth - Theme.PADDING) / 2, Theme.TEXTFIELD_HEIGHT, "Notification Title");
            notificationTitleField.setText(action.notificationTitle != null ? action.notificationTitle : "");
            notificationTitleField.setOnValueChange(val -> {
                action.notificationTitle = val;
                WaypointManager.getInstance().save();
            });
            widgets.add(notificationTitleField);

            notificationMessageField = new TextField(fieldX + (fieldWidth + Theme.PADDING) / 2, currentY, (fieldWidth - Theme.PADDING) / 2, Theme.TEXTFIELD_HEIGHT, "Notification Message");
            notificationMessageField.setText(action.notificationMessage != null ? action.notificationMessage : "");
            notificationMessageField.setOnValueChange(val -> {
                action.notificationMessage = val;
                WaypointManager.getInstance().save();
            });
            widgets.add(notificationMessageField);
            currentY += 35;

            List<String> typeOptions = java.util.stream.Stream.of(NotificationType.values()).map(Enum::name).toList();
            notificationTypeDropdown = new Dropdown(fieldX, currentY, fieldWidth - 54, Theme.TEXTFIELD_HEIGHT, "Notification Type", typeOptions, selected -> {
                action.notificationType = NotificationType.valueOf(selected);
                WaypointManager.getInstance().save();
            });
            notificationTypeDropdown.setSelectedIndex(action.notificationType.ordinal());
            widgets.add(notificationTypeDropdown);

            testNotificationButton = new Button(fieldX + fieldWidth - 50, currentY, 50, Theme.TEXTFIELD_HEIGHT, "Test", () -> {
                NotificationManager.addNotification(
                        org.blackum.blackaddons.core.util.FormatUtils.formatColor(action.notificationTitle != null && !action.notificationTitle.isEmpty() ? action.notificationTitle : "Test Title"),
                        org.blackum.blackaddons.core.util.FormatUtils.formatColor(action.notificationMessage != null && !action.notificationMessage.isEmpty() ? action.notificationMessage : "Test Message"),
                        action.notificationType
                );
            });
            widgets.add(testNotificationButton);
            currentY += 35;
        } else {
            notificationTitleField = null;
            notificationMessageField = null;
            notificationTypeDropdown = null;
            testNotificationButton = null;
        }

        stepsLabel = new Label(fieldX, currentY, "Action Steps:", Label.Style.CAPTION);
        widgets.add(stepsLabel);
        currentY += 15;

        int listHeight = containerHeight - (currentY - containerY) - 40;
        stepsList = new ListView(fieldX, currentY, fieldWidth, listHeight);
        rebuildSteps();
        widgets.add(stepsList);

        addStepButton = new Button(fieldX, currentY + listHeight + 5, fieldWidth / 2 - 2, 20, "Add Step", () -> {
            action.actions.add(new ConfigManager.ActionStep(ConfigManager.ActionStepType.SEND_MESSAGE, 0, "", 0, 0));
            WaypointManager.getInstance().save();
            int currentScroll = stepsList.getScrollOffset();
            rebuildSteps();
            stepsList.setScrollOffset(currentScroll);
        });
        widgets.add(addStepButton);

        backButton = new Button(fieldX + fieldWidth / 2 + 2, currentY + listHeight + 5, fieldWidth / 2 - 2, 20, "Back", () -> {
            minecraft.setScreen(parent);
        });
        widgets.add(backButton);

        layoutWidgets();
    }

    private void rebuildLayoutPreservingScroll() {
        double currentScroll = scrollOffset;
        init();
        double maxPossibleScroll = Math.max(0, getContentHeight() - (containerHeight - 40));
        scrollOffset = Math.max(0, Math.min(currentScroll, maxPossibleScroll));
    }

    private void layoutWidgets() {
        if (entryToggle == null || exitToggle == null || guiExitToggle == null || notifyToggle == null
                || titleLabel == null || titleField == null || subtitleField == null
                || stepsLabel == null || stepsList == null || addStepButton == null || backButton == null) {
            return;
        }

        int fieldX = containerX + Theme.PADDING;
        int fieldWidth = containerWidth - Theme.PADDING * 2;
        int halfWidth = (fieldWidth - Theme.PADDING) / 2;
        int currentY = containerY + 75;

        entryToggle.setX(fieldX);
        entryToggle.setY(currentY);
        entryToggle.setWidth(fieldWidth);
        currentY += entryToggle.getHeight() + 5;

        exitToggle.setX(fieldX);
        exitToggle.setY(currentY);
        exitToggle.setWidth(fieldWidth);
        currentY += exitToggle.getHeight() + 5;

        guiExitToggle.setX(fieldX);
        guiExitToggle.setY(currentY);
        guiExitToggle.setWidth(fieldWidth);
        currentY += guiExitToggle.getHeight() + 15;

        titleLabel.setX(fieldX);
        titleLabel.setY(currentY);
        currentY += 15;

        titleField.setX(fieldX);
        titleField.setY(currentY);
        titleField.setWidth(halfWidth);
        subtitleField.setX(fieldX + (fieldWidth + Theme.PADDING) / 2);
        subtitleField.setY(currentY);
        subtitleField.setWidth(halfWidth);
        currentY += 35;

        notifyToggle.setX(fieldX);
        notifyToggle.setY(currentY);
        notifyToggle.setWidth(fieldWidth);
        currentY += notifyToggle.getHeight() + 5;

        if (notificationTitleField != null && notificationMessageField != null) {
            notificationTitleField.setX(fieldX);
            notificationTitleField.setY(currentY);
            notificationTitleField.setWidth(halfWidth);
            notificationMessageField.setX(fieldX + (fieldWidth + Theme.PADDING) / 2);
            notificationMessageField.setY(currentY);
            notificationMessageField.setWidth(halfWidth);
            currentY += 35;
        }

        if (notificationTypeDropdown != null && testNotificationButton != null) {
            notificationTypeDropdown.setX(fieldX);
            notificationTypeDropdown.setY(currentY);
            notificationTypeDropdown.setWidth(fieldWidth - 54);
            testNotificationButton.setX(fieldX + fieldWidth - 50);
            testNotificationButton.setY(currentY);
            currentY += 35;
        }

        stepsLabel.setX(fieldX);
        stepsLabel.setY(currentY);
        currentY += 15;

        int listHeight = Math.max(80, containerHeight - (currentY - containerY) - 40);
        stepsList.setX(fieldX);
        stepsList.setY(currentY);
        stepsList.setWidth(fieldWidth);
        stepsList.setHeight(listHeight);

        addStepButton.setX(fieldX);
        addStepButton.setY(currentY + listHeight + 5);
        addStepButton.setWidth(fieldWidth / 2 - 2);
        backButton.setX(fieldX + fieldWidth / 2 + 2);
        backButton.setY(currentY + listHeight + 5);
        backButton.setWidth(fieldWidth / 2 - 2);

        baseContentHeight = Math.max(0, (backButton.getY() + backButton.getHeight()) - containerY + 20);
    }

    @Override
    public void tick() {
        super.tick();
        layoutWidgets();
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

            step.normalizeTiming();

            SettingWrapper delayWrap = new SettingWrapper(0, 0, itemWidth, "Delay (Seconds)", "Wait before this step", null);
            GridRow delayRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
            final Slider[] delaySliderRef = new Slider[1];
            final TextField[] delayFieldRef = new TextField[1];
            TextField delayField = createNonNegativeSecondsField(step.delaySeconds, value -> {
                step.setDelaySeconds(value);
                delaySliderRef[0].setValue(Math.min(value, SAFE_ACTION_TIME_SECONDS));
                delayWrap.setRightLabel(formatSeconds(step.delaySeconds));
                updateTextField(delayFieldRef[0], step.delaySeconds);
                WaypointManager.getInstance().save();
            });
            delayFieldRef[0] = delayField;
            Slider delaySlider = new Slider(0, 0, itemWidth - CUSTOM_INPUT_WIDTH - Theme.PADDING_SMALL, 0, SAFE_ACTION_TIME_SECONDS,
                    Math.max(0.0f, Math.min(step.delaySeconds, SAFE_ACTION_TIME_SECONDS)), val -> {
                step.setDelaySeconds(roundToMillis(val));
                delayWrap.setRightLabel(formatSeconds(step.delaySeconds));
                updateTextField(delayFieldRef[0], step.delaySeconds);
                WaypointManager.getInstance().save();
            });
            delaySliderRef[0] = delaySlider;
            delayRow.addChild(delaySlider, 0);
            delayRow.addChild(delayField, itemWidth - CUSTOM_INPUT_WIDTH);
            delayWrap.setControl(delayRow);
            delayWrap.setRightLabel(formatSeconds(step.delaySeconds));
            group.addChild(delayWrap);

            if (step.type == ConfigManager.ActionStepType.USE_ITEM
                    || step.type == ConfigManager.ActionStepType.ATTACK
                    || step.type == ConfigManager.ActionStepType.PRESS_KEYBIND
                    || step.type == ConfigManager.ActionStepType.MOVE_KEYBINDS
                    || step.type == ConfigManager.ActionStepType.ALIGN) {
                SettingWrapper durationWrap = new SettingWrapper(0, 0, itemWidth, 
                        step.type == ConfigManager.ActionStepType.ALIGN ? "Timeout (Seconds)" : "Duration (Seconds)", 
                        step.type == ConfigManager.ActionStepType.ALIGN ? "Max time to wait for alignment" : "How long to hold. 0 = click.", null);
                GridRow durationRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                final Slider[] durationSliderRef = new Slider[1];
                final TextField[] durationFieldRef = new TextField[1];
                TextField durationField = createNonNegativeSecondsField(step.durationSeconds, value -> {
                    step.setDurationSeconds(value);
                    durationSliderRef[0].setValue(Math.min(value, SAFE_ACTION_TIME_SECONDS));
                    durationWrap.setRightLabel(formatSeconds(step.durationSeconds));
                    updateTextField(durationFieldRef[0], step.durationSeconds);
                    WaypointManager.getInstance().save();
                });
                durationFieldRef[0] = durationField;
                Slider durationSlider = new Slider(0, 0, itemWidth - CUSTOM_INPUT_WIDTH - Theme.PADDING_SMALL, 0, SAFE_ACTION_TIME_SECONDS,
                        Math.max(0.0f, Math.min(step.durationSeconds, SAFE_ACTION_TIME_SECONDS)), val -> {
                    step.setDurationSeconds(roundToMillis(val));
                    durationWrap.setRightLabel(formatSeconds(step.durationSeconds));
                    updateTextField(durationFieldRef[0], step.durationSeconds);
                    WaypointManager.getInstance().save();
                });
                durationSliderRef[0] = durationSlider;
                durationRow.addChild(durationSlider, 0);
                durationRow.addChild(durationField, itemWidth - CUSTOM_INPUT_WIDTH);
                durationWrap.setControl(durationRow);
                durationWrap.setRightLabel(formatSeconds(step.durationSeconds));
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
            } else if (step.type == ConfigManager.ActionStepType.MOVE_KEYBINDS) {
                step.movementKeybinds = MovementKeybindSelector.sanitizeSelection(step.movementKeybinds);
                SettingWrapper movementWrap = new SettingWrapper(0, 0, itemWidth, "Movement Keys",
                        "Toggle any movement keys to press together during this step.", null);
                MovementKeybindSelector selector = new MovementKeybindSelector(0, 0, itemWidth, step.movementKeybinds, values -> {
                    step.movementKeybinds = MovementKeybindSelector.sanitizeSelection(values);
                    movementWrap.setRightLabel(MovementKeybindSelector.summarize(step.movementKeybinds));
                    WaypointManager.getInstance().save();
                });
                movementWrap.setControl(selector);
                movementWrap.setRightLabel(MovementKeybindSelector.summarize(step.movementKeybinds));
                group.addChild(movementWrap);
            } else if (step.type == ConfigManager.ActionStepType.ROTATE) {
                GridRow topRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
                topRow.addChild(new ToggleSwitch(0, 0, (itemWidth / 2) - 2, "Insta Snap", step.instaSnap, val -> {
                    step.instaSnap = val;
                    WaypointManager.getInstance().save();
                    rebuildSteps();
                }), 0);
                topRow.addChild(new ToggleSwitch(0, 0, (itemWidth / 2) - 2, "Use Coords", step.useCoordinates, val -> {
                    step.useCoordinates = val;
                    WaypointManager.getInstance().save();
                    rebuildSteps();
                }), (itemWidth / 2) + 2);
                group.addChild(topRow);

                if (!step.instaSnap) {
                    SettingWrapper speedWrap = new SettingWrapper(0, 0, itemWidth, "Rotation Speed", "Override speed (0 = use global)", null);
                    Slider speedSlider = new Slider(0, 0, itemWidth, 0, 100, step.rotationSpeed, val -> {
                        step.rotationSpeed = val;
                        speedWrap.setRightLabel(val == 0 ? "Global" : String.format(java.util.Locale.ROOT, "%.1f", val));
                        WaypointManager.getInstance().save();
                    });
                    speedWrap.setControl(speedSlider);
                    speedWrap.setRightLabel(step.rotationSpeed == 0 ? "Global" : String.format(java.util.Locale.ROOT, "%.1f", step.rotationSpeed));
                    group.addChild(speedWrap);
                }

                SettingWrapper lookAtWrap = new SettingWrapper(0, 0, itemWidth, "Look At Time", "Keep aiming at the target after rotation completes", null);
                GridRow lookAtRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                final TextField[] lookAtFieldRef = new TextField[1];
                Slider lookAtSlider = new Slider(0, 0, itemWidth - CUSTOM_INPUT_WIDTH - Theme.PADDING_SMALL, 0, 10,
                        Math.max(0.0f, Math.min(step.lookAtSeconds, 10.0f)), val -> {
                    step.lookAtSeconds = roundToMillis(val);
                    lookAtWrap.setRightLabel(formatSeconds(step.lookAtSeconds));
                    updateTextField(lookAtFieldRef[0], step.lookAtSeconds);
                    WaypointManager.getInstance().save();
                });
                TextField lookAtField = createNonNegativeDecimalField(step.lookAtSeconds, value -> {
                    step.lookAtSeconds = value;
                    lookAtSlider.setValue(Math.min(value, 10.0f));
                    lookAtWrap.setRightLabel(formatSeconds(step.lookAtSeconds));
                    updateTextField(lookAtFieldRef[0], step.lookAtSeconds);
                    WaypointManager.getInstance().save();
                });
                lookAtFieldRef[0] = lookAtField;
                lookAtRow.addChild(lookAtSlider, 0);
                lookAtRow.addChild(lookAtField, itemWidth - CUSTOM_INPUT_WIDTH);
                lookAtWrap.setControl(lookAtRow);
                lookAtWrap.setRightLabel(formatSeconds(step.lookAtSeconds));
                group.addChild(lookAtWrap);

                if (!step.useCoordinates) {
                    GridRow angleRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT + 15);
                    SettingWrapper yawWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Yaw", null, null);
                    GridRow yawRow = new GridRow((itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT);
                    final TextField[] yawFieldRef = new TextField[1];
                    Slider yawSlider = new Slider(0, 0, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH - Theme.PADDING_SMALL, -180, 180,
                            normalizeYaw(step.yaw), val -> {
                        step.yaw = normalizeYaw(val);
                        yawWrap.setRightLabel(formatAngle(step.yaw));
                        updateAngleField(yawFieldRef[0], step.yaw);
                        WaypointManager.getInstance().save();
                    });
                    TextField yawField = createYawField(step.yaw, value -> {
                        step.yaw = value;
                        yawSlider.setValue(step.yaw);
                        yawWrap.setRightLabel(formatAngle(step.yaw));
                        updateAngleField(yawFieldRef[0], step.yaw);
                        WaypointManager.getInstance().save();
                    });
                    yawFieldRef[0] = yawField;
                    yawRow.addChild(yawSlider, 0);
                    yawRow.addChild(yawField, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH);
                    yawWrap.setControl(yawRow);
                    yawWrap.setRightLabel(formatAngle(normalizeYaw(step.yaw)));
                    angleRow.addChild(yawWrap, 0);

                    SettingWrapper pitchWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Pitch", null, null);
                    GridRow pitchRow = new GridRow((itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT);
                    final TextField[] pitchFieldRef = new TextField[1];
                    Slider pitchSlider = new Slider(0, 0, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH - Theme.PADDING_SMALL, -90, 90,
                            clampPitch(step.pitch), val -> {
                        step.pitch = clampPitch(val);
                        pitchWrap.setRightLabel(formatAngle(step.pitch));
                        updateAngleField(pitchFieldRef[0], step.pitch);
                        WaypointManager.getInstance().save();
                    });
                    TextField pitchField = createPitchField(step.pitch, value -> {
                        step.pitch = value;
                        pitchSlider.setValue(step.pitch);
                        pitchWrap.setRightLabel(formatAngle(step.pitch));
                        updateAngleField(pitchFieldRef[0], step.pitch);
                        WaypointManager.getInstance().save();
                    });
                    pitchFieldRef[0] = pitchField;
                    pitchRow.addChild(pitchSlider, 0);
                    pitchRow.addChild(pitchField, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH);
                    pitchWrap.setControl(pitchRow);
                    pitchWrap.setRightLabel(formatAngle(clampPitch(step.pitch)));
                    angleRow.addChild(pitchWrap, (itemWidth / 2) + 2);
                    group.addChild(angleRow);

                    Button captureBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Capture Current Rotation", () -> {
                        if (minecraft.player != null) {
                            step.yaw = normalizeYaw(minecraft.player.getYRot());
                            step.pitch = clampPitch(minecraft.player.getXRot());
                            WaypointManager.getInstance().save();
                            rebuildSteps();
                        }
                    });
                    group.addChild(captureBtn);
                } else {
                    GridRow coordRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                    TextField xField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "X");
                    xField.setText(formatOptionalCoord(step.targetX));
                    xField.setOnValueChange(val -> { try { step.targetX = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); WaypointManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(xField, 0);

                    TextField yField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Y");
                    yField.setText(formatOptionalCoord(step.targetY));
                    yField.setOnValueChange(val -> { try { step.targetY = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); WaypointManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(yField, (itemWidth / 3) + 1);

                    TextField zField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Z");
                    zField.setText(formatOptionalCoord(step.targetZ));
                    zField.setOnValueChange(val -> { try { step.targetZ = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); WaypointManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(zField, (itemWidth * 2 / 3) + 2);
                    group.addChild(coordRow);

                    Button captureBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Capture Looking At", () -> {
                        net.minecraft.world.phys.HitResult hr = minecraft.hitResult;
                        if (hr instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                            net.minecraft.core.BlockPos pos = bhr.getBlockPos();
                            step.targetX = pos.getX();
                            step.targetY = pos.getY();
                            step.targetZ = pos.getZ();
                            WaypointManager.getInstance().save();
                            rebuildSteps();
                        }
                    });
                    group.addChild(captureBtn);
                }
            } else if (step.type == ConfigManager.ActionStepType.ALIGN) {
                GridRow topRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
                topRow.addChild(new ToggleSwitch(0, 0, (itemWidth / 2) - 2, "Use Coords", step.useCoordinates, val -> {
                    step.useCoordinates = val;
                    WaypointManager.getInstance().save();
                    rebuildSteps();
                }), 0);
                topRow.addChild(new ToggleSwitch(0, 0, (itemWidth / 2) - 2, "Look After", step.lookAfterAlign, val -> {
                    step.lookAfterAlign = val;
                    WaypointManager.getInstance().save();
                    rebuildSteps();
                }), (itemWidth / 2) + 2);
                group.addChild(topRow);

                if (step.useCoordinates) {
                    GridRow coordRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                    TextField xField = new TextField(0, 0, (itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT, "X");
                    xField.setText(formatOptionalCoord(step.targetX));
                    xField.setOnValueChange(val -> { try { step.targetX = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); WaypointManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(xField, 0);

                    TextField zField = new TextField(0, 0, (itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT, "Z");
                    zField.setText(formatOptionalCoord(step.targetZ));
                    zField.setOnValueChange(val -> { try { step.targetZ = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); WaypointManager.getInstance().save(); } catch (Exception ignored) {} });
                    coordRow.addChild(zField, (itemWidth / 2) + 2);
                    group.addChild(coordRow);

                    GridRow captureRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
                    Button captureBtn = new Button(0, 0, (itemWidth / 2) - 2, Theme.BUTTON_HEIGHT, "Capture Looking At", () -> {
                        net.minecraft.world.phys.HitResult hr = minecraft.hitResult;
                        if (hr instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                            net.minecraft.core.BlockPos pos = bhr.getBlockPos();
                            step.targetX = pos.getX() + 0.5D;
                            step.targetZ = pos.getZ() + 0.5D;
                            WaypointManager.getInstance().save();
                            rebuildSteps();
                        }
                    });
                    captureRow.addChild(captureBtn, 0);

                    Button capturePosBtn = new Button(0, 0, (itemWidth / 2) - 2, Theme.BUTTON_HEIGHT, "Capture Current Pos", () -> {
                        if (minecraft.player != null) {
                            step.targetX = minecraft.player.getX();
                            step.targetZ = minecraft.player.getZ();
                            WaypointManager.getInstance().save();
                            rebuildSteps();
                        }
                    });
                    captureRow.addChild(capturePosBtn, (itemWidth / 2) + 2);
                    group.addChild(captureRow);
                }

                if (step.lookAfterAlign) {
                    ToggleSwitch useLookCoordsToggle = new ToggleSwitch(0, 0, itemWidth, "Look At Coordinates", "Look at specific XYZ", step.useLookAfterCoords, val -> {
                        step.useLookAfterCoords = val;
                        WaypointManager.getInstance().save();
                        rebuildSteps();
                    });
                    group.addChild(useLookCoordsToggle);

                    if (step.useLookAfterCoords) {
                        GridRow lookCoordRow = new GridRow(itemWidth, Theme.TEXTFIELD_HEIGHT);
                        TextField lxField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "X");
                        lxField.setText(formatOptionalCoord(step.alignLookAtX));
                        lxField.setOnValueChange(val -> { try { step.alignLookAtX = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); WaypointManager.getInstance().save(); } catch (Exception ignored) {} });
                        lookCoordRow.addChild(lxField, 0);

                        TextField lyField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Y");
                        lyField.setText(formatOptionalCoord(step.alignLookAtY));
                        lyField.setOnValueChange(val -> { try { step.alignLookAtY = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); WaypointManager.getInstance().save(); } catch (Exception ignored) {} });
                        lookCoordRow.addChild(lyField, (itemWidth / 3) + 1);

                        TextField lzField = new TextField(0, 0, (itemWidth / 3) - 2, Theme.TEXTFIELD_HEIGHT, "Z");
                        lzField.setText(formatOptionalCoord(step.alignLookAtZ));
                        lzField.setOnValueChange(val -> { try { step.alignLookAtZ = (val == null || val.isEmpty()) ? 0.0D : Double.parseDouble(val); WaypointManager.getInstance().save(); } catch (Exception ignored) {} });
                        lookCoordRow.addChild(lzField, (itemWidth * 2 / 3) + 2);
                        group.addChild(lookCoordRow);

                        GridRow lookCaptureRow = new GridRow(itemWidth, Theme.BUTTON_HEIGHT);
                        Button captureLookBtn = new Button(0, 0, (itemWidth / 2) - 2, Theme.BUTTON_HEIGHT, "Capture Looking At", () -> {
                            net.minecraft.world.phys.HitResult hr = minecraft.hitResult;
                            if (hr instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                                net.minecraft.core.BlockPos pos = bhr.getBlockPos();
                                step.alignLookAtX = pos.getX() + 0.5D;
                                step.alignLookAtY = pos.getY() + 0.5D;
                                step.alignLookAtZ = pos.getZ() + 0.5D;
                                WaypointManager.getInstance().save();
                                rebuildSteps();
                            }
                        });
                        lookCaptureRow.addChild(captureLookBtn, 0);

                        Button captureLookPosBtn = new Button(0, 0, (itemWidth / 2) - 2, Theme.BUTTON_HEIGHT, "Capture Current Pos", () -> {
                            if (minecraft.player != null) {
                                step.alignLookAtX = minecraft.player.getX();
                                step.alignLookAtY = minecraft.player.getY();
                                step.alignLookAtZ = minecraft.player.getZ();
                                WaypointManager.getInstance().save();
                                rebuildSteps();
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
                                normalizeYaw(step.alignPostYaw), val -> {
                            step.alignPostYaw = normalizeYaw(val);
                            yawWrap.setRightLabel(formatAngle(step.alignPostYaw));
                            updateAngleField(yawFieldRef[0], step.alignPostYaw);
                            WaypointManager.getInstance().save();
                        });
                        TextField yawField = createYawField(step.alignPostYaw, value -> {
                            step.alignPostYaw = value;
                            yawSlider.setValue(step.alignPostYaw);
                            yawWrap.setRightLabel(formatAngle(step.alignPostYaw));
                            updateAngleField(yawFieldRef[0], step.alignPostYaw);
                            WaypointManager.getInstance().save();
                        });
                        yawFieldRef[0] = yawField;
                        yawRow.addChild(yawSlider, 0);
                        yawRow.addChild(yawField, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH);
                        yawWrap.setControl(yawRow);
                        yawWrap.setRightLabel(formatAngle(normalizeYaw(step.alignPostYaw)));
                        angleRow.addChild(yawWrap, 0);

                        SettingWrapper pitchWrap = new SettingWrapper(0, 0, (itemWidth / 2) - 2, "Post Pitch", null, null);
                        GridRow pitchRow = new GridRow((itemWidth / 2) - 2, Theme.TEXTFIELD_HEIGHT);
                        final TextField[] pitchFieldRef = new TextField[1];
                        Slider pitchSlider = new Slider(0, 0, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH - Theme.PADDING_SMALL, -90, 90,
                                clampPitch(step.alignPostPitch), val -> {
                            step.alignPostPitch = clampPitch(val);
                            pitchWrap.setRightLabel(formatAngle(step.alignPostPitch));
                            updateAngleField(pitchFieldRef[0], step.alignPostPitch);
                            WaypointManager.getInstance().save();
                        });
                        TextField pitchField = createPitchField(step.alignPostPitch, value -> {
                            step.alignPostPitch = value;
                            pitchSlider.setValue(step.alignPostPitch);
                            pitchWrap.setRightLabel(formatAngle(step.alignPostPitch));
                            updateAngleField(pitchFieldRef[0], step.alignPostPitch);
                            WaypointManager.getInstance().save();
                        });
                        pitchFieldRef[0] = pitchField;
                        pitchRow.addChild(pitchSlider, 0);
                        pitchRow.addChild(pitchField, ((itemWidth / 2) - 2) - ANGLE_INPUT_WIDTH);
                        pitchWrap.setControl(pitchRow);
                        pitchWrap.setRightLabel(formatAngle(clampPitch(step.alignPostPitch)));
                        angleRow.addChild(pitchWrap, (itemWidth / 2) + 2);
                        group.addChild(angleRow);

                        Button captureRotBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Capture Current Rotation", () -> {
                            if (minecraft.player != null) {
                                step.alignPostYaw = normalizeYaw(minecraft.player.getYRot());
                                step.alignPostPitch = clampPitch(minecraft.player.getXRot());
                                WaypointManager.getInstance().save();
                                rebuildSteps();
                            }
                        });
                        group.addChild(captureRotBtn);
                    }
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
    protected void extractScrolledContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        String title = "Editing Actions for: " + (waypoint.name != null && !waypoint.name.isEmpty() ? waypoint.name : "Unnamed Waypoint");
        if (minecraft.player != null) {
            double dist = Math.sqrt(Math.pow(waypoint.x - minecraft.player.getX(), 2) +
                    Math.pow(waypoint.y - minecraft.player.getY(), 2) +
                    Math.pow(waypoint.z - minecraft.player.getZ(), 2));
            title += String.format(java.util.Locale.ROOT, " (%.1fm)", dist);
        }
        RenderHelper.drawCenteredString(graphics, font, title, containerX + containerWidth / 2, containerY + 20, Theme.TEXT_PRIMARY);
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
