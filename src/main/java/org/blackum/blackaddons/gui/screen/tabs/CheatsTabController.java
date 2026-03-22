package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.gui.screen.AutoSSOverlayPositionScreen;
import org.blackum.blackaddons.gui.screen.RotationOverlayPositionScreen;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.blackum.blackaddons.gui.widget.*;

import java.util.List;

public class CheatsTabController extends SimpleTabController {
    private ResizableCard autoTntCard;
    private ResizableCard autoSSCard;
    private ResizableCard fastLeapCard;
    private ResizableCard rotationCard;
    private ResizableCard autoBMCard;
    private ResizableCard freecamCard;
    private Dropdown s1Dropdown;
    private Dropdown s2Dropdown;
    private Dropdown s3Dropdown;
    private Dropdown s4Dropdown;
    private Label startDelayLabel;
    private Slider startDelaySlider;

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

            cheatsTab.addWidget(new Label(contentX, contentY + 140, "AutoSS Solver", Label.Style.TITLE));

            ToggleSwitch ssEnableToggle = new ToggleSwitch(contentX, contentY + 170, contentWidth - 20,
                    "Enable AutoSS",
                    "Automatically solves F7/M7 Simon Says",
                    ConfigManager.data.AutoSSEnabled, value -> {
                        ConfigManager.data.AutoSSEnabled = value;
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(ssEnableToggle);

            Label ssTickLabel = new Label(contentX, contentY + 220,
                    delayLabel(ConfigManager.data.AutoSSDelay), Label.Style.BODY);
            cheatsTab.addWidget(ssTickLabel);

            Slider ssTickSlider = new Slider(contentX, contentY + 240, contentWidth - 20, 0, 20,
                    ConfigManager.data.AutoSSDelay, val -> {
                        int ticks = Math.round(val);
                        if (ticks != ConfigManager.data.AutoSSDelay) {
                            ConfigManager.data.AutoSSDelay = ticks;
                            ssTickLabel.setText(delayLabel(ticks));
                            ConfigManager.save();
                        }
                    });
            cheatsTab.addWidget(ssTickSlider);

            Label ssDistLabel = new Label(contentX, contentY + 290,
                    String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", ConfigManager.data.AutoSSDistanceLimit), Label.Style.BODY);
            cheatsTab.addWidget(ssDistLabel);

            Slider ssDistSlider = new Slider(contentX, contentY + 310, contentWidth - 20, 2.0f, 10.0f,
                    ConfigManager.data.AutoSSDistanceLimit, val -> {
                        ConfigManager.data.AutoSSDistanceLimit = val;
                        ssDistLabel.setText(String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", val));
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(ssDistSlider);
            
            cheatsTab.addWidget(new Label(contentX, contentY + 360, "Auto Ballista Mechanic", Label.Style.TITLE));
            
            ToggleSwitch bmEnableToggle = new ToggleSwitch(contentX, contentY + 390, contentWidth - 20,
                    "Enable AutoBM",
                    "Automatically clicks ballista upgrades",
                    ConfigManager.data.autoBMConfig.AutoBMEnabled, value -> {
                        ConfigManager.data.autoBMConfig.AutoBMEnabled = value;
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(bmEnableToggle);

            return;
        }

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("autoTnt", "autoSS", "fastLeap", "rotationSet", "autoBM", "freecam");
        });
        cheatsTab.addWidget(resetLayout);

        CardContainer cheatsCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
        cheatsTab.addWidget(cheatsCardContainer);

        int containerY = contentY + 30;
        boolean isSingleColumn = contentWidth < 680;
        int col1X = contentX + 20;
        int col2X = contentX + 340;

        if (isSingleColumn) {
            autoTntCard = createAutoTntCard(col1X, containerY + 20);
            int currentY = containerY + 20 + autoTntCard.getHeight() + 10;

            autoSSCard = createAutoSSCard(col1X, currentY);
            currentY += autoSSCard.getHeight() + 10;

            fastLeapCard = createFastLeapCard(col1X, currentY);
            currentY += fastLeapCard.getHeight() + 10;

            rotationCard = createRotationCard(col1X, currentY);
            currentY += rotationCard.getHeight() + 10;

            autoBMCard = createAutoBM(col1X, currentY);
            currentY += autoBMCard.getHeight() + 10;

            freecamCard = createFreecamCard(col1X, currentY);
            currentY += freecamCard.getHeight() + 10;
        } else {
            int currentY1 = containerY + 20;
            int currentY2 = containerY + 20;

            autoTntCard = createAutoTntCard(col1X, currentY1);
            currentY1 += autoTntCard.getHeight() + 10;

            fastLeapCard = createFastLeapCard(col1X, currentY1);
            currentY1 += fastLeapCard.getHeight() + 10;

            autoBMCard = createAutoBM(col1X, currentY1);
            currentY1 += autoBMCard.getHeight() + 10;

            freecamCard = createFreecamCard(col1X, currentY1);
            currentY1 += freecamCard.getHeight() + 10;

            autoSSCard = createAutoSSCard(col2X, currentY2);
            currentY2 += autoSSCard.getHeight() + 10;

            rotationCard = createRotationCard(col2X, currentY2);
            currentY2 += rotationCard.getHeight() + 10;
        }

        cheatsCardContainer.addCard(autoTntCard);
        cheatsCardContainer.addCard(autoSSCard);
        cheatsCardContainer.addCard(fastLeapCard);
        cheatsCardContainer.addCard(rotationCard);
        cheatsCardContainer.addCard(autoBMCard);
        cheatsCardContainer.addCard(freecamCard);
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

    private ResizableCard createAutoSSCard(int x, int y) {
        autoSSCard = screen.createResizableCard("autoSS", x, y, 300, 310, "AutoSS Solver");

        int contentX = autoSSCard.getContentX();
        int contentY = autoSSCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 260);

        ToggleSwitch enableToggle = new ToggleSwitch(0, 0, 260,
                "Enable AutoSS",
                "Automatically solves F7 devices",
                ConfigManager.data.AutoSSEnabled, value -> {
            ConfigManager.data.AutoSSEnabled = value;
            ConfigManager.save();
        });
        listView.addItem(enableToggle);

        Label tickLabel = new Label(0, 0,
                delayLabel(ConfigManager.data.AutoSSDelay), Label.Style.BODY);
        listView.addItem(tickLabel);

        Slider tickSlider = new Slider(0, 0, 260, 0, 20,
                ConfigManager.data.AutoSSDelay, val -> {
            int ticks = Math.round(val);
            if (ticks != ConfigManager.data.AutoSSDelay) {
                ConfigManager.data.AutoSSDelay = ticks;
                tickLabel.setText(delayLabel(ticks));
                ConfigManager.save();
            }
        });
        listView.addItem(tickSlider);

        Label distLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", ConfigManager.data.AutoSSDistanceLimit), Label.Style.BODY);
        listView.addItem(distLabel);

        Slider distSlider = new Slider(0, 0, 260, 2.0f, 10.0f,
                ConfigManager.data.AutoSSDistanceLimit, val -> {
            ConfigManager.data.AutoSSDistanceLimit = val;
            distLabel.setText(String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", val));
            ConfigManager.save();
        });
        listView.addItem(distSlider);

        Label speedLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Rotation Speed: %.1f", ConfigManager.data.AutoSSRotationSpeed), Label.Style.BODY);
        listView.addItem(speedLabel);

        Slider speedSlider = new Slider(0, 0, 260, 1.0f, 50.0f,
                ConfigManager.data.AutoSSRotationSpeed, val -> {
            ConfigManager.data.AutoSSRotationSpeed = val;
            speedLabel.setText(String.format(java.util.Locale.ROOT, "Rotation Speed: %.1f", val));
            ConfigManager.save();
        });
        listView.addItem(speedSlider);

        Label curveLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Rotation Curve: %.0f%%", ConfigManager.data.AutoSSRotationCurve * 100), Label.Style.BODY);
        listView.addItem(curveLabel);

        Slider curveSlider = new Slider(0, 0, 260, 0.0f, 200.0f,
                ConfigManager.data.AutoSSRotationCurve * 100, val -> {
            ConfigManager.data.AutoSSRotationCurve = val / 100f;
            curveLabel.setText(String.format(java.util.Locale.ROOT, "Rotation Curve: %.0f%%", val));
            ConfigManager.save();
        });
        listView.addItem(curveSlider);

        ToggleSwitch trySkipToggle = new ToggleSwitch(0, 0, 260,
                "Try SS Skip",
                "Clicks start button 3 times for potential skip",
                ConfigManager.data.AutoSSTrySkip, value -> {
            ConfigManager.data.AutoSSTrySkip = value;
            ConfigManager.save();
        });
        trySkipToggle.setVisible(ConfigManager.data.AutoSSAutoStart);

        ToggleSwitch autoStartToggle = new ToggleSwitch(0, 0, 260,
                "Auto SS Start",
                "Automatically aims and clicks the start button",
                ConfigManager.data.AutoSSAutoStart, value -> {
            ConfigManager.data.AutoSSAutoStart = value;
            trySkipToggle.setVisible(value);
            startDelayLabel.setVisible(value);
            startDelaySlider.setVisible(value);
            ConfigManager.save();
        });
        listView.addItem(autoStartToggle);

        startDelayLabel = new Label(0, 0,
                startDelayLabel(ConfigManager.data.AutoSSAutoStartDelay), Label.Style.BODY);
        startDelayLabel.setVisible(ConfigManager.data.AutoSSAutoStart);
        listView.addItem(startDelayLabel);

        startDelaySlider = new Slider(0, 0, 260, 0, 40,
                ConfigManager.data.AutoSSAutoStartDelay, val -> {
            int ticks = Math.round(val);
            if (ticks != ConfigManager.data.AutoSSAutoStartDelay) {
                ConfigManager.data.AutoSSAutoStartDelay = ticks;
                startDelayLabel.setText(startDelayLabel(ticks));
                ConfigManager.save();
            }
        });
        startDelaySlider.setVisible(ConfigManager.data.AutoSSAutoStart);
        listView.addItem(startDelaySlider);

        listView.addItem(trySkipToggle);

        ToggleSwitch swapToItemToggle = new ToggleSwitch(0, 0, 260,
                "Swap to InfiniLeap on Complete",
                "Swaps to a specific item when device is finished",
                ConfigManager.data.AutoSSSwapToItem, value -> {
            ConfigManager.data.AutoSSSwapToItem = value;
            ConfigManager.save();
        });
        listView.addItem(swapToItemToggle);

        Dropdown swapModeDropdown = new Dropdown(0, 0, 260,
                "Select what to do when device is finished",
                List.of("Swap to InfiniLeap", "Swap and Open"),
                mode -> {
                    ConfigManager.data.AutoSSSwapMode = mode.equals("Swap and Open") ? 1 : 0;
                    ConfigManager.save();
                });
        swapModeDropdown.setSelectedIndex(ConfigManager.data.AutoSSSwapMode);
        listView.addItem(swapModeDropdown);

        ToggleSwitch debugToggle = new ToggleSwitch(0, 0, 260,
                "Debug Mode",
                "Shows debug overlay and logs to a file",
                ConfigManager.data.AutoSSDebug, value -> {
            ConfigManager.data.AutoSSDebug = value;
            ConfigManager.save();
        });
        listView.addItem(debugToggle);

        Button moveOverlayButton = new Button(0, 0, 260, 20, "Set Overlay Position", () -> {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.minecraft.client.Minecraft.getInstance().setScreen(new AutoSSOverlayPositionScreen(screen));
            });
        });
        listView.addItem(moveOverlayButton);

        autoSSCard.addChild(listView);
        autoSSCard.updateLayout();
        return autoSSCard;
    }

    private ResizableCard createFastLeapCard(int x, int y) {
        fastLeapCard = screen.createResizableCard("fastLeap", x, y, 300, 310, "⚠ FastLeap [WIP] (S1 is currently bugged)");

        int contentX = fastLeapCard.getContentX();
        int contentY = fastLeapCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 260);

        List<String> classOptions = List.of("NONE", "HEALER", "MAGE", "BERSERK", "ARCHER", "TANK");

        ToggleSwitch enableToggle = new ToggleSwitch(0, 0, 260,
                "⚠ Enable FastLeap [WIP]",
                "Automatically leaps to players or classes (S1 is currently bugged)",
                ConfigManager.data.FastLeapEnabled, value -> {
                    ConfigManager.data.FastLeapEnabled = value;
                    ConfigManager.save();
                });
        listView.addItem(enableToggle);

        ToggleSwitch doorOpenerToggle = new ToggleSwitch(0, 0, 260,
                "Door Opener",
                "Leap to the player who opens wither doors",
                ConfigManager.data.FastLeapDoorOpener, value -> {
                    ConfigManager.data.FastLeapDoorOpener = value;
                    ConfigManager.save();
                });
        listView.addItem(doorOpenerToggle);

        ToggleSwitch positionalToggle = new ToggleSwitch(0, 0, 260,
                "Positional",
                "Leap to a class based on your S-room position",
                ConfigManager.data.FastLeapPositional, value -> {
                    ConfigManager.data.FastLeapPositional = value;
                    ConfigManager.save();
                });
        listView.addItem(positionalToggle);

        listView.addItem(new Label(0, 0, "S1 Class", Label.Style.BODY));
        s1Dropdown = new Dropdown(0, 0, 260, "S1 Class", classOptions, value -> {
            ConfigManager.data.FastLeapS1Class = value;
            ConfigManager.save();
        });
        s1Dropdown.setSelectedOption(ConfigManager.data.FastLeapS1Class);
        s1Dropdown.setOnExpand(() -> collapseOtherDropdowns(s1Dropdown));
        listView.addItem(s1Dropdown);

        listView.addItem(new Label(0, 0, "S2 Class", Label.Style.BODY));
        s2Dropdown = new Dropdown(0, 0, 260, "S2 Class", classOptions, value -> {
            ConfigManager.data.FastLeapS2Class = value;
            ConfigManager.save();
        });
        s2Dropdown.setSelectedOption(ConfigManager.data.FastLeapS2Class);
        s2Dropdown.setOnExpand(() -> collapseOtherDropdowns(s2Dropdown));
        listView.addItem(s2Dropdown);

        listView.addItem(new Label(0, 0, "S3 Class", Label.Style.BODY));
        s3Dropdown = new Dropdown(0, 0, 260, "S3 Class", classOptions, value -> {
            ConfigManager.data.FastLeapS3Class = value;
            ConfigManager.save();
        });
        s3Dropdown.setSelectedOption(ConfigManager.data.FastLeapS3Class);
        s3Dropdown.setOnExpand(() -> collapseOtherDropdowns(s3Dropdown));
        listView.addItem(s3Dropdown);

        listView.addItem(new Label(0, 0, "S4 Class", Label.Style.BODY));
        s4Dropdown = new Dropdown(0, 0, 260, "S4 Class", classOptions, value -> {
            ConfigManager.data.FastLeapS4Class = value;
            ConfigManager.save();
        });
        s4Dropdown.setSelectedOption(ConfigManager.data.FastLeapS4Class);
        s4Dropdown.setOnExpand(() -> collapseOtherDropdowns(s4Dropdown));
        listView.addItem(s4Dropdown);

        ToggleSwitch debugToggle = new ToggleSwitch(0, 0, 260,
                "Debug Mode",
                "Shows FastLeap debug messages in chat",
                ConfigManager.data.FastLeapDebug, value -> {
                    ConfigManager.data.FastLeapDebug = value;
                    ConfigManager.save();
                });
        listView.addItem(debugToggle);

        fastLeapCard.addChild(listView);
        fastLeapCard.updateLayout();
        return fastLeapCard;
    }

    private void collapseOtherDropdowns(Dropdown active) {
        if (s1Dropdown != null && s1Dropdown != active) s1Dropdown.collapse();
        if (s2Dropdown != null && s2Dropdown != active) s2Dropdown.collapse();
        if (s3Dropdown != null && s3Dropdown != active) s3Dropdown.collapse();
        if (s4Dropdown != null && s4Dropdown != active) s4Dropdown.collapse();
    }

    private ResizableCard createAutoBM(int x, int y) {
        // 1. Initialize the correct variable
        autoBMCard = screen.createResizableCard("autoBM", x, y, 300, 310, "Auto Ballista Mechanic");

        int contentX = autoBMCard.getContentX();
        int contentY = autoBMCard.getContentY();

        // Use a ListView to handle the multiple sliders/labels
        ListView listView = new ListView(contentX, contentY, 260, 260);

        // Toggle
        ToggleSwitch enabled = new ToggleSwitch(0, 0, 260,
                "Enabled",
                "Enabled and disables auto BM",
                ConfigManager.data.autoBMConfig.AutoBMEnabled, value -> {
            ConfigManager.data.autoBMConfig.AutoBMEnabled = value;
            ConfigManager.save();
        }
        );
        listView.addItem(enabled);

        // Min FC Delay
        Label min_fc_label = new Label(0, 0,
                String.format("Min first click delay: %.0fms", ConfigManager.data.autoBMConfig.min_fc_delay),
                Label.Style.BODY);
        listView.addItem(min_fc_label);

        Slider min_fc_slider = new Slider(0, 0, 260, 10.f, 1000.f,
                ConfigManager.data.autoBMConfig.min_fc_delay, val -> {
            ConfigManager.data.autoBMConfig.min_fc_delay = val;
            min_fc_label.setText(String.format("Min first click delay: %.0fms", val));
            ConfigManager.save();
        });
        listView.addItem(min_fc_slider);

        // Max FC Delay
        Label max_fc_label = new Label(0, 0,
                String.format("Max first click delay: %.0fms", ConfigManager.data.autoBMConfig.max_fc_delay),
                Label.Style.BODY);
        listView.addItem(max_fc_label);

        Slider max_fc_slider = new Slider(0, 0, 260, 10.f, 1000.f,
                ConfigManager.data.autoBMConfig.max_fc_delay, val -> {
            ConfigManager.data.autoBMConfig.max_fc_delay = val;
            max_fc_label.setText(String.format("Max first click delay: %.0fms", val));
            ConfigManager.save();
        });
        listView.addItem(max_fc_slider);

        // Min Between Click
        Label min_between_click_label = new Label(0, 0,
                String.format("Min between click delay: %.0fms", ConfigManager.data.autoBMConfig.min_between_click_delay),
                Label.Style.BODY);
        listView.addItem(min_between_click_label);

        Slider min_between_click_slider = new Slider(0, 0, 260, 10.f, 1000.f,
                ConfigManager.data.autoBMConfig.min_between_click_delay, val -> {
            ConfigManager.data.autoBMConfig.min_between_click_delay = val;
            min_between_click_label.setText(String.format("Min between click delay: %.0fms", val));
            ConfigManager.save();
        });
        listView.addItem(min_between_click_slider);

        // Max Between Click
        Label max_between_click_label = new Label(0, 0,
                String.format("Max between click delay: %.0fms", ConfigManager.data.autoBMConfig.max_between_click_delay),
                Label.Style.BODY);
        listView.addItem(max_between_click_label);

        Slider max_between_click_slider = new Slider(0, 0, 260, 10.f, 1000.f,
                ConfigManager.data.autoBMConfig.max_between_click_delay, val -> {
            ConfigManager.data.autoBMConfig.max_between_click_delay = val;
            max_between_click_label.setText(String.format("Max between click delay: %.0fms", val));
            ConfigManager.save();
        });
        listView.addItem(max_between_click_slider);

        // Add the list to the card and update
        autoBMCard.addChild(listView);
        autoBMCard.updateLayout();

        return autoBMCard;
    }

    private ResizableCard createRotationCard(int x, int y) {
        rotationCard = screen.createResizableCard("rotationSet", x, y, 300, 310, "Rotation Settings (custom actions)");
        int contentX = rotationCard.getContentX();
        int contentY = rotationCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 260);

        ToggleSwitch rotationDebugToggle = new ToggleSwitch(0, 0, 260,
                "Rotation Debugger",
                "Shows target rotation info and world visuals",
                ConfigManager.data.showRotationDebug, value -> {
                    ConfigManager.data.showRotationDebug = value;
                    ConfigManager.save();
                });
        listView.addItem(rotationDebugToggle);

        Button positionButton = new Button(0, 0, 260, 20, "Set Overlay Position", () ->
                net.minecraft.client.Minecraft.getInstance().execute(() ->
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                                new RotationOverlayPositionScreen(screen))));
        listView.addItem(positionButton);

        ToggleSwitch humanizerToggle = new ToggleSwitch(0, 0, 260,
                "Enable Humanizer",
                "Applies curved paths and randomized targeting",
                ConfigManager.data.rotationHumanizerEnabled, value -> {
                    ConfigManager.data.rotationHumanizerEnabled = value;
                    ConfigManager.save();
                });
        listView.addItem(humanizerToggle);

        Label curveLabel = new Label(0, 0,
                String.format("Rotation Curve: %.0f%%", ConfigManager.data.rotationVariance * 100),
                Label.Style.BODY);
        listView.addItem(curveLabel);
        Slider curveSlider = new Slider(0, 0, 260, 0, 50,
                ConfigManager.data.rotationVariance * 100, val -> {
            ConfigManager.data.rotationVariance = val / 100f;
            curveLabel.setText(String.format("Rotation Curve: %.0f%%", val));
            ConfigManager.save();
        });
        listView.addItem(curveSlider);

        Label randomLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Target Randomness: %.2f blocks", ConfigManager.data.rotationTargetRandomness),
                Label.Style.BODY);
        listView.addItem(randomLabel);
        Slider randomSlider = new Slider(0, 0, 260, 0.0f, 0.49f,
                ConfigManager.data.rotationTargetRandomness, val -> {
                    ConfigManager.data.rotationTargetRandomness = val;
                    randomLabel.setText(String.format(java.util.Locale.ROOT, "Target Randomness: %.2f blocks", val));
                    ConfigManager.save();
                });
        listView.addItem(randomSlider);

