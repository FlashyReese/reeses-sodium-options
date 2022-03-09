package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.ScrollBarComponent;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.util.math.MatrixStack;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class TabFrame extends AbstractFrame {

    private final boolean tabSectionCanScroll;
    private final Dim2i tabSection;
    private final Dim2i frameSection;
    private final List<Tab<?>> tabs = new ArrayList<>();
    private ScrollBarComponent tabSectionScrollBar = null;
    private Tab<?> selectedTab;

    public TabFrame(Dim2i dim, List<Function<Dim2i, Tab<?>>> functions) {
        super(dim);
        this.tabSection = new Dim2i(this.dim.x(), this.dim.y(), (int) (this.dim.width() * 0.35D), this.dim.height());
        this.frameSection = new Dim2i(this.tabSection.getLimitX(), this.dim.y(), this.dim.width() - this.tabSection.width(), this.dim.height());
        functions.forEach(function -> this.tabs.add(function.apply(this.frameSection)));

        int tabSectionY = this.tabs.size() * 18;
        this.tabSectionCanScroll = tabSectionY > this.tabSection.height();
        if (this.tabSectionCanScroll) {
            this.tabSectionScrollBar = new ScrollBarComponent(new Dim2i(this.tabSection.getLimitX() - 11, this.tabSection.y(), 10, this.tabSection.height()), ScrollBarComponent.Mode.VERTICAL, tabSectionY, this.dim.height(), this::buildFrame, this.dim);
        }

        this.buildFrame();
    }

    public TabFrame(Dim2i dim, List<Function<Dim2i, Tab<?>>> functions, boolean renderOutline) {
        this(dim, functions);
        this.renderOutline = renderOutline;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public void setTab(Tab<?> tab) {
        this.selectedTab = tab;

        this.buildFrame();
    }

    @Override
    public void buildFrame() {
        this.children.clear();
        this.drawable.clear();
        this.controlElements.clear();

        if (this.selectedTab == null) {
            if (this.tabs != null && !this.tabs.isEmpty()) {
                // Just use the first tab for now
                this.selectedTab = this.tabs.get(0);
            }
        }

        this.rebuildTabFrame();
        this.rebuildTabs();

        if (this.tabSectionCanScroll) {
            this.tabSectionScrollBar.updateThumbPosition();
        }

        super.buildFrame();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.applyScissor(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height(), () -> {
            for (AbstractWidget widget : this.children) {
                if (widget != this.selectedTab.getFrame()) {
                    widget.render(matrices, mouseX, mouseY, delta);
                }
            }
        });
        this.selectedTab.getFrame().render(matrices, mouseX, mouseY, delta);
        if (this.tabSectionCanScroll) {
            this.tabSectionScrollBar.render(matrices, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.dim.containsCursor(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.tabSectionCanScroll) {
            return this.tabSectionScrollBar.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        if (this.tabSectionCanScroll) {
            return this.tabSectionScrollBar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (this.tabSectionCanScroll) {
            return this.tabSectionScrollBar.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (super.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        if (this.tabSectionCanScroll) {
            return this.tabSectionScrollBar.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }

    private void rebuildTabs() {
        if (this.tabs == null) return;
        int offsetY = 0;
        for (Tab<?> tab : this.tabs) {
            int x = this.tabSection.x();
            int y = this.tabSection.y() + offsetY - (this.tabSectionCanScroll ? this.tabSectionScrollBar.getOffset() : 0);
            int width = this.tabSection.width() - (this.tabSectionCanScroll ? 12 : 4);
            int height = 18;
            Dim2i tabDim = new Dim2i(x, y, width, height);

            FlatButtonWidget button = new FlatButtonWidget(tabDim, tab.getTitle(), () -> this.setTab(tab));
            button.setSelected(this.selectedTab == tab);
            this.children.add(button);

            offsetY += 18;
        }
    }

    private void rebuildTabFrame() {
        if (this.selectedTab == null) return;
        AbstractFrame frame = this.selectedTab.getFrame();
        if (frame != null) {
            frame.buildFrame();
            this.children.add(frame);
        }
    }

    public static class Builder {
        private final List<Function<Dim2i, Tab<?>>> functions = new ArrayList<>();
        private boolean renderOutline = false;
        private Dim2i dim = null;

        public Builder setDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder shouldRenderOutline(boolean state) {
            this.renderOutline = state;
            return this;
        }

        public Builder addTab(Function<Dim2i, Tab<?>> function) {
            this.functions.add(function);
            return this;
        }

        public Builder addTabs(List<Function<Dim2i, Tab<?>>> functions) {
            this.functions.addAll(functions);
            return this;
        }

        public Builder addTabs(Consumer<List<Function<Dim2i, Tab<?>>>> tabs) {
            tabs.accept(this.functions);
            return this;
        }

        public TabFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");

            return new TabFrame(this.dim, this.functions, this.renderOutline);
        }
    }
}