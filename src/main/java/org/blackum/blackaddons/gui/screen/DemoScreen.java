package org.blackum.blackaddons.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.gui.render.ColorUtils;
import org.blackum.blackaddons.Blackaddons;

import java.util.Arrays;

public class DemoScreen extends BaseScreen {

        public DemoScreen() {
                super(Component.literal("Demo Screen"));
                this.showClickDebug = true;
        }

        private void sendMessage(String message) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                        Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal(
                                        ChatFormatting.AQUA + "[GUI] " + ChatFormatting.WHITE + message));
                }
        }

        @Override
        public void initWidgets() {
                int contentX = containerX + Theme.SPACING_LARGE;
                int contentY = containerY + Theme.SPACING_LARGE * 2;
                int contentWidth = containerWidth - Theme.SPACING_LARGE * 2;

                initGrid(contentX, contentY, contentWidth, Theme.GRID_COLUMNS, Theme.BUTTON_HEIGHT, Theme.GRID_GAP);

                addToGrid(new Button(0, 0, 0, "Primary Button",
                                () -> sendMessage("Primary clicked!")), 1);

                addToGrid(new Button(0, 0, 0, "Secondary Button",
                                () -> sendMessage("Secondary clicked!")), 1);

                addToGrid(new TextField(0, 0, 0, 40, "Tall text field..."), 2);

                addToGrid(new Slider(0, 0, 0, 16, 0f, 100f, 50f,
                                value -> sendMessage("Slider value: " + String.format("%.1f", value))), 2);

                addToGrid(new Checkbox(0, 0, 32, "Custom Height Checkbox", false,
                                checked -> sendMessage("Checkbox: " + (checked ? "Enabled" : "Disabled"))), 1);

                addToGrid(new Checkbox(0, 0, "Default Checkbox", false,
                                checked -> sendMessage("Feature B: " + (checked ? "Enabled" : "Disabled"))), 1);

                addToGrid(new Dropdown(0, 0, 0, "Select Option",
                                Arrays.asList("Option 1", "Option 2", "Option 3"),
                                selected -> sendMessage("Selected: " + selected)), 2);

                addToGrid(new ColorPicker(0, 0, Theme.ACCENT,
                                color -> sendMessage("Color changed: "
                                                + ColorUtils.toRGBA(color))),
                                1);

                currentGridRow += 4;
                currentGridColumn = 0;

                int currentGridBottom = gridStartY + (currentGridRow) * (gridRowHeight + gridGap) + gridRowHeight + 20;

                int radioWidth = (contentWidth - Theme.GRID_GAP * 2) / 3;
                for (int i = 0; i < 3; i++) {
                        int x = contentX + i * (radioWidth + Theme.GRID_GAP);
                        final String option = "Option " + (i + 1);
                        addWidget(new RadioButton(x, currentGridBottom, option, "grp1", false,
                                        selected -> {
                                                if (selected)
                                                        sendMessage(option + " selected");
                                        }));
                }

                int controlY = currentGridBottom + 40;

                addWidget(new Button(
                                contentX, controlY, (contentWidth - 20) / 2, "Toggle Hitboxes",
                                () -> {
                                        BaseScreen.showHitboxes = !BaseScreen.showHitboxes;
                                        sendMessage("Hitboxes: " + BaseScreen.showHitboxes);
                                }));

                addWidget(new Button(
                                contentX + (contentWidth + Theme.GRID_GAP) / 2, controlY,
                                (contentWidth - Theme.GRID_GAP) / 2,
                                "Toggle Overlay",
                                () -> {
                                        BaseScreen.showDebugOverlay = !BaseScreen.showDebugOverlay;
                                        sendMessage("Overlay: " + BaseScreen.showDebugOverlay);
                                }));

                addWidget(new Button(
                                contentX, controlY + 45, contentWidth,
                                "Open Overlay Editor",
                                () -> {
                                        if (!BaseScreen.showDebugOverlay) {
                                                BaseScreen.showDebugOverlay = true;
                                                sendMessage("Overlay auto-enabled for editing");
                                        }
                                if (Blackaddons.screenOpener != null) {
                                        Blackaddons.screenOpener.accept(new OverlayEditorScreen(this));
                                }

                                }));

                int startY = controlY + 90;
                for (int i = 0; i < 10; i++) {
                        int yPos = startY + (i * (Theme.BUTTON_HEIGHT + Theme.SPACING_SMALL));
                        final int idx = i + 1;
                        addWidget(new Button(contentX, yPos, 120, "Scroll Test Item " + idx,
                                        () -> sendMessage("Clicked Item " + idx)));
                }

                addWidget(new Button(
                                containerX + (containerWidth - 100) / 2, containerY + containerHeight, 100,
                                "Close",
                                () -> this.onClose()));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void extractScrolledContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                int contentX = containerX + 30;
                int contentY = containerY + 60;

                String titleText = "BlackAddons GUI Control Panel";
                int titleWidth = this.font.width(titleText);
                graphics.text(this.font, titleText, containerX + (containerWidth - titleWidth) / 2,
                                containerY + 20, -1);

                graphics.text(this.font, "Interaction Tests:", contentX, contentY - 20, Theme.TEXT_SECONDARY);
                graphics.text(this.font, "Selection Controls:", contentX, contentY + 130, Theme.TEXT_SECONDARY);
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }
}
