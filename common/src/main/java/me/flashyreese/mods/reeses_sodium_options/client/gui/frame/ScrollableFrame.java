package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.flashyreese.mods.reeses_sodium_options.client.gui.AbstractWidgetExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.Dim2iExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.ScrollBarComponent;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
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

    public ScrollableFrame(Dim2i dim, Screen screen, ModOptions modOptions, AbstractFrame frame, boolean renderOutline, AtomicReference<Integer> verticalScrollBarOffset, AtomicReference<Integer> horizontalScrollBarOffset) {
        super(dim, screen, renderOutline, modOptions);
        this.frame = frame;
        this.frameOrigin = new Dim2i(((AbstractWidgetExtended) frame).getDim().x(), ((AbstractWidgetExtended) frame).getDim().y(), 0, 0);
        this.setupFrame(verticalScrollBarOffset, horizontalScrollBarOffset);
        this.buildFrame();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setupFrame(AtomicReference<Integer> verticalScrollBarOffset, AtomicReference<Integer> horizontalScrollBarOffset) {
        int maxWidth = 0;
        int maxHeight = 0;
        if (!((Dim2iExtended) ((Object) ((AbstractWidgetExtended) this).getDim())).canFitDimension(((AbstractWidgetExtended) this.frame).getDim())) {
            if (((AbstractWidgetExtended) this).getDim().getLimitX() < ((AbstractWidgetExtended) this.frame).getDim().getLimitX()) {
                int value = ((AbstractWidgetExtended) this.frame).getDim().x() - ((AbstractWidgetExtended) this).getDim().x() + ((AbstractWidgetExtended) this.frame).getDim().width();
                if (maxWidth < value) {
                    maxWidth = value;
                }
            }
            if (((AbstractWidgetExtended) this).getDim().getLimitY() < ((AbstractWidgetExtended) this.frame).getDim().getLimitY()) {
                int value = ((AbstractWidgetExtended) this.frame).getDim().y() - ((AbstractWidgetExtended) this).getDim().y() + ((AbstractWidgetExtended) this.frame).getDim().height();
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
            this.viewPortDimension = new Dim2i(((AbstractWidgetExtended) this).getDim().x(), ((AbstractWidgetExtended) this).getDim().y(), ((AbstractWidgetExtended) this).getDim().width() - 11, ((AbstractWidgetExtended) this).getDim().height() - 11);
        } else if (this.canScrollHorizontal) {
            this.viewPortDimension = new Dim2i(((AbstractWidgetExtended) this).getDim().x(), ((AbstractWidgetExtended) this).getDim().y(), ((AbstractWidgetExtended) this).getDim().width(), ((AbstractWidgetExtended) this).getDim().height() - 11);
            ((Dim2iExtended) ((Object) ((AbstractWidgetExtended) this.frame).getDim())).setHeight(((AbstractWidgetExtended) this.frame).getDim().height() - 11); // fixme: don't mutate rather
        } else if (this.canScrollVertical) {
            this.viewPortDimension = new Dim2i(((AbstractWidgetExtended) this).getDim().x(), ((AbstractWidgetExtended) this).getDim().y(), ((AbstractWidgetExtended) this).getDim().width() - 11, ((AbstractWidgetExtended) this).getDim().height());
            ((Dim2iExtended) ((Object) ((AbstractWidgetExtended) this.frame).getDim())).setWidth(((AbstractWidgetExtended) this.frame).getDim().width() - 11); // fixme: don't mutate rather
        }

        if (this.canScrollHorizontal) {
            this.horizontalScrollBar = new ScrollBarComponent(new Dim2i(this.viewPortDimension.x(), this.viewPortDimension.getLimitY() + 1, this.viewPortDimension.width(), 10), ScrollBarComponent.ScrollDirection.HORIZONTAL, ((AbstractWidgetExtended) this.frame).getDim().width(), this.viewPortDimension.width(), offset -> {
                //this.buildFrame();
                ((Dim2iExtended) ((Object) ((AbstractWidgetExtended) this.frame).getDim())).setX(this.frameOrigin.x() - this.horizontalScrollBar.getOffset());
                horizontalScrollBarOffset.set(offset);
            });
            this.horizontalScrollBar.setOffset(horizontalScrollBarOffset.get());
        }
        if (this.canScrollVertical) {
            this.verticalScrollBar = new ScrollBarComponent(new Dim2i(this.viewPortDimension.getLimitX() + 1, this.viewPortDimension.y(), 10, this.viewPortDimension.height()), ScrollBarComponent.ScrollDirection.VERTICAL, ((AbstractWidgetExtended) this.frame).getDim().height(), this.viewPortDimension.height(), offset -> {
                //this.buildFrame();
                ((Dim2iExtended) ((Object) ((AbstractWidgetExtended) this.frame).getDim())).setY(this.frameOrigin.y() - this.verticalScrollBar.getOffset());
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
            ((Dim2iExtended) ((Object) ((AbstractWidgetExtended) this.frame).getDim())).setX(this.frameOrigin.x() - this.horizontalScrollBar.getOffset());
            this.children.add(this.horizontalScrollBar);
        }

        if (this.canScrollVertical) {
            ((Dim2iExtended) ((Object) ((AbstractWidgetExtended) this.frame).getDim())).setY(this.frameOrigin.y() - this.verticalScrollBar.getOffset());
            this.children.add(this.verticalScrollBar);
        }

        this.frame.buildFrame();
        this.children.add(this.frame);
        super.buildFrame();

        // fixme: Ridiculous hack to snap to focused element
        // for the meanwhile this works until a proper solution is implemented.
        // this shouldn't be hardcoded into scrollable frame
        this.frame.registerFocusListener(element -> {
            if (element instanceof ControlElement controlElement && this.canScrollVertical) {
                Dim2i dim = ((AbstractWidgetExtended) controlElement).getDim();
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
                this.drawBorder(guiGraphics, ((AbstractWidgetExtended) this).getDim().x(), ((AbstractWidgetExtended) this).getDim().y(), ((AbstractWidgetExtended) this).getDim().getLimitX(), ((AbstractWidgetExtended) this).getDim().getLimitY(), 0xFFAAAAAA);
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
        private Screen screen;
        private ModOptions modOptions;

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