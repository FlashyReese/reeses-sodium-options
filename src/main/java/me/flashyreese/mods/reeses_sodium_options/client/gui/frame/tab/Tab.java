package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.OptionPageScrollFrame;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.text.Text;

public record Tab<T extends AbstractFrame>(Text title, T frame) implements TabOption<T> {

    public static Tab.Builder<?> createBuilder() {
        return new Tab.Builder<>();
    }

    public Text getTitle() {
        return title;
    }

    @Override
    public T getFrame() {
        return this.frame;
    }

    public static class Builder<T extends AbstractFrame> {
        private Text title;
        private T frame;

        public Builder<T> setTitle(Text title) {
            this.title = title;
            return this;
        }

        public Builder<T> setFrame(T frame) {
            this.frame = frame;
            return this;
        }

        public Tab<T> build() {
            return new Tab<T>(this.title, this.frame);
        }

        public Tab<OptionPageScrollFrame> from(OptionPage page, Dim2i dim) {
            return new Tab<>(page.getName(), OptionPageScrollFrame.createBuilder().setDimension(dim).shouldRenderOutline(false).setOptionPage(page).build());
        }
    }
}