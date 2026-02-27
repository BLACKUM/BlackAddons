package org.blackum.blackaddons.core.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.blackum.blackaddons.Blackaddons;
import java.util.HashMap;
import java.util.Map;

public class LegacyItemResolver {
    private static final Map<Integer, Item> ID_MAP = new HashMap<>();

    static {
        // Blocks (0-255)
        ID_MAP.put(1, Items.STONE);
        ID_MAP.put(2, Items.GRASS_BLOCK);
        ID_MAP.put(3, Items.DIRT);
        ID_MAP.put(4, Items.COBBLESTONE);
        ID_MAP.put(5, Items.OAK_PLANKS);
        ID_MAP.put(6, Items.OAK_SAPLING);
        ID_MAP.put(7, Items.BEDROCK);
        ID_MAP.put(8, Items.WATER_BUCKET);
        ID_MAP.put(9, Items.WATER_BUCKET);
        ID_MAP.put(10, Items.LAVA_BUCKET);
        ID_MAP.put(11, Items.LAVA_BUCKET);
        ID_MAP.put(12, Items.SAND);
        ID_MAP.put(13, Items.GRAVEL);
        ID_MAP.put(14, Items.GOLD_ORE);
        ID_MAP.put(15, Items.IRON_ORE);
        ID_MAP.put(16, Items.COAL_ORE);
        ID_MAP.put(17, Items.OAK_LOG);
        ID_MAP.put(18, Items.OAK_LEAVES);
        ID_MAP.put(19, Items.SPONGE);
        ID_MAP.put(20, Items.GLASS);
        ID_MAP.put(21, Items.LAPIS_ORE);
        ID_MAP.put(22, Items.LAPIS_BLOCK);
        ID_MAP.put(23, Items.DISPENSER);
        ID_MAP.put(24, Items.SANDSTONE);
        ID_MAP.put(25, Items.NOTE_BLOCK);
        ID_MAP.put(26, Items.WHITE_BED);
        ID_MAP.put(27, Items.POWERED_RAIL);
        ID_MAP.put(28, Items.DETECTOR_RAIL);
        ID_MAP.put(29, Items.STICKY_PISTON);
        ID_MAP.put(30, Items.COBWEB);
        ID_MAP.put(31, Items.SHORT_GRASS);
        ID_MAP.put(32, Items.DEAD_BUSH);
        ID_MAP.put(33, Items.PISTON);
        ID_MAP.put(35, Items.WHITE_WOOL);
        ID_MAP.put(37, Items.DANDELION);
        ID_MAP.put(38, Items.POPPY);
        ID_MAP.put(39, Items.BROWN_MUSHROOM);
        ID_MAP.put(40, Items.RED_MUSHROOM);
        ID_MAP.put(41, Items.GOLD_BLOCK);
        ID_MAP.put(42, Items.IRON_BLOCK);
        ID_MAP.put(43, Items.STONE_SLAB);
        ID_MAP.put(44, Items.STONE_SLAB);
        ID_MAP.put(45, Items.BRICKS);
        ID_MAP.put(46, Items.TNT);
        ID_MAP.put(47, Items.BOOKSHELF);
        ID_MAP.put(48, Items.MOSSY_COBBLESTONE);
        ID_MAP.put(49, Items.OBSIDIAN);
        ID_MAP.put(50, Items.TORCH);
        ID_MAP.put(51, Items.FIRE_CHARGE);
        ID_MAP.put(52, Items.SPAWNER);
        ID_MAP.put(53, Items.OAK_STAIRS);
        ID_MAP.put(54, Items.CHEST);
        ID_MAP.put(56, Items.DIAMOND_ORE);
        ID_MAP.put(57, Items.DIAMOND_BLOCK);
        ID_MAP.put(58, Items.CRAFTING_TABLE);
        ID_MAP.put(59, Items.WHEAT);
        ID_MAP.put(60, Items.FARMLAND);
        ID_MAP.put(61, Items.FURNACE);
        ID_MAP.put(63, Items.OAK_SIGN);
        ID_MAP.put(64, Items.OAK_DOOR);
        ID_MAP.put(65, Items.LADDER);
        ID_MAP.put(66, Items.RAIL);
        ID_MAP.put(67, Items.STONE_STAIRS);
        ID_MAP.put(68, Items.OAK_SIGN);
        ID_MAP.put(69, Items.LEVER);
        ID_MAP.put(70, Items.STONE_PRESSURE_PLATE);
        ID_MAP.put(71, Items.IRON_DOOR);
        ID_MAP.put(72, Items.OAK_PRESSURE_PLATE);
        ID_MAP.put(73, Items.REDSTONE_ORE);
        ID_MAP.put(76, Items.REDSTONE_TORCH);
        ID_MAP.put(77, Items.STONE_BUTTON);
        ID_MAP.put(78, Items.SNOW);
        ID_MAP.put(79, Items.ICE);
        ID_MAP.put(80, Items.SNOW_BLOCK);
        ID_MAP.put(81, Items.CACTUS);
        ID_MAP.put(82, Items.CLAY);
        ID_MAP.put(83, Items.SUGAR_CANE);
        ID_MAP.put(84, Items.JUKEBOX);
        ID_MAP.put(85, Items.OAK_FENCE);
        ID_MAP.put(86, Items.PUMPKIN);
        ID_MAP.put(87, Items.NETHERRACK);
        ID_MAP.put(88, Items.SOUL_SAND);
        ID_MAP.put(89, Items.GLOWSTONE);
        ID_MAP.put(91, Items.CARVED_PUMPKIN);
        ID_MAP.put(95, Items.WHITE_STAINED_GLASS);
        ID_MAP.put(96, Items.OAK_TRAPDOOR);
        ID_MAP.put(98, Items.STONE_BRICKS);
        ID_MAP.put(101, Items.IRON_BARS);
        ID_MAP.put(102, Items.GLASS_PANE);
        ID_MAP.put(103, Items.MELON);
        ID_MAP.put(106, Items.VINE);
        ID_MAP.put(107, Items.OAK_FENCE_GATE);
        ID_MAP.put(108, Items.BRICK_STAIRS);
        ID_MAP.put(109, Items.STONE_BRICK_STAIRS);
        ID_MAP.put(110, Items.MYCELIUM);
        ID_MAP.put(111, Items.LILY_PAD);
        ID_MAP.put(112, Items.NETHER_BRICKS);
        ID_MAP.put(113, Items.NETHER_BRICK_FENCE);
        ID_MAP.put(114, Items.NETHER_BRICK_STAIRS);
        ID_MAP.put(115, Items.NETHER_WART);
        ID_MAP.put(116, Items.ENCHANTING_TABLE);
        ID_MAP.put(117, Items.BREWING_STAND);
        ID_MAP.put(118, Items.CAULDRON);
        ID_MAP.put(120, Items.END_PORTAL_FRAME);
        ID_MAP.put(121, Items.END_STONE);
        ID_MAP.put(122, Items.DRAGON_EGG);
        ID_MAP.put(123, Items.REDSTONE_LAMP);
        ID_MAP.put(126, Items.OAK_SLAB);
        ID_MAP.put(128, Items.SANDSTONE_STAIRS);
        ID_MAP.put(129, Items.EMERALD_ORE);
        ID_MAP.put(130, Items.ENDER_CHEST);
        ID_MAP.put(131, Items.TRIPWIRE_HOOK);
        ID_MAP.put(133, Items.EMERALD_BLOCK);
        ID_MAP.put(134, Items.SPRUCE_STAIRS);
        ID_MAP.put(135, Items.BIRCH_STAIRS);
        ID_MAP.put(136, Items.JUNGLE_STAIRS);
        ID_MAP.put(138, Items.BEACON);
        ID_MAP.put(139, Items.COBBLESTONE_WALL);
        ID_MAP.put(140, Items.FLOWER_POT);
        ID_MAP.put(143, Items.OAK_BUTTON);
        ID_MAP.put(145, Items.ANVIL);
        ID_MAP.put(146, Items.TRAPPED_CHEST);
        ID_MAP.put(147, Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        ID_MAP.put(148, Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
        ID_MAP.put(151, Items.DAYLIGHT_DETECTOR);
        ID_MAP.put(152, Items.REDSTONE_BLOCK);
        ID_MAP.put(153, Items.NETHER_QUARTZ_ORE);
        ID_MAP.put(154, Items.HOPPER);
        ID_MAP.put(155, Items.QUARTZ_BLOCK);
        ID_MAP.put(156, Items.QUARTZ_STAIRS);
        ID_MAP.put(157, Items.ACTIVATOR_RAIL);
        ID_MAP.put(158, Items.DROPPER);
        ID_MAP.put(159, Items.WHITE_TERRACOTTA);
        ID_MAP.put(160, Items.WHITE_STAINED_GLASS_PANE);
        ID_MAP.put(161, Items.ACACIA_LEAVES);
        ID_MAP.put(162, Items.ACACIA_LOG);
        ID_MAP.put(163, Items.ACACIA_STAIRS);
        ID_MAP.put(164, Items.DARK_OAK_STAIRS);
        ID_MAP.put(165, Items.SLIME_BLOCK);
        ID_MAP.put(166, Items.BARRIER);
        ID_MAP.put(167, Items.IRON_TRAPDOOR);
        ID_MAP.put(168, Items.PRISMARINE);
        ID_MAP.put(169, Items.SEA_LANTERN);
        ID_MAP.put(170, Items.HAY_BLOCK);
        ID_MAP.put(171, Items.WHITE_CARPET);
        ID_MAP.put(172, Items.TERRACOTTA);
        ID_MAP.put(173, Items.COAL_BLOCK);
        ID_MAP.put(174, Items.PACKED_ICE);
        ID_MAP.put(175, Items.SUNFLOWER);

        // Items (256+)
        ID_MAP.put(256, Items.IRON_SHOVEL);
        ID_MAP.put(257, Items.IRON_PICKAXE);
        ID_MAP.put(258, Items.IRON_AXE);
        ID_MAP.put(259, Items.FLINT_AND_STEEL);
        ID_MAP.put(260, Items.APPLE);
        ID_MAP.put(261, Items.BOW);
        ID_MAP.put(262, Items.ARROW);
        ID_MAP.put(263, Items.COAL);
        ID_MAP.put(264, Items.DIAMOND);
        ID_MAP.put(265, Items.IRON_INGOT);
        ID_MAP.put(266, Items.GOLD_INGOT);
        ID_MAP.put(267, Items.IRON_SWORD);
        ID_MAP.put(268, Items.WOODEN_SWORD);
        ID_MAP.put(269, Items.WOODEN_SHOVEL);
        ID_MAP.put(270, Items.WOODEN_PICKAXE);
        ID_MAP.put(271, Items.WOODEN_AXE);
        ID_MAP.put(272, Items.STONE_SWORD);
        ID_MAP.put(273, Items.STONE_SHOVEL);
        ID_MAP.put(274, Items.STONE_PICKAXE);
        ID_MAP.put(275, Items.STONE_AXE);
        ID_MAP.put(276, Items.DIAMOND_SWORD);
        ID_MAP.put(277, Items.DIAMOND_SHOVEL);
        ID_MAP.put(278, Items.DIAMOND_PICKAXE);
        ID_MAP.put(279, Items.DIAMOND_AXE);
        ID_MAP.put(280, Items.STICK);
        ID_MAP.put(281, Items.BOWL);
        ID_MAP.put(282, Items.MUSHROOM_STEW);
        ID_MAP.put(283, Items.GOLDEN_SWORD);
        ID_MAP.put(284, Items.GOLDEN_SHOVEL);
        ID_MAP.put(285, Items.GOLDEN_PICKAXE);
        ID_MAP.put(286, Items.GOLDEN_AXE);
        ID_MAP.put(287, Items.STRING);
        ID_MAP.put(288, Items.FEATHER);
        ID_MAP.put(289, Items.GUNPOWDER);
        ID_MAP.put(290, Items.WOODEN_HOE);
        ID_MAP.put(291, Items.STONE_HOE);
        ID_MAP.put(292, Items.IRON_HOE);
        ID_MAP.put(293, Items.DIAMOND_HOE);
        ID_MAP.put(294, Items.GOLDEN_HOE);
        ID_MAP.put(295, Items.WHEAT_SEEDS);
        ID_MAP.put(296, Items.WHEAT);
        ID_MAP.put(297, Items.BREAD);
        ID_MAP.put(298, Items.LEATHER_HELMET);
        ID_MAP.put(299, Items.LEATHER_CHESTPLATE);
        ID_MAP.put(300, Items.LEATHER_LEGGINGS);
        ID_MAP.put(301, Items.LEATHER_BOOTS);
        ID_MAP.put(302, Items.CHAINMAIL_HELMET);
        ID_MAP.put(303, Items.CHAINMAIL_CHESTPLATE);
        ID_MAP.put(304, Items.CHAINMAIL_LEGGINGS);
        ID_MAP.put(305, Items.CHAINMAIL_BOOTS);
        ID_MAP.put(306, Items.IRON_HELMET);
        ID_MAP.put(307, Items.IRON_CHESTPLATE);
        ID_MAP.put(308, Items.IRON_LEGGINGS);
        ID_MAP.put(309, Items.IRON_BOOTS);
        ID_MAP.put(310, Items.DIAMOND_HELMET);
        ID_MAP.put(311, Items.DIAMOND_CHESTPLATE);
        ID_MAP.put(312, Items.DIAMOND_LEGGINGS);
        ID_MAP.put(313, Items.DIAMOND_BOOTS);
        ID_MAP.put(314, Items.GOLDEN_HELMET);
        ID_MAP.put(315, Items.GOLDEN_CHESTPLATE);
        ID_MAP.put(316, Items.GOLDEN_LEGGINGS);
        ID_MAP.put(317, Items.GOLDEN_BOOTS);
        ID_MAP.put(318, Items.FLINT);
        ID_MAP.put(319, Items.PORKCHOP);
        ID_MAP.put(320, Items.COOKED_PORKCHOP);
        ID_MAP.put(321, Items.PAINTING);
        ID_MAP.put(322, Items.GOLDEN_APPLE);
        ID_MAP.put(323, Items.OAK_SIGN);
        ID_MAP.put(324, Items.OAK_DOOR);
        ID_MAP.put(325, Items.BUCKET);
        ID_MAP.put(326, Items.WATER_BUCKET);
        ID_MAP.put(327, Items.LAVA_BUCKET);
        ID_MAP.put(328, Items.MINECART);
        ID_MAP.put(329, Items.SADDLE);
        ID_MAP.put(330, Items.IRON_DOOR);
        ID_MAP.put(331, Items.REDSTONE);
        ID_MAP.put(332, Items.SNOWBALL);
        ID_MAP.put(333, Items.OAK_BOAT);
        ID_MAP.put(334, Items.LEATHER);
        ID_MAP.put(335, Items.MILK_BUCKET);
        ID_MAP.put(336, Items.BRICK);
        ID_MAP.put(337, Items.CLAY_BALL);
        ID_MAP.put(338, Items.SUGAR_CANE);
        ID_MAP.put(339, Items.PAPER);
        ID_MAP.put(340, Items.BOOK);
        ID_MAP.put(341, Items.SLIME_BALL);
        ID_MAP.put(342, Items.CHEST_MINECART);
        ID_MAP.put(343, Items.FURNACE_MINECART);
        ID_MAP.put(344, Items.EGG);
        ID_MAP.put(345, Items.COMPASS);
        ID_MAP.put(346, Items.FISHING_ROD);
        ID_MAP.put(347, Items.CLOCK);
        ID_MAP.put(348, Items.GLOWSTONE_DUST);
        ID_MAP.put(349, Items.COD);
        ID_MAP.put(350, Items.COOKED_COD);
        ID_MAP.put(351, Items.INK_SAC);
        ID_MAP.put(352, Items.BONE);
        ID_MAP.put(353, Items.SUGAR);
        ID_MAP.put(354, Items.CAKE);
        ID_MAP.put(355, Items.WHITE_BED);
        ID_MAP.put(356, Items.REPEATER);
        ID_MAP.put(357, Items.COOKIE);
        ID_MAP.put(358, Items.FILLED_MAP);
        ID_MAP.put(359, Items.SHEARS);
        ID_MAP.put(360, Items.MELON_SLICE);
        ID_MAP.put(361, Items.PUMPKIN_SEEDS);
        ID_MAP.put(362, Items.MELON_SEEDS);
        ID_MAP.put(363, Items.BEEF);
        ID_MAP.put(364, Items.COOKED_BEEF);
        ID_MAP.put(365, Items.CHICKEN);
        ID_MAP.put(366, Items.COOKED_CHICKEN);
        ID_MAP.put(367, Items.ROTTEN_FLESH);
        ID_MAP.put(368, Items.ENDER_PEARL);
        ID_MAP.put(369, Items.BLAZE_ROD);
        ID_MAP.put(370, Items.GHAST_TEAR);
        ID_MAP.put(371, Items.GOLD_NUGGET);
        ID_MAP.put(372, Items.NETHER_WART);
        ID_MAP.put(373, Items.POTION);
        ID_MAP.put(374, Items.GLASS_BOTTLE);
        ID_MAP.put(375, Items.SPIDER_EYE);
        ID_MAP.put(376, Items.FERMENTED_SPIDER_EYE);
        ID_MAP.put(377, Items.BLAZE_POWDER);
        ID_MAP.put(378, Items.MAGMA_CREAM);
        ID_MAP.put(379, Items.BREWING_STAND);
        ID_MAP.put(380, Items.CAULDRON);
        ID_MAP.put(381, Items.ENDER_EYE);
        ID_MAP.put(382, Items.GLISTERING_MELON_SLICE);
        ID_MAP.put(383, Items.PIG_SPAWN_EGG);
        ID_MAP.put(384, Items.EXPERIENCE_BOTTLE);
        ID_MAP.put(385, Items.FIRE_CHARGE);
        ID_MAP.put(386, Items.WRITABLE_BOOK);
        ID_MAP.put(387, Items.WRITTEN_BOOK);
        ID_MAP.put(388, Items.EMERALD);
        ID_MAP.put(389, Items.ITEM_FRAME);
        ID_MAP.put(390, Items.FLOWER_POT);
        ID_MAP.put(391, Items.CARROT);
        ID_MAP.put(392, Items.POTATO);
        ID_MAP.put(393, Items.BAKED_POTATO);
        ID_MAP.put(394, Items.POISONOUS_POTATO);
        ID_MAP.put(395, Items.MAP);
        ID_MAP.put(396, Items.GOLDEN_CARROT);
        ID_MAP.put(397, Items.PLAYER_HEAD);
        ID_MAP.put(398, Items.CARROT_ON_A_STICK);
        ID_MAP.put(399, Items.NETHER_STAR);
        ID_MAP.put(400, Items.PUMPKIN_PIE);
        ID_MAP.put(401, Items.FIREWORK_STAR);
        ID_MAP.put(402, Items.FIREWORK_ROCKET);
        ID_MAP.put(403, Items.ENCHANTED_BOOK);
        ID_MAP.put(404, Items.COMPARATOR);
        ID_MAP.put(405, Items.NETHER_BRICK);
        ID_MAP.put(406, Items.QUARTZ);
        ID_MAP.put(407, Items.TNT_MINECART);
        ID_MAP.put(408, Items.HOPPER_MINECART);
        ID_MAP.put(409, Items.PRISMARINE_SHARD);
        ID_MAP.put(410, Items.PRISMARINE_CRYSTALS);
        ID_MAP.put(411, Items.RABBIT);
        ID_MAP.put(412, Items.COOKED_RABBIT);
        ID_MAP.put(413, Items.RABBIT_STEW);
        ID_MAP.put(414, Items.RABBIT_FOOT);
        ID_MAP.put(415, Items.RABBIT_HIDE);
        ID_MAP.put(416, Items.ARMOR_STAND);
        ID_MAP.put(417, Items.IRON_HORSE_ARMOR);
        ID_MAP.put(418, Items.GOLDEN_HORSE_ARMOR);
        ID_MAP.put(419, Items.DIAMOND_HORSE_ARMOR);
        ID_MAP.put(420, Items.LEAD);
        ID_MAP.put(421, Items.NAME_TAG);
        ID_MAP.put(425, Items.WHITE_BANNER);
        ID_MAP.put(2256, Items.MUSIC_DISC_13);
        ID_MAP.put(2258, Items.MUSIC_DISC_CAT);
        ID_MAP.put(2262, Items.MUSIC_DISC_BLOCKS);
    }

    public static Item resolve(int id, int damage) {
        if (id == 1) { // Stone
            return switch (damage) {
                case 1 -> Items.GRANITE;
                case 2 -> Items.POLISHED_GRANITE;
                case 3 -> Items.DIORITE;
                case 4 -> Items.POLISHED_DIORITE;
                case 5 -> Items.ANDESITE;
                case 6 -> Items.POLISHED_ANDESITE;
                default -> Items.STONE;
            };
        }
        if (id == 3) { // Dirt
            return switch (damage) {
                case 1 -> Items.COARSE_DIRT;
                case 2 -> Items.PODZOL;
                default -> Items.DIRT;
            };
        }
        if (id == 5) { // Planks
            return switch (damage) {
                case 1 -> Items.SPRUCE_PLANKS;
                case 2 -> Items.BIRCH_PLANKS;
                case 3 -> Items.JUNGLE_PLANKS;
                case 4 -> Items.ACACIA_PLANKS;
                case 5 -> Items.DARK_OAK_PLANKS;
                default -> Items.OAK_PLANKS;
            };
        }
        if (id == 6) { // Saplings
            return switch (damage) {
                case 1 -> Items.SPRUCE_SAPLING;
                case 2 -> Items.BIRCH_SAPLING;
                case 3 -> Items.JUNGLE_SAPLING;
                case 4 -> Items.ACACIA_SAPLING;
                case 5 -> Items.DARK_OAK_SAPLING;
                default -> Items.OAK_SAPLING;
            };
        }
        if (id == 12) { // Sand
            return switch (damage) {
                case 1 -> Items.RED_SAND;
                default -> Items.SAND;
            };
        }
        if (id == 17) { // Log
            return switch (damage & 3) {
                case 1 -> Items.SPRUCE_LOG;
                case 2 -> Items.BIRCH_LOG;
                case 3 -> Items.JUNGLE_LOG;
                default -> Items.OAK_LOG;
            };
        }
        if (id == 18) { // Leaves
            return switch (damage & 3) {
                case 1 -> Items.SPRUCE_LEAVES;
                case 2 -> Items.BIRCH_LEAVES;
                case 3 -> Items.JUNGLE_LEAVES;
                default -> Items.OAK_LEAVES;
            };
        }
        if (id == 19) { // Sponge
            return switch (damage) {
                case 1 -> Items.WET_SPONGE;
                default -> Items.SPONGE;
            };
        }
        if (id == 24) { // Sandstone
            return switch (damage) {
                case 1 -> Items.CHISELED_SANDSTONE;
                case 2 -> Items.SMOOTH_SANDSTONE;
                default -> Items.SANDSTONE;
            };
        }
        if (id == 31) { // Short Grass
            return switch (damage) {
                case 2 -> Items.FERN;
                default -> Items.SHORT_GRASS;
            };
        }
        if (id == 35) { // Wool
            return getColorItem(damage, "WOOL");
        }
        if (id == 38) { // Flowers
            return switch (damage) {
                case 1 -> Items.BLUE_ORCHID;
                case 2 -> Items.ALLIUM;
                case 3 -> Items.AZURE_BLUET;
                case 4 -> Items.RED_TULIP;
                case 5 -> Items.ORANGE_TULIP;
                case 6 -> Items.WHITE_TULIP;
                case 7 -> Items.PINK_TULIP;
                case 8 -> Items.OXEYE_DAISY;
                default -> Items.POPPY;
            };
        }
        if (id == 44) { // Slabs
            return switch (damage) {
                case 1 -> Items.SANDSTONE_SLAB;
                case 2 -> Items.OAK_SLAB;
                case 3 -> Items.COBBLESTONE_SLAB;
                case 4 -> Items.BRICK_SLAB;
                case 5 -> Items.STONE_BRICK_SLAB;
                case 6 -> Items.NETHER_BRICK_SLAB;
                case 7 -> Items.QUARTZ_SLAB;
                default -> Items.STONE_SLAB;
            };
        }
        if (id == 95) { // Stained Glass
            return getColorItem(damage, "STAINED_GLASS");
        }
        if (id == 98) { // Stone Bricks
            return switch (damage) {
                case 1 -> Items.MOSSY_STONE_BRICKS;
                case 2 -> Items.CRACKED_STONE_BRICKS;
                case 3 -> Items.CHISELED_STONE_BRICKS;
                default -> Items.STONE_BRICKS;
            };
        }
        if (id == 126) { // Wood Slabs
            return switch (damage) {
                case 1 -> Items.SPRUCE_SLAB;
                case 2 -> Items.BIRCH_SLAB;
                case 3 -> Items.JUNGLE_SLAB;
                case 4 -> Items.ACACIA_SLAB;
                case 5 -> Items.DARK_OAK_SLAB;
                default -> Items.OAK_SLAB;
            };
        }
        if (id == 139) { // Walls
            return switch (damage) {
                case 1 -> Items.MOSSY_COBBLESTONE_WALL;
                default -> Items.COBBLESTONE_WALL;
            };
        }
        if (id == 155) { // Quartz
            return switch (damage) {
                case 1 -> Items.CHISELED_QUARTZ_BLOCK;
                case 2 -> Items.QUARTZ_PILLAR;
                default -> Items.QUARTZ_BLOCK;
            };
        }
        if (id == 159) { // Terracotta
            return getColorItem(damage, "TERRACOTTA");
        }
        if (id == 160) { // Stained Glass Pane
            return getColorItem(damage, "STAINED_GLASS_PANE");
        }
        if (id == 161) { // Leaves 2
            return switch (damage & 3) {
                case 1 -> Items.DARK_OAK_LEAVES;
                default -> Items.ACACIA_LEAVES;
            };
        }
        if (id == 162) { // Log 2
            return switch (damage & 3) {
                case 1 -> Items.DARK_OAK_LOG;
                default -> Items.ACACIA_LOG;
            };
        }
        if (id == 168) { // Prismarine
            return switch (damage) {
                case 1 -> Items.PRISMARINE_BRICKS;
                case 2 -> Items.DARK_PRISMARINE;
                default -> Items.PRISMARINE;
            };
        }
        if (id == 171) { // Carpet
            return getColorItem(damage, "CARPET");
        }
        if (id == 175) { // Double plants
            return switch (damage) {
                case 1 -> Items.LILAC;
                case 2 -> Items.TALL_GRASS;
                case 3 -> Items.LARGE_FERN;
                case 4 -> Items.ROSE_BUSH;
                case 5 -> Items.PEONY;
                default -> Items.SUNFLOWER;
            };
        }
        if (id == 263) { // Coal
            return switch (damage) {
                case 1 -> Items.CHARCOAL;
                default -> Items.COAL;
            };
        }
        if (id == 322) { // Golden Apple
            return switch (damage) {
                case 1 -> Items.ENCHANTED_GOLDEN_APPLE;
                default -> Items.GOLDEN_APPLE;
            };
        }
        if (id == 349) { // Fish
            return switch (damage) {
                case 1 -> Items.SALMON;
                case 2 -> Items.TROPICAL_FISH;
                case 3 -> Items.PUFFERFISH;
                default -> Items.COD;
            };
        }
        if (id == 350) { // Cooked Fish
            return switch (damage) {
                case 1 -> Items.COOKED_SALMON;
                default -> Items.COOKED_COD;
            };
        }
        if (id == 351) { // Dye logic
            return switch (damage) {
                case 1 -> Items.RED_DYE;
                case 2 -> Items.GREEN_DYE;
                case 3 -> Items.COCOA_BEANS;
                case 4 -> Items.LAPIS_LAZULI;
                case 5 -> Items.PURPLE_DYE;
                case 6 -> Items.CYAN_DYE;
                case 7 -> Items.LIGHT_GRAY_DYE;
                case 8 -> Items.GRAY_DYE;
                case 9 -> Items.PINK_DYE;
                case 10 -> Items.LIME_DYE;
                case 11 -> Items.YELLOW_DYE;
                case 12 -> Items.LIGHT_BLUE_DYE;
                case 13 -> Items.MAGENTA_DYE;
                case 14 -> Items.ORANGE_DYE;
                case 15 -> Items.BONE_MEAL;
                default -> Items.INK_SAC;
            };
        }
        if (id == 425) { // Banner
            return getColorItem(damage, "BANNER");
        }

        Item item = ID_MAP.get(id);
        if (item == null && id != 0) {
            Blackaddons.LOGGER.warn("Unknown Legacy ID: {}", id);
            return Items.BARRIER;
        }
        return item != null ? item : Items.AIR;
    }

    private static Item getColorItem(int color, String suffix) {
        return switch (color) {
            case 1 -> getItem(suffix, "ORANGE");
            case 2 -> getItem(suffix, "MAGENTA");
            case 3 -> getItem(suffix, "LIGHT_BLUE");
            case 4 -> getItem(suffix, "YELLOW");
            case 5 -> getItem(suffix, "LIME");
            case 6 -> getItem(suffix, "PINK");
            case 7 -> getItem(suffix, "GRAY");
            case 8 -> getItem(suffix, "LIGHT_GRAY");
            case 9 -> getItem(suffix, "CYAN");
            case 10 -> getItem(suffix, "PURPLE");
            case 11 -> getItem(suffix, "BLUE");
            case 12 -> getItem(suffix, "BROWN");
            case 13 -> getItem(suffix, "GREEN");
            case 14 -> getItem(suffix, "RED");
            case 15 -> getItem(suffix, "BLACK");
            default -> getItem(suffix, "WHITE");
        };
    }

    private static Item getItem(String suffix, String color) {
        return switch (suffix) {
            case "WOOL" -> switch (color) {
                case "ORANGE" -> Items.ORANGE_WOOL;
                case "MAGENTA" -> Items.MAGENTA_WOOL;
                case "LIGHT_BLUE" -> Items.LIGHT_BLUE_WOOL;
                case "YELLOW" -> Items.YELLOW_WOOL;
                case "LIME" -> Items.LIME_WOOL;
                case "PINK" -> Items.PINK_WOOL;
                case "GRAY" -> Items.GRAY_WOOL;
                case "LIGHT_GRAY" -> Items.LIGHT_GRAY_WOOL;
                case "CYAN" -> Items.CYAN_WOOL;
                case "PURPLE" -> Items.PURPLE_WOOL;
                case "BLUE" -> Items.BLUE_WOOL;
                case "BROWN" -> Items.BROWN_WOOL;
                case "GREEN" -> Items.GREEN_WOOL;
                case "RED" -> Items.RED_WOOL;
                case "BLACK" -> Items.BLACK_WOOL;
                default -> Items.WHITE_WOOL;
            };
            case "STAINED_GLASS" -> switch (color) {
                case "ORANGE" -> Items.ORANGE_STAINED_GLASS;
                case "MAGENTA" -> Items.MAGENTA_STAINED_GLASS;
                case "LIGHT_BLUE" -> Items.LIGHT_BLUE_STAINED_GLASS;
                case "YELLOW" -> Items.YELLOW_STAINED_GLASS;
                case "LIME" -> Items.LIME_STAINED_GLASS;
                case "PINK" -> Items.PINK_STAINED_GLASS;
                case "GRAY" -> Items.GRAY_STAINED_GLASS;
                case "LIGHT_GRAY" -> Items.LIGHT_GRAY_STAINED_GLASS;
                case "CYAN" -> Items.CYAN_STAINED_GLASS;
                case "PURPLE" -> Items.PURPLE_STAINED_GLASS;
                case "BLUE" -> Items.BLUE_STAINED_GLASS;
                case "BROWN" -> Items.BROWN_STAINED_GLASS;
                case "GREEN" -> Items.GREEN_STAINED_GLASS;
                case "RED" -> Items.RED_STAINED_GLASS;
                case "BLACK" -> Items.BLACK_STAINED_GLASS;
                default -> Items.WHITE_STAINED_GLASS;
            };
            case "TERRACOTTA" -> switch (color) {
                case "ORANGE" -> Items.ORANGE_TERRACOTTA;
                case "MAGENTA" -> Items.MAGENTA_TERRACOTTA;
                case "LIGHT_BLUE" -> Items.LIGHT_BLUE_TERRACOTTA;
                case "YELLOW" -> Items.YELLOW_TERRACOTTA;
                case "LIME" -> Items.LIME_TERRACOTTA;
                case "PINK" -> Items.PINK_TERRACOTTA;
                case "GRAY" -> Items.GRAY_TERRACOTTA;
                case "LIGHT_GRAY" -> Items.LIGHT_GRAY_TERRACOTTA;
                case "CYAN" -> Items.CYAN_TERRACOTTA;
                case "PURPLE" -> Items.PURPLE_TERRACOTTA;
                case "BLUE" -> Items.BLUE_TERRACOTTA;
                case "BROWN" -> Items.BROWN_TERRACOTTA;
                case "GREEN" -> Items.GREEN_TERRACOTTA;
                case "RED" -> Items.RED_TERRACOTTA;
                case "BLACK" -> Items.BLACK_TERRACOTTA;
                default -> Items.WHITE_TERRACOTTA;
            };
            case "STAINED_GLASS_PANE" -> switch (color) {
                case "ORANGE" -> Items.ORANGE_STAINED_GLASS_PANE;
                case "MAGENTA" -> Items.MAGENTA_STAINED_GLASS_PANE;
                case "LIGHT_BLUE" -> Items.LIGHT_BLUE_STAINED_GLASS_PANE;
                case "YELLOW" -> Items.YELLOW_STAINED_GLASS_PANE;
                case "LIME" -> Items.LIME_STAINED_GLASS_PANE;
                case "PINK" -> Items.PINK_STAINED_GLASS_PANE;
                case "GRAY" -> Items.GRAY_STAINED_GLASS_PANE;
                case "LIGHT_GRAY" -> Items.LIGHT_GRAY_STAINED_GLASS_PANE;
                case "CYAN" -> Items.CYAN_STAINED_GLASS_PANE;
                case "PURPLE" -> Items.PURPLE_STAINED_GLASS_PANE;
                case "BLUE" -> Items.BLUE_STAINED_GLASS_PANE;
                case "BROWN" -> Items.BROWN_STAINED_GLASS_PANE;
                case "GREEN" -> Items.GREEN_STAINED_GLASS_PANE;
                case "RED" -> Items.RED_STAINED_GLASS_PANE;
                case "BLACK" -> Items.BLACK_STAINED_GLASS_PANE;
                default -> Items.WHITE_STAINED_GLASS_PANE;
            };
            case "CARPET" -> switch (color) {
                case "ORANGE" -> Items.ORANGE_CARPET;
                case "MAGENTA" -> Items.MAGENTA_CARPET;
                case "LIGHT_BLUE" -> Items.LIGHT_BLUE_CARPET;
                case "YELLOW" -> Items.YELLOW_CARPET;
                case "LIME" -> Items.LIME_CARPET;
                case "PINK" -> Items.PINK_CARPET;
                case "GRAY" -> Items.GRAY_CARPET;
                case "LIGHT_GRAY" -> Items.LIGHT_GRAY_CARPET;
                case "CYAN" -> Items.CYAN_CARPET;
                case "PURPLE" -> Items.PURPLE_CARPET;
                case "BLUE" -> Items.BLUE_CARPET;
                case "BROWN" -> Items.BROWN_CARPET;
                case "GREEN" -> Items.GREEN_CARPET;
                case "RED" -> Items.RED_CARPET;
                case "BLACK" -> Items.BLACK_CARPET;
                default -> Items.WHITE_CARPET;
            };
            case "BANNER" -> switch (color) {
                case "ORANGE" -> Items.ORANGE_BANNER;
                case "MAGENTA" -> Items.MAGENTA_BANNER;
                case "LIGHT_BLUE" -> Items.LIGHT_BLUE_BANNER;
                case "YELLOW" -> Items.YELLOW_BANNER;
                case "LIME" -> Items.LIME_BANNER;
                case "PINK" -> Items.PINK_BANNER;
                case "GRAY" -> Items.GRAY_BANNER;
                case "LIGHT_GRAY" -> Items.LIGHT_GRAY_BANNER;
                case "CYAN" -> Items.CYAN_BANNER;
                case "PURPLE" -> Items.PURPLE_BANNER;
                case "BLUE" -> Items.BLUE_BANNER;
                case "BROWN" -> Items.BROWN_BANNER;
                case "GREEN" -> Items.GREEN_BANNER;
                case "RED" -> Items.RED_BANNER;
                case "BLACK" -> Items.BLACK_BANNER;
                default -> Items.WHITE_BANNER;
            };
            default -> Items.AIR;
        };
    }
}
