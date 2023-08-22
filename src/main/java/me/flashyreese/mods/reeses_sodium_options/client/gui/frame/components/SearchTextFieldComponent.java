package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.util.StringUtils;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
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
    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    private final Predicate<String> textPredicate = Objects::nonNull;
    private final BiFunction<String, Integer, OrderedText> renderTextProvider = (string, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString(string, Style.EMPTY);
    private final AtomicReference<Text> tabFrameSelectedTab;
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

    public SearchTextFieldComponent(Dim2i dim, List<OptionPage> pages, AtomicReference<Text> tabFrameSelectedTab, AtomicReference<Integer> tabFrameScrollBarOffset, AtomicReference<Integer> optionPageScrollBarOffset, int tabDimHeight, SodiumVideoOptionsScreen sodiumVideoOptionsScreen, AtomicReference<String> lastSearch, AtomicReference<Integer> lastSearchIndex) {
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) {
            return;
        }
        if (!this.isFocused() && this.text.isBlank()) {
            String key = "rso.search_bar_empty";
            Text text = Text.translatable(key);
            if (text.getString().equals(key))
                text = Text.literal("Search options...");
            this.drawString(context, text, this.dim.x() + 6, this.dim.y() + 6, 0xFFAAAAAA);
        }

        this.drawRect(context, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.isFocused() ? 0xE0000000 : 0x90000000);
        int j = this.selectionStart - this.firstCharacterIndex;
        int k = this.selectionEnd - this.firstCharacterIndex;
        String string = this.textRenderer.trimToWidth(this.text.substring(this.firstCharacterIndex), this.getInnerWidth());
        boolean bl = j >= 0 && j <= string.length();
        int l = this.dim.x() + 6;
        int m = this.dim.y() + 6;
        int n = l;
        if (k > string.length()) {
            k = string.length();
        }
        if (!string.isEmpty()) {
            String string2 = bl ? string.substring(0, j) : string;
            n = context.drawTextWithShadow(this.textRenderer, this.renderTextProvider.apply(string2, this.firstCharacterIndex), n, m, 0xFFFFFFFF);
        }
        boolean bl3 = this.selectionStart < this.text.length() || this.text.length() >= this.getMaxLength();
        int o = n;
        if (!bl) {
            o = j > 0 ? l + this.dim.width() - 12 : l;
        } else if (bl3) {
            --o;
            --n;
        }
        if (!string.isEmpty() && bl && j < string.length()) {
            context.drawTextWithShadow(this.textRenderer, this.renderTextProvider.apply(string.substring(j), this.selectionStart), n, m, 0xFFFFFFFF);
        }
        // Cursor
        if (this.isFocused()) {
            context.fill(RenderLayer.getGuiOverlay(), o, m - 1, o + 1, m + 1 + this.textRenderer.fontHeight, -3092272);
        }
        // Highlighted text
        if (k != j) {
            int p = l + this.textRenderer.getWidth(string.substring(0, k));
            this.drawSelectionHighlight(context, o, m - 1, p - 1, m + 1 + this.textRenderer.fontHeight);
        }

        /*this.drawRect(context, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0x90000000);
        //this.drawBorder(context, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0xffffffff);
        this.drawString(context, this.text, this.dim.x() + 6, this.dim.y() + 6, 0xffffffff);*/
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = MathHelper.floor(mouseX) - this.dim.x() - 6;
        String string = this.textRenderer.trimToWidth(this.text.substring(this.firstCharacterIndex), this.getInnerWidth());
        this.setCursor(this.textRenderer.trimToWidth(string, i).length() + this.firstCharacterIndex);

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

    private void drawSelectionHighlight(DrawContext context, int x1, int y1, int x2, int y2) {
        int i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }
        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }
        if (x2 > this.dim.x() + this.dim.width()) {
            x2 = this.dim.x() + this.dim.width();
        }
        if (x1 > this.dim.x() + this.dim.width()) {
            x1 = this.dim.x() + this.dim.width();
        }
        context.fill(RenderLayer.getGuiTextHighlight(), x1, y1, x2, y2, -16776961);
    }

    private int getMaxLength() {
        return this.maxLength;
    }

    public String getSelectedText() {
        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        return this.text.substring(i, j);
    }

    public void write(String text) {


        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        int k = this.maxLength - this.text.length() - (i - j);
        String string = SharedConstants.stripInvalidChars(text);
        int l = string.length();
        if (k < l) {
            string = string.substring(0, k);
            l = k;
        }

        String string2 = (new StringBuilder(this.text)).replace(i, j, string).toString();
        if (this.textPredicate.test(string2)) {
            this.text = string2;
            this.setSelectionStart(i + l);
            this.setSelectionEnd(this.selectionStart);
            this.onChanged(this.text);
        }
    }

    private void onChanged(String newText) {
        this.pages.forEach(page -> page
                .getOptions()
                .stream()
                .filter(OptionExtended.class::isInstance)
                .map(OptionExtended.class::cast)
                .forEach(optionExtended -> optionExtended.setHighlight(false)));

        this.lastSearch.set(newText.trim());
        if (this.editable) {
            if (!newText.trim().isEmpty()) {
                List<Option<?>> fuzzy = StringUtils.fuzzySearch(this.pages, newText, 2);
                fuzzy.stream()
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
                int i = this.getCursorPosWithOffset(characterOffset);
                int j = Math.min(i, this.selectionStart);
                int k = Math.max(i, this.selectionStart);
                if (j != k) {
                    String string = (new StringBuilder(this.text)).delete(j, k).toString();
                    if (this.textPredicate.test(string)) {
                        this.text = string;
                        this.setCursor(j);
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
        int i = cursorPosition;
        boolean bl = wordOffset < 0;
        int j = Math.abs(wordOffset);

        for (int k = 0; k < j; ++k) {
            if (!bl) {
                int l = this.text.length();
                i = this.text.indexOf(32, i);
                if (i == -1) {
                    i = l;
                } else {
                    while (skipOverSpaces && i < l && this.text.charAt(i) == ' ') {
                        ++i;
                    }
                }
            } else {
                while (skipOverSpaces && i > 0 && this.text.charAt(i - 1) == ' ') {
                    --i;
                }

                while (i > 0 && this.text.charAt(i - 1) != ' ') {
                    --i;
                }
            }
        }

        return i;
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
        return Util.moveCursor(this.text, this.selectionStart, offset);
    }

    public void setSelectionStart(int cursor) {
        this.selectionStart = MathHelper.clamp(cursor, 0, this.text.length());
    }

    public void setCursorToStart() {
        this.setCursor(0);
    }

    public void setCursorToEnd() {
        this.setCursor(this.text.length());
    }

    public void setSelectionEnd(int index) {
        int i = this.text.length();
        this.selectionEnd = MathHelper.clamp(index, 0, i);
        if (this.textRenderer != null) {
            if (this.firstCharacterIndex > i) {
                this.firstCharacterIndex = i;
            }

            int j = this.getInnerWidth();
            String string = this.textRenderer.trimToWidth(this.text.substring(this.firstCharacterIndex), j);
            int k = string.length() + this.firstCharacterIndex;
            if (this.selectionEnd == this.firstCharacterIndex) {
                this.firstCharacterIndex -= this.textRenderer.trimToWidth(this.text, j, true).length();
            }

            if (this.selectionEnd > k) {
                this.firstCharacterIndex += this.selectionEnd - k;
            } else if (this.selectionEnd <= this.firstCharacterIndex) {
                this.firstCharacterIndex -= this.firstCharacterIndex - this.selectionEnd;
            }

            this.firstCharacterIndex = MathHelper.clamp(this.firstCharacterIndex, 0, i);
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
        if (SharedConstants.isValidChar(chr)) {
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
        this.pages.forEach(page2 -> page2
                .getOptions()
                .stream()
                .filter(OptionExtended.class::isInstance)
                .map(OptionExtended.class::cast)
                .forEach(optionExtended2 -> optionExtended2.setSelected(false)));
        if (!this.isActive()) {
            return false;
        } else {
            this.selecting = Screen.hasShiftDown();
            if (Screen.isSelectAll(keyCode)) {
                this.setCursorToEnd();
                this.setSelectionEnd(0);
                return true;
            } else if (Screen.isCopy(keyCode)) {
                MinecraftClient.getInstance().keyboard.setClipboard(this.getSelectedText());
                return true;
            } else if (Screen.isPaste(keyCode)) {
                if (this.editable) {
                    this.write(MinecraftClient.getInstance().keyboard.getClipboard());
                }

                return true;
            } else if (Screen.isCut(keyCode)) {
                MinecraftClient.getInstance().keyboard.setClipboard(this.getSelectedText());
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
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        if (!this.visible)
            return null;
        return super.getNavigationPath(navigation);
    }

    @Override
    public ScreenRect getNavigationFocus() {
        return new ScreenRect(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }
}