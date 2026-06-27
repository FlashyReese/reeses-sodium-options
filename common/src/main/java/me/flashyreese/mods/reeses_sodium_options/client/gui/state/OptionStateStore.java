package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public interface OptionStateStore {
    OptionUiState optionUiState(ResourceLocation id);

    OptionLayoutState optionLayoutState(ResourceLocation id);

    boolean searchActive();

    List<SearchResultEntry> searchResults();

    Set<ResourceLocation> collapsedOptionGroups();
}
