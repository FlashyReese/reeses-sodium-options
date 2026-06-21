package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SliderControlElementExtended;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl$SliderControlElement")
public abstract class MixinSliderControlElement extends ControlElement implements SliderControlElementExtended {

    @Shadow
    private double thumbPosition;

    @Unique
    private boolean editMode;

    public MixinSliderControlElement(AbstractOptionList list, Dim2i dim, ColorTheme theme) {
        super(list, dim, theme);
    }

    @Override
    public boolean isEditMode() {
        return this.editMode;
    }

    @Override
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    @Shadow
    public abstract double getThumbPositionForValue(int value);

    @Shadow
    public abstract Option getOption();

    @Shadow
    @Final
    private IntegerOption option;

    @Shadow
    public abstract boolean isMouseOverSlider(double mouseX, double mouseY);

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        //this.sliderBounds = new Rect2i(this.dim.getLimitX() - 96, this.dim.getCenterY() - 5, 90, 10);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/options/control/SliderControl$SliderControlElement;drawString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;III)V", ordinal = 0))
    public void rso$renderSlider(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci, @Local(ordinal = 3) int sliderY, @Local(ordinal = 5) int sliderHeight, @Local(ordinal = 1) boolean drawSlider, @Local(ordinal = 7) int thumbX) {
        if (drawSlider && this.isFocused() && this.isEditMode()) {
            this.drawRect(graphics, thumbX - 1, sliderY - 1, thumbX + 5, sliderY + sliderHeight + 1, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        if (CommonInputs.selected(keyCode)) {
            this.setEditMode(!this.isEditMode());
            return true;
        }

        if (this.isEditMode()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                this.option.modifyValue(Mth.clamp(this.option.getValidatedValue() - this.option.getSteppedValidator().step(), this.option.getSteppedValidator().min(), this.option.getSteppedValidator().max()));
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                this.option.modifyValue(Mth.clamp(this.option.getValidatedValue() + this.option.getSteppedValidator().step(), this.option.getSteppedValidator().min(), this.option.getSteppedValidator().max()));
                return true;
            }
        }

        return false;
    }

    @Unique
    private void setValueFromMouseScroll(double amount) {
        int newValue = this.option.getValidatedValue() + this.option.getSteppedValidator().step() * (int) amount;
        if (newValue <= this.option.getSteppedValidator().max() && newValue >= this.option.getSteppedValidator().min()) {
            this.option.modifyValue(newValue);
            this.thumbPosition = this.getThumbPositionForValue(this.option.getValidatedValue());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!ReeseSodiumOptionsConfig.config().isShiftScrollSliderAdjustments()
                || !this.getOption().isEnabled()
                || !Screen.hasShiftDown()) {
            return false;
        }

        if (this.isMouseOverSlider((int) mouseX, (int) mouseY)) {
            this.setValueFromMouseScroll(verticalAmount); // todo: horizontal separation

            return true;
        }

        return false;
    }
}
