package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.OptionPageScrollFrame;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class Tab<T extends AbstractFrame> implements TabOption<T> {
    private final Text title;
    private final T frame;

    public Tab(Text title, T frame) {
        this.title = title;
        this.frame = frame;
    }

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
        private Text title = null;
        private T frame = null;

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
            this.title = new LiteralText(page.getName());
            return new Tab<>(this.title, OptionPageScrollFrame.createBuilder().setDimension(dim).setOptionPage(page).build());
        }
    }
}