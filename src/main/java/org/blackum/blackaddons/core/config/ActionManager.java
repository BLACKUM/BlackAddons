package org.blackum.blackaddons.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.core.util.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ActionManager {
    private static ActionManager instance;
    private static final Path OLD_CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME);
    private static final File OLD_ACTIONS_FILE = OLD_CONFIG_DIR.resolve(Constants.ACTIONS_FILE_NAME).toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getChatActionsFile() {
        return ProfileManager.getActiveProfileFile(ProfileManager.Category.CHAT_ACTIONS);
    }

    private final List<ConfigManager.ChatAction> chatActions = new ArrayList<>();

    public void resetToDefaults() {
        chatActions.clear();
        chatActions.add(new ConfigManager.ChatAction(
                "(?s).*?(?:\\[.*?\\] )?([A-Za-z0-9_]+) has invited you to join their party!.*You have 60 seconds to accept.*",
                true,
                "entity.cat.ambient", 1.0f, 1.0f, true, "&cParty Invite!", "&6From: &e{1}", 2.0f));
        chatActions.add(new ConfigManager.ChatAction(
                "Party Finder > ([A-Za-z0-9_]+) joined the dungeon group! \\((.*)\\)", 
                true,
                "entity.experience_orb.pickup",
                1.0f, 1.0f, true, "&a{1} Joined!", "&7Class: &b{2}", 3.0f));
    }

    private ActionManager() {
        load();
    }

    public static ActionManager getInstance() {
        if (instance == null) {
            instance = new ActionManager();
        }
        return instance;
    }

    public List<ConfigManager.ChatAction> getChatActions() {
        return chatActions;
    }

    public void save() {
        try {
            File chatActionsFile = getChatActionsFile();
            File parent = chatActionsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter writer = new FileWriter(chatActionsFile)) {
                GSON.toJson(chatActions, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        migrateProfile();
        File chatActionsFile = getChatActionsFile();
        if (!chatActionsFile.exists()) {
            migrateOld();
            return;
        }

        try (FileReader reader = new FileReader(chatActionsFile)) {
            List<ConfigManager.ChatAction> loaded = GSON.fromJson(reader, new TypeToken<List<ConfigManager.ChatAction>>() {}.getType());
            if (loaded != null) {
                chatActions.clear();
                chatActions.addAll(loaded);
                for (ConfigManager.ChatAction action : chatActions) {
                    ConfigManager.normalizeActionSteps(action.actions);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void migrateProfile() {
        File targetFile = getChatActionsFile();
        File rootChatFile = OLD_CONFIG_DIR.resolve(Constants.CHAT_ACTIONS_FILE_NAME).toFile();
        
        if (rootChatFile.exists() && !targetFile.exists()) {
            try {
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                if (rootChatFile.renameTo(targetFile)) {
                    org.blackum.blackaddons.Blackaddons.LOGGER.info("Successfully migrated chat_actions.json to default profile");
                }
            } catch (Exception e) {
                org.blackum.blackaddons.Blackaddons.LOGGER.error("Failed to migrate chat_actions.json", e);
            }
        }
    }

    private void migrateOld() {
        if (!OLD_ACTIONS_FILE.exists()) {
            boolean migrated = false;
            if (!ConfigManager.data.chatActions.isEmpty()) {
                chatActions.addAll(ConfigManager.data.chatActions);
                ConfigManager.data.chatActions.clear();
                migrated = true;
            }

            for (org.blackum.blackaddons.core.waypoint.Waypoint wp : org.blackum.blackaddons.core.waypoint.WaypointManager.getInstance().getWaypoints()) {
                if (wp.actions != null && !wp.actions.isEmpty()) {
                    migrated = true;
                }
            }

            if (migrated) {
                save();
                ConfigManager.save();
                org.blackum.blackaddons.core.waypoint.WaypointManager.getInstance().save();
            }
            return;
        }

        try (FileReader reader = new FileReader(OLD_ACTIONS_FILE)) {
            ActionData data = GSON.fromJson(reader, ActionData.class);
            if (data != null) {
                if (data.chatActions != null) {
                    chatActions.addAll(data.chatActions);
                    for (ConfigManager.ChatAction action : chatActions) {
                        ConfigManager.normalizeActionSteps(action.actions);
                    }
                }
                if (data.waypointActions != null) {
                    org.blackum.blackaddons.core.waypoint.WaypointManager.getInstance().mergeActions(data.waypointActions);
                }
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ActionData {
        public List<ConfigManager.ChatAction> chatActions;
        public Map<UUID, List<ConfigManager.WaypointAction>> waypointActions;

        public ActionData(List<ConfigManager.ChatAction> chatActions, Map<UUID, List<ConfigManager.WaypointAction>> waypointActions) {
            this.chatActions = chatActions;
            this.waypointActions = waypointActions;
        }
    }
}
