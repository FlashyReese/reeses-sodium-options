package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl$CyclingControlElement")
public abstract class MixinCyclingControlElement<T extends Enum<T>> extends ControlElement<T> {

    @Shadow
    private int currentIndex;

    @Shadow
    @Final
    private T[] allowedValues;

    public MixinCyclingControlElement(Option<T> option, Dim2i dim) {
        super(option, dim);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.option.isAvailable() && this.dim.containsCursor(mouseX, mouseY) && (button == 0 || button == 1)) {
            this.currentIndex = Math.floorMod(this.option.getValue().ordinal() + (button == 0 ? 1 : -1), this.allowedValues.length);
            this.option.setValue(this.allowedValues[this.currentIndex]);
            this.playClickSound();

            return true;
        }

        return false;
    }
}
