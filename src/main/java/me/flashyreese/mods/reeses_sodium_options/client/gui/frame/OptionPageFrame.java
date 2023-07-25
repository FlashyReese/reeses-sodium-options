package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.flashyreese.mods.reeses_sodium_options.client.gui.Dim2iExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class OptionPageFrame extends AbstractFrame {
    protected final Dim2i originalDim;
    protected final OptionPage page;
    private long lastTime = 0;
    private ControlElement<?> lastHoveredElement = null;

    public OptionPageFrame(Dim2i dim, boolean renderOutline, OptionPage page) {
        super(dim, renderOutline);
        this.originalDim = new Dim2i(dim.x(), dim.y(), dim.width(), dim.height());
        this.page = page;
        this.setupFrame();
        this.buildFrame();
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public void setupFrame() {
        this.children.clear();
        this.drawable.clear();
        this.controlElements.clear();

        int y = 0;
        if (!this.page.getGroups().isEmpty()) {
            OptionGroup lastGroup = this.page.getGroups().get(this.page.getGroups().size() - 1);

            for (OptionGroup group : this.page.getGroups()) {
                y += group.getOptions().size() * 18;
                if (group != lastGroup) {
                    y += 4;
                }
            }
        }

        ((Dim2iExtended) ((Object) this.dim)).setHeight(y);
        this.page.getGroups().forEach(group -> group.getOptions().forEach(option -> {
            if (option instanceof OptionExtended optionExtended) {
                optionExtended.setParentDimension(this.dim);
            }
        }));
    }

    @Override
    public void buildFrame() {
        if (this.page == null) return;

        this.children.clear();
        this.drawable.clear();
        this.controlElements.clear();

        int y = 0;
        for (OptionGroup group : this.page.getGroups()) {
            // Add each option's control element
            for (Option<?> option : group.getOptions()) {
                Control<?> control = option.getControl();
                ControlElement<?> element = control.createElement(new Dim2i(this.dim.x(), this.dim.y() + y, this.dim.width(), 18));
                this.children.add(element);

                // Move down to the next option
                y += 18;
            }

            // Add padding beneath each option group
            y += 4;
        }

        super.buildFrame();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        ControlElement<?> hoveredElement = this.controlElements.stream()
                .filter(controlElement -> ((Dim2iExtended) (Object) controlElement.getDimensions()).overlapWith(this.originalDim))
                .filter(ControlElement::isHovered)
                .findFirst()
                .orElse(null);
        super.render(matrixStack, mouseX, mouseY, delta);
        if (hoveredElement != null && this.lastHoveredElement == hoveredElement &&
                (this.originalDim.containsCursor(mouseX, mouseY) && hoveredElement.isHovered() && hoveredElement.isMouseOver(mouseX, mouseY))) {
            if (this.lastTime == 0) {
                this.lastTime = System.currentTimeMillis();
            }
            this.renderOptionTooltip(matrixStack, hoveredElement);
        } else {
            this.lastTime = 0;
            this.lastHoveredElement = hoveredElement;
        }
    }

    private void renderOptionTooltip(MatrixStack matrixStack, ControlElement<?> element) {
        if (this.lastTime + 500 > System.currentTimeMillis()) return;

        Dim2i dim = element.getDimensions();

        int textPadding = 3;
        int boxPadding = 3;

        int boxWidth = dim.width();

        //Offset based on mouse position, width and height of content and width and height of the window
        int boxY = dim.getLimitY();
        int boxX = dim.x();

        Option<?> option = element.getOption();
        List<OrderedText> tooltip = new ArrayList<>(MinecraftClient.getInstance().textRenderer.wrapLines(option.getTooltip(), boxWidth - (textPadding * 2)));

        OptionImpact impact = option.getImpact();

        if (impact != null) {
            tooltip.add(Language.getInstance().reorder(Text.translatable("sodium.options.performance_impact_string", impact.getLocalizedName()).formatted(Formatting.GRAY)));
        }

        int boxHeight = (tooltip.size() * 12) + boxPadding;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.originalDim.getLimitY();

        // If the box is going to be cutoff on the Y-axis, move it back up the difference
        if (boxYLimit > boxYCutoff) {
            boxY -= boxHeight + dim.height();
        }

        if (boxY < 0) {
            boxY = dim.getLimitY();
        }

        this.drawRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000);
        this.drawRectOutline(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF94E4D3);

        for (int i = 0; i < tooltip.size(); i++) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrixStack, tooltip.get(i), boxX + textPadding, boxY + textPadding + (i * 12), 0xFFFFFFFF);
        }
    }

    public static class Builder {
        private Dim2i dim;
        private boolean renderOutline;
        private OptionPage page;

        public Builder setDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder shouldRenderOutline(boolean renderOutline) {
            this.renderOutline = renderOutline;
            return this;
        }

        public Builder setOptionPage(OptionPage page) {
            this.page = page;
            return this;
        }

        public OptionPageFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");
            Validate.notNull(this.page, "Option Page must be specified");

            return new OptionPageFrame(this.dim, this.renderOutline, this.page);
        }
    }
}
