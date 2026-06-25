package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuide;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import net.caffeinemc.mods.sodium.client.config.structure.EnumOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

final class EnumOptionRow<E extends Enum<E>> extends AbstractOptionRow {
    private static final int MAX_CONTENT_WIDTH = 70;

    private final EnumOption<E> option;

    EnumOptionRow(LayoutBounds dim, GuiTheme theme, OptionStateStore optionStateStore, EnumOption<E> option) {
        super(dim, theme, optionStateStore, option);
        this.option = option;
    }

    @Override
    public EnumOption<E> getOption() {
        return this.option;
    }

    @Override
    protected int controlContentWidth() {
        return Math.min(MAX_CONTENT_WIDTH, this.font.width(this.displayValue()));
    }

    public List<ControlGuide> controlGuides() {
        return this.canShowControlGuide() ? List.of(ControlGuide.press("Next Value")) : List.of();
    }

    @Override
    protected void renderControl(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!this.option.showControl()) {
            return;
        }

        Component value = this.displayValue();
        int valueWidth = this.font.width(value);
        int x = this.rightAlignedControlX(valueWidth);
        int y = this.centeredTextY();

        this.drawString(guiGraphics, value, x, y, 0xFFFFFFFF);

        if (this.option.isEnabled()) {
            this.requestPointerCursorIfHovered(guiGraphics);
        }
    }

    @Override
    protected boolean controlMouseClicked(double mouseX, double mouseY, int button) {
        boolean reverse = Screen.hasShiftDown();
        if (button == 1) {
            if (!ReeseSodiumOptionsConfig.config().isReverseCyclingControls()) {
                return false;
            }

            reverse = true;
        } else if (button != 0) {
            return false;
        }

        if (!this.option.isEnabled()
                || !this.option.showControl()
                || !this.isMouseOverRow(mouseX, mouseY)) {
            return false;
        }

        this.cycleControl(reverse);

        return true;
    }

    @Override
    protected boolean controlKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isRowFocused() || !isSelectionKey(keyCode)) {
            return false;
        }

        this.cycleControl(Screen.hasShiftDown());

        return true;
    }

    @Override
    protected boolean activateControl() {
        this.cycleControl(Screen.hasShiftDown());

        return true;
    }

    private Component displayValue() {
        Component value = this.option.getElementName(this.option.getValidatedValue());

        return this.option.isEnabled() ? value : this.formatDisabledControlValue(value);
    }

    @Override
    protected Component narrationValue() {
        return this.option.showControl() ? this.option.getElementName(this.option.getValidatedValue()) : null;
    }

    @Override
    protected void updateControlNarration(NarrationElementOutput builder) {
        if (!this.option.isEnabled()) {
            builder.add(NarratedElementType.HINT, Component.translatable("rso.narration.option_unavailable"));
            return;
        }

        if (!this.option.showControl()) {
            return;
        }

        Component nextValue = this.option.getElementName(this.nextValue(false));
        if (this.isFocused()) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.focused", nextValue));
        } else if (this.isHovered()) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.cycle_button.usage.hovered", nextValue));
        }
    }

    private void cycleControl(boolean reverse) {
        E nextValue = this.nextValue(reverse);
        if (nextValue == this.option.getValidatedValue()) {
            return;
        }

        this.option.modifyValue(nextValue);
        this.playClickSound();
    }

    private E nextValue(boolean reverse) {
        E[] values = this.option.getEnumClass().getEnumConstants();
        E currentValue = this.option.getValidatedValue();
        int valueIndex = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentValue) {
                valueIndex = i;
                break;
            }
        }

        for (int i = 0; i < values.length; i++) {
            valueIndex = reverse
                    ? (valueIndex + values.length - 1) % values.length
                    : (valueIndex + 1) % values.length;
            E nextValue = values[valueIndex];

            if (this.option.isValueAllowed(nextValue)) {
                return nextValue;
            }
        }

        return currentValue;
    }
}
