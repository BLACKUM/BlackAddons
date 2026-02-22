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
        fastLeapCard = screen.createResizableCard("fastLeap", x, y, 300, 360, "FastLeap");

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

        ToggleSwitch positionalToggle = new ToggleSwitch(contentX, contentY + 60, 260,
                "Positional Leap",
                "Leap to classes automatically depending on the phase",
                ConfigManager.data.FastLeapPositional, value -> {
                    ConfigManager.data.FastLeapPositional = value;
                    ConfigManager.save();
                });
        fastLeapCard.addChild(positionalToggle);

        Label s1Label = new Label(contentX, contentY + 100, "S1 Class Name:", Label.Style.BODY);
        fastLeapCard.addChild(s1Label);
        TextField s1Field = new TextField(contentX, contentY + 120, 260, "e.g. Mage");
        s1Field.setText(ConfigManager.data.FastLeapS1);
        s1Field.setOnValueChange(value -> {
            ConfigManager.data.FastLeapS1 = value;
            ConfigManager.save();
        });
        fastLeapCard.addChild(s1Field);

        Label s2Label = new Label(contentX, contentY + 150, "S2 Class Name:", Label.Style.BODY);
        fastLeapCard.addChild(s2Label);
        TextField s2Field = new TextField(contentX, contentY + 170, 260, "e.g. Berserk");
        s2Field.setText(ConfigManager.data.FastLeapS2);
        s2Field.setOnValueChange(value -> {
            ConfigManager.data.FastLeapS2 = value;
            ConfigManager.save();
        });
        fastLeapCard.addChild(s2Field);

        Label s3Label = new Label(contentX, contentY + 200, "S3 Class Name:", Label.Style.BODY);
        fastLeapCard.addChild(s3Label);
        TextField s3Field = new TextField(contentX, contentY + 220, 260, "e.g. Healer");
        s3Field.setText(ConfigManager.data.FastLeapS3);
        s3Field.setOnValueChange(value -> {
            ConfigManager.data.FastLeapS3 = value;
            ConfigManager.save();
        });
        fastLeapCard.addChild(s3Field);

        Label s4Label = new Label(contentX, contentY + 250, "S4 Class Name:", Label.Style.BODY);
        fastLeapCard.addChild(s4Label);
        TextField s4Field = new TextField(contentX, contentY + 270, 260, "e.g. Archer");
        s4Field.setText(ConfigManager.data.FastLeapS4);
        s4Field.setOnValueChange(value -> {
            ConfigManager.data.FastLeapS4 = value;
            ConfigManager.save();
        });
        fastLeapCard.addChild(s4Field);

        fastLeapCard.updateLayout();
        return fastLeapCard;
    }
}
