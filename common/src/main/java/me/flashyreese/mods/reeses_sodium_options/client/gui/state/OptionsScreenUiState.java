package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OptionsScreenUiState implements OptionStateStore {
    private final Holder<String> tabFrameSelectedTab = new Holder<>(null);
    private final Holder<String> tabFrameSelectedGroup = new Holder<>(null);
    private final Holder<Integer> tabFrameScrollBarOffset = new Holder<>(0);
    private final Holder<Boolean> scrollSelectedTabIntoView = new Holder<>(false);
    private final Holder<Integer> optionPageScrollBarOffset = new Holder<>(0);
    private final Holder<String> lastSearch = new Holder<>("");
    private final Holder<Integer> lastSearchIndex = new Holder<>(null);
    private final List<SearchResultEntry> searchResults = new ArrayList<>();
    private boolean searchActive;
    private final Set<String> manuallyCollapsedTabGroups = new HashSet<>();
    private final Set<ResourceLocation> collapsedOptionGroups = new HashSet<>();
    private final Map<String, ResourceLocation> focusedOptionIdsByTab = new HashMap<>();
    private final Map<ResourceLocation, OptionUiState> optionUiStates = new HashMap<>();
    private final Map<ResourceLocation, OptionLayoutState> optionLayoutStates = new HashMap<>();

    public Holder<String> tabFrameSelectedTab() {
        return tabFrameSelectedTab;
    }

    public Holder<String> tabFrameSelectedGroup() {
        return tabFrameSelectedGroup;
    }

    public Holder<Integer> tabFrameScrollBarOffset() {
        return tabFrameScrollBarOffset;
    }

    public Holder<Boolean> scrollSelectedTabIntoView() {
        return scrollSelectedTabIntoView;
    }

    public Holder<Integer> optionPageScrollBarOffset() {
        return optionPageScrollBarOffset;
    }

    public Set<String> manuallyCollapsedTabGroups() {
        return manuallyCollapsedTabGroups;
    }

    @Override
    public Set<ResourceLocation> collapsedOptionGroups() {
        return collapsedOptionGroups;
    }

    public Map<String, ResourceLocation> focusedOptionIdsByTab() {
        return focusedOptionIdsByTab;
    }

    public Holder<String> lastSearch() {
        return lastSearch;
    }

    public Holder<Integer> lastSearchIndex() {
        return lastSearchIndex;
    }

    @Override
    public boolean searchActive() {
        return this.searchActive;
    }

    @Override
    public List<SearchResultEntry> searchResults() {
        return List.copyOf(searchResults);
    }

    @Override
    public OptionUiState optionUiState(ResourceLocation id) {
        return this.optionUiStates.computeIfAbsent(id, unused -> new OptionUiState());
    }

    @Override
    public OptionLayoutState optionLayoutState(ResourceLocation id) {
        return this.optionLayoutStates.computeIfAbsent(id, unused -> new OptionLayoutState());
    }

    public void setHighlightedOptions(List<SearchResultEntry> results) {
        this.optionUiStates.values().forEach(OptionUiState::clearHighlight);
        results.forEach(result -> this.optionUiState(result.optionId()).setHighlighted(true));
    }

    public void clearSelectedOptions() {
        this.optionUiStates.values().forEach(state -> state.setSelected(false));
    }

    public void clearOptionUiStates() {
        this.optionUiStates.clear();
        this.optionLayoutStates.clear();
    }

    public boolean updateSearchResults(boolean active, List<SearchResultEntry> results) {
        if (this.searchActive == active && this.searchResults.equals(results)) {
            return false;
        }

        this.searchActive = active;
        this.searchResults.clear();
        this.searchResults.addAll(results);

        return true;
    }
}
