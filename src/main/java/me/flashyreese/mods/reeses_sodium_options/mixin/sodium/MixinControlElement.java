package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlElement.class)
public abstract class MixinControlElement<T> extends AbstractWidget {

    @Shadow
    @Final
    protected Dim2i dim;

    @Shadow @Final protected Option<T> option;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void postInit(Option<T> option, Dim2i dim, CallbackInfo ci) {
        if (this.option instanceof OptionExtended optionExtended) {
            optionExtended.setDim2i(this.dim);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gui/options/control/ControlElement;drawRect(Lnet/minecraft/client/gui/DrawContext;IIIII)V"))
    public void redirectText(ControlElement<T> instance, DrawContext drawContext, int x1, int y1, int x2, int y2, int color) {
        this.drawRect(drawContext, x1, y1, x2, y2, color);
        if (this.option instanceof OptionExtended optionExtended && optionExtended.isHighlight()) {
            this.drawBorder(drawContext, x1, y1, x2, y2, optionExtended.getSelected() ? 0xFFFFAA00 : 0xFF55FFFF);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/util/Dim2i;containsCursor(DD)Z"))
    public boolean render(Dim2i dim2i, double x, double y) {
        return this.isMouseOver(x, y);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }
}
