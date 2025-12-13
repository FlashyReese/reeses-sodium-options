package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class LabelComponent extends AbstractWidget {
    private final Component text;
    private final int color;
    public LabelComponent(Dim2i dim, Component text, int color) {
        super(dim);
        this.text = text;
        this.color = color;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        int textWidth = this.getStringWidth(this.text);
        int x = this.getCenterX() - (textWidth / 2);
        int y = this.getY();
        this.drawString(guiGraphics, this.text, x, y, this.color);
    }
}
