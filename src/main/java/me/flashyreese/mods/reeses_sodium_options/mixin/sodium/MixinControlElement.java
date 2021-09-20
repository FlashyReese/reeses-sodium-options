package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.jellysquid.mods.sodium.gui.options.ControlElement;
import me.jellysquid.mods.sodium.gui.values.Dim2i;
import me.jellysquid.mods.sodium.gui.widgets.AbstractWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ControlElement.class)
public abstract class MixinControlElement extends AbstractWidget {

    @Shadow
    @Final
    protected Dim2i dim;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/gui/values/Dim2i;containsCursor(DD)Z"))
    public boolean render(Dim2i dim2i, double x, double y) {
        return this.isMouseOver(x, y);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }
}
