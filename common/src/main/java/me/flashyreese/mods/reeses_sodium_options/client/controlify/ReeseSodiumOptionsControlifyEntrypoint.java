package me.flashyreese.mods.reeses_sodium_options.client.controlify;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.screenop.ComponentProcessorProvider;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.client.gui.search.SearchTextFieldWidget;

public final class ReeseSodiumOptionsControlifyEntrypoint implements ControlifyEntrypoint {
    @Override
    public void onControlifyPreInit(PreInitContext context) {
        ScreenProcessorProvider.registerProvider(SodiumVideoOptionsScreen.class, RsoOptionsScreenProcessor::new);
        ComponentProcessorProvider.REGISTRY.register(SearchTextFieldWidget.class, RsoSearchTextFieldProcessor::new);
    }

    @Override
    public void onControlifyInit(InitContext context) {
    }

    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {
    }
}
