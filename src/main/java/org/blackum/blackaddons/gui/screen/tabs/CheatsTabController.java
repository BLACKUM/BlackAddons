package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.gui.screen.AutoSSOverlayPositionScreen;
import org.blackum.blackaddons.gui.screen.RotationOverlayPositionScreen;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.*;

import java.util.List;

public class CheatsTabController extends SimpleTabController {
    private ResizableCard autoTntCard;
    private ResizableCard autoSSCard;
    private ResizableCard fastLeapCard;
    private ResizableCard debugCard;
    private Dropdown s1Dropdown;
    private Dropdown s2Dropdown;
    private Dropdown s3Dropdown;
    private Dropdown s4Dropdown;

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
                    "Automatically solves F7 devices",
                    ConfigManager.data.AutoSSEnabled, value -> {
                        ConfigManager.data.AutoSSEnabled = value;
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(ssEnableToggle);

            Label ssTickLabel = new Label(contentX, contentY + 220,
                    "Action Delay: " + ConfigManager.data.AutoSSDelay + " ticks", Label.Style.BODY);
            cheatsTab.addWidget(ssTickLabel);

            Slider ssTickSlider = new Slider(contentX, contentY + 240, contentWidth - 20, 0, 20,
                    ConfigManager.data.AutoSSDelay, val -> {
                        int ticks = Math.round(val);
                        if (ticks != ConfigManager.data.AutoSSDelay) {
                            ConfigManager.data.AutoSSDelay = ticks;
                            ssTickLabel.setText("Action Delay: " + ticks + " ticks");
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

            return;
        }

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("autoTnt", "autoSS", "fastLeap", "debug");
        });
        cheatsTab.addWidget(resetLayout);

        CardContainer cheatsCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
        cheatsTab.addWidget(cheatsCardContainer);

        int currentY = contentY + 50;
        autoTntCard = createAutoTntCard(contentX + 20, currentY);
        cheatsCardContainer.addCard(autoTntCard);

        autoSSCard = createAutoSSCard(contentX + 340, currentY);
        cheatsCardContainer.addCard(autoSSCard);

        fastLeapCard = createFastLeapCard(contentX + 20, currentY + 300);
        cheatsCardContainer.addCard(fastLeapCard);

        debugCard = createRotationCard(contentX + 340, currentY + 300);
        cheatsCardContainer.addCard(debugCard);
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

        ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable AutoSS",
                "Automatically solves F7 devices",
                ConfigManager.data.AutoSSEnabled, value -> {
            ConfigManager.data.AutoSSEnabled = value;
            ConfigManager.save();
        });
        autoSSCard.addChild(enableToggle);

        Label tickLabel = new Label(contentX, contentY + 50,
                "Action Delay: " + ConfigManager.data.AutoSSDelay + " ticks", Label.Style.BODY);
        autoSSCard.addChild(tickLabel);

        Slider tickSlider = new Slider(contentX, contentY + 70, 260, 0, 20,
                ConfigManager.data.AutoSSDelay, val -> {
            int ticks = Math.round(val);
            if (ticks != ConfigManager.data.AutoSSDelay) {
                ConfigManager.data.AutoSSDelay = ticks;
                tickLabel.setText("Action Delay: " + ticks + " ticks");
                ConfigManager.save();
            }
        });
        autoSSCard.addChild(tickSlider);

