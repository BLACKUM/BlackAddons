package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AutocompleteTextField extends TextField {
    private final Supplier<List<String>> suggestionProvider;
    private List<String> currentSuggestions = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean showSuggestions = false;
    private final int MAX_VISIBLE_SUGGESTIONS = 7;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public AutocompleteTextField(int x, int y, int width, int height, String placeholder,
            Supplier<List<String>> suggestionProvider) {
        super(x, y, width, height, placeholder);
        this.suggestionProvider = suggestionProvider;
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        updateSuggestions(text);
    }

    @Override
    public boolean charTyped(char chr, CharacterEvent event) {
        boolean result = super.charTyped(chr, event);
        if (result) {
            updateSuggestions(getText());
        }
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, KeyEvent event) {
        if (showSuggestions && isFocused()) {
            if (keyCode == 264) { // Down
                selectedIndex = Math.min(selectedIndex + 1, currentSuggestions.size() - 1);
                return true;
            } else if (keyCode == 265) { // Up
                selectedIndex = Math.max(selectedIndex - 1, 0);
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter
                if (selectedIndex >= 0 && selectedIndex < currentSuggestions.size()) {
                    setText(currentSuggestions.get(selectedIndex));
                    showSuggestions = false;
                    return true;
                }
            } else if (keyCode == 256) { // Escape
                showSuggestions = false;
                return true;
            }
        }
        boolean result = super.keyPressed(keyCode, scanCode, event);
        if (result && (keyCode == 259 || keyCode == 261 || event.hasControlDown())) { // Backspace, Delete, or Ctrl+V
            updateSuggestions(getText());
        }
        return result;
    }

    private void updateSuggestions(String query) {
        if (query.isEmpty() || !isFocused()) {
            showSuggestions = false;
            return;
        }

        List<String> all = suggestionProvider.get();
        currentSuggestions.clear();
        String q = query.toLowerCase();
        for (String s : all) {
            if (s.toLowerCase().contains(q) && !s.equalsIgnoreCase(query)) {
                currentSuggestions.add(s);
            }
            if (currentSuggestions.size() >= MAX_VISIBLE_SUGGESTIONS)
                break;
        }

        selectedIndex = currentSuggestions.isEmpty() ? -1 : 0;
        showSuggestions = !currentSuggestions.isEmpty();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            showSuggestions = false;
        } else {
            updateSuggestions(getText());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (showSuggestions) {
            int suggestionHeight = 12;
            int totalHeight = currentSuggestions.size() * suggestionHeight + 4;
            if (mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + totalHeight) {
                int index = (int) ((mouseY - (y + height + 2)) / suggestionHeight);
                if (index >= 0 && index < currentSuggestions.size()) {
                    setText(currentSuggestions.get(index));
                    showSuggestions = false;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, event);
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!showSuggestions || !visible || currentSuggestions.isEmpty())
            return;

        int suggestionHeight = 12;
        int maxItems = Math.min(currentSuggestions.size(), MAX_VISIBLE_SUGGESTIONS);
        int totalHeight = maxItems * suggestionHeight + 4;
        int overlayY = y + height;

        RenderHelper.renderRoundedRect(graphics, x, overlayY, width, totalHeight, Theme.BORDER_RADIUS_SMALL,
                Theme.GLASS_FILL);
        RenderHelper.renderRoundedOutline(graphics, x, overlayY, width, totalHeight, Theme.BORDER_RADIUS_SMALL,
                Theme.GLASS_BORDER);

        for (int i = 0; i < maxItems; i++) {
            int itemY = overlayY + 2 + (i * suggestionHeight);
            String suggestion = currentSuggestions.get(i);

            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY
                    && mouseY < itemY + suggestionHeight;

            if (hovered && (mouseX != lastMouseX || mouseY != lastMouseY)) {
                selectedIndex = i;
            }

            if (i == selectedIndex) {
                graphics.fill(x + 1, itemY, x + width - 1, itemY + suggestionHeight,
                        Theme.withAlpha(Theme.ACCENT, 0.3f));
            }

            graphics.text(Minecraft.getInstance().font, suggestion, x + 4, itemY + (suggestionHeight - 8) / 2,
                    Theme.TEXT_PRIMARY);
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }
}
