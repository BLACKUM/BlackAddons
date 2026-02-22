package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.*;

public class CheatsTabController extends SimpleTabController {
    private ResizableCard autoTntCard;
    private ResizableCard fastLeapCard;

    public CheatsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab cheatsTab) {
        int contentX = cheatsTab.getParent().getContentX();
        int contentY = cheatsTab.getParent().getContentY();
        int contentWidth = cheatsTab.getParent().getContentWidth();

        if (!ConfigManager.data.useCardLayout) {
            cheatsTab.addWidget(new Label(contentX, contentY, "AutoTnt", Label.Style.TITLE));

            ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY + 30, contentWidth - 20,
                    "Enable AutoTnt",
                    "Automatically places TNT",
                    ConfigManager.data.autoTntConfig.AutoTNTEnabled, value -> {
                        ConfigManager.data.autoTntConfig.AutoTNTEnabled = value;
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(enableToggle);

            Label tickLabel = new Label(contentX, contentY + 80,
                    "Tick Delay: " + ConfigManager.data.autoTntConfig.AutoTNTDelay + " ticks", Label.Style.BODY);
            cheatsTab.addWidget(tickLabel);

            Slider tickSlider = new Slider(contentX, contentY + 100, contentWidth - 20, 3, 10,
                    ConfigManager.data.autoTntConfig.AutoTNTDelay, val -> {
                        int ticks = Math.round(val);
                        if (ticks != ConfigManager.data.autoTntConfig.AutoTNTDelay) {
                            ConfigManager.data.autoTntConfig.AutoTNTDelay = ticks;
                            tickLabel.setText("Tick Delay: " + ticks + " ticks");
                            ConfigManager.save();
                        }
                    });
            cheatsTab.addWidget(tickSlider);
            return;
        }

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("autoTnt");
        });
        cheatsTab.addWidget(resetLayout);

        CardContainer cheatsCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
        cheatsTab.addWidget(cheatsCardContainer);

        int currentY = contentY + 50;
        autoTntCard = createAutoTntCard(contentX + 20, currentY);
        cheatsCardContainer.addCard(autoTntCard);

        fastLeapCard = createFastLeapCard(contentX + 340, currentY);
        cheatsCardContainer.addCard(fastLeapCard);

        ResizableCard bloodBlinkCard = createBloodBlinkCard(contentX + 340, currentY + 140);
        cheatsCardContainer.addCard(bloodBlinkCard);
    }

    private ResizableCard createAutoTntCard(int x, int y) {
        autoTntCard = screen.createResizableCard("autoTnt", x, y, 300, 260, "AutoTnt");

        int contentX = autoTntCard.getContentX();
        int contentY = autoTntCard.getContentY();

        ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable AutoTnt",
                "Automatically places TNT",
                ConfigManager.data.autoTntConfig.AutoTNTEnabled, value -> {
                    ConfigManager.data.autoTntConfig.AutoTNTEnabled = value;
                    ConfigManager.save();
                });
        autoTntCard.addChild(enableToggle);

        Label tickLabel = new Label(contentX, contentY + 50,
                "Tick Delay: " + ConfigManager.data.autoTntConfig.AutoTNTDelay + " ticks", Label.Style.BODY);
        autoTntCard.addChild(tickLabel);

        Slider tickSlider = new Slider(contentX, contentY + 70, 260, 3, 10,
                ConfigManager.data.autoTntConfig.AutoTNTDelay, val -> {
                    int ticks = Math.round(val);
                    if (ticks != ConfigManager.data.autoTntConfig.AutoTNTDelay) {
                        ConfigManager.data.autoTntConfig.AutoTNTDelay = ticks;
                        tickLabel.setText("Tick Delay: " + ticks + " ticks");
                        ConfigManager.save();
                    }
                });
        autoTntCard.addChild(tickSlider);

        Label unequipLabel = new Label(contentX, contentY + 100,
                "Unequip Delay: " + ConfigManager.data.autoTntConfig.UnequipDelay + " ticks", Label.Style.BODY);
        autoTntCard.addChild(unequipLabel);

        Slider unequipSlider = new Slider(contentX, contentY + 120, 260, 3, 10,
                ConfigManager.data.autoTntConfig.UnequipDelay, val -> {
                    int ticks = Math.round(val);
                    if (ticks != ConfigManager.data.autoTntConfig.UnequipDelay) {
                        ConfigManager.data.autoTntConfig.UnequipDelay = ticks;
                        unequipLabel.setText("Unequip Delay: " + ticks + " ticks");
                        ConfigManager.save();
                    }
                });
        autoTntCard.addChild(unequipSlider);

        ToggleSwitch swapBackToggle = new ToggleSwitch(contentX, contentY + 150, 260,
                "Swap Back",
                "Switch to original item after interaction",
                ConfigManager.data.autoTntConfig.SwapBack, value -> {
                    ConfigManager.data.autoTntConfig.SwapBack = value;
                    ConfigManager.save();
                });
        autoTntCard.addChild(swapBackToggle);

        autoTntCard.updateLayout();
        return autoTntCard;
    }

    private ResizableCard createFastLeapCard(int x, int y) {
        fastLeapCard = screen.createResizableCard("fastLeap", x, y, 300, 120, "FastLeap");

        int contentX = fastLeapCard.getContentX();
        int contentY = fastLeapCard.getContentY();

        ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable FastLeap",
                "Automatically leaps to players or classes",
                ConfigManager.data.FastLeapEnabled, value -> {
                    ConfigManager.data.FastLeapEnabled = value;
                    ConfigManager.save();
                });
        fastLeapCard.addChild(enableToggle);

        ToggleSwitch doorOpenerToggle = new ToggleSwitch(contentX, contentY + 30, 260,
                "Door Opener",
                "Leap to the player who opens wither doors",
                ConfigManager.data.FastLeapDoorOpener, value -> {
                    ConfigManager.data.FastLeapDoorOpener = value;
                    ConfigManager.save();
                });
        fastLeapCard.addChild(doorOpenerToggle);

        fastLeapCard.updateLayout();
        return fastLeapCard;
    }

    private ResizableCard createBloodBlinkCard(int x, int y) {
        ResizableCard bloodBlinkCard = screen.createResizableCard("bloodBlink", x, y, 300, 100, "BloodBlink");

        int contentX = bloodBlinkCard.getContentX();
        int contentY = bloodBlinkCard.getContentY();

        ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable BloodBlink",
                "AOTV (48.9/-29.3) -> Pearls (-90 Yaw) x7",
                ConfigManager.data.BloodBlinkEnabled, value -> {
                    ConfigManager.data.BloodBlinkEnabled = value;
                    ConfigManager.save();
                });
        bloodBlinkCard.addChild(enableToggle);

        bloodBlinkCard.updateLayout();
        return bloodBlinkCard;
    }
}
