package me.flashyreese.mods.reeses_sodium_options.client.gui.theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class IconRenderer {
    public static int renderIconWithSpacing(GuiGraphics guiGraphics, ResourceLocation icon, int color, boolean monochrome, int x, int y, int height, int spacing) {
        int iconSize = height - spacing * 2;
        int iconX = x + spacing;
        int iconY = y + height / 2 - iconSize / 2;

        if (monochrome) {
            guiGraphics.setColor(
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F,
                    ((color >> 24) & 0xFF) / 255.0F
            );
        }
        guiGraphics.blit(icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
        if (monochrome) {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        return spacing * 2 + iconSize;
    }
}