        Label distLabel = new Label(contentX, contentY + 100,
                String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", ConfigManager.data.AutoSSDistanceLimit), Label.Style.BODY);
        autoSSCard.addChild(distLabel);

        Slider distSlider = new Slider(contentX, contentY + 120, 260, 2.0f, 10.0f,
                ConfigManager.data.AutoSSDistanceLimit, val -> {
            ConfigManager.data.AutoSSDistanceLimit = val;
            distLabel.setText(String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", val));
            ConfigManager.save();
        });
        autoSSCard.addChild(distSlider);

        Label speedLabel = new Label(contentX, contentY + 150,
                String.format(java.util.Locale.ROOT, "Rotation Speed: %.1f", ConfigManager.data.AutoSSRotationSpeed), Label.Style.BODY);
        autoSSCard.addChild(speedLabel);

        Slider speedSlider = new Slider(contentX, contentY + 170, 260, 1.0f, 50.0f,
                ConfigManager.data.AutoSSRotationSpeed, val -> {
            ConfigManager.data.AutoSSRotationSpeed = val;
            speedLabel.setText(String.format(java.util.Locale.ROOT, "Rotation Speed: %.1f", val));
            ConfigManager.save();
        });
        autoSSCard.addChild(speedSlider);

        Label curveLabel = new Label(contentX, contentY + 200,
                String.format(java.util.Locale.ROOT, "Rotation Curve: %.2f", ConfigManager.data.AutoSSRotationCurve), Label.Style.BODY);
        autoSSCard.addChild(curveLabel);

        Slider curveSlider = new Slider(contentX, contentY + 220, 260, 0.0f, 1.0f,
                ConfigManager.data.AutoSSRotationCurve, val -> {
            ConfigManager.data.AutoSSRotationCurve = val;
            curveLabel.setText(String.format(java.util.Locale.ROOT, "Rotation Curve: %.2f", val));
            ConfigManager.save();
        });
        autoSSCard.addChild(curveSlider);

        ToggleSwitch instantToggle = new ToggleSwitch(contentX, contentY + 260, 260,
                "Instant Snap",
                "Instantly snaps to targets",
                ConfigManager.data.AutoSSInstantSnap, value -> {
            ConfigManager.data.AutoSSInstantSnap = value;
            ConfigManager.save();
        });
        autoSSCard.addChild(instantToggle);

        ToggleSwitch autoStartToggle = new ToggleSwitch(contentX, contentY + 300, 260,
                "Auto SS Start",
                "Automatically aims and clicks the start button",
                ConfigManager.data.AutoSSAutoStart, value -> {
            ConfigManager.data.AutoSSAutoStart = value;
            ConfigManager.save();
        });
        autoSSCard.addChild(autoStartToggle);

        ToggleSwitch trySkipToggle = new ToggleSwitch(contentX, contentY + 340, 260,
                "Try SS Skip",
                "Clicks start button 3 times for potential skip",
                ConfigManager.data.AutoSSTrySkip, value -> {
            ConfigManager.data.AutoSSTrySkip = value;
            ConfigManager.save();
        });
        autoSSCard.addChild(trySkipToggle);

        ToggleSwitch debugToggle = new ToggleSwitch(contentX, contentY + 380, 260,
                "Debug Mode",
                "Shows debug overlay and logs to a file",
                ConfigManager.data.AutoSSDebug, value -> {
            ConfigManager.data.AutoSSDebug = value;
            ConfigManager.save();
        });
        autoSSCard.addChild(debugToggle);

        Button moveOverlayButton = new Button(contentX, contentY + 420, 260, 20, "Set Overlay Position", () -> {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.minecraft.client.Minecraft.getInstance().setScreen(new AutoSSOverlayPositionScreen(screen));
            });
        });
        autoSSCard.addChild(moveOverlayButton);

