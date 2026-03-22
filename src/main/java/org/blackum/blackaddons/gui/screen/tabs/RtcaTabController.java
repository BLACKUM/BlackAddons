package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.core.util.JsonUtils;
import org.blackum.blackaddons.core.util.FormatUtils;
import org.blackum.blackaddons.core.util.CatacombsUtils;
import org.blackum.blackaddons.core.util.DungeonUtils;
import org.blackum.blackaddons.integration.BotIntegration;
import org.blackum.blackaddons.integration.LocalRtcaService;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RtcaTabController extends ProfileTabController {
    private static final RtcaViewState LAST_VIEW_STATE = new RtcaViewState();

    private ListView simResultsList;
    private Button simulateBtn;
    private Dropdown rtcaFloorDropdown;
    private TextField desiredLvlField;

    private boolean simRing = true;
    private int simHecatombLvl = 10;
    private int simScarfAccIndex = 3;
    private int simScarfAttrLvl = 10;
    private int simGlobalIndex = 0;
    private int simMayorIndex = 0;
    private String rtcaFloor = "M7";

    private static final List<String> FLOOR_OPTIONS = List.of(
            "M7", "M6", "M5", "M4", "M3", "M2", "M1",
            "F7", "F6", "F5", "F4", "F3", "F2", "F1", "Entrance");

    public RtcaTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
        restoreViewState();
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int w = tab.getParent().getContentWidth();
        int cx = tab.getParent().getContentX();
        int cy = tab.getParent().getContentY();

        Label settingsLabel = new Label(cx, cy, ChatFormatting.BOLD + "Configuration", Label.Style.TITLE);
        settingsLabel.setColor(Theme.ACCENT);
        tab.addWidget(settingsLabel);

        int currentY = cy + 25;
        currentY = initControls(tab, cx, currentY, w);

        int listHeight = tab.getParent().getMaxContentHeight() - (currentY - cy) - 5;
        simResultsList = new ListView(cx, currentY, w - 10, Math.max(100, listHeight));
        addSectionHeader(simResultsList, "Results");
        tab.addWidget(simResultsList);

        if (LAST_VIEW_STATE.hasResults) {
            runSimulation();
        }
    }

    private int initControls(TabPanel.Tab tab, int cx, int y, int w) {
        int currentY = y;
        int btnW = (w - 30) / 2;
        int btnH = 20;

        Button[] ringBtnRef = new Button[1];
        ringBtnRef[0] = new Button(cx, currentY, btnW, btnH, getRingLabel(), () -> {
            simRing = !simRing;
            rememberViewState();
            if (ringBtnRef[0] != null)
                ringBtnRef[0].setText(getRingLabel());
        });

        Button[] hecaBtnRef = new Button[1];
        hecaBtnRef[0] = new Button(cx + btnW + 10, currentY, btnW, btnH, getHecatombLabel(), () -> {
            simHecatombLvl = (simHecatombLvl + 1) % 11;
            rememberViewState();
            if (hecaBtnRef[0] != null)
                hecaBtnRef[0].setText(getHecatombLabel());
        });
        tab.addWidget(ringBtnRef[0]);
        tab.addWidget(hecaBtnRef[0].setOnRightClick(() -> {
            simHecatombLvl = (simHecatombLvl - 1 + 11) % 11;
            rememberViewState();
            if (hecaBtnRef[0] != null)
                hecaBtnRef[0].setText(getHecatombLabel());
        }));
        currentY += btnH + 5;

        Button[] scarfAccBtnRef = new Button[1];
        scarfAccBtnRef[0] = new Button(cx, currentY, btnW, btnH, getScarfAccLabel(), () -> {
            simScarfAccIndex = (simScarfAccIndex + 1) % 4;
            rememberViewState();
            if (scarfAccBtnRef[0] != null)
                scarfAccBtnRef[0].setText(getScarfAccLabel());
        });

        Button[] scarfAttrBtnRef = new Button[1];
        scarfAttrBtnRef[0] = new Button(cx + btnW + 10, currentY, btnW, btnH, getScarfAttrLabel(), () -> {
            simScarfAttrLvl = (simScarfAttrLvl + 1) % 11;
            rememberViewState();
            if (scarfAttrBtnRef[0] != null)
                scarfAttrBtnRef[0].setText(getScarfAttrLabel());
        });
        tab.addWidget(scarfAccBtnRef[0].setOnRightClick(() -> {
            simScarfAccIndex = (simScarfAccIndex - 1 + 4) % 4;
            rememberViewState();
            if (scarfAccBtnRef[0] != null)
                scarfAccBtnRef[0].setText(getScarfAccLabel());
        }));
        tab.addWidget(scarfAttrBtnRef[0].setOnRightClick(() -> {
            simScarfAttrLvl = (simScarfAttrLvl - 1 + 11) % 11;
            rememberViewState();
            if (scarfAttrBtnRef[0] != null)
                scarfAttrBtnRef[0].setText(getScarfAttrLabel());
        }));
        currentY += btnH + 5;

        Button[] globalBtnRef = new Button[1];
        globalBtnRef[0] = new Button(cx, currentY, btnW, btnH, getGlobalLabel(), () -> {
            simGlobalIndex = (simGlobalIndex + 1) % 6;
            rememberViewState();
            if (globalBtnRef[0] != null)
                globalBtnRef[0].setText(getGlobalLabel());
        });

        Button[] mayorBtnRef = new Button[1];
        mayorBtnRef[0] = new Button(cx + btnW + 10, currentY, btnW, btnH, getMayorLabel(), () -> {
            simMayorIndex = (simMayorIndex + 1) % 3;
            rememberViewState();
            if (mayorBtnRef[0] != null)
                mayorBtnRef[0].setText(getMayorLabel());
        });
        tab.addWidget(globalBtnRef[0].setOnRightClick(() -> {
            simGlobalIndex = (simGlobalIndex - 1 + 6) % 6;
            rememberViewState();
            if (globalBtnRef[0] != null)
                globalBtnRef[0].setText(getGlobalLabel());
        }));
        tab.addWidget(mayorBtnRef[0].setOnRightClick(() -> {
            simMayorIndex = (simMayorIndex - 1 + 3) % 3;
            rememberViewState();
            if (mayorBtnRef[0] != null)
                mayorBtnRef[0].setText(getMayorLabel());
        }));
        currentY += btnH + 10;

        rtcaFloorDropdown = new Dropdown(cx, currentY, (w - 30) / 2, 20, "Floor: M7", FLOOR_OPTIONS, (val) -> {
            rtcaFloor = val;
            rememberViewState();
            updateSimulateButtonText();
        });
        rtcaFloorDropdown.setSelectedOption(rtcaFloor);
        tab.addWidget(rtcaFloorDropdown);

        desiredLvlField = new TextField(cx + (w - 30) / 2 + 10, currentY, (w - 30) / 2, 20, "Desired Lvl (50)");
        desiredLvlField.setText(LAST_VIEW_STATE.desiredLevelText);
        desiredLvlField.setOnValueChange(value -> {
            LAST_VIEW_STATE.desiredLevelText = value == null || value.isBlank() ? "50" : value;
        });
        tab.addWidget(desiredLvlField);

        currentY += 25;

        simulateBtn = new Button(cx, currentY, w - 20, 20, "Simulate & Calculate (" + rtcaFloor + ")",
                this::runSimulation);
        tab.addWidget(simulateBtn);

        return currentY + 25;
    }

    private void updateSimulateButtonText() {
        if (simulateBtn != null) {
            simulateBtn.setText("Simulate & Calculate (" + rtcaFloor + ")");
        }
    }

    private String getRingLabel() {
        return "Expert Ring: " + (simRing ? "Yes (+10%)" : "No");
    }

    private String getHecatombLabel() {
        if (simHecatombLvl == 0)
            return "Hecatomb: None";
        double bonus = 0.4 + (simHecatombLvl * 0.16);
        return "Hecatomb: " + intToRoman(simHecatombLvl) + String.format(" (+%.2f%%)", bonus);
    }

    private String getScarfAccLabel() {
        double[] bonus = { 0, 2, 4, 6 };
        String name = switch (simScarfAccIndex) {
            case 1 -> "Studies";
            case 2 -> "Thesis";
            case 3 -> "Grimoire";
            default -> "None";
        };
        String extra = simScarfAccIndex == 0 ? "" : " (+" + (int) bonus[simScarfAccIndex] + "%)";
        return "Scarf's Talisman: " + name + extra;
    }

    private String getScarfAttrLabel() {
        if (simScarfAttrLvl == 0)
            return "Scarf's Attribute: None";
        return "Scarf's Attribute: " + intToRoman(simScarfAttrLvl) + " (+" + (simScarfAttrLvl * 2) + "%)";
    }

    private String getGlobalLabel() {
        double[] vals = { 0, 0.05, 0.1, 0.15, 0.2, 0.3 };
        return "Global: " + String.format("%.0f%%", vals[simGlobalIndex] * 100);
    }

    private String getMayorLabel() {
        return switch (simMayorIndex) {
            case 1 -> "Mayor: Derpy";
            case 2 -> "Mayor: Aura";
            default -> "Mayor: None";
        };
    }

    private String intToRoman(int num) {
        String[] roman = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        if (num >= 0 && num < roman.length)
            return roman[num];
        return String.valueOf(num);
    }

    private void renderCalculatorResults() {
        if (simResultsList == null)
            return;

        try {
            String targetText = desiredLvlField.getText();
            if (targetText != null && !targetText.isEmpty()) {
                double targetLvl = Double.parseDouble(targetText);

                double cataXp = 0;
                if (profileData != null && profileData.has("catacombs")) {
                    cataXp = profileData.get("catacombs").getAsDouble();
                }
                double curLvlVal = DungeonUtils.getCataLevel(cataXp);

                double ringVal = simRing ? 0.1 : 0.0;
                double calcHeca = simHecatombLvl > 0 ? 0.004 + (simHecatombLvl * 0.0016) : 0;
                double[] globalVals = { 1.0, 1.05, 1.1, 1.15, 1.2, 1.3 };
                double calcGlobal = globalVals[simGlobalIndex];
                double[] mayorVals = { 1.0, 1.5, 1.55 };
                double calcMayor = mayorVals[simMayorIndex];

                double xpPerRun = CatacombsUtils.calculateDungeonXpPerRun(rtcaFloor, ringVal, calcHeca, calcGlobal,
                        calcMayor);

                addSectionHeader(simResultsList, "Goal Progress (Level " + (int) targetLvl + ")");
                addInfoRow(simResultsList, "Current Level:", String.format("%.2f", curLvlVal));
                addInfoRow(simResultsList, "Est. XP/Run:", String.format("%,.0f", xpPerRun));

                double targetXp = CatacombsUtils.getTotalXpForLevel(targetLvl);
                if (cataXp >= targetXp) {
                    addInfoRow(simResultsList, "Runs Needed:", "0 (Reached!)");
                } else {
                    double remaining = targetXp - cataXp;
                    if (xpPerRun > 0) {
                        long runs = (long) Math.ceil(remaining / xpPerRun);
                        addInfoRow(simResultsList, "Runs Needed:", String.format("%,d", runs));
                        addInfoRow(simResultsList, "Remaining XP:", FormatUtils.formatNumber(remaining));
                    } else {
                        addInfoRow(simResultsList, "Runs Needed:", "? (XP=0)");
                    }
                }

                simResultsList.addItem(new Widget(0, 0, 0, 5) {
                    @Override
                    public void render(GuiGraphics g, int x, int y, float p) {
                    }
                });
            }
        } catch (Exception e) {
            addInfoRow(simResultsList, "Calc Error:", "Invalid Input");
        }
    }

    private void runSimulation() {
        if (simResultsList != null)
            simResultsList.clearItems();
        rememberViewState();
        LAST_VIEW_STATE.hasResults = true;

        String playerName = screen.getPlayer();
        if (playerName == null || playerName.isEmpty()) {
            if (simResultsList != null)
                addInfoRow(simResultsList, "Error:", "No player selected.");
            return;
        }

        renderCalculatorResults();

        Map<String, Double> bonuses = new HashMap<>();
        bonuses.put("ring", simRing ? 0.1 : 0.0);
        double hecaVal = 0.0;
        if (simHecatombLvl > 0)
            hecaVal = 0.004 + (simHecatombLvl * 0.0016);
        bonuses.put("hecatomb", hecaVal);
        double[] scarfAccVals = { 0.0, 0.02, 0.04, 0.06 };
        bonuses.put("scarf_accessory", scarfAccVals[simScarfAccIndex]);
        bonuses.put("scarf_attribute", simScarfAttrLvl * 0.02);
        double[] globalVals = { 1.0, 1.05, 1.1, 1.15, 1.2, 1.3 };
        bonuses.put("global", globalVals[simGlobalIndex]);
        double[] mayorVals = { 1.0, 1.5, 1.55 };
        bonuses.put("mayor", mayorVals[simMayorIndex]);

        String profileName = screen.getProfileName();

        Map<String, Double> currentClassXp = new HashMap<>();
        boolean canDoLocal = true;
        try {
            JsonObject classesObj = JsonUtils.getObject(profileData, "classes");
            for (String cls : classesObj.keySet()) {
                currentClassXp.put(cls, classesObj.get(cls).getAsDouble());
            }
        } catch (Exception e) {
            canDoLocal = false;
        }

        if (canDoLocal) {
            LocalRtcaService.simulate(rtcaFloor, currentClassXp, bonuses)
                    .thenAccept(json -> {
                        Minecraft.getInstance().execute(() -> {
                            if (simResultsList == null)
                                return;

                            simResultsList.clearItems();
                            renderCalculatorResults();

                            processRtcaResults(json);
                        });
                    });
        } else {
            BotIntegration.getRtcaStats(playerName, profileName, rtcaFloor, bonuses).thenAccept(json -> {
                Minecraft.getInstance().execute(() -> {
                    if (simResultsList == null)
                        return;

                    simResultsList.clearItems();
                    renderCalculatorResults();

                    if (json == null) {
                        addInfoRow(simResultsList, "Error:", "API Unavailable or Failed.");
                        return;
                    }

                    if (json.has("error")) {
                        addInfoRow(simResultsList, "Error:", JsonUtils.getString(json, "error", "Unknown error"));
                        return;
                    }

                    processRtcaResults(json);
                });
            });
        }
    }

    private void processRtcaResults(JsonObject json) {
        try {
            int totalRuns = JsonUtils.getInt(json, "total_runs");
            if (totalRuns == 0) {
                screen.startConfetti();
                simResultsList.addItem(new Widget(0, 0, simResultsList.getWidth(), 25) {
                    @Override
                    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                        graphics.drawCenteredString(Minecraft.getInstance().font,
                                ChatFormatting.GOLD.toString() + ChatFormatting.BOLD + "🎉 Congratulations "
                                        + screen.getPlayer()
                                        + ", you already hit Class Average 50! 🎉",
                                x + width / 2, y + 8, 0xFFFFFFFF);
                    }
                });
                simResultsList.addItem(new Widget(0, 0, simResultsList.getWidth(), 30) {
                    @Override
                    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                        graphics.drawCenteredString(Minecraft.getInstance().font,
                                ChatFormatting.YELLOW
                                        + "You don't need this simulation anymore. Go touch some grass! 🌱",
                                x + width / 2, y + 5, 0xFFFFD700);
                    }
                });
                return;
            }

            simResultsList.addItem(new Widget(0, 0, 0, 5) {
                @Override
                public void render(GuiGraphics g, int x, int y, float p) {
                }
            });
            addSectionHeader(simResultsList, "Class Simulation");

            addInfoRow(simResultsList, "Total Runs Needed:", String.format("%,d", totalRuns));

            simResultsList.addItem(new Widget(0, 0, 0, 5) {

                @Override
                public void render(GuiGraphics g, int x, int y, float p) {
                }
            });

            JsonObject results = JsonUtils.getObject(json, "results");
            List<String> sortedClasses = new ArrayList<>(results.keySet());
            sortedClasses.sort(String::compareTo);

            Map<String, Double> xpData = new HashMap<>();

            GridRow header = new GridRow(simResultsList.getWidth() - 10, 15);
            header.addChild(new Label(0, 0, "Class", Label.Style.BODY), 5);
            header.addChild(new Label(0, 0, "Remaining Runs", Label.Style.BODY), 80);
            header.addChild(new Label(0, 0, "XP to Class Lvl 50", Label.Style.BODY), 180);
            simResultsList.addItem(header);

            for (String cls : sortedClasses) {
                JsonObject clsData = JsonUtils.getObject(results, cls);
                int runs = JsonUtils.getInt(clsData, "runs_done");
                double remaining = JsonUtils.getDouble(clsData, "remaining_xp");
                xpData.put(cls, remaining);

                GridRow row = new GridRow(simResultsList.getWidth() - 10, 15);
                String name = cls.substring(0, 1).toUpperCase() + cls.substring(1);
                Label nameLabel = new Label(0, 0, name, Label.Style.BODY);
                nameLabel.setColor(Theme.ACCENT);
                row.addChild(nameLabel, 5);
                row.addChild(new Label(0, 0, String.format("%,d", runs), Label.Style.BODY), 80);
                String xpText = FormatUtils.formatNumber(remaining);
                row.addChild(new Label(0, 0, xpText, Label.Style.BODY), 180);
                simResultsList.addItem(row);
            }

            simResultsList.addItem(new Widget(0, 0, 0, 5) {
                @Override
                public void render(GuiGraphics g, int x, int y, float p) {
                }
            });

            BarGraphWidget xpGraph = new BarGraphWidget(0, 0, simResultsList.getWidth() - 10,
                    "Remaining XP per Class");
            xpGraph.setData(xpData, "XP");

            Map<String, Integer> classColors = new HashMap<>();
            classColors.put("archer", 0xFFFFAA00);
            classColors.put("berserk", 0xFFFF5555);
            classColors.put("healer", 0xFFFF55FF);
            classColors.put("mage", 0xFF55FFFF);
            classColors.put("tank", 0xFF00AA00);
            xpGraph.setColorMap(classColors);

            simResultsList.addItem(xpGraph);

        } catch (Exception e) {
            addInfoRow(simResultsList, "Error:", "Failed to parse results.");
            e.printStackTrace();
        }
    }

    private void restoreViewState() {
        simRing = LAST_VIEW_STATE.simRing;
        simHecatombLvl = LAST_VIEW_STATE.simHecatombLvl;
        simScarfAccIndex = LAST_VIEW_STATE.simScarfAccIndex;
        simScarfAttrLvl = LAST_VIEW_STATE.simScarfAttrLvl;
        simGlobalIndex = LAST_VIEW_STATE.simGlobalIndex;
        simMayorIndex = LAST_VIEW_STATE.simMayorIndex;
        rtcaFloor = LAST_VIEW_STATE.rtcaFloor;
    }

    private void rememberViewState() {
        LAST_VIEW_STATE.simRing = simRing;
        LAST_VIEW_STATE.simHecatombLvl = simHecatombLvl;
        LAST_VIEW_STATE.simScarfAccIndex = simScarfAccIndex;
        LAST_VIEW_STATE.simScarfAttrLvl = simScarfAttrLvl;
        LAST_VIEW_STATE.simGlobalIndex = simGlobalIndex;
        LAST_VIEW_STATE.simMayorIndex = simMayorIndex;
        LAST_VIEW_STATE.rtcaFloor = rtcaFloor;
        if (desiredLvlField != null) {
            String desiredLevel = desiredLvlField.getText();
            LAST_VIEW_STATE.desiredLevelText = desiredLevel == null || desiredLevel.isBlank() ? "50" : desiredLevel;
        }
    }

    private static final class RtcaViewState {
        private boolean simRing = true;
        private int simHecatombLvl = 10;
        private int simScarfAccIndex = 3;
        private int simScarfAttrLvl = 10;
        private int simGlobalIndex = 0;
        private int simMayorIndex = 0;
        private String rtcaFloor = "M7";
        private String desiredLevelText = "50";
        private boolean hasResults = false;
    }

}
