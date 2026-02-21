package org.blackum.blackaddons.gui.screen.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.ArrayList;

public class SoundAlertsTabController extends SimpleTabController {

    public SoundAlertsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int contentX = tab.getParent().getContentX();
        int contentY = tab.getParent().getContentY();
        int width = tab.getParent().getContentWidth() - 20;
        int height = tab.getParent().getContentHeight() - Theme.PADDING_MEDIUM * 2;
        int itemWidth = width - 16;

        ListView listView = new ListView(contentX, contentY + Theme.PADDING_MEDIUM, width, height);
        tab.addWidget(listView);

        listView.addItem(new Label(0, 0, "Chat Triggers", Label.Style.TITLE));

        Label description = new Label(0, 0, "Trigger custom sounds when chat matches text or regex.",
                Label.Style.BODY);
        listView.addItem(description);

        for (int i = 0; i < ConfigManager.data.chatSoundAlerts.size(); i++) {
            final int index = i;
            ConfigManager.SoundAlert alert = ConfigManager.data.chatSoundAlerts.get(i);

            String headerTitle = alert.title != null && !alert.title.isEmpty()
                    ? alert.title.replace("&", "§")
                    : "Alert " + (i + 1);

            SectionHeader header = new SectionHeader(itemWidth, headerTitle);
            ExpandableGroup group = new ExpandableGroup(0, 0, itemWidth, header, !alert.collapsed);
            List<Widget> alertWidgets = new ArrayList<>();

            Runnable onToggle = () -> {
                alert.collapsed = !alert.collapsed;
                group.setExpanded(!alert.collapsed);
                ConfigManager.save();
            };

            header.setCollapsed(alert.collapsed);
            header.setToggleCallback(onToggle);

            listView.addItem(group);

            TextField patternField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Pattern...");
            patternField.setText(alert.pattern != null ? alert.pattern : "");
            patternField.setMaxLength(256);
            patternField.setOnValueChange(val -> {
                alert.pattern = val;
                ConfigManager.save();
            });
            alertWidgets.add(new SettingWrapper(0, 0, itemWidth, "Pattern",
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
            soundField.setText(alert.soundId != null ? alert.soundId : "");
            soundField.setMaxLength(128);
            soundField.setOnValueChange(val -> {
                alert.soundId = val;
                ConfigManager.save();
            });
            row.addChild(soundField, 0);

            Button testBtn = new Button(0, 0, 50, Theme.TEXTFIELD_HEIGHT,
                    "Test", () -> {
                        try {
                            ResourceLocation location = ResourceLocation.tryParse(alert.soundId);
                            if (location == null)
                                location = ResourceLocation.fromNamespaceAndPath("minecraft", alert.soundId);
                            SoundEvent event = SoundEvent.createVariableRangeEvent(location);
                            Minecraft client = Minecraft.getInstance();
                            client.getSoundManager().play(SimpleSoundInstance.forUI(event, alert.pitch, alert.volume));

                            if (alert.title != null && !alert.title.isEmpty() && client.gui != null) {
                                client.gui.setTimes(10, alert.durationSeconds * 20, 20);
                                client.gui.setTitle(net.minecraft.network.chat.Component
                                        .literal(org.blackum.blackaddons.core.util.FormatUtils
                                                .formatColor(alert.title)));
                                if (alert.subtitle != null && !alert.subtitle.isEmpty()) {
                                    client.gui.setSubtitle(
                                            net.minecraft.network.chat.Component
                                                    .literal(org.blackum.blackaddons.core.util.FormatUtils
                                                            .formatColor(alert.subtitle)));
                                }
                            }
                        } catch (Exception e) {
                        }
                    });
            row.addChild(testBtn, childrenWidth - 50);
            alertWidgets.add(
                    new SettingWrapper(0, 0, itemWidth, "Sound ID", "The Minecraft sound to play when triggered", row));

            SettingWrapper[] volWrap = new SettingWrapper[1];
            Slider volumeSlider = new Slider(0, 0, itemWidth, 0.0f, 1.0f, alert.volume, val -> {
                alert.volume = val;
                if (volWrap[0] != null)
                    volWrap[0].setRightLabel(String.format("%.2f", val));
                ConfigManager.save();
            });
            volWrap[0] = new SettingWrapper(0, 0, itemWidth, "Volume", "Sets the volume of the sound effect",
                    volumeSlider);
            volWrap[0].setRightLabel(String.format("%.2f", alert.volume));
            alertWidgets.add(volWrap[0]);

            SettingWrapper[] pitchWrap = new SettingWrapper[1];
            Slider pitchSlider = new Slider(0, 0, itemWidth, 0.1f, 2.0f, alert.pitch, val -> {
                alert.pitch = val;
                if (pitchWrap[0] != null)
                    pitchWrap[0].setRightLabel(String.format("%.2fx", val));
                ConfigManager.save();
            });
            pitchWrap[0] = new SettingWrapper(0, 0, itemWidth, "Pitch", "Sets the pitch of the sound effect",
                    pitchSlider);
            pitchWrap[0].setRightLabel(String.format("%.2fx", alert.pitch));
            alertWidgets.add(pitchWrap[0]);

            TextField titleField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Title...");
            titleField.setText(alert.title != null ? alert.title : "");
            titleField.setMaxLength(128);
            titleField.setOnValueChange(val -> {
                alert.title = val;
                ConfigManager.save();
            });
            alertWidgets.add(new SettingWrapper(0, 0, itemWidth, "Title",
                    "Optional. Large text to show on screen. Supports & colors.", titleField));

            TextField subtitleField = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "Subtitle...");
            subtitleField.setText(alert.subtitle != null ? alert.subtitle : "");
            subtitleField.setMaxLength(128);
            subtitleField.setOnValueChange(val -> {
                alert.subtitle = val;
                ConfigManager.save();
            });
            alertWidgets.add(new SettingWrapper(0, 0, itemWidth, "Subtitle",
                    "Optional. Smaller text to show below title. Supports & colors.", subtitleField));

            SettingWrapper[] durWrap = new SettingWrapper[1];
            Slider durationSlider = new Slider(0, 0, itemWidth, 0.0f, 10.0f, alert.durationSeconds, val -> {
                alert.durationSeconds = Math.round(val);
                if (durWrap[0] != null)
                    durWrap[0].setRightLabel(alert.durationSeconds + "s");
                ConfigManager.save();
            });
            durWrap[0] = new SettingWrapper(0, 0, itemWidth, "Display Duration",
                    "How long the title and subtitle stay on screen", durationSlider);
            durWrap[0].setRightLabel(alert.durationSeconds + "s");
            alertWidgets.add(durWrap[0]);

            ToggleSwitch regexToggle = new ToggleSwitch(0, 0, itemWidth, "Is Regex",
                    "Evaluate pattern as regular expression", alert.isRegex, val -> {
                        alert.isRegex = val;
                        ConfigManager.save();
                    });
            alertWidgets.add(regexToggle);

            ToggleSwitch enabledToggle = new ToggleSwitch(0, 0, itemWidth, "Enabled",
                    "Enable this sound alert", alert.enabled, val -> {
                        alert.enabled = val;
                        ConfigManager.save();
                    });
            alertWidgets.add(enabledToggle);

            Button deleteBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Delete Alert",
                    () -> {
                        ConfigManager.data.chatSoundAlerts.remove(index);
                        ConfigManager.save();
                        screen.init();
                    });
            alertWidgets.add(deleteBtn);

            for (Widget w : alertWidgets) {
                group.addChild(w);
            }
        }

        Button addBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Add New Alert",
                () -> {
                    ConfigManager.data.chatSoundAlerts
                            .add(new ConfigManager.SoundAlert("", true, "entity.experience_orb.pickup", 1.0f, 1.0f,
                                    true, "",
                                    "", 2));
                    ConfigManager.save();
                    screen.init();
                });
        listView.addItem(addBtn);
    }
}
