package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Set;

public interface OptionStateStore {
    OptionUiState optionUiState(Identifier id);

    OptionLayoutState optionLayoutState(Identifier id);

    List<Identifier> searchResultIds();

    Set<Identifier> collapsedOptionGroups();
}
