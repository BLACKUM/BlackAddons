package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.Button;
import org.blackum.blackaddons.gui.widget.ChatFilterRow;
import org.blackum.blackaddons.gui.widget.Checkbox;
import org.blackum.blackaddons.gui.widget.Dropdown;
import org.blackum.blackaddons.gui.widget.Label;
import org.blackum.blackaddons.gui.widget.ListView;
import org.blackum.blackaddons.gui.widget.TabPanel;
import org.blackum.blackaddons.gui.widget.TextField;
import org.blackum.blackaddons.gui.widget.ToggleSwitch;

import java.util.List;

public class ChatFiltersTabController extends SimpleTabController {

    private ListView listView;

    public ChatFiltersTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int contentX = tab.getParent().getContentX();
        int contentY = tab.getParent().getContentY();
        int width = tab.getParent().getContentWidth() - 20;
        int height = tab.getParent().getContentHeight() - Theme.PADDING_MEDIUM * 2;

        listView = new ListView(contentX, contentY + Theme.PADDING_MEDIUM, width, height);
        tab.addWidget(listView);
        rebuildList();
    }

    private void rebuildList() {
        listView.clearItems();

        listView.addItem(new Label(0, 0, "Chat Filters", Label.Style.TITLE));
        listView.addItem(new Label(0, 0,
                "Hides messages, only visually.",
                Label.Style.BODY));

        ToggleSwitch masterToggle = new ToggleSwitch(0, 0, listView.getWidth(),
                "Enable Chat Filters",
                "Enables chat filters duh",
                ConfigManager.data.chatVisualFiltersEnabled,
                value -> {
                    ConfigManager.data.chatVisualFiltersEnabled = value;
                    ConfigManager.save();
                });
        listView.addItem(masterToggle);

        listView.addItem(new Label(0, 0, "Create Filter", Label.Style.TITLE));

        TextField patternField = new TextField(0, 0, listView.getWidth(), Theme.TEXTFIELD_HEIGHT,
                "Keyword, full message, or regex");
        patternField.setMaxLength(256);
        listView.addItem(patternField);

        List<String> options = List.of("Contains keyword", "Starts with", "Exact match", "Regex");
        Dropdown matchTypeDropdown = new Dropdown(0, 0, listView.getWidth(), Theme.BUTTON_HEIGHT,
                "Match Type", options, selected -> {
                });
        matchTypeDropdown.setSelectedIndex(0);
        listView.addItem(matchTypeDropdown);

        Checkbox caseSensitiveCheckbox = new Checkbox(0, 0, "Case-sensitive", false, value -> {
        });
        listView.addItem(caseSensitiveCheckbox);

        Button addButton = new Button(0, 0, 140, Theme.BUTTON_HEIGHT, "Add Filter", () -> {
            String pattern = patternField.getText().trim();
            if (pattern.isEmpty()) {
                NotificationManager.addNotification("Chat Filters", "Enter a pattern first.", NotificationType.WARNING);
                return;
            }

            ConfigManager.ChatVisualFilter filter = new ConfigManager.ChatVisualFilter(
                    pattern,
                    getMatchType(matchTypeDropdown),
                    caseSensitiveCheckbox.isChecked());
            ConfigManager.data.chatVisualFilters.add(filter);
            ConfigManager.save();
            rebuildList();
            NotificationManager.addNotification("Chat Filters", "Filter added.", NotificationType.SUCCESS);
        });
        listView.addItem(addButton);

        listView.addItem(new Label(0, 0, "Saved Filters", Label.Style.TITLE));
        if (ConfigManager.data.chatVisualFilters.isEmpty()) {
            listView.addItem(new Label(0, 0, "No filters yet.", Label.Style.CAPTION));
            return;
        }

        for (ConfigManager.ChatVisualFilter filter : ConfigManager.data.chatVisualFilters) {
            ChatFilterRow row = new ChatFilterRow(filter, this::rebuildList, () -> {
                ConfigManager.data.chatVisualFilters.remove(filter);
                ConfigManager.save();
                rebuildList();
            });
            listView.addItem(row);
        }
    }

    private ConfigManager.ChatFilterMatchType getMatchType(Dropdown dropdown) {
        String selected = dropdown.getSelectedIndex() >= 0 ? switch (dropdown.getSelectedIndex()) {
            case 1 -> "STARTS_WITH";
            case 2 -> "EXACT";
            case 3 -> "REGEX";
            default -> "CONTAINS";
        } : "CONTAINS";
        return ConfigManager.ChatFilterMatchType.valueOf(selected);
    }
}
