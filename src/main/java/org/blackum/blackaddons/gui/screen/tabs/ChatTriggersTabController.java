package org.blackum.blackaddons.gui.screen.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.TriggerActionEditScreen;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.ArrayList;

public class ChatTriggersTabController extends SimpleTabController {
    private static int lastScrollOffset = 0;
    private ListView listView;

    public ChatTriggersTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int contentX = tab.getParent().getContentX();
        int contentY = tab.getParent().getContentY();
        int width = tab.getParent().getContentWidth() - 20;
        int height = tab.getParent().getContentHeight() - Theme.PADDING_MEDIUM * 2;
        int itemWidth = width - 16;

        listView = new ListView(contentX, contentY + Theme.PADDING_MEDIUM, width, height);
        listView.setScrollOffset(lastScrollOffset);
        tab.addWidget(listView);

        listView.addItem(new Label(0, 0, "Chat Triggers", Label.Style.TITLE));

        Label description = new Label(0, 0, "Trigger custom sounds and actions when chat matches text or regex.",
                Label.Style.BODY);
        listView.addItem(description);

        for (int i = 0; i < ConfigManager.data.chatTriggers.size(); i++) {
            final int index = i;
            ConfigManager.ChatTrigger trigger = ConfigManager.data.chatTriggers.get(i);

            String headerTitle = trigger.title != null && !trigger.title.isEmpty()
                    ? trigger.title.replace("&", "§")
                    : "Trigger " + (i + 1);

            SectionHeader header = new SectionHeader(itemWidth, headerTitle);
            ExpandableGroup group = new ExpandableGroup(0, 0, itemWidth, header, !trigger.collapsed);
            List<Widget> triggerWidgets = new ArrayList<>();

            Runnable onToggle = () -> {
                trigger.collapsed = !trigger.collapsed;
                group.setExpanded(!trigger.collapsed);
                ConfigManager.save();
            };

            header.setCollapsed(trigger.collapsed);
            header.setToggleCallback(onToggle);

            listView.addItem(group);

            TextField patternField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Pattern...");
            patternField.setText(trigger.pattern != null ? trigger.pattern : "");
            patternField.setMaxLength(256);
            patternField.setOnValueChange(val -> {
                trigger.pattern = val;
                ConfigManager.save();
            });
            triggerWidgets.add(new SettingWrapper(0, 0, itemWidth, "Pattern",
                    "The text or regex pattern to trigger the alert", patternField));

            int childrenWidth = itemWidth;
            GridRow row = new GridRow(childrenWidth, Theme.TEXTFIELD_HEIGHT);

            AutocompleteTextField soundField = new AutocompleteTextField(0, 0, childrenWidth - 54,
                    Theme.TEXTFIELD_HEIGHT,
                    "e.g., entity.cat.ambient", () -> {
                        List<String> sounds = new ArrayList<>();
                        for (ResourceLocation id : BuiltInRegistries.SOUND_EVENT.keySet()) {
                            sounds.add(id.toString());
                        }
                        return sounds;
                    });
            soundField.setText(trigger.soundId != null ? trigger.soundId : "");
            soundField.setMaxLength(128);
            soundField.setOnValueChange(val -> {
                trigger.soundId = val;
                ConfigManager.save();
            });
            row.addChild(soundField, 0);

            Button testBtn = new Button(0, 0, 50, Theme.TEXTFIELD_HEIGHT,
                    "Test", () -> {
                        try {
                            if (trigger.soundId != null && !trigger.soundId.isEmpty()) {
                                ResourceLocation location = ResourceLocation.tryParse(trigger.soundId);
                                if (location == null)
                                    location = ResourceLocation.fromNamespaceAndPath("minecraft", trigger.soundId);
                                SoundEvent event = SoundEvent.createVariableRangeEvent(location);
                                Minecraft client = Minecraft.getInstance();
                                client.getSoundManager().play(SimpleSoundInstance.forUI(event, trigger.pitch, trigger.volume));
                            }

                            Minecraft client = Minecraft.getInstance();
                            if (trigger.durationSeconds > 0 && trigger.title != null && !trigger.title.isEmpty() && client.gui != null) {
                                client.gui.setTimes(10, (int) (trigger.durationSeconds * 20), 20);
                                client.gui.setTitle(net.minecraft.network.chat.Component
                                        .literal(org.blackum.blackaddons.core.util.FormatUtils
                                                .formatColor(trigger.title)));
                                if (trigger.subtitle != null && !trigger.subtitle.isEmpty()) {
                                    client.gui.setSubtitle(
                                            net.minecraft.network.chat.Component
                                                    .literal(org.blackum.blackaddons.core.util.FormatUtils
                                                            .formatColor(trigger.subtitle)));
                                }
                            }
                        } catch (Exception e) {
                        }
                    });
            row.addChild(testBtn, childrenWidth - 50);
            triggerWidgets.add(
                    new SettingWrapper(0, 0, itemWidth, "Sound ID", "The Minecraft sound to play when triggered", row));

            SettingWrapper volWrap = new SettingWrapper(0, 0, itemWidth, "Volume", "Sets the volume of the sound effect", null);
            Slider volumeSlider = new Slider(0, 0, itemWidth, 0.0f, 1.0f, trigger.volume, val -> {
                trigger.volume = val;
                volWrap.setRightLabel(String.format("%.2f", val));
                ConfigManager.save();
            });
            volWrap.setControl(volumeSlider);
            volWrap.setRightLabel(String.format("%.2f", trigger.volume));
            triggerWidgets.add(volWrap);

            SettingWrapper pitchWrap = new SettingWrapper(0, 0, itemWidth, "Pitch", "Sets the pitch of the sound effect", null);
            Slider pitchSlider = new Slider(0, 0, itemWidth, 0.1f, 2.0f, trigger.pitch, val -> {
                trigger.pitch = val;
                pitchWrap.setRightLabel(String.format("%.2fx", val));
                ConfigManager.save();
            });
            pitchWrap.setControl(pitchSlider);
            pitchWrap.setRightLabel(String.format("%.2fx", trigger.pitch));
            triggerWidgets.add(pitchWrap);

            TextField titleField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Title...");
            titleField.setText(trigger.title != null ? trigger.title : "");
            titleField.setMaxLength(128);
            titleField.setOnValueChange(val -> {
                trigger.title = val;
                ConfigManager.save();
            });
            triggerWidgets.add(new SettingWrapper(0, 0, itemWidth, "Title",
                    "Optional. Large text to show on screen. Supports & colors.", titleField));

            TextField subtitleField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Subtitle...");
            subtitleField.setText(trigger.subtitle != null ? trigger.subtitle : "");
            subtitleField.setMaxLength(128);
            subtitleField.setOnValueChange(val -> {
                trigger.subtitle = val;
                ConfigManager.save();
            });
            triggerWidgets.add(new SettingWrapper(0, 0, itemWidth, "Subtitle",
                    "Optional. Smaller text to show below title. Supports & colors.", subtitleField));

            SettingWrapper durWrap = new SettingWrapper(0, 0, itemWidth, "Display Duration",
                    "How long the title and subtitle stay on screen", null);
            Slider durationSlider = new Slider(0, 0, itemWidth, 0.0f, 10.0f, trigger.durationSeconds, val -> {
                trigger.durationSeconds = val;
                durWrap.setRightLabel(String.format("%.2f seconds", val));
                ConfigManager.save();
            });
            durWrap.setControl(durationSlider);
            durWrap.setRightLabel(String.format("%.2f seconds", trigger.durationSeconds));
            triggerWidgets.add(durWrap);

            ToggleSwitch regexToggle = new ToggleSwitch(0, 0, itemWidth, "Is Regex",
                    "Evaluate pattern as regular expression", trigger.isRegex, val -> {
                        trigger.isRegex = val;
                        ConfigManager.save();
                    });
            triggerWidgets.add(regexToggle);

            ToggleSwitch enabledToggle = new ToggleSwitch(0, 0, itemWidth, "Enabled",
                    "Enable this chat trigger", trigger.enabled, val -> {
                        trigger.enabled = val;
                        ConfigManager.save();
                    });
            triggerWidgets.add(enabledToggle);

            Button editActionsBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Edit Trigger Actions", () -> {
                Minecraft.getInstance().setScreen(new TriggerActionEditScreen(screen, trigger));
            });
            triggerWidgets.add(editActionsBtn);

            Button deleteBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Delete Trigger",
                    () -> {
                        ConfigManager.data.chatTriggers.remove(index);
                        ConfigManager.save();
                        screen.init();
                    });
            triggerWidgets.add(deleteBtn);

            for (Widget w : triggerWidgets) {
                group.addChild(w);
            }
        }

        Button addBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Add New Trigger",
                () -> {
                    ConfigManager.data.chatTriggers
                            .add(new ConfigManager.ChatTrigger("", true, "entity.experience_orb.pickup", 1.0f, 1.0f,
                                    true, "",
                                    "", 2.0f));
                    ConfigManager.save();
                    screen.init();
                });
        listView.addItem(addBtn);
    }

    public void onSelected() {
        if (listView != null) {
            listView.setScrollOffset(lastScrollOffset);
        }
    }

    public void tick() {
        if (listView != null) {
            lastScrollOffset = listView.getScrollOffset();
        }
    }
}
