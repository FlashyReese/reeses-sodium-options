package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

final class TabButtonWidget extends BaseWidget {
    private static final int TEXT_X = 10;
    private static final int SELECTION_WIDTH = 2;

    private final Tab<?> tab;
    private final GuiTheme theme;
    private final Runnable action;
    private boolean selected;

    TabButtonWidget(LayoutBounds dim, Tab<?> tab, boolean selected, Runnable action) {
        super(dim);
        this.tab = tab;
        this.selected = selected;
        this.action = action;
        this.theme = ReeseSodiumOptionsConfig.config().isColorThemes()
                ? GuiThemes.fromSodium(tab.getModOptions().theme())
                : GuiThemes.DEFAULT_BUTTON;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        this.hovered = this.isMouseOver(mouseX, mouseY);
        this.drawRect(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), this.backgroundColor());

        Component label = this.tab.getTitle();
        int textY = this.getCenterY() - this.font.lineHeight / 2;
        this.drawString(guiGraphics, label, this.getX() + TEXT_X, textY, this.textColor());

        if (this.selected) {
            this.drawRect(guiGraphics, this.getX(), this.getY(), this.getX() + SELECTION_WIDTH, this.getLimitY(), this.theme.theme);
        }

        if (this.shouldRenderFocusBorder()) {
            this.drawBorder(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), GuiThemes.OPTION_FOCUS_BORDER);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
            this.doAction();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.isFocused() && event.isSelection()) {
            this.doAction();
            return true;
        }

        return false;
    }

    private void doAction() {
        this.action.run();
        this.playClickSound();
    }

    private int backgroundColor() {
        return this.hovered ? this.theme.bgHighlight : this.theme.bgDefault;
    }

    private int textColor() {
        if (!ReeseSodiumOptionsConfig.config().isColorThemes()) {
            return this.theme.themeLighter;
        }

        if (this.selected) {
            return this.theme.themeLighter;
        }

        return this.hovered ? this.theme.theme : this.theme.themeDarker;
    }
}
