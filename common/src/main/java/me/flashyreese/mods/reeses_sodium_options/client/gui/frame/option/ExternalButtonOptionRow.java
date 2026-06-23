package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

final class ExternalButtonOptionRow extends AbstractOptionRow {
    private static final int CONTENT_WIDTH = 65;
    private static final Component BASE_BUTTON_TEXT = Component.translatable("sodium.options.open_external_page_button");

    private final Screen screen;
    private final ExternalButtonOption option;

    ExternalButtonOptionRow(Screen screen, LayoutBounds dim, GuiTheme theme, OptionStateStore optionStateStore, ExternalButtonOption option) {
        super(dim, theme, optionStateStore, option);
        this.screen = screen;
        this.option = option;
    }

    @Override
    public ExternalButtonOption getOption() {
        return this.option;
    }

    @Override
    protected int controlContentWidth() {
        return CONTENT_WIDTH;
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        Component text = this.buttonText();
        int x = this.rightAlignedControlX(this.font.width(text));
        int y = this.centeredTextY();

        this.drawString(guiGraphics, text, x, y, 0xFFFFFFFF);

        if (this.option.isEnabled()) {
            this.requestPointerCursorIfHovered(guiGraphics);
        }
    }

    @Override
    protected boolean activateControl() {
        if (!this.option.isEnabled()) {
            return false;
        }

        this.option.getCurrentScreenConsumer().accept(this.screen);
        this.playClickSound();

        return true;
    }

    private Component buttonText() {
        if (!this.option.isEnabled()) {
            return BASE_BUTTON_TEXT.copy().withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.GRAY);
        }

        return Component.empty()
                .append(BASE_BUTTON_TEXT.copy().withStyle(ChatFormatting.UNDERLINE))
                .append(Component.literal(" >").withStyle(Style.EMPTY.withColor(this.theme.theme)));
    }
}
