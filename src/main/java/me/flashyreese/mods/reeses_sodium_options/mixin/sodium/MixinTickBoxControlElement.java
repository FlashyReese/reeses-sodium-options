package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl$TickBoxControlElement")
public abstract class MixinTickBoxControlElement extends ControlElement<Boolean> {
    public MixinTickBoxControlElement(Option<Boolean> option, Dim2i dim) {
        super(option, dim);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Rect2i;getX()I"))
    public int rso$renderSliderBoundsGetX(Rect2i instance) {
        return this.dim.getLimitX() - 16;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Rect2i;getY()I"))
    public int rso$renderSliderBoundsGetY(Rect2i instance) {
        return this.dim.getCenterY() - 5;
    }
}
