package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl$CyclingControlElement")
public abstract class MixinCyclingControlElement<T extends Enum<T>> extends ControlElement {
    public MixinCyclingControlElement(AbstractOptionList list, Dim2i dim, ColorTheme theme) {
        super(list, dim, theme);
    }

    @Shadow
    public abstract Option getOption();

    @Shadow
    protected abstract void cycleControl(boolean reverse);

    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (this.getOption().isEnabled() && event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
            this.cycleControl(event.button() == 1);
            return true;
        } else {
            return false;
        }
    }
}
