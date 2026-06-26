package me.flashyreese.mods.reeses_sodium_options.client.gui.widget;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.IconRenderer;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class TabHeaderWidget extends BaseWidget {

    // tweak these to taste
    private static final float SCROLL_SPEED_PX_PER_SEC = 10f;
    private static final int TEXT_PADDING_RIGHT = 2;
    private static final long DWELL_MS = 1000;
    private static final int HEADER_BACKGROUND = 0xBE000000;
    private static final int HEADER_BACKGROUND_HOVERED = 0xE0000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xFFAAAAAA;
    private static final int TEXT_X = 5;
    private static final int TEXT_X_WITH_ICON = 20;
    private static final int ICON_SPACING = 4;

    private final ModOptions modOptions;
    private final @Nullable Runnable action;
    private boolean selected;

    public TabHeaderWidget(LayoutBounds dim, ModOptions modOptions) {
        this(dim, modOptions, null, false);
    }

    public TabHeaderWidget(LayoutBounds dim, ModOptions modOptions, @Nullable Runnable action, boolean selected) {
        super(dim);
        this.modOptions = modOptions;
        this.action = action;
        this.selected = selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void applyScissor(GuiGraphics guiGraphics, int x, int y, int width, int height, Runnable action) {
        guiGraphics.enableScissor(x, y, x + width, y + height);
        action.run();
        guiGraphics.disableScissor();
    }

    private void drawScrollingString(GuiGraphics g, String text, int x, int y, int color, int availableWidth) {
        Font font = Minecraft.getInstance().font;

        if (availableWidth <= 0 || text == null || text.isEmpty()) return;

        int textWidth = font.width(text);
        int lineHeight = font.lineHeight;

        if (textWidth <= availableWidth) {
            g.drawString(font, text, x, y, color, false);
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

        this.applyScissor(g, x, y, availableWidth, lineHeight, () -> g.drawString(font, text, (int) (x - offset), y, color, false));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int i, int j, float f) {
        this.hovered = this.action != null && this.isMouseOver(i, j);
        this.applyScissor(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), () -> {
            this.drawRect(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), this.hovered ? HEADER_BACKGROUND_HOVERED : HEADER_BACKGROUND);
            if (this.selected) {
                this.drawRect(guiGraphics, this.getX(), this.getY(), this.getX() + 2, this.getLimitY(), TEXT_COLOR);
            }
            int xOffset;
            if (!ReeseSodiumOptionsConfig.config().isTabHeaderIcons() || this.modOptions.icon() == null) {
                xOffset = TEXT_X;
            } else {
                IconRenderer.renderIconWithSpacing(guiGraphics, this.modOptions.icon(), TEXT_COLOR, modOptions.iconMonochrome(), this.getX(), this.getY(), this.getHeight(), ICON_SPACING);
                xOffset = TEXT_X_WITH_ICON;
            }

            int textX = this.getX() + xOffset;
            int available = (this.getX() + this.getWidth()) - textX - TEXT_PADDING_RIGHT;

            if (ReeseSodiumOptionsConfig.config().isTabHeaderVersionLabels()) {
                drawScrollingString(guiGraphics, this.modOptions.name(), textX, this.getY() + 2, TEXT_COLOR, available);
                drawScrollingString(guiGraphics, this.modOptions.version(), textX, this.getY() + 12, SECONDARY_TEXT_COLOR, available);
            } else {
                int centeredY = this.getY() + Math.ceilDiv(this.getHeight() - Minecraft.getInstance().font.lineHeight, 2);
                drawScrollingString(guiGraphics, this.modOptions.name(), textX, centeredY, TEXT_COLOR, available);
            }

            if (this.shouldRenderFocusBorder()) {
                this.drawBorder(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), GuiThemes.OPTION_FOCUS_BORDER);
            }
        });

        if (this.hovered) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (this.action != null && event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
            this.action.run();
            this.playClickSound();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.action != null && this.isFocused() && event.isSelection()) {
            this.action.run();
            this.playClickSound();
            return true;
        }

        return false;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent navigation) {
        if (this.action == null || this.isFocused()) {
            return null;
        }

        return ComponentPath.leaf(this);
    }

    @Override
    public boolean isActive() {
        return this.action != null;
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput builder) {
        if (this.action == null) {
            return;
        }

        Component label = Component.literal(this.modOptions.name());
        if (ReeseSodiumOptionsConfig.config().isTabHeaderVersionLabels()) {
            label = CommonComponents.optionNameValue(label, Component.literal(this.modOptions.version()));
        }

        this.addButtonNarration(builder, label);
    }
}
