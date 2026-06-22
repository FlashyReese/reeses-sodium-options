package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.PageFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.ScrollableFrame;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Tab<T extends AbstractFrame> {
    private final ModOptions modOptions;
    private final Component title;
    private final Page page;
    private final Function<Dim2i, T> frameFunction;

    public Tab(ModOptions modOptions, Component title, Page page, Function<Dim2i, T> frameFunction) {
        this.modOptions = modOptions;
        this.title = title;
        this.page = page;
        this.frameFunction = frameFunction;
    }

    public static Tab.Builder<?> builder() {
        return new Tab.Builder<>();
    }

    public ModOptions getModOptions() {
        return modOptions;
    }

    public Component getTitle() {
        return title;
    }

    public Page getPage() {
        return page;
    }

    public Function<Dim2i, T> getFrameFunction() {
        return this.frameFunction;
    }

    public static class Builder<T extends AbstractFrame> {
        private ModOptions modOptions;
        private Component title;
        private Page page;
        private Function<Dim2i, T> frameFunction;

        public Builder<T> withTitle(Component title) {
            this.title = title;
            return this;
        }

        public Builder<T> withFrameFunction(Function<Dim2i, T> frameFunction) {
            this.frameFunction = frameFunction;
            return this;
        }

        public Builder<T> withPage(Page page) {
            this.page = page;
            return this;
        }

        public Builder<T> withModOptions(ModOptions modOptions) {
            this.modOptions = modOptions;
            return this;
        }


        public Tab<T> build() {
            return new Tab<T>(this.modOptions, this.title, this.page, this.frameFunction);
        }

        public Tab<ScrollableFrame> from(Screen screen, ModOptions modOptions, Page page, AtomicReference<Integer> verticalScrollBarOffset) {
            return new Tab<>(modOptions, page.name(), page, dim2i -> ScrollableFrame
                    .builder()
                    .withDimension(dim2i)
                    .withModOptions(modOptions)
                    .withScreen(screen)
                    .withFrame(PageFrame
                            .builder()
                            .withDimension(new Dim2i(dim2i.x(), dim2i.y(), dim2i.width(), dim2i.height()))
                            .withModOptions(modOptions)
                            .withPage(page)
                            .withScreen(screen)
                            .build())
                    .withVerticalScrollBarOffset(verticalScrollBarOffset)
                    .build());
        }
    }
}