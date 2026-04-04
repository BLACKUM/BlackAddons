package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.blackum.blackaddons.gui.render.Theme;

import java.util.ArrayList;
import java.util.List;

public class CardContainer extends Widget {

    private List<ResizableCard> cards = new ArrayList<>();
    private ResizableCard activeCard = null;

    public CardContainer(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public void addCard(ResizableCard card) {
        cards.add(card);
        card.setDragBounds(x, y, x + width, y + height);
    }

    public void removeCard(ResizableCard card) {
        cards.remove(card);
    }

    public void clearCards() {
        cards.clear();
    }

    public List<ResizableCard> getCards() {
        return cards;
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        boolean blocked = false;
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.isVisible()) {
                if (blocked) {
                    card.updateHoverState(-10000, -10000);
                } else {
                    card.updateHoverState(mouseX, mouseY);
                    if (card.isMouseOver(mouseX, mouseY)) {
                        blocked = true;
                    }
                }
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        for (ResizableCard card : cards) {
            if (card.isVisible()) {
                card.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public void extractRenderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;
        for (ResizableCard card : cards) {
            if (card.isVisible()) {
                card.extractRenderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
            }
        }
    }

    @Override
    public void tick() {
        for (ResizableCard card : cards) {
            card.tick();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible)
            return false;

        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.isVisible() && card.mouseClicked(mouseX, mouseY, event)) {
                bringToFront(card);
                activeCard = card;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, MouseButtonEvent event) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.mouseReleased(mouseX, mouseY, event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, MouseButtonEvent event, double dragX, double dragY) {
        if (activeCard != null && (activeCard.isDragging() || activeCard.isResizing())) {
            return activeCard.mouseDragged(mouseX, mouseY, event, dragX, dragY);
        }

        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.mouseDragged(mouseX, mouseY, event, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, KeyEvent event) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.keyPressed(keyCode, scanCode, event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, CharacterEvent event) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.charTyped(chr, event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, MouseButtonEvent event, double scrollX, double scrollY) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.mouseScrolled(mouseX, mouseY, event, scrollX, scrollY)) {
                return true;
            }
        }
        return false;
    }

    private void bringToFront(ResizableCard card) {
        if (cards.remove(card)) {
            cards.add(card);
        }
    }
}
