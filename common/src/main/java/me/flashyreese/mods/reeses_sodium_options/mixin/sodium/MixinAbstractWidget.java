package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.AbstractWidgetExtended;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractWidget.class, remap = false)
public class MixinAbstractWidget implements AbstractWidgetExtended {

    @Mutable
    @Shadow
    @Final
    private Dim2i dim;

    @Override
    public Dim2i getDim() {
        return this.dim;
    }

    @Override
    public void setDim(Dim2i dim2i) {
        this.dim = dim2i;
    }
}
