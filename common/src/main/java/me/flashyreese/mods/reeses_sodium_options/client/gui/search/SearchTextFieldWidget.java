package me.flashyreese.mods.reeses_sodium_options.client.gui.search;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionsScreenUiState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultEntry;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.TextFieldWidget;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SearchTextFieldWidget extends TextFieldWidget {
    private static final int CLEAR_BUTTON_SIZE = 12;
    private static final int CLEAR_BUTTON_RIGHT_PADDING = 4;
    private static final int CLEAR_BUTTON_TEXT_GAP = 4;
    private static final Component CLEAR_BUTTON_LABEL = Component.literal("x");

    private final OptionSearch optionSearch;
    private final OptionsScreenUiState uiState;
    private final int tabDimHeight;
    private final Runnable refreshSearchResults;

    public SearchTextFieldWidget(LayoutBounds dim, List<ModOptions> modOptions, OptionsScreenUiState uiState, int tabDimHeight, Runnable refreshSearchResults) {
        super(dim, Component.translatable("rso.search_bar_empty"));
        this.uiState = uiState;
        this.tabDimHeight = tabDimHeight;
        this.refreshSearchResults = refreshSearchResults;
        this.optionSearch = new OptionSearch(modOptions);

        String lastSearch = this.uiState.lastSearch().get();
        if (lastSearch != null && !lastSearch.trim().isEmpty()) {
            this.write(lastSearch);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (!this.showClearButton()) {
            return;
        }

        LayoutBounds buttonBounds = this.clearButtonBounds();
        boolean hovered = buttonBounds.contains(mouseX, mouseY);
        if (hovered) {
            this.drawRect(guiGraphics, buttonBounds.x(), buttonBounds.y(), buttonBounds.getLimitX(), buttonBounds.getLimitY(), GuiThemes.DEFAULT_BUTTON.bgHighlight);
        }

        int textX = buttonBounds.getCenterX() - this.getStringWidth(CLEAR_BUTTON_LABEL) / 2;
        int textY = buttonBounds.getCenterY() - Minecraft.getInstance().font.lineHeight / 2;
        this.drawString(guiGraphics, CLEAR_BUTTON_LABEL, textX, textY, hovered ? GuiThemes.DEFAULT_BUTTON.themeLighter : GuiThemes.DEFAULT_BUTTON.themeDarker);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.showClearButton() && this.clearButtonBounds().contains(mouseX, mouseY)) {
            this.setFocused(true);
            this.onInteraction();
            this.clearText();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public int getInnerWidth() {
        int width = super.getInnerWidth();
        if (this.showClearButton()) {
            width -= CLEAR_BUTTON_SIZE + CLEAR_BUTTON_TEXT_GAP;
        }

        return Math.max(0, width);
    }

    @Override
    protected void onTextChanged(String query) {
        String trimmedQuery = query.trim();
        boolean searchActive = !trimmedQuery.isEmpty();
        this.uiState.lastSearch().set(trimmedQuery);

        List<SearchResultEntry> results = List.of();
        if (this.isEditable() && searchActive) {
            results = this.optionSearch.query(query);
        }

        this.uiState.setHighlightedOptions(results);
        if (this.uiState.updateSearchResults(searchActive, results)) {
            this.uiState.lastSearchIndex().set(null);
            this.refreshSearchResults.run();
        }
    }

    @Override
    protected void onInteraction() {
        this.uiState.clearSelectedOptions();
    }

    @Override
    protected boolean onSubmit(boolean reverse) {
        if (!this.isEditable()) {
            return true;
        }

        List<OptionSearch.NavigationTarget> targets = this.optionSearch.navigationTargets(this.uiState, SearchResultOrder.DEFAULT);
        int total = targets.size();
        if (total == 0) {
            return true;
        }

        Integer lastIndex = this.uiState.lastSearchIndex().get();
        int targetIndex = lastIndex == null
                ? (reverse ? total - 1 : 0)
                : Math.floorMod(lastIndex + (reverse ? -1 : 1), total);
        OptionSearch.NavigationTarget target = targets.get(targetIndex);

        target.optionUiState().setSelected(true);
        this.uiState.lastSearchIndex().set(targetIndex);
        this.uiState.tabFrameSelectedTab().set(target.tabKey());
        this.uiState.scrollSelectedTabIntoView().set(true);
        this.uiState.optionPageScrollBarOffset().set(target.scrollOffset(this.tabDimHeight));
        this.refreshSearchResults.run();

        return true;
    }

    private boolean showClearButton() {
        return this.isVisible() && this.hasText();
    }

    private LayoutBounds clearButtonBounds() {
        int x = this.getLimitX() - CLEAR_BUTTON_RIGHT_PADDING - CLEAR_BUTTON_SIZE;
        int y = this.getY() + (this.getHeight() - CLEAR_BUTTON_SIZE) / 2;

        return new LayoutBounds(x, y, CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE);
    }
}
