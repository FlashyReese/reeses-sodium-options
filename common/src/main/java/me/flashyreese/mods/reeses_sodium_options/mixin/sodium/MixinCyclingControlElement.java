package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl$CyclingControlElement")
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
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (this.option.isAvailable() && this.dim.containsCursor(event.x(), event.y()) && (event.button() == 0 || event.button() == 1)) {
            this.currentIndex = Math.floorMod(this.option.getValue().ordinal() + (event.button() == 0 ? 1 : -1), this.allowedValues.length);
            this.option.setValue(this.allowedValues[this.currentIndex]);
            this.playClickSound();

            return true;
        }

        return false;
    }
}
