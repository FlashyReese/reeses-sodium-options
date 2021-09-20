package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.SliderControlElement;
import me.jellysquid.mods.sodium.config.render.Option;
import me.jellysquid.mods.sodium.gui.options.ControlElement;
import me.jellysquid.mods.sodium.gui.options.ControlValueFormatter;
import me.jellysquid.mods.sodium.gui.options.SliderControl;
import me.jellysquid.mods.sodium.gui.values.Dim2i;
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
