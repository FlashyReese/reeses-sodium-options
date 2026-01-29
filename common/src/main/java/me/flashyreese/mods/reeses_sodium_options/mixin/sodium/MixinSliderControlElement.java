package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import me.flashyreese.mods.reeses_sodium_options.client.gui.SliderControlElementExtended;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl$SliderControlElement")
public abstract class MixinSliderControlElement extends ControlElement implements SliderControlElementExtended {

    @Shadow
    private double thumbPosition;

    @Unique
    private boolean editMode;

    @Mutable
    @Unique
    @Final
    private Dim2i dimBorder = new Dim2i(this.getSliderX(), this.getSliderY(), this.getSliderWidth(), this.getSliderHeight());

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

    @Shadow
    public abstract int getSliderX();

    @Shadow
    public abstract int getSliderY();

    @Shadow
    public abstract int getSliderWidth();

    @Shadow
    public abstract int getSliderHeight();

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

    @Inject(method = "isMouseOverSlider", at = @At("HEAD"), cancellable = true)
    public void modifyIsMouseOverSlider(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        Dim2i dim = new Dim2i(this.getSliderX(), this.getSliderY(), this.getSliderWidth(), this.getSliderHeight());
        Dim2i border = this.getDimBorder();

        if (dim.getLimitX() <= border.x() || dim.getLimitY() <= border.y() || dim.x() >= border.getLimitX() || dim.y() >= border.getLimitY()) {
            cir.cancel();
            cir.setReturnValue(false);
            return;
        }

        double x = Math.max(dim.x(), border.x());
        double y = Math.max(dim.y(), border.y());
        double limitX = Math.min(dim.getLimitX(), border.getLimitX());
        double limitY = Math.min(dim.getLimitY(), border.getLimitY());

        cir.cancel();
        cir.setReturnValue(mouseX >= x && mouseX < limitX && mouseY >= y && mouseY < limitY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused()) return false;

        if (event.isSelection()) {
            this.setEditMode(!this.isEditMode());
            return true;
        }

        if (!event.isSelection() && !(event.isLeft() || event.isRight())) {
            this.setEditMode(false);
            return false;
        }

        if (this.isEditMode()) {
            if (event.isLeft()) {
                this.option.modifyValue(Mth.clamp(this.option.getValidatedValue() - this.option.getSteppedValidator().step(), this.option.getSteppedValidator().min(), this.option.getSteppedValidator().max()));
                return true;
            } else if (event.isRight()) {
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
        if (this.getOption().isEnabled() && this.isMouseOverSlider((int) mouseX, (int) mouseY) && Minecraft.getInstance().hasShiftDown()) {
            this.setValueFromMouseScroll(verticalAmount); // todo: horizontal separation

            return true;
        }

        return false;
    }

    @Override
    public Dim2i getDimBorder(){
        return this.dimBorder;
    }

    @Override
    public void setDimBorder(Dim2i dim2i) {
        this.dimBorder = dim2i;
    }
}