package org.blackum.blackaddons.gui.screen.tabs;

import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.screen.BlackAddonsGUI;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.gui.notification.NotificationManager;
import org.blackum.blackaddons.gui.notification.NotificationType;

import java.util.ArrayList;
import java.util.List;

public class PayloadsTabController extends SimpleTabController {
    private ResizableCard customClientCard;
    private ResizableCard allowedChannelsCard;

    public PayloadsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab payloadsTab) {
        if (!ConfigManager.data.useCardLayout) {
            return;
        }

        int contentX = payloadsTab.getParent().getContentX();
        int contentY = payloadsTab.getParent().getContentY();
        int contentWidth = payloadsTab.getParent().getContentWidth();

        Button resetLayout = new Button(contentX + Theme.PADDING, contentY, contentWidth - (Theme.PADDING * 2),
                "Reset Layout", () -> {
                    screen.resetCardStates("customClient", "allowedChannels");
                });
        payloadsTab.addWidget(resetLayout);

        CardContainer payloadsCardContainer = new CardContainer(contentX, contentY + Theme.SPACING_LARGE, contentWidth,
                570);
        payloadsTab.addWidget(payloadsCardContainer);

        int containerY = contentY + Theme.SPACING_LARGE;
        int numCols = contentWidth < 680 ? 1 : (contentWidth < 1000 ? 2 : 3);
        int colWidth = 300;
        int spacing = 20;
        int[] colY = new int[numCols];
        for (int i = 0; i < numCols; i++) colY[i] = containerY + Theme.SPACING_NORMAL;

        List<ResizableCard> cards = new ArrayList<>();
        customClientCard = createCustomClientCard(0, 0);
        cards.add(customClientCard);
        allowedChannelsCard = createPayloadChannelsCard(0, 0);
        cards.add(allowedChannelsCard);

        for (ResizableCard card : cards) {
            int shortestCol = 0;
            for (int i = 1; i < numCols; i++) {
                if (colY[i] < colY[shortestCol]) shortestCol = i;
            }

            card.setX(contentX + Theme.SPACING_NORMAL + shortestCol * (colWidth + spacing));
            card.setY(colY[shortestCol]);
            colY[shortestCol] += card.getHeight() + Theme.CARD_SPACING;
        }

        payloadsCardContainer.addCard(customClientCard);
        payloadsCardContainer.addCard(allowedChannelsCard);
    }

    private ResizableCard createCustomClientCard(int x, int y) {
        customClientCard = screen.createResizableCard("customClient", x, y, 300, 140, "Custom Client Brand");

        int contentX = customClientCard.getContentX();
        int contentY = customClientCard.getContentY();

        Label description = new Label(contentX, contentY,
                "Set custom client brand (CUSTOM mode only)", Label.Style.BODY);
        customClientCard.addChild(description);

        int inputWidth = 180;
        int btnWidth = 70;
        int yOffset = Theme.SPACING_LARGE;

        TextField customClient = new TextField(contentX, contentY + yOffset, inputWidth, "fabric");
        customClient.setText(ConfigManager.data.modHiderCustomClient == null ? "fabric"
                : ConfigManager.data.modHiderCustomClient);
        customClientCard.addChild(customClient);

        Button applyCustomClient = new Button(contentX + inputWidth + Theme.PADDING, contentY + yOffset, btnWidth,
                "Apply", () -> {
                    ConfigManager.data.modHiderCustomClient = customClient.getText().isBlank() ? "fabric"
                            : customClient.getText();
                    ConfigManager.save();
                    NotificationManager.addNotification(
                            "BlackAddons", "Saved custom client brand!",
                            NotificationType.SUCCESS);
                });
        customClientCard.addChild(applyCustomClient);

        customClientCard.updateLayout();
        return customClientCard;
    }

    private ResizableCard createPayloadChannelsCard(int x, int y) {
        allowedChannelsCard = screen.createResizableCard("allowedChannels", x, y, 300, 300,
                "Registered Channels Modifier");

        int contentX = allowedChannelsCard.getContentX();
        int contentY = allowedChannelsCard.getContentY();

        Label description = new Label(contentX, contentY,
                "One channel per line. Example: fabric:recipe_sync", Label.Style.BODY);
        allowedChannelsCard.addChild(description);

        int editorHeight = 170;
        int yOffset = Theme.SPACING_LARGE;
        CodeEditorWidget codeEditor = new CodeEditorWidget(contentX, contentY + yOffset, 260, editorHeight);
        String initialText = String.join("\n", ConfigManager.data.modHiderAllowedCustomPayloadChannels);
        codeEditor.setText(initialText);
        allowedChannelsCard.addChild(codeEditor);

        int buttonsY = contentY + yOffset + editorHeight + Theme.PADDING;
        int btnWidth = 125;
        Button saveBtn = new Button(contentX, buttonsY, btnWidth, "Save", () -> {
            String text = codeEditor.getText();
            ConfigManager.data.modHiderAllowedCustomPayloadChannels.clear();
            if (text != null && !text.isBlank()) {
                String[] lines = text.split("\n", -1);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        ConfigManager.data.modHiderAllowedCustomPayloadChannels.add(line.trim());
                    }
                }
            }
            ConfigManager.save();
            NotificationManager.addNotification(
                    "BlackAddons", "Saved registered channels!",
                    NotificationType.SUCCESS);
        });
        allowedChannelsCard.addChild(saveBtn);

        Button addDefaultsBtn = new Button(contentX + btnWidth + Theme.PADDING, buttonsY, btnWidth, "+ Fabric Default",
                () -> {
                    StringBuilder sb = new StringBuilder();
                    if (!codeEditor.getText().isEmpty()) {
                        sb.append(codeEditor.getText());
                        if (!codeEditor.getText().endsWith("\n")) {
                            sb.append("\n");
                        }
                    }

                    for (String ch : ConfigManager.FABRIC_DEFAULT_CHANNELS) {
                        if (!ConfigManager.data.modHiderAllowedCustomPayloadChannels.contains(ch)) {
                            sb.append(ch).append("\n");
                        }
                    }
                    codeEditor.setText(sb.toString());
                });
        allowedChannelsCard.addChild(addDefaultsBtn);

        allowedChannelsCard.updateLayout();
        return allowedChannelsCard;
    }
}
