package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.PreviousScreenHolder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VideoSettingsScreen.class)
public abstract class MixinSodiumOptionsGUI extends Screen implements PreviousScreenHolder {

    @Shadow
    @Final
    private Screen prevScreen;

    protected MixinSodiumOptionsGUI(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void postInit(CallbackInfo ci) {
        if (!ReeseSodiumOptionsConfig.config().isEnabled()) {
            return;
        }

        this.minecraft.gui.setScreen(new SodiumVideoOptionsScreen(this.prevScreen));
    }

    @Override
    public Screen rso$previousScreen() {
        return this.prevScreen;
    }
}
