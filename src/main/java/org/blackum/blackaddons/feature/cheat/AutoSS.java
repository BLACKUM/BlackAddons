package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.RotationManager;
import org.blackum.blackaddons.core.util.LocationUtils;
import org.blackum.blackaddons.feature.chat.TriggerActionExecutor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AutoSS {
    private static final BlockPos startButton = new BlockPos(110, 121, 91);
    private static final BlockPos buttonCheckPos = new BlockPos(110, 120, 92);
    private static final BlockPos startPos = new BlockPos(111, 120, 92);
    
    private static final Pattern startRegex = Pattern.compile("^\\[BOSS\\] Goldor: Who dares trespass into my domain\\?$");
    
    private static final float TARGET_X_OFFSET = 0.50f;
    private static final float TARGET_Y_OFFSET = -0.05f;
    private static final float TARGET_Z_OFFSET = 0.00f;

    private static final List<BlockPos> solution = new ArrayList<>();
    private static final List<BlockPos> solverQueue = new ArrayList<>();
    private static boolean lastExisted = false;
    private static boolean skipOver = false;
    private static boolean allObi = true;
    private static boolean hasReturnPoint = false;

    private static boolean isSolving = false;
    private static boolean isPreAiming = false;
    private static boolean settingsOverridden = false;
    private static int solvingIndex = 0;
    private static int delayTicksRemaining = 0;
    private static boolean waitingForRotation = false;
    private static float originalRandomness = 0f;
    private static float originalSpeed = 0f;
    private static float originalCurve = 0f;

    private static long lastClick = 0L;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoSS::onClientTick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleChatMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleChatMessage(message));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            handleClick(hitResult.getBlockPos());
            return InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            handleClick(pos);
            return InteractionResult.PASS;
        });
    }

    private static void handleChatMessage(Component message) {
        if (!LocationUtils.inDungeons()) return;
        String text = message.getString();
        if (text == null) return;

        if (startRegex.matcher(text).matches()) {
            resetSolver();
        }
    }

    private static void onClientTick(Minecraft client) {
        if (!ConfigManager.data.AutoSSEnabled || client.player == null || client.level == null) return;
        if (!LocationUtils.inDungeons()) return;

        boolean buttonsExist = client.level.getBlockState(buttonCheckPos).getBlock() == Blocks.STONE_BUTTON;

        if (isSolving) {
            if (!buttonsExist || solverQueue.isEmpty() || solvingIndex >= solverQueue.size()) {
                endSolving();
                return;
            }

            if (delayTicksRemaining > 0) {
                delayTicksRemaining--;
                if (delayTicksRemaining == 0) {
                    if (solvingIndex < solverQueue.size()) {
                        RotationManager.getInstance().advanceSpline();
                        waitingForRotation = true;
                    } else {
                        endSolving();
                    }
                }
                return;
            }

            if (solvingIndex >= solverQueue.size()) {
                endSolving();
                return;
            }

            if (waitingForRotation) {
                boolean lookingAtButton = false;
                boolean isReturnPoint = hasReturnPoint && (solvingIndex == solverQueue.size() - 1);

                if (client.hitResult != null && client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    BlockPos hitBlock = ((net.minecraft.world.phys.BlockHitResult) client.hitResult).getBlockPos();
                    if (solvingIndex < solverQueue.size() && !isReturnPoint) {
                        BlockPos expectedLantern = solverQueue.get(solvingIndex);
                        if (hitBlock.equals(expectedLantern.west())) {
                            lookingAtButton = true;
                        }
                    }
                }

                if (lookingAtButton || RotationManager.getInstance().isAtSplineNode()) {
                    if (!isReturnPoint) {
                        List<ConfigManager.TriggerAction> actions = new java.util.ArrayList<>();
                        actions.add(new ConfigManager.TriggerAction(ConfigManager.TriggerActionType.USE_ITEM, 0, "", 1, 0));
                        TriggerActionExecutor.getInstance().execute(actions, null);
                        delayTicksRemaining = Math.max(2, ConfigManager.data.AutoSSDelay);
                    } else {
                        delayTicksRemaining = 20;
                    }

                    solvingIndex++;
                    waitingForRotation = false;
                }
                return;
            }

            if (solvingIndex < solverQueue.size()) {
                if (!waitingForRotation) {
                    List<net.minecraft.world.phys.Vec3> splinePoints = new java.util.ArrayList<>();
                    
                    for (BlockPos p : solverQueue) {
                        double tx = p.getX() - 1 + 0.5 + TARGET_X_OFFSET;
                        double ty = p.getY() + 0.5 + TARGET_Y_OFFSET;
                        double tz = p.getZ() + 0.5 + TARGET_Z_OFFSET;
                        splinePoints.add(new net.minecraft.world.phys.Vec3(tx, ty, tz));
                    }

                    RotationManager.getInstance().rotateToSpline(splinePoints);
                    waitingForRotation = true;
                }
            } else {
                endSolving();
            }
            return;
        }

        if (buttonsExist && !lastExisted) {
            allObi = true;

            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = 0; dz <= 3; dz++) {
                    BlockPos pos = startPos.offset(0, dy, dz);
                    if (client.level.getBlockState(pos).getBlock() != Blocks.OBSIDIAN) {
                        allObi = false;
                    }
                }
            }

            if (allObi) {
                lastExisted = true;
                skipOver = true;

                float maxDist = ConfigManager.data.AutoSSDistanceLimit;
                if (client.player.distanceToSqr(startPos.getX(), client.player.getY(), startPos.getZ()) > maxDist * maxDist) {
                    return;
                }
                
                if (!solution.isEmpty()) {
                    isSolving = true;
                    isPreAiming = false;
                    solvingIndex = 0;
                    delayTicksRemaining = 0;
                    waitingForRotation = false;
                    
                    solverQueue.clear();
                    solverQueue.addAll(solution);

                    hasReturnPoint = false;
                    if (!solution.isEmpty()) {
                        solverQueue.add(solution.get(0));
                        hasReturnPoint = true;
                    }
                    
                    applyRotationSettings();
                }
            }
        }

        if (!buttonsExist && lastExisted) {
            lastExisted = false;
            endSolving();
            solution.clear();
        }

        for (int dy = 0; dy <= 3; dy++) {
            for (int dz = 0; dz <= 3; dz++) {
                BlockPos pos = startPos.offset(0, dy, dz);
                if (client.level.getBlockState(pos).getBlock() == Blocks.SEA_LANTERN && !solution.contains(pos)) {
                    solution.add(pos);
                    if (!isSolving && !isPreAiming) {
                        startPreAiming(pos);
                    }
                }
            }
        }
    }

    private static void startPreAiming(BlockPos pos) {
        if (isPreAiming || isSolving) return;
        isPreAiming = true;
        applyRotationSettings();
        
        double tx = pos.getX() - 1 + TARGET_X_OFFSET;
        double ty = pos.getY() + TARGET_Y_OFFSET;
        double tz = pos.getZ() + TARGET_Z_OFFSET;
        
        RotationManager.getInstance().rotateToBlock(tx, ty, tz);
    }

    private static void applyRotationSettings() {
        if (settingsOverridden) return;
        originalRandomness = ConfigManager.data.rotationTargetRandomness;
        ConfigManager.data.rotationTargetRandomness = 0.05f;

        originalSpeed = ConfigManager.data.rotationSpeed;
        ConfigManager.data.rotationSpeed = ConfigManager.data.AutoSSRotationSpeed;

        originalCurve = ConfigManager.data.rotationJitter;
        ConfigManager.data.rotationJitter = ConfigManager.data.AutoSSRotationCurve;
        settingsOverridden = true;
    }

    private static void restoreRotationSettings() {
        if (!settingsOverridden) return;
        ConfigManager.data.rotationTargetRandomness = originalRandomness;
        ConfigManager.data.rotationSpeed = originalSpeed;
        ConfigManager.data.rotationJitter = originalCurve;
        settingsOverridden = false;
    }

    private static void handleClick(BlockPos clickedPos) {
        if (!LocationUtils.inDungeons()) return;

        if (clickedPos.getX() == 110 && clickedPos.getY() == 121 && clickedPos.getZ() == 91) {
            solution.clear();
            skipOver = false;
            return;
        }

        if (solution.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        if (client.level.getBlockState(clickedPos).getBlock() != Blocks.STONE_BUTTON) return;
        
        long currentTime = System.currentTimeMillis();
        if (lastClick == currentTime) return;
        lastClick = currentTime;

        BlockPos checkPos = clickedPos.east();
        if (solution.isEmpty()) return;
        BlockPos expected = solution.get(0);

        if (!checkPos.equals(expected)) {
            if (solution.size() == 3 && checkPos.equals(solution.get(1))) {
                solution.remove(1);
                solution.remove(0);
            }
        } else {
            solution.remove(expected);
        }
    }

    private static void endSolving() {
        restoreRotationSettings();
        isSolving = false;
        isPreAiming = false;
        solvingIndex = 0;
        waitingForRotation = false;
        solverQueue.clear();
        RotationManager.getInstance().clearSpline();
    }

    private static void resetSolver() {
        endSolving();
        lastExisted = false;
        skipOver = false;
        solution.clear();
        allObi = true;
        hasReturnPoint = false;
    }
}
