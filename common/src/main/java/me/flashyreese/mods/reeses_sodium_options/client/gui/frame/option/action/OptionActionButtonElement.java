package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class OptionActionButtonElement implements GuiEventListener, NarratableEntry {
    private final Supplier<LayoutBounds> rowBoundsSupplier;
    private final Supplier<@Nullable StatefulOption<?>> optionSupplier;
    private final IntSupplier buttonsFromRight;
    private final Identifier icon;
    private final Function<StatefulOption<?>, Component> narrationLabelProvider;
    private final Predicate<StatefulOption<?>> activePredicate;
    private final Consumer<StatefulOption<?>> action;
    private final Runnable clickSound;
    private final Runnable afterAction;
    private boolean focused;
    private boolean hovered;

    OptionActionButtonElement(Supplier<LayoutBounds> rowBoundsSupplier, Supplier<@Nullable StatefulOption<?>> optionSupplier,
                              IntSupplier buttonsFromRight, Identifier icon, Function<StatefulOption<?>, Component> narrationLabelProvider, Predicate<StatefulOption<?>> activePredicate,
                              Consumer<StatefulOption<?>> action, Runnable clickSound, Runnable afterAction) {
        this.rowBoundsSupplier = rowBoundsSupplier;
        this.optionSupplier = optionSupplier;
        this.buttonsFromRight = buttonsFromRight;
        this.icon = icon;
        this.narrationLabelProvider = narrationLabelProvider;
        this.activePredicate = activePredicate;
        this.action = action;
        this.clickSound = clickSound;
        this.afterAction = afterAction;
    }

    @Override
    public boolean isActive() {
        StatefulOption<?> option = this.optionSupplier.get();

        return option != null && this.activePredicate.test(option);
    }

    public LayoutBounds getDimensions() {
        return this.getButtonDim(this.rowBoundsSupplier.get());
    }

    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean focused) {
        StatefulOption<?> option = this.optionSupplier.get();

        if (option == null || !this.activePredicate.test(option)) {
            return;
        }

        this.hovered = this.isMouseOver(mouseX, mouseY);
        OptionActionButtonRenderer.render(guiGraphics, this.icon, this.getButtonDim(this.rowBoundsSupplier.get()), mouseX, mouseY, focused);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || !this.isMouseOver(event.x(), event.y())) {
            return false;
        }

        return this.performAction();
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!this.focused || !event.isSelection()) {
            return false;
        }

        return this.performAction();
    }

    public boolean performAction() {
        StatefulOption<?> option = this.optionSupplier.get();

        if (option == null || !this.activePredicate.test(option)) {
            return false;
        }

        this.action.accept(option);
        this.clickSound.run();
        this.afterAction.run();

        return true;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent navigation) {
        return this.isActive() && !this.focused ? ComponentPath.leaf(this) : null;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        StatefulOption<?> option = this.optionSupplier.get();

        return option != null && this.activePredicate.test(option)
                && this.getButtonDim(this.rowBoundsSupplier.get()).contains(mouseX, mouseY);
    }

    @Override
    public @NonNull ScreenRectangle getRectangle() {
        if (!this.isActive()) {
            return ScreenRectangle.empty();
        }

        LayoutBounds dim = this.getDimensions();

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

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        }

        return this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput builder) {
        StatefulOption<?> option = this.optionSupplier.get();

        if (option == null || !this.activePredicate.test(option)) {
            return;
        }

        builder.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", this.narrationLabelProvider.apply(option)));
        if (this.focused) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
        } else if (this.hovered) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
        }
    }

    private LayoutBounds getButtonDim(LayoutBounds rowBounds) {
        return OptionActionButtonRenderer.buttonBounds(rowBounds, this.buttonsFromRight.getAsInt());
    }
}
