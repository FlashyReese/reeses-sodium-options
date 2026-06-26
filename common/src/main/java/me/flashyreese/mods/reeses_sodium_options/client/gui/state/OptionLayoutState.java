package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import org.jetbrains.annotations.Nullable;

public final class OptionLayoutState {
    private @Nullable LayoutBounds bounds;
    private @Nullable LayoutBounds parentBounds;

    public @Nullable LayoutBounds bounds() {
        return this.bounds;
    }

    public void setBounds(@Nullable LayoutBounds bounds) {
        this.bounds = bounds;
    }

    public @Nullable LayoutBounds parentBounds() {
        return this.parentBounds;
    }

    public void setParentBounds(@Nullable LayoutBounds parentBounds) {
        this.parentBounds = parentBounds;
    }
}
