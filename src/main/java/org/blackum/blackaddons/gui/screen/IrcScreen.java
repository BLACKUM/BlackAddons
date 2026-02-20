package org.blackum.blackaddons.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.blackum.blackaddons.core.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.*;
import org.blackum.blackaddons.feature.chat.IrcClient;
import org.blackum.blackaddons.core.util.MinecraftInstance;
import org.blackum.blackaddons.feature.chat.ChatImageHandler;
import org.blackum.blackaddons.core.util.Constants;
import net.minecraft.network.chat.HoverEvent;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IrcScreen extends BaseScreen implements IrcClient.IrcAuthListener {

    private ListView messageList;
    private TextField inputField;
    private EmojiWidget emojiWidget;
    private String currentChannel = "general";
    private int lastScrollOffset = -1;
    private boolean wasAtBottom = true;

    public IrcScreen() {
        super(Component.literal("IRC Chat"));
    }

    @Override
    protected void initWidgets() {
        int contentX = containerX + 20;
        int contentY = containerY + 20;
        int contentWidth = containerWidth - 40;
        int contentHeight = containerHeight - 40;

        if (messageList != null) {
            lastScrollOffset = messageList.getScrollOffset();
            wasAtBottom = messageList.isAtBottom();
        }

        widgets.add(new Label(contentX, contentY, "IRC Chat", Label.Style.TITLE));

        int tabX = contentX;
        int tabY = contentY + 18;
        List<String> channels = ConfigManager.data.ircChannels;
        for (String channel : channels) {
            if (channel.equalsIgnoreCase("admin") && !IrcClient.getInstance().isAdmin()) {
                continue;
            }
            String label = "#" + channel;
            int tabWidth = MinecraftInstance.mc.font.width(label) + 20;
            Button tabBtn = new Button(tabX, tabY, tabWidth, 20, label, () -> {
                this.currentChannel = channel;
                init();
            });
            if (channel.equals(currentChannel)) {
                tabBtn.setTextColor(Theme.ACCENT);
            }
            widgets.add(tabBtn);
            tabX += tabWidth + 5;
        }

        int listY = contentY + 45;
        int listHeight = contentHeight - 85;
        messageList = new ListView(contentX, listY, contentWidth, listHeight);
        widgets.add(messageList);

        refreshMessages();

        if (wasAtBottom) {
            messageList.setScrollOffset(messageList.getMaxScroll());
        } else if (lastScrollOffset != -1) {
            messageList.setScrollOffset(lastScrollOffset);
        }

        int inputY = listY + listHeight + 10;
        inputField = new TextField(contentX, inputY, contentWidth - 90,
                "Type a message in #" + currentChannel + "...");
        inputField.setMaxLength(256);
        widgets.add(inputField);

        emojiWidget = new EmojiWidget(contentX + contentWidth - 195, inputY, (emoji) -> {
            inputField.insertText(emoji);
            inputField.setFocused(true);
            emojiWidget.setVisible(false);
        });

        Button emojiBtn = new Button(contentX + contentWidth - 85, inputY, 20, Theme.TEXTFIELD_HEIGHT, "😊", () -> {
            emojiWidget.setVisible(!emojiWidget.isVisible());
        });
        widgets.add(emojiBtn);

        Button sendBtn = new Button(contentX + contentWidth - 60, inputY, 60, Theme.TEXTFIELD_HEIGHT, "Send",
                this::sendCurrentMessage);
        widgets.add(sendBtn);

        widgets.add(emojiWidget);

        boolean isReadOnly = currentChannel.equalsIgnoreCase("announcements");
        if (isReadOnly) {
            inputField.setEnabled(false);
            sendBtn.setEnabled(false);
            inputField.setPlaceholder("This channel is read-only.");
        }

        IrcClient.getInstance().addListener(this);
    }

    private void refreshMessages() {
        if (messageList == null)
            return;
        messageList.getItems().clear();
        List<IrcClient.IrcMessage> history = IrcClient.getInstance().getMessageBuffer();
        for (IrcClient.IrcMessage msg : history) {
            if (msg.channel().equals(currentChannel)) {
                addMessageToWidget(msg, false);
            }
        }
    }

    private void sendCurrentMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            IrcClient.getInstance().sendMessage(text, currentChannel);
            inputField.setText("");
        }
    }

    private void addMessageToWidget(IrcClient.IrcMessage msg, boolean scrollToBottom) {
        if (messageList != null) {
            if (!msg.channel().equals(currentChannel))
                return;
            messageList.addItem(new MessageWidget(0, 0, messageList.getWidth(), msg));
            if (scrollToBottom) {
                messageList.setScrollOffset(messageList.getMaxScroll());
            }
        }
    }

    @Override
    public void onMessageReceived(IrcClient.IrcMessage message) {
        MinecraftInstance.mc.execute(() -> {
            if (message == null) {
                refreshMessages();
            } else {
                boolean isAtBottom = messageList.isAtBottom();
                addMessageToWidget(message, isAtBottom);
            }
        });
    }

    @Override
    public void onAuthStatusChanged(boolean isAdmin) {
        MinecraftInstance.mc.execute(this::init);
    }

    @Override
    public void onClose() {
        IrcClient.getInstance().removeListener(this);
        super.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 257 || event.key() == 335) {
            if (inputField != null && inputField.isFocused()) {
                sendCurrentMessage();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private boolean handleComponentClick(Style style) {
        if (style == null)
            return false;
        ClickEvent event = style.getClickEvent();
        if (event == null)
            return false;

        if (event instanceof ClickEvent.OpenUrl(java.net.URI uri)) {
            @SuppressWarnings("null")
            String url = uri.toString();
            if (url != null) {
                net.minecraft.Util.getPlatform().openUri(url);
            }
            return true;
        } else if (event instanceof ClickEvent.RunCommand runCommand) {
            String command = runCommand.command();
            if (MinecraftInstance.mc.player != null && MinecraftInstance.mc.player.connection != null) {
                if (command.startsWith("/")) {
                    MinecraftInstance.mc.player.connection.sendCommand(command.substring(1));
                } else {
                    MinecraftInstance.mc.player.connection.sendChat(command);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected int getContentHeight() {
        return containerHeight - 40;
    }

    private Style getStyleAt(FormattedCharSequence line, int x) {
        Style[] found = { Style.EMPTY };
        int[] currentX = { 0 };
        line.accept((index, style, codePoint) -> {
            @SuppressWarnings("null")
            String charStr = String.valueOf((char) codePoint);
            if (charStr != null) {
                int charWidth = MinecraftInstance.mc.font.width(charStr);
                if (x >= currentX[0] && x < currentX[0] + charWidth) {
                    found[0] = style;
                    return false;
                }
                currentX[0] += charWidth;
            }
            return true;
        });
        return found[0];
    }

    private class MessageWidget extends Widget {
        private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
        private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
        private static final int[] COLORS = {
                0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF9575CD, 0xFF7986CB,
                0xFF64B5F6, 0xFF4FC3F7, 0xFF4DD0E1, 0xFF4DB6AC, 0xFF81C784,
                0xFFAED581, 0xFFFFD54F, 0xFFFFB74D, 0xFFFF8A65, 0xFFA1887F
        };

        private final List<FormattedCharSequence> lines;

        public MessageWidget(int x, int y, int width, IrcClient.IrcMessage msg) {
            super(x, y, width, 20);
            if (msg == null) {
                this.lines = new ArrayList<>();
                return;
            }

            MutableComponent fullComp = Component.literal("");

            String timeStr = "[" + TIME_FORMAT.format(new Date(msg.timestamp())) + "] ";
            fullComp.append(Component.literal(timeStr).withStyle(ChatFormatting.GRAY));

            int colorIndex = Math.abs(msg.user().hashCode()) % COLORS.length;
            fullComp.append(Component.literal(msg.user() + ": ")
                    .withStyle(Style.EMPTY.withColor(COLORS[colorIndex])));

            String text = msg.message();
            Matcher matcher = URL_PATTERN.matcher(text);
            int lastEnd = 0;
            while (matcher.find()) {
                if (matcher.start() > lastEnd) {
                    fullComp.append(Component.literal(text.substring(lastEnd, matcher.start()))
                            .withStyle(ChatFormatting.WHITE));
                }
                String url = matcher.group(1);
                fullComp.append(Component.literal(url)
                        .withStyle(style -> style.withColor(Theme.ACCENT)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))));

                if (ChatImageHandler.DISCORD_IMAGE_PATTERN.matcher(url).find()) {
                    fullComp.append(Component.literal(Constants.PREVIEW_LABEL)
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent.RunCommand("/ba preview " + url))
                                    .withHoverEvent(new HoverEvent.ShowText(
                                            Component.literal(Constants.PREVIEW_HOVER)))));
                }

                lastEnd = matcher.end();
            }
            if (lastEnd < text.length()) {
                fullComp.append(Component.literal(text.substring(lastEnd)).withStyle(ChatFormatting.WHITE));
            }

            this.lines = MinecraftInstance.mc.font.split(fullComp, width - 20);
            this.height = lines.size() * (MinecraftInstance.mc.font.lineHeight + 2) + 12;
            this.height = Math.max(20, lines.size() * (MinecraftInstance.mc.font.lineHeight + 2) + 8);
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float p) {
            int currentY = y + 6;
            for (FormattedCharSequence line : lines) {
                if (line == null)
                    continue;
                g.drawString(MinecraftInstance.mc.font, line, x + 10, currentY, Theme.TEXT_PRIMARY, false);
                currentY += MinecraftInstance.mc.font.lineHeight + 2;
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0)
                return false;

            int currentY = y + 6;
            for (FormattedCharSequence line : lines) {
                if (mouseY >= currentY && mouseY < currentY + MinecraftInstance.mc.font.lineHeight) {
                    int relativeMouseX = (int) (mouseX - (x + 10));
                    Style style = getStyleAt(line, relativeMouseX);
                    if (style != null && style.getClickEvent() != null) {
                        return handleComponentClick(style);
                    }
                }
                currentY += MinecraftInstance.mc.font.lineHeight + 2;
            }
            return false;
        }
    }
}
