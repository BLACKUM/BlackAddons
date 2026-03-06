package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.*;

public class LegitTabController extends SimpleTabController {
    private ResizableCard visualsCard;

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
                screen.resetCardStates("legit_visuals");
            });
            legitTab.addWidget(resetLayout);

            CardContainer legitCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
            legitTab.addWidget(legitCardContainer);

            int currentY = contentY + 50;
            visualsCard = createVisualsCard(contentX + 20, currentY);

            legitCardContainer.addCard(visualsCard);
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

        visualsCard.updateLayout();
        return visualsCard;
    }
}
