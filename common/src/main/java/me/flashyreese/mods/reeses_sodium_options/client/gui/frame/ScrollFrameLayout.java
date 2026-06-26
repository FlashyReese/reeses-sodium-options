package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;

public final class ScrollFrameLayout {
    private static final int SCROLL_BAR_SIZE = 10;
    private static final int SCROLL_BAR_GAP = 1;
    private static final int SCROLL_BAR_RESERVED_SIZE = SCROLL_BAR_SIZE + SCROLL_BAR_GAP;

    private final LayoutBounds viewport;
    private final int contentWidth;
    private final int contentHeight;
    private final boolean canScrollHorizontal;
    private final boolean canScrollVertical;

    private ScrollFrameLayout(LayoutBounds viewport, int contentWidth, int contentHeight, boolean canScrollHorizontal, boolean canScrollVertical) {
        this.viewport = viewport;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.canScrollHorizontal = canScrollHorizontal;
        this.canScrollVertical = canScrollVertical;
    }

    public static ScrollFrameLayout create(LayoutBounds frameBounds, LayoutBounds contentBounds) {
        boolean canScrollHorizontal = contentBounds.getLimitX() > frameBounds.getLimitX();
        boolean canScrollVertical = contentBounds.getLimitY() > frameBounds.getLimitY();
        int viewportWidth = frameBounds.width() - (canScrollVertical ? SCROLL_BAR_RESERVED_SIZE : 0);
        int viewportHeight = frameBounds.height() - (canScrollHorizontal ? SCROLL_BAR_RESERVED_SIZE : 0);
        int contentWidth = contentBounds.width();
        int contentHeight = contentBounds.height();

        if (canScrollHorizontal && !canScrollVertical) {
            contentHeight -= SCROLL_BAR_RESERVED_SIZE;
        } else if (canScrollVertical && !canScrollHorizontal) {
            contentWidth -= SCROLL_BAR_RESERVED_SIZE;
        }

        return new ScrollFrameLayout(
                new LayoutBounds(frameBounds.x(), frameBounds.y(), Math.max(0, viewportWidth), Math.max(0, viewportHeight)),
                Math.max(0, contentWidth),
                Math.max(0, contentHeight),
                canScrollHorizontal,
                canScrollVertical
        );
    }

    public LayoutBounds viewport() {
        return this.viewport;
    }

    public int contentWidth() {
        return this.contentWidth;
    }

    public int contentHeight() {
        return this.contentHeight;
    }

    public boolean hasScrollBars() {
        return this.canScrollHorizontal || this.canScrollVertical;
    }

    public boolean canScrollHorizontal() {
        return this.canScrollHorizontal;
    }

    public boolean canScrollVertical() {
        return this.canScrollVertical;
    }

    public LayoutBounds horizontalScrollBarBounds() {
        return new LayoutBounds(this.viewport.x(), this.viewport.getLimitY() + SCROLL_BAR_GAP, this.viewport.width(), SCROLL_BAR_SIZE);
    }

    public LayoutBounds verticalScrollBarBounds() {
        return new LayoutBounds(this.viewport.getLimitX() + SCROLL_BAR_GAP, this.viewport.y(), SCROLL_BAR_SIZE, this.viewport.height());
    }

    public boolean overlapsViewport(LayoutBounds dim) {
        return this.viewport.overlaps(dim);
    }

    public int verticalScrollOffsetToInclude(LayoutBounds dim, int currentOffset) {
        return scrollIntoViewOffset(this.viewport, dim, currentOffset);
    }

    public static int scrollIntoViewOffset(LayoutBounds viewport, LayoutBounds dim, int currentOffset) {
        if (dim.y() <= viewport.y()) {
            return currentOffset + dim.y() - viewport.y();
        }

        if (dim.getLimitY() >= viewport.getLimitY()) {
            return currentOffset + dim.getLimitY() - viewport.getLimitY();
        }

        return currentOffset;
    }
}
