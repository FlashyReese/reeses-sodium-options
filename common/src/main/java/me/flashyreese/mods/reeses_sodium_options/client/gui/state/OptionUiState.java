package me.flashyreese.mods.reeses_sodium_options.client.gui.state;

public final class OptionUiState {
    private boolean highlighted;
    private boolean selected;

    public boolean isHighlighted() {
        return this.highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void clearHighlight() {
        this.highlighted = false;
    }
}
