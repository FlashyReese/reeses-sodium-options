package me.flashyreese.mods.reeses_sodium_options.client.gui;

import net.caffeinemc.mods.sodium.client.config.structure.Config;
import org.jetbrains.annotations.Nullable;

public interface OptionStateProvider {
    @Nullable Config rso$getParentConfig();
}
