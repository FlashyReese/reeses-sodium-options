package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.widgets.ResetButton")
public class MixinResetButton {
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void rso$disableResetOverlay(CallbackInfoReturnable<Boolean> cir) {
        if (!ReeseSodiumOptionsConfig.config().isResetButtonOverlay()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rso$disableResetClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!ReeseSodiumOptionsConfig.config().isResetButtonOverlay()) {
            cir.setReturnValue(false);
        }
    }
}
