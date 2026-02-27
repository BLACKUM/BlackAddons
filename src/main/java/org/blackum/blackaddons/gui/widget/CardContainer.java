package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        for (ResizableCard card : cards) {
            if (card.isVisible()) {
                card.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible)
            return;
        for (ResizableCard card : cards) {
            if (card.isVisible()) {
                card.renderOverlay(graphics, mouseX, mouseY, rawMouseX, rawMouseY, partialTick);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;

        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.isVisible() && card.mouseClicked(mouseX, mouseY, button)) {
                bringToFront(card);
                activeCard = card;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeCard != null && (activeCard.isDragging() || activeCard.isResizing())) {
            return activeCard.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.charTyped(character, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            ResizableCard card = cards.get(i);
            if (card.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
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
