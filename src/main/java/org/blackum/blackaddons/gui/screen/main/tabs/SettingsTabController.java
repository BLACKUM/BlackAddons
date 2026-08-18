package org.blackum.blackaddons.gui.screen.main.tabs;

import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.feature.chat.IrcClient;

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
                        IrcClient.getInstance().connect();
                    } else {
                        IrcClient.getInstance().disconnect();
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

        listView.addItem(new Label(0, 0, "Data Source Priority", Label.Style.TITLE));

        List<String> apiOptions = List.of(
                "PlainDawn (Full stats)",
                "Adjectils (Full stats)",
                "SkyCrypt (Bugged inventory, slow, won't fix)",
                "Soopy (No secrets/score/inventory)");

        String[] tierLabels = {
                "Primary Source", "Fallback", "Fallback 2"
        };
        for (int i = 0; i < ConfigManager.API_PRIORITY_SLOTS; i++) {
            final int index = i;
            listView.addItem(new Label(0, 0, tierLabels[i], Label.Style.BODY));
            Dropdown dd = new Dropdown(0, 0, width, Theme.BUTTON_HEIGHT, (i + 1) + " Priority",
                    apiOptions, (selected) -> updatePriority(index, selected));
            dd.setSelectedOption(formatApiName(ConfigManager.data.apiPriorityList.get(i)));
            listView.addItem(dd);
        }

        listView.addItem(new Label(0, 0, "Appearance", Label.Style.TITLE));
        listView.addItem(new Label(0, 0, "Accent Color", Label.Style.BODY));

        ColorPicker accentPicker = new ColorPicker(0, 0, ConfigManager.data.accentColor, (color) -> {
            ConfigManager.data.accentColor = color;
            Theme.ACCENT = color;
            Theme.refreshColors();
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

        Label guiScaleLabel = new Label(0, 0,
                "Forced GUI Scale: " + formatGuiScale(ConfigManager.data.forcedGuiScale),
                Label.Style.BODY);
        listView.addItem(guiScaleLabel);

        Slider guiScaleSlider = new Slider(0, 0, width, 0f, 5f,
                ConfigManager.data.forcedGuiScale, (val) -> {
                    guiScaleLabel.setText("Forced GUI Scale: " + formatGuiScale(val));
                }).onRelease((val) -> {
                    ConfigManager.data.forcedGuiScale = roundGuiScale(val);
                    ConfigManager.save();

                    Minecraft mc = Minecraft.getInstance();
                    if (McCompat.getScreen(mc) instanceof BaseScreen) {
                        McCompat.setScreen(mc, McCompat.getScreen(mc));
                    }
                });
        listView.addItem(guiScaleSlider);

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

        // Only needed for a self-hosted bot URL that refuses anonymous requests. Empty means no
        // Authorization header is sent at all, which is what the official backend expects.
        TextField botTokenField = new TextField(0, 0, width, Theme.TEXTFIELD_HEIGHT,
                "Bot Token (self-hosted only)");
        botTokenField.setText(ConfigManager.data.botToken != null ? ConfigManager.data.botToken : "");
        botTokenField.setMaxLength(128);
        listView.addItem(botTokenField);

        Button saveBotTokenBtn = new Button(0, 0, 100, Theme.BUTTON_HEIGHT, "Save Token", () -> {
            ConfigManager.data.botToken = botTokenField.getText();
            ConfigManager.save();
            NotificationManager.addNotification("Config", "Bot token saved.", NotificationType.SUCCESS);
        });
        listView.addItem(saveBotTokenBtn);
    }

    private void updatePriority(int index, String selected) {
        ConfigManager.ApiPriority priority = parseApiName(selected);
        if (priority == null)
            return;
        if (ConfigManager.data.apiPriorityList.size() > index) {
            ConfigManager.data.apiPriorityList.set(index, priority);
            ConfigManager.save();
        }
    }

    private ConfigManager.ApiPriority parseApiName(String selected) {
        String first = selected.split(" ")[0];
        switch (first) {
            case "PlainDawn":
                return ConfigManager.ApiPriority.PLAIN_DAWN;
            case "Adjectils":
                return ConfigManager.ApiPriority.ADJECTILS;
            case "SkyCrypt":
                return ConfigManager.ApiPriority.SKYCRYPT;
            case "Soopy":
                return ConfigManager.ApiPriority.SOOPY;
            default:
                return null;
        }
    }

    private String formatApiName(ConfigManager.ApiPriority priority) {
        switch (priority) {
            case PLAIN_DAWN:
                return "PlainDawn (Full stats)";
            case ADJECTILS:
                return "Adjectils (Full stats)";
            case SKYCRYPT:
                return "SkyCrypt (Bugged inventory, slow, won't fix)";
            case SOOPY:
                return "Soopy (No secrets/score/inventory)";
            default:
                return priority.name();
        }
    }

    private static float roundGuiScale(float value) {
        return Math.round(Math.max(0.0f, value) * 100.0f) / 100.0f;
    }

    private static String formatGuiScale(float value) {
        float normalized = roundGuiScale(value);
        return normalized <= 0.0f ? "Off" : String.format(Locale.ROOT, "%.2f", normalized);
    }
}
