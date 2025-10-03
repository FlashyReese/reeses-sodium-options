package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.SliderControlElementExtended;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl$Button")
public abstract class MixinSliderControlElement extends ControlElement<Integer> implements SliderControlElementExtended {

    @Shadow
    @Final
    private int interval;

    @Shadow
    private double thumbPosition;

    @Shadow
    @Final
    private int min;

    @Unique
    private boolean editMode;

    public MixinSliderControlElement(Option<Integer> option, Dim2i dim) {
        super(option, dim);
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

    @Mutable
    @Shadow
    @Final
    private Rect2i sliderBounds;

    @Shadow
    @Final
    private int max;

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.sliderBounds = new Rect2i(this.dim.getLimitX() - 96, this.dim.getCenterY() - 5, 90, 10);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/options/control/SliderControl$Button;drawString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;III)V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void rso$renderSlider(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci, int sliderX, int sliderY, int sliderWidth, int sliderHeight, MutableComponent label, int labelWidth, boolean drawSlider, double thumbOffset, int thumbX, int trackY) {
        if (drawSlider && this.isFocused() && this.isEditMode()) {
            this.drawRect(graphics, thumbX - 1, sliderY - 1, thumbX + 5, sliderY + sliderHeight + 1, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused()) return false;

        if (event.isSelection()) {
            this.setEditMode(!this.isEditMode());
            return true;
        }

        if (this.isEditMode()) {
            if (event.isLeft()) {
                this.option.setValue(Mth.clamp(this.option.getValue() - this.interval, this.min, this.max));
                return true;
            } else if (event.isRight()) {
                this.option.setValue(Mth.clamp(this.option.getValue() + this.interval, this.min, this.max));
                return true;
            }
        }

        return false;
    }

    @Unique
    private void setValueFromMouseScroll(double amount) {
        if (this.option.getValue() + this.interval * (int) amount <= this.max && this.option.getValue() + this.interval * (int) amount >= this.min) {
            this.option.setValue(this.option.getValue() + this.interval * (int) amount);
            this.thumbPosition = this.getThumbPositionForValue(this.option.getValue());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.option.isAvailable() && this.sliderBounds.contains((int) mouseX, (int) mouseY) && Minecraft.getInstance().hasShiftDown()) {
            this.setValueFromMouseScroll(verticalAmount); // todo: horizontal separation

            return true;
        }

        return false;
    }
}