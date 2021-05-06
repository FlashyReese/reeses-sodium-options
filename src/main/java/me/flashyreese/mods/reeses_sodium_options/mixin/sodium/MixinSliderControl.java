package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.SliderControlElement;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SliderControl.class)
public class MixinSliderControl {
    @Shadow
    @Final
    private Option<Integer> option;

    @Shadow
    @Final
    private int max;

    @Shadow
    @Final
    private int min;

    @Shadow
    @Final
    private int interval;

    @Shadow
    @Final
    private ControlValueFormatter mode;

    @Inject(method = "createElement", at = @At(value = "RETURN"), cancellable = true, remap = false)
    public void createElement(Dim2i dim, CallbackInfoReturnable<ControlElement<Integer>> cir) {
        cir.setReturnValue(new SliderControlElement(this.option, dim, this.min, this.max, this.interval, this.mode));
    }
}
