package org.blackum.blackaddons.gui.screen.tabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.blackum.blackaddons.gui.screen.ProfileViewerScreen;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.core.model.Teammate;
import org.blackum.blackaddons.core.util.JsonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.blackum.blackaddons.gui.render.Theme;

public class TeammatesTabController extends ProfileTabController {

    private List<Teammate> allTeammates = new ArrayList<>();
    private String filterClass = "All";
    private String filterTime = "Any";

    private SortColumn currentSort = SortColumn.RUNS;
    private boolean sortAsc = false;

    private TextField searchField;
    private ListView teammatesList;
    private String lastSearchText = "";

    private static final int COL_IGN = 90;
    private static final int COL_RUNS = 40;
    private static final int COL_CLASS = 80;
    private static final int COL_FLOOR = 45;

    private enum SortColumn {
        IGN, RUNS, CLASS, FLOOR, LAST_SEEN
    }

    public TeammatesTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int controlsHeight = 20;

        int startY = tab.getParent().getContentY();
        int contentWidth = tab.getParent().getContentWidth();

        searchField = new TextField(tab.getParent().getContentX(), startY, 120, controlsHeight, "Search IGN...");
        searchField.setCharFilter(c -> Character.isLetterOrDigit(c) || c == '_');
        tab.addWidget(searchField);

        int btnY = startY;
        int btnH = controlsHeight;
        int btnW = 90;
        int btnGap = 5;

        int currentBtnX = tab.getParent().getContentX() + 120 + btnGap;

