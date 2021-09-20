package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.OptionPageScrollFrame;
import me.jellysquid.mods.sodium.config.render.OptionPage;
import me.jellysquid.mods.sodium.gui.values.Dim2i;
import net.minecraft.text.Text;

public record Tab<T extends AbstractFrame>(Text text, T frame) implements TabOption<T> {

    public Text getText() {
        return text;
    }

    @Override
    public T getFrame() {
        return this.frame;
    }

    public static class Builder<T extends AbstractFrame> {
        private Text text = null;
        private T frame = null;

        public Builder<T> setText(Text text) {
            this.text = text;
            return this;
        }

        public Builder<T> setFrame(T frame) {
            this.frame = frame;
            return this;
        }

        public Tab<T> build() {
            return new Tab<T>(this.text, this.frame);
        }

        public Tab<OptionPageScrollFrame> from(OptionPage page, Dim2i dim) {
            this.text = page.getName();
            return new Tab<>(this.text, OptionPageScrollFrame.createBuilder().setDimension(dim).setOptionPage(page).build());
        }
    }
}