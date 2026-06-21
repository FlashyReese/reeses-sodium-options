package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonControl;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionResetButtonRenderer;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonRenderer;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.glfw.GLFW;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.options.control.StatefulControlElement")
public abstract class MixinStatefulControlElement extends ControlElement {
    public MixinStatefulControlElement(AbstractOptionList list, Dim2i dim, ColorTheme theme) {
        super(list, dim, theme);
    }

    @Shadow
    public abstract StatefulOption<?> getOption();

    @Inject(method = "render", at = @At("TAIL"))
    private void rso$renderActionButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        OptionUndoButtonControl actionButtonControl = (OptionUndoButtonControl) this;

        if (actionButtonControl.rso$isActionButtonLayoutHeld() && !rso$isLeftMouseButtonDown()) {
            actionButtonControl.rso$releaseUndoButtonLayoutHold();
        }

        boolean undoButtonVisible = actionButtonControl.rso$isUndoButtonVisible();

        if (actionButtonControl.rso$isResetButtonVisible()) {
            OptionResetButtonRenderer.render(guiGraphics, this.rso$getVisibleDim(), this.getOption(), mouseX, mouseY, actionButtonControl.rso$isResetButtonFocused(), undoButtonVisible);
        }

        if (undoButtonVisible) {
            OptionUndoButtonRenderer.render(guiGraphics, this.rso$getVisibleDim(), this.getOption(), mouseX, mouseY, actionButtonControl.rso$isUndoButtonFocused());
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rso$mouseClickedActionButton(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        StatefulOption<?> option = this.getOption();
        OptionUndoButtonControl actionButtonControl = (OptionUndoButtonControl) this;

        if (button != 0) {
            return;
        }

        Dim2i visibleDim = this.rso$getVisibleDim();
        boolean undoButtonVisible = actionButtonControl.rso$isUndoButtonVisible();

        if (actionButtonControl.rso$isResetButtonVisible()
                && OptionResetButtonRenderer.isMouseOver(visibleDim, option, mouseX, mouseY, undoButtonVisible)) {
            actionButtonControl.rso$focusResetButton();
            actionButtonControl.rso$getResetButtonElement().mouseClicked(mouseX, mouseY, button);
            cir.setReturnValue(true);
            return;
        }

        if (undoButtonVisible && OptionUndoButtonRenderer.isMouseOver(visibleDim, option, mouseX, mouseY)) {
            actionButtonControl.rso$focusUndoButton();
            actionButtonControl.rso$getUndoButtonElement().mouseClicked(mouseX, mouseY, button);
            cir.setReturnValue(true);
        }
    }

    @Unique
    private Dim2i rso$getVisibleDim() {
        Dim2i dim = this.getDimensions();

        return new Dim2i(this.getX(), this.getY(), dim.width(), dim.height());
    }

    @Unique
    private static boolean rso$isLeftMouseButtonDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();

        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

}
