package org.blackum.blackaddons.core.waypoint;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.config.ConfigManager.WaypointAction;
import org.blackum.blackaddons.core.config.ProfileManager;
import org.blackum.blackaddons.core.util.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WaypointManager {
    private static WaypointManager instance;
    private static final Path OLD_CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME);
    private static final File OLD_WAYPOINTS_FILE = OLD_CONFIG_DIR.resolve(Constants.WAYPOINTS_FILE_NAME).toFile();

    private static File getWaypointsFile() {
        return ProfileManager.getActiveProfileFile(ProfileManager.Category.WAYPOINTS);
    }

    private static File getGroupsFile() {
        File waypointsFile = getWaypointsFile();
        String name = waypointsFile.getName().replace(".json", "_groups.json");
        return new File(waypointsFile.getParentFile(), name);
    }

    private final List<Waypoint> waypoints = new ArrayList<>();
    private final List<WaypointGroup> groups = new ArrayList<>();

    public void resetToDefaults() {
        waypoints.clear();
        groups.clear();
    }

    private WaypointManager() {
        load();
    }

    public static WaypointManager getInstance() {
        if (instance == null) {
            instance = new WaypointManager();
        }
        return instance;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public List<WaypointGroup> getGroups() {
        return groups;
    }

    public WaypointGroup getGroup(UUID id) {
        if (id == null) return null;
        for (WaypointGroup group : groups) {
            if (id.equals(group.id)) return group;
        }
        return null;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        save();
    }

    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
        save();
    }

    public void addGroup(WaypointGroup group) {
        groups.add(group);
        save();
    }

    public void removeGroup(WaypointGroup group) {
        for (Waypoint wp : waypoints) {
            if (group.id.equals(wp.groupId)) {
                wp.groupId = null;
            }
        }
        groups.remove(group);
        save();
    }

    public List<Waypoint> getWaypointsForGroup(UUID groupId) {
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : waypoints) {
            if (groupId == null ? wp.groupId == null : groupId.equals(wp.groupId)) {
                result.add(wp);
            }
        }
        return result;
    }

    public void save() {
        saveWaypoints();
        saveGroups();
    }

    private void saveWaypoints() {
        try {
            File waypointsFile = getWaypointsFile();
            ensureParent(waypointsFile);
            try (FileWriter writer = new FileWriter(waypointsFile)) {
                Constants.GSON.toJson(waypoints, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveGroups() {
        try {
            File groupsFile = getGroupsFile();
            ensureParent(groupsFile);
            try (FileWriter writer = new FileWriter(groupsFile)) {
                Constants.GSON.toJson(groups, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void ensureParent(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    public void load() {
        migrate();
        loadWaypoints();
        loadGroups();
    }

    private void loadWaypoints() {
        File waypointsFile = getWaypointsFile();
        if (!waypointsFile.exists()) return;
        try (FileReader reader = new FileReader(waypointsFile)) {
            List<Waypoint> loaded = Constants.GSON.fromJson(reader, new TypeToken<List<Waypoint>>() {}.getType());
            if (loaded != null) {
                waypoints.clear();
                waypoints.addAll(loaded);
                for (Waypoint waypoint : waypoints) {
                    if (waypoint.actions != null) {
                        for (WaypointAction action : waypoint.actions) {
                            ConfigManager.normalizeActionSteps(action.actions);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGroups() {
        File groupsFile = getGroupsFile();
        if (!groupsFile.exists()) return;
        try (FileReader reader = new FileReader(groupsFile)) {
            List<WaypointGroup> loaded = Constants.GSON.fromJson(reader, new TypeToken<List<WaypointGroup>>() {}.getType());
            if (loaded != null) {
                groups.clear();
                groups.addAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void migrate() {
        File targetFile = getWaypointsFile();
        if (OLD_WAYPOINTS_FILE.exists() && !targetFile.exists()) {
            try {
                ensureParent(targetFile);
                if (OLD_WAYPOINTS_FILE.renameTo(targetFile)) {
                    org.blackum.blackaddons.Blackaddons.LOGGER.info("Successfully migrated waypoints.json to default profile");
                }
            } catch (Exception e) {
                org.blackum.blackaddons.Blackaddons.LOGGER.error("Failed to migrate waypoints.json", e);
            }
        }
    }

    public void mergeActions(Map<UUID, List<WaypointAction>> actionsMap) {
        boolean changed = false;
        for (Waypoint waypoint : waypoints) {
            List<WaypointAction> actions = actionsMap.get(waypoint.id);
            if (actions != null && !actions.isEmpty()) {
                for (WaypointAction action : actions) {
                    ConfigManager.normalizeActionSteps(action.actions);
                }
                waypoint.actions.addAll(actions);
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }
}
