package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class OptionUndoButtonElement implements GuiEventListener {
    private final Supplier<Dim2i> controlDimSupplier;
    private final Supplier<@Nullable StatefulOption<?>> optionSupplier;
    private final Runnable clickSound;
    private final Runnable afterUndo;
    private boolean focused;

    public OptionUndoButtonElement(Supplier<Dim2i> controlDimSupplier, Supplier<@Nullable StatefulOption<?>> optionSupplier, Runnable clickSound, Runnable afterUndo) {
        this.controlDimSupplier = controlDimSupplier;
        this.optionSupplier = optionSupplier;
        this.clickSound = clickSound;
        this.afterUndo = afterUndo;
    }

    public boolean isActive() {
        StatefulOption<?> option = this.optionSupplier.get();

        return option != null && OptionUndoButtonRenderer.isActive(option);
    }

    public Dim2i getDimensions() {
        return OptionUndoButtonRenderer.getButtonDim(this.controlDimSupplier.get());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }

        return this.undo();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.focused || !event.isSelection()) {
            return false;
        }

        return this.undo();
    }

    public boolean undo() {
        StatefulOption<?> option = this.optionSupplier.get();

        if (option == null || !OptionUndoButtonRenderer.isActive(option)) {
            return false;
        }

        OptionUndoButtonRenderer.undoChanges(option);
        this.clickSound.run();
        this.afterUndo.run();

        return true;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        return this.isActive() && !this.focused ? ComponentPath.leaf(this) : null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        StatefulOption<?> option = this.optionSupplier.get();

        return option != null && OptionUndoButtonRenderer.isMouseOver(this.controlDimSupplier.get(), option, mouseX, mouseY);
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (!this.isActive()) {
            return ScreenRectangle.empty();
        }

        Dim2i dim = this.getDimensions();

        return new ScreenRectangle(dim.x(), dim.y(), dim.width(), dim.height());
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused && this.isActive();
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }
}
