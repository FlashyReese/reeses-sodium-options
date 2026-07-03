package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumWidgetDimensions;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Last-resort compatibility row for custom Sodium controls. Sodium's ControlElement owns the whole
 * row, so this wrapper delegates row rendering and input to it instead of mixing it into RSO's row
 * chrome.
 */
final class SodiumControlElementOptionRow extends BaseWidget implements OptionRow {
    private final Screen screen;
    private final ColorTheme theme;
    private final Option option;
    private final StubOptionList list;
    private ControlElement element;

    SodiumControlElementOptionRow(Screen screen, LayoutBounds dim, ColorTheme theme, Option option) {
        super(dim);
        this.screen = screen;
        this.theme = theme;
        this.option = option;
        this.list = new StubOptionList(this.toSodiumDim(dim));
        this.element = this.createElement(dim);
        this.list.getControls().add(this.element);
    }

    @Override
    public Option getOption() {
        return this.option;
    }

    @Override
    public void setDim(LayoutBounds dim) {
        super.setDim(dim);
        this.relayoutElement(dim);
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.element.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return this.element.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return this.element.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
        return this.element.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        return this.element.keyPressed(event);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.element.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isFocused() {
        return this.element.isFocused();
    }

    @Override
    public void setFocused(boolean focused) {
        this.element.setFocused(focused);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent navigation) {
        return this.isFocused() || !this.option.isEnabled() ? null : ComponentPath.leaf(this);
    }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        return this.element.narrationPriority();
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput builder) {
        this.element.updateNarration(builder);
    }

    @Override
    public void releaseActionButtonLayoutHold() {
    }

    @Override
    public boolean handleBackNavigation() {
        return false;
    }

    @Override
    public boolean undoFocusedActionButton() {
        return false;
    }

    @Override
    public void clearActionButtonFocus() {
    }

    private ControlElement createElement(LayoutBounds dim) {
        return this.option.getControl().createElement(this.screen, this.list, this.toSodiumDim(dim), this.theme);
    }

    private void relayoutElement(LayoutBounds dim) {
        Dim2i sodiumDim = this.toSodiumDim(dim);
        ((SodiumWidgetDimensions) (Object) this.list).rso$setDimensions(sodiumDim);
        ((SodiumWidgetDimensions) (Object) this.element).rso$setDimensions(sodiumDim);
    }

    private Dim2i toSodiumDim(LayoutBounds dim) {
        return new Dim2i(dim.x(), dim.y(), dim.width(), dim.height());
    }

    private static final class StubOptionList extends AbstractOptionList {
        private StubOptionList(Dim2i dim) {
            super(dim);
        }

        @Override
        public int getScrollAmount() {
            return 0;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            return false;
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of();
        }
    }
}
