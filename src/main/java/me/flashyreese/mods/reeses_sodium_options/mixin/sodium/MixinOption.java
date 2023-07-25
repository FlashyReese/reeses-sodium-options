package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Option.class)
public interface MixinOption extends OptionExtended {
}
