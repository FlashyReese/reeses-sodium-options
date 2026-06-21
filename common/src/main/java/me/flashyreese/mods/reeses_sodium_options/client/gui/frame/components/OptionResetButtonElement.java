package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class OptionResetButtonElement implements GuiEventListener {
    private final Supplier<Dim2i> controlDimSupplier;
    private final Supplier<@Nullable StatefulOption<?>> optionSupplier;
    private final BooleanSupplier undoButtonVisibleSupplier;
    private final Runnable clickSound;
    private final Runnable afterReset;
    private boolean focused;

    public OptionResetButtonElement(Supplier<Dim2i> controlDimSupplier, Supplier<@Nullable StatefulOption<?>> optionSupplier, BooleanSupplier undoButtonVisibleSupplier, Runnable clickSound, Runnable afterReset) {
        this.controlDimSupplier = controlDimSupplier;
        this.optionSupplier = optionSupplier;
        this.undoButtonVisibleSupplier = undoButtonVisibleSupplier;
        this.clickSound = clickSound;
        this.afterReset = afterReset;
    }

    public boolean isActive() {
        StatefulOption<?> option = this.optionSupplier.get();

        return option != null && OptionResetButtonRenderer.isActive(option);
    }

    public Dim2i getDimensions() {
        return OptionResetButtonRenderer.getButtonDim(this.controlDimSupplier.get(), this.undoButtonVisibleSupplier.getAsBoolean());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        return this.reset();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused || !CommonInputs.selected(keyCode)) {
            return false;
        }

        return this.reset();
    }

    public boolean reset() {
        StatefulOption<?> option = this.optionSupplier.get();

        if (option == null || !OptionResetButtonRenderer.isActive(option)) {
            return false;
        }

        OptionResetButtonRenderer.resetToDefault(option);
        this.clickSound.run();
        this.afterReset.run();

        return true;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        return this.isActive() && !this.focused ? ComponentPath.leaf(this) : null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        StatefulOption<?> option = this.optionSupplier.get();

        return option != null && OptionResetButtonRenderer.isMouseOver(this.controlDimSupplier.get(), option, mouseX, mouseY, this.undoButtonVisibleSupplier.getAsBoolean());
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
