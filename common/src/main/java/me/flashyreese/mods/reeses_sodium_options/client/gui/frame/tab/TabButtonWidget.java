package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isMouseOver(mouseX, mouseY)) {
            this.doAction();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isFocused() && isSelectionKey(keyCode)) {
            this.doAction();
            return true;
        }

        return false;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", this.tab.getTitle()));

        if (!this.selected) {
            this.addButtonUsageNarration(builder);
        }
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
