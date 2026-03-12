package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.mixin.core.KeyBindingAccessor;
import org.blackum.blackaddons.mixin.core.InventoryAccessor;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.core.util.LocationUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

public class AutoTNT {
    private static final List<Block> TARGET_BLOCKS = List.of(
            Blocks.CRACKED_STONE_BRICKS,
            Blocks.SMOOTH_STONE_SLAB,
            Blocks.BARRIER
    );

    private static final double BASE_DISTANCE_LIMIT = 3.3;
    private static final Random RANDOM = new Random();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoTNT::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        if (!ConfigManager.data.autoTntConfig.AutoTNTEnabled || client.player == null || client.level == null) {
            return;
        }
        
        if (client.screen != null || !LocationUtils.inDungeons()) {
            return;
        }

        if (client.hitResult instanceof BlockHitResult blockHit && client.hitResult.getType() == HitResult.Type.BLOCK) {
            if (isTargetBlock(client, blockHit)) {
                if (!blockHit.getBlockPos().equals(ConfigManager.data.autoTntConfig.lastTargetPos)) {
                    ConfigManager.data.autoTntConfig.lastTargetPos = blockHit.getBlockPos();
                    if (true) {
                        ConfigManager.data.autoTntConfig.ticksSinceEquip = 0;
                        ConfigManager.data.autoTntConfig.updateDelays();
                    }
                }

                int tntSlot = findTntHotbarSlot(client.player);
                if (tntSlot != -1) {
                    if (!ConfigManager.data.autoTntConfig.hasClicked) {
                        equipTnt(client.player, tntSlot);
                    }

                    if (ConfigManager.data.autoTntConfig.isTntEquipped) {
                        if (!ConfigManager.data.autoTntConfig.hasClicked) {
                            ConfigManager.data.autoTntConfig.ticksSinceEquip++;
                            if (ConfigManager.data.autoTntConfig.ticksSinceEquip >= ConfigManager.data.autoTntConfig.currentRandomDelay) {
                                triggerAttack(client);
                                ConfigManager.data.autoTntConfig.hasClicked = true;
                                ConfigManager.data.autoTntConfig.ticksSinceClick = 0;
                            }
                        } else if (ConfigManager.data.autoTntConfig.SwapBack) {
                            ConfigManager.data.autoTntConfig.ticksSinceClick++;
                            if (ConfigManager.data.autoTntConfig.ticksSinceClick >= ConfigManager.data.autoTntConfig.currentSwapDelay) {
                                unequipTnt(client.player, false);
                            }
                        }
                    }
                }
            } else {
                handleNotLooking(client.player);
            }
        } else {
            handleNotLooking(client.player);
        }
    }

    private static void handleNotLooking(Player player) {
        if (ConfigManager.data.autoTntConfig.isTntEquipped || ConfigManager.data.autoTntConfig.hasClicked) {
            ConfigManager.data.autoTntConfig.ticksSinceStopLooking++;
            if (ConfigManager.data.autoTntConfig.ticksSinceStopLooking >= ConfigManager.data.autoTntConfig.unequipDelay) {
                if (ConfigManager.data.autoTntConfig.isTntEquipped) {
                    unequipTnt(player, true);
                } else {
                    ConfigManager.data.autoTntConfig.fullReset();
                }
            }
        }
    }

    private static void triggerAttack(Minecraft client) {
        if (client.options.keyAttack instanceof KeyBindingAccessor accessor) {
            KeyMapping.click(accessor.getBoundKey());
        }
    }

    private static boolean isTargetBlock(Minecraft client, BlockHitResult blockHit) {
        double distanceSq = client.player.distanceToSqr(blockHit.getLocation());
        double limit = ConfigManager.data.autoTntConfig.currentDistanceLimit;

        if (distanceSq > (limit * limit))
            return false;

        Block block = client.level.getBlockState(blockHit.getBlockPos()).getBlock();
        return TARGET_BLOCKS.contains(block);
    }

    private static int findTntHotbarSlot(Player player) {
        if (ConfigManager.data.autoTntConfig.lastKnownTntSlot != -1) {
            ItemStack stack = player.getInventory().getItem(ConfigManager.data.autoTntConfig.lastKnownTntSlot);
            if (isTnt(stack))
                return ConfigManager.data.autoTntConfig.lastKnownTntSlot;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isTnt(stack)) {
                ConfigManager.data.autoTntConfig.lastKnownTntSlot = i;
                return i;
            }
        }
        ConfigManager.data.autoTntConfig.lastKnownTntSlot = -1;
        return -1;
    }

    private static boolean isTnt(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        String name = stack.getHoverName().getString().replaceAll("(?i)§[0-9A-FK-OR]", "").toLowerCase();
        return name.contains("superboom") || name.contains("infinityboom");
    }

    private static void equipTnt(Player player, int slot) {
        if (ConfigManager.data.autoTntConfig.isTntEquipped)
            return;
        Minecraft mc = Minecraft.getInstance();
        ConfigManager.data.autoTntConfig.originalItemSlot = ((InventoryAccessor) player.getInventory()).getBlackaddonsSelected();
        
        KeyMapping[] hotbarKeys = mc.options.keyHotbarSlots;
        if (hotbarKeys != null && slot >= 0 && slot < hotbarKeys.length) {
            KeyMapping.click(((KeyBindingAccessor) hotbarKeys[slot]).getBoundKey());
        }
        
        ConfigManager.data.autoTntConfig.isTntEquipped = true;
        ConfigManager.data.autoTntConfig.ticksSinceEquip = 0;
        ConfigManager.data.autoTntConfig.ticksSinceStopLooking = 0;
    }

    private static void unequipTnt(Player player, boolean fullReset) {
        if (!ConfigManager.data.autoTntConfig.isTntEquipped || player == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        KeyMapping[] hotbarKeys = mc.options.keyHotbarSlots;
        
        if (ConfigManager.data.autoTntConfig.SwapBack && ConfigManager.data.autoTntConfig.originalItemSlot != -1) {
            int original = ConfigManager.data.autoTntConfig.originalItemSlot;
            if (original >= 0 && original < 9 && hotbarKeys != null) {
                KeyMapping.click(((KeyBindingAccessor) hotbarKeys[original]).getBoundKey());
            }
        } else {
            int nonTnt = findNonTntHotbarSlot(player);
            if (nonTnt != -1 && hotbarKeys != null) {
                KeyMapping.click(((KeyBindingAccessor) hotbarKeys[nonTnt]).getBoundKey());
            }
        }

        if (fullReset) {
            ConfigManager.data.autoTntConfig.fullReset();
        } else {
            ConfigManager.data.autoTntConfig.reset();
        }
    }

    private static int findNonTntHotbarSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            if (!isTnt(player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    public static List<String> getDebugInfo() {
        java.util.List<String> info = new ArrayList<>();
        if (!ConfigManager.data.autoTntConfig.AutoTNTEnabled)
            return info;

        info.add("");
        info.add(ChatFormatting.RED + "[AutoTNT Debug]");

        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null && client.hitResult instanceof BlockHitResult blockHit
                && client.hitResult.getType() == HitResult.Type.BLOCK) {
            Block block = client.level.getBlockState(blockHit.getBlockPos()).getBlock();
            boolean isTarget = TARGET_BLOCKS.contains(block);
            double dist = client.player.distanceToSqr(blockHit.getLocation());
            info.add("Target: " + (isTarget ? ChatFormatting.GREEN + "YES" : ChatFormatting.RED + "NO") + " "
                    + ChatFormatting.RESET + "("
                    + BuiltInRegistries.BLOCK.getKey(block).getPath() + ")");
            info.add("Distance: " + String.format("%.2f", dist) + " (Limit: "
                    + String.format("%.2f", ConfigManager.data.autoTntConfig.currentDistanceLimit
                            * ConfigManager.data.autoTntConfig.currentDistanceLimit)
                    + ")");
        } else {
            info.add("Target: None");
        }

        info.add("Equipped: " + ConfigManager.data.autoTntConfig.isTntEquipped);
        info.add("Has Clicked: " + ConfigManager.data.autoTntConfig.hasClicked);
        info.add("Ticks Eq: " + ConfigManager.data.autoTntConfig.ticksSinceEquip + " / "
                + ConfigManager.data.autoTntConfig.currentRandomDelay);
        info.add("Ticks Look: " + ConfigManager.data.autoTntConfig.ticksSinceStopLooking + " / "
                + ConfigManager.data.autoTntConfig.unequipDelay);

        return info;
    }

    public static class FeatureConfig {
        // Config
        public boolean AutoTNTEnabled = false;
        public int AutoTNTDelay = 4;
        public int UnequipDelay = 5;

        public boolean SwapBack = false;

        // State
        boolean isTntEquipped = false;
        boolean hasClicked = false;
        int ticksSinceEquip = 0;
        int ticksSinceClick = 0;
        int ticksSinceStopLooking = 0;
        double currentRandomDelay = 0;
        int currentSwapDelay = 3;
        double unequipDelay = 0;
        int originalItemSlot = -1;
        int lastKnownTntSlot = -1;
        BlockPos lastTargetPos = null;
        double currentDistanceLimit = BASE_DISTANCE_LIMIT;

        public FeatureConfig() {
            fullReset();
        }

        void reset() {
            isTntEquipped = false;
            ticksSinceEquip = 0;
            ticksSinceStopLooking = 0;
            originalItemSlot = -1;
            updateDelays();
        }

        void fullReset() {
            reset();
            hasClicked = false;
            ticksSinceClick = 0;
            lastKnownTntSlot = -1;
            lastTargetPos = null;
        }

        void updateDelays() {
            this.currentRandomDelay = AutoTNTDelay + RANDOM.nextInt(2);
            int baseUnequip = UnequipDelay;
            if (baseUnequip > 2) {
                this.unequipDelay = baseUnequip + RANDOM.nextInt(3) - 1;
            } else {
                this.unequipDelay = baseUnequip;
            }
            this.currentSwapDelay = 2 + RANDOM.nextInt(3);
            this.currentDistanceLimit = BASE_DISTANCE_LIMIT + (RANDOM.nextFloat() * 0.06 - 0.03);
        }
    }
}
