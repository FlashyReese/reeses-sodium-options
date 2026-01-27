package me.flashyreese.mods.reeses_sodium_options.mixin.sodium;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.AbstractWidgetExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.FlatButtonWidgetExtended;
import net.caffeinemc.mods.sodium.client.gui.ButtonTheme;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FlatButtonWidget.class)
public abstract class MixinFlatButtonWidget extends AbstractWidget implements FlatButtonWidgetExtended, AbstractWidgetExtended {

    @Mutable
    @Shadow
    @Final
    private boolean leftAlign;

    @Mutable
    @Unique
    @Final
    private boolean isTab = false;

    @Mutable
    @Unique
    @Final
    private Dim2i dimBorder = this.getDim();

    @Shadow
    @Final
    private ButtonTheme theme;

    @Shadow
    private boolean selected;

    @Shadow
    private boolean enabled;


    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/widgets/FlatButtonWidget;isMouseOver(DD)Z", shift = At.Shift.AFTER))
    private void changeCursorType(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.enabled && this.hovered && !this.isTab()) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        if (!this.enabled && this.hovered) {
            graphics.requestCursor(CursorTypes.NOT_ALLOWED);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/widgets/FlatButtonWidget;isMouseOver(DD)Z"))
    private boolean redirectIsMouseOver(FlatButtonWidget instance, double mouseX, double mouseY) {
        Dim2i dim = this.getDim();
        Dim2i border = this.getDimBorder();

        if (dim.getLimitX() <= border.x() || dim.getLimitY() <= border.y() || dim.x() >= border.getLimitX() || dim.y() >= border.getLimitY()) {
            return false;
        }

        double x = Math.max(dim.x(), border.x());
        double y = Math.max(dim.y(), border.y());
        double limitX = Math.min(dim.getLimitX(), border.getLimitX());
        double limitY = Math.min(dim.getLimitY(), border.getLimitY());

        return mouseX >= x && mouseX < limitX && mouseY >= y && mouseY < limitY;
    }

    @Inject(method = "getTextColor", at = @At("HEAD"), cancellable = true)
    private void modifyGetTextColor(CallbackInfoReturnable<Integer> cir) {
        if (this.isTab()) {
            int color = this.enabled ? (this.selected ? this.theme.themeLighter : this.hovered ? this.theme.theme : this.theme.themeDarker) : this.theme.themeDarker;
            cir.setReturnValue(color);
        }
    }

    protected MixinFlatButtonWidget(Dim2i dim) {
        super(dim);
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/widgets/FlatButtonWidget;drawString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;III)V"))
    public void redirectDrawString(Args args) {
        if (this.leftAlign) { // Aligns the text to the left by 10 pixels
            //this.drawString(guiGraphics, text, this.dim.x() + 10, y, color);
            args.set(2, this.getDim().x() + 10);
        }
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/widgets/FlatButtonWidget;drawRect(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", ordinal = 1))
    public void redirectDrawRect(Args args) {
        if (this.leftAlign) {
            //this.drawRect(guiGraphics, x1, this.dim.y(), x1 + 2, y2, color);
            args.set(2, this.getDim().y());
            args.set(3, (int) args.get(1) + 2);
            args.set(5, this.theme.theme);
        }
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/widgets/FlatButtonWidget;drawBorder(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", ordinal = 0))
    public void redirectDrawBorder(Args args) {
        if (this.leftAlign) {
            //this.drawRect(guiGraphics, x1, this.dim.y(), x1 + 1, y2, color);
            args.set(5, (0x80 << 24) | this.theme.themeLighter & 0xFFFFFF);
        }
    }

    @Override
    public boolean isLeftAlign() {
        return this.leftAlign;
    }

    @Override
    public void setLeftAlign(boolean leftAlign) {
        this.leftAlign = leftAlign;
    }

    @Override
    public boolean isTab() {
        return this.isTab;
    }

    @Override
    public void setTab(boolean tab) {
        this.isTab = tab;
    }

    @Override
    public Dim2i getDimBorder(){
        return this.dimBorder;
    }

    @Override
    public void setDimBorder(Dim2i dim2i) {
        this.dimBorder = dim2i;
    }
}
