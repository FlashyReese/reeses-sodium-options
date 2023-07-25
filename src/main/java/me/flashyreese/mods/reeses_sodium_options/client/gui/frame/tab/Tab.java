package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.OptionPageFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.ScrollableFrame;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Tab<T extends AbstractFrame> {
    private final Text title;
    private final Function<Dim2i, T> frameFunction;

    public Tab(Text title, Function<Dim2i, T> frameFunction) {
        this.title = title;
        this.frameFunction = frameFunction;
    }

    public static Tab.Builder<?> createBuilder() {
        return new Tab.Builder<>();
    }

    public Text getTitle() {
        return title;
    }

    public Function<Dim2i, T> getFrameFunction() {
        return this.frameFunction;
    }

    public static class Builder<T extends AbstractFrame> {
        private Text title;
        private Function<Dim2i, T> frameFunction;

        public Builder<T> setTitle(Text title) {
            this.title = title;
            return this;
        }

        public Builder<T> setFrameFunction(Function<Dim2i, T> frameFunction) {
            this.frameFunction = frameFunction;
            return this;
        }

        public Tab<T> build() {
            return new Tab<T>(this.title, this.frameFunction);
        }

        public Tab<ScrollableFrame> from(OptionPage page, AtomicReference<Integer> verticalScrollBarOffset) {
            return new Tab<>(new LiteralText(page.getName()), dim2i -> ScrollableFrame
                    .createBuilder()
                    .setDimension(dim2i)
                    .setFrame(OptionPageFrame
                            .createBuilder()
                            .setDimension(new Dim2i(dim2i.getOriginX(), dim2i.getOriginY(), dim2i.getWidth(), dim2i.getHeight()))
                            .setOptionPage(page)
                            .build())
                    .setVerticalScrollBarOffset(verticalScrollBarOffset)
                    .build());
        }
    }
}