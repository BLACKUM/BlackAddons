package org.blackum.blackaddons.core.util;

import java.util.HashMap;
import java.util.Map;

public class PetUtils {

    private static final long[] PET_XP_CURVE = new long[101];
    private static final Map<String, Integer> RARITY_OFFSETS = new HashMap<>();

    public static final long GREG_XP_PER_LEVEL = 1825000L;

    static {
        int[] xpPerLevel = {
                0, 100, 155, 160, 245, 340, 500, 600, 700, 800,
                900, 1100, 1200, 1400, 1600, 1700, 1900, 2100, 2300, 2500,
                2700, 3000, 3200, 3400, 3600, 3800, 4000, 4200, 4400, 4600,
                4800, 5200, 5400, 5600, 5800, 6200, 6400, 6600, 6800, 7200,
                7400, 7800, 8000, 8400, 8600, 9000, 9200, 9600, 9800, 10200,
                10400, 10800, 11000, 11400, 11600, 12000, 12200, 12600, 12800, 13200,
                13400, 13800, 14000, 14400, 14600, 15000, 15200, 15600, 15800, 16200,
                16400, 16800, 17000, 17400, 17600, 18000, 18200, 18600, 18800, 19200,
                19400, 19800, 20000, 20400, 20600, 21000, 21200, 21600, 21800, 22200,
                22400, 22800, 23000, 23400, 23600, 24000, 24200, 24600, 24800, 25200
        };

        long total = 0;
        for (int i = 0; i < xpPerLevel.length; i++) {
            total += xpPerLevel[i];
            PET_XP_CURVE[i + 1] = total;
        }

        RARITY_OFFSETS.put("COMMON", 0);
        RARITY_OFFSETS.put("UNCOMMON", 6);
        RARITY_OFFSETS.put("RARE", 11);
        RARITY_OFFSETS.put("EPIC", 16);
        RARITY_OFFSETS.put("LEGENDARY", 20);
        RARITY_OFFSETS.put("MYTHIC", 20);
    }

    public static int getLevel(String type, String rarity, long exp) {
        int offset = RARITY_OFFSETS.getOrDefault(rarity.toUpperCase(), 0);
        int level = 1;
        int maxLevel = ("GOLDEN_DRAGON".equalsIgnoreCase(type) ? 200 : 100);
        long baseExp = getCumulativeExp(offset + 1);

        if (maxLevel == 100) {
            for (int i = 1; i <= 100; i++) {
                if (exp >= getCumulativeExp(offset + i) - baseExp) {
                    level = i;
                } else {
                    break;
                }
            }
        } else {
            for (int i = 1; i <= 100; i++) {
                if (exp >= getCumulativeExp(offset + i) - baseExp) {
                    level = i;
                } else {
                    break;
                }
            }
            if (level == 100) {
                long extraExp = exp - (getCumulativeExp(offset + 100) - baseExp);
                level += (int) (extraExp / GREG_XP_PER_LEVEL);
            }
        }

        return Math.min(level, maxLevel);
    }

    public static float getProgress(String type, String rarity, long exp) {
        int level = getLevel(type, rarity, exp);
        int maxLevel = ("GOLDEN_DRAGON".equalsIgnoreCase(type) ? 200 : 100);
        if (level >= maxLevel)
            return 1.0f;

        int offset = RARITY_OFFSETS.getOrDefault(rarity.toUpperCase(), 0);
        long baseExp = getCumulativeExp(offset + 1);

        if (maxLevel == 200 && level >= 100) {
            long base100 = getCumulativeExp(offset + 100) - baseExp;
            long progressInLevel = (exp - base100) % GREG_XP_PER_LEVEL;
            return (float) progressInLevel / GREG_XP_PER_LEVEL;
        }

        long currentLevelExp = getCumulativeExp(offset + level) - baseExp;
        long nextLevelExp = getCumulativeExp(offset + level + 1) - baseExp;

        if (nextLevelExp <= currentLevelExp)
            return 1.0f;

        return Math.max(0.0f, (float) (exp - currentLevelExp) / (nextLevelExp - currentLevelExp));
    }

    private static long getCumulativeExp(int index) {
        if (index <= 0)
            return 0;
        if (index >= PET_XP_CURVE.length)
            return PET_XP_CURVE[PET_XP_CURVE.length - 1];
        return PET_XP_CURVE[index];
    }

    public static int getRarityColor(String rarity) {
        return switch (rarity.toUpperCase()) {
            case "COMMON" -> 0xFFFFFF;
            case "UNCOMMON" -> 0x55FF55;
            case "RARE" -> 0x5555FF;
            case "EPIC" -> 0xAA00AA;
            case "LEGENDARY" -> 0xFFAA00;
            case "MYTHIC" -> 0xFF55FF;
            case "DIVINE" -> 0x55FFFF;
            default -> 0xFFFFFF;
        };
    }

    public static String getRarityCode(String rarity) {
        return switch (rarity.toUpperCase()) {
            case "COMMON" -> "§f";
            case "UNCOMMON" -> "§a";
            case "RARE" -> "§9";
            case "EPIC" -> "§5";
            case "LEGENDARY" -> "§6";
            case "MYTHIC" -> "§d";
            case "DIVINE" -> "§b";
            default -> "§f";
        };
    }
}
