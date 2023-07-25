package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.jellysquid.mods.sodium.client.util.Dim2i;

public interface OptionExtended {
    void setHighlight(boolean highlight);

    boolean isHighlight();

    void setDim2i(Dim2i dim2i);

    Dim2i getDim2i();

    void setParentDimension(Dim2i dim2i);

    Dim2i getParentDimension();

    void setSelected(boolean selected);

    boolean getSelected();
}
