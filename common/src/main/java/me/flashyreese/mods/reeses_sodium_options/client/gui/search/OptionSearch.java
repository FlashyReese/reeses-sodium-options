package me.flashyreese.mods.reeses_sodium_options.client.gui.search;

import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionLayoutState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionUiState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultEntry;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrder;
import me.flashyreese.mods.reeses_sodium_options.client.search.SearchIndex;
import me.flashyreese.mods.reeses_sodium_options.client.search.SearchResult;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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
                                option,
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
                .minScore(0.3)
                .rerankWithEditDistance(true)
                .rerankLimit(50)
                .rerankWeight(0.1)
                .build();
    }

    List<SearchResultEntry> query(String query) {
        return this.searchIndex.newSession(query)
                .results()
                .stream()
                .map(SearchResult::item)
                .map(SearchableOption::toSearchResult)
                .toList();
    }

    List<NavigationTarget> navigationTargets(OptionStateStore optionStateStore, SearchResultOrder order) {
        List<SearchResultEntry> orderedResults = this.orderResults(optionStateStore.searchResults(), order);

        List<NavigationTarget> targets = new ArrayList<>(orderedResults.size());
        for (SearchResultEntry result : orderedResults) {
            NavigationTarget target = this.createNavigationTarget(result, optionStateStore);
            if (target != null) {
                targets.add(target);
            }
        }

        return targets;
    }

    private List<SearchResultEntry> orderResults(List<SearchResultEntry> results, SearchResultOrder order) {
        if (results.isEmpty()) {
            return List.of();
        }

        if (order == SearchResultOrder.RANKED) {
            return results;
        }

        Set<Option> resultOptions = Collections.newSetFromMap(new IdentityHashMap<>());
        results.forEach(result -> resultOptions.add(result.option()));

        List<SearchResultEntry> ordered = new ArrayList<>(results.size());
        for (SearchableOption option : this.options) {
            if (resultOptions.contains(option.option())) {
                ordered.add(option.toSearchResult());
            }
        }

        return ordered;
    }

    private @Nullable NavigationTarget createNavigationTarget(SearchResultEntry result, OptionStateStore optionStateStore) {
        OptionUiState optionUiState = optionStateStore.optionUiState(result.optionId());
        OptionLayoutState optionLayoutState = optionStateStore.optionLayoutState(result.optionId());
        LayoutBounds bounds = optionLayoutState.bounds();
        LayoutBounds parentBounds = optionLayoutState.parentBounds();

        if (!optionUiState.isHighlighted() || parentBounds == null || bounds == null) {
            return null;
        }

        return new NavigationTarget(result.tabKey(), optionUiState, bounds, parentBounds);
    }

    private record SearchableOption(ResourceLocation id, String tabKey, Option option, String searchableText) {
        SearchResultEntry toSearchResult() {
            return new SearchResultEntry(this.tabKey, this.id, this.option);
        }
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
