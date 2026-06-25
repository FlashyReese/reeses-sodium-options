package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.ScrollBarWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.OptionRow;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.Holder;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class ScrollableFrame extends AbstractFrame {

    protected final LayoutBounds frameOrigin;
    protected final AbstractFrame frame;
    private final int contentFrameWidth;
    private final Holder<Integer> verticalScrollBarOffsetHolder;
    private final Holder<Integer> horizontalScrollBarOffsetHolder;

    private ScrollFrameLayout scrollLayout;
    private ScrollBarWidget verticalScrollBar = null;
    private ScrollBarWidget horizontalScrollBar = null;

    public ScrollableFrame(LayoutBounds dim, Screen screen, ModOptions modOptions, AbstractFrame frame, boolean renderOutline, Holder<Integer> verticalScrollBarOffset, Holder<Integer> horizontalScrollBarOffset) {
        super(dim, screen, renderOutline, modOptions);
        this.frame = frame;
        this.frameOrigin = new LayoutBounds(frame.getDimensions().x(), frame.getDimensions().y(), 0, 0);
        this.contentFrameWidth = frame.getDimensions().width();
        this.verticalScrollBarOffsetHolder = verticalScrollBarOffset;
        this.horizontalScrollBarOffsetHolder = horizontalScrollBarOffset;
        this.setupFrame();
        this.buildFrame();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setupFrame() {
        this.horizontalScrollBar = null;
        this.verticalScrollBar = null;
        this.scrollLayout = ScrollFrameLayout.create(this.getFrameDim(), this.contentFrameDim());
        this.applyContentFrameBounds(0, 0);

        if (this.scrollLayout.canScrollHorizontal()) {
            this.horizontalScrollBar = new ScrollBarWidget(this.scrollLayout.horizontalScrollBarBounds(), ScrollBarWidget.ScrollDirection.HORIZONTAL, this.contentFrameDim().width(), this.scrollLayout.viewport().width(), offset -> {
                this.applyContentFrameBounds(offset, this.verticalOffset());
                this.horizontalScrollBarOffsetHolder.set(offset);
            });
            this.horizontalScrollBar.setOffset(this.horizontalScrollBarOffsetHolder.getOrDefault(0));
        }

        if (this.scrollLayout.canScrollVertical()) {
            this.verticalScrollBar = new ScrollBarWidget(this.scrollLayout.verticalScrollBarBounds(), ScrollBarWidget.ScrollDirection.VERTICAL, this.contentFrameDim().height(), this.scrollLayout.viewport().height(), offset -> {
                this.applyContentFrameBounds(this.horizontalOffset(), offset);
                this.verticalScrollBarOffsetHolder.set(offset);
            }, this.scrollLayout.viewport());
            this.verticalScrollBar.setOffset(this.verticalScrollBarOffsetHolder.getOrDefault(0));
        }
    }

    public void rebuildContentFrame() {
        this.frame.rebuildFrameContent();
        this.setupFrame();
        this.buildFrame();
    }

    @Override
    public void buildFrame() {
        this.children.clear();
        this.optionRows.clear();

        this.applyStoredScrollOffsets();

        if (this.scrollLayout.canScrollHorizontal()) {
            this.horizontalScrollBar.updateThumbLocation();
        }

        if (this.scrollLayout.canScrollVertical()) {
            this.verticalScrollBar.updateThumbLocation();
        }

        this.applyContentFrameBounds(this.horizontalOffset(), this.verticalOffset());

        if (this.scrollLayout.canScrollHorizontal()) {
            this.children.add(this.horizontalScrollBar);
        }

        if (this.scrollLayout.canScrollVertical()) {
            this.children.add(this.verticalScrollBar);
        }

        this.frame.buildFrame();
        this.children.add(this.frame);
        super.buildFrame();

        this.frame.registerFocusListener(element -> {
            if (element instanceof BaseWidget widget && this.scrollLayout.canScrollVertical()) {
                LayoutBounds dim = widget.getDimensions();
                this.verticalScrollBar.setOffset(this.scrollLayout.verticalScrollOffsetToInclude(dim, this.verticalScrollBar.getOffset()));
            }
        });
    }

    private void applyStoredScrollOffsets() {
        if (this.scrollLayout.canScrollHorizontal()) {
            this.horizontalScrollBar.setOffset(this.horizontalScrollBarOffsetHolder.getOrDefault(0));
        }

        if (this.scrollLayout.canScrollVertical()) {
            this.verticalScrollBar.setOffset(this.verticalScrollBarOffsetHolder.getOrDefault(0));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.scrollLayout.hasScrollBars()) {
            if (this.renderOutline) {
                this.drawBorder(guiGraphics, this.getFrameDim().x(), this.getFrameDim().y(), this.getFrameDim().getLimitX(), this.getFrameDim().getLimitY(), 0xFFAAAAAA);
            }
            LayoutBounds viewport = this.scrollLayout.viewport();
            this.applyScissor(guiGraphics, viewport.x(), viewport.y(), viewport.width(), viewport.height(), () -> super.render(guiGraphics, mouseX, mouseY, delta));
        } else {
            super.render(guiGraphics, mouseX, mouseY, delta);
        }

        if (this.scrollLayout.canScrollHorizontal()) {
            this.horizontalScrollBar.render(guiGraphics, mouseX, mouseY, delta);
        }

        if (this.scrollLayout.canScrollVertical()) {
            this.verticalScrollBar.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick) || (this.scrollLayout.canScrollHorizontal() && this.horizontalScrollBar.mouseClicked(event, doubleClick)) || (this.scrollLayout.canScrollVertical() && this.verticalScrollBar.mouseClicked(event, doubleClick));
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
        return super.mouseDragged(event, deltaX, deltaY) || (this.scrollLayout.canScrollHorizontal() && this.horizontalScrollBar.mouseDragged(event, deltaX, deltaY)) || (this.scrollLayout.canScrollVertical() && this.verticalScrollBar.mouseDragged(event, deltaX, deltaY));
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return super.mouseReleased(event) || (this.scrollLayout.canScrollHorizontal() && this.horizontalScrollBar.mouseReleased(event)) || (this.scrollLayout.canScrollVertical() && this.verticalScrollBar.mouseReleased(event));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) || (this.scrollLayout.canScrollHorizontal() && this.horizontalScrollBar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) || (this.scrollLayout.canScrollVertical() && this.verticalScrollBar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount));
    }

    public boolean scrollToStart() {
        if (!this.scrollLayout.canScrollVertical()) {
            return false;
        }

        return this.setVerticalOffset(0);
    }

    public boolean scrollToEnd() {
        if (!this.scrollLayout.canScrollVertical()) {
            return false;
        }

        return this.setVerticalOffset(Integer.MAX_VALUE);
    }

    public boolean scrollPage(int direction) {
        if (!this.scrollLayout.canScrollVertical() || direction == 0) {
            return false;
        }

        int pageSize = Math.max(ScrollBarWidget.SCROLL_STEP, this.scrollLayout.viewport().height() - 18);

        return this.setVerticalOffset(this.verticalScrollBar.getOffset() + direction * pageSize);
    }

    public @Nullable OptionRow findFirstOptionRow() {
        return this.findFirstOptionRow(control -> true);
    }

    public @Nullable OptionRow findLastOptionRow() {
        return this.findLastOptionRow(control -> true);
    }

    public @Nullable OptionRow findFirstVisibleOptionRow() {
        return this.findFirstOptionRow(this::isOptionRowVisible);
    }

    public @Nullable OptionRow findLastVisibleOptionRow() {
        return this.findLastOptionRow(this::isOptionRowVisible);
    }

    private boolean setVerticalOffset(int offset) {
        int previousOffset = this.verticalScrollBar.getOffset();
        this.verticalScrollBar.setOffset(offset);

        return this.verticalScrollBar.getOffset() != previousOffset;
    }

    private boolean isOptionRowVisible(OptionRow optionRow) {
        if (!this.scrollLayout.canScrollVertical()) {
            return true;
        }

        LayoutBounds dim = optionRow.getDimensions();

        return this.scrollLayout.overlapsViewport(dim);
    }

    private LayoutBounds contentFrameDim() {
        return new LayoutBounds(this.frameOrigin.x(), this.frameOrigin.y(), this.contentFrameWidth, this.frame.getDimensions().height());
    }

    private int horizontalOffset() {
        return this.horizontalScrollBar == null ? 0 : this.horizontalScrollBar.getOffset();
    }

    private int verticalOffset() {
        return this.verticalScrollBar == null ? 0 : this.verticalScrollBar.getOffset();
    }

    private void applyContentFrameBounds(int horizontalOffset, int verticalOffset) {
        this.frame.updateFrameDim(new LayoutBounds(
                this.frameOrigin.x() - horizontalOffset,
                this.frameOrigin.y() - verticalOffset,
                this.scrollLayout.contentWidth(),
                this.scrollLayout.contentHeight()
        ));
        this.collectOptionRows();
    }

    public static class Builder {
        private boolean renderOutline = false;
        private LayoutBounds dim = null;
        private AbstractFrame frame = null;
        private Holder<Integer> verticalScrollBarOffset = new Holder<>(0);
        private Holder<Integer> horizontalScrollBarOffset = new Holder<>(0);
        private Screen screen;
        private ModOptions modOptions;

        public Builder withDimension(LayoutBounds dim) {
            this.dim = dim;
            return this;
        }

        public Builder withRenderOutline(boolean state) {
            this.renderOutline = state;
            return this;
        }

        public Builder withVerticalScrollBarOffset(Holder<Integer> verticalScrollBarOffset) {
            this.verticalScrollBarOffset = verticalScrollBarOffset;
            return this;
        }

        public Builder withHorizontalScrollBarOffset(Holder<Integer> horizontalScrollBarOffset) {
            this.horizontalScrollBarOffset = horizontalScrollBarOffset;
            return this;
        }

        public Builder withFrame(AbstractFrame frame) {
            this.frame = frame;
            return this;
        }

        public Builder withScreen(Screen screen) {
            this.screen = screen;
            return this;
        }

        public Builder withModOptions(ModOptions modConfig) {
            this.modOptions = modConfig;
            return this;
        }

        public ScrollableFrame build() {
            return new ScrollableFrame(this.dim, this.screen, this.modOptions, this.frame, this.renderOutline, this.verticalScrollBarOffset, this.horizontalScrollBarOffset);
        }
    }
}
