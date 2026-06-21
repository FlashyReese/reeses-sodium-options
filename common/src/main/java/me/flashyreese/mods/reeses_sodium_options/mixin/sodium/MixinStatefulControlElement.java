package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonControl;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonRenderer;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
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
    private void rso$renderUndoButton(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        OptionUndoButtonControl undoButtonControl = (OptionUndoButtonControl) this;

        if (undoButtonControl.rso$isUndoButtonHidden()) {
            if (rso$isLeftMouseButtonDown()) {
                return;
            }

            undoButtonControl.rso$releaseUndoButtonLayoutHold();
        }

        boolean focused = undoButtonControl.rso$isUndoButtonFocused();

        OptionUndoButtonRenderer.render(guiGraphics, this.rso$getVisibleDim(), this.getOption(), mouseX, mouseY, focused);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rso$mouseClickedUndoButton(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        StatefulOption<?> option = this.getOption();

        if (!((OptionUndoButtonControl) this).rso$isUndoButtonHidden()
                && event.button() == 0
                && OptionUndoButtonRenderer.isMouseOver(this.rso$getVisibleDim(), option, event.x(), event.y())) {
            ((OptionUndoButtonControl) this).rso$focusUndoButton();
            ((OptionUndoButtonControl) this).rso$getUndoButtonElement().mouseClicked(event, doubleClick);
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
        long window = Minecraft.getInstance().getWindow().handle();

        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

}
