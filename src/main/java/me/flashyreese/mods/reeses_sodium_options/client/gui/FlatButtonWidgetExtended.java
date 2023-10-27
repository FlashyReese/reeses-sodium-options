package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.jellysquid.mods.sodium.client.util.Dim2i;

public interface FlatButtonWidgetExtended {
    boolean isLeftAligned();

    void setLeftAligned(boolean leftAligned);

    Dim2i getDimensions();
}
