package org.blackum.blackaddons.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

public class KeybindButton extends Widget {
    private static final int MOUSE_BIND_OFFSET = 1000;
    private final String label;
    private final IntConsumer onChange;
    private int keyCode;
    private boolean listening;

    public KeybindButton(int x, int y, int width, String label, int keyCode, IntConsumer onChange) {
        super(x, y, width, Theme.BUTTON_HEIGHT);
        this.label = label;
        this.keyCode = keyCode;
        this.onChange = onChange;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, listening);

        String text = listening ? label + ": Press key..." : label + ": " + getKeyName();
        int textColor = listening ? Theme.ACCENT : Theme.TEXT_PRIMARY;
        if (Minecraft.getInstance().font.width(text) > width - 20) {
            text = Minecraft.getInstance().font.plainSubstrByWidth(text, width - 25) + "...";
        }

        int textX = x + 10;
        int textY = y + (height - 8) / 2;
        graphics.text(Minecraft.getInstance().font, text, textX, textY, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible) {
            return false;
        }
        if (!isMouseOver(mouseX, mouseY)) {
            listening = false;
            return false;
        }
        if (listening) {
            setKeyCode(encodeMouseButton(event.button()));
            listening = false;
            return true;
        }
        if (event.button() == 1) {
            setKeyCode(unboundCode());
            return true;
        }
        if (event.button() == 0) {
            listening = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, KeyEvent event) {
        if (!listening) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setKeyCode(unboundCode());
        } else {
            setKeyCode(keyCode);
        }
        listening = false;
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            listening = false;
        }
    }

    private void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        onChange.accept(keyCode);
    }

    private String getKeyName() {
        if (keyCode == unboundCode()) {
            return "Unbound";
        }
        if (isMouseButton(keyCode)) {
            return InputConstants.Type.MOUSE.getOrCreate(decodeMouseButton(keyCode)).getDisplayName().getString();
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
    }

    private static int unboundCode() {
        return GLFW.GLFW_KEY_UNKNOWN;
    }

    private static int encodeMouseButton(int button) {
        return MOUSE_BIND_OFFSET + button;
    }

    private static boolean isMouseButton(int keyCode) {
        return keyCode >= MOUSE_BIND_OFFSET;
    }

    private static int decodeMouseButton(int keyCode) {
        return keyCode - MOUSE_BIND_OFFSET;
    }
}
