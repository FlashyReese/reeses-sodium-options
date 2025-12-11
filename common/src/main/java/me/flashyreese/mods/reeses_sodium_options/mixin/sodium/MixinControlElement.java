package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ControlElement.class)
public abstract class MixinControlElement extends AbstractWidget {

    protected MixinControlElement(Dim2i dim) {
        super(dim);
    }

    @Shadow
    public abstract Option getOption();

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/options/control/ControlElement;drawString(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;III)V"))
    public void drawString(ControlElement instance, GuiGraphics drawContext, String s, int x, int y, int color) {
        if (this.getOption() instanceof OptionExtended optionExtended && optionExtended.isHighlight()) {
            String replacement = optionExtended.getSelected() ? ChatFormatting.DARK_GREEN.toString() : ChatFormatting.YELLOW.toString();

            s = s.replace(ChatFormatting.WHITE.toString(), ChatFormatting.WHITE + replacement);
            s = s.replace(ChatFormatting.STRIKETHROUGH.toString(), ChatFormatting.STRIKETHROUGH + replacement);
            s = s.replace(ChatFormatting.ITALIC.toString(), ChatFormatting.ITALIC + replacement);
        }

        this.drawString(drawContext, s, x, y, color);
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, this.getOption().getName());
        super.updateNarration(builder);
    }
}
