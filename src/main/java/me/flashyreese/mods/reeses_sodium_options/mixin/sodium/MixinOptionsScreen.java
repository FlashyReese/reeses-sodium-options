package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumIrisVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OptionsScreen.class, priority = 999)
public class MixinOptionsScreen extends Screen {
    protected MixinOptionsScreen(Text title) {
        super(title);
    }

    @Dynamic
    @Inject(method = "method_19828(Lnet/minecraft/client/gui/widget/ButtonWidget;)V", at = @At("HEAD"), cancellable = true)
    private void open(ButtonWidget widget, CallbackInfo ci) {
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            this.client.openScreen(new SodiumIrisVideoOptionsScreen(this));
        } else {
            this.client.openScreen(new SodiumVideoOptionsScreen(this));
        }

        ci.cancel();
    }
}
