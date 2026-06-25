package me.flashyreese.mods.reeses_sodium_options.client.gui.widget;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlatButtonWidget extends BaseWidget {
    public static final GuiTheme DEFAULT_THEME = GuiThemes.DEFAULT_BUTTON;

    private final Component label;
    private final Runnable action;
    private final boolean drawBackground;
    private final boolean drawFrame;
    private final boolean leftAlign;
    private final GuiTheme theme;
    private boolean selected;
    private boolean enabled = true;
    private boolean visible = true;

    public FlatButtonWidget(LayoutBounds dim, Component label, Runnable action, boolean drawBackground, boolean leftAlign) {
        this(dim, label, action, drawBackground, !drawBackground, leftAlign, DEFAULT_THEME);
    }

    public FlatButtonWidget(LayoutBounds dim, Component label, Runnable action, boolean drawBackground, boolean drawFrame, boolean leftAlign, GuiTheme theme) {
        super(dim);
        this.label = label;
        this.action = action;
        this.drawBackground = drawBackground;
        this.drawFrame = drawFrame;
        this.leftAlign = leftAlign;
        this.theme = theme;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        this.hovered = this.isMouseOver(mouseX, mouseY);
        if (this.drawBackground) {
            this.drawRect(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), this.backgroundColor());
        }

        int textWidth = this.font.width(this.label);
        int textX = this.leftAlign ? this.getX() + 8 : this.getCenterX() - textWidth / 2;
        int textY = this.getCenterY() - this.font.lineHeight / 2;
        this.drawString(guiGraphics, this.label, textX, textY, this.textColor());

        if (this.enabled && this.selected) {
            this.drawRect(guiGraphics, this.getX(), this.getLimitY() - 1, this.getLimitX(), this.getLimitY(), GuiThemes.SELECTED_UNDERLINE);
        }

        if (this.drawFrame || (this.enabled && this.shouldRenderFocusBorder())) {
            this.drawBorder(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), GuiThemes.FLAT_BUTTON_FOCUS_BORDER);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.enabled || !this.visible || button != 0 || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        this.doAction();

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.enabled || !this.visible || !this.isFocused() || !isSelectionKey(keyCode)) {
            return false;
        }

        this.doAction();

        return true;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        return this.enabled && this.visible ? super.nextFocusPath(navigation) : null;
    }

    @Override
    public boolean isActive() {
        return this.enabled && this.visible;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        this.addButtonNarration(builder, this.label);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return this.visible;
    }

    private int backgroundColor() {
        if (!this.enabled) {
            return this.theme.bgInactive;
        }

        return this.hovered ? this.theme.bgHighlight : this.theme.bgDefault;
    }

    private int textColor() {
        return this.enabled ? this.theme.themeLighter : this.theme.themeDarker;
    }

    private void doAction() {
        this.action.run();
        this.playClickSound();
    }
}
