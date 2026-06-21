package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonControl;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonRenderer;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

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
        if (this.rso$mouseClickedUndoButton(event)) {
            return true;
        }

        boolean reverse = Minecraft.getInstance().hasShiftDown();
        if (event.button() == 1) {
            if (!ReeseSodiumOptionsConfig.config().isReverseCyclingControls()) {
                return false;
            }

            reverse = true;
        } else if (event.button() != 0) {
            return false;
        }

        if (this.getOption().isEnabled() && this.isMouseOver(event.x(), event.y())) {
            this.cycleControl(reverse);
            return true;
        } else {
            return false;
        }
    }

    @Unique
    private boolean rso$mouseClickedUndoButton(MouseButtonEvent event) {
        Option option = this.getOption();
        if (!(option instanceof StatefulOption<?> statefulOption)) {
            return false;
        }

        if (event.button() != 0 || !OptionUndoButtonRenderer.isMouseOver(this.rso$getVisibleDim(), statefulOption, event.x(), event.y())) {
            return false;
        }

        ((OptionUndoButtonControl) this).rso$focusUndoButton();
        ((OptionUndoButtonControl) this).rso$getUndoButtonElement().mouseClicked(event, false);

        return true;
    }

    @Unique
    private Dim2i rso$getVisibleDim() {
        Dim2i dim = this.getDimensions();

        return new Dim2i(this.getX(), this.getY(), dim.width(), dim.height());
    }

}
