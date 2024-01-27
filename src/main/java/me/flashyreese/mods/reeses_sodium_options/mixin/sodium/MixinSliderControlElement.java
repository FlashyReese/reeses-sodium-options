package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.SliderControlElementExtended;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.options.control.SliderControl$Button")
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
    private int max;

    @Unique
    private boolean editMode;

    @Shadow
    @Final
    private int range;

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

    @Shadow
    public abstract int getIntValue();

    @Shadow public abstract void setValue(double d);

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void postInit(Option<Integer> option, Dim2i dim, int min, int max, int interval, ControlValueFormatter formatter, CallbackInfo ci) {
        this.max = max;
    }

    @Unique
    private Dim2i getSliderBounds() { // fixme: insanity
        return new Dim2i(this.dim.getLimitX() - 96, this.dim.getCenterY() - 5, 90, 10);
    }

    @Inject(method = "renderSlider", at = @At(value = "TAIL"))
    public void rso$renderSlider(DrawContext drawContext, CallbackInfo ci) {
        int sliderX = this.getSliderBounds().x();
        int sliderY = this.getSliderBounds().y();
        int sliderWidth = this.getSliderBounds().width();
        int sliderHeight = this.getSliderBounds().height();
        this.thumbPosition = this.getThumbPositionForValue(this.option.getValue());
        double thumbOffset = MathHelper.clamp((double) (this.getIntValue() - this.min) / (double) this.range * (double) sliderWidth, 0.0, sliderWidth);
        double thumbX = (double) sliderX + thumbOffset - 2.0;
        if (this.isFocused() && this.isEditMode()) {
            this.drawRect(drawContext, (int) (thumbX - 1), sliderY - 1, (int) (thumbX + 5), sliderY + sliderHeight + 1, 0xFFFFFFFF);
        }
    }

    @Redirect(method = "renderStandaloneValue", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Rect2i;getX()I"))
    public int rso$renderStandaloneValueSliderBoundsGetX(Rect2i instance) {
        return this.getSliderBounds().x();
    }

    @Redirect(method = "renderStandaloneValue", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Rect2i;getY()I"))
    public int renderStandaloneValueSliderBoundsGetY(Rect2i instance) {
        return this.getSliderBounds().y();
    }

    @Redirect(method = "renderSlider", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Rect2i;getX()I"))
    public int rso$renderSliderSliderBoundsGetX(Rect2i instance) {
        return this.getSliderBounds().x();
    }

    @Redirect(method = "renderSlider", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Rect2i;getY()I"))
    public int rso$renderSliderSliderBoundsGetY(Rect2i instance) {
        return this.getSliderBounds().y();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        if (keyCode == InputUtil.GLFW_KEY_ENTER) {
            this.setEditMode(!this.isEditMode());;
            return true;
        }

        if (this.isEditMode()) {
            if (keyCode == InputUtil.GLFW_KEY_LEFT) {
                this.option.setValue(MathHelper.clamp(this.option.getValue() - interval, min, max));
                return true;
            } else if (keyCode == InputUtil.GLFW_KEY_RIGHT) {
                this.option.setValue(MathHelper.clamp(this.option.getValue() + interval, min, max));
                return true;
            }
        }

        return false;
    }

    @Inject(method = "setValueFromMouse", at = @At(value = "HEAD"), cancellable = true, remap = false) // fixme: insanity part 3
    public void rso$setValueFromMouse(double d, CallbackInfo ci) {
        this.setValue((d - (double)this.getSliderBounds().x()) / (double)this.getSliderBounds().width());
        ci.cancel();
    }

    @Redirect(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Rect2i;contains(II)Z"))
    public boolean rso$mouseClicked(Rect2i instance, int x, int y) {
        return this.getSliderBounds().containsCursor(x, y);
    }

    @Unique
    private void setValueFromMouseScroll(double amount) {
        if (this.option.getValue() + this.interval * (int) amount <= this.max && this.option.getValue() + this.interval * (int) amount >= this.min) {
            this.option.setValue(this.option.getValue() + this.interval * (int) amount);
            this.thumbPosition = this.getThumbPositionForValue(this.option.getValue());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.option.isAvailable() && this.getSliderBounds().containsCursor(mouseX, mouseY) && Screen.hasShiftDown()) {
            this.setValueFromMouseScroll(amount); // todo: horizontal separation

            return true;
        }

        return false;
    }
}