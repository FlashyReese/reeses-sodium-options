package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.minecraft.resources.ResourceLocation;

public record SearchResultEntry(String tabKey, ResourceLocation optionId, Option option) {
}
