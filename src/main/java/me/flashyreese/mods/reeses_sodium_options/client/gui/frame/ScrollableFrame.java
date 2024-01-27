package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.flashyreese.mods.reeses_sodium_options.client.gui.Dim2iExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.ScrollBarComponent;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
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

    public static Builder createBuilder() {
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
            this.horizontalScrollBar = new ScrollBarComponent(new Dim2i(this.viewPortDimension.x(), this.viewPortDimension.getLimitY() + 1, this.viewPortDimension.width(), 10), ScrollBarComponent.Mode.HORIZONTAL, this.frame.dim.width(), this.viewPortDimension.width(), offset -> {
                //this.buildFrame();
                ((Dim2iExtended) ((Object) this.frame.dim)).setX(this.frameOrigin.x() - this.horizontalScrollBar.getOffset());
                horizontalScrollBarOffset.set(offset);
            });
            this.horizontalScrollBar.setOffset(horizontalScrollBarOffset.get());
        }
        if (this.canScrollVertical) {
            this.verticalScrollBar = new ScrollBarComponent(new Dim2i(this.viewPortDimension.getLimitX() + 1, this.viewPortDimension.y(), 10, this.viewPortDimension.height()), ScrollBarComponent.Mode.VERTICAL, this.frame.dim.height(), this.viewPortDimension.height(), offset -> {
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
        this.drawable.clear();
        this.controlElements.clear();

        if (this.canScrollHorizontal) {
            this.horizontalScrollBar.updateThumbPosition();
        }

        if (this.canScrollVertical) {
            this.verticalScrollBar.updateThumbPosition();
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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (this.canScrollHorizontal || this.canScrollVertical) {
            if (this.renderOutline) {
                this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
            }
            this.applyScissor(this.viewPortDimension.x(), this.viewPortDimension.y(), this.viewPortDimension.width(), this.viewPortDimension.height(), () -> super.render(drawContext, mouseX, mouseY, delta));
        } else {
            super.render(drawContext, mouseX, mouseY, delta);
        }

        if (this.canScrollHorizontal) {
            this.horizontalScrollBar.render(drawContext, mouseX, mouseY, delta);
        }

        if (this.canScrollVertical) {
            this.verticalScrollBar.render(drawContext, mouseX, mouseY, delta);
        }
    }

    @Override
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        //this.snapFocusedInViewport();
        return super.getNavigationPath(navigation);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button) || (this.canScrollHorizontal && this.horizontalScrollBar.mouseClicked(mouseX, mouseY, button)) || (this.canScrollVertical && this.verticalScrollBar.mouseClicked(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) || (this.canScrollHorizontal && this.horizontalScrollBar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) || (this.canScrollVertical && this.verticalScrollBar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button) || (this.canScrollHorizontal && this.horizontalScrollBar.mouseReleased(mouseX, mouseY, button)) || (this.canScrollVertical && this.verticalScrollBar.mouseReleased(mouseX, mouseY, button));
    }
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount) || (this.canScrollHorizontal && this.horizontalScrollBar.mouseScrolled(mouseX, mouseY, amount)) || (this.canScrollVertical && this.verticalScrollBar.mouseScrolled(mouseX, mouseY, amount));
    }

    public static class Builder {
        private boolean renderOutline = false;
        private Dim2i dim = null;
        private AbstractFrame frame = null;
        private AtomicReference<Integer> verticalScrollBarOffset = new AtomicReference<>(0);
        private AtomicReference<Integer> horizontalScrollBarOffset = new AtomicReference<>(0);

        public Builder setDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder shouldRenderOutline(boolean state) {
            this.renderOutline = state;
            return this;
        }

        public Builder setVerticalScrollBarOffset(AtomicReference<Integer> verticalScrollBarOffset) {
            this.verticalScrollBarOffset = verticalScrollBarOffset;
            return this;
        }

        public Builder setHorizontalScrollBarOffset(AtomicReference<Integer> horizontalScrollBarOffset) {
            this.horizontalScrollBarOffset = horizontalScrollBarOffset;
            return this;
        }

        public Builder setFrame(AbstractFrame frame) {
            this.frame = frame;
            return this;
        }

        public ScrollableFrame build() {
            return new ScrollableFrame(this.dim, this.frame, this.renderOutline, this.verticalScrollBarOffset, this.horizontalScrollBarOffset);
        }
    }
}