package org.blackum.blackaddons.gui.screen.tabs;

import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.core.config.ActionManager;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.config.ProfileManager;
import net.minecraft.util.Util;
import org.blackum.blackaddons.core.util.Constants;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigsTabController extends SimpleTabController {
    private static ProfileManager.Category selectedCategory = ProfileManager.Category.CONFIG;
    private ListView listView;

    public ConfigsTabController(BlackAddonsGUI screen) {
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
        tab.addWidget(listView);

        listView.addItem(new Label(0, 0, "Profile Management", Label.Style.TITLE));
        listView.addItem(new Label(0, 0, "Manage independent profiles for different categories.", Label.Style.BODY));

        Button openFolderBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Open Config Folder", this::openConfigFolder);
        listView.addItem(openFolderBtn);

        Button reloadAllBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Reload All Configs", () -> {
            reloadAll();
            screen.init();
        });
        listView.addItem(reloadAllBtn);

        List<String> categories = Arrays.stream(ProfileManager.Category.values())
                .map(c -> formatCategoryName(c))
                .collect(Collectors.toList());

        Dropdown categoryDropdown = new Dropdown(0, 0, itemWidth, "Select Category", categories, selected -> {
            selectedCategory = parseCategoryName(selected);
            screen.init();
        });
        categoryDropdown.setSelectedOption(formatCategoryName(selectedCategory));
        
        listView.addItem(new SettingWrapper(0, 0, itemWidth, "Profile Category", "Which category's profiles to manage", categoryDropdown));

        listView.addItem(new SectionHeader(itemWidth, formatCategoryName(selectedCategory) + " Profiles"));

        String activeProfile = ProfileManager.getActiveProfile(selectedCategory);
        List<String> profiles = ProfileManager.listProfiles(selectedCategory);

        for (String profile : profiles) {
            boolean isActive = profile.equals(activeProfile);
            String title = profile + (isActive ? " (Active)" : "");
            
            SectionHeader header = new SectionHeader(itemWidth, title, false, null);
            ExpandableGroup group = new ExpandableGroup(0, 0, itemWidth, header, isActive);
            header.setToggleCallback(() -> group.setExpanded(!group.isExpanded()));
            
            if (!isActive) {
                Button activateBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Activate", () -> {
                    ProfileManager.setActiveProfile(selectedCategory, profile);
                    reloadCategory(selectedCategory);
                    screen.init();
                });
                group.addChild(activateBtn);
            }

            Button duplicateBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Duplicate", () -> {
                String newName = profile + "_copy";
                int i = 1;
                while (profiles.contains(newName)) {
                    newName = profile + "_copy_" + i++;
                }
                ProfileManager.duplicateProfile(selectedCategory, profile, newName);
                screen.init();
            });
            group.addChild(duplicateBtn);

            if (!profile.equals("default")) {
                Button deleteBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Delete", () -> {
                    ProfileManager.deleteProfile(selectedCategory, profile);
                    screen.init();
                });
                group.addChild(deleteBtn);
            }

            listView.addItem(group);
        }

        TextField newProfileName = new TextField(0, 0, itemWidth, Theme.TEXTFIELD_HEIGHT, "New profile name...");
        listView.addItem(newProfileName);

        Button addBtn = new Button(0, 0, itemWidth, Theme.BUTTON_HEIGHT, "Create New Profile", () -> {
            String name = newProfileName.getText().trim();
            if (!name.isEmpty() && !profiles.contains(name)) {
                ProfileManager.setActiveProfile(selectedCategory, name);
                resetAndSaveCategory(selectedCategory);
                screen.init();
            }
        });
        listView.addItem(addBtn);
    }

    private String formatCategoryName(ProfileManager.Category category) {
        return switch (category) {
            case CONFIG -> "Main Config";
            case CHAT_ACTIONS -> "Chat Actions";
            case WAYPOINTS -> "Waypoints";
        };
    }

    private ProfileManager.Category parseCategoryName(String name) {
        if (name.equals("Main Config")) return ProfileManager.Category.CONFIG;
        if (name.equals("Chat Actions")) return ProfileManager.Category.CHAT_ACTIONS;
        if (name.equals("Waypoints")) return ProfileManager.Category.WAYPOINTS;
        return ProfileManager.Category.CONFIG;
    }

    private void reloadCategory(ProfileManager.Category category) {
        switch (category) {
            case CONFIG -> ConfigManager.load();
            case CHAT_ACTIONS -> ActionManager.getInstance().load();
            case WAYPOINTS -> WaypointManager.getInstance().load();
        }
    }

    private void saveCategory(ProfileManager.Category category) {
        switch (category) {
            case CONFIG -> ConfigManager.save();
            case CHAT_ACTIONS -> ActionManager.getInstance().save();
            case WAYPOINTS -> WaypointManager.getInstance().save();
        }
    }

    private void resetAndSaveCategory(ProfileManager.Category category) {
        switch (category) {
            case CONFIG -> {
                ConfigManager.resetToDefaults();
                ConfigManager.save();
            }
            case CHAT_ACTIONS -> {
                ActionManager.getInstance().resetToDefaults();
                ActionManager.getInstance().save();
            }
            case WAYPOINTS -> {
                WaypointManager.getInstance().resetToDefaults();
                WaypointManager.getInstance().save();
            }
        }
    }

    private void openConfigFolder() {
        File folder = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME).toFile();
        if (folder.exists()) {
            Util.getPlatform().openUri(folder.toURI().toString());
        }
    }

    private void reloadAll() {
        ConfigManager.load();
        ActionManager.getInstance().load();
        WaypointManager.getInstance().load();
    }
}
