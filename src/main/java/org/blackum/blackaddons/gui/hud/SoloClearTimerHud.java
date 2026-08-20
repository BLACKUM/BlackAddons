package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.feature.dungeon.tracker.SoloClearTimer;
import org.blackum.blackaddons.gui.screen.overlay.OverlayEditScreen;

@AutoModule(order = 407)
public class SoloClearTimerHud implements HudElement {
    private static final int BASE_W = 95;
    private static final int BASE_H = 30;
    private static final int LINE_HEIGHT = 10;
    private static final int DEFAULT_X = 10;
    private static final int DEFAULT_Y = 100;
    private static final float DEFAULT_SCALE = 1.0f;

    private static final String PREVIEW_RTA = "RTA: 01:05.000";
    private static final String PREVIEW_IGT = "IGT: 01:02.700";
    private static final String PREVIEW_LAG = "Lag: 02.300";

    private static final String PREFIX_RTA = "RTA: ";
    private static final String PREFIX_IGT = "IGT: ";
    private static final String PREFIX_LAG = "Lag: ";

    public static void register() {
        HudRegistry.register(new SoloClearTimerHud());
    }

    @Override
    public String id() {
        return "solo_clear_timer";
    }

    @Override
    public String displayName() {
        return "Solo Clear Timer";
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.soloClearTimerEnabled && !McCompat.isGuiHidden(Minecraft.getInstance());
    }

    @Override
    public int x() {
        return ConfigManager.data.soloClearTimerX < 0 ? DEFAULT_X : ConfigManager.data.soloClearTimerX;
    }

    @Override
    public int y() {
        return ConfigManager.data.soloClearTimerY < 0 ? DEFAULT_Y : ConfigManager.data.soloClearTimerY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.soloClearTimerX = x;
        ConfigManager.data.soloClearTimerY = y;
    }

    @Override
    public void reset() {
        ConfigManager.data.soloClearTimerX = DEFAULT_X;
        ConfigManager.data.soloClearTimerY = DEFAULT_Y;
        ConfigManager.data.soloClearTimerScale = DEFAULT_SCALE;
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        float scale = Math.max(0.5f, Math.min(3.0f, (float) width / BASE_W));
        ConfigManager.data.soloClearTimerScale = scale;
    }

    @Override
    public int width() {
        return Math.max(16, Math.round(BASE_W * ConfigManager.data.soloClearTimerScale));
    }

    @Override
    public int height() {
        return Math.max(16, Math.round(BASE_H * ConfigManager.data.soloClearTimerScale));
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        boolean isEditing = McCompat.getScreen(mc) instanceof OverlayEditScreen;

        if (!isEditing && !SoloClearTimer.shouldRender()) {
            return;
        }

        String rtaText;
        String igtText;
        String lagText;

        if (isEditing) {
            rtaText = PREVIEW_RTA;
            igtText = PREVIEW_IGT;
            lagText = PREVIEW_LAG;
        } else {
            rtaText = PREFIX_RTA + SoloClearTimer.formatTimeWithMinutes(SoloClearTimer.getRtaMs());
            igtText = PREFIX_IGT + SoloClearTimer.formatTimeWithMinutes(SoloClearTimer.getIgtMs());
            lagText = PREFIX_LAG + SoloClearTimer.formatDesync(SoloClearTimer.getDesyncMs());
        }

        int rtaColor = ConfigManager.data.soloClearTimerRtaColor;
        int igtColor = ConfigManager.data.soloClearTimerIgtColor;
        int desyncColor = ConfigManager.data.soloClearTimerDesyncColor;
        float scale = ConfigManager.data.soloClearTimerScale;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x(), (float) y());
        graphics.pose().scale(scale, scale);
        graphics.text(mc.font, rtaText, 0, 0, rtaColor);
        graphics.text(mc.font, igtText, 0, LINE_HEIGHT, igtColor);
        graphics.text(mc.font, lagText, 0, LINE_HEIGHT * 2, desyncColor);
        graphics.pose().popMatrix();
    }
}
