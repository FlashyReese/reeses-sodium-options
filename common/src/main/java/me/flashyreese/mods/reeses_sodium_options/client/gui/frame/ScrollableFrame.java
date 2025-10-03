package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.flashyreese.mods.reeses_sodium_options.client.gui.Dim2iExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.ScrollBarComponent;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class ScrollableFrame extends AbstractFrame {

    protected final Dim2i frameOrigin;
    protected final AbstractFrame frame;

    private boolean canScrollHorizontal;
    private boolean canScrollVertical;
    private Dim2i viewPortDimension = null;
    private ScrollBarComponent verticalScrollBar = null;
    private ScrollBarComponent horizontalScrollBar = null;

    public ScrollableFrame(Dim2i dim, AbstractFrame frame, boolean renderOutline, AtomicReference<Integer> verticalScrollBarOffset, AtomicReference<Integer> horizontalScrollBarOffset) {
        super(dim, renderOutline);
        this.frame = frame;
        this.frameOrigin = new Dim2i(frame.dim.x(), frame.dim.y(), 0, 0);
        this.setupFrame(verticalScrollBarOffset, horizontalScrollBarOffset);
        this.buildFrame();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setupFrame(AtomicReference<Integer> verticalScrollBarOffset, AtomicReference<Integer> horizontalScrollBarOffset) {
        int maxWidth = 0;
        int maxHeight = 0;
        if (!((Dim2iExtended) ((Object) this.dim)).canFitDimension(this.frame.dim)) {
            if (this.dim.getLimitX() < this.frame.dim.getLimitX()) {
                int value = this.frame.dim.x() - this.dim.x() + this.frame.dim.width();
                if (maxWidth < value) {
                    maxWidth = value;
                }
            }
            if (this.dim.getLimitY() < this.frame.dim.getLimitY()) {
                int value = this.frame.dim.y() - this.dim.y() + this.frame.dim.height();
                if (maxHeight < value) {
                    maxHeight = value;
                }
            }
        }

        if (maxWidth > 0) {
            this.canScrollHorizontal = true;
        }
        if (maxHeight > 0) {
            this.canScrollVertical = true;
        }

        if (this.canScrollHorizontal && this.canScrollVertical) {
            this.viewPortDimension = new Dim2i(this.dim.x(), this.dim.y(), this.dim.width() - 11, this.dim.height() - 11);
        } else if (this.canScrollHorizontal) {
            this.viewPortDimension = new Dim2i(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height() - 11);
            ((Dim2iExtended) ((Object) this.frame.dim)).setHeight(this.frame.dim.height() - 11); // fixme: don't mutate rather
        } else if (this.canScrollVertical) {
            this.viewPortDimension = new Dim2i(this.dim.x(), this.dim.y(), this.dim.width() - 11, this.dim.height());
            ((Dim2iExtended) ((Object) this.frame.dim)).setWidth(this.frame.dim.width() - 11); // fixme: don't mutate rather
        }

        if (this.canScrollHorizontal) {
            this.horizontalScrollBar = new ScrollBarComponent(new Dim2i(this.viewPortDimension.x(), this.viewPortDimension.getLimitY() + 1, this.viewPortDimension.width(), 10), ScrollBarComponent.ScrollDirection.HORIZONTAL, this.frame.dim.width(), this.viewPortDimension.width(), offset -> {
                //this.buildFrame();
                ((Dim2iExtended) ((Object) this.frame.dim)).setX(this.frameOrigin.x() - this.horizontalScrollBar.getOffset());
                horizontalScrollBarOffset.set(offset);
            });
            this.horizontalScrollBar.setOffset(horizontalScrollBarOffset.get());
        }
        if (this.canScrollVertical) {
            this.verticalScrollBar = new ScrollBarComponent(new Dim2i(this.viewPortDimension.getLimitX() + 1, this.viewPortDimension.y(), 10, this.viewPortDimension.height()), ScrollBarComponent.ScrollDirection.VERTICAL, this.frame.dim.height(), this.viewPortDimension.height(), offset -> {
                //this.buildFrame();
                ((Dim2iExtended) ((Object) this.frame.dim)).setY(this.frameOrigin.y() - this.verticalScrollBar.getOffset());
                verticalScrollBarOffset.set(offset);
            }, this.viewPortDimension);
            this.verticalScrollBar.setOffset(verticalScrollBarOffset.get());
        }
    }

    @Override
    public void buildFrame() {
        this.children.clear();
        this.controlElements.clear();

        if (this.canScrollHorizontal) {
            this.horizontalScrollBar.updateThumbLocation();
        }

        if (this.canScrollVertical) {
            this.verticalScrollBar.updateThumbLocation();
        }

        if (this.canScrollHorizontal) {
            ((Dim2iExtended) ((Object) this.frame.dim)).setX(this.frameOrigin.x() - this.horizontalScrollBar.getOffset());
            this.children.add(this.horizontalScrollBar);
        }

        if (this.canScrollVertical) {
            ((Dim2iExtended) ((Object) this.frame.dim)).setY(this.frameOrigin.y() - this.verticalScrollBar.getOffset());
            this.children.add(this.verticalScrollBar);
        }

        this.frame.buildFrame();
        this.children.add(this.frame);
        super.buildFrame();

        // fixme: Ridiculous hack to snap to focused element
        // for the meanwhile this works until a proper solution is implemented.
        // this shouldn't be hardcoded into scrollable frame
        this.frame.registerFocusListener(element -> {
            if (element instanceof ControlElement<?> controlElement && this.canScrollVertical) {
                Dim2i dim = controlElement.getDimensions();
                int inputOffset = this.verticalScrollBar.getOffset();
                if (dim.y() <= this.viewPortDimension.y()) {
                    inputOffset += dim.y() - this.viewPortDimension.y();
                } else if (dim.getLimitY() >= this.viewPortDimension.getLimitY()) {
                    inputOffset += dim.getLimitY() - this.viewPortDimension.getLimitY();
                }
                this.verticalScrollBar.setOffset(inputOffset);
            }
        });
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.canScrollHorizontal || this.canScrollVertical) {
            if (this.renderOutline) {
                this.drawBorder(guiGraphics, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
            }
            this.applyScissor(guiGraphics, this.viewPortDimension.x(), this.viewPortDimension.y(), this.viewPortDimension.width(), this.viewPortDimension.height(), () -> super.render(guiGraphics, mouseX, mouseY, delta));
        } else {
            super.render(guiGraphics, mouseX, mouseY, delta);
        }

        if (this.canScrollHorizontal) {
            this.horizontalScrollBar.render(guiGraphics, mouseX, mouseY, delta);
        }

        if (this.canScrollVertical) {
            this.verticalScrollBar.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent navigation) {
        //this.snapFocusedInViewport();
        return super.nextFocusPath(navigation);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        return super.mouseClicked(event, bl) || (this.canScrollHorizontal && this.horizontalScrollBar.mouseClicked(event, bl)) || (this.canScrollVertical && this.verticalScrollBar.mouseClicked(event, bl));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return super.mouseDragged(event, deltaX, deltaY) || (this.canScrollHorizontal && this.horizontalScrollBar.mouseDragged(event, deltaX, deltaY)) || (this.canScrollVertical && this.verticalScrollBar.mouseDragged(event, deltaX, deltaY));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event) || (this.canScrollHorizontal && this.horizontalScrollBar.mouseReleased(event)) || (this.canScrollVertical && this.verticalScrollBar.mouseReleased(event));
    }
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) || (this.canScrollHorizontal && this.horizontalScrollBar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) || (this.canScrollVertical && this.verticalScrollBar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount));
    }

    public static class Builder {
        private boolean renderOutline = false;
        private Dim2i dim = null;
        private AbstractFrame frame = null;
        private AtomicReference<Integer> verticalScrollBarOffset = new AtomicReference<>(0);
        private AtomicReference<Integer> horizontalScrollBarOffset = new AtomicReference<>(0);

        public Builder withDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder withRenderOutline(boolean state) {
            this.renderOutline = state;
            return this;
        }

        public Builder withVerticalScrollBarOffset(AtomicReference<Integer> verticalScrollBarOffset) {
            this.verticalScrollBarOffset = verticalScrollBarOffset;
            return this;
        }

        public Builder withHorizontalScrollBarOffset(AtomicReference<Integer> horizontalScrollBarOffset) {
            this.horizontalScrollBarOffset = horizontalScrollBarOffset;
            return this;
        }

        public Builder withFrame(AbstractFrame frame) {
            this.frame = frame;
            return this;
        }

        public ScrollableFrame build() {
            return new ScrollableFrame(this.dim, this.frame, this.renderOutline, this.verticalScrollBarOffset, this.horizontalScrollBarOffset);
        }
    }
}