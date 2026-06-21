package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

public interface OptionUndoButtonControl {
    OptionUndoButtonElement rso$getUndoButtonElement();

    boolean rso$isUndoButtonFocused();

    void rso$focusUndoButton();

    void rso$clearUndoButtonFocus();
}
