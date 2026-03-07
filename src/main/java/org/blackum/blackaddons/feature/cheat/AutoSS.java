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
    private static final int MAX_SS_ROUNDS = 5;
    
    private static final Pattern startRegex = Pattern.compile("^\\[BOSS\\] Goldor: Who dares trespass into my domain\\?$");
    private static final Pattern completeRegex = Pattern.compile("^([A-Za-z0-9_]+) completed a device! \\(\\d/7\\)$");
    
    private static final float TARGET_X_OFFSET = 0.50f;
    private static final float TARGET_Y_OFFSET = -0.05f;
    private static final float TARGET_Z_OFFSET = 0.00f;

    private static final List<BlockPos> solution = new ArrayList<>();
    private static final List<BlockPos> solverQueue = new ArrayList<>();
    private static boolean lastExisted = false;
    private static boolean skipOver = false;
    private static boolean firstPatternOfRound = true;
    private static boolean allObi = true;
    private static boolean hasReturnPoint = false;

    private static boolean isSolving = false;
    private static boolean isPreAiming = false;
    private static boolean autoStartTriggered = false;
    private static boolean settingsOverridden = false;
    private static int solvingIndex = 0;
    private static BlockPos preAimTarget = null;
    private static int delayTicksRemaining = 0;
    private static boolean waitingForRotation = false;
    private static float originalRandomness = 0f;
    private static float originalSpeed = 0f;
    private static float originalCurve = 0f;

    private static long lastClick = 0L;

    private static int breakTicks = 0;
    private static boolean canBreak = false;
    private static boolean wasBroken = false;
    private static long ssStartTime = 0;
    private static final java.util.Set<BlockPos> lastLitPositions = new java.util.HashSet<>();
    private static final java.util.Set<BlockPos> brokenPositions = new java.util.HashSet<>();
    private static int skipClicksRemaining = 0;

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

        if (startRegex.matcher(text).find()) {
            resetSolver();
            autoStartTriggered = true;
        } else {
            java.util.regex.Matcher completeMatcher = completeRegex.matcher(text);
            if (completeMatcher.matches()) {
                String playerName = completeMatcher.group(1);
                
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.player != null) {
                    if (playerName.equals(client.player.getName().getString()) || playerName.equals(client.player.getScoreboardName())) {
                        if (ssStartTime > 0) {
                            long time = System.currentTimeMillis() - ssStartTime;
                            org.blackum.blackaddons.feature.chat.ChatUtils.send_debug(String.format(java.util.Locale.US, "§aSS completed in %.3fs", time / 1000.0f));
                            ssStartTime = 0;
                            canBreak = false;
                        }
                    }
                }
            }
        }
    }

    private static void onClientTick(Minecraft client) {
        if (!ConfigManager.data.AutoSSEnabled || client.player == null || client.level == null) return;
        if (!LocationUtils.inDungeons()) return;

        boolean buttonsExist = client.level.getBlockState(buttonCheckPos).getBlock() == Blocks.STONE_BUTTON;

        boolean isGameActive = false;
        for (int dy = 0; dy <= 3; dy++) {
            for (int dz = 0; dz <= 3; dz++) {
                BlockPos p = startPos.offset(0, dy, dz);
                if (client.level.getBlockState(p).getBlock() != Blocks.OBSIDIAN) {
                    isGameActive = true;
                    break;
                }
            }
            if (isGameActive) break;
        }

        if (isGameActive) {
            breakTicks = 12;
            canBreak = true;
            if (wasBroken) {
                wasBroken = false;
                if (ConfigManager.data.AutoSSAlerts) {
                    org.blackum.blackaddons.feature.chat.ChatUtils.send_debug("§aSS started");
                }
            }
        } else {
            if (canBreak) {
                if (breakTicks > 0) {
                    breakTicks--;
                } else {
                    boolean allButtonsMissing = true;
                    for (int dy = 0; dy <= 3; dy++) {
                        for (int dz = 0; dz <= 3; dz++) {
                            BlockPos p = buttonCheckPos.offset(0, dy, dz);
                            if (client.level.getBlockState(p).getBlock() != Blocks.AIR) {
                                allButtonsMissing = false;
                                break;
                            }
                        }
                        if (!allButtonsMissing) break;
                    }

                    if (allButtonsMissing) {
                        canBreak = false;
                        wasBroken = true;
                        if (ConfigManager.data.AutoSSAlerts) {
                            org.blackum.blackaddons.feature.chat.ChatUtils.send_debug("§cSS broke");
                            client.player.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 5f, 0f);
                        }
                        ssStartTime = 0;
                    }
                }
            }
            
            if (!isGameActive && !canBreak) {
                if (lastExisted || isSolving) {
                    resetSolver();
                }
                
                if (autoStartTriggered && client.level.getBlockState(startButton).getBlock() == Blocks.STONE_BUTTON) {
                    float maxDist = ConfigManager.data.AutoSSDistanceLimit;
                    if (client.player.distanceToSqr(startButton.getX() + 0.5, client.player.getY(), startButton.getZ() + 0.5) <= maxDist * maxDist) {
                        if (ConfigManager.data.AutoSSAutoStart || ConfigManager.data.AutoSSTrySkip) {
                            if (isLookingAtTarget(startButton.east())) {
                                if (ConfigManager.data.AutoSSTrySkip) {
                                    skipClicksRemaining = 3;
                                } else if (ConfigManager.data.AutoSSAutoStart) {
                                    skipClicksRemaining = 1;
                                }
                                autoStartTriggered = false;
                                isPreAiming = false;
                                preAimTarget = null;
                                RotationManager.getInstance().clearSpline();
                            } else {
                                startPreAiming(startButton.east());
                            }
                        } else {
                            autoStartTriggered = false;
                        }
                    }
                }
            }
        }

        if (skipClicksRemaining > 0) {
            if (isLookingAtTarget(startButton.east())) {
                List<ConfigManager.TriggerAction> actions = new java.util.ArrayList<>();
                actions.add(new ConfigManager.TriggerAction(ConfigManager.TriggerActionType.USE_ITEM, 0, "", 0, 0));
                TriggerActionExecutor.getInstance().execute(actions, null);
                skipClicksRemaining--;
            } else {
                startPreAiming(startButton.east());
            }
        }

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
                        if (ConfigManager.data.AutoSSInstantSnap) {
                            RotationManager.getInstance().snapToTarget();
                        }
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

                if (solvingIndex < solverQueue.size() && !isReturnPoint) {
                    lookingAtButton = isLookingAtTarget(solverQueue.get(solvingIndex));
                }

                if (lookingAtButton || RotationManager.getInstance().isAtSplineNode()) {
                    if (!isReturnPoint) {
                        List<ConfigManager.TriggerAction> actions = new java.util.ArrayList<>();
                        actions.add(new ConfigManager.TriggerAction(ConfigManager.TriggerActionType.USE_ITEM, 0, "", 1, 0));
                        TriggerActionExecutor.getInstance().execute(actions, null);
                        
                        delayTicksRemaining = ConfigManager.data.AutoSSInstantSnap ? 1 : Math.max(1, ConfigManager.data.AutoSSDelay);
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
                    if (ConfigManager.data.AutoSSInstantSnap) {
                        RotationManager.getInstance().snapToTarget();
                    }
                    waitingForRotation = true;
                }
            } else {
                endSolving();
            }
            return;
        }

        if (buttonsExist && !lastExisted) {
            lastExisted = true;
            skipOver = false; 
            if (ssStartTime == 0) ssStartTime = System.currentTimeMillis();
            
            brokenPositions.clear();
            lastLitPositions.clear();
            
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = 0; dz <= 3; dz++) {
                    BlockPos p = startPos.offset(0, dy, dz);
                    if (client.level.getBlockState(p).is(Blocks.SEA_LANTERN)) {
                        brokenPositions.add(p);
                    }
                }
            }

            float maxDist = ConfigManager.data.AutoSSDistanceLimit;
            if (client.player.distanceToSqr(startPos.getX(), client.player.getY(), startPos.getZ()) > maxDist * maxDist) {
                lastExisted = false;
                return;
            }
            
            if (!solution.isEmpty() && !isSolving) {
                boolean wasPreAiming = isPreAiming;
                isSolving = true;
                isPreAiming = false;
                firstPatternOfRound = false;
                solvingIndex = 0;
                delayTicksRemaining = 0;
                waitingForRotation = false;
                
                solverQueue.clear();
                solverQueue.addAll(solution);

                hasReturnPoint = false;
                if (!solution.isEmpty() && solution.size() < MAX_SS_ROUNDS) {
                    solverQueue.add(solution.get(0));
                    hasReturnPoint = true;
                }
                
                applyRotationSettings();

                if (wasPreAiming) {
                    List<net.minecraft.world.phys.Vec3> splinePoints = new java.util.ArrayList<>();
                    for (BlockPos p : solverQueue) {
                        double tx = p.getX() - 1 + 0.5 + TARGET_X_OFFSET;
                        double ty = p.getY() + 0.5 + TARGET_Y_OFFSET;
                        double tz = p.getZ() + 0.5 + TARGET_Z_OFFSET;
                        splinePoints.add(new net.minecraft.world.phys.Vec3(tx, ty, tz));
                    }
                    
                    if (isLookingAtTarget(solverQueue.get(0))) {
                        RotationManager.getInstance().resumeWithSpline(splinePoints);
                        if (ConfigManager.data.AutoSSInstantSnap) {
                            RotationManager.getInstance().snapToTarget();
                        }
                    } else {
                        RotationManager.getInstance().rotateToSpline(splinePoints);
                    }
                    waitingForRotation = true;
                }
            }
        }

        if (!buttonsExist && lastExisted) {
            lastExisted = false;
            endSolving();
            solution.clear();
            lastLitPositions.clear();
        }

        java.util.Set<BlockPos> currentLit = new java.util.HashSet<>();
        for (int dy = 0; dy <= 3; dy++) {
            for (int dz = 0; dz <= 3; dz++) {
                BlockPos pos = startPos.offset(0, dy, dz);
                boolean isLantern = client.level.getBlockState(pos).is(Blocks.SEA_LANTERN);
                if (isLantern) {
                    currentLit.add(pos);
                    if (buttonsExist && !isSolving) {
                        brokenPositions.add(pos);
                    } else if (!buttonsExist) {
                        if (!brokenPositions.contains(pos) && !lastLitPositions.contains(pos) && !solution.contains(pos)) {
                            solution.add(pos);
                            if (firstPatternOfRound && solution.size() == 3) {
                                solution.remove(0);
                                skipOver = true;
                                firstPatternOfRound = false;
                            }
                        }
                    }
                }
            }
        }
        lastLitPositions.clear();
        lastLitPositions.addAll(currentLit);
        
        if (!solution.isEmpty() && !isSolving) {
            float maxDist = ConfigManager.data.AutoSSDistanceLimit;
            if (client.player.distanceToSqr(startPos.getX(), client.player.getY(), startPos.getZ()) <= maxDist * maxDist) {
                BlockPos target = null;
                if (firstPatternOfRound) {
                    if (solution.size() >= 2) {
                        target = solution.get(1);
                    }
                } else {
                    target = solution.get(0);
                }
                
                if (target != null) {
                    startPreAiming(target);
                } else if (isPreAiming) {
                    isPreAiming = false;
                    preAimTarget = null;
                    RotationManager.getInstance().clearSpline();
                }
            }
        }
    }

    private static void startPreAiming(BlockPos pos) {
        if (isSolving) return;
        if (isPreAiming && pos.equals(preAimTarget)) return;
        
        isPreAiming = true;
        preAimTarget = pos;
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

    private static boolean isLookingAtTarget(BlockPos expectedLantern) {
        Minecraft client = Minecraft.getInstance();
        if (client.hitResult != null && client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos hitBlock = ((net.minecraft.world.phys.BlockHitResult) client.hitResult).getBlockPos();
            return hitBlock.equals(expectedLantern.west());
        }
        return false;
    }

    private static void handleClick(BlockPos clickedPos) {
        if (!LocationUtils.inDungeons()) return;

        if (clickedPos.getX() == 110 && clickedPos.getY() == 121 && clickedPos.getZ() == 91) {
            if (ssStartTime == 0) ssStartTime = System.currentTimeMillis();
            solution.clear();
            skipOver = false;
            return;
        }

        if (solution.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        if (client.level.getBlockState(clickedPos).getBlock() != Blocks.STONE_BUTTON) return;
        
        skipOver = true;

        long currentTime = System.currentTimeMillis();
        if (lastClick == currentTime) return;
        lastClick = currentTime;

        BlockPos checkPos = clickedPos.east();
        if (solution.isEmpty()) return;
        
        if (checkPos.equals(solution.get(0))) {
            solution.remove(0);
            return;
        }

        if (solution.size() >= 2 && checkPos.equals(solution.get(1))) {
            solution.remove(0);
            solution.remove(0);
            skipOver = true;
        }
    }

    private static void endSolving() {
        restoreRotationSettings();
        isSolving = false;
        isPreAiming = false;
        preAimTarget = null;
        solvingIndex = 0;
        waitingForRotation = false;
        solverQueue.clear();
        RotationManager.getInstance().clearSpline();
    }

    private static void resetSolver() {
        endSolving();
        lastExisted = false;
        skipOver = false;
        firstPatternOfRound = true;
        preAimTarget = null;
        solution.clear();
        lastLitPositions.clear();
        brokenPositions.clear();
        allObi = true;
        hasReturnPoint = false;
        ssStartTime = 0;
        skipClicksRemaining = 0;
    }
}
