package org.blackum.blackaddons.core.manager;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.feature.cheat.AutoTNT;
import org.blackum.blackaddons.feature.cheat.FastLeap;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BaseScreen;

import java.util.ArrayList;
import java.util.List;
import org.blackum.blackaddons.core.manager.RotationManager;

public class DebugOverlayManager {
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final int LINE_HEIGHT = 10;

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            if (!BaseScreen.showDebugOverlay)
                return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui)
                return;

            int x = BaseScreen.overlayX;
            int y = BaseScreen.overlayY;
            float scale = BaseScreen.overlayScale;

            List<String> debugInfo = gatherDebugInfo(mc);

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) x, (float) y);
            graphics.pose().scale(scale, scale);

            int lineY = 0;
            for (String line : debugInfo) {
                graphics.drawString(mc.font, line, 0, lineY, DEFAULT_COLOR);
                lineY += LINE_HEIGHT;
            }
            graphics.pose().popMatrix();
        });
    }

    private static List<String> gatherDebugInfo(Minecraft mc) {
        List<String> debugInfo = new ArrayList<>();
        debugInfo.add(ChatFormatting.GOLD + "[BlackAddons Debug]");
        debugInfo.add("Nick: " + mc.getUser().getName());
        debugInfo.add("VSync: " + mc.options.enableVsync().get());

        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();

        int finalMouseX = (int) (mc.mouseHandler.xpos() * ((double) scaledWidth / windowWidth));
        int finalMouseY = (int) (mc.mouseHandler.ypos() * ((double) scaledHeight / windowHeight));
        debugInfo.add("Mouse: " + finalMouseX + ", " + finalMouseY);
        debugInfo.add("Screen: " + (mc.screen != null ? mc.screen.getClass().getSimpleName() : "None"));

        debugInfo.add("");
        debugInfo.add(ChatFormatting.GOLD + "[Mod Hider Real Info]");
        debugInfo.add("Real Brand: fabric");

        int modCount = 0;
        int libCount = 0;
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if ("builtin".equals(mod.getMetadata().getType()))
                continue;
            String type = mod.getMetadata().getType();
            if (type.contains("library") || type.contains("api") ||
                    mod.getMetadata().getId().contains("library") ||
                    mod.getMetadata().getId().contains("api")) {
                libCount++;
            } else {
                modCount++;
            }
        }
        debugInfo.add("Real Mods: " + modCount);
        debugInfo.add("Real Libraries: " + libCount);

        debugInfo.add("");
        debugInfo.add(ChatFormatting.GOLD + "[Mod Hider Status]");
        debugInfo.add("Spoof Mode: " + ConfigManager.data.modHiderSpoofMode.name());
        debugInfo.add("Hide Mods: " + ConfigManager.data.hideMods());
        debugInfo.add("Custom Client: " + ConfigManager.data.modHiderCustomClient);
        debugInfo.add("Disable Payloads: " + ConfigManager.data.modHiderDisableCustomPayloads);

        addModHiderDetail(debugInfo);
        debugInfo.addAll(AutoTNT.getDebugInfo());
        debugInfo.addAll(FastLeap.getDebugInfo());
        debugInfo.addAll(RotationManager.getDebugInfo());

        return debugInfo;
    }

    private static void addModHiderDetail(List<String> debugInfo) {
        List<String> hiddenModIds = new ArrayList<>();
        List<String> hiddenLibIds = new ArrayList<>();
        int allowedModCount = 0;
        int allowedLibCount = 0;

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if ("builtin".equals(mod.getMetadata().getType()))
                continue;
            String modId = mod.getMetadata().getId();
            String type = mod.getMetadata().getType();
            boolean isLibrary = type.contains("library") || type.contains("api") ||
                    modId.contains("library") || modId.contains("api");
            boolean isAllowed = ConfigManager.data.modHiderAllowedMods.contains(modId);

            if (isLibrary) {
                if (isAllowed)
                    allowedLibCount++;
                else
                    hiddenLibIds.add(modId);
            } else {
                if (isAllowed)
                    allowedModCount++;
                else
                    hiddenModIds.add(modId);
            }
        }

        debugInfo.add("Allowed Mods: " + allowedModCount);
        debugInfo.add("Allowed Libraries: " + allowedLibCount);

        if (!hiddenModIds.isEmpty()) {
            debugInfo.add("");
            debugInfo.add(ChatFormatting.RED + "Hidden Mods:");
            for (int i = 0; i < Math.min(5, hiddenModIds.size()); i++) {
                debugInfo.add(" - " + hiddenModIds.get(i));
            }
            if (hiddenModIds.size() > 5) {
                debugInfo.add("  " + ChatFormatting.GRAY + "and " + (hiddenModIds.size() - 5) + " more mods");
            }
        }

        if (!hiddenLibIds.isEmpty()) {
            debugInfo.add("");
            debugInfo.add(ChatFormatting.RED + "Hidden Libraries:");
            for (int i = 0; i < Math.min(5, hiddenLibIds.size()); i++) {
                debugInfo.add("  " + hiddenLibIds.get(i));
            }
            if (hiddenLibIds.size() > 5) {
                debugInfo.add("  " + ChatFormatting.GRAY + "and " + (hiddenLibIds.size() - 5) + " more libraries");
            }
        }

        debugInfo.add("Allowed Channels: " + ConfigManager.data.modHiderAllowedCustomPayloadChannels.size());
    }
}
