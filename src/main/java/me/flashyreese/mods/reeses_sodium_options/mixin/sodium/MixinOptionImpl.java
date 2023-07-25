package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(OptionImpl.class)
public class MixinOptionImpl implements OptionExtended {
    private Dim2i dim2i;
    private boolean highlight;

    @Override
    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    @Override
    public boolean isHighlight() {
        return this.highlight;
    }

    @Override
    public void setDim2i(Dim2i dim2i) {
        this.dim2i = dim2i;
    }

    @Override
    public Dim2i getDim2i() {
        return this.dim2i;
    }
}
