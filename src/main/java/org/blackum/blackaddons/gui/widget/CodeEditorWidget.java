package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CodeEditorWidget extends Widget {
    private List<StringBuilder> lines = new ArrayList<>();
    private int cursorLine = 0;
    private int cursorColumn = 0;
    private int scrollX = 0;
    private int scrollY = 0;
    private int lineHeight = 10;
    private int gutterWidth = 30;
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;

    private SyntaxHighlighter syntaxHighlighter = null;

    public CodeEditorWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        lines.add(new StringBuilder());
        this.history.add(new State(lines, cursorLine, cursorColumn, selectionStartLine, selectionStartCol,
                selectionEndLine, selectionEndCol));
        this.historyIndex = 0;
    }

    public void setText(String text) {
        lines.clear();
        if (text == null || text.isEmpty()) {
            lines.add(new StringBuilder());
        } else {
            for (String line : text.split("\n", -1)) {
                lines.add(new StringBuilder(line));
            }
        }
        cursorLine = 0;
        cursorColumn = 0;
        limitCursor();
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void setSyntaxHighlighter(SyntaxHighlighter highlighter) {
        this.syntaxHighlighter = highlighter;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, true);

        graphics.fill(x, y, x + gutterWidth, y + height, Theme.CONTROL_BG_HOVER);
        graphics.fill(x + gutterWidth - 1, y, x + gutterWidth, y + height, Theme.BORDER);

        int maxVisibleLines = height / lineHeight;
        int startLine = scrollY;
        int endLine = Math.min(lines.size(), startLine + maxVisibleLines + 1);

        int textX = x + gutterWidth + 4;

        graphics.enableScissor(x + gutterWidth, y, x + width, y + height);

        for (int i = startLine; i < endLine; i++) {
            int drawY = y + (i - startLine) * lineHeight + 2;

            String lineNum = String.valueOf(i + 1);
            int lineNumWidth = Minecraft.getInstance().font.width(lineNum);
            graphics.drawString(Minecraft.getInstance().font, lineNum, x + gutterWidth - lineNumWidth - 4, drawY,
                    Theme.TEXT_SECONDARY, false);

            String lineText = lines.get(i).toString();

            if (hasSelection()) {
                int startL = selectionStartLine;
                int startC = selectionStartCol;
                int endL = selectionEndLine;
                int endC = selectionEndCol;

                if (startL > endL || (startL == endL && startC > endC)) {
                    int tempL = startL;
                    startL = endL;
                    endL = tempL;
                    int tempC = startC;
                    startC = endC;
                    endC = tempC;
                }

                if (i >= startL && i <= endL) {
                    int selStartC = (i == startL) ? startC : 0;
                    int selEndC = (i == endL) ? endC : lineText.length();

                    if (i != endL)
                        selEndC++;

                    String renderText = lineText;
                    if (i != endL)
                        renderText += " ";

                    if (selStartC < renderText.length()) {
                        selEndC = Math.min(selEndC, renderText.length());

                        int sX = textX - scrollX
                                + Minecraft.getInstance().font.width(renderText.substring(0, selStartC));
                        int eX = textX - scrollX + Minecraft.getInstance().font.width(renderText.substring(0, selEndC));

                        graphics.fill(sX, drawY - 1, eX, drawY + lineHeight - 1, Theme.withAlpha(Theme.ACCENT, 0.4f));
                    }
                }
            }

            if (syntaxHighlighter != null) {
                List<Style> styles = syntaxHighlighter.highlight(lineText);
                int currentX = textX - scrollX;
                for (int j = 0; j < lineText.length(); j++) {
                    Style style = (styles != null && j < styles.size()) ? styles.get(j) : Style.EMPTY;
                    String charStr = String.valueOf(lineText.charAt(j));
                    graphics.drawString(Minecraft.getInstance().font, Component.literal(charStr).setStyle(style),
                            currentX, drawY, Theme.TEXT_PRIMARY, false);
                    currentX += Minecraft.getInstance().font.width(charStr);
                }
            } else {
                graphics.drawString(Minecraft.getInstance().font, lineText, textX - scrollX, drawY, Theme.TEXT_PRIMARY,
                        false);
            }
        }

        if (focused && cursorVisible) {
            if (cursorLine >= startLine && cursorLine < endLine) {
                int cursorDrawY = y + (cursorLine - startLine) * lineHeight + 2;
                String lineBeforeCursor = lines.get(cursorLine).substring(0, cursorColumn);
                int cursorDrawX = textX - scrollX + Minecraft.getInstance().font.width(lineBeforeCursor);

                if (cursorDrawX >= x + gutterWidth && cursorDrawX < x + width) {
                    graphics.fill(cursorDrawX, cursorDrawY - 1, cursorDrawX + 1, cursorDrawY + 9, Theme.TEXT_PRIMARY);
                }
            }
        }

        graphics.disableScissor();

        if (focused) {
            RenderHelper.renderRoundedOutline(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, Theme.ACCENT);
        }
    }

    @Override
    public void tick() {
        if (System.currentTimeMillis() - lastCursorBlink > 500) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = System.currentTimeMillis();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            focused = true;
            if (button == 0) {
                int rY = (int) mouseY - y - 2;
                int clickedLine = scrollY + (rY / lineHeight);

                if (clickedLine < 0)
                    clickedLine = 0;
                if (clickedLine >= lines.size())
                    clickedLine = lines.size() - 1;

                int textX = x + gutterWidth + 4 - scrollX;
                int rX = (int) mouseX - textX;

                int col = getColFromX(clickedLine, rX);

                cursorLine = clickedLine;
                cursorColumn = col;

                clearSelection();
                isDragging = true;
                selectionStartLine = cursorLine;
                selectionStartCol = cursorColumn;
                selectionEndLine = cursorLine;
                selectionEndCol = cursorColumn;

                scrollToCursor();
                return true;
            }
        }
        focused = false;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            int rY = (int) mouseY - y - 2;
            int targetLine = scrollY + (rY / lineHeight);

            if (targetLine < 0)
                targetLine = 0;
            if (targetLine >= lines.size())
                targetLine = lines.size() - 1;

            int textX = x + gutterWidth + 4 - scrollX;
            int rX = (int) mouseX - textX;

            int col = getColFromX(targetLine, rX);

            cursorLine = targetLine;
            cursorColumn = col;

            selectionEndLine = cursorLine;
            selectionEndCol = cursorColumn;

            scrollToCursor();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private int getColFromX(int lineIdx, int rX) {
        if (rX < 0)
            return 0;

        StringBuilder line = lines.get(lineIdx);
        String lineStr = line.toString();
        int col = 0;
        int cw = 0;
        for (int i = 0; i < lineStr.length(); i++) {
            int charW = Minecraft.getInstance().font.width(String.valueOf(lineStr.charAt(i)));
            if (cw + charW / 2 > rX) {
                break;
            }
            cw += charW;
            col++;
        }
        return col;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused)
            return false;

        boolean isCtrlPressed = (modifiers & 2) != 0;

        // Undo
        if (isCtrlPressed && keyCode == 90) { // Z
            undo();
            return true;
        }

        // Redo
        if (isCtrlPressed && keyCode == 89) { // Y
            redo();
            return true;
        }

        if (keyCode == 259) { // Backspace
            if (hasSelection()) {
                deleteSelection();
                pushHistory();
                scrollToCursor();
                return true;
            }
        }

        if (keyCode == 261) { // Delete
            if (hasSelection()) {
                deleteSelection();
                pushHistory();
                scrollToCursor();
                return true;
            }
        }

        // Select All
        if (isCtrlPressed && keyCode == 65) {
            selectAll();
            return true;
        }

        // Copy
        if (isCtrlPressed && keyCode == 67) {
            if (hasSelection()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(getSelectedText());
            }
            return true;
        }

        // Paste
        if (isCtrlPressed && keyCode == 86) {
            if (hasSelection()) {
                deleteSelection();
            }
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                insertText(clipboard);
                pushHistory();
            }
            return true;
        }

        // Cut
        if (isCtrlPressed && keyCode == 88) {
            if (hasSelection()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(getSelectedText());
                deleteSelection();
                pushHistory();
            }
            return true;
        }

        boolean isShiftPressed = (modifiers & 1) != 0;

        if (keyCode == 257) { // Enter
            if (hasSelection())
                deleteSelection();
            insertNewLine();
            pushHistory();
            return true;
        }

        if (keyCode == 259) { // Backspace
            deleteBack();
            pushHistory();
            return true;
        }

        if (keyCode == 261) { // Delete
            deleteForward();
            pushHistory();
            return true;
        }

        if (keyCode == 263) { // Left
            if (isCtrlPressed) {
                moveCursorWord(-1, isShiftPressed);
            } else {
                moveCursor(-1, isShiftPressed);
            }
            return true;
        }

        if (keyCode == 262) { // Right
            if (isCtrlPressed) {
                moveCursorWord(1, isShiftPressed);
            } else {
                moveCursor(1, isShiftPressed);
            }
            return true;
        }

        if (keyCode == 265) { // Up
            moveCursorLine(-1, isShiftPressed);
            return true;
        }

        if (keyCode == 264) { // Down
            moveCursorLine(1, isShiftPressed);
            return true;
        }

        if (keyCode == 268) { // Home
            if (isCtrlPressed) {
                moveCursorToDocStart(isShiftPressed);
            } else {
                moveCursorToLineStart(isShiftPressed);
            }
            return true;
        }

        if (keyCode == 269) { // End
            if (isCtrlPressed) {
                moveCursorToDocEnd(isShiftPressed);
            } else {
                moveCursorToLineEnd(isShiftPressed);
            }
            return true;
        }

        return false;
    }

    private void insertNewLine() {
        StringBuilder current = lines.get(cursorLine);
        String afterCursor = current.substring(cursorColumn);
        current.setLength(cursorColumn);

        lines.add(cursorLine + 1, new StringBuilder(afterCursor));
        cursorLine++;
        cursorColumn = 0;
        scrollToCursor();
    }

    private void deleteBack() {
        if (cursorColumn > 0) {
            lines.get(cursorLine).deleteCharAt(cursorColumn - 1);
            cursorColumn--;
        } else if (cursorLine > 0) {
            StringBuilder prev = lines.get(cursorLine - 1);
            int prevLen = prev.length();
            prev.append(lines.get(cursorLine));
            lines.remove(cursorLine);
            cursorLine--;
            cursorColumn = prevLen;
        }
        scrollToCursor();
    }

    private void deleteForward() {
        if (cursorColumn < lines.get(cursorLine).length()) {
            lines.get(cursorLine).deleteCharAt(cursorColumn);
        } else if (cursorLine < lines.size() - 1) {
            StringBuilder current = lines.get(cursorLine);
            current.append(lines.get(cursorLine + 1));
            lines.remove(cursorLine + 1);
        }
        scrollToCursor();
    }

    private void insertText(String text) {
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] parts = text.split("\n", -1);

        if (parts.length == 0)
            return;

        StringBuilder currentLine = lines.get(cursorLine);
        String suffix = currentLine.substring(cursorColumn);
        currentLine.setLength(cursorColumn);
        currentLine.append(parts[0]);

        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                lines.add(cursorLine + i, new StringBuilder(parts[i]));
            }
            cursorLine += parts.length - 1;
            cursorColumn = lines.get(cursorLine).length();
        } else {
            cursorColumn += parts[0].length();
        }

        lines.get(cursorLine).append(suffix);
        scrollToCursor();
    }

    private void moveCursor(int offset, boolean select) {
        if (offset == -1) {
            if (cursorColumn > 0) {
                cursorColumn--;
            } else if (cursorLine > 0) {
                cursorLine--;
                cursorColumn = lines.get(cursorLine).length();
            }
        } else if (offset == 1) {
            if (cursorColumn < lines.get(cursorLine).length()) {
                cursorColumn++;
            } else if (cursorLine < lines.size() - 1) {
                cursorLine++;
                cursorColumn = 0;
            }
        }
        updateSelection(select);
        scrollToCursor();
    }

    private void moveCursorWord(int dir, boolean select) {
        if (dir == -1) {
            if (cursorColumn > 0) {
                int pos = cursorColumn;
                String line = lines.get(cursorLine).toString();
                while (pos > 0 && line.charAt(pos - 1) == ' ')
                    pos--;
                while (pos > 0 && line.charAt(pos - 1) != ' ')
                    pos--;
                cursorColumn = pos;
            } else if (cursorLine > 0) {
                cursorLine--;
                cursorColumn = lines.get(cursorLine).length();
            }
        } else {
            String line = lines.get(cursorLine).toString();
            if (cursorColumn < line.length()) {
                int pos = cursorColumn;
                while (pos < line.length() && line.charAt(pos) == ' ')
                    pos++;
                while (pos < line.length() && line.charAt(pos) != ' ')
                    pos++;
                cursorColumn = pos;
            } else if (cursorLine < lines.size() - 1) {
                cursorLine++;
                cursorColumn = 0;
            }
        }
        updateSelection(select);
        scrollToCursor();
    }

    private void moveCursorLine(int dir, boolean select) {
        if (dir == -1) {
            if (cursorLine > 0) {
                cursorLine--;
                limitCursor();
            }
        } else {
            if (cursorLine < lines.size() - 1) {
                cursorLine++;
                limitCursor();
            }
        }
        updateSelection(select);
        scrollToCursor();
    }

    private void moveCursorToLineStart(boolean select) {
        cursorColumn = 0;
        updateSelection(select);
        scrollToCursor();
    }

    private void moveCursorToLineEnd(boolean select) {
        cursorColumn = lines.get(cursorLine).length();
        updateSelection(select);
        scrollToCursor();
    }

    private void moveCursorToDocStart(boolean select) {
        cursorLine = 0;
        cursorColumn = 0;
        updateSelection(select);
        scrollToCursor();
    }

    private void moveCursorToDocEnd(boolean select) {
        cursorLine = lines.size() - 1;
        cursorColumn = lines.get(cursorLine).length();
        updateSelection(select);
        scrollToCursor();
    }

    private int selectionStartLine = -1;
    private int selectionStartCol = -1;
    private int selectionEndLine = -1;
    private int selectionEndCol = -1;
    private boolean isDragging = false;

    private boolean hasSelection() {
        return selectionStartLine != -1;
    }

    private void updateSelection(boolean keep) {
        if (keep) {
            if (selectionStartLine == -1) {
                selectionStartLine = cursorLine;
                selectionStartCol = cursorColumn;
            }
            selectionEndLine = cursorLine;
            selectionEndCol = cursorColumn;
        } else {
            clearSelection();
        }
    }

    private void clearSelection() {
        selectionStartLine = -1;
        selectionStartCol = -1;
        selectionEndLine = -1;
        selectionEndCol = -1;
    }

    private void selectAll() {
        selectionStartLine = 0;
        selectionStartCol = 0;
        selectionEndLine = lines.size() - 1;
        selectionEndCol = lines.get(selectionEndLine).length();
        cursorLine = selectionEndLine;
        cursorColumn = selectionEndCol;
        scrollToCursor();
    }

    private String getSelectedText() {
        if (!hasSelection())
            return "";
        int startL = selectionStartLine;
        int startC = selectionStartCol;
        int endL = selectionEndLine;
        int endC = selectionEndCol;

        if (startL > endL || (startL == endL && startC > endC)) {
            int tempL = startL;
            startL = endL;
            endL = tempL;
            int tempC = startC;
            startC = endC;
            endC = tempC;
        }

        StringBuilder sb = new StringBuilder();
        if (startL == endL) {
            sb.append(lines.get(startL).substring(startC, endC));
        } else {
            sb.append(lines.get(startL).substring(startC));
            sb.append("\n");
            for (int i = startL + 1; i < endL; i++) {
                sb.append(lines.get(i)).append("\n");
            }
            sb.append(lines.get(endL).substring(0, endC));
        }
        return sb.toString();
    }

    private void deleteSelection() {
        if (!hasSelection())
            return;

        int startL = selectionStartLine;
        int startC = selectionStartCol;
        int endL = selectionEndLine;
        int endC = selectionEndCol;

        if (startL > endL || (startL == endL && startC > endC)) {
            int tempL = startL;
            startL = endL;
            endL = tempL;
            int tempC = startC;
            startC = endC;
            endC = tempC;
        }

        if (startL == endL) {
            lines.get(startL).delete(startC, endC);
        } else {
            lines.get(startL).delete(startC, lines.get(startL).length());
            lines.get(endL).delete(0, endC);

            lines.get(startL).append(lines.get(endL));

            for (int i = 0; i < endL - startL; i++) {
                lines.remove(startL + 1);
            }
        }

        cursorLine = startL;
        cursorColumn = startC;
        clearSelection();
        scrollToCursor();
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!focused)
            return false;

        if (character >= 32) {
            if (hasSelection()) {
                deleteSelection();
            }
            lines.get(cursorLine).insert(cursorColumn, character);
            cursorColumn++;
            pushHistory();
            scrollToCursor();
            return true;
        }
        return false;
    }

    private void limitCursor() {
        if (cursorLine < 0)
            cursorLine = 0;
        if (cursorLine >= lines.size())
            cursorLine = lines.size() - 1;

        int len = lines.get(cursorLine).length();
        if (cursorColumn > len)
            cursorColumn = len;
        if (cursorColumn < 0)
            cursorColumn = 0;
    }

    private void scrollToCursor() {
        int maxVisibleLines = height / lineHeight;
        if (cursorLine < scrollY) {
            scrollY = cursorLine;
        } else if (cursorLine >= scrollY + maxVisibleLines) {
            scrollY = cursorLine - maxVisibleLines + 1;
        }

    }

    private static class State {
        List<StringBuilder> lines;
        int cursorLine;
        int cursorColumn;
        int selectionStartLine;
        int selectionStartCol;
        int selectionEndLine;
        int selectionEndCol;

        State(List<StringBuilder> lines, int cursorLine, int cursorColumn, int selectionStartLine,
                int selectionStartCol, int selectionEndLine, int selectionEndCol) {
            this.lines = new ArrayList<>();
            for (StringBuilder sb : lines) {
                this.lines.add(new StringBuilder(sb));
            }
            this.cursorLine = cursorLine;
            this.cursorColumn = cursorColumn;
            this.selectionStartLine = selectionStartLine;
            this.selectionStartCol = selectionStartCol;
            this.selectionEndLine = selectionEndLine;
            this.selectionEndCol = selectionEndCol;
        }
    }

    private LinkedList<State> history = new LinkedList<>();
    private int historyIndex = -1;

    private void pushHistory() {
        while (history.size() > historyIndex + 1) {
            history.removeLast();
        }
        if (history.size() > 50) {
            history.removeFirst();
            historyIndex--;
        }
        history.add(new State(lines, cursorLine, cursorColumn, selectionStartLine, selectionStartCol, selectionEndLine,
                selectionEndCol));
        historyIndex++;
    }

    private void undo() {
        if (historyIndex > 0) {
            historyIndex--;
            restoreState(history.get(historyIndex));
        }
    }

    private void redo() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            restoreState(history.get(historyIndex));
        }
    }

    private void restoreState(State state) {
        this.lines.clear();
        for (StringBuilder sb : state.lines) {
            this.lines.add(new StringBuilder(sb));
        }
        this.cursorLine = state.cursorLine;
        this.cursorColumn = state.cursorColumn;
        this.selectionStartLine = state.selectionStartLine;
        this.selectionStartCol = state.selectionStartCol;
        this.selectionEndLine = state.selectionEndLine;
        this.selectionEndCol = state.selectionEndCol;
        scrollToCursor();
    }
}
