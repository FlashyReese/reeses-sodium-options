package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.Holder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.PageFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.ScrollableFrame;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class Tab<T extends AbstractFrame> {
    private final ModOptions modOptions;
    private final Component title;
    private final Page page;
    private final Function<LayoutBounds, T> frameFunction;

    public Tab(ModOptions modOptions, Component title, Page page, Function<LayoutBounds, T> frameFunction) {
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

    public Function<LayoutBounds, T> getFrameFunction() {
        return this.frameFunction;
    }

    public static class Builder<T extends AbstractFrame> {
        private ModOptions modOptions;
        private Component title;
        private Page page;
        private Function<LayoutBounds, T> frameFunction;

        public Builder<T> withTitle(Component title) {
            this.title = title;
            return this;
        }

        public Builder<T> withFrameFunction(Function<LayoutBounds, T> frameFunction) {
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

        public Tab<ScrollableFrame> from(Screen screen, ModOptions modOptions, Page page, Holder<Integer> verticalScrollBarOffset, OptionStateStore optionStateStore) {
            return new Tab<>(modOptions, page.name(), page, bounds -> {
                PageFrame pageFrame = PageFrame
                        .builder()
                        .withDimension(bounds)
                        .withModOptions(modOptions)
                        .withPage(page)
                        .withScreen(screen)
                        .withOptionStateStore(optionStateStore)
                        .build();
                ScrollableFrame scrollableFrame = ScrollableFrame
                        .builder()
                        .withDimension(bounds)
                        .withModOptions(modOptions)
                        .withScreen(screen)
                        .withFrame(pageFrame)
                        .withVerticalScrollBarOffset(verticalScrollBarOffset)
                        .build();
                pageFrame.setGroupToggleRebuildHandler(collapseKey -> {
                    scrollableFrame.rebuildContentFrame();
                    if (pageFrame.focusGroupHeader(collapseKey)) {
                        scrollableFrame.setFocused(pageFrame);
                    }
                });
                return scrollableFrame;
            });
        }
    }
}
