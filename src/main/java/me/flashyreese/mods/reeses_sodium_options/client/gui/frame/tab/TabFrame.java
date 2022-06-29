package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.FlatButtonWidgetExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.ScrollBarComponent;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.util.math.MatrixStack;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class TabFrame extends AbstractFrame {

    private final boolean tabSectionCanScroll;
    private final Dim2i tabSection;
    private final Dim2i frameSection;
    private final List<Tab<?>> tabs = new ArrayList<>();
    private ScrollBarComponent tabSectionScrollBar = null;
    private Tab<?> selectedTab;
    private AbstractFrame selectedFrame;

    public TabFrame(Dim2i dim, boolean renderOutline, List<Tab<?>> tabs) {
        super(dim, renderOutline);

        Optional<Integer> result = tabs.stream().map(tab -> this.getStringWidth(tab.getTitle().getString())).max(Integer::compareTo);

        this.tabSection = new Dim2i(this.dim.getOriginX(), this.dim.getOriginY(), result.map(integer -> (int) (integer * 2.5)).orElseGet(() -> (int) (this.dim.getWidth() * 0.35D)), this.dim.getHeight());
        this.frameSection = new Dim2i(this.tabSection.getLimitX(), this.dim.getOriginY(), this.dim.getWidth() - this.tabSection.getWidth(), this.dim.getHeight());
        this.tabs.addAll(tabs);

        int tabSectionY = this.tabs.size() * 18;
        this.tabSectionCanScroll = tabSectionY > this.tabSection.getHeight();
        if (this.tabSectionCanScroll) {
            this.tabSectionScrollBar = new ScrollBarComponent(new Dim2i(this.tabSection.getLimitX() - 11, this.tabSection.getOriginY(), 10, this.tabSection.getHeight()), ScrollBarComponent.Mode.VERTICAL, tabSectionY, this.dim.getHeight(), this::buildFrame, this.dim);
        }

        this.buildFrame();
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
            if (!this.tabs.isEmpty()) {
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
        this.applyScissor(this.dim.getOriginX(), this.dim.getOriginY(), this.dim.getWidth(), this.dim.getHeight(), () -> {
            for (AbstractWidget widget: this.children) {
                if (widget != this.selectedFrame){
                    widget.render(matrices, mouseX, mouseY, delta);
                }
            }
        });
        this.selectedFrame.render(matrices, mouseX, mouseY, delta);
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
        int offsetY = 0;
        for (Tab<?> tab : this.tabs) {
            int x = this.tabSection.getOriginX();
            int y = this.tabSection.getOriginY() + offsetY - (this.tabSectionCanScroll ? this.tabSectionScrollBar.getOffset() : 0);
            int width = this.tabSection.getWidth() - (this.tabSectionCanScroll ? 12 : 4);
            int height = 18;
            Dim2i tabDim = new Dim2i(x, y, width, height);

            FlatButtonWidget button = new FlatButtonWidget(tabDim, tab.getTitle().asString(), () -> this.setTab(tab));
            button.setSelected(this.selectedTab == tab);
            ((FlatButtonWidgetExtended) button).setLeftAligned(true);
            this.children.add(button);

            offsetY += 18;
        }
    }

    private void rebuildTabFrame() {
        if (this.selectedTab == null) return;
        AbstractFrame frame = this.selectedTab.getFrameFunction().apply(this.frameSection);
        if (frame != null) {
            this.selectedFrame = frame;
            frame.buildFrame();
            this.children.add(frame);
        }
    }

    public static class Builder {
        private final List<Tab<?>> functions = new ArrayList<>();
        private Dim2i dim;
        private boolean renderOutline;

        public Builder setDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder shouldRenderOutline(boolean renderOutline) {
            this.renderOutline = renderOutline;
            return this;
        }

        public Builder addTabs(Consumer<List<Tab<?>>> tabs) {
            tabs.accept(this.functions);
            return this;
        }

        public TabFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");

            return new TabFrame(this.dim, this.renderOutline, this.functions);
        }
    }
}