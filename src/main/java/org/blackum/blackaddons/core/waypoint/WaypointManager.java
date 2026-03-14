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

public class WaypointManager {
    private static WaypointManager instance;
    private static final Path OLD_CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Constants.CONFIG_DIR_NAME);
    private static final File OLD_WAYPOINTS_FILE = OLD_CONFIG_DIR.resolve(Constants.WAYPOINTS_FILE_NAME).toFile();
    
    private static File getWaypointsFile() {
        return ProfileManager.getActiveProfileFile(ProfileManager.Category.WAYPOINTS);
    }
    
    private final List<Waypoint> waypoints = new ArrayList<>();

    public void resetToDefaults() {
        waypoints.clear();
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

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        save();
    }

    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
        save();
    }

    public void save() {
        try {
            File waypointsFile = getWaypointsFile();
            File parent = waypointsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter writer = new FileWriter(waypointsFile)) {
                Constants.GSON.toJson(waypoints, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        migrate();
        File waypointsFile = getWaypointsFile();
        if (!waypointsFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(waypointsFile)) {
            List<Waypoint> loaded = Constants.GSON.fromJson(reader, new TypeToken<List<Waypoint>>() {}.getType());
            if (loaded != null) {
                waypoints.clear();
                waypoints.addAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void migrate() {
        File targetFile = getWaypointsFile();
        if (OLD_WAYPOINTS_FILE.exists() && !targetFile.exists()) {
            try {
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                if (OLD_WAYPOINTS_FILE.renameTo(targetFile)) {
                    org.blackum.blackaddons.Blackaddons.LOGGER.info("Successfully migrated waypoints.json to default profile");
                }
            } catch (Exception e) {
                org.blackum.blackaddons.Blackaddons.LOGGER.error("Failed to migrate waypoints.json", e);
            }
        }
    }

    public void mergeActions(java.util.Map<java.util.UUID, List<WaypointAction>> actionsMap) {
        boolean changed = false;
        for (Waypoint waypoint : waypoints) {
            List<WaypointAction> actions = actionsMap.get(waypoint.id);
            if (actions != null && !actions.isEmpty()) {
                waypoint.actions.addAll(actions);
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }
}
