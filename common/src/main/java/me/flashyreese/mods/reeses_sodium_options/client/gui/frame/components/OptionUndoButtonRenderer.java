package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class OptionUndoButtonRenderer {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("sodium", "textures/gui/reset_button.png");
    private static final int ICON_SIZE = 10;
    private static final int BACKGROUND = 0x66000000;
    private static final int BACKGROUND_HOVERED = 0xA0000000;
    private static final int BORDER_FOCUSED = 0xFFFFFFFF;
    private static final int ICON_COLOR = 0xFFFF8D30;

    public static boolean isActive(StatefulOption<?> option) {
        return ReeseSodiumOptionsConfig.config().isUndoButtonOverlay()
                && option.isEnabled()
                && option.hasChanged();
    }

    public static int getReservedWidth(Dim2i controlDim, StatefulOption<?> option) {
        return isActive(option) ? controlDim.height() : 0;
    }

    public static boolean isMouseOver(Dim2i controlDim, StatefulOption<?> option, double mouseX, double mouseY) {
        return isActive(option) && getButtonDim(controlDim).containsCursor(mouseX, mouseY);
    }

    public static void render(GuiGraphicsExtractor guiGraphics, Dim2i controlDim, StatefulOption<?> option, int mouseX, int mouseY) {
        render(guiGraphics, controlDim, option, mouseX, mouseY, false);
    }

    public static void render(GuiGraphicsExtractor guiGraphics, Dim2i controlDim, StatefulOption<?> option, int mouseX, int mouseY, boolean focused) {
        if (!isActive(option)) {
            return;
        }

        Dim2i buttonDim = getButtonDim(controlDim);
        boolean hovered = buttonDim.containsCursor(mouseX, mouseY);

        guiGraphics.fill(buttonDim.x(), buttonDim.y(), buttonDim.getLimitX(), buttonDim.getLimitY(), hovered ? BACKGROUND_HOVERED : BACKGROUND);
        if (focused) {
            drawBorder(guiGraphics, buttonDim, BORDER_FOCUSED);
        }

        int iconX = buttonDim.getCenterX() - ICON_SIZE / 2;
        int iconY = buttonDim.getCenterY() - ICON_SIZE / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON, iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_COLOR);

        if (hovered) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    public static Dim2i getButtonDim(Dim2i controlDim) {
        int size = controlDim.height();
        int x = controlDim.getLimitX() - size;

        return new Dim2i(x, controlDim.y(), size, size);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void undoChanges(StatefulOption<?> option) {
        ((StatefulOption) option).modifyValue(option.getAppliedValue());
    }

    private static void drawBorder(GuiGraphicsExtractor guiGraphics, Dim2i dim, int color) {
        guiGraphics.fill(dim.x(), dim.y(), dim.getLimitX(), dim.y() + 1, color);
        guiGraphics.fill(dim.x(), dim.getLimitY() - 1, dim.getLimitX(), dim.getLimitY(), color);
        guiGraphics.fill(dim.x(), dim.y(), dim.x() + 1, dim.getLimitY(), color);
        guiGraphics.fill(dim.getLimitX() - 1, dim.y(), dim.getLimitX(), dim.getLimitY(), color);
    }
}
