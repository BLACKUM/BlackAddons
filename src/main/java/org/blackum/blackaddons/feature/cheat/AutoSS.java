package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.manager.RotationManager;
import org.blackum.blackaddons.core.util.LocationUtils;
import org.blackum.blackaddons.core.util.Scheduler;
import org.blackum.blackaddons.feature.chat.ChatActionExecutor;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.mixin.core.GameRendererAccessor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoSS {
    private static final BlockPos START_BUTTON = new BlockPos(110, 121, 91);
    private static final BlockPos BUTTON_CHECK_POS = new BlockPos(110, 120, 92);
    private static final BlockPos START_POS = new BlockPos(111, 120, 92);
    private static final int MAX_SS_ROUNDS = 5;

    private static final Pattern START_REGEX = Pattern.compile("^\\[BOSS\\] Goldor: Who dares trespass into my domain\\?$");
    private static final Pattern COMPLETE_REGEX = Pattern.compile("^([A-Za-z0-9_]+) completed a device! \\(\\d/7\\)$");

    private static final float TARGET_X_OFFSET = 0.50f;
    private static final float TARGET_Y_OFFSET = -0.05f;
    private static final float TARGET_Z_OFFSET = 0.00f;

    private static final int BREAK_TICKS_THRESHOLD = 12;
    private static final int RETURN_POINT_DELAY_TICKS = 20;
    private static final float AUTO_SS_FORCED_RANDOMNESS = 0.05f;
    private static final double DEVICE_SEARCH_RADIUS = 3.0;
    private static final int NODE_MARKER_HALF_SIZE = 6;

    private static final int COLOR_CURRENT_NODE = 0xFF00FF00;
    private static final int COLOR_NEXT_NODE = 0xFFFFFF00;
    private static final int COLOR_OTHER_NODE = 0xFFFF0000;
    private static final int COLOR_MARKER_BG = 0xAA000000;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;

    private static final int DELAY_JITTER_MAX = 2;
    private static final Random RANDOM = new Random();

    private static final List<BlockPos> solution = new ArrayList<>();
    private static final List<BlockPos> solverQueue = new ArrayList<>();
    private static final Set<BlockPos> lastLitPositions = new HashSet<>();
    private static final Set<BlockPos> brokenPositions = new HashSet<>();
    private static final Set<BlockPos> permanentBrokenLamps = new HashSet<>();

    private static boolean lastExisted = false;
    private static boolean firstPatternOfRound = true;
    private static boolean allObi = true;
    private static boolean hasReturnPoint = false;

    private static boolean isSolving = false;
    private static boolean isPreAiming = false;
    private static boolean autoStartTriggered = false;
    private static boolean settingsOverridden = false;
    private static int solvingIndex = 0;
    private static BlockPos preAimTarget = null;
    private static boolean waitingForRotation = false;
    private static boolean waitingForDelay = false;
    private static float originalRandomness = 0f;
    private static float originalSpeed = 0f;
    private static float originalCurve = 0f;

    private static long lastClick = 0L;

    private static int breakTicks = 0;
    private static boolean canBreak = false;
    private static boolean wasBroken = false;
    private static long ssStartTime = 0;
    private static int skipClicksRemaining = 0;
    private static String lastCompletionTime = "None";

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoSS::onClientTick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleChatMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleChatMessage(message));
        HudRenderCallback.EVENT.register(AutoSS::onRenderHud);

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

        if (START_REGEX.matcher(text).find()) {
            resetSolver();
            autoStartTriggered = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                permanentBrokenLamps.clear();
                for (int dy = 0; dy <= 3; dy++) {
                    for (int dz = 0; dz <= 3; dz++) {
                        BlockPos p = START_POS.offset(0, dy, dz);
                        if (mc.level.getBlockState(p).is(Blocks.SEA_LANTERN)) {
                            permanentBrokenLamps.add(p);
                        }
                    }
                }
            }
        } else {
            Matcher completeMatcher = COMPLETE_REGEX.matcher(text);
            if (completeMatcher.matches()) {
                String playerName = completeMatcher.group(1);

                Minecraft client = Minecraft.getInstance();
                if (client != null && client.player != null) {
                    if (playerName.equals(client.player.getName().getString()) || playerName.equals(client.player.getScoreboardName())) {
                        if (ssStartTime > 0) {
                            long time = System.currentTimeMillis() - ssStartTime;
                            lastCompletionTime = String.format(Locale.US, "%.3fs", time / 1000.0f);
                            String logMsg = "SS completed in " + lastCompletionTime;
                            ChatUtils.send_debug("§a" + logMsg);
                            AutoSSLogger.log(logMsg);
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

        boolean deviceActive = false;
        for (net.minecraft.world.entity.Entity entity : client.level.getEntities(null, new net.minecraft.world.phys.AABB(START_BUTTON).inflate(DEVICE_SEARCH_RADIUS))) {
            if (entity.hasCustomName()) {
                String name = entity.getCustomName().getString();
                if (name.contains("Device Active")) {
                    deviceActive = true;
                    break;
                }
            }
        }

        if (deviceActive) {
            if (isSolving || isPreAiming || skipClicksRemaining > 0 || ssStartTime > 0 || lastExisted || !solution.isEmpty()) {
                resetSolver();
            }
            return;
        }

        boolean buttonsExist = client.level.getBlockState(BUTTON_CHECK_POS).getBlock() == Blocks.STONE_BUTTON;

        boolean isGameActive = false;
        for (int dy = 0; dy <= 3; dy++) {
            for (int dz = 0; dz <= 3; dz++) {
                BlockPos p = START_POS.offset(0, dy, dz);
                if (client.level.getBlockState(p).getBlock() != Blocks.OBSIDIAN && !permanentBrokenLamps.contains(p)) {
                    isGameActive = true;
                    break;
                }
            }
            if (isGameActive) break;
        }

        if (isGameActive) {
            breakTicks = BREAK_TICKS_THRESHOLD;
            canBreak = true;
            if (wasBroken) {
                wasBroken = false;
                AutoSSLogger.log("SS Started/Resumed (isGameActive=true)");
                if (ConfigManager.data.AutoSSAlerts) {
                    ChatUtils.send_debug("§aSS started");
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
                            BlockPos p = BUTTON_CHECK_POS.offset(0, dy, dz);
                            if (client.level.getBlockState(p).getBlock() != Blocks.AIR) {
                                allButtonsMissing = false;
                                break;
                            }
                        }
                        if (!allButtonsMissing) break;
                    }

                    if (allButtonsMissing && solution.isEmpty()) {
                        canBreak = false;
                        wasBroken = true;
                        permanentBrokenLamps.clear();
                        for (int dy2 = 0; dy2 <= 3; dy2++) {
                            for (int dz2 = 0; dz2 <= 3; dz2++) {
                                BlockPos p = START_POS.offset(0, dy2, dz2);
                                if (client.level.getBlockState(p).is(Blocks.SEA_LANTERN)) {
                                    permanentBrokenLamps.add(p);
                                }
                            }
                        }
                        if (!permanentBrokenLamps.isEmpty()) {
                            AutoSSLogger.log("Permanent broken lamps detected: " + permanentBrokenLamps.size());
                        }
                        if (ConfigManager.data.AutoSSAlerts) {
                            ChatUtils.send_debug("§cSS broke");
                            client.player.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 5f, 0f);
                        }
                        ssStartTime = 0;
                    }
                }
            }

            if (!isGameActive && !canBreak) {
                if (isSolving || (!solution.isEmpty() && !isPreAiming)) {
                    resetSolver();
                }

                if (autoStartTriggered && client.level.getBlockState(START_BUTTON).getBlock() == Blocks.STONE_BUTTON) {
                    float maxDist = ConfigManager.data.AutoSSDistanceLimit;
                    if (client.player.distanceToSqr(START_BUTTON.getX() + 0.5, client.player.getY(), START_BUTTON.getZ() + 0.5) <= maxDist * maxDist) {
                        if (ConfigManager.data.AutoSSAutoStart) {
                            if (isLookingAtTarget(START_BUTTON.east())) {
                                skipClicksRemaining = ConfigManager.data.AutoSSTrySkip ? 3 : 1;
                                if (ssStartTime == 0) ssStartTime = System.currentTimeMillis();
                                autoStartTriggered = false;
                                isPreAiming = false;
                                preAimTarget = null;
                                RotationManager.getInstance().clearSpline();
                            } else {
                                startPreAiming(START_BUTTON.east());
                            }
                        } else {
                            autoStartTriggered = false;
                        }
                    }
                }
            }
        }

        if (skipClicksRemaining > 0) {
            if (isLookingAtTarget(START_BUTTON.east())) {
                if (ssStartTime == 0) ssStartTime = System.currentTimeMillis();
                performClick();
                skipClicksRemaining--;
            } else {
                startPreAiming(START_BUTTON.east());
            }
        }

        if (isSolving) {
            boolean playerInFront = false;
            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY) {
                if (((EntityHitResult) client.hitResult).getEntity() instanceof net.minecraft.world.entity.player.Player) {
                    playerInFront = true;
                }
            }

            if (playerInFront) {
                RotationManager.getInstance().clearSpline();
                waitingForRotation = false;
                return;
            }

            if (!buttonsExist || solverQueue.isEmpty() || solvingIndex >= solverQueue.size()) {
                endSolving();
                return;
            }

            if (waitingForDelay) return;

            if (solvingIndex >= solverQueue.size()) {
                endSolving();
                return;
            }

            if (waitingForRotation) {
                boolean isReturnPoint = hasReturnPoint && (solvingIndex == solverQueue.size() - 1);

                boolean lookingAtButton = false;
                if (solvingIndex < solverQueue.size() && !isReturnPoint) {
                    lookingAtButton = isLookingAtTarget(solverQueue.get(solvingIndex));
                }

                if (lookingAtButton || RotationManager.getInstance().isAtSplineNode()) {
                    int capturedIndex = solvingIndex;
                    if (!isReturnPoint) {
                        AutoSSLogger.log("Clicking node " + capturedIndex + " at " + solverQueue.get(capturedIndex).toShortString());
                        int tickDelay = ConfigManager.data.AutoSSDelay + RANDOM.nextInt(DELAY_JITTER_MAX + 1);
                        performClick();
                        scheduleAdvance(tickDelay);
                    } else {
                        AutoSSLogger.log("Reached return point " + solverQueue.get(capturedIndex).toShortString());
                        scheduleAdvance(RETURN_POINT_DELAY_TICKS);
                    }

                    solvingIndex++;
                    waitingForRotation = false;
                }
                return;
            }

            if (solvingIndex < solverQueue.size()) {
                if (!waitingForRotation) {
                    List<Vec3> splinePoints = buildSplinePoints(solverQueue);
                    RotationManager.getInstance().rotateToSpline(splinePoints);
                    waitingForRotation = true;
                }
            } else {
                endSolving();
            }

            Set<BlockPos> currentLitWhileSolving = new HashSet<>();
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = 0; dz <= 3; dz++) {
                    BlockPos pos = START_POS.offset(0, dy, dz);
                    if (client.level.getBlockState(pos).is(Blocks.SEA_LANTERN)) {
                        currentLitWhileSolving.add(pos);
                    } else if (lastLitPositions.contains(pos) && client.level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                        AutoSSLogger.log("Last chain node confirmed (lantern->obsidian): " + pos.toShortString());
                        brokenPositions.add(pos);
                    }
                }
            }
            lastLitPositions.clear();
            lastLitPositions.addAll(currentLitWhileSolving);
            return;
        }

        if (buttonsExist && !lastExisted) {
            lastExisted = true;
            firstPatternOfRound = true;
            canBreak = true;
            breakTicks = BREAK_TICKS_THRESHOLD;
            if (ssStartTime == 0) {
                ssStartTime = System.currentTimeMillis();
                AutoSSLogger.log("SS Started");
            }

            brokenPositions.clear();
            lastLitPositions.clear();

            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = 0; dz <= 3; dz++) {
                    BlockPos p = START_POS.offset(0, dy, dz);
                    if (client.level.getBlockState(p).is(Blocks.SEA_LANTERN)) {
                        brokenPositions.add(p);
                    }
                }
            }

            float maxDist = ConfigManager.data.AutoSSDistanceLimit;
            if (client.player.distanceToSqr(START_POS.getX(), client.player.getY(), START_POS.getZ()) > maxDist * maxDist) {
                lastExisted = false;
                return;
            }

            if (!solution.isEmpty() && !isSolving) {
                boolean wasPreAiming = isPreAiming;
                isSolving = true;
                isPreAiming = false;
                firstPatternOfRound = false;
                solvingIndex = 0;
                waitingForRotation = false;
                waitingForDelay = false;

                solverQueue.clear();
                solverQueue.addAll(solution);

                hasReturnPoint = false;
                if (!solution.isEmpty() && solution.size() < MAX_SS_ROUNDS) {
                    solverQueue.add(solution.get(0));
                    hasReturnPoint = true;
                }

                applyRotationSettings();

                if (wasPreAiming) {
                    List<Vec3> splinePoints = buildSplinePoints(solverQueue);

                    if (isLookingAtTarget(solverQueue.get(0))) {
                        RotationManager.getInstance().resumeWithSpline(splinePoints);
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
        }

        Set<BlockPos> currentLit = new HashSet<>();
        for (int dy = 0; dy <= 3; dy++) {
            for (int dz = 0; dz <= 3; dz++) {
                BlockPos pos = START_POS.offset(0, dy, dz);
                boolean isLantern = client.level.getBlockState(pos).is(Blocks.SEA_LANTERN);
                if (isLantern) {
                    currentLit.add(pos);
                    if (ssStartTime == 0) {
                        brokenPositions.add(pos);
                    } else if (buttonsExist && !isSolving) {
                        brokenPositions.add(pos);
                    } else if (!buttonsExist) {
                        if (!brokenPositions.contains(pos) && !lastLitPositions.contains(pos) && !solution.contains(pos)) {
                            solution.add(pos);
                            AutoSSLogger.log("Lantern detected: " + pos.toShortString() + " (Total: " + solution.size() + ")");
                            if (firstPatternOfRound && solution.size() == 3) {
                                solution.remove(0);
                                AutoSSLogger.log("Applied Skip Over - removed first lantern");
                                firstPatternOfRound = false;
                            }
                        }
                    }
                } else if (lastLitPositions.contains(pos) && client.level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                    AutoSSLogger.log("Last chain node confirmed (lantern->obsidian): " + pos.toShortString());
                    if (permanentBrokenLamps.contains(pos) && !solution.contains(pos)) {
                        solution.add(pos);
                        permanentBrokenLamps.remove(pos);
                        AutoSSLogger.log("Permanent broken lamp is last node, added to solution: " + pos.toShortString());
                    }
                    brokenPositions.add(pos);
                }
            }
        }
        lastLitPositions.clear();
        lastLitPositions.addAll(currentLit);

        if (!solution.isEmpty() && !isSolving && !autoStartTriggered && skipClicksRemaining == 0) {
            float maxDist = ConfigManager.data.AutoSSDistanceLimit;
            if (client.player.distanceToSqr(START_POS.getX(), client.player.getY(), START_POS.getZ()) <= maxDist * maxDist) {
                BlockPos target = null;
                if (firstPatternOfRound) {
                    if (solution.size() >= 2) {
                        target = solution.get(1);
                    } else if (solution.size() == 1) {
                        target = solution.get(0);
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

    private static List<Vec3> buildSplinePoints(List<BlockPos> positions) {
        List<Vec3> splinePoints = new ArrayList<>();
        for (BlockPos p : positions) {
            double tx = p.getX() - 1 + 0.5 + TARGET_X_OFFSET;
            double ty = p.getY() + 0.5 + TARGET_Y_OFFSET;
            double tz = p.getZ() + 0.5 + TARGET_Z_OFFSET;
            splinePoints.add(new Vec3(tx, ty, tz));
        }
        return splinePoints;
    }

    private static void performClick() {
        List<ConfigManager.ActionStep> actions = new ArrayList<>();
        actions.add(new ConfigManager.ActionStep(ConfigManager.ActionStepType.USE_ITEM, 0, "", 0, 0));
        ChatActionExecutor.getInstance().execute(actions, null);
    }

    private static void scheduleAdvance(int tickDelay) {
        waitingForDelay = true;
        Scheduler.schedule(0, tickDelay, () -> {
            waitingForDelay = false;
            if (!isSolving) return;
            if (solvingIndex < solverQueue.size()) {
                RotationManager.getInstance().advanceSpline();
                if (ConfigManager.data.AutoSSInstantSnap) {
                    RotationManager.getInstance().snapToTarget();
                }
                waitingForRotation = true;
            } else {
                endSolving();
            }
        });
    }

    private static void onRenderHud(GuiGraphics graphics, net.minecraft.client.DeltaTracker tracker) {
        if (!ConfigManager.data.AutoSSDebug || !ConfigManager.data.AutoSSEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        List<String> debugInfo = new ArrayList<>();
        debugInfo.add(ChatFormatting.GOLD + "[AutoSS Debug]");
        debugInfo.add("Solving: " + isSolving + (waitingForRotation ? " (Waiting Rot)" : "") + (waitingForDelay ? " (Waiting Delay)" : ""));
        debugInfo.add("Solution Size: " + solution.size());
        debugInfo.add("Solving Index: " + solvingIndex + "/" + solverQueue.size());

        if (ssStartTime > 0) {
            long current = System.currentTimeMillis() - ssStartTime;
            debugInfo.add(String.format(Locale.US, "Current Time: %.3fs", current / 1000.0f));
        }
        debugInfo.add("Last Time: " + lastCompletionTime);

        if (isSolving && solvingIndex < solverQueue.size()) {
            BlockPos target = solverQueue.get(solvingIndex);
            boolean onTarget = isLookingAtTarget(target);
            debugInfo.add("Target: " + target.toShortString());
            debugInfo.add("On Target: " + (onTarget ? ChatFormatting.GREEN + "YES" : ChatFormatting.RED + "NO"));

            RotationManager rm = RotationManager.getInstance();
            boolean rotDone = rm.isAtSplineNode();
            debugInfo.add("Rot Done: " + (rotDone ? ChatFormatting.GREEN + "YES" : ChatFormatting.RED + "NO"));

            if (rm.isActive()) {
                debugInfo.add(String.format("Rot: %.1f, %.1f", client.player.getYRot(), client.player.getXRot()));
            }

            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hit = ((BlockHitResult) client.hitResult).getBlockPos();
                debugInfo.add("Looking At: " + hit.toShortString());
            }
        }

        int overlayX = ConfigManager.data.AutoSSOverlayX < 0 ? 10 : ConfigManager.data.AutoSSOverlayX;
        int overlayY = ConfigManager.data.AutoSSOverlayY;

        int y = overlayY;
        for (String line : debugInfo) {
            graphics.drawString(client.font, line, overlayX, y, COLOR_TEXT_WHITE);
            y += 10;
        }
        graphics.drawString(client.font, ChatFormatting.YELLOW + "Solution:", overlayX, y, COLOR_TEXT_WHITE);
        y += 10;
        for (int i = 0; i < solution.size(); i++) {
            graphics.drawString(client.font, (i + 1) + ". " + solution.get(i).toShortString(), overlayX + 10, y, COLOR_TEXT_WHITE);
            y += 10;
        }

        renderVisualNodes(graphics, tracker);
    }

    private static void renderVisualNodes(GuiGraphics g, net.minecraft.client.DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!ConfigManager.data.AutoSSDebug || mc.player == null || mc.gameRenderer == null || solution.isEmpty()) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float partialTicks = tracker.getGameTimeDeltaTicks();
        float fov = (float) Math.toRadians(((GameRendererAccessor) mc.gameRenderer).invokeGetFov(mc.gameRenderer.getMainCamera(), partialTicks, true));
        float aspect = (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight();
        Matrix4f proj = new Matrix4f().perspective(fov, aspect, 0.05f, mc.gameRenderer.getRenderDistance() * 4.0f);

        Quaternionf camRot = new Quaternionf(mc.gameRenderer.getMainCamera().rotation());
        camRot.conjugate();
        Matrix4f view = new Matrix4f().rotation(camRot);

        for (int i = 0; i < solution.size(); i++) {
            BlockPos p = solution.get(i);
            double tx = p.getX() + 0.5;
            double ty = p.getY() + 0.5;
            double tz = p.getZ() + 0.5;

            double relX = tx - cam.x;
            double relY = ty - cam.y;
            double relZ = tz - cam.z;

            Vector4f clipPos = new Vector4f((float) relX, (float) relY, (float) relZ, 1.0f);
            clipPos.mul(view).mul(proj);

            if (clipPos.w <= 0.0f) continue;

            float ndcX = clipPos.x / clipPos.w;
            float ndcY = clipPos.y / clipPos.w;

            float sx = (ndcX + 1.0f) * 0.5f * screenW;
            float sy = (1.0f - ndcY) * 0.5f * screenH;

            int ix = (int) sx;
            int iy = (int) sy;

            if (ix < -20 || ix > screenW + 20 || iy < -20 || iy > screenH + 20) continue;

            int color;
            if (i == 0) color = COLOR_CURRENT_NODE;
            else if (i == 1) color = COLOR_NEXT_NODE;
            else color = COLOR_OTHER_NODE;

            String text = String.valueOf(i + 1);
            int textW = mc.font.width(text);

            g.fill(ix - NODE_MARKER_HALF_SIZE, iy - NODE_MARKER_HALF_SIZE, ix + NODE_MARKER_HALF_SIZE, iy + NODE_MARKER_HALF_SIZE, COLOR_MARKER_BG);
            g.drawString(mc.font, text, ix - textW / 2, iy - 4, color, true);
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
        ConfigManager.data.rotationTargetRandomness = AUTO_SS_FORCED_RANDOMNESS;

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
        if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitBlock = ((BlockHitResult) client.hitResult).getBlockPos();
            return hitBlock.equals(expectedLantern.west());
        }
        return false;
    }

    private static void handleClick(BlockPos clickedPos) {
        if (!LocationUtils.inDungeons()) return;

        if (clickedPos.getX() == START_BUTTON.getX() && clickedPos.getY() == START_BUTTON.getY() && clickedPos.getZ() == START_BUTTON.getZ()) {
            if (ssStartTime == 0) ssStartTime = System.currentTimeMillis();
            solution.clear();
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

        if (checkPos.equals(solution.get(0))) {
            solution.remove(0);
            return;
        }

        if (solution.size() >= 2 && checkPos.equals(solution.get(1))) {
            solution.remove(0);
            solution.remove(0);
        }
    }

    private static void endSolving() {
        restoreRotationSettings();
        isSolving = false;
        isPreAiming = false;
        preAimTarget = null;
        solvingIndex = 0;
        waitingForRotation = false;
        waitingForDelay = false;
        solverQueue.clear();
        RotationManager.getInstance().clearSpline();
    }

    private static void resetSolver() {
        if (isSolving || isPreAiming) AutoSSLogger.log("Resetting solver (Solving: " + isSolving + ", PreAim: " + isPreAiming + ")");
        endSolving();
        autoStartTriggered = false;
        lastExisted = false;
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
