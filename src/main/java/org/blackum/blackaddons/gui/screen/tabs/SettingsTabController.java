package org.blackum.blackaddons.gui.screen.tabs;

import java.util.List;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;

public class SettingsTabController extends SimpleTabController {

    public SettingsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab settingsTab) {
        int contentX = settingsTab.getParent().getContentX();
        int contentY = settingsTab.getParent().getContentY();
        int width = settingsTab.getParent().getContentWidth() - 20;
        int height = settingsTab.getParent().getContentHeight() - Theme.PADDING_MEDIUM * 2;

        ListView listView = new ListView(contentX, contentY + Theme.PADDING_MEDIUM, width, height);
        settingsTab.addWidget(listView);

        listView.addItem(new Label(0, 0, "General Settings", Label.Style.TITLE));

        listView.addItem(new Label(0, 0, "Party Finder", Label.Style.TITLE));

        ToggleSwitch pfAutoInviteToggle = new ToggleSwitch(0, 0, width,
                "Auto-Invite Join Requests",
                "Automatically invite players who send a join request",
                ConfigManager.data.partyFinderAutoInvite, (val) -> {
                    ConfigManager.data.partyFinderAutoInvite = val;
                    ConfigManager.save();
                });
        listView.addItem(pfAutoInviteToggle);

        ToggleSwitch pfAutoAcceptToggle = new ToggleSwitch(0, 0, width,
                "Auto-Accept Party Invites",
                "Automatically accept party invites from join requests",
                ConfigManager.data.partyFinderAutoAcceptInvite, (val) -> {
                    ConfigManager.data.partyFinderAutoAcceptInvite = val;
                    ConfigManager.save();
                });
        listView.addItem(pfAutoAcceptToggle);

        ToggleSwitch pfShowStatsJoinToggle = new ToggleSwitch(0, 0, width,
                "Show Stats on Join",
                "Show player stats in chat when they join your dungeon group",
                ConfigManager.data.partyFinderShowStatsOnJoin, (val) -> {
                    ConfigManager.data.partyFinderShowStatsOnJoin = val;
                    ConfigManager.save();
                });
        listView.addItem(pfShowStatsJoinToggle);

        ToggleSwitch pfShowStatsReqToggle = new ToggleSwitch(0, 0, width,
                "Show Stats on Request",
                "Show player stats in chat when you receive a join request",
                ConfigManager.data.partyFinderShowStatsOnRequest, (val) -> {
                    ConfigManager.data.partyFinderShowStatsOnRequest = val;
                    ConfigManager.save();
                });
        listView.addItem(pfShowStatsReqToggle);

        listView.addItem(new Label(0, 0, "IRC Chat", Label.Style.TITLE));

        ToggleSwitch ircEnabledToggle = new ToggleSwitch(0, 0, width,
                "Enable IRC",
                "Enable the in-game IRC chat client",
                ConfigManager.data.ircEnabled, (val) -> {
                    ConfigManager.data.ircEnabled = val;
                    ConfigManager.save();
                    if (val) {
                        org.blackum.blackaddons.feature.chat.IrcClient.getInstance().connect();
                    } else {
                        org.blackum.blackaddons.feature.chat.IrcClient.getInstance().disconnect();
                    }
                });
        listView.addItem(ircEnabledToggle);

        listView.addItem(new Label(0, 0, "Profiles & Cache", Label.Style.TITLE));
        listView.addItem(new Label(0, 0, "Cache Duration", Label.Style.BODY));

        List<String> cacheOptions = List.of("5 Minutes", "10 Minutes", "30 Minutes", "1 Hour");
        Dropdown cacheDropdown = new Dropdown(0, 0, width, Theme.BUTTON_HEIGHT,
                "Cache Duration",
                cacheOptions, (selected) -> {
                    int minutes = 5;
                    if (selected.contains("5 Minutes"))
                        minutes = 5;
                    else if (selected.contains("10 Minutes"))
                        minutes = 10;
                    else if (selected.contains("30 Minutes"))
                        minutes = 30;
                    else if (selected.contains("1 Hour"))
                        minutes = 60;
                    ConfigManager.data.cacheDurationMinutes = minutes;
                    ConfigManager.save();
                });

        String currentCache = ConfigManager.data.cacheDurationMinutes + " Minutes";
        if (ConfigManager.data.cacheDurationMinutes == 60)
            currentCache = "1 Hour";
        cacheDropdown.setSelectedOption(currentCache);
        listView.addItem(cacheDropdown);

        listView.addItem(new Label(0, 0, "Appearance", Label.Style.TITLE));
        listView.addItem(new Label(0, 0, "Accent Color", Label.Style.BODY));

        ColorPicker accentPicker = new ColorPicker(0, 0, (color) -> {
            ConfigManager.data.accentColor = color;
            Theme.ACCENT = color;
            ConfigManager.save();
        });
        listView.addItem(accentPicker);

        ToggleSwitch layoutToggle = new ToggleSwitch(0, 0, width, "Use Card Layout",
                "Enable card-based layout for various mod screens", ConfigManager.data.useCardLayout,
                (val) -> {
                    ConfigManager.data.useCardLayout = val;
                    ConfigManager.save();
                });
        listView.addItem(layoutToggle);

        listView.addItem(new Label(0, 0, "Interface", Label.Style.TITLE));

        Label durationLabel = new Label(0, 0,
                "Notification Duration: " + ConfigManager.data.notificationDuration + "ms", Label.Style.BODY);
        listView.addItem(durationLabel);

        Slider durationSlider = new Slider(0, 0, width, 1000f, 10000f,
                ConfigManager.data.notificationDuration, (val) -> {
                    int duration = Math.round(val);
                    ConfigManager.data.notificationDuration = duration;
                    durationLabel.setText("Notification Duration: " + duration + "ms");
                    ConfigManager.save();
                });
        listView.addItem(durationSlider);

        listView.addItem(new Label(0, 0, "Developer", Label.Style.TITLE));

        TextField devKeyField = new TextField(0, 0, width, Theme.TEXTFIELD_HEIGHT,
                "Developer Key");
        devKeyField.setText(ConfigManager.data.developerKey != null ? ConfigManager.data.developerKey : "");
        devKeyField.setMaxLength(128);
        listView.addItem(devKeyField);

        Button saveKeyBtn = new Button(0, 0, 100, Theme.BUTTON_HEIGHT, "Save Key", () -> {
            ConfigManager.data.developerKey = devKeyField.getText();
            ConfigManager.save();
            NotificationManager.addNotification("Config", "Developer key saved.", NotificationType.SUCCESS);
        });
        listView.addItem(saveKeyBtn);
    }
}
