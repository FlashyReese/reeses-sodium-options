package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class OptionActionButtonRenderer {
    private static final int ICON_SIZE = 10;
    private static final int BACKGROUND = 0x66000000;
    private static final int BACKGROUND_HOVERED = 0xA0000000;
    private static final int BACKGROUND_DISABLED = 0x33000000;
    private static final int BORDER_FOCUSED = 0xFFFFFFFF;
    private static final int ICON_COLOR = 0xFFFFFFFF;
    private static final int ICON_COLOR_DISABLED = 0x66FFFFFF;

    static LayoutBounds buttonBounds(LayoutBounds rowBounds, int buttonsFromRight) {
        int size = rowBounds.height();
        int x = rowBounds.getLimitX() - size * buttonsFromRight;

        return new LayoutBounds(x, rowBounds.y(), size, size);
    }

    static void render(GuiGraphics guiGraphics, Identifier icon, LayoutBounds buttonBounds, int mouseX, int mouseY, boolean focused, boolean active) {
        boolean hovered = active && buttonBounds.contains(mouseX, mouseY);

        guiGraphics.fill(buttonBounds.x(), buttonBounds.y(), buttonBounds.getLimitX(), buttonBounds.getLimitY(), active ? (hovered ? BACKGROUND_HOVERED : BACKGROUND) : BACKGROUND_DISABLED);
        if (focused && BaseWidget.isKeyboardFocusVisible()) {
            BaseWidget.border(guiGraphics, buttonBounds.x(), buttonBounds.y(), buttonBounds.getLimitX(), buttonBounds.getLimitY(), BORDER_FOCUSED);
        }

        int iconX = buttonBounds.getCenterX() - ICON_SIZE / 2;
        int iconY = buttonBounds.getCenterY() - ICON_SIZE / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, active ? ICON_COLOR : ICON_COLOR_DISABLED);

        if (hovered) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }
}
