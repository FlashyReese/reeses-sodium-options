package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import me.flashyreese.mods.reeses_sodium_options.client.gui.AbstractWidgetExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.client.search.SearchIndex;
import me.flashyreese.mods.reeses_sodium_options.client.search.SearchResult;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class SearchTextFieldComponent extends AbstractWidget {
    // Cursor properties
    private static final long CURSOR_ANIMATION_DURATION = 750;
    protected final List<Page> pages;
    private final Font font = Minecraft.getInstance().font;
    private final Predicate<String> textPredicate = Objects::nonNull;
    private final BiFunction<String, Integer, FormattedCharSequence> renderTextProvider = (string, firstCharacterIndex) -> FormattedCharSequence.forward(string, Style.EMPTY);
    private final SodiumVideoOptionsScreen.UiState uiState;
    private final int tabDimHeight;
    private final SodiumVideoOptionsScreen sodiumVideoOptionsScreen;
    protected boolean selecting;
    protected String text = "";
    protected int maxLength = 100;
    protected boolean visible = true;
    protected boolean editable = true;
    private final SearchIndex<Option> searchIndex;
    private int firstCharacterIndex;
    private int selectionStart;
    private int selectionEnd;
    private int lastCursorPosition = this.getCursor();
    private long nextCursorUpdate;
    private boolean currentCursorState;
    private float currentCursorAlpha;

    public SearchTextFieldComponent(Dim2i dim, List<Page> pages, SodiumVideoOptionsScreen.UiState uiState, int tabDimHeight, SodiumVideoOptionsScreen sodiumVideoOptionsScreen) {
        super(dim);
        this.pages = pages;
        this.uiState = uiState;
        this.tabDimHeight = tabDimHeight;
        this.sodiumVideoOptionsScreen = sodiumVideoOptionsScreen;
        List<Option> options = this.pages.stream()
                .flatMap(page -> page.groups().stream())
                .flatMap(optionGroup -> optionGroup.options().stream())
                .toList();
        this.searchIndex = SearchIndex.builder((Option option) -> String.format("%s %s", option.getName().getString(), option.getTooltip().getString()))
                .addAll(options)
                .foldDiacritics(true)
                .maxResults(10)
                .minScore(0.15)
                .rerankWithEditDistance(true)
                .rerankLimit(50)
                .rerankWeight(0.1)
                .build();
        if (!this.uiState.lastSearch().get().trim().isEmpty()) {
            this.write(this.uiState.lastSearch().get());
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) {
            return;
        }
        updateCursorAlpha();
        if (!this.isFocused() && this.text.isBlank()) {
            String key = "rso.search_bar_empty";
            Component emptyText = Component.translatable(key);
            if (emptyText.getString().equals(key))
                emptyText = Component.literal("Search options...");
            this.drawString(guiGraphics, emptyText, this.getX() + 6, ((AbstractWidgetExtended) this).getDim().y() + 6, 0xFFAAAAAA);
        }

        this.drawRect(guiGraphics, this.getX(), ((AbstractWidgetExtended) this).getDim().y(), ((AbstractWidgetExtended) this).getDim().getLimitX(), ((AbstractWidgetExtended) this).getDim().getLimitY(), this.isFocused() ? 0xE0000000 : 0x90000000);
        int selectionStartOffset = this.selectionStart - this.firstCharacterIndex;
        int selectionEndOffset = this.selectionEnd - this.firstCharacterIndex;
        String displayedText = this.font.plainSubstrByWidth(this.text.substring(this.firstCharacterIndex), this.getInnerWidth());
        boolean isCursorWithinDisplayedText = selectionStartOffset >= 0 && selectionStartOffset <= displayedText.length();
        int textStartX = this.getX() + 6;
        int textStartY = this.getY() + 6;
        int textEndX = textStartX;
        if (selectionEndOffset > displayedText.length()) {
            selectionEndOffset = displayedText.length();
        }
        if (!displayedText.isEmpty()) {
            String preCursorText = isCursorWithinDisplayedText ? displayedText.substring(0, selectionStartOffset) : displayedText;
            guiGraphics.drawString(this.font, this.renderTextProvider.apply(preCursorText, this.firstCharacterIndex), textEndX, textStartY, 0xFFFFFFFF);
            textEndX = textEndX + this.font.width(this.renderTextProvider.apply(preCursorText, this.firstCharacterIndex));
        }
        boolean isCursorAtEnd = this.selectionStart < this.text.length() || this.text.length() >= this.getMaxLength();
        int cursorX = textEndX;
        if (!isCursorWithinDisplayedText) {
            cursorX = selectionStartOffset > 0 ? textStartX + this.getWidth() - 12 : textStartX;
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
            guiGraphics.fill(RenderPipelines.GUI, cursorX, textStartY - 1, cursorX + 1, textStartY + 1 + this.font.lineHeight, color);
        }
        // Highlighted text
        if (selectionEndOffset != selectionStartOffset) {
            int selectionEndX = textStartX + this.font.width(displayedText.substring(0, selectionEndOffset));
            this.drawSelectionHighlight(guiGraphics, cursorX, textStartY - 1, selectionEndX - 1, textStartY + 1 + this.font.lineHeight);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean repeated) {
        int clickX = Mth.floor(event.x()) - this.getX() - 6;
        String displayedText = this.font.plainSubstrByWidth(this.text.substring(this.firstCharacterIndex), this.getInnerWidth());
        this.setCursor(this.font.plainSubstrByWidth(displayedText, clickX).length() + this.firstCharacterIndex);

        this.setFocused(this.isMouseOver(event.x(), event.y()));
        this.pages.forEach(page -> page
                .groups()
                .stream()
                .flatMap(optionGroup -> optionGroup.options().stream())
                .toList()
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
        if (endX > this.getX() + this.getWidth()) {
            endX = this.getX() + this.getWidth();
        }
        if (startX > this.getX() + this.getWidth()) {
            startX = this.getX() + this.getWidth();
        }
        guiGraphics.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, startX, startY, endX, endY, -16776961);
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

    public boolean hasText() {
        return !this.text.isEmpty();
    }

    public void clearText() {
        if (this.text.isEmpty()) {
            return;
        }

        this.text = "";
        this.firstCharacterIndex = 0;
        this.selectionStart = 0;
        this.selectionEnd = 0;
        this.lastCursorPosition = 0;
        this.uiState.lastSearchIndex().set(0);
        this.onChanged(this.text);
    }

    public void selectAllText() {
        this.setCursorToEnd();
        this.setSelectionEnd(0);
    }

    private void onChanged(String query) {
        this.pages.forEach(page -> page
                .groups()
                .stream()
                .flatMap(optionGroup -> optionGroup.options().stream())
                .toList()
                .stream()
                .filter(OptionExtended.class::isInstance)
                .map(OptionExtended.class::cast)
                .forEach(optionExtended -> optionExtended.setHighlight(false))
        );

        this.uiState.lastSearch().set(query.trim());
        List<Identifier> resultIds = List.of();
        if (this.editable && !query.trim().isEmpty()) {
            List<Option> searchResults = this.searchIndex.newSession(query)
                    .results()
                    .stream()
                    .map(SearchResult::item)
                    .toList();
            searchResults.stream()
                    .filter(OptionExtended.class::isInstance)
                    .map(OptionExtended.class::cast)
                    .forEach(optionExtended -> optionExtended.setHighlight(true));
            resultIds = searchResults.stream()
                    .filter(OptionExtended.class::isInstance)
                    .map(OptionExtended.class::cast)
                    .map(OptionExtended::getId)
                    .toList();
        }
        if (this.uiState.updateSearchResults(resultIds)) {
            this.sodiumVideoOptionsScreen.rebuildUI();
        }
    }

    private void erase(int offset) {
        if (Minecraft.getInstance().hasControlDown()) {
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

    public boolean isActive() {
        return this.isVisible() && this.isFocused() && this.isEditable();
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!this.isActive()) {
            return false;
        }
        if (characterEvent.isAllowedChatCharacter()) {
            if (this.editable) {
                this.uiState.lastSearch().set(this.text.trim());
                this.write(characterEvent.codepointAsString());
                this.uiState.lastSearchIndex().set(0);            }
            return true;
        }
        return false;
    }


    @Override
    public boolean keyPressed(KeyEvent event) {
        this.pages.forEach(page -> page
                .groups()
                .stream()
                .flatMap(optionGroup -> optionGroup.options().stream())
                .toList()
                .stream()
                .filter(OptionExtended.class::isInstance)
                .map(OptionExtended.class::cast)
                .forEach(optionExtended -> optionExtended.setSelected(false))
        );
        if (!this.isActive()) {
            return false;
        } else {
            this.selecting = event.hasShiftDown();
            if (event.isSelectAll()) {
                this.setCursorToEnd();
                this.setSelectionEnd(0);
                return true;
            } else if (event.isCopy()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
                return true;
            } else if (event.isPaste()) {
                if (this.editable) {
                    this.write(Minecraft.getInstance().keyboardHandler.getClipboard());
                }

                return true;
            } else if (event.isCut()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
                if (this.editable) {
                    this.write("");
                }

                return true;
            } else {
                switch (event.key()) {
                    case GLFW.GLFW_KEY_ENTER -> {
                        if (this.editable) {
                            int count = 0;
                            for (Page page : this.pages) {
                                for (OptionGroup group : page.groups()) {
                                    for (Option option : group.options()) {
                                        if (option instanceof OptionExtended optionExtended && optionExtended.isHighlight() && optionExtended.getParentDimension() != null) {
                                            if (count == this.uiState.lastSearchIndex().get()) {
                                                Dim2i optionDim = optionExtended.getDim2i();
                                                Dim2i parentDim = optionExtended.getParentDimension();
                                                int maxOffset = parentDim.height() - this.tabDimHeight;
                                                int input = optionDim.y() - parentDim.y();
                                                int inputOffset = input + optionDim.height() == parentDim.height() ? parentDim.height() : input;
                                                int offset = inputOffset * maxOffset / parentDim.height();

                                                int total = this.pages.stream().mapToInt(page2 -> Math.toIntExact(page2.groups().stream().flatMap(group2 -> group2.options().stream()).toList().stream().filter(OptionExtended.class::isInstance).map(OptionExtended.class::cast).filter(OptionExtended::isHighlight).count())).sum();

                                                int value = total == this.uiState.lastSearchIndex().get() + 1 ? 0 : this.uiState.lastSearchIndex().get() + 1;
                                                optionExtended.setSelected(true);
                                                this.uiState.lastSearchIndex().set(value);
                                                this.uiState.tabFrameSelectedTab().set(page.name());
                                                // todo: calculate tab frame scroll bar offset
                                                this.uiState.tabFrameScrollBarOffset().set(0);

                                                this.uiState.optionPageScrollBarOffset().set(offset);
                                                this.sodiumVideoOptionsScreen.rebuildUI();
                                                return true;
                                            }
                                            count++;
                                        }
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
                            this.selecting = event.hasShiftDown();
                        }
                        return true;
                    }
                    case GLFW.GLFW_KEY_DELETE -> {
                        if (this.editable) {
                            this.selecting = false;
                            this.erase(1);
                            this.selecting = event.hasShiftDown();
                        }
                        return true;
                    }
                    case GLFW.GLFW_KEY_RIGHT -> {
                        if (event.hasControlDown()) {
                            this.setCursor(this.getWordSkipPosition(1));
                        } else {
                            this.moveCursor(1);
                        }
                        boolean state = this.getCursor() != this.lastCursorPosition && this.getCursor() != this.text.length() + 1;
                        this.lastCursorPosition = this.getCursor();
                        return state;
                    }
                    case GLFW.GLFW_KEY_LEFT -> {
                        if (event.hasControlDown()) {
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
        return this.getWidth() - 12;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        if (!this.visible)
            return null;
        return super.nextFocusPath(navigation);
    }
}