        autoSSCard.setExpandedHeight(autoSSCard.getExpandedHeight() + 160);
        autoSSCard.updateLayout();
        return autoSSCard;
    }

    private ResizableCard createFastLeapCard(int x, int y) {
        fastLeapCard = screen.createResizableCard("fastLeap", x, y, 300, 330, "⚠ FastLeap [WIP] (DO NOT USE)");

        int contentX = fastLeapCard.getContentX();
        int contentY = fastLeapCard.getContentY();

        List<String> classOptions = List.of("NONE", "HEALER", "MAGE", "BERSERK", "ARCHER", "TANK");

        ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY, 260,
                "⚠ Enable FastLeap [WIP]",
                "Automatically leaps to players or classes (DO NOT USE)",
                ConfigManager.data.FastLeapEnabled, value -> {
                    ConfigManager.data.FastLeapEnabled = value;
                    ConfigManager.save();
                });
        fastLeapCard.addChild(enableToggle);

        ToggleSwitch doorOpenerToggle = new ToggleSwitch(contentX, contentY + 40, 260,
                "Door Opener",
                "Leap to the player who opens wither doors",
                ConfigManager.data.FastLeapDoorOpener, value -> {
                    ConfigManager.data.FastLeapDoorOpener = value;
                    ConfigManager.save();
                });
        fastLeapCard.addChild(doorOpenerToggle);

        ToggleSwitch positionalToggle = new ToggleSwitch(contentX, contentY + 80, 260,
                "Positional",
                "Leap to a class based on your S-room position",
                ConfigManager.data.FastLeapPositional, value -> {
                    ConfigManager.data.FastLeapPositional = value;
                    ConfigManager.save();
                });
        fastLeapCard.addChild(positionalToggle);

        fastLeapCard.addChild(new Label(contentX, contentY + 125, "S1 Class", Label.Style.BODY));
        s1Dropdown = new Dropdown(contentX, contentY + 137, 260, "S1 Class", classOptions, value -> {
            ConfigManager.data.FastLeapS1Class = value;
            ConfigManager.save();
        });
        s1Dropdown.setSelectedOption(ConfigManager.data.FastLeapS1Class);
        s1Dropdown.setOnExpand(() -> collapseOtherDropdowns(s1Dropdown));
        fastLeapCard.addChild(s1Dropdown);

        fastLeapCard.addChild(new Label(contentX, contentY + 175, "S2 Class", Label.Style.BODY));
        s2Dropdown = new Dropdown(contentX, contentY + 187, 260, "S2 Class", classOptions, value -> {
            ConfigManager.data.FastLeapS2Class = value;
            ConfigManager.save();
        });
        s2Dropdown.setSelectedOption(ConfigManager.data.FastLeapS2Class);
        s2Dropdown.setOnExpand(() -> collapseOtherDropdowns(s2Dropdown));
        fastLeapCard.addChild(s2Dropdown);

        fastLeapCard.addChild(new Label(contentX, contentY + 225, "S3 Class", Label.Style.BODY));
        s3Dropdown = new Dropdown(contentX, contentY + 237, 260, "S3 Class", classOptions, value -> {
            ConfigManager.data.FastLeapS3Class = value;
            ConfigManager.save();
        });
        s3Dropdown.setSelectedOption(ConfigManager.data.FastLeapS3Class);
        s3Dropdown.setOnExpand(() -> collapseOtherDropdowns(s3Dropdown));
        fastLeapCard.addChild(s3Dropdown);

        fastLeapCard.addChild(new Label(contentX, contentY + 275, "S4 Class", Label.Style.BODY));
        s4Dropdown = new Dropdown(contentX, contentY + 287, 260, "S4 Class", classOptions, value -> {
            ConfigManager.data.FastLeapS4Class = value;
            ConfigManager.save();
        });
        s4Dropdown.setSelectedOption(ConfigManager.data.FastLeapS4Class);
        s4Dropdown.setOnExpand(() -> collapseOtherDropdowns(s4Dropdown));
        fastLeapCard.addChild(s4Dropdown);

        fastLeapCard.updateLayout();
        return fastLeapCard;
    }

    private void collapseOtherDropdowns(Dropdown active) {
        if (s1Dropdown != null && s1Dropdown != active) s1Dropdown.collapse();
        if (s2Dropdown != null && s2Dropdown != active) s2Dropdown.collapse();
        if (s3Dropdown != null && s3Dropdown != active) s3Dropdown.collapse();
        if (s4Dropdown != null && s4Dropdown != active) s4Dropdown.collapse();
    }

    private ResizableCard createRotationCard(int x, int y) {
        debugCard = screen.createResizableCard("rotationSet", x, y, 300, 310, "Rotation Settings");
        int contentX = debugCard.getContentX();
        int contentY = debugCard.getContentY();

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
                String.format("Curve Strength: %.0f%%", ConfigManager.data.rotationJitter * 100),
                Label.Style.BODY);
        listView.addItem(curveLabel);
        Slider curveSlider = new Slider(0, 0, 260, 0, 200,
                ConfigManager.data.rotationJitter * 100, val -> {
                    ConfigManager.data.rotationJitter = val / 100f;
                    curveLabel.setText(String.format("Curve Strength: %.0f%%", val));
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

        debugCard.addChild(listView);
        debugCard.updateLayout();
        return debugCard;
    }


}
