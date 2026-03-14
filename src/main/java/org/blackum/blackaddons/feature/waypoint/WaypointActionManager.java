package org.blackum.blackaddons.feature.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.blackum.blackaddons.core.config.ActionManager;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.config.ConfigManager.WaypointAction;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.feature.chat.ChatActionExecutor;
import org.blackum.blackaddons.core.util.FormatUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WaypointActionManager {
    private static WaypointActionManager instance;
    private final Map<UUID, Boolean> playerInsideWaypoint = new HashMap<>();

    private WaypointActionManager() {
    }

    public static WaypointActionManager getInstance() {
        if (instance == null) {
            instance = new WaypointActionManager();
        }
        return instance;
    }

    public void tick(java.util.List<Waypoint> waypoints) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();
        String dimension = client.level.dimension().identifier().toString();

        for (Waypoint waypoint : waypoints) {
            java.util.List<WaypointAction> actions = waypoint.actions;
            if (!waypoint.enabled || !waypoint.dimension.equals(dimension)) continue;

            double dx = playerX - waypoint.x;
            double dy = playerY - waypoint.y;
            double dz = playerZ - waypoint.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            double radiusSq = waypoint.radius * waypoint.radius;

            boolean currentlyInside = distanceSq <= radiusSq;
            Boolean previouslyInside = playerInsideWaypoint.get(waypoint.id);

            if (previouslyInside == null) {
                playerInsideWaypoint.put(waypoint.id, currentlyInside);
                continue;
            }

            if (currentlyInside && !previouslyInside) {
                triggerActions(waypoint, true);
            } else if (!currentlyInside && previouslyInside) {
                triggerActions(waypoint, false);
            }

            playerInsideWaypoint.put(waypoint.id, currentlyInside);
        }
    }

    private void triggerActions(Waypoint waypoint, boolean entry) {
        Minecraft client = Minecraft.getInstance();
        java.util.List<WaypointAction> actions = waypoint.actions;
        for (WaypointAction action : actions) {
            if (!action.enabled) continue;
            if ((entry && !action.triggerOnEntry) || (!entry && !action.triggerOnExit)) continue;

            if (action.soundId != null && !action.soundId.isEmpty()) {
                try {
                    Identifier location = Identifier.tryParse(action.soundId);
                    if (location == null) location = Identifier.fromNamespaceAndPath("minecraft", action.soundId);
                    SoundEvent event = SoundEvent.createVariableRangeEvent(location);
                    client.getSoundManager().play(SimpleSoundInstance.forUI(event, action.pitch, action.volume));
                } catch (Exception ignored) {}
            }

            if (action.durationSeconds > 0 && action.title != null && !action.title.isEmpty() && client.gui != null) {
                client.gui.setTimes(10, (int) (action.durationSeconds * 20), 20);
                client.gui.setTitle(Component.literal(FormatUtils.formatColor(action.title)));
                if (action.subtitle != null && !action.subtitle.isEmpty()) {
                    client.gui.setSubtitle(Component.literal(FormatUtils.formatColor(action.subtitle)));
                }
            }

            if (!action.actions.isEmpty()) {
                ChatActionExecutor.getInstance().execute(action.actions, new String[0]);
            }
        }
    }
}
