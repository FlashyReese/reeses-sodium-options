package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class OptionActionButtonController {
    private final Supplier<LayoutBounds> rowBoundsSupplier;
    private final Supplier<@Nullable StatefulOption<?>> optionSupplier;
    private final List<ActionButton> buttons;
    private final ActionButton undoButton;
    private @Nullable GuiEventListener focusedChild;
    private int heldActionButtonWidth = -1;
    private boolean hideNewActionButtons;

    public OptionActionButtonController(Supplier<LayoutBounds> rowBoundsSupplier, Supplier<@Nullable StatefulOption<?>> optionSupplier, Runnable clickSound) {
        this.rowBoundsSupplier = rowBoundsSupplier;
        this.optionSupplier = optionSupplier;
        // Ordered left-to-right: reset sits to the left of undo.
        this.buttons = new ArrayList<>(2);
        this.buttons.add(new ActionButton(0, OptionResetAction.ICON, Component.literal("Reset"), option -> Component.translatable("rso.narration.reset_to_default", option.getName()), OptionResetAction::isActive, OptionResetAction::resetToDefault, clickSound));
        this.undoButton = new ActionButton(1, OptionUndoAction.ICON, Component.literal("Undo"), option -> Component.translatable("rso.narration.undo_changes", option.getName()), OptionUndoAction::isActive, OptionUndoAction::undoChanges, clickSound);
        this.buttons.add(this.undoButton);
    }

    public int actionButtonWidth() {
        if (this.isLayoutHeld()) {
            return this.heldActionButtonWidth;
        }

        return this.naturalReservedWidth();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.optionSupplier.get() == null) {
            return false;
        }

        for (ActionButton button : this.buttons) {
            if (button.visible() && button.element.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }

        return false;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.pruneInvisibleFocus();

        if (this.optionSupplier.get() == null) {
            return;
        }

        for (ActionButton button : this.buttons) {
            if (button.visible()) {
                button.element.render(guiGraphics, mouseX, mouseY, this.getFocused() == button.element);
            }
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || this.optionSupplier.get() == null) {
            return false;
        }

        for (ActionButton button : this.buttons) {
            if (button.visible() && button.element.isMouseOver(event.x(), event.y())) {
                this.setFocused(button.element);
                button.element.mouseClicked(event, doubleClick);
                return true;
            }
        }

        return false;
    }

    public boolean keyPressedFocusedChild(KeyEvent event) {
        GuiEventListener focusedChild = this.getFocused();

        return focusedChild != null && focusedChild.keyPressed(event);
    }

    public FocusPathResult nextFocusPath(ContainerEventHandler parent, GuiEventListener owner, boolean ownerFocused, FocusNavigationEvent navigation) {
        GuiEventListener focusedChild = this.getFocused();

        if (focusedChild != null) {
            return this.nextFocusPathFromChild(parent, owner, focusedChild, navigation);
        }

        GuiEventListener firstActionButton = this.firstVisibleActionButton();

        if (ownerFocused && firstActionButton != null && this.shouldEnterActionButton(navigation)) {
            return FocusPathResult.handled(this.childFocusPath(parent, firstActionButton));
        }

        return FocusPathResult.unhandled();
    }

    private FocusPathResult nextFocusPathFromChild(ContainerEventHandler parent, GuiEventListener owner, GuiEventListener focusedChild, FocusNavigationEvent navigation) {
        ComponentPath childPath = focusedChild.nextFocusPath(navigation);

        if (childPath != null) {
            return FocusPathResult.handled(ComponentPath.path(parent, childPath));
        }

        List<GuiEventListener> visible = this.visibleButtonElements();
        int index = visible.indexOf(focusedChild);

        if (this.shouldEnterActionButton(navigation)) {
            if (index >= 0 && index + 1 < visible.size()) {
                return FocusPathResult.handled(this.childFocusPath(parent, visible.get(index + 1)));
            }

            return FocusPathResult.handled(null);
        }

        if (this.shouldReturnToControl(navigation)) {
            if (index > 0) {
                return FocusPathResult.handled(this.childFocusPath(parent, visible.get(index - 1)));
            }

            return FocusPathResult.handled(ComponentPath.leaf(owner));
        }

        return FocusPathResult.handled(null);
    }

    public @Nullable ComponentPath currentFocusPath(ContainerEventHandler parent, GuiEventListener owner, boolean ownerFocused) {
        GuiEventListener focusedChild = this.getFocused();

        if (focusedChild != null) {
            return ComponentPath.path(parent, focusedChild.getCurrentFocusPath());
        }

        return ownerFocused ? ComponentPath.leaf(owner) : null;
    }

    public List<GuiEventListener> children() {
        return this.visibleButtonElements();
    }

    public @Nullable GuiEventListener getFocused() {
        return this.focusedChild;
    }

    private void pruneInvisibleFocus() {
        if (this.focusedChild != null && !this.isFocusedChildVisible()) {
            this.clearFocus();
        }
    }

    public void setFocused(@Nullable GuiEventListener focused) {
        if (this.focusedChild == focused) {
            return;
        }

        if (this.focusedChild != null) {
            this.focusedChild.setFocused(false);
        }

        this.focusedChild = focused;

        if (focused != null) {
            focused.setFocused(true);
        }
    }

    public void holdLayout(boolean hideButton) {
        if (!this.isLayoutHeld()) {
            for (ActionButton button : this.buttons) {
                button.heldVisible = button.naturallyVisible();
            }
            this.heldActionButtonWidth = this.naturalReservedWidth();
        }

        this.hideNewActionButtons = hideButton;

        if (this.focusedChild != null && !this.isFocusedChildVisible()) {
            this.clearFocus();
        }
    }

    public void releaseLayoutHold() {
        this.heldActionButtonWidth = -1;
        this.hideNewActionButtons = false;
        for (ActionButton button : this.buttons) {
            button.heldVisible = false;
        }
    }

    public void clearFocus() {
        if (this.focusedChild != null) {
            this.focusedChild.setFocused(false);
        }

        this.focusedChild = null;
    }

    public boolean undoFocusedButton() {
        return this.getFocused() == this.undoButton.element
                && this.undoButton.element.performAction();
    }

    private int naturalReservedWidth() {
        if (this.optionSupplier.get() == null) {
            return 0;
        }

        int height = this.rowBounds().height();
        int reserved = 0;

        for (ActionButton button : this.buttons) {
            if (button.naturallyVisible()) {
                reserved += height;
            }
        }

        return reserved;
    }

    private boolean isLayoutHeld() {
        return this.heldActionButtonWidth >= 0;
    }

    private boolean isFocusedChildVisible() {
        for (ActionButton button : this.buttons) {
            if (button.element == this.focusedChild) {
                return button.visible();
            }
        }

        return true;
    }

    private List<GuiEventListener> visibleButtonElements() {
        List<GuiEventListener> visible = new ArrayList<>(this.buttons.size());

        for (ActionButton button : this.buttons) {
            if (button.visible()) {
                visible.add(button.element);
            }
        }

        return visible;
    }

    private @Nullable GuiEventListener firstVisibleActionButton() {
        for (ActionButton button : this.buttons) {
            if (button.visible()) {
                return button.element;
            }
        }

        return null;
    }

    private LayoutBounds rowBounds() {
        return this.rowBoundsSupplier.get();
    }

    private ComponentPath childFocusPath(ContainerEventHandler parent, GuiEventListener child) {
        return ComponentPath.path(parent, ComponentPath.leaf(child));
    }

    private boolean shouldEnterActionButton(FocusNavigationEvent navigation) {
        if (navigation instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            return arrowNavigation.direction() == ScreenDirection.RIGHT;
        }

        return navigation instanceof FocusNavigationEvent.TabNavigation(boolean forward) && forward;
    }

    private boolean shouldReturnToControl(FocusNavigationEvent navigation) {
        if (navigation instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            return arrowNavigation.direction() == ScreenDirection.LEFT;
        }

        return navigation instanceof FocusNavigationEvent.TabNavigation(boolean forward) && !forward;
    }

    private final class ActionButton {
        private final int index;
        private final OptionActionButtonElement element;
        private boolean heldVisible;

        private ActionButton(int index, Identifier icon, Component guideLabel, Function<StatefulOption<?>, Component> narrationLabelProvider, Predicate<StatefulOption<?>> activePredicate, Consumer<StatefulOption<?>> action, Runnable clickSound) {
            this.index = index;
            this.element = new OptionActionButtonElement(
                    OptionActionButtonController.this.rowBoundsSupplier,
                    OptionActionButtonController.this.optionSupplier,
                    this::buttonsFromRight,
                    icon,
                    guideLabel,
                    narrationLabelProvider,
                    activePredicate,
                    action,
                    clickSound,
                    OptionActionButtonController.this::clearFocus
            );
        }

        private boolean naturallyVisible() {
            return this.element.isActive();
        }

        private boolean visible() {
            return this.naturallyVisible()
                    && (!isLayoutHeld() || !hideNewActionButtons || this.heldVisible);
        }

        private int buttonsFromRight() {
            int slot = 1;

            for (int i = this.index + 1; i < buttons.size(); i++) {
                if (buttons.get(i).visible()) {
                    slot++;
                }
            }

            return slot;
        }
    }

    public record FocusPathResult(boolean handled, @Nullable ComponentPath path) {
        private static FocusPathResult handled(@Nullable ComponentPath path) {
            return new FocusPathResult(true, path);
        }

        private static FocusPathResult unhandled() {
            return new FocusPathResult(false, null);
        }
    }
}
