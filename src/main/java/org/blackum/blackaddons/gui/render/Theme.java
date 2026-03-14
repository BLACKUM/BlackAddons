package org.blackum.blackaddons.gui.render;

public class Theme {

    // Colors
    public static int BACKGROUND = 0xAA000000;
    public static int SURFACE = 0xCC1A1A1A;
    public static int SURFACE_LIGHT = 0xCC2D2D2D;
    public static int SURFACE_PRESSED = 0xCC151515;
    public static final int DEFAULT_ACCENT = 0xFF00A8FF;
    public static int ACCENT = DEFAULT_ACCENT;
    public static int ACCENT_HOVER = 0xFF0090D9;
    public static int TEXT_PRIMARY = 0xFFFFFFFF;
    public static int TEXT_SECONDARY = 0xFFBBBBBB;
    public static int TEXT = TEXT_PRIMARY; // TODO: just make it as primary everywhere
    public static int BORDER = 0x44FFFFFF;

    public static int BACKGROUND_SECONDARY = 0xAA222222;
    public static int BACKGROUND_TERTIARY = 0xAA333333;
    public static int ACCENT_PRIMARY = ACCENT;

    public static void refreshColors() {
        ACCENT_PRIMARY = ACCENT;
        
        int r = (ACCENT >> 16) & 0xFF;
        int g = (ACCENT >> 8) & 0xFF;
        int b = ACCENT & 0xFF;
        
        float factor = 0.85f;
        r = (int) (r * factor);
        g = (int) (g * factor);
        b = (int) (b * factor);
        
        ACCENT_HOVER = 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int GLASS_FILL = 0x801A1A1A;
    public static int GLASS_BORDER = 0x40FFFFFF;
    public static int GLASS_HIGHLIGHT = 0x22FFFFFF;
    public static int SHADOW = 0x44000000;
    public static int SCROLLBAR_BG = 0x80000000;
    public static int SCROLLBAR_THUMB = 0xFFFFFFFF;
    public static int TOOLTIP_BG = 0xE0000000;

    public static int CONTROL_BG_HOVER = 0x22FFFFFF;

    public static final int BORDER_RADIUS = 12;
    public static final int BORDER_RADIUS_SMALL = 8;
    public static final int BORDER_RADIUS_LARGE = 16;

    public static final int ANIM_HOVER = 200;
    public static final int ANIM_CLICK = 100;
    public static final int ANIM_FOCUS = 150;
    public static final int ANIM_DIALOG = 300;
    public static final int ANIM_SCROLL = 250;
    public static final int ANIM_NORMAL = 200;

    public static final int BUTTON_HEIGHT = 36;
    public static final int TEXTFIELD_HEIGHT = 32;
    public static final int CHECKBOX_SIZE = 18;
    public static final int RADIO_SIZE = 18;
    public static final int SLIDER_HEIGHT = 6;
    public static final int SLIDER_THUMB_SIZE = 16;
    public static final int SCROLLBAR_WIDTH = 8;
    public static final int DROPDOWN_WIDTH = 200;
    public static final int COLOR_PICKER_HEIGHT = 210;
    public static final int TOGGLE_HEIGHT = 20;

    public static final float ITEM_GRID_SCALE = 2.0f;
    public static final int ITEM_GRID_SLOT_SIZE = 18;
    public static final int ITEM_GRID_PADDING = 6;

    public static final int PADDING_SMALL = 8;
    public static final int PADDING_MEDIUM = 12;
    public static final int PADDING_LARGE = 16;
    public static final int PADDING = PADDING_MEDIUM; // TODO: replace all paddings with this
    public static final int MARGIN = 8;

    public static final int GRID_GAP = 20;
    public static final int GRID_COLUMNS = 2;
    public static final int GRID_MARGIN = 20;

    public static final int CARD_HEIGHT_SMALL = 100;
    public static final int CARD_HEIGHT_MEDIUM = 150;
    public static final int CARD_HEIGHT_LARGE = 200;

    public static final int SPACING_SMALL = 8;
    public static final int SPACING_NORMAL = 20;
    public static final int SPACING_LARGE = 30;
    public static final int CARD_SPACING = 20;

    public static int withAlpha(int rgb, float alpha) {
        int a = (int) (alpha * 255) << 24;
        return a | (rgb & 0x00FFFFFF);
    }

    public static int lerpColor(int colorA, int colorB, float t) {
        int ar = (colorA >> 16) & 0xFF;
        int ag = (colorA >> 8) & 0xFF;
        int ab = colorA & 0xFF;

        int br = (colorB >> 16) & 0xFF;
        int bg = (colorB >> 8) & 0xFF;
        int bb = colorB & 0xFF;

        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int b = (int) (ab + (bb - ab) * t);

        return (r << 16) | (g << 8) | b;
    }
}
