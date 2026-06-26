package me.flashyreese.mods.reeses_sodium_options.client.gui.search;

import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionLayoutState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionUiState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrdering;
import me.flashyreese.mods.reeses_sodium_options.client.search.SearchIndex;
import me.flashyreese.mods.reeses_sodium_options.client.search.SearchResult;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class OptionSearch {
    private final List<SearchableOption> options;
    private final SearchIndex<SearchableOption> searchIndex;

    OptionSearch(List<ModOptions> modOptionsList) {
        List<SearchableOption> options = new ArrayList<>();

        for (ModOptions modOptions : modOptionsList) {
            for (Page page : modOptions.pages()) {
                String tabKey = modOptions.configId() + ":" + page.name().getString();
                for (var group : page.groups()) {
                    for (var option : group.options()) {
                        if (!(option instanceof OptionExtended optionExtended)) {
                            continue;
                        }

                        options.add(new SearchableOption(
                                optionExtended.rso$getId(),
                                tabKey,
                                String.format("%s %s", option.getName().getString(), option.getTooltip().getString())));
                    }
                }
            }
        }

        this.options = List.copyOf(options);
        this.searchIndex = SearchIndex.builder(SearchableOption::searchableText)
                .addAll(this.options)
                .foldDiacritics(true)
                .maxResults(10)
                .minScore(0.15)
                .rerankWithEditDistance(true)
                .rerankLimit(50)
                .rerankWeight(0.1)
                .build();
    }

    List<ResourceLocation> query(String query) {
        return this.searchIndex.newSession(query)
                .results()
                .stream()
                .map(SearchResult::item)
                .map(SearchableOption::id)
                .toList();
    }

    List<NavigationTarget> navigationTargets(OptionStateStore optionStateStore, SearchResultOrder order) {
        List<SearchableOption> orderedOptions = SearchResultOrdering.order(
                order, optionStateStore.searchResultIds(), this.options, SearchableOption::id);

        List<NavigationTarget> targets = new ArrayList<>(orderedOptions.size());
        for (SearchableOption option : orderedOptions) {
            NavigationTarget target = this.createNavigationTarget(option, optionStateStore);
            if (target != null) {
                targets.add(target);
            }
        }

        return targets;
    }

    private @Nullable NavigationTarget createNavigationTarget(SearchableOption option, OptionStateStore optionStateStore) {
        OptionUiState optionUiState = optionStateStore.optionUiState(option.id());
        OptionLayoutState optionLayoutState = optionStateStore.optionLayoutState(option.id());
        LayoutBounds bounds = optionLayoutState.bounds();
        LayoutBounds parentBounds = optionLayoutState.parentBounds();

        if (!optionUiState.isHighlighted() || parentBounds == null || bounds == null) {
            return null;
        }

        return new NavigationTarget(option.tabKey(), optionUiState, bounds, parentBounds);
    }

    private record SearchableOption(ResourceLocation id, String tabKey, String searchableText) {
    }

    record NavigationTarget(String tabKey, OptionUiState optionUiState, LayoutBounds bounds, LayoutBounds parentBounds) {
        int scrollOffset(int viewportHeight) {
            if (this.parentBounds.height() <= 0) {
                return 0;
            }

            int maxOffset = this.parentBounds.height() - viewportHeight;
            int input = this.bounds.y() - this.parentBounds.y();
            int inputOffset = input + this.bounds.height() == this.parentBounds.height() ? this.parentBounds.height() : input;

            return inputOffset * maxOffset / this.parentBounds.height();
        }
    }
}
