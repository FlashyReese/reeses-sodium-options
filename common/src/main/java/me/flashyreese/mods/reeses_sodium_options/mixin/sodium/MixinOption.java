package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionStateProvider;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Option.class)
public class MixinOption implements OptionExtended, OptionStateProvider {
    @Shadow
    @Final
    Identifier id;

    @Shadow
    Config state;

    @Override
    public Identifier rso$getId() {
        return id;
    }

    @Override
    public @Nullable Config rso$getParentConfig() {
        return this.state;
    }
}
