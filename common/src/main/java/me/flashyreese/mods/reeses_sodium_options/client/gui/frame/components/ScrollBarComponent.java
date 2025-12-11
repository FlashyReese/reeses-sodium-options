package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ScrollBarComponent extends AbstractWidget {

    protected static final int SCROLL_STEP = 6;

    private final ScrollDirection mode;
    private final int contentLength;
    private final int visibleAreaLength;
    private final int maxContentOffset;
    private final Consumer<Integer> offsetChangeListener;
    private final Dim2i extraScrollArea;
    private int offset = 0;
    private boolean isDragging;
    private Dim2i scrollThumb = null;
    private int scrollThumbClickOffset;

    public ScrollBarComponent(Dim2i trackArea, ScrollDirection scrollDirection, int contentLength, int visibleAreaLength, Consumer<Integer> offsetChangeListener) {
        this(trackArea, scrollDirection, contentLength, visibleAreaLength, offsetChangeListener, null);
    }

    public ScrollBarComponent(Dim2i scrollBarArea, ScrollDirection scrollDirection, int contentLength, int visibleAreaLength, Consumer<Integer> offsetChangeListener, Dim2i extraScrollArea) {
        super(scrollBarArea);
        this.mode = scrollDirection;
        this.contentLength = contentLength;
        this.visibleAreaLength = visibleAreaLength;
        this.offsetChangeListener = offsetChangeListener;
        this.maxContentOffset = this.contentLength - this.visibleAreaLength;
        this.extraScrollArea = extraScrollArea;
        this.updateThumbLocation();
    }

    public void updateThumbLocation() {
        int trackSize = (this.mode == ScrollDirection.VERTICAL ? this.getHeight() : this.getWidth() - 6);
        int scrollThumbLength = (this.visibleAreaLength * trackSize) / this.contentLength;
        int maximumScrollThumbOffset = this.visibleAreaLength - scrollThumbLength;
        int scrollThumbOffset = (this.offset * maximumScrollThumbOffset) / this.maxContentOffset;
        this.scrollThumb = new Dim2i(
                this.getX() + 2 + (this.mode == ScrollDirection.HORIZONTAL ? scrollThumbOffset : 0),
                this.getY() + 2 + (this.mode == ScrollDirection.VERTICAL ? scrollThumbOffset : 0),
                (this.mode == ScrollDirection.VERTICAL ? this.getWidth() : scrollThumbLength) - 4,
                (this.mode == ScrollDirection.VERTICAL ? scrollThumbLength : this.getHeight()) - 4
        );
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.drawBorder(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), 0xFFAAAAAA);
        this.drawRect(guiGraphics, this.scrollThumb.x(), this.scrollThumb.y(), this.scrollThumb.getLimitX(), this.scrollThumb.getLimitY(), 0xFFAAAAAA);
        if (this.isFocused()) {
            this.drawBorder(guiGraphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), -1);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (this.isMouseOver(event.x(), event.y())) {
            if (this.scrollThumb.containsCursor(event.x(), event.y())) {
                this.scrollThumbClickOffset = (int) (this.mode == ScrollDirection.VERTICAL ? event.y() - this.scrollThumb.getCenterY() : event.x() - this.scrollThumb.getCenterX());
                this.isDragging = true;
            } else {
                int thumbLength = this.mode == ScrollDirection.VERTICAL ? this.scrollThumb.height() : this.scrollThumb.width();
                int trackLength = this.mode == ScrollDirection.VERTICAL ? this.getHeight() : this.getWidth();
                int value = (int) (((this.mode == ScrollDirection.VERTICAL ? event.y() - this.getY() : event.x() - this.getX()) - thumbLength / 2.0) * this.maxContentOffset / (trackLength - thumbLength));
                this.setOffset(value);
                this.isDragging = false;
            }
            return true;
        }
        this.isDragging = false;
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.isDragging = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.isDragging) {
            int thumbLength = this.mode == ScrollDirection.VERTICAL ? this.scrollThumb.height() : this.scrollThumb.width();
            int trackLength = this.mode == ScrollDirection.VERTICAL ? this.getHeight() : this.getWidth();
            int value = (int) (((this.mode == ScrollDirection.VERTICAL ? event.y() : event.x()) - this.scrollThumbClickOffset - (this.mode == ScrollDirection.VERTICAL ? this.getY() : this.getX()) - thumbLength / 2.0) * this.maxContentOffset / (trackLength - thumbLength));
            this.setOffset(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isMouseOver(mouseX, mouseY) || this.extraScrollArea != null && this.extraScrollArea.containsCursor(mouseX, mouseY)) {
            this.setOffset(this.offset - (int) verticalAmount * SCROLL_STEP);
            return true;
        }
        return false;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int value) {
        this.offset = Mth.clamp(value, 0, this.maxContentOffset);
        this.updateThumbLocation();
        this.offsetChangeListener.accept(this.offset);
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.isFocused()) {
            return false;
        }

        int newOffset = switch (event.key()) {
            case GLFW.GLFW_KEY_UP -> this.getOffset() - SCROLL_STEP;
            case GLFW.GLFW_KEY_DOWN -> this.getOffset() + SCROLL_STEP;
            case GLFW.GLFW_KEY_LEFT ->
                    this.mode == ScrollDirection.HORIZONTAL ? this.getOffset() - SCROLL_STEP : this.getOffset();
            case GLFW.GLFW_KEY_RIGHT ->
                    this.mode == ScrollDirection.HORIZONTAL ? this.getOffset() + SCROLL_STEP : this.getOffset();
            default -> this.getOffset();
        };

        if (newOffset != this.getOffset()) {
            this.setOffset(newOffset);
            return true;
        }

        return false;
    }

    public enum ScrollDirection {
        HORIZONTAL,
        VERTICAL
    }
}