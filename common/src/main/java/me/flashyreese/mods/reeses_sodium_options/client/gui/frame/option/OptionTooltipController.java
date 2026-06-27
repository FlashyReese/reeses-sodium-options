package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class OptionTooltipController {
    private static final int DEFAULT_TOOLTIP_BORDER_COLOR = 0xFF94E4D3;
    private static final int TEXT_PADDING = 3;
    private static final int BOX_PADDING = 3;
    private static final int LINE_HEIGHT = 12;
    private static final float TOOLTIP_Z_OFFSET = 400.0F;

    private final LayoutBounds viewportBounds;
    private final ModOptions modOptions;
    private final BoxRenderer boxRenderer;
    private long targetStartTime;
    private @Nullable OptionRow targetElement;

    OptionTooltipController(LayoutBounds viewportBounds, ModOptions modOptions, BoxRenderer boxRenderer) {
        this.viewportBounds = viewportBounds;
        this.modOptions = modOptions;
        this.boxRenderer = boxRenderer;
    }

    void render(GuiGraphics guiGraphics, List<OptionRow> optionRows, int mouseX, int mouseY) {
        OptionRow targetElement = this.findTargetOptionRow(optionRows, mouseX, mouseY);
        if (targetElement != null && this.targetElement == targetElement) {
            if (this.targetStartTime == 0) {
                this.targetStartTime = System.currentTimeMillis();
            }

            this.renderTooltip(guiGraphics, targetElement);
        } else {
            this.targetStartTime = 0;
            this.targetElement = targetElement;
        }
    }

    private @Nullable OptionRow findTargetOptionRow(List<OptionRow> optionRows, int mouseX, int mouseY) {
        OptionRow hoveredElement = this.findHoveredOptionRow(optionRows, mouseX, mouseY);
        if (hoveredElement != null) {
            return hoveredElement;
        }

        return this.findFocusedOptionRow(optionRows);
    }

    private @Nullable OptionRow findHoveredOptionRow(List<OptionRow> optionRows, int mouseX, int mouseY) {
        if (!this.viewportBounds.contains(mouseX, mouseY)) {
            return null;
        }

        return optionRows.stream()
                .filter(this::isVisibleOptionRow)
                .filter(optionRow -> optionRow.isMouseOver(mouseX, mouseY))
                .findFirst()
                .orElse(null);
    }

    private @Nullable OptionRow findFocusedOptionRow(List<OptionRow> optionRows) {
        if (!BaseWidget.isKeyboardFocusVisible()) {
            return null;
        }

        return optionRows.stream()
                .filter(this::isVisibleOptionRow)
                .filter(OptionRow::isFocused)
                .findFirst()
                .orElse(null);
    }

    private boolean isVisibleOptionRow(OptionRow optionRow) {
        return this.viewportBounds.overlaps(optionRow.getDimensions());
    }

    private void renderTooltip(GuiGraphics guiGraphics, OptionRow element) {
        if (this.targetStartTime + ReeseSodiumOptionsConfig.config().getTooltipDelayMs() > System.currentTimeMillis()) {
            return;
        }

        LayoutBounds dim = element.getDimensions();
        int boxWidth = dim.width();
        int boxY = dim.getLimitY();
        int boxX = dim.x();
        List<FormattedCharSequence> tooltip = this.buildTooltip(element.getOption(), boxWidth);

        if (tooltip.isEmpty()) {
            return;
        }

        int boxHeight = (tooltip.size() * LINE_HEIGHT) + BOX_PADDING;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.viewportBounds.getLimitY();

        if (boxYLimit > boxYCutoff) {
            boxY -= boxHeight + dim.height();
        }

        if (boxY < 0) {
            boxY = dim.getLimitY();
        }

        guiGraphics.flush();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, TOOLTIP_Z_OFFSET);
        try {
            this.boxRenderer.drawRect(guiGraphics, boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);
            int borderColor = ReeseSodiumOptionsConfig.config().isColorThemes() && ReeseSodiumOptionsConfig.config().isThemedTooltipBorders()
                    ? GuiThemes.fromSodium(this.modOptions.theme()).theme
                    : DEFAULT_TOOLTIP_BORDER_COLOR;
            this.boxRenderer.drawBorder(guiGraphics, boxX, boxY, boxX + boxWidth, boxY + boxHeight, borderColor);

            for (int i = 0; i < tooltip.size(); i++) {
                guiGraphics.drawString(Minecraft.getInstance().font, tooltip.get(i), boxX + TEXT_PADDING, boxY + TEXT_PADDING + (i * LINE_HEIGHT), 0xFFFFFFFF, true);
            }
        } finally {
            guiGraphics.flush();
            guiGraphics.pose().popPose();
        }
    }

    private List<FormattedCharSequence> buildTooltip(Option option, int boxWidth) {
        List<FormattedCharSequence> tooltip = new ArrayList<>();

        if (ReeseSodiumOptionsConfig.config().isTooltipOptionIds() && option instanceof OptionExtended optionExtended) {
            tooltip.add(Language.getInstance().getVisualOrder(Component.literal(optionExtended.rso$getId().toString()).withStyle(ChatFormatting.GRAY)));
            tooltip.add(Language.getInstance().getVisualOrder(Component.literal("")));
        }

        tooltip.addAll(Minecraft.getInstance().font.split(option.getTooltip(), boxWidth - (TEXT_PADDING * 2)));

        OptionImpact impact = option.getImpact();
        if (impact != null) {
            tooltip.add(Language.getInstance().getVisualOrder(Component.translatable("sodium.options.performance_impact_string", impact.getName()).withStyle(ChatFormatting.GRAY)));
        }

        return tooltip;
    }

    interface BoxRenderer {
        void drawRect(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color);

        void drawBorder(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color);
    }
}
