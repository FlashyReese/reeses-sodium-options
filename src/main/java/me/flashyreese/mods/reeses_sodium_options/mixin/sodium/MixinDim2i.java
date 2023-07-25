package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.Dim2iExtended;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Dim2i.class)
public abstract class MixinDim2i implements Dim2iExtended {

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
    public boolean canFitDimension(Dim2i anotherDim) {
        return this.x <= anotherDim.getOriginX() && this.y <= anotherDim.getOriginY() && this.getLimitX() >= anotherDim.getLimitX() && this.getLimitY() >= anotherDim.getLimitY();
    }

    @Override
    public boolean overlapWith(Dim2i other) {
        return this.x < other.getLimitX() && this.getLimitX() > other.getOriginX() && this.y < other.getLimitY() && this.getLimitY() > other.getOriginY();
    }
}
