package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.FlatButtonWidgetExtended;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FlatButtonWidget.class)
public abstract class MixinFlatButtonWidget extends AbstractWidget implements FlatButtonWidgetExtended {

    @Shadow
    @Final
    private Dim2i dim;

    @Unique
    private boolean leftAligned;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gui/widgets/FlatButtonWidget;drawString(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;III)V"))
    public void redirectDrawString(FlatButtonWidget instance, MatrixStack matrixStack, Text text, int x, int y, int color) {
        if (this.leftAligned) {
            this.drawString(matrixStack, text, this.dim.x() + 10, y, color);
        } else {
            this.drawString(matrixStack, text, x, y, color);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gui/widgets/FlatButtonWidget;drawRect(DDDDI)V", ordinal = 1))
    public void redirectDrawRect(FlatButtonWidget instance, double x1, double y1, double x2, double y2, int color) {
        if (this.leftAligned) {
            this.drawRect(x1, this.dim.y(), x1 + 1, y2, color);
        } else {
            this.drawRect(x1, y1, x2, y2, color);
        }
    }

    @Override
    public boolean isLeftAligned() {
        return this.leftAligned;
    }

    @Override
    public void setLeftAligned(boolean leftAligned) {
        this.leftAligned = leftAligned;
    }
}
