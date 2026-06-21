package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

public interface OptionUndoButtonControl {
    OptionResetButtonElement rso$getResetButtonElement();

    OptionUndoButtonElement rso$getUndoButtonElement();

    boolean rso$isResetButtonFocused();

    boolean rso$isUndoButtonFocused();

    boolean rso$isResetButtonVisible();

    boolean rso$isUndoButtonVisible();

    boolean rso$isActionButtonLayoutHeld();

    void rso$holdUndoButtonLayout(boolean hideButton);

    void rso$releaseUndoButtonLayoutHold();

    void rso$focusResetButton();

    void rso$focusUndoButton();

    void rso$clearUndoButtonFocus();
}
