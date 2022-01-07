package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class TabFrame extends AbstractFrame {

    private final Dim2i tabSection;
    private final Dim2i frameSection;

    private Tab<?> selectedTab;
    private final List<Tab<?>> tabs = new ArrayList<>();

    public TabFrame(Dim2i dim, List<Function<Dim2i, Tab<?>>> functions) {
        super(dim);
        this.tabSection = new Dim2i(this.dim.x(), this.dim.y(), (int) (this.dim.width() * 0.35D), this.dim.height());
        this.frameSection = new Dim2i(this.tabSection.getLimitX(), this.dim.y(), this.dim.width() - this.tabSection.width(), this.dim.height());
        functions.forEach(function -> this.tabs.add(function.apply(this.frameSection)));
        this.buildFrame();
    }

    public TabFrame(Dim2i dim, List<Function<Dim2i, Tab<?>>> functions, boolean renderOutline) {
        this(dim, functions);
        this.renderOutline = renderOutline;
    }

    public void setTab(Tab<?> tab) {
        this.selectedTab = tab;

        this.buildFrame();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.dim.containsCursor(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button);
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

        super.buildFrame();
    }

    private void rebuildTabs() {
        if (this.tabs == null) return;
        int offsetY = 0;
        for (Tab<?> tab : this.tabs) {
            int x = this.tabSection.x();
            int y = this.tabSection.y() + offsetY;
            int width = this.tabSection.width() - 4;
            int height = 18;
            Dim2i tabDim = new Dim2i(x, y, width, height);

            FlatButtonWidget button = new FlatButtonWidget(tabDim, tab.getText(), () -> this.setTab(tab));
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

    public static Builder createBuilder() {
        return new Builder();
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