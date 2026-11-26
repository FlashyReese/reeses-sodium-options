package me.flashyreese.mods.reeses_sodium_options.client.controlify;

import dev.isxander.controlify.api.ControlifyEntrypoint;
import dev.isxander.controlify.api.ControlifyEvents;
import dev.isxander.controlify.api.screenop.ScreenopEvents;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.client.gui.search.SearchTextFieldWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.ScrollBarWidget;

public final class ReeseSodiumOptionsControlifyEntrypoint implements ControlifyEntrypoint {
    @Override
    public void onControlifyInitialize() {
        ScreenopEvents.REGISTER_SCREEN_BEHAVIOURS.subscribeForever(registry -> {
            registry.<SodiumVideoOptionsScreen>register(SodiumVideoOptionsScreen.class, RsoOptionsScreenBehaviour::new);
            registry.<SodiumVideoOptionsScreen>register(SodiumVideoOptionsScreen.class, 101, RsoSearchTextFieldBehaviour::retainKeyboardFocus);
        });
        ScreenopEvents.REGISTER_ELEMENT_BEHAVIOURS.subscribeForever(event ->
                event.registry().register(SearchTextFieldWidget.class, RsoSearchTextFieldBehaviour::new));
        ControlifyEvents.INITIALIZED.subscribeForever(controlify ->
                ScrollBarWidget.setControllerModeSupplier(() -> controlify.currentInputMode().isController()));
    }
}
