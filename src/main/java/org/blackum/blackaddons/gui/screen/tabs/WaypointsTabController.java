package org.blackum.blackaddons.gui.screen.tabs;

import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.core.config.ActionManager;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.config.ConfigManager.WaypointAction;
import org.blackum.blackaddons.core.waypoint.Waypoint;
import org.blackum.blackaddons.core.waypoint.WaypointManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.WaypointEditScreen;
import org.blackum.blackaddons.gui.screen.WaypointActionEditScreen;
import org.blackum.blackaddons.gui.widget.*;

public class WaypointsTabController extends SimpleTabController {
    private ListView waypointList;

    public WaypointsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab waypointsTab) {
        int contentX = waypointsTab.getParent().getContentX();
        int contentY = waypointsTab.getParent().getContentY();
        int contentWidth = waypointsTab.getParent().getContentWidth();
        int contentHeight = waypointsTab.getParent().getContentHeight();

        Button addBtn = new Button(contentX + Theme.PADDING, contentY + Theme.PADDING, contentWidth - Theme.PADDING * 2, 20, "Add Waypoint", () -> {
            Minecraft mc = Minecraft.getInstance();
            double x = 0, y = 0, z = 0;
            String dim = null;
            if (mc.player != null && mc.level != null) {
                x = mc.player.getX();
                y = mc.player.getY();
                z = mc.player.getZ();
                dim = mc.level.dimension().location().toString();
            }
            final double fx = x, fy = y, fz = z;
            final String fdim = dim;
            
            mc.execute(() -> {
                mc.setScreen(new WaypointEditScreen(screen, new Waypoint("", fx, fy, fz, fdim), wp -> {
                    WaypointManager.getInstance().addWaypoint(wp);
                    rebuildList();
                }));
            });
        });
        waypointsTab.addWidget(addBtn);

        waypointList = new ListView(contentX + Theme.PADDING, contentY + 40, contentWidth - Theme.PADDING * 2, contentHeight - 50);
        waypointsTab.addWidget(waypointList);
        
        rebuildList();
    }

    private void rebuildList() {
        if (waypointList == null) return;
        waypointList.clearItems();
        int itemWidth = waypointList.getWidth() - 16;
        Minecraft mc = Minecraft.getInstance();

        java.util.List<Waypoint> waypoints = new java.util.ArrayList<>(WaypointManager.getInstance().getWaypoints());
        
        if (mc.player != null) {
            waypoints.sort((a, b) -> {
                double distA = Math.sqrt(Math.pow(a.x - mc.player.getX(), 2) + Math.pow(a.y - mc.player.getY(), 2) + Math.pow(a.z - mc.player.getZ(), 2));
                double distB = Math.sqrt(Math.pow(b.x - mc.player.getX(), 2) + Math.pow(b.y - mc.player.getY(), 2) + Math.pow(b.z - mc.player.getZ(), 2));
                return Double.compare(distA, distB);
            });
        }

        for (Waypoint wp : waypoints) {
            final Waypoint finalWp = wp;
            
            GridRow row = new GridRow(itemWidth, 25);
            
            String displayText = wp.name;
            if (mc.player != null) {
                double dist = Math.sqrt(Math.pow(wp.x - mc.player.getX(), 2) + 
                                      Math.pow(wp.y - mc.player.getY(), 2) + 
                                      Math.pow(wp.z - mc.player.getZ(), 2));
                displayText += String.format(java.util.Locale.ROOT, " (%.1fm)", dist);
            }

            ToggleSwitch enabledToggle = new ToggleSwitch(0, 0, itemWidth - 185, displayText, "", wp.enabled, val -> {
                finalWp.enabled = val;
                WaypointManager.getInstance().save();
            });
            enabledToggle.setLabelColor(wp.color);
            row.addChild(enabledToggle, 0);

            Button editBtn = new Button(0, 0, 50, 20, "Edit", () -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new WaypointEditScreen(screen, finalWp, savedWp -> {
                        WaypointManager.getInstance().save();
                        rebuildList();
                    }));
                });
            });
            row.addChild(editBtn, itemWidth - 180);

            Button actionsBtn = new Button(0, 0, 60, 20, "Actions", () -> {
                java.util.List<WaypointAction> actions = finalWp.actions;
                if (actions.isEmpty()) {
                    actions.add(new WaypointAction());
                }
                mc.setScreen(new WaypointActionEditScreen(screen, finalWp, actions.get(0)));
            });
            row.addChild(actionsBtn, itemWidth - 125);

            Button deleteBtn = new Button(0, 0, 60, 20, "Delete", () -> {
                WaypointManager.getInstance().removeWaypoint(finalWp);
                rebuildList();
            });
            row.addChild(deleteBtn, itemWidth - 60);

            waypointList.addItem(row);
        }
    }
}
