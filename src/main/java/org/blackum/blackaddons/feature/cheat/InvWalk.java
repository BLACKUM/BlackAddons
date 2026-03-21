package org.blackum.blackaddons.feature.cheat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.mixin.core.KeyBindingAccessor;

import java.util.HashSet;
import java.util.Set;

public final class InvWalk {
    private static boolean forcingMovement;

    private InvWalk() {
    }

    public static boolean areMovementKeysPressed(Minecraft mc) {
        if (mc == null || mc.options == null) return false;
        for (KeyMapping key : getMovementKeys(mc)) {
            if (!(key instanceof KeyBindingAccessor accessor)) continue;
            if (isPhysicalKeyDown(mc, accessor.getBoundKey())) return true;
        }
        return false;
    }

    public static void beginForcedMovement(Minecraft mc) {
        if (forcingMovement || mc == null || mc.options == null) return;
        forcingMovement = true;
        for (KeyMapping key : getMovementKeys(mc)) {
            setKeyState(key, false);
        }
    }

    public static void forceMovement(Minecraft mc, boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak) {
        if (mc == null || mc.options == null) return;

        Set<KeyMapping> desired = new HashSet<>();
        if (forward) desired.add(mc.options.keyUp);
        if (back) desired.add(mc.options.keyDown);
        if (left) desired.add(mc.options.keyLeft);
        if (right) desired.add(mc.options.keyRight);
        if (jump) desired.add(mc.options.keyJump);
        if (sneak) desired.add(mc.options.keyShift);

        for (KeyMapping key : getMovementKeys(mc)) {
            setKeyState(key, desired.contains(key));
        }
    }

    public static void releaseForcedMovement(Minecraft mc) {
        if (!forcingMovement || mc == null || mc.options == null) return;
        forcingMovement = false;
        for (KeyMapping key : getMovementKeys(mc)) {
            if (!(key instanceof KeyBindingAccessor accessor)) continue;
            boolean physicalDown = isPhysicalKeyDown(mc, accessor.getBoundKey());
            KeyMapping.set(accessor.getBoundKey(), physicalDown);
            accessor.setBlackaddonsIsDown(physicalDown);
        }
    }

    private static KeyMapping[] getMovementKeys(Minecraft mc) {
        return new KeyMapping[]{
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight,
                mc.options.keyJump,
                mc.options.keyShift
        };
    }

    private static void setKeyState(KeyMapping key, boolean pressed) {
        if (key instanceof KeyBindingAccessor accessor) {
            KeyMapping.set(accessor.getBoundKey(), pressed);
            accessor.setBlackaddonsIsDown(pressed);
        }
    }

    private static boolean isPhysicalKeyDown(Minecraft mc, InputConstants.Key key) {
        if (mc == null || mc.getWindow() == null || key == null) return false;
        if (key.getType() != InputConstants.Type.KEYSYM) return false;
        return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
    }
}
