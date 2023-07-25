package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(OptionImpl.class)
public class MixinOptionImpl implements OptionExtended {
    @Unique
    private Dim2i parent;
    @Unique
    private Dim2i dim2i;
    @Unique
    private boolean highlight;
    @Unique
    private boolean selected;

    @Override
    public boolean isHighlight() {
        return this.highlight;
    }

    @Override
    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    @Override
    public Dim2i getDim2i() {
        return this.dim2i;
    }

    @Override
    public void setDim2i(Dim2i dim2i) {
        this.dim2i = dim2i;
    }

    @Override
    public Dim2i getParentDimension() {
        return this.parent;
    }

    @Override
    public void setParentDimension(Dim2i parent) {
        this.parent = parent;
    }

    @Override
    public boolean getSelected() {
        return this.selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
