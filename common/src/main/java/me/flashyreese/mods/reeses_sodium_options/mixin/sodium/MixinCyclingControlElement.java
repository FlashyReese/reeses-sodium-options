package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.getOption().isEnabled() && button == 0 && this.isMouseOver(mouseX, mouseY)) {
            this.cycleControl(button == 1);
            return true;
        } else {
            return false;
        }
    }
}
