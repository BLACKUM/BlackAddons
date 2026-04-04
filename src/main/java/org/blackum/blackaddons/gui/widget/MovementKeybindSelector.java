package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.input.MouseButtonEvent;


import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;

import java.util.*;
import java.util.function.Consumer;

public class MovementKeybindSelector extends Widget {
    public static final String FORWARD = "forward";
    public static final String BACKWARD = "backward";
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String JUMP = "jump";
    public static final String SPRINT = "sprint";
    public static final String SHIFT = "shift";

    private static final int CHIP_HEIGHT = 34;
    private static final int GAP = 6;

    private static final String[] ORDER = { FORWARD, LEFT, BACKWARD, RIGHT, JUMP, SPRINT, SHIFT };

    private final Consumer<List<String>> onChange;
    private final Set<String> selected = new LinkedHashSet<>();
    private final List<Chip> chips = new ArrayList<>();

    public MovementKeybindSelector(int x, int y, int width, List<String> initialSelection, Consumer<List<String>> onChange) {
        super(x, y, width, CHIP_HEIGHT * 4 + GAP * 3);
        this.onChange = onChange;
        if (initialSelection != null) {
            selected.addAll(initialSelection);
        }
        rebuildChips();
    }

    public void setSelection(List<String> values) {
        selected.clear();
        if (values != null) {
            selected.addAll(values);
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        rebuildChips();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        rebuildChips();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        rebuildChips();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }

        for (Chip chip : chips) {
            boolean active = selected.contains(chip.id);
            boolean hovered = chip.contains(mouseX, mouseY);
            float hoverProgress = chip.hoverAnimation.getValue();

            int fill = active
                    ? Theme.withAlpha(Theme.ACCENT, 0.28f + hoverProgress * 0.10f)
                    : Theme.withAlpha(Theme.SURFACE_LIGHT, 0.72f + hoverProgress * 0.18f);
            int border = active ? Theme.withAlpha(Theme.ACCENT, 0.95f) : Theme.GLASS_BORDER;
            int glow = active ? Theme.withAlpha(Theme.ACCENT, 0.14f + hoverProgress * 0.08f) : 0;

            if (glow != 0) {
                RenderHelper.renderRoundedRect(graphics, chip.x - 1, chip.y - 1, chip.width + 2, chip.height + 2,
                        Theme.BORDER_RADIUS_SMALL, glow);
            }

            RenderHelper.renderRoundedRect(graphics, chip.x, chip.y, chip.width, chip.height, Theme.BORDER_RADIUS_SMALL, fill);
            RenderHelper.renderRoundedOutline(graphics, chip.x, chip.y, chip.width, chip.height, Theme.BORDER_RADIUS_SMALL, border);

            int titleColor = active ? Theme.TEXT_PRIMARY : Theme.withAlpha(Theme.TEXT_PRIMARY, 0.92f);
            int detailColor = active ? Theme.withAlpha(Theme.TEXT_PRIMARY, 0.8f) : Theme.withAlpha(Theme.TEXT_SECONDARY, 0.75f);
            int titleWidth = Minecraft.getInstance().font.width(chip.label);
            int detailWidth = Minecraft.getInstance().font.width(chip.detail);
            int centerX = chip.x + chip.width / 2;
            graphics.text(Minecraft.getInstance().font, chip.label, centerX - titleWidth / 2, chip.y + 8, titleColor);
            graphics.text(Minecraft.getInstance().font, chip.detail, centerX - detailWidth / 2, chip.y + 19, detailColor);
        }
    }

