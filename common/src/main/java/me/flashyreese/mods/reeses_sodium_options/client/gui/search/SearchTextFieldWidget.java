package me.flashyreese.mods.reeses_sodium_options.client.gui.search;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionsScreenUiState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.TextFieldWidget;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class SearchTextFieldWidget extends TextFieldWidget {
    private final OptionSearch optionSearch;
    private final OptionsScreenUiState uiState;
    private final int tabDimHeight;
    private final Runnable refreshSearchResults;

    public SearchTextFieldWidget(LayoutBounds dim, List<Page> pages, OptionsScreenUiState uiState, int tabDimHeight, Runnable refreshSearchResults) {
        super(dim, Component.translatable("rso.search_bar_empty"));
        this.uiState = uiState;
        this.tabDimHeight = tabDimHeight;
        this.refreshSearchResults = refreshSearchResults;
        this.optionSearch = new OptionSearch(pages);

        String lastSearch = this.uiState.lastSearch().get();
        if (lastSearch != null && !lastSearch.trim().isEmpty()) {
            this.write(lastSearch);
        }
    }

    @Override
    protected void onTextChanged(String query) {
        this.uiState.lastSearch().set(query.trim());

        List<Identifier> resultIds = List.of();
        if (this.isEditable() && !query.trim().isEmpty()) {
            resultIds = this.optionSearch.query(query);
        }

        this.uiState.setHighlightedOptions(resultIds);
        if (this.uiState.updateSearchResults(resultIds)) {
            this.uiState.lastSearchIndex().set(0);
            this.refreshSearchResults.run();
        }
    }

    @Override
    protected void onInteraction() {
        this.uiState.clearSelectedOptions();
    }

    @Override
    protected boolean onSubmit() {
        if (!this.isEditable()) {
            return true;
        }

        List<OptionSearch.NavigationTarget> targets = this.optionSearch.navigationTargets(this.uiState, SearchResultOrder.DEFAULT);
        int total = targets.size();
        if (total == 0) {
            return true;
        }

        int startIndex = Math.floorMod(this.uiState.lastSearchIndex().getOrDefault(0), total);
        OptionSearch.NavigationTarget target = targets.get(startIndex);

        target.optionUiState().setSelected(true);
        this.uiState.lastSearchIndex().set((startIndex + 1) % total);
        this.uiState.tabFrameSelectedTab().set(target.tabName());
        this.uiState.scrollSelectedTabIntoView().set(true);
        this.uiState.optionPageScrollBarOffset().set(target.scrollOffset(this.tabDimHeight));
        this.refreshSearchResults.run();

        return true;
    }
}
