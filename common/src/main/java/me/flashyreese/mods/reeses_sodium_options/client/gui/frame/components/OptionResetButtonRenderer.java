package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionStateProvider;
import net.caffeinemc.mods.sodium.client.config.structure.Config;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public final class OptionResetButtonRenderer {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("reeses-sodium-options", "textures/gui/reset_to_default.png");
    private static final int ICON_SIZE = 10;
    private static final int BACKGROUND = 0x66000000;
    private static final int BACKGROUND_HOVERED = 0xA0000000;
    private static final int BORDER_FOCUSED = 0xFFFFFFFF;
    private static final int ICON_COLOR = 0xFFFFFFFF;

    private OptionResetButtonRenderer() {
    }

    public static boolean isActive(StatefulOption<?> option) {
        return ReeseSodiumOptionsConfig.config().isRsoResetButtonOverlay()
                && canReset(option);
    }

    public static boolean canReset(StatefulOption<?> option) {
        if (!option.isEnabled()) {
            return false;
        }

        Config config = getParentConfig(option);

        return config != null
                && !Objects.equals(option.getValidatedValue(), option.getDefaultValue().get(config));
    }

    public static int getReservedWidth(Dim2i controlDim, StatefulOption<?> option) {
        return isActive(option) ? controlDim.height() : 0;
    }

    public static boolean isMouseOver(Dim2i controlDim, StatefulOption<?> option, double mouseX, double mouseY, boolean undoButtonVisible) {
        return isActive(option) && getButtonDim(controlDim, undoButtonVisible).containsCursor(mouseX, mouseY);
    }

    public static void render(GuiGraphics guiGraphics, Dim2i controlDim, StatefulOption<?> option, int mouseX, int mouseY, boolean focused, boolean undoButtonVisible) {
        if (!isActive(option)) {
            return;
        }

        Dim2i buttonDim = getButtonDim(controlDim, undoButtonVisible);
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

    public static Dim2i getButtonDim(Dim2i controlDim, boolean undoButtonVisible) {
        int size = controlDim.height();
        int x = controlDim.getLimitX() - size - (undoButtonVisible ? size : 0);

        return new Dim2i(x, controlDim.y(), size, size);
    }

    public static void resetToDefault(StatefulOption<?> option) {
        option.resetToDefault();
    }

    private static Config getParentConfig(StatefulOption<?> option) {
        if (!(option instanceof OptionStateProvider stateProvider)) {
            return null;
        }

        return stateProvider.rso$getParentConfig();
    }

    private static void drawBorder(GuiGraphics guiGraphics, Dim2i dim, int color) {
        guiGraphics.fill(dim.x(), dim.y(), dim.getLimitX(), dim.y() + 1, color);
        guiGraphics.fill(dim.x(), dim.getLimitY() - 1, dim.getLimitX(), dim.getLimitY(), color);
        guiGraphics.fill(dim.x(), dim.y(), dim.x() + 1, dim.getLimitY(), color);
        guiGraphics.fill(dim.getLimitX() - 1, dim.y(), dim.getLimitX(), dim.getLimitY(), color);
    }
}
