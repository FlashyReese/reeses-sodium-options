package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

public class TabHeaderComponent extends AbstractWidget {

    // tweak these to taste
    private static final float SCROLL_SPEED_PX_PER_SEC = 10f;
    private static final int TEXT_PADDING_RIGHT = 2;
    private static final long DWELL_MS = 1000;

    private final ModOptions modOptions;

    public TabHeaderComponent(Dim2i dim, ModOptions modOptions) {
        super(dim);
        this.modOptions = modOptions;
    }

    public void applyScissor(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, Runnable action) {
        guiGraphics.enableScissor(x, y, x + width, y + height);
        action.run();
        guiGraphics.disableScissor();
    }

    private void drawScrollingString(GuiGraphicsExtractor g, String text, int x, int y, int color, int availableWidth) {
        Font font = Minecraft.getInstance().font;

        if (availableWidth <= 0 || text == null || text.isEmpty()) return;

        int textWidth = font.width(text);
        int lineHeight = font.lineHeight;

        if (textWidth <= availableWidth) {
            g.text(font, text, x, y, color, false);
            return;
        }

        int overflow = textWidth - availableWidth;

        long nowMs = Util.getMillis();

        // Duration (in ms) to traverse overflow pixels at the chosen speed
        double speedPxPerMs = SCROLL_SPEED_PX_PER_SEC / 1000.0;
        long travelMs = Math.max(1L, (long) Math.ceil(overflow / speedPxPerMs));

        // Full cycle: dwell(start) + forward + dwell(end) + back
        long cycleMs = DWELL_MS + travelMs + DWELL_MS + travelMs;

        long t = nowMs % cycleMs;

        double offset; // [0..overflow]

        if (t < DWELL_MS) {
            // pause at start
            offset = 0.0;
        } else if (t < DWELL_MS + travelMs) {
            // scroll forward 0 -> overflow
            long tt = t - DWELL_MS;
            offset = Math.min(overflow, tt * speedPxPerMs);
        } else if (t < DWELL_MS + travelMs + DWELL_MS) {
            // pause at end
            offset = overflow;
        } else {
            // scroll back overflow -> 0
            long tt = t - (DWELL_MS + travelMs + DWELL_MS);
            offset = Math.max(0.0, overflow - tt * speedPxPerMs);
        }

        this.applyScissor(g, x, y, availableWidth, lineHeight, () -> g.text(font, text, (int) (x - offset), y, color, false));
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
        this.applyScissor(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), () -> {
            this.drawRect(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), ColorARGB.pack(0, 0, 0, 245));
            this.drawRect(guiGraphics, this.getX(), this.getLimitY() - 1, this.getLimitX(), this.getLimitY(), modOptions.theme().themeLighter);
            int xOffset;
            if (this.modOptions.icon() == null) {
                xOffset = 2;
            } else {
                VideoSettingsScreen.renderIconWithSpacing(guiGraphics, this.modOptions.icon(), modOptions.theme().themeLighter, modOptions.iconMonochrome(), this.getX(), this.getY(), this.getHeight(), 2);
                xOffset = this.getHeight() + 2;
            }

            int textX = this.getX() + xOffset;
            int available = (this.getX() + this.getWidth()) - textX - TEXT_PADDING_RIGHT;

            drawScrollingString(guiGraphics, this.modOptions.name(), textX, this.getY() + 2, modOptions.theme().themeLighter, available);
            drawScrollingString(guiGraphics, this.modOptions.version(), textX, this.getY() + 12, modOptions.theme().themeDarker, available);
        });
    }
}
