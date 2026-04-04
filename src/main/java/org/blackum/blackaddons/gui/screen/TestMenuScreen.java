package org.blackum.blackaddons.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;

import java.util.ArrayList;
import java.util.List;

public class TestMenuScreen extends BaseScreen {

        private TabPanel tabPanel;
        private List<Card> sampleCards = new ArrayList<>();
        private SearchField searchField;
        private ListView listView;

        public TestMenuScreen() {
                super(Component.literal("Test Menu"));
                this.showClickDebug = true;
        }

        private void sendMessage(String message) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                        Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal(
                                        ChatFormatting.AQUA + "[Test] " + ChatFormatting.WHITE + message));
                }
        }

        @Override
        public void initWidgets() {
                int contentX = containerX + 30;
                int contentY = containerY + 60;
                int contentWidth = containerWidth - 60;
                int contentHeight = containerHeight - 120;

                tabPanel = new TabPanel(contentX, contentY, contentWidth, contentHeight);
                tabPanel.setOnTabChange(index -> sendMessage("Switched to tab: " + tabPanel.getTab(index).name));

                initSwitchesTab();
                initSearchListTab();
                initCardsTab();

                addWidget(tabPanel);
        }

        private void initSwitchesTab() {
                TabPanel.Tab switchesTab = tabPanel.addTab("Switches");

                int contentX = tabPanel.getContentX();
                int contentY = tabPanel.getContentY();
                int contentWidth = tabPanel.getContentWidth();
                int contentHeight = tabPanel.getContentHeight();

                switchesTab.addWidget(new Label(contentX, contentY, "Toggle Switches", Label.Style.TITLE));

                ListView switchList = new ListView(contentX, contentY + 30, contentWidth - Theme.GRID_MARGIN,
                                contentHeight - 40);

                for (int i = 1; i <= 5; i++) {
                        String label = "Feature " + i;
                        String desc = "This is a description for feature " + i
                                        + ". It can be expanded by clicking the arrow.";

                        ToggleSwitch toggle = new ToggleSwitch(0, 0, contentWidth - Theme.GRID_MARGIN,
                                        label, desc, i % 2 == 0,
                                        value -> sendMessage(label + ": " + (value ? "ON" : "OFF")));

                        switchList.addItem(toggle);
                }

                switchesTab.addWidget(switchList);
        }

        private void initSearchListTab() {
                TabPanel.Tab searchTab = tabPanel.addTab("Search & List");
                sampleCards.clear();

                int contentX = tabPanel.getContentX();
                int contentY = tabPanel.getContentY();
                int contentWidth = tabPanel.getContentWidth();

                searchTab.addWidget(new Label(contentX, contentY, "Searchable List", Label.Style.TITLE));

                searchField = new SearchField(contentX, contentY + 30, contentWidth - Theme.GRID_MARGIN,
                                this::onSearch);
                searchTab.addWidget(searchField);

                listView = new ListView(contentX, contentY + 70, contentWidth - Theme.GRID_MARGIN,
                                tabPanel.getContentHeight() - 80);

                for (int i = 1; i <= 20; i++) {
                        Card card = createSampleCard(i);
                        sampleCards.add(card);
                        listView.addItem(card);
                }

                searchTab.addWidget(listView);
        }

        private Card createSampleCard(int index) {
                Card card = new Card(0, 0, tabPanel.getContentWidth() - 20, 60, "Item " + index);

                int contentX = card.getContentX();
                int contentY = card.getContentY();

                Label desc = new Label(contentX, contentY, "Sample description for item " + index,
                                Label.Style.BODY);
                card.addChild(desc);

                ToggleSwitch toggle = new ToggleSwitch(contentX, contentY + 15,
                                card.getContentWidth(), "Enabled", false,
                                value -> sendMessage("Item " + index + ": " + value));
                card.addChild(toggle);

                return card;
        }

        private void onSearch(String query) {
                sendMessage("Searching for: " + query);
                if (listView != null) {
                        listView.clearItems();
                        String lowerQuery = query.toLowerCase();
                        for (Card card : sampleCards) {
                                if (card.getTitle().toLowerCase().contains(lowerQuery)) {
                                        listView.addItem(card);
                                }
                        }
                }
        }

        private void initCardsTab() {
                TabPanel.Tab cardsTab = tabPanel.addTab("Cards");

                int contentX = tabPanel.getContentX();
                int contentY = tabPanel.getContentY();
                int contentWidth = tabPanel.getContentWidth();

                cardsTab.addWidget(new Label(contentX, contentY, "Card Examples", Label.Style.TITLE));

                final int gap = Theme.GRID_GAP;
                final int columns = Theme.GRID_COLUMNS;
                final int gridWidth = contentWidth - Theme.GRID_MARGIN;
                final int colWidth = (gridWidth - (columns - 1) * gap) / columns;

                int row1Y = contentY + 30;
                int row1Height = Theme.CARD_HEIGHT_SMALL;

                Card simpleCard = new Card(contentX, row1Y, colWidth, row1Height, "Simple Card");
                Label simpleLabel = new Label(simpleCard.getContentX(), simpleCard.getContentY(),
                                "This is a simple card", Label.Style.BODY);
                simpleCard.addChild(simpleLabel);
                cardsTab.addWidget(simpleCard);

                Card interactiveCard = new Card(contentX + colWidth + gap, row1Y, colWidth, row1Height,
                                "Interactive Card");
                Button cardButton = new Button(interactiveCard.getContentX(), interactiveCard.getContentY(),
                                interactiveCard.getContentWidth(), "Click Me",
                                () -> sendMessage("Card button clicked!"));
                interactiveCard.addChild(cardButton);
                cardsTab.addWidget(interactiveCard);

                int row2Y = row1Y + row1Height + gap;
                Card complexCard = new Card(contentX, row2Y, gridWidth, Theme.CARD_HEIGHT_MEDIUM, "Complex Card");

                Label complexDesc = new Label(complexCard.getContentX(), complexCard.getContentY(),
                                "A card with multiple widgets", Label.Style.BODY);
                complexCard.addChild(complexDesc);

                Slider slider = new Slider(complexCard.getContentX(), complexCard.getContentY() + 20,
                                complexCard.getContentWidth(), 16, 0f, 100f, 50f,
                                value -> sendMessage("Slider: " + value));
                complexCard.addChild(slider);

                Checkbox checkbox = new Checkbox(complexCard.getContentX(), complexCard.getContentY() + 50,
                                "Enable Feature", false,
                                checked -> sendMessage("Checkbox: " + checked));
                complexCard.addChild(checkbox);

                TextField textField = new TextField(complexCard.getContentX(), complexCard.getContentY() + 85,
                                complexCard.getContentWidth(), "Type here...");
                complexCard.addChild(textField);

                cardsTab.addWidget(complexCard);
        }

        @Override
        protected void extractScrolledContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                String titleText = "Widget Test Menu";
                int titleWidth = this.font.width(titleText);
                graphics.text(this.font, titleText, containerX + (containerWidth - titleWidth) / 2,
                                containerY + 20, -1);

                graphics.text(this.font, "Test all widgets:", containerX + 30, containerY + 40,
                                Theme.TEXT_SECONDARY);
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }
}
