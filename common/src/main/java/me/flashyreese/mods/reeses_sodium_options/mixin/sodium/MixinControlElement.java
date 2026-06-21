package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonControl;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionResetButtonElement;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionResetButtonRenderer;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonElement;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonRenderer;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ControlElement.class)
public abstract class MixinControlElement extends AbstractWidget implements ContainerEventHandler, OptionUndoButtonControl {
    @Unique
    private GuiEventListener rso$focusedChild;
    @Unique
    private boolean rso$dragging;
    @Unique
    private OptionResetButtonElement rso$resetButtonElement;
    @Unique
    private OptionUndoButtonElement rso$undoButtonElement;
    @Unique
    private int rso$heldActionButtonWidth = -1;
    @Unique
    private boolean rso$heldResetButtonVisible;
    @Unique
    private boolean rso$heldUndoButtonVisible;
    @Unique
    private boolean rso$hideNewActionButtons;

    protected MixinControlElement(Dim2i dim) {
        super(dim);
    }

    @Shadow
    public abstract Option getOption();

    @Override
    public int getWidth() {
        return this.getDimensions().width() - this.rso$getActionButtonWidth();
    }

    @Override
    public int getLimitX() {
        return this.getDimensions().getLimitX() - this.rso$getActionButtonWidth();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
            return true;
        }

        if (!(this.getOption() instanceof StatefulOption<?> statefulOption)) {
            return false;
        }

        Dim2i visibleDim = this.rso$getVisibleDim();
        boolean undoButtonVisible = this.rso$isUndoButtonVisible();

