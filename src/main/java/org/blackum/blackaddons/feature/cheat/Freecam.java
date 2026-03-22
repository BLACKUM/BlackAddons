package org.blackum.blackaddons.feature.cheat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class Freecam {
    private static final Freecam INSTANCE = new Freecam();
    private static final double MOVEMENT_MULTIPLIER = 0.5;
    private static final int MOUSE_BIND_OFFSET = 1000;

    private final Vector3d pos = new Vector3d();
    private final Vector3d prevPos = new Vector3d();
    private final Set<Integer> pressedMouseButtons = new HashSet<>();

    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;

    private ResourceKey<Level> lastDimension;
    private int lastPlayerId = -1;
    private boolean active;
    private boolean forward;
    private boolean backward;
    private boolean right;
    private boolean left;
    private boolean up;
    private boolean down;
    private boolean keybindPressedLastTick;
    private boolean holdKeyActive;

    private Freecam() {
    }

    public static Freecam getInstance() {
        return INSTANCE;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> getInstance().onTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> getInstance().deactivate());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> getInstance().deactivate());
    }

    public void setMouseButtonState(int button, boolean pressed) {
        if (pressed) {
            pressedMouseButtons.add(button);
        } else {
            pressedMouseButtons.remove(button);
        }
    }

    public void toggle() {
        if (active) {
            deactivate();
        } else {
            activate();
        }
    }

    public void activate() {
        if (active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        active = true;
        ConfigManager.data.freecamEnabled = true;

        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
        lastYaw = yaw;
        lastPitch = pitch;
        lastPlayerId = mc.player.getId();
        lastDimension = mc.level.dimension();

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        pos.set(camPos.x, camPos.y, camPos.z);
        prevPos.set(camPos.x, camPos.y, camPos.z);

        unpressKeys();
    }
    public void deactivate() {
        boolean wasActive = active;
        active = false;
        holdKeyActive = false;
        keybindPressedLastTick = false;
        ConfigManager.data.freecamEnabled = false;

        if (wasActive) {
            unpressKeys();
        }
    }

    private void onTick() {
        handleKeybind();

        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            deactivate();
            return;
        }

        if (lastPlayerId != -1 && mc.player.getId() != lastPlayerId) {
             deactivate();
             return;
        }

        if (lastDimension == null || lastDimension != mc.level.dimension()) {
            lastDimension = mc.level.dimension();
            deactivate();
            return;
        }

        Vec3 forwardVec = Vec3.directionFromRotation(0, yaw);
        Vec3 rightVec = Vec3.directionFromRotation(0, yaw + 90);

        double velX = 0;
        double velY = 0;
        double velZ = 0;

        double speed = ConfigManager.data.freecamSpeed * MOVEMENT_MULTIPLIER;
        if (mc.options.keySprint.isDown()) speed *= 2;

        boolean movingInPlane = false;
        if (mc.options.keyUp.isDown()) {
            velX += forwardVec.x * speed;
            velZ += forwardVec.z * speed;
            movingInPlane = true;
        }
        if (mc.options.keyDown.isDown()) {
            velX -= forwardVec.x * speed;
            velZ -= forwardVec.z * speed;
            movingInPlane = true;
        }

        boolean strafing = false;
        if (mc.options.keyRight.isDown()) {
            velX += rightVec.x * speed;
            velZ += rightVec.z * speed;
            strafing = true;
        }
        if (mc.options.keyLeft.isDown()) {
            velX -= rightVec.x * speed;
            velZ -= rightVec.z * speed;
            strafing = true;
        }

        if (movingInPlane && strafing) {
            double diagonal = 1.0 / Math.sqrt(2.0);
            velX *= diagonal;
            velZ *= diagonal;
        }

        if (mc.options.keyJump.isDown()) velY += speed;
        if (mc.options.keyShift.isDown()) velY -= speed;

        prevPos.set(pos);
        pos.add(velX, velY, velZ);
    }

    private void handleKeybind() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            keybindPressedLastTick = false;
            holdKeyActive = false;
            return;
        }

        int keyCode = ConfigManager.data.freecamKeyCode;
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode < 0) {
            keybindPressedLastTick = false;
            if (holdKeyActive) {
                holdKeyActive = false;
            }
            return;
        }

        boolean pressed = isBindPressed(mc, keyCode);
        if (ConfigManager.data.freecamHoldMode) {
            if (pressed && !keybindPressedLastTick && !active) {
                activate();
                holdKeyActive = active;
            } else if (!pressed && holdKeyActive) {
                deactivate();
            }
        } else if (pressed && !keybindPressedLastTick) {
            toggle();
        }

        keybindPressedLastTick = pressed;
    }

    private boolean isBindPressed(Minecraft mc, int keyCode) {
        if (keyCode >= MOUSE_BIND_OFFSET) {
            return pressedMouseButtons.contains(keyCode - MOUSE_BIND_OFFSET);
        }
        return InputConstants.isKeyDown(mc.getWindow(), keyCode);
    }

    private void unpressKeys() {
        Minecraft mc = Minecraft.getInstance();
        Options options = mc.options;

        options.keyUp.setDown(false);
        options.keyDown.setDown(false);
        options.keyRight.setDown(false);
        options.keyLeft.setDown(false);
        options.keyJump.setDown(false);
        options.keyShift.setDown(false);
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;

        yaw += (float) deltaX * 0.15f;
        pitch += (float) deltaY * 0.15f;
        pitch = Mth.clamp(pitch, -90.0f, 90.0f);
    }

    public boolean isActive() {
        return active;
    }

    public double getX(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.x, pos.x);
    }

    public double getY(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.y, pos.y);
    }

    public double getZ(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.z, pos.z);
    }

    public float getYaw(float tickDelta) {
        return Mth.lerp(tickDelta, lastYaw, yaw);
    }

    public float getPitch(float tickDelta) {
        return Mth.lerp(tickDelta, lastPitch, pitch);
    }
}
