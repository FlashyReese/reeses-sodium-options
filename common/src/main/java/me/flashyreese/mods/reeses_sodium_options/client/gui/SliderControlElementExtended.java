package me.flashyreese.mods.reeses_sodium_options.client.gui;

import net.caffeinemc.mods.sodium.client.util.Dim2i;

public interface SliderControlElementExtended {
    boolean isEditMode();

    void setEditMode(boolean editMode);

    Dim2i getDimBorder();

    void setDimBorder(Dim2i dim2i);
}