    @Override
    public void tick() {
        for (Chip chip : chips) {
            chip.tick();
        }
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        for (Chip chip : chips) {
            chip.updateHover(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
        if (!enabled || !visible || event.button() != 0) {
            return false;
        }

        for (Chip chip : chips) {
            if (chip.contains(mouseX, mouseY)) {
                if (!selected.add(chip.id)) {
                    selected.remove(chip.id);
                }
                onChange.accept(new ArrayList<>(selected));
                return true;
            }
        }
        return false;
    }

    private void rebuildChips() {
        chips.clear();

        int thirdWidth = (width - GAP * 2) / 3;
        int firstTwoWidth = (width - GAP) / 2;

        chips.add(new Chip(FORWARD, "Forward", getKeyText(FORWARD), x + thirdWidth + GAP, y, thirdWidth, CHIP_HEIGHT));

        int secondRowY = y + CHIP_HEIGHT + GAP;
        chips.add(new Chip(LEFT, "Left", getKeyText(LEFT), x, secondRowY, thirdWidth, CHIP_HEIGHT));
        chips.add(new Chip(BACKWARD, "Backward", getKeyText(BACKWARD), x + thirdWidth + GAP, secondRowY, thirdWidth, CHIP_HEIGHT));
        chips.add(new Chip(RIGHT, "Right", getKeyText(RIGHT), x + (thirdWidth + GAP) * 2, secondRowY, thirdWidth, CHIP_HEIGHT));

        int thirdRowY = secondRowY + CHIP_HEIGHT + GAP;
        chips.add(new Chip(JUMP, "Jump", getKeyText(JUMP), x, thirdRowY, width, CHIP_HEIGHT));

        int fourthRowY = thirdRowY + CHIP_HEIGHT + GAP;
        chips.add(new Chip(SPRINT, "Sprint", getKeyText(SPRINT), x, fourthRowY, firstTwoWidth, CHIP_HEIGHT));
        chips.add(new Chip(SHIFT, "Shift", getKeyText(SHIFT), x + firstTwoWidth + GAP, fourthRowY, width - firstTwoWidth - GAP, CHIP_HEIGHT));
    }

    private String getKeyText(String id) {
        KeyMapping mapping = getKeyMapping(id);
        if (mapping == null) {
            return "";
        }
        return mapping.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
    }

    public static KeyMapping getKeyMapping(Minecraft minecraft, String id) {
        if (minecraft == null || minecraft.options == null || id == null) {
            return null;
        }
        return switch (id) {
            case FORWARD -> minecraft.options.keyUp;
            case BACKWARD -> minecraft.options.keyDown;
            case LEFT -> minecraft.options.keyLeft;
            case RIGHT -> minecraft.options.keyRight;
            case JUMP -> minecraft.options.keyJump;
            case SPRINT -> minecraft.options.keySprint;
            case SHIFT -> minecraft.options.keyShift;
            default -> null;
        };
    }

    public static KeyMapping getKeyMapping(String id) {
        return getKeyMapping(Minecraft.getInstance(), id);
    }

    public static List<String> sanitizeSelection(List<String> values) {
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    for (String id : ORDER) {
                        if (id.equalsIgnoreCase(value)) {
                            sanitized.add(id);
                            break;
                        }
                    }
                }
            }
        }
        return new ArrayList<>(sanitized);
    }

    public static String summarize(List<String> values) {
        List<String> sanitized = sanitizeSelection(values);
        if (sanitized.isEmpty()) {
            return "None";
        }
        List<String> labels = new ArrayList<>();
        for (String id : sanitized) {
            labels.add(switch (id) {
                case FORWARD -> "Forward";
                case BACKWARD -> "Backward";
                case LEFT -> "Left";
                case RIGHT -> "Right";
                case JUMP -> "Jump";
                case SPRINT -> "Sprint";
                case SHIFT -> "Shift";
                default -> id;
            });
        }
        return String.join(", ", labels);
    }

    private static class Chip {
        private final String id;
        private final String label;
        private final String detail;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private Animation hoverAnimation = new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut);
        private boolean hovered;

        private Chip(String id, String label, String detail, int x, int y, int width, int height) {
            this.id = id;
            this.label = label;
            this.detail = detail;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private void updateHover(int mouseX, int mouseY) {
            hovered = contains(mouseX, mouseY);
        }

        private void tick() {
            if (hovered && hoverAnimation.getProgress() < 1
                    && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            } else if (!hovered && hoverAnimation.getProgress() > 0
                    && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            }
        }
    }
}
