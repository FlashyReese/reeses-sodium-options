package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
import net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.ExternalButtonControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OptionRowFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger("Reese's Sodium Options");

    private final Screen screen;
    private final ColorTheme sodiumTheme;
    private final GuiTheme theme;
    private final OptionStateStore optionStateStore;

    OptionRowFactory(Screen screen, ColorTheme sodiumTheme, GuiTheme theme, OptionStateStore optionStateStore) {
        this.screen = screen;
        this.sodiumTheme = sodiumTheme;
        this.theme = theme;
        this.optionStateStore = optionStateStore;
    }

    OptionRow create(Option option, LayoutBounds dim) {
        OptionRow element = this.createOptionRow(option, dim);

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

    private OptionRow createOptionRow(Option option, LayoutBounds dim) {
        Control control = option.getControl();

        return switch (option) {
            case BooleanOption booleanOption when control instanceof TickBoxControl -> new BooleanOptionRow(dim, this.theme, this.optionStateStore, booleanOption);
            case IntegerOption integerOption when control instanceof SliderControl -> new IntegerSliderOptionRow(dim, this.theme, this.optionStateStore, integerOption);
            case EnumOption<?> enumOption when control instanceof CyclingControl<?> -> new EnumOptionRow<>(dim, this.theme, this.optionStateStore, enumOption);
            case ExternalButtonOption externalButtonOption when control instanceof ExternalButtonControl -> new ExternalButtonOptionRow(this.screen, dim, this.theme, this.optionStateStore, externalButtonOption);
            default -> this.createSodiumFallbackRow(option, dim);
        };
    }

    private OptionRow createSodiumFallbackRow(Option option, LayoutBounds dim) {
        try {
            return new SodiumControlElementOptionRow(this.screen, dim, this.sodiumTheme, option);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not create Sodium fallback row for option type {}; rendering it as unsupported", option.getClass().getName(), e);
            return new UnsupportedOptionRow(dim, this.theme, this.optionStateStore, option);
        }
    }
}
