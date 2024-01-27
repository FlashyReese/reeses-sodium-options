package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlElement.class)
public abstract class MixinControlElement<T> extends AbstractWidget {

    @Shadow
    @Final
    protected Dim2i dim;

    @Shadow @Final protected Option<T> option;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void postInit(Option<T> option, Dim2i dim, CallbackInfo ci) {
        if (this.option instanceof OptionExtended optionExtended) {
            optionExtended.setDim2i(this.dim);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gui/options/control/ControlElement;drawString(Lnet/minecraft/client/gui/DrawContext;Ljava/lang/String;III)V"))
    public void drawString(ControlElement<T> instance, DrawContext drawContext, String s, int x, int y, int color) {
        if (this.option instanceof OptionExtended optionExtended && optionExtended.isHighlight()) {
            String replacement = optionExtended.getSelected() ? Formatting.DARK_GREEN.toString() : Formatting.YELLOW.toString();

            s = s.replace(Formatting.WHITE.toString(), Formatting.WHITE + replacement);
            s = s.replace(Formatting.STRIKETHROUGH.toString(), Formatting.STRIKETHROUGH + replacement);
            s = s.replace(Formatting.ITALIC.toString(), Formatting.ITALIC + replacement);
        }

        this.drawString(drawContext, s, x, y, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/util/Dim2i;containsCursor(DD)Z"))
    public boolean render(Dim2i dim2i, double x, double y) {
        return this.isMouseOver(x, y);
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.option.getName());
        super.appendNarrations(builder);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }
}
