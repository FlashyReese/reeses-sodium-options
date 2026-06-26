package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.minecraft.client.gui.screens.Screen;

final class OptionRowFactory {
    private final Screen screen;
    private final GuiTheme theme;
    private final OptionStateStore optionStateStore;

    OptionRowFactory(Screen screen, GuiTheme theme, OptionStateStore optionStateStore) {
        this.screen = screen;
        this.theme = theme;
        this.optionStateStore = optionStateStore;
    }

    AbstractOptionRow create(Option option, LayoutBounds dim) {
        AbstractOptionRow element = switch (option) {
            case BooleanOption booleanOption -> new BooleanOptionRow(dim, this.theme, this.optionStateStore, booleanOption);
            case IntegerOption integerOption -> new IntegerSliderOptionRow(dim, this.theme, this.optionStateStore, integerOption);
            case EnumOption<?> enumOption -> new EnumOptionRow<>(dim, this.theme, this.optionStateStore, enumOption);
            case ExternalButtonOption externalButtonOption -> new ExternalButtonOptionRow(this.screen, dim, this.theme, this.optionStateStore, externalButtonOption);
            default -> throw new IllegalArgumentException("Unsupported Sodium option type: " + option.getClass().getName());
        };

        this.registerOptionBounds(element, dim);

        return element;
    }

    void registerParentBounds(PageLayout layout, LayoutBounds parentDim) {
        for (PageLayout.Row row : layout.rows()) {
            if (row instanceof PageLayout.OptionRow optionRow && optionRow.option() instanceof OptionExtended optionExtended) {
                this.optionStateStore.optionLayoutState(optionExtended.rso$getId())
                        .setParentBounds(parentDim);
            }
        }
    }

    void registerOptionBounds(OptionRow element, LayoutBounds dim) {
        if (element.getOption() instanceof OptionExtended optionExtended) {
            this.optionStateStore.optionLayoutState(optionExtended.rso$getId())
                    .setBounds(dim);
        }
    }

}
