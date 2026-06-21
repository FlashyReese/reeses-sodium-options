package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

public interface OptionUndoButtonControl {
    OptionUndoButtonElement rso$getUndoButtonElement();

    boolean rso$isUndoButtonFocused();

    boolean rso$isUndoButtonHidden();

    void rso$holdUndoButtonLayout(boolean hideButton);

    void rso$releaseUndoButtonLayoutHold();

    void rso$focusUndoButton();

    void rso$clearUndoButtonFocus();
}
