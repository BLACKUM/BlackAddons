package org.blackum.blackaddons.gui.screen.tabs;

import net.minecraft.client.Minecraft;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.BaseScreen;
import org.blackum.blackaddons.gui.screen.AlignOverlayPositionScreen;
import org.blackum.blackaddons.gui.screen.LocationOverlayPositionScreen;
import org.blackum.blackaddons.gui.screen.OverlayEditorScreen;
import org.blackum.blackaddons.gui.screen.RotationOverlayPositionScreen;
import org.blackum.blackaddons.gui.screen.AutoSSOverlayPositionScreen;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;

public class LegitTabController extends SimpleTabController {
    private ResizableCard visualsCard;
    private ResizableCard debuggersCard;

    public LegitTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab legitTab) {
        int contentX = legitTab.getParent().getContentX();
        int contentY = legitTab.getParent().getContentY();
        int contentWidth = legitTab.getParent().getContentWidth();

        if (ConfigManager.data.useCardLayout) {
            Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
                screen.resetCardStates("legit_visuals", "legit_debuggers");
            });
            legitTab.addWidget(resetLayout);

            CardContainer legitCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
            legitTab.addWidget(legitCardContainer);

            int containerY = contentY + 30;
            int numCols = contentWidth < 680 ? 1 : (contentWidth < 1000 ? 2 : 3);
            int colWidth = 300;
            int spacing = 20;
            int[] colY = new int[numCols];
            for (int i = 0; i < numCols; i++) colY[i] = containerY + 20;

            List<ResizableCard> cards = new ArrayList<>();
            visualsCard = createVisualsCard(0, 0);
            cards.add(visualsCard);
            debuggersCard = createDebuggersCard(0, 0);
            cards.add(debuggersCard);

            for (ResizableCard card : cards) {
                int shortestCol = 0;
                for (int i = 1; i < numCols; i++) {
                    if (colY[i] < colY[shortestCol]) shortestCol = i;
                }

                card.setX(contentX + spacing + shortestCol * (colWidth + spacing));
                card.setY(colY[shortestCol]);
                colY[shortestCol] += card.getHeight() + 10;
            }

            legitCardContainer.addCard(visualsCard);
            legitCardContainer.addCard(debuggersCard);
            return;
        }

        legitTab.addWidget(new Label(contentX, contentY, "Visuals", Label.Style.TITLE));

        ToggleSwitch fullbrightToggle = new ToggleSwitch(contentX, contentY + 30, contentWidth - 20,
                "Enable Fullbright",
                "Maximizes gamma (Night Vision)",
                ConfigManager.data.legitFullbrightEnabled, value -> {
                    ConfigManager.data.legitFullbrightEnabled = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(fullbrightToggle);

        ToggleSwitch fireOverlayToggle = new ToggleSwitch(contentX, contentY + 60, contentWidth - 20,
                "Remove Fire Overlay",
                "Hides the fire overlay when on fire",
                ConfigManager.data.removeFireOverlay, value -> {
                    ConfigManager.data.removeFireOverlay = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(fireOverlayToggle);

        ToggleSwitch hideStatusEffectsToggle = new ToggleSwitch(contentX, contentY + 90, contentWidth - 20,
                "Hide Status Effects",
                "Hides status effect icons from the HUD and inventory",
                ConfigManager.data.hideStatusEffects, value -> {
                    ConfigManager.data.hideStatusEffects = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(hideStatusEffectsToggle);

        ToggleSwitch disableNearbyParticlesToggle = new ToggleSwitch(contentX, contentY + 120, contentWidth - 20,
                "Disable Nearby Particles",
                "Visually disables particles within 3 blocks of the player",
                ConfigManager.data.disableNearbyParticles, value -> {
                    ConfigManager.data.disableNearbyParticles = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(disableNearbyParticlesToggle);

        legitTab.addWidget(new Label(contentX, contentY + 170, "Debuggers", Label.Style.TITLE));

        int debugY = contentY + 200;
        addDebuggerWidgets(legitTab, contentX, debugY, contentWidth - 20);
    }

    private ResizableCard createVisualsCard(int x, int y) {
        visualsCard = screen.createResizableCard("legit_visuals", x, y, 300, 100, "Visuals");
        int contentX = visualsCard.getContentX();
        int contentY = visualsCard.getContentY();

        ToggleSwitch fullbrightToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable Fullbright",
                "Maximizes gamma (Night Vision)",
                ConfigManager.data.legitFullbrightEnabled, value -> {
                    ConfigManager.data.legitFullbrightEnabled = value;
                    ConfigManager.save();
                });
        visualsCard.addChild(fullbrightToggle);

        ToggleSwitch fireOverlayToggle = new ToggleSwitch(contentX, contentY + 30, 260,
                "Remove Fire Overlay",
                "Hides the fire overlay when on fire",
                ConfigManager.data.removeFireOverlay, value -> {
                    ConfigManager.data.removeFireOverlay = value;
                    ConfigManager.save();
                });
        visualsCard.addChild(fireOverlayToggle);

        ToggleSwitch hideStatusEffectsToggle = new ToggleSwitch(contentX, contentY + 60, 260,
                "Hide Status Effects",
                "Hides status effect icons from the HUD and inventory",
                ConfigManager.data.hideStatusEffects, value -> {
                    ConfigManager.data.hideStatusEffects = value;
                    ConfigManager.save();
                });
        visualsCard.addChild(hideStatusEffectsToggle);

        ToggleSwitch disableNearbyParticlesToggle = new ToggleSwitch(contentX, contentY + 90, 260,
                "Disable Nearby Particles",
                "Visually disables particles within 2 blocks of the player",
                ConfigManager.data.disableNearbyParticles, value -> {
                    ConfigManager.data.disableNearbyParticles = value;
                    ConfigManager.save();
                });
        visualsCard.addChild(disableNearbyParticlesToggle);

        visualsCard.updateLayout();
        return visualsCard;
    }

    private ResizableCard createDebuggersCard(int x, int y) {
        debuggersCard = screen.createResizableCard("legit_debuggers", x, y, 300, 250, "Debuggers");
        int contentX = debuggersCard.getContentX();
        int contentY = debuggersCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 200);
        addDebuggerItems(listView);

        debuggersCard.addChild(listView);
        debuggersCard.updateLayout();
        return debuggersCard;
    }

    private void addDebuggerWidgets(TabPanel.Tab legitTab, int x, int startY, int width) {
        int y = startY;

        ToggleSwitch debugOverlayToggle = new ToggleSwitch(x, y, width,
                "Global Debug Overlay",
                "Shows the shared BlackAddons debug HUD",
                BaseScreen.showDebugOverlay, value -> {
                    BaseScreen.showDebugOverlay = value;
                    ConfigManager.data.showDebugOverlay = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(debugOverlayToggle);
        y += 30;

        Button overlayPositionButton = new Button(x, y, width, 20, "Edit Global Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new OverlayEditorScreen(screen))));
        legitTab.addWidget(overlayPositionButton);
        y += 30;

        ToggleSwitch autoSSDebugToggle = new ToggleSwitch(x, y, width,
                "AutoSS Debug",
                "Shows AutoSS overlay and writes debug logs",
                ConfigManager.data.AutoSSDebug, value -> {
                    ConfigManager.data.AutoSSDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(autoSSDebugToggle);
        y += 30;

        Button autoSSPositionButton = new Button(x, y, width, 20, "Set AutoSS Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new AutoSSOverlayPositionScreen(screen))));
        legitTab.addWidget(autoSSPositionButton);
        y += 30;

        ToggleSwitch fastLeapDebugToggle = new ToggleSwitch(x, y, width,
                "FastLeap Debug",
                "Shows FastLeap debug messages in chat",
                ConfigManager.data.FastLeapDebug, value -> {
                    ConfigManager.data.FastLeapDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(fastLeapDebugToggle);
        y += 30;

        ToggleSwitch rotationDebugToggle = new ToggleSwitch(x, y, width,
                "Rotation Debugger",
                "Shows target rotation info and world visuals",
                ConfigManager.data.showRotationDebug, value -> {
                    ConfigManager.data.showRotationDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(rotationDebugToggle);
        y += 30;

        Button rotationPositionButton = new Button(x, y, width, 20, "Set Rotation Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new RotationOverlayPositionScreen(screen))));
        legitTab.addWidget(rotationPositionButton);
        y += 30;

        ToggleSwitch locationDebugToggle = new ToggleSwitch(x, y, width,
                "Location Utils Debug",
                "Shows location, floor, boss and phase debug info",
                ConfigManager.data.showLocationDebug, value -> {
                    ConfigManager.data.showLocationDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(locationDebugToggle);
        y += 30;

        Button locationPositionButton = new Button(x, y, width, 20, "Set Location Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new LocationOverlayPositionScreen(screen))));
        legitTab.addWidget(locationPositionButton);
        y += 30;

        ToggleSwitch alignDebugToggle = new ToggleSwitch(x, y, width,
                "Align Debugger",
                "Shows expected align math and measured final position",
                ConfigManager.data.showAlignDebug, value -> {
                    ConfigManager.data.showAlignDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(alignDebugToggle);
        y += 30;

        Button alignPositionButton = new Button(x, y, width, 20, "Set Align Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new AlignOverlayPositionScreen(screen))));
        legitTab.addWidget(alignPositionButton);
    }

    private void addDebuggerItems(ListView listView) {
        listView.addItem(new ToggleSwitch(0, 0, 260,
                "Global Debug Overlay",
                "Shows the shared BlackAddons debug HUD",
                BaseScreen.showDebugOverlay, value -> {
                    BaseScreen.showDebugOverlay = value;
                    ConfigManager.data.showDebugOverlay = value;
                    ConfigManager.save();
                }));

        listView.addItem(new Button(0, 0, 260, 20, "Edit Global Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new OverlayEditorScreen(screen)))));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "AutoSS Debug",
                "Shows AutoSS overlay and writes debug logs",
                ConfigManager.data.AutoSSDebug, value -> {
                    ConfigManager.data.AutoSSDebug = value;
                    ConfigManager.save();
                }));

        listView.addItem(new Button(0, 0, 260, 20, "Set AutoSS Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new AutoSSOverlayPositionScreen(screen)))));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "FastLeap Debug",
                "Shows FastLeap debug messages in chat",
                ConfigManager.data.FastLeapDebug, value -> {
                    ConfigManager.data.FastLeapDebug = value;
                    ConfigManager.save();
                }));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "Rotation Debugger",
                "Shows target rotation info and world visuals",
                ConfigManager.data.showRotationDebug, value -> {
                    ConfigManager.data.showRotationDebug = value;
                    ConfigManager.save();
                }));

        listView.addItem(new Button(0, 0, 260, 20, "Set Rotation Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new RotationOverlayPositionScreen(screen)))));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "Location Utils Debug",
                "Shows location, floor, boss and phase debug info",
                ConfigManager.data.showLocationDebug, value -> {
                    ConfigManager.data.showLocationDebug = value;
                    ConfigManager.save();
                }));

        listView.addItem(new Button(0, 0, 260, 20, "Set Location Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new LocationOverlayPositionScreen(screen)))));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "Align Debugger",
                "Shows expected align math and measured final position",
                ConfigManager.data.showAlignDebug, value -> {
                    ConfigManager.data.showAlignDebug = value;
                    ConfigManager.save();
                }));

        listView.addItem(new Button(0, 0, 260, 20, "Set Align Overlay Position", () ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new AlignOverlayPositionScreen(screen)))));
    }
}
