package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OptionsScreenUiState implements OptionStateStore {
    private final Holder<Component> tabFrameSelectedTab = new Holder<>(null);
    private final Holder<String> tabFrameSelectedGroup = new Holder<>(null);
    private final Holder<Integer> tabFrameScrollBarOffset = new Holder<>(0);
    private final Holder<Boolean> scrollSelectedTabIntoView = new Holder<>(false);
    private final Holder<Integer> optionPageScrollBarOffset = new Holder<>(0);
    private final Holder<String> lastSearch = new Holder<>("");
    private final Holder<Integer> lastSearchIndex = new Holder<>(0);
    private final List<Identifier> searchResultIds = new ArrayList<>();
    private final Set<String> manuallyCollapsedTabGroups = new HashSet<>();
    private final Set<Identifier> collapsedOptionGroups = new HashSet<>();
    private final Map<String, Identifier> focusedOptionIdsByTab = new HashMap<>();
    private final Map<Identifier, OptionUiState> optionUiStates = new HashMap<>();
    private final Map<Identifier, OptionLayoutState> optionLayoutStates = new HashMap<>();

    public Holder<Component> tabFrameSelectedTab() {
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
    public Set<Identifier> collapsedOptionGroups() {
        return collapsedOptionGroups;
    }

    public Map<String, Identifier> focusedOptionIdsByTab() {
        return focusedOptionIdsByTab;
    }

    public Holder<String> lastSearch() {
        return lastSearch;
    }

    public Holder<Integer> lastSearchIndex() {
        return lastSearchIndex;
    }

    @Override
    public List<Identifier> searchResultIds() {
        return List.copyOf(searchResultIds);
    }

    @Override
    public OptionUiState optionUiState(Identifier id) {
        return this.optionUiStates.computeIfAbsent(id, unused -> new OptionUiState());
    }

    @Override
    public OptionLayoutState optionLayoutState(Identifier id) {
        return this.optionLayoutStates.computeIfAbsent(id, unused -> new OptionLayoutState());
    }

    public void setHighlightedOptions(List<Identifier> ids) {
        this.optionUiStates.values().forEach(OptionUiState::clearHighlight);
        ids.forEach(id -> this.optionUiState(id).setHighlighted(true));
    }

    public void clearSelectedOptions() {
        this.optionUiStates.values().forEach(state -> state.setSelected(false));
    }

    public void clearOptionUiStates() {
        this.optionUiStates.clear();
        this.optionLayoutStates.clear();
    }

    public boolean updateSearchResults(List<Identifier> ids) {
        if (this.searchResultIds.equals(ids)) {
            return false;
        }

        this.searchResultIds.clear();
        this.searchResultIds.addAll(ids);

        return true;
    }
}
