package me.flashyreese.mods.reeses_sodium_options.client.gui;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes the screen to return to from whichever options screen is currently open (Reese's
 * Sodium Options' own screen or Sodium's, when RSO is disabled), so the screen can be reopened
 * and re-routed through {@code MixinSodiumOptionsGUI} when the "enabled" option is toggled.
 */
public interface PreviousScreenHolder {
    @Nullable Screen rso$previousScreen();
}