        Label speedLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Speed: %.1f", ConfigManager.data.rotationSpeed),
                Label.Style.BODY);
        listView.addItem(speedLabel);
        Slider speedSlider = new Slider(0, 0, 260, 0.5f, 50.0f,
                ConfigManager.data.rotationSpeed, val -> {
                    ConfigManager.data.rotationSpeed = val;
                    speedLabel.setText(String.format(java.util.Locale.ROOT, "Speed: %.1f", val));
                    ConfigManager.save();
                });
        listView.addItem(speedSlider);

        Label slowdownLabel = new Label(0, 0,
                String.format("Distance Slowdown Rate: %.0f%%", ConfigManager.data.rotationDistanceSlowdown * 100),
                Label.Style.BODY);
        listView.addItem(slowdownLabel);
        Slider slowdownSlider = new Slider(0, 0, 260, 0.0f, 500.0f,
                ConfigManager.data.rotationDistanceSlowdown * 100, val -> {
                    ConfigManager.data.rotationDistanceSlowdown = val / 100f;
                    slowdownLabel.setText(String.format("Distance Slowdown Rate: %.0f%%", val));
                    ConfigManager.save();
                });
        listView.addItem(slowdownSlider);

        Label slowdownRadiusLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Max Slowdown Distance: %.0f blocks", ConfigManager.data.rotationDistanceRadius),
                Label.Style.BODY);
        listView.addItem(slowdownRadiusLabel);
        Slider slowdownRadiusSlider = new Slider(0, 0, 260, 5.0f, 150.0f,
                ConfigManager.data.rotationDistanceRadius, val -> {
                    ConfigManager.data.rotationDistanceRadius = val;
                    slowdownRadiusLabel.setText(String.format(java.util.Locale.ROOT, "Max Slowdown Distance: %.0f blocks", val));
                    ConfigManager.save();
                });
        listView.addItem(slowdownRadiusSlider);

        Label fovLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Distance Slowdown FOV: %.0f°", ConfigManager.data.rotationFovSlowdown),
                Label.Style.BODY);
        listView.addItem(fovLabel);
        Slider fovSlider = new Slider(0, 0, 260, 0.0f, 180.0f,
                ConfigManager.data.rotationFovSlowdown, val -> {
                    ConfigManager.data.rotationFovSlowdown = val;
                    fovLabel.setText(String.format(java.util.Locale.ROOT, "Distance Slowdown FOV: %.0f°", val));
                    ConfigManager.save();
                });
        listView.addItem(fovSlider);

        Label smoothLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Smoothness: %.2f", ConfigManager.data.rotationSmoothness),
                Label.Style.BODY);
        listView.addItem(smoothLabel);
        Slider smoothSlider = new Slider(0, 0, 260, 0.0f, 1.0f,
                ConfigManager.data.rotationSmoothness, val -> {
                    ConfigManager.data.rotationSmoothness = val;
                    smoothLabel.setText(String.format(java.util.Locale.ROOT, "Smoothness: %.2f", val));
                    ConfigManager.save();
                });
        listView.addItem(smoothSlider);

        Label thresholdLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Stop Threshold: %.2f", ConfigManager.data.rotationStopThreshold),
                Label.Style.BODY);
        listView.addItem(thresholdLabel);
        Slider thresholdSlider = new Slider(0, 0, 260, 0.01f, 5.0f,
                ConfigManager.data.rotationStopThreshold, val -> {
                    ConfigManager.data.rotationStopThreshold = val;
                    thresholdLabel.setText(String.format(java.util.Locale.ROOT, "Stop Threshold: %.2f", val));
                    ConfigManager.save();
                });
        listView.addItem(thresholdSlider);

        rotationCard.addChild(listView);
        rotationCard.updateLayout();
        return rotationCard;
    }

    private ResizableCard createFreecamCard(int x, int y) {
        freecamCard = screen.createResizableCard("freecam", x, y, 300, 300, "Freecam");
        int contentX = freecamCard.getContentX();
        int contentY = freecamCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 250);

        ToggleSwitch enableToggle = new ToggleSwitch(0, 0, 260,
                "Enabled",
                "Detaches camera from player",
                ConfigManager.data.freecamEnabled, value -> {
            if (value) {
                Freecam.getInstance().activate();
            } else {
                Freecam.getInstance().deactivate();
            }
            ConfigManager.data.freecamEnabled = Freecam.getInstance().isActive();
            ConfigManager.save();
        });
        listView.addItem(enableToggle);

        KeybindButton keybindButton = new KeybindButton(0, 0, 260,
                "Keybind",
                ConfigManager.data.freecamKeyCode,
                keyCode -> {
                    ConfigManager.data.freecamKeyCode = keyCode;
                    ConfigManager.save();
                });
        listView.addItem(keybindButton);

        Dropdown activationModeDropdown = new Dropdown(0, 0, 260,
                "Activation Mode",
                List.of("Toggle On/Off", "Hold Key"),
                mode -> {
                    ConfigManager.data.freecamHoldMode = mode.equals("Hold Key");
                    ConfigManager.save();
                });
        activationModeDropdown.setSelectedIndex(ConfigManager.data.freecamHoldMode ? 1 : 0);
        listView.addItem(activationModeDropdown);

        Label speedLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Speed: %.1f", ConfigManager.data.freecamSpeed),
                Label.Style.BODY);
        listView.addItem(speedLabel);

        Slider speedSlider = new Slider(0, 0, 260, 0.1f, 10.0f,
                ConfigManager.data.freecamSpeed, val -> {
            ConfigManager.data.freecamSpeed = val;
            speedLabel.setText(String.format(java.util.Locale.ROOT, "Speed: %.1f", val));
            ConfigManager.save();
        });
        listView.addItem(speedSlider);

        ToggleSwitch showHandsToggle = new ToggleSwitch(0, 0, 260,
                "Show Hands",
                "Shows player hands in freecam",
                ConfigManager.data.freecamShowHands, value -> {
            ConfigManager.data.freecamShowHands = value;
            ConfigManager.save();
        });
        listView.addItem(showHandsToggle);

        freecamCard.addChild(listView);
        freecamCard.updateLayout();
        return freecamCard;
    }
    private static String delayLabel(int ticks) {
        String base = "Action Delay: " + ticks + (ticks == 1 ? " tick" : " ticks");
        return ticks <= 1 ? base + " (can be broken)" : base;
    }

    private static String startDelayLabel(int ticks) {
        double seconds = ticks * 0.05;
        String base = String.format(java.util.Locale.US, "Auto Start Delay: %d ticks (%.3f seconds)", ticks, seconds);
        if (ticks <= 2) {
            base += " §c(not safe)";
        }
        return base;
    }

}
