package org.blackum.blackaddons.feature.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.model.DungeonFloor;
import org.blackum.blackaddons.core.util.LocationUtils;
import org.blackum.blackaddons.core.util.ScoreboardUtils;
import org.blackum.blackaddons.core.util.TabListUtils;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;

import org.blackum.blackaddons.feature.chat.ChatUtils;
import net.minecraft.ChatFormatting;
import org.blackum.blackaddons.core.util.DungeonScore;
import org.blackum.blackaddons.integration.BotIntegration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoloClearsTracker {
    private static boolean runRecorded = false;
    private static boolean princeKilledThisRun = false;
    private static int ticks = 0;
    private static String lastLocation = "";


    private static final Pattern SCORE_PATTERN = Pattern.compile("(?i)(?:Score:\\s*(\\d+))|(?:Cleared:.*?\\((\\d+)\\))");
    private static final Pattern TIME_PATTERN = Pattern.compile("(?i)(?:Elapsed|Time|Cleared:.*?\\(\\d+\\))\\s*:?\\s*[^0-9\\s]*\\s*([0-9][0-9:m\\s]*s?)");
    

    private static final Pattern COMPLETED_ROOMS_PATTERN = Pattern.compile("(?i)Completed Rooms:\\s*(\\d+)(?:/|\\s*out\\s*of\\s*)(\\d+)");
    private static final Pattern CRYPTS_PATTERN = Pattern.compile("(?i)Crypts:\\s*(\\d+)");
    private static final Pattern PUZZLES_HEADER_PATTERN = Pattern.compile("(?i)Puzzles:\\s*\\((\\d+)\\)");

    public static void tick() {
        ticks++;
        if (ticks % 10 != 0) return;

        String currentLocation = LocationUtils.getLocation();
        if (!LocationUtils.inDungeons()) {
            runRecorded = false;
            lastLocation = currentLocation;
            princeKilledThisRun = false;
            DungeonScore.reset();
            return;
        }

        if (!lastLocation.equals(currentLocation)) {
            runRecorded = false;
            lastLocation = currentLocation;
            princeKilledThisRun = false;
            DungeonScore.reset();
        }

        DungeonScore.update();
        if (runRecorded) return;

        DungeonFloor floor = LocationUtils.getCurrentFloor();
        if (floor == null) return;
        
        String floorName = floor.getDisplayName();
        if (!floorName.equals("F7") && !floorName.equals("M7")) return;

        List<String> scoreboardLines = ScoreboardUtils.getCleanSidebarLines();
        List<String> tabListLines = TabListUtils.getTabListLines();

        boolean isSolo = false;
        String time = "Unknown";
        
        int sidebarScore = 0;
        for (String line : scoreboardLines) {
            String cleanLine = line.trim();
            if (cleanLine.contains("Solo")) isSolo = true;
            
            Matcher timeMatcher = TIME_PATTERN.matcher(cleanLine);
            if (timeMatcher.find()) {
                time = timeMatcher.group(1).trim();
            }
            
            Matcher scoreMatcher = SCORE_PATTERN.matcher(cleanLine);
            if (scoreMatcher.find()) {
                try {
                    String strScore = scoreMatcher.group(1) != null ? scoreMatcher.group(1) : scoreMatcher.group(2);
                    if (strScore != null) sidebarScore = Integer.parseInt(strScore);
                } catch (NumberFormatException ignored) {}
            }
        }

        for (String line : tabListLines) {
            if (line.trim().contains("Solo")) isSolo = true;
        }

        org.blackum.blackaddons.core.util.DungeonUtils.DungeonStats stats = org.blackum.blackaddons.core.util.DungeonUtils.parseDungeonStats(tabListLines);
        
        int finalScore = DungeonScore.getScore();

        boolean mimicKilled = DungeonScore.isMimicKilled() || stats.mimicKilled;
        boolean princeDefeated = DungeonScore.isPrinceKilled() || stats.princeKilled || princeKilledThisRun;

        // TRIGGER: Manual Score >= 300
        if (finalScore >= 300 && isSolo && !time.equals("Unknown") && !time.equals("00m 00s") && !time.equals("00:00")) {
            List<ConfigManager.SoloClearInfo> floorClears = floorName.equals("M7")
                    ? ConfigManager.data.m7SoloClears : ConfigManager.data.f7SoloClears;

            // Determine if this is a new personal best before adding
            int newTimeSeconds = parseTimeToSeconds(time);
            boolean isNewPB = true;
            if (newTimeSeconds == Integer.MAX_VALUE) {
                isNewPB = false; // can't parse – skip bot submission
            } else {
                for (ConfigManager.SoloClearInfo existing : floorClears) {
                    int existingSeconds = parseTimeToSeconds(existing.time);
                    if (existingSeconds != Integer.MAX_VALUE && existingSeconds <= newTimeSeconds) {
                        isNewPB = false;
                        break;
                    }
                }
            }

            ConfigManager.SoloClearInfo info = new ConfigManager.SoloClearInfo(floorName, time, stats.secretsFound, stats.completedPuzzles, princeDefeated, mimicKilled);
            if (floorName.equals("M7")) {
                ConfigManager.data.m7SoloClears.add(info);
            } else {
                ConfigManager.data.f7SoloClears.add(info);
            }
            ConfigManager.save();
            runRecorded = true;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String colorTime = "§e" + time;
                String puzzleStr = stats.completedPuzzles.isEmpty() ? "None" : String.join(", ", stats.completedPuzzles);
                String princeStr = princeDefeated ? "§a✔" : "§c✘";
                String mimicStr = mimicKilled ? "§a✔" : "§c✘";
                mc.gui.getChat().addClientSystemMessage(ChatUtils.getMessage("§b§l" + floorName + " SOLO CLEAR DONE! §r§fTime: " + colorTime +
                    " §r§fSecrets: §b" + stats.secretsFound + " §r§fPuzzles: §d[" + puzzleStr + "] " +
                    "§r§fPrince: " + princeStr + " §r§fMimic: " + mimicStr));

                if (isNewPB) {
                    final String player = mc.getUser().getName();
                    final String normalizedTime = normalizeTimeForBot(time);
                    final String submittedFloor = floorName;
                    final int submittedSecrets = stats.secretsFound;
                    final List<String> submittedPuzzles = new ArrayList<>(stats.completedPuzzles);
                    final boolean submittedPrince = princeDefeated;
                    final boolean submittedMimic = mimicKilled;
                    final boolean needsVerification = newTimeSeconds < 180;
                    BotIntegration.sendSoloClear(player, submittedFloor, normalizedTime,
                            submittedSecrets, submittedPuzzles, submittedPrince, submittedMimic, needsVerification)
                            .thenAccept(res -> {
                                if (res != null && mc.player != null) {
                                    mc.execute(() -> mc.gui.getChat().addClientSystemMessage(
                                            ChatUtils.getMessage("§a[SoloClears] New PB submitted to leaderboard!")));
                                }
                            });
                }
            }
            NotificationManager.addNotification("Solo Clear", floorName + " Clear Recorded: " + time + " (" + stats.secretsFound + " secrets)", NotificationType.SUCCESS);
        }
    }

    /**
     * Converts sidebar time strings like "4m 20s" or "04m 20s" into "MM:SS"
     * which the bot's parse_time() expects.
     */
    private static String normalizeTimeForBot(String raw) {
        if (raw == null) return "00:00";
        // Already colon format
        if (raw.matches("\\d+:\\d+.*")) return raw;
        // Regex for "Xm Ys" style
        java.util.regex.Matcher m = Pattern.compile("(?:(\\d+)m)?\\s*(?:(\\d+)s)?").matcher(raw);
        if (m.find()) {
            int mins = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int secs = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            return String.format("%02d:%02d", mins, secs);
        }
        return raw;
    }

    private static int parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty() || timeStr.equals("Unknown")) return Integer.MAX_VALUE;
        try {
            if (timeStr.contains("m") || timeStr.contains("s")) {
                Matcher m = Pattern.compile("(?:(\\d+)m)?\\s*(?:(\\d+)s)?").matcher(timeStr);
                if (m.find()) {
                    int mins = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
                    int secs = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
                    int total = mins * 60 + secs;
                    return total <= 0 ? Integer.MAX_VALUE : total;
                }
            } else if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                if (parts.length >= 2) {
                    int total = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                    return total <= 0 ? Integer.MAX_VALUE : total;
                }
            }
        } catch (Exception ignored) {}
        return Integer.MAX_VALUE;
    }

    public static void onChatMessage(Component message) {
        String cleanText = message.getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim();
        if (cleanText.contains("A Prince falls. +1 Bonus Score")) {
            princeKilledThisRun = true;
            DungeonScore.onPrinceKill();
        } else if (cleanText.contains("[BOSS] The Watcher: You have proven yourself. You may pass.")) {
            DungeonScore.onBloodRoomPassed();
        }
    }



    public static void dumpDebugInfo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
 
        mc.gui.getChat().addClientSystemMessage(Component.literal("§b[SoloClears Debug] §fDungeon: " + LocationUtils.inDungeons() + " | Floor: " + LocationUtils.getCurrentFloor()));
        
        List<String> scoreboardLines = ScoreboardUtils.getCleanSidebarLines();
        mc.gui.getChat().addClientSystemMessage(Component.literal("§e--- Sidebar Lines ---"));
        for (String line : scoreboardLines) {
            mc.gui.getChat().addClientSystemMessage(Component.literal("§7- " + line));
        }
 
        List<String> tabListLines = TabListUtils.getTabListLines();
        mc.gui.getChat().addClientSystemMessage(Component.literal("§e--- Tablist Lines (First 20) ---"));
        for (int i = 0; i < Math.min(20, tabListLines.size()); i++) {
            mc.gui.getChat().addClientSystemMessage(Component.literal("§7- " + tabListLines.get(i)));
        }
    }
}
