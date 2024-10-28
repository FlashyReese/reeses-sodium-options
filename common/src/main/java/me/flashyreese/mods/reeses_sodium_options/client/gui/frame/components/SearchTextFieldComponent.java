package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.util.StringUtils;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class SearchTextFieldComponent extends AbstractWidget {
    protected final Dim2i dim;
    protected final List<OptionPage> pages;
    private final Font font = Minecraft.getInstance().font;
    private final Predicate<String> textPredicate = Objects::nonNull;
    private final BiFunction<String, Integer, FormattedCharSequence> renderTextProvider = (string, firstCharacterIndex) -> FormattedCharSequence.forward(string, Style.EMPTY);
    private final AtomicReference<Component> tabFrameSelectedTab;
    private final AtomicReference<Integer> tabFrameScrollBarOffset;
    private final AtomicReference<Integer> optionPageScrollBarOffset;
    private final int tabDimHeight;
    private final SodiumVideoOptionsScreen sodiumVideoOptionsScreen;
    private final AtomicReference<String> lastSearch;
    private final AtomicReference<Integer> lastSearchIndex;
    protected boolean selecting;
    protected String text = "";
    protected int maxLength = 100;
    protected boolean visible = true;
    protected boolean editable = true;
    private int firstCharacterIndex;
    private int selectionStart;
    private int selectionEnd;
    private int lastCursorPosition = this.getCursor();

    // Cursor properties
    private static final long CURSOR_ANIMATION_DURATION = 750;
    private long nextCursorUpdate;
    private boolean currentCursorState;
    private float currentCursorAlpha;

    public SearchTextFieldComponent(Dim2i dim, List<OptionPage> pages, AtomicReference<Component> tabFrameSelectedTab, AtomicReference<Integer> tabFrameScrollBarOffset, AtomicReference<Integer> optionPageScrollBarOffset, int tabDimHeight, SodiumVideoOptionsScreen sodiumVideoOptionsScreen, AtomicReference<String> lastSearch, AtomicReference<Integer> lastSearchIndex) {
        this.dim = dim;
        this.pages = pages;
        this.tabFrameSelectedTab = tabFrameSelectedTab;
        this.tabFrameScrollBarOffset = tabFrameScrollBarOffset;
        this.optionPageScrollBarOffset = optionPageScrollBarOffset;
        this.tabDimHeight = tabDimHeight;
        this.sodiumVideoOptionsScreen = sodiumVideoOptionsScreen;
        this.lastSearch = lastSearch;
        this.lastSearchIndex = lastSearchIndex;
        if (!lastSearch.get().trim().isEmpty()) {
            this.write(lastSearch.get());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) {
            return;
        }
        updateCursorAlpha();
        if (!this.isFocused() && this.text.isBlank()) {
            String key = "rso.search_bar_empty";
            Component emptyText = Component.translatable(key);
            if (emptyText.getString().equals(key))
                emptyText = Component.literal("Search options...");
            this.drawString(guiGraphics, emptyText, this.dim.x() + 6, this.dim.y() + 6, 0xFFAAAAAA);
        }

        this.drawRect(guiGraphics, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.isFocused() ? 0xE0000000 : 0x90000000);
        int selectionStartOffset = this.selectionStart - this.firstCharacterIndex;
        int selectionEndOffset = this.selectionEnd - this.firstCharacterIndex;
        String displayedText = this.font.plainSubstrByWidth(this.text.substring(this.firstCharacterIndex), this.getInnerWidth());
        boolean isCursorWithinDisplayedText = selectionStartOffset >= 0 && selectionStartOffset <= displayedText.length();
        int textStartX = this.dim.x() + 6;
        int textStartY = this.dim.y() + 6;
        int textEndX = textStartX;
        if (selectionEndOffset > displayedText.length()) {
            selectionEndOffset = displayedText.length();
        }
        if (!displayedText.isEmpty()) {
            String preCursorText = isCursorWithinDisplayedText ? displayedText.substring(0, selectionStartOffset) : displayedText;
            textEndX = guiGraphics.drawString(this.font, this.renderTextProvider.apply(preCursorText, this.firstCharacterIndex), textEndX, textStartY, 0xFFFFFFFF);
        }
        boolean isCursorAtEnd = this.selectionStart < this.text.length() || this.text.length() >= this.getMaxLength();
        int cursorX = textEndX;
        if (!isCursorWithinDisplayedText) {
            cursorX = selectionStartOffset > 0 ? textStartX + this.dim.width() - 12 : textStartX;
        } else if (isCursorAtEnd) {
            --cursorX;
            --textEndX;
        }
        if (!displayedText.isEmpty() && isCursorWithinDisplayedText && selectionStartOffset < displayedText.length()) {
            guiGraphics.drawString(this.font, this.renderTextProvider.apply(displayedText.substring(selectionStartOffset), this.selectionStart), textEndX, textStartY, 0xFFFFFFFF);
        }
        // Cursor
        if (this.isFocused()) {
            int color = ((int) (this.currentCursorAlpha * 255) << 24) | 0x00D0D0D0;
            guiGraphics.fill(RenderType.guiOverlay(), cursorX, textStartY - 1, cursorX + 1, textStartY + 1 + this.font.lineHeight, color);
        }
        // Highlighted text
        if (selectionEndOffset != selectionStartOffset) {
            int selectionEndX = textStartX + this.font.width(displayedText.substring(0, selectionEndOffset));
            this.drawSelectionHighlight(guiGraphics, cursorX, textStartY - 1, selectionEndX - 1, textStartY + 1 + this.font.lineHeight);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int clickX = Mth.floor(mouseX) - this.dim.x() - 6;
        String displayedText = this.font.plainSubstrByWidth(this.text.substring(this.firstCharacterIndex), this.getInnerWidth());
        this.setCursor(this.font.plainSubstrByWidth(displayedText, clickX).length() + this.firstCharacterIndex);

        this.setFocused(this.dim.containsCursor(mouseX, mouseY));
        this.pages.forEach(page -> page
                .getOptions()
                .stream()
                .filter(OptionExtended.class::isInstance)
                .map(OptionExtended.class::cast)
                .forEach(optionExtended -> optionExtended.setSelected(false)));
        return this.isFocused();
    }

    // fixme: this is here because of 0.5.1's https://github.com/CaffeineMC/sodium-fabric/commit/20006a85fb7a64889f507eb13521e55693ae0d7e
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    private void drawSelectionHighlight(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY) {
        int temp;
        if (startX < endX) {
            temp = startX;
            startX = endX;
            endX = temp;
        }
        if (startY < endY) {
            temp = startY;
            startY = endY;
            endY = temp;
        }
        if (endX > this.dim.x() + this.dim.width()) {
            endX = this.dim.x() + this.dim.width();
        }
        if (startX > this.dim.x() + this.dim.width()) {
            startX = this.dim.x() + this.dim.width();
        }
        guiGraphics.fill(RenderType.guiTextHighlight(), startX, startY, endX, endY, -16776961);
    }

    private int getMaxLength() {
        return this.maxLength;
    }

    public String getSelectedText() {
        int selectionStartIndex = Math.min(this.selectionStart, this.selectionEnd);
        int selectionEndIndex = Math.max(this.selectionStart, this.selectionEnd);
        return this.text.substring(selectionStartIndex, selectionEndIndex);
    }

    public void write(String text) {
        int selectionStartIndex = Math.min(this.selectionStart, this.selectionEnd);
        int selectionEndIndex = Math.max(this.selectionStart, this.selectionEnd);
        int availableSpace = this.maxLength - this.text.length() - (selectionStartIndex - selectionEndIndex);
        String filteredText = StringUtil.filterText(text);
        int filteredTextLength = filteredText.length();
        if (availableSpace < filteredTextLength) {
            filteredText = filteredText.substring(0, availableSpace);
            filteredTextLength = availableSpace;
        }

        String beforeSelectionText = (new StringBuilder(this.text)).replace(selectionStartIndex, selectionEndIndex, filteredText).toString();
        if (this.textPredicate.test(beforeSelectionText)) {
            this.currentCursorState = true;
            this.nextCursorUpdate = System.currentTimeMillis() + CURSOR_ANIMATION_DURATION;

            this.text = beforeSelectionText;
            this.setSelectionStart(selectionStartIndex + filteredTextLength);
            this.setSelectionEnd(this.selectionStart);
            this.onChanged(this.text);
        }
    }

    private void onChanged(String query) {
        this.pages.forEach(page -> page.getOptions()
                .stream()
                .filter(OptionExtended.class::isInstance)
                .map(OptionExtended.class::cast)
                .forEach(optionExtended -> optionExtended.setHighlight(false))
        );

        this.lastSearch.set(query.trim());
        if (this.editable) {
            if (!query.trim().isEmpty()) {
                List<Option<?>> searchResults = StringUtils.searchElements(
                        () -> this.pages.stream().flatMap(p -> p.getOptions().stream()).iterator(),
                        query,
                        o -> String.format("%s %s", o.getName().getString(), o.getTooltip().getString())
                );
                searchResults.stream()
                        .filter(OptionExtended.class::isInstance)
                        .map(OptionExtended.class::cast)
                        .forEach(optionExtended -> optionExtended.setHighlight(true));
            }
        }
    }

    private void erase(int offset) {
        if (Screen.hasControlDown()) {
            this.eraseWords(offset);
        } else {
            this.eraseCharacters(offset);
        }

    }

    public void eraseWords(int wordOffset) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.selectionStart) {
                this.write("");
            } else {
                this.eraseCharacters(this.getWordSkipPosition(wordOffset) - this.selectionStart);
            }
        }
    }

    public void eraseCharacters(int characterOffset) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.selectionStart) {
                this.write("");
            } else {
                int cursorPosWithOffset = this.getCursorPosWithOffset(characterOffset);
                int startIndex = Math.min(cursorPosWithOffset, this.selectionStart);
                int endIndex = Math.max(cursorPosWithOffset, this.selectionStart);
                if (startIndex != endIndex) {
                    String newText = new StringBuilder(this.text).delete(startIndex, endIndex).toString();
                    if (this.textPredicate.test(newText)) {
                        this.text = newText;
                        this.setCursor(startIndex);
                        this.onChanged(this.text);
                    }
                }
            }
        }
    }

    public int getWordSkipPosition(int wordOffset) {
        return this.getWordSkipPosition(wordOffset, this.getCursor());
    }

    private int getWordSkipPosition(int wordOffset, int cursorPosition) {
        return this.getWordSkipPosition(wordOffset, cursorPosition, true);
    }

    private int getWordSkipPosition(int wordOffset, int cursorPosition, boolean skipOverSpaces) {
        int newPosition = cursorPosition;
        boolean isNegativeOffset = wordOffset < 0;
        int absoluteOffset = Math.abs(wordOffset);

        for (int i = 0; i < absoluteOffset; ++i) {
            if (!isNegativeOffset) {
                int textLength = this.text.length();
                newPosition = this.text.indexOf(' ', newPosition);
                if (newPosition == -1) {
                    newPosition = textLength;
                } else {
                    while (skipOverSpaces && newPosition < textLength && this.text.charAt(newPosition) == ' ') {
                        ++newPosition;
                    }
                }
            } else {
                while (skipOverSpaces && newPosition > 0 && this.text.charAt(newPosition - 1) == ' ') {
                    --newPosition;
                }
                while (newPosition > 0 && this.text.charAt(newPosition - 1) != ' ') {
                    --newPosition;
                }
            }
        }

        return newPosition;
    }

    public int getCursor() {
        return this.selectionStart;
    }

    public void setCursor(int cursor) {
        this.setSelectionStart(cursor);
        if (!this.selecting) {
            this.setSelectionEnd(this.selectionStart);
        }

        this.onChanged(this.text);
    }

    public void moveCursor(int offset) {
        this.setCursor(this.getCursorPosWithOffset(offset));
    }

    private int getCursorPosWithOffset(int offset) {
        return Util.offsetByCodepoints(this.text, this.selectionStart, offset);
    }

    public void setSelectionStart(int cursor) {
        this.selectionStart = Mth.clamp(cursor, 0, this.text.length());
    }

    public void setCursorToStart() {
        this.setCursor(0);
    }

    public void setCursorToEnd() {
        this.setCursor(this.text.length());
    }

    public void setSelectionEnd(int index) {
        int textLength = this.text.length();
        this.selectionEnd = Mth.clamp(index, 0, textLength);
        if (this.font != null) {
            if (this.firstCharacterIndex > textLength) {
                this.firstCharacterIndex = textLength;
            }

            int innerWidth = this.getInnerWidth();
            String displayText = this.font.plainSubstrByWidth(this.text.substring(this.firstCharacterIndex), innerWidth);
            int endIndex = displayText.length() + this.firstCharacterIndex;
            if (this.selectionEnd == this.firstCharacterIndex) {
                this.firstCharacterIndex -= this.font.plainSubstrByWidth(this.text, innerWidth, true).length();
            }

            if (this.selectionEnd > endIndex) {
                this.firstCharacterIndex += this.selectionEnd - endIndex;
            } else if (this.selectionEnd <= this.firstCharacterIndex) {
                this.firstCharacterIndex -= this.firstCharacterIndex - this.selectionEnd;
            }

            this.firstCharacterIndex = Mth.clamp(this.firstCharacterIndex, 0, textLength);
        }
    }

    public boolean isActive() {
        return this.isVisible() && this.isFocused() && this.isEditable();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!this.isActive()) {
            return false;
        }
        if (StringUtil.isAllowedChatCharacter(chr)) {
            if (this.editable) {
                this.lastSearch.set(this.text.trim());
                this.write(Character.toString(chr));
                this.lastSearchIndex.set(0);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.pages.forEach(page -> page.getOptions()
                .stream()
                .filter(OptionExtended.class::isInstance)
                .map(OptionExtended.class::cast)
                .forEach(optionExtended -> optionExtended.setSelected(false))
        );
        if (!this.isActive()) {
            return false;
        } else {
            this.selecting = Screen.hasShiftDown();
            if (Screen.isSelectAll(keyCode)) {
                this.setCursorToEnd();
                this.setSelectionEnd(0);
                return true;
            } else if (Screen.isCopy(keyCode)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
                return true;
            } else if (Screen.isPaste(keyCode)) {
                if (this.editable) {
                    this.write(Minecraft.getInstance().keyboardHandler.getClipboard());
                }

                return true;
            } else if (Screen.isCut(keyCode)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
                if (this.editable) {
                    this.write("");
                }

                return true;
            } else {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_ENTER -> {
                        if (this.editable) {
                            int count = 0;
                            for (OptionPage page : this.pages) {
                                for (Option<?> option : page.getOptions()) {
                                    if (option instanceof OptionExtended optionExtended && optionExtended.isHighlight() && optionExtended.getParentDimension() != null) {
                                        if (count == this.lastSearchIndex.get()) {
                                            Dim2i optionDim = optionExtended.getDim2i();
                                            Dim2i parentDim = optionExtended.getParentDimension();
                                            int maxOffset = parentDim.height() - this.tabDimHeight;
                                            int input = optionDim.y() - parentDim.y();
                                            int inputOffset = input + optionDim.height() == parentDim.height() ? parentDim.height() : input;
                                            int offset = inputOffset * maxOffset / parentDim.height();

                                            int total = this.pages.stream().mapToInt(page2 -> Math.toIntExact(page2.getOptions().stream().filter(OptionExtended.class::isInstance).map(OptionExtended.class::cast).filter(OptionExtended::isHighlight).count())).sum();

                                            int value = total == this.lastSearchIndex.get() + 1 ? 0 : this.lastSearchIndex.get() + 1;
                                            optionExtended.setSelected(true);
                                            this.lastSearchIndex.set(value);
                                            this.tabFrameSelectedTab.set(page.getName());
                                            // todo: calculate tab frame scroll bar offset
                                            this.tabFrameScrollBarOffset.set(0);

                                            this.optionPageScrollBarOffset.set(offset);
                                            this.setFocused(false);
                                            this.sodiumVideoOptionsScreen.rebuildUI();
                                            return true;
                                        }
                                        count++;
                                    }
                                }
                            }
                        }
                        return true;
                    }
                    case GLFW.GLFW_KEY_BACKSPACE -> {
                        if (this.editable) {
                            this.selecting = false;
                            this.erase(-1);
                            this.selecting = Screen.hasShiftDown();
                        }
                        return true;
                    }
                    case GLFW.GLFW_KEY_DELETE -> {
                        if (this.editable) {
                            this.selecting = false;
                            this.erase(1);
                            this.selecting = Screen.hasShiftDown();
                        }
                        return true;
                    }
                    case GLFW.GLFW_KEY_RIGHT -> {
                        if (Screen.hasControlDown()) {
                            this.setCursor(this.getWordSkipPosition(1));
                        } else {
                            this.moveCursor(1);
                        }
                        boolean state = this.getCursor() != this.lastCursorPosition && this.getCursor() != this.text.length() + 1;
                        this.lastCursorPosition = this.getCursor();
                        return state;
                    }
                    case GLFW.GLFW_KEY_LEFT -> {
                        if (Screen.hasControlDown()) {
                            this.setCursor(this.getWordSkipPosition(-1));
                        } else {
                            this.moveCursor(-1);
                        }
                        boolean state = this.getCursor() != this.lastCursorPosition && this.getCursor() != 0;
                        this.lastCursorPosition = this.getCursor();
                        return state;
                    }
                    case GLFW.GLFW_KEY_HOME -> {
                        this.setCursorToStart();
                        return true;
                    }
                    case GLFW.GLFW_KEY_END -> {
                        this.setCursorToEnd();
                        return true;
                    }
                    default -> {
                        return false;
                    }
                }
            }
        }
    }

    private void updateCursorAlpha() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis >= this.nextCursorUpdate) {
            this.currentCursorState = !this.currentCursorState;
            this.nextCursorUpdate = currentTimeMillis + CURSOR_ANIMATION_DURATION;
        }

        float cursorAlpha = (float) (this.nextCursorUpdate - currentTimeMillis) / CURSOR_ANIMATION_DURATION;

        if (cursorAlpha <= 0.25f) {
            cursorAlpha *= 4f;
        } else if (cursorAlpha >= 0.75f) {
            cursorAlpha = (1 - cursorAlpha) * 4f;
        } else {
            cursorAlpha = 1f;
        }

        cursorAlpha = Math.clamp(cursorAlpha, 0f, 1f);

        this.currentCursorAlpha = this.currentCursorState ? 1 : 1 - cursorAlpha;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isEditable() {
        return editable;
    }

    public int getInnerWidth() {
        return this.dim.width() - 12;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        if (!this.visible)
            return null;
        return super.nextFocusPath(navigation);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }
}