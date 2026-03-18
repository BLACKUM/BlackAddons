package org.blackum.blackaddons.feature.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.blackum.blackaddons.core.config.ConfigManager.WaypointAction;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointGroup;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.feature.chat.ChatActionExecutor;
import org.blackum.blackaddons.core.util.FormatUtils;

import org.blackum.blackaddons.gui.notification.NotificationManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WaypointActionManager {
    private enum TriggerType {
        ENTRY,
        EXIT,
        GUI_EXIT
    }

    private static WaypointActionManager instance;
    private final Map<Waypoint, Boolean> playerInsideWaypoint = new HashMap<>();
    private boolean hasLastPosition;
    private double lastPlayerX;
    private double lastPlayerY;
    private double lastPlayerZ;

    private WaypointActionManager() {
    }

    public static WaypointActionManager getInstance() {
        if (instance == null) {
            instance = new WaypointActionManager();
        }
        return instance;
    }

    public void onPlayerPositionChanged() {
        checkWaypoints(WaypointManager.getInstance().getWaypoints());
    }

    public void onGuiClosed() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        String dimension = client.level.dimension().location().toString();
        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();

        for (Waypoint waypoint : WaypointManager.getInstance().getWaypoints()) {
            if (!waypoint.enabled || waypoint.dimension == null || !waypoint.dimension.equals(dimension)) {
                continue;
            }
            if (waypoint.groupId != null) {
                WaypointGroup group = WaypointManager.getInstance().getGroup(waypoint.groupId);
                if (group != null && !group.isActive()) {
                    continue;
                }
            }
            if (isInside(waypoint, playerX, playerY, playerZ)) {
                triggerActions(waypoint, TriggerType.GUI_EXIT);
            }
        }
    }

    public void tick(java.util.List<Waypoint> waypoints) {
        checkWaypoints(waypoints);
    }

    private void checkWaypoints(java.util.List<Waypoint> waypoints) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            playerInsideWaypoint.clear();
            hasLastPosition = false;
            return;
        }

        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();
        String dimension = client.level.dimension().location().toString();
        double previousX = hasLastPosition ? lastPlayerX : playerX;
        double previousY = hasLastPosition ? lastPlayerY : playerY;
        double previousZ = hasLastPosition ? lastPlayerZ : playerZ;
        Set<Waypoint> activeWaypoints = new HashSet<>();

        for (Waypoint waypoint : waypoints) {
            activeWaypoints.add(waypoint);
            if (!waypoint.enabled || waypoint.dimension == null || !waypoint.dimension.equals(dimension)) {
                playerInsideWaypoint.put(waypoint, false);
                continue;
            }
            if (waypoint.groupId != null) {
                WaypointGroup group = WaypointManager.getInstance().getGroup(waypoint.groupId);
                if (group != null && !group.isActive()) {
                    playerInsideWaypoint.put(waypoint, false);
                    continue;
                }
            }

            boolean currentlyInside = isInside(waypoint, playerX, playerY, playerZ);
            boolean intersectedDuringMove = hasLastPosition
                    && intersectsMovement(waypoint, previousX, previousY, previousZ, playerX, playerY, playerZ);
            Boolean previouslyInside = playerInsideWaypoint.get(waypoint);

            if (previouslyInside == null) {
                playerInsideWaypoint.put(waypoint, currentlyInside);
                continue;
            }

            if (!previouslyInside && (currentlyInside || intersectedDuringMove)) {
                triggerActions(waypoint, TriggerType.ENTRY);
            } else if (!currentlyInside && previouslyInside) {
                triggerActions(waypoint, TriggerType.EXIT);
            }

            playerInsideWaypoint.put(waypoint, currentlyInside);
        }

        playerInsideWaypoint.keySet().removeIf(waypoint -> !activeWaypoints.contains(waypoint));
        lastPlayerX = playerX;
        lastPlayerY = playerY;
        lastPlayerZ = playerZ;
        hasLastPosition = true;
    }

    private boolean isInside(Waypoint waypoint, double x, double y, double z) {
        double dx = x - waypoint.x;
        double dz = z - waypoint.z;
        if (y < waypoint.y || y > waypoint.y + waypoint.height) {
            return false;
        }
        if (waypoint.shape == org.blackum.blackaddons.core.waypoint.WaypointShape.BOX) {
            return Math.abs(dx) <= waypoint.radius && Math.abs(dz) <= waypoint.radius;
        }

        double radiusSq = waypoint.radius * waypoint.radius;
        return dx * dx + dz * dz <= radiusSq;
    }

    private boolean intersectsMovement(Waypoint waypoint, double x1, double y1, double z1, double x2, double y2, double z2) {
        if (isInside(waypoint, x1, y1, z1) || isInside(waypoint, x2, y2, z2)) {
            return true;
        }

        if (waypoint.shape == org.blackum.blackaddons.core.waypoint.WaypointShape.BOX) {
            return intersectsBox(waypoint, x1, y1, z1, x2, y2, z2);
        }

        return intersectsCylinder(waypoint, x1, y1, z1, x2, y2, z2);
    }

    private boolean intersectsBox(Waypoint waypoint, double x1, double y1, double z1, double x2, double y2, double z2) {
        double minX = waypoint.x - waypoint.radius;
        double maxX = waypoint.x + waypoint.radius;
        double minY = waypoint.y;
        double maxY = waypoint.y + waypoint.height;
        double minZ = waypoint.z - waypoint.radius;
        double maxZ = waypoint.z + waypoint.radius;

        double tMin = 0.0;
        double tMax = 1.0;

        double dx = x2 - x1;
        if (!clipAxis(x1, dx, minX, maxX, tMin, tMax)) return false;
        tMin = clipMin;
        tMax = clipMax;

        double dy = y2 - y1;
        if (!clipAxis(y1, dy, minY, maxY, tMin, tMax)) return false;
        tMin = clipMin;
        tMax = clipMax;

        double dz = z2 - z1;
        return clipAxis(z1, dz, minZ, maxZ, tMin, tMax);
    }

    private double clipMin;
    private double clipMax;

    private boolean clipAxis(double start, double delta, double min, double max, double currentMin, double currentMax) {
        if (Math.abs(delta) < 1.0E-7) {
            if (start < min || start > max) {
                return false;
            }
            clipMin = currentMin;
            clipMax = currentMax;
            return true;
        }

        double t1 = (min - start) / delta;
        double t2 = (max - start) / delta;
        double axisMin = Math.min(t1, t2);
        double axisMax = Math.max(t1, t2);
        clipMin = Math.max(currentMin, axisMin);
        clipMax = Math.min(currentMax, axisMax);
        return clipMin <= clipMax;
    }

    private boolean intersectsCylinder(Waypoint waypoint, double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        double tMinY = 0.0;
        double tMaxY = 1.0;
        if (Math.abs(dy) < 1.0E-7) {
            if (y1 < waypoint.y || y1 > waypoint.y + waypoint.height) {
                return false;
            }
        } else {
            double ty1 = (waypoint.y - y1) / dy;
            double ty2 = (waypoint.y + waypoint.height - y1) / dy;
            tMinY = Math.max(0.0, Math.min(ty1, ty2));
            tMaxY = Math.min(1.0, Math.max(ty1, ty2));
            if (tMinY > tMaxY) {
                return false;
            }
        }

        double ox = x1 - waypoint.x;
        double oz = z1 - waypoint.z;
        double a = dx * dx + dz * dz;
        double radiusSq = waypoint.radius * waypoint.radius;

        if (a < 1.0E-7) {
            return ox * ox + oz * oz <= radiusSq;
        }

        double b = 2.0 * (ox * dx + oz * dz);
        double c = ox * ox + oz * oz - radiusSq;
        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0.0) {
            return false;
        }

        double sqrt = Math.sqrt(discriminant);
        double inv = 1.0 / (2.0 * a);
        double t1 = (-b - sqrt) * inv;
        double t2 = (-b + sqrt) * inv;
        double hitMin = Math.max(tMinY, Math.min(t1, t2));
        double hitMax = Math.min(tMaxY, Math.max(t1, t2));
        return hitMin <= hitMax && hitMax >= 0.0 && hitMin <= 1.0;
    }

    private void triggerActions(Waypoint waypoint, TriggerType triggerType) {
        Minecraft client = Minecraft.getInstance();
        java.util.List<WaypointAction> actions = waypoint.actions;
        for (WaypointAction action : actions) {
            if (!action.enabled) continue;
            if (triggerType == TriggerType.ENTRY && !action.triggerOnEntry) continue;
            if (triggerType == TriggerType.EXIT && !action.triggerOnExit) continue;
            if (triggerType == TriggerType.GUI_EXIT && !action.triggerOnGuiExit) continue;

            if (action.soundId != null && !action.soundId.isEmpty()) {
                try {
                    ResourceLocation location = ResourceLocation.tryParse(action.soundId);
                    if (location == null) location = ResourceLocation.fromNamespaceAndPath("minecraft", action.soundId);
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

            if (action.showNotification && !action.notificationMessage.isEmpty()) {
                String nTitle = action.notificationTitle != null && !action.notificationTitle.isEmpty()
                        ? action.notificationTitle : (waypoint.name != null ? waypoint.name : "Waypoint Action");
                NotificationManager.addNotification(
                        FormatUtils.formatColor(nTitle),
                        FormatUtils.formatColor(action.notificationMessage),
                        action.notificationType
                );
            }

            if (!action.actions.isEmpty()) {
                ChatActionExecutor.getInstance().execute(action.actions, new String[0]);
            }
        }
    }
}
