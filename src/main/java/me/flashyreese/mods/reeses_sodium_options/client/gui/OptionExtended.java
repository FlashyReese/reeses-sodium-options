package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.jellysquid.mods.sodium.client.util.Dim2i;

public interface OptionExtended {
    boolean isHighlight();

    void setHighlight(boolean highlight);

    Dim2i getDim2i();

    void setDim2i(Dim2i dim2i);

    Dim2i getParentDimension();

    void setParentDimension(Dim2i dim2i);

    boolean getSelected();

    void setSelected(boolean selected);
}
