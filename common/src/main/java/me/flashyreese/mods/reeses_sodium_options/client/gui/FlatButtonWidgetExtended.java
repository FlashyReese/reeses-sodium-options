package me.flashyreese.mods.reeses_sodium_options.client.gui;

import net.caffeinemc.mods.sodium.client.util.Dim2i;

public interface FlatButtonWidgetExtended {
    boolean isLeftAlign();

    void setLeftAlign(boolean leftAligned);

    boolean isTab();

    void setTab(boolean tab);

    Dim2i getDimBorder();

    void setDimBorder(Dim2i dim);
}
