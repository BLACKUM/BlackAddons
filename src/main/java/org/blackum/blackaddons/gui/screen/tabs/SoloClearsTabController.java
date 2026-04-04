package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.core.config.ConfigManager.SoloClearInfo;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.*;
import net.minecraft.ChatFormatting;

import java.util.List;

public class SoloClearsTabController extends SimpleTabController {
    private String selectedFloor = "F7";
    private ListView clearsList;

    public SoloClearsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    private Label bestTimeLabel;
    private Label ao5Label;

    @Override
    public void init(TabPanel.Tab tab) {
        int contentX = tab.getParent().getContentX() + 20;
        int contentY = tab.getParent().getContentY() + 20;
        int contentWidth = tab.getParent().getContentWidth() - 40;

        Dropdown floorDropdown = new Dropdown(contentX, contentY, 100, "Floor", List.of("F7", "M7"), value -> {
            selectedFloor = value;
            rebuildList();
        });
        floorDropdown.setSelectedOption(selectedFloor);
        tab.addWidget(floorDropdown);

        Button clearButton = new Button(contentX + 110, contentY, 100, 20, "Clear Data", () -> {
            if ("F7".equals(selectedFloor)) {
                ConfigManager.data.f7SoloClears.clear();
            } else {
                ConfigManager.data.m7SoloClears.clear();
            }
            ConfigManager.save();
            rebuildList();
        });
        tab.addWidget(clearButton);

        bestTimeLabel = new Label(contentX, contentY + 40, "Best Time: None", Label.Style.BODY);
        tab.addWidget(bestTimeLabel);

        ao5Label = new Label(contentX + 150, contentY + 40, "Ao5: None", Label.Style.BODY);
        tab.addWidget(ao5Label);

        clearsList = new ListView(contentX, contentY + 60, contentWidth, 380);
        tab.addWidget(clearsList);

        rebuildList();
    }

    private int parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty() || timeStr.equals("Unknown")) return Integer.MAX_VALUE;
        try {
            if (timeStr.contains("m") || timeStr.contains("s")) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?:(\\d+)m)?\\s*(?:(\\d+)s)?");
                java.util.regex.Matcher m = p.matcher(timeStr);
                if (m.find()) {
                    int mins = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
                    int secs = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
                    int total = mins * 60 + secs;
                    return total <= 0 ? Integer.MAX_VALUE : total;
                }
            } else if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                if (parts.length == 2) {
                    int total = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                    return total <= 0 ? Integer.MAX_VALUE : total;
                }
            }
        } catch (Exception ignored) {}
        return Integer.MAX_VALUE;
    }

    private String formatSecondsToTime(int totalSeconds) {
        if (totalSeconds == Integer.MAX_VALUE) return "Unknown";
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02dm %02ds", mins, secs);
    }

    private void rebuildList() {
        if (clearsList == null) return;
        clearsList.clearItems();

        List<SoloClearInfo> clears = "M7".equals(selectedFloor) ?
                ConfigManager.data.m7SoloClears : ConfigManager.data.f7SoloClears;

        int bestSeconds = Integer.MAX_VALUE;
        int sumLast5 = 0;
        int countLast5 = 0;

        for (int i = 0; i < clears.size(); i++) {
            int secs = parseTimeToSeconds(clears.get(i).time);
            if (secs < bestSeconds) bestSeconds = secs;

            if (i >= clears.size() - 5) {
                if (secs != Integer.MAX_VALUE) {
                    sumLast5 += secs;
                    countLast5++;
                }
            }
        }

        String bestTimeStr = bestSeconds == Integer.MAX_VALUE ? "None" : formatSecondsToTime(bestSeconds);
        String ao5Str = countLast5 == 0 ? "None" : formatSecondsToTime(sumLast5 / countLast5);

        if (bestTimeLabel != null) bestTimeLabel.setText("Best Time: §a" + bestTimeStr);
        if (ao5Label != null) ao5Label.setText("Ao5: §e" + ao5Str);

        if (clears.isEmpty()) {
            clearsList.addItem(new Label(0, 0, ChatFormatting.GRAY + "No " + selectedFloor + " clears recorded yet.", Label.Style.BODY));
            return;
        }

        int startIdx = Math.max(0, clears.size() - 10);
        for (int i = clears.size() - 1; i >= startIdx; i--) {
            SoloClearInfo info = clears.get(i);
            String title = ChatFormatting.AQUA + "Run #" + (i + 1) + ChatFormatting.WHITE + 
                           " | Time: " + ChatFormatting.YELLOW + info.time + ChatFormatting.WHITE +
                           " | Secrets: " + ChatFormatting.GREEN + info.secrets + ChatFormatting.WHITE +
                           " | Prince: " + (info.princeKilled ? ChatFormatting.GREEN + "✔" : ChatFormatting.RED + "✘") + ChatFormatting.WHITE +
                           " | Mimic: " + (info.mimicKilled ? ChatFormatting.GREEN + "✔" : ChatFormatting.RED + "✘");
            
            clearsList.addItem(new Label(0, 0, title, Label.Style.BODY));

            if (!info.puzzles.isEmpty()) {
                java.util.List<String> coloredPuzzles = new java.util.ArrayList<>();
                for (String p : info.puzzles) {
                    if (p.equalsIgnoreCase("Quiz")) {
                        coloredPuzzles.add(ChatFormatting.RED + p + ChatFormatting.LIGHT_PURPLE);
                    } else {
                        coloredPuzzles.add(p);
                    }
                }
                String puzzlesJoined = String.join(", ", coloredPuzzles);
                clearsList.addItem(new Label(10, 0, ChatFormatting.GRAY + "Puzzles: " + ChatFormatting.LIGHT_PURPLE + puzzlesJoined, Label.Style.BODY));
            }

            clearsList.addItem(new Label(0, 0, ChatFormatting.DARK_GRAY + "--------------------------------------------------", Label.Style.BODY));
            clearsList.addItem(new Widget(0, 0, 0, 5) {
                @Override
                public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}
            });
        }
    }
}
