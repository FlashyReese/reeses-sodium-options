package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.Dim2iExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.Point2i;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Dim2i.class, remap = false)
public abstract class MixinDim2i implements Dim2iExtended, Point2i {

    @Unique
    private Point2i point2i;

    @Shadow
    @Final
    @Mutable
    private int x;

    @Shadow
    @Final
    @Mutable
    private int y;

    @Shadow
    @Final
    @Mutable
    private int width;

    @Shadow
    @Final
    @Mutable
    private int height;

    @Shadow
    public abstract int getLimitX();

    @Shadow
    public abstract int getLimitY();

    @Shadow
    public abstract int x();

    @Shadow
    public abstract int width();

    @Shadow
    public abstract int height();

    @Shadow
    public abstract int y();

    @Override
    public void setPoint2i(Point2i point2i) {
        this.point2i = point2i;
    }

    @Inject(method = "x", at = @At("HEAD"), cancellable = true)
    public void x(CallbackInfoReturnable<Integer> cir) {
        if (this.point2i != null) {
            cir.setReturnValue(this.x + this.point2i.getX());
        }
    }

    @Inject(method = "y", at = @At("HEAD"), cancellable = true)
    public void y(CallbackInfoReturnable<Integer> cir) {
        if (this.point2i != null) {
            cir.setReturnValue(this.y + this.point2i.getY());
        }
    }

    @Inject(method = "getLimitX", at = @At("HEAD"), cancellable = true)
    public void redirectGetLimitX(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.x() + this.width());
    }

    @Inject(method = "getLimitY", at = @At("HEAD"), cancellable = true)
    public void redirectGetLimitY(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.y() + this.height());
    }

    @Inject(method = "containsCursor", at = @At("HEAD"), cancellable = true)
    public void containsCursor(double x, double y, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(x >= (double) this.x() && x < (double) this.getLimitX() && y >= (double) this.y() && y < (double) this.getLimitY());
    }

    @Inject(method = "getCenterX", at = @At("HEAD"), cancellable = true)
    public void redirectGetCenterX(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.x() + this.width() / 2);
    }

    @Inject(method = "getCenterY", at = @At("HEAD"), cancellable = true)
    public void redirectGetCenterY(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.y() + this.height() / 2);
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getX() {
        return this.x();
    }

    @Override
    public int getY() {
        return this.y();
    }

    @Override
    public boolean canFitDimension(Dim2i anotherDim) {
        return this.x() <= anotherDim.x() && this.y() <= anotherDim.y() && this.getLimitX() >= anotherDim.getLimitX() && this.getLimitY() >= anotherDim.getLimitY();
    }

    @Override
    public boolean overlapWith(Dim2i other) {
        return this.x() < other.getLimitX() && this.getLimitX() > other.x() && this.y() < other.getLimitY() && this.getLimitY() > other.y();
    }
}
