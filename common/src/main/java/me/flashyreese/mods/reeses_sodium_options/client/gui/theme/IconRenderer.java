package me.flashyreese.mods.reeses_sodium_options.client.gui.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public final class IconRenderer {
    public static int renderIconWithSpacing(GuiGraphicsExtractor guiGraphics, Identifier icon, int color, boolean monochrome, int x, int y, int height, int spacing) {
        int iconSize = height - spacing * 2;
        AbstractTexture texture = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(icon);
        int textureWidth = texture.getTexture().getWidth(0);
        int textureHeight = texture.getTexture().getHeight(0);
        int iconX = x + spacing;
        int iconY = y + height / 2 - iconSize / 2;

        if (monochrome) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, textureWidth, textureHeight, textureWidth, textureHeight, color);
        } else {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, textureWidth, textureHeight, textureWidth, textureHeight);
        }

        return spacing * 2 + iconSize;
    }
}
