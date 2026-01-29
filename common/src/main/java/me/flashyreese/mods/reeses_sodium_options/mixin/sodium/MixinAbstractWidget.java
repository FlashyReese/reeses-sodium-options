package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.AbstractWidgetExtended;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractWidget.class, remap = false)
public class MixinAbstractWidget implements AbstractWidgetExtended {

    @Mutable
    @Shadow
    @Final
    private Dim2i dim;

    @Mutable
    @Unique
    @Final
    private Dim2i dimBorder = null;

    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
    private void modifyIsMouseOver(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        Dim2i dim = this.dim;
        if ( getDimBorder() == null) {
            this.dimBorder = dim;
        }
        Dim2i border = this.getDimBorder();

        if (dim.getLimitX() <= border.x() || dim.getLimitY() <= border.y() || dim.x() >= border.getLimitX() || dim.y() >= border.getLimitY()) {
            cir.cancel();
            cir.setReturnValue(false);
            return;
        }

        double x = Math.max(dim.x(), border.x());
        double y = Math.max(dim.y(), border.y());
        double limitX = Math.min(dim.getLimitX(), border.getLimitX());
        double limitY = Math.min(dim.getLimitY(), border.getLimitY());

        cir.cancel();
        cir.setReturnValue(mouseX >= x && mouseX < limitX && mouseY >= y && mouseY < limitY);
    }

    @Override
    public Dim2i getDim() {
        return this.dim;
    }

    @Override
    public void setDim(Dim2i dim2i) {
        this.dim = dim2i;
    }

    @Override
    public Dim2i getDimBorder(){
        return this.dimBorder;
    }

    @Override
    public void setDimBorder(Dim2i dim2i) {
        this.dimBorder = dim2i;
    }
}
