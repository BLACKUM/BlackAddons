package org.blackum.blackaddons.core.util;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.model.DungeonFloor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationUtils {
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int LINE_HEIGHT = 10;

    private static final Map<Integer, int[]> BOSS_ROOM_BOUNDS = Map.of(
        7, new int[]{-8, 0, -8, 134, 254, 147},
        6, new int[]{-40, 51, -8, 22, 110, 134},
        5, new int[]{-40, 112, -8, 50, 53, 118},
        4, new int[]{-40, 112, -40, 50, 53, 47},
        3, new int[]{-40, 118, -40, 42, 64, 31},
        2, new int[]{-40, 99, -40, 24, 54, 59},
        1, new int[]{-14, 55, 49, -72, 146, -40}
    );

    public static String getLocation() {
        List<String> lines = ScoreboardUtils.getCleanSidebarLines();
        for (String line : lines) {
            if (line.contains("⏣")) {
                return line.replace("⏣", "").trim();
            }
        }
        return "Unknown";
    }

    public static boolean inSkyblock() {
        return !ScoreboardUtils.getCleanSidebarLines().isEmpty() && !getLocation().equals("Unknown");
    }

    public static boolean inDungeons() {
        String loc = getLocation();
        for (int i = 1; i <= 7; i++) {
            if (loc.contains("(F" + i + ")") || loc.contains("(M" + i + ")")) {
                return true;
            }
        }
        return loc.contains("(E)") || loc.contains("Catacombs");
    }

    public static DungeonFloor getCurrentFloor() {
        String loc = getLocation();
        for (DungeonFloor floor : DungeonFloor.values()) {
            String name = floor.getDisplayName();
            if (loc.contains("(" + name + ")")) {
                return floor;
            }
        }
        if (loc.contains("(E)") || loc.startsWith("Catacombs")) {
            return DungeonFloor.ENTRANCE;
        }
        return null;
    }

    public static boolean inBoss() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        DungeonFloor floor = getCurrentFloor();
        if (floor == null) return false;

        int floorNum;
        String display = floor.getDisplayName();
        if (display.equals("Entrance")) return false;
        char letter = display.charAt(0);
        try {
            floorNum = Integer.parseInt(display.substring(1));
        } catch (NumberFormatException e) {
            return false;
        }

        int[] bounds = BOSS_ROOM_BOUNDS.get(floorNum);
        if (bounds == null) return false;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        int minX = Math.min(bounds[0], bounds[3]);
        int maxX = Math.max(bounds[0], bounds[3]);
        int minY = Math.min(bounds[1], bounds[4]);
        int maxY = Math.max(bounds[1], bounds[4]);
        int minZ = Math.min(bounds[2], bounds[5]);
        int maxZ = Math.max(bounds[2], bounds[5]);

        return px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ;
    }

    public static int getF7Phase() {
        DungeonFloor floor = getCurrentFloor();
        if (floor == null) return 0;
        String name = floor.getDisplayName();
        if (!name.equals("F7") && !name.equals("M7")) return 0;
        if (!inBoss()) return 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        double y = mc.player.getY();

        if (y > 210) return 1;
        if (y > 155) return 2;
        if (y > 100) return 3;
        if (y > 45) return 4;
        return 5;
    }

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, partialTick) -> renderOverlay(graphics));
    }

    public static List<String> getDebugInfo() {
        List<String> info = new ArrayList<>();
        if (!ConfigManager.data.showLocationDebug) return info;

        Minecraft mc = Minecraft.getInstance();
        info.add("");
        info.add(ChatFormatting.GOLD + "[Location Utils]");
        info.add("Location: " + getLocation());
        info.add("SkyBlock: " + yesNo(inSkyblock()));
        info.add("Dungeons: " + yesNo(inDungeons()));

        DungeonFloor floor = getCurrentFloor();
        info.add("Floor: " + (floor != null ? floor.getDisplayName() : "None"));

        boolean inBoss = inBoss();
        info.add("Boss: " + yesNo(inBoss));

        int f7Phase = getF7Phase();
        info.add("F7 Phase: " + (f7Phase > 0 ? f7Phase : "N/A"));

        if (mc.player != null) {
            Vec3 pos = mc.player.position();
            info.add(String.format(Locale.US, "Player: %.1f %.1f %.1f", pos.x, pos.y, pos.z));
        }

        if (floor != null && floor != DungeonFloor.ENTRANCE) {
            int[] bounds = getBossBounds(floor);
            if (bounds != null) {
                info.add(String.format(Locale.US, "Boss Box: (%d, %d, %d) -> (%d, %d, %d)",
                        bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]));
            }
        }

        return info;
    }

    private static void renderOverlay(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!ConfigManager.data.showLocationDebug || mc.options.hideGui) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int overlayX = ConfigManager.data.locationOverlayX < 0
                ? screenW - 170
                : ConfigManager.data.locationOverlayX;
        int overlayY = ConfigManager.data.locationOverlayY < 0
                ? 65
                : ConfigManager.data.locationOverlayY;

        int y = overlayY;
        for (String line : getDebugInfo()) {
            graphics.drawString(mc.font, line, overlayX, y, COLOR_WHITE);
            y += LINE_HEIGHT;
        }
    }

    private static String yesNo(boolean value) {
        return value ? ChatFormatting.GREEN + "YES" : ChatFormatting.RED + "NO";
    }

    private static int[] getBossBounds(DungeonFloor floor) {
        String display = floor.getDisplayName();
        if (display.equals("Entrance")) return null;
        try {
            return BOSS_ROOM_BOUNDS.get(Integer.parseInt(display.substring(1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