        Button classBtn = new Button(currentBtnX, btnY, btnW, btnH, "Class: All", () -> {
            cycleClassFilter();
            updateTeammatesList();
        });
        tab.addWidget(classBtn);
        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                classBtn.setText("Class: " + filterClass);
            }

            @Override
            public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float p) {
            }
        });
        currentBtnX += btnW + btnGap;

        Button timeBtn = new Button(currentBtnX, btnY, btnW, btnH, "Time: Any", () -> {
            cycleTimeFilter();
            updateTeammatesList();
        });
        tab.addWidget(timeBtn);
        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                timeBtn.setText("Time: " + filterTime);
            }

            @Override
            public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float p) {
            }
        });

        int headerY = startY + controlsHeight + 10;
        int headerX = tab.getParent().getContentX();

        tab.addWidget(new Widget(headerX, headerY, contentWidth - 20, 15) {
            @Override
            public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                int x = this.x + 2;
                drawHeader(graphics, "IGN", x, COL_IGN, SortColumn.IGN);
                x += COL_IGN;
                drawHeader(graphics, "Runs", x, COL_RUNS, SortColumn.RUNS);
                x += COL_RUNS;
                drawHeader(graphics, "Class", x, COL_CLASS, SortColumn.CLASS);
                x += COL_CLASS;
                drawHeader(graphics, "Floor", x, COL_FLOOR, SortColumn.FLOOR);
                x += COL_FLOOR;
                drawHeader(graphics, "Last Seen", x, 100, SortColumn.LAST_SEEN);

                graphics.fill(this.x, this.y + 14, this.x + width, this.y + 15, 0x40FFFFFF);
            }

            private void drawHeader(GuiGraphicsExtractor g, String text, int x, int w, SortColumn col) {
                int color = 0xFFFFFFFF;
                if (currentSort == col) {
                    color = Theme.ACCENT;
                    String arrow = sortAsc ? " ▲" : " ▼";
                    text += arrow;
                }
                g.text(Minecraft.getInstance().font, text, x, y + 4, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, MouseButtonEvent event) {
                int button = event.button();
                if (button == 0 && isMouseOver(mouseX, mouseY)) {
                    int relX = (int) (mouseX - this.x - 2);
                    SortColumn clicked = null;

                    int currentX = 0;
                    if (relX >= currentX && relX < currentX + COL_IGN)
                        clicked = SortColumn.IGN;
                    currentX += COL_IGN;
                    if (relX >= currentX && relX < currentX + COL_RUNS)
                        clicked = SortColumn.RUNS;
                    currentX += COL_RUNS;
                    if (relX >= currentX && relX < currentX + COL_CLASS)
                        clicked = SortColumn.CLASS;
                    currentX += COL_CLASS;
                    if (relX >= currentX && relX < currentX + COL_FLOOR)
                        clicked = SortColumn.FLOOR;
                    currentX += COL_FLOOR;
                    if (relX >= currentX)
                        clicked = SortColumn.LAST_SEEN;

                    if (clicked != null) {
                        if (currentSort == clicked) {
                            sortAsc = !sortAsc;
                        } else {
                            currentSort = clicked;
                            if (clicked == SortColumn.IGN || clicked == SortColumn.CLASS
                                    || clicked == SortColumn.FLOOR) {
                                sortAsc = true;
                            } else {
                                sortAsc = false;
                            }
                        }
                        Minecraft.getInstance().getSoundManager()
                                .play(SimpleSoundInstance
                                        .forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        updateTeammatesList();
                        return true;
                    }
                }
                return false;
            }
        });

        teammatesList = new ListView(tab.getParent().getContentX(), headerY + 15,
                contentWidth - 20, tab.getParent().getContentHeight() - controlsHeight - 10 - 15);
        teammatesList.setItemSpacing(0);
        tab.addWidget(teammatesList);

        tab.addWidget(new Widget(0, 0, 0, 0) {
            @Override
            public void tick() {
                if (searchField != null) {
                    String currentText = searchField.getText();
                    if (!currentText.equals(lastSearchText)) {
                        lastSearchText = currentText;
                        updateTeammatesList();
                    }
                }
            }

            @Override
            public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
            }
        });

        if (profileData.has("teammates")) {
            JsonArray tmArray = profileData.getAsJsonArray("teammates");
            allTeammates.clear();
            for (JsonElement tmElem : tmArray) {
                if (tmElem.isJsonArray()) {
                    JsonArray tuple = tmElem.getAsJsonArray();
                    if (tuple.size() >= 2) {
                        String ign = tuple.get(0).getAsString();
                        JsonObject data = JsonUtils.getObject(tuple, 1);
                        allTeammates.add(new Teammate(ign, data));
                    }
                } else if (tmElem.isJsonObject()) {
                    JsonObject obj = tmElem.getAsJsonObject();
                    if (obj.has("ign")) {
                        String ign = obj.get("ign").getAsString();
                        allTeammates.add(new Teammate(ign, obj));
                    }
                }
            }
            updateTeammatesList();
        } else {
            addInfoRow(teammatesList, "No teammate data available.", "");
        }
    }

    private void cycleClassFilter() {
        switch (filterClass) {
            case "All" -> filterClass = "Archer";
            case "Archer" -> filterClass = "Berserk";
            case "Berserk" -> filterClass = "Healer";
            case "Healer" -> filterClass = "Mage";
            case "Mage" -> filterClass = "Tank";
            default -> filterClass = "All";
        }
    }

    private void cycleTimeFilter() {
        switch (filterTime) {
            case "Any" -> filterTime = "24h";
            case "24h" -> filterTime = "7d";
            case "7d" -> filterTime = "30d";
            default -> filterTime = "Any";
        }
    }

    private void updateTeammatesList() {
        if (teammatesList == null)
            return;
        teammatesList.clearItems();

        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        long now = System.currentTimeMillis() / 1000;
        long timeThreshold = 0;

        if (filterTime.equals("24h"))
            timeThreshold = now - 86400;
        else if (filterTime.equals("7d"))
            timeThreshold = now - 604800;
        else if (filterTime.equals("30d"))
            timeThreshold = now - 2592000;

        List<Teammate> filtered = new ArrayList<>();
        for (Teammate tm : allTeammates) {
            if (!search.isEmpty() && !tm.ign.toLowerCase().contains(search))
                continue;

            if (!filterClass.equals("All") && !filterClass.equalsIgnoreCase(tm.lastClass))
                continue;

            if (timeThreshold > 0 && tm.lastTs < timeThreshold)
                continue;

            filtered.add(tm);
        }

        Comparator<Teammate> comparator;
        switch (currentSort) {
            case IGN -> comparator = Comparator.comparing(t -> t.ign.toLowerCase());
            case CLASS -> comparator = Comparator.comparing(t -> t.lastClass);
            case FLOOR -> comparator = Comparator.comparing(t -> t.lastFloor);
            case LAST_SEEN -> comparator = Comparator.comparingLong(t -> t.lastTs);
            case RUNS -> comparator = Comparator.comparingInt(t -> t.count);
            default -> comparator = Comparator.comparingInt(t -> t.count);
        }

        if (!sortAsc) {
            comparator = comparator.reversed();
        }

        filtered.sort(comparator);

        if (filtered.isEmpty()) {
            teammatesList.addItem(new Widget(0, 0, 0, 5) {
                @Override
                public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float p) {
                }
            });
            addInfoRow(teammatesList, "No teammates found.", "");
        } else {
            for (Teammate tm : filtered) {
                teammatesList.addItem(new TeammateRow(teammatesList.getWidth(), tm));
            }
        }
    }

}
