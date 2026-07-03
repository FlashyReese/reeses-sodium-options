package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.minecraft.resources.Identifier;

public record SearchResultEntry(String tabKey, Identifier optionId, Option option) {
}
