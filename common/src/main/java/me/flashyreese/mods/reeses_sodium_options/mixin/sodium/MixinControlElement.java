package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonControl;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonElement;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonRenderer;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

import java.util.List;

@Mixin(ControlElement.class)
public abstract class MixinControlElement extends AbstractWidget implements ContainerEventHandler, OptionUndoButtonControl {
    @Unique
    private GuiEventListener rso$focusedChild;
    @Unique
    private boolean rso$dragging;
    @Unique
    private OptionUndoButtonElement rso$undoButtonElement;
    @Unique
    private int rso$heldUndoButtonWidth = -1;
    @Unique
    private boolean rso$undoButtonHidden;

    protected MixinControlElement(Dim2i dim) {
        super(dim);
    }

    @Shadow
    public abstract Option getOption();

    @Override
    public int getWidth() {
        return this.getDimensions().width() - this.rso$getUndoButtonWidth();
    }

    @Override
    public int getLimitX() {
        return this.getDimensions().getLimitX() - this.rso$getUndoButtonWidth();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
            return true;
        }

        return !this.rso$isUndoButtonHidden()
                && this.getOption() instanceof StatefulOption<?> statefulOption
                && OptionUndoButtonRenderer.isMouseOver(this.rso$getVisibleDim(), statefulOption, mouseX, mouseY);
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

            if (this.rso$shouldReturnToControl(navigation)) {
                cir.setReturnValue(ComponentPath.leaf((GuiEventListener) (Object) this));
                return;
            }

            cir.setReturnValue(null);
            return;
        }

        OptionUndoButtonElement undoButton = this.rso$getUndoButtonElement();

        if (this.isFocused() && !this.rso$isUndoButtonHidden() && undoButton.isActive() && this.rso$shouldEnterUndoButton(navigation)) {
            ComponentPath childPath = undoButton.nextFocusPath(navigation);

            cir.setReturnValue(childPath == null ? null : ComponentPath.path((ContainerEventHandler) (Object) this, childPath));
        }
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/options/control/ControlElement;drawString(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/lang/String;III)V"))
    public void drawString(ControlElement instance, GuiGraphicsExtractor drawContext, String s, int x, int y, int color) {
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
        OptionUndoButtonElement undoButton = this.rso$getUndoButtonElement();

        return !this.rso$isUndoButtonHidden() && undoButton.isActive() ? List.of(undoButton) : List.of();
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
        if (this.rso$focusedChild instanceof OptionUndoButtonElement undoButton
                && (this.rso$isUndoButtonHidden() || !undoButton.isActive())) {
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
    public boolean rso$isUndoButtonFocused() {
        return this.getFocused() == this.rso$getUndoButtonElement();
    }

    @Override
    public boolean rso$isUndoButtonHidden() {
        return this.rso$undoButtonHidden;
    }

    @Override
    public void rso$holdUndoButtonLayout(boolean hideButton) {
        if (this.rso$heldUndoButtonWidth < 0) {
            this.rso$heldUndoButtonWidth = this.rso$getNaturalUndoButtonWidth();
        }

        this.rso$undoButtonHidden = hideButton && this.rso$heldUndoButtonWidth == 0;

        if (this.rso$undoButtonHidden) {
            this.rso$clearUndoButtonFocus();
        }
    }

    @Override
    public void rso$releaseUndoButtonLayoutHold() {
        this.rso$heldUndoButtonWidth = -1;
        this.rso$undoButtonHidden = false;
    }

    @Override
    public void rso$focusUndoButton() {
        OptionUndoButtonElement undoButton = this.rso$getUndoButtonElement();

        if (!this.rso$isUndoButtonHidden() && undoButton.isActive()) {
            this.setFocused(undoButton);
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
    private int rso$getUndoButtonWidth() {
        if (this.rso$heldUndoButtonWidth >= 0) {
            return this.rso$heldUndoButtonWidth;
        }

        return this.rso$getNaturalUndoButtonWidth();
    }

    @Unique
    private int rso$getNaturalUndoButtonWidth() {
        return this.getOption() instanceof StatefulOption<?> statefulOption
                ? OptionUndoButtonRenderer.getReservedWidth(this.rso$getVisibleDim(), statefulOption)
                : 0;
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
    private boolean rso$shouldEnterUndoButton(FocusNavigationEvent navigation) {
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