        return (this.rso$isResetButtonVisible() && OptionResetButtonRenderer.isMouseOver(visibleDim, statefulOption, mouseX, mouseY, undoButtonVisible))
                || (undoButtonVisible && OptionUndoButtonRenderer.isMouseOver(visibleDim, statefulOption, mouseX, mouseY));
    }

    @Inject(method = "nextFocusPath", at = @At("HEAD"), cancellable = true)
    private void rso$nextFocusPath(FocusNavigationEvent navigation, CallbackInfoReturnable<ComponentPath> cir) {
        GuiEventListener focusedChild = this.getFocused();

        if (focusedChild != null) {
            ComponentPath childPath = focusedChild.nextFocusPath(navigation);

            if (childPath != null) {
                cir.setReturnValue(ComponentPath.path((ContainerEventHandler) (Object) this, childPath));
                return;
            }

            if (focusedChild == this.rso$getResetButtonElement() && this.rso$shouldEnterActionButton(navigation) && this.rso$isUndoButtonVisible()) {
                cir.setReturnValue(this.rso$getChildFocusPath(this.rso$getUndoButtonElement()));
                return;
            }

            if (focusedChild == this.rso$getUndoButtonElement() && this.rso$shouldReturnToControl(navigation) && this.rso$isResetButtonVisible()) {
                cir.setReturnValue(this.rso$getChildFocusPath(this.rso$getResetButtonElement()));
                return;
            }

            if (this.rso$shouldReturnToControl(navigation)) {
                cir.setReturnValue(ComponentPath.leaf((GuiEventListener) (Object) this));
                return;
            }

            cir.setReturnValue(null);
            return;
        }

        GuiEventListener firstActionButton = this.rso$getFirstVisibleActionButton();

        if (this.isFocused() && firstActionButton != null && this.rso$shouldEnterActionButton(navigation)) {
            cir.setReturnValue(this.rso$getChildFocusPath(firstActionButton));
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/options/control/ControlElement;drawString(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;III)V"))
    public void drawString(ControlElement instance, GuiGraphics drawContext, String s, int x, int y, int color) {
        if (this.getOption() instanceof OptionExtended optionExtended && optionExtended.isHighlight()) {
            String replacement = optionExtended.getSelected() ? ChatFormatting.DARK_GREEN.toString() : ChatFormatting.YELLOW.toString();

            s = s.replace(ChatFormatting.WHITE.toString(), ChatFormatting.WHITE + replacement);
            s = s.replace(ChatFormatting.STRIKETHROUGH.toString(), ChatFormatting.STRIKETHROUGH + replacement);
            s = s.replace(ChatFormatting.ITALIC.toString(), ChatFormatting.ITALIC + replacement);
        }

        this.drawString(drawContext, s, x, y, color);
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, this.getOption().getName());
        super.updateNarration(builder);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        List<GuiEventListener> children = new ArrayList<>(2);

        if (this.rso$isResetButtonVisible()) {
            children.add(this.rso$getResetButtonElement());
        }

        if (this.rso$isUndoButtonVisible()) {
            children.add(this.rso$getUndoButtonElement());
        }

        return children;
    }

    @Override
    public boolean isDragging() {
        return this.rso$dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.rso$dragging = dragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        if ((this.rso$focusedChild == this.rso$resetButtonElement && !this.rso$isResetButtonVisible())
                || (this.rso$focusedChild == this.rso$undoButtonElement && !this.rso$isUndoButtonVisible())) {
            this.rso$clearUndoButtonFocus();
        }

        return this.rso$focusedChild;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (this.rso$focusedChild == focused) {
            return;
        }

        if (this.rso$focusedChild != null) {
            this.rso$focusedChild.setFocused(false);
        }

        this.rso$focusedChild = focused;

        if (focused != null) {
            focused.setFocused(true);
        }
    }

    @Override
    public @Nullable ComponentPath getCurrentFocusPath() {
        GuiEventListener focusedChild = this.getFocused();

        if (focusedChild != null) {
            return ComponentPath.path((ContainerEventHandler) (Object) this, focusedChild.getCurrentFocusPath());
        }

        return this.isFocused() ? ComponentPath.leaf((GuiEventListener) (Object) this) : null;
    }

    @Override
    public OptionResetButtonElement rso$getResetButtonElement() {
        if (this.rso$resetButtonElement == null) {
            this.rso$resetButtonElement = new OptionResetButtonElement(
                    this::rso$getVisibleDim,
                    this::rso$getStatefulOption,
                    this::rso$isUndoButtonVisible,
                    this::playClickSound,
                    this::rso$clearUndoButtonFocus
            );
        }

        return this.rso$resetButtonElement;
    }

    @Override
    public OptionUndoButtonElement rso$getUndoButtonElement() {
        if (this.rso$undoButtonElement == null) {
            this.rso$undoButtonElement = new OptionUndoButtonElement(
                    this::rso$getVisibleDim,
                    this::rso$getStatefulOption,
                    this::playClickSound,
                    this::rso$clearUndoButtonFocus
            );
        }

        return this.rso$undoButtonElement;
    }

    @Override
    public boolean rso$isResetButtonFocused() {
        return this.getFocused() == this.rso$getResetButtonElement();
    }

    @Override
    public boolean rso$isUndoButtonFocused() {
        return this.getFocused() == this.rso$getUndoButtonElement();
    }

    @Override
    public boolean rso$isResetButtonVisible() {
        return this.rso$isResetButtonNaturallyVisible()
                && (!this.rso$isActionButtonLayoutHeld() || !this.rso$hideNewActionButtons || this.rso$heldResetButtonVisible);
    }

    @Override
    public boolean rso$isUndoButtonVisible() {
        return this.rso$isUndoButtonNaturallyVisible()
                && (!this.rso$isActionButtonLayoutHeld() || !this.rso$hideNewActionButtons || this.rso$heldUndoButtonVisible);
    }

    @Override
    public boolean rso$isActionButtonLayoutHeld() {
        return this.rso$heldActionButtonWidth >= 0;
    }

    @Override
    public void rso$holdUndoButtonLayout(boolean hideButton) {
        if (!this.rso$isActionButtonLayoutHeld()) {
            this.rso$heldResetButtonVisible = this.rso$isResetButtonNaturallyVisible();
            this.rso$heldUndoButtonVisible = this.rso$isUndoButtonNaturallyVisible();
            this.rso$heldActionButtonWidth = this.rso$getNaturalActionButtonWidth();
        }

        this.rso$hideNewActionButtons = hideButton;

        if ((this.rso$focusedChild == this.rso$resetButtonElement && !this.rso$isResetButtonVisible())
                || (this.rso$focusedChild == this.rso$undoButtonElement && !this.rso$isUndoButtonVisible())) {
            this.rso$clearUndoButtonFocus();
        }
    }

    @Override
    public void rso$releaseUndoButtonLayoutHold() {
        this.rso$heldActionButtonWidth = -1;
        this.rso$heldResetButtonVisible = false;
        this.rso$heldUndoButtonVisible = false;
        this.rso$hideNewActionButtons = false;
    }

    @Override
    public void rso$focusResetButton() {
        if (this.rso$isResetButtonVisible()) {
            this.setFocused(this.rso$getResetButtonElement());
        }
    }

    @Override
    public void rso$focusUndoButton() {
        if (this.rso$isUndoButtonVisible()) {
            this.setFocused(this.rso$getUndoButtonElement());
        }
    }

    @Override
    public void rso$clearUndoButtonFocus() {
        if (this.rso$focusedChild != null) {
            this.rso$focusedChild.setFocused(false);
        }

        this.rso$focusedChild = null;
    }

    @Unique
    private int rso$getActionButtonWidth() {
        if (this.rso$isActionButtonLayoutHeld()) {
            return this.rso$heldActionButtonWidth;
        }

        return this.rso$getNaturalActionButtonWidth();
    }

    @Unique
    private int rso$getNaturalActionButtonWidth() {
        StatefulOption<?> statefulOption = this.rso$getStatefulOption();
        if (statefulOption == null) {
            return 0;
        }

        Dim2i visibleDim = this.rso$getVisibleDim();

        return OptionResetButtonRenderer.getReservedWidth(visibleDim, statefulOption)
                + OptionUndoButtonRenderer.getReservedWidth(visibleDim, statefulOption);
    }

    @Unique
    private @Nullable StatefulOption<?> rso$getStatefulOption() {
        Option option = this.getOption();

        return option instanceof StatefulOption<?> statefulOption ? statefulOption : null;
    }

    @Unique
    private Dim2i rso$getVisibleDim() {
        Dim2i dim = this.getDimensions();

        return new Dim2i(this.getX(), this.getY(), dim.width(), dim.height());
    }

    @Unique
    private boolean rso$isResetButtonNaturallyVisible() {
        StatefulOption<?> statefulOption = this.rso$getStatefulOption();

        return statefulOption != null && OptionResetButtonRenderer.isActive(statefulOption);
    }

    @Unique
    private boolean rso$isUndoButtonNaturallyVisible() {
        StatefulOption<?> statefulOption = this.rso$getStatefulOption();

        return statefulOption != null && OptionUndoButtonRenderer.isActive(statefulOption);
    }

    @Unique
    private @Nullable GuiEventListener rso$getFirstVisibleActionButton() {
        if (this.rso$isResetButtonVisible()) {
            return this.rso$getResetButtonElement();
        }

        return this.rso$isUndoButtonVisible() ? this.rso$getUndoButtonElement() : null;
    }

    @Unique
    private ComponentPath rso$getChildFocusPath(GuiEventListener child) {
        return ComponentPath.path((ContainerEventHandler) (Object) this, ComponentPath.leaf(child));
    }

    @Unique
    private boolean rso$shouldEnterActionButton(FocusNavigationEvent navigation) {
        if (navigation instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            return arrowNavigation.direction() == ScreenDirection.RIGHT;
        }

        return navigation instanceof FocusNavigationEvent.TabNavigation tabNavigation && tabNavigation.forward();
    }

    @Unique
    private boolean rso$shouldReturnToControl(FocusNavigationEvent navigation) {
        if (navigation instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            return arrowNavigation.direction() == ScreenDirection.LEFT;
        }

        return navigation instanceof FocusNavigationEvent.TabNavigation tabNavigation && !tabNavigation.forward();
    }
}
