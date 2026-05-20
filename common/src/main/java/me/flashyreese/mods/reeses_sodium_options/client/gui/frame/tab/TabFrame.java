package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.AbstractWidgetExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.Dim2iAccess;
import me.flashyreese.mods.reeses_sodium_options.client.gui.FlatButtonWidgetExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.Point2iAccess;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.ScrollBarComponent;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.TabHeaderComponent;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.gui.ButtonTheme;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TabFrame extends AbstractFrame {

    public static final int TAB_HEIGHT = 18;
    public static final int TAB_HEADER_HEIGHT = 22;
    public static final int TAB_HEADER_PADDING = 4;
    public static final float TEXT_WIDTH_MULTIPLIER = 1.25F;

    private final boolean tabSectionCanScroll;
    private final Dim2i tabSection;
    private final Dim2i frameSection;
    private final List<Tab<?>> tabs = new ArrayList<>();
    private final Runnable onSetTab;
    private final AtomicReference<Component> tabSectionSelectedTab;
    private ScrollBarComponent tabSectionScrollBar = null;
    private Optional<Tab<?>> selectedTab = Optional.empty();
    private AbstractFrame selectedFrame;

    public TabFrame(Dim2i dim, Screen screen, ModOptions modOptions, boolean renderOutline, List<Tab<?>> tabs, Runnable onSetTab, AtomicReference<Component> tabSectionSelectedTab, AtomicReference<Integer> tabSectionScrollBarOffset) {
        super(dim, screen, renderOutline, modOptions);
        this.tabs.addAll(tabs);
        int uniqueGroups = Math.toIntExact(this.tabs.stream().map(tab -> tab.getModOptions().name()).distinct().count());
        int tabSectionY = this.tabs.size() * TAB_HEIGHT + uniqueGroups * TAB_HEADER_HEIGHT + (uniqueGroups - 1) * TAB_HEADER_PADDING;
        Dim2i frameDim = this.getFrameDim();
        this.tabSectionCanScroll = tabSectionY > frameDim.height();

        Optional<Integer> result = tabs.stream().map(tab -> (int) (this.getStringWidth(tab.getTitle()) * TEXT_WIDTH_MULTIPLIER)).max(Integer::compareTo);

        this.tabSection = new Dim2i(frameDim.x(), frameDim.y(), result.map(integer -> integer + (this.tabSectionCanScroll ? 32 : 24)).orElseGet(() -> (int) (frameDim.width() * 0.35D)), frameDim.height());
        this.frameSection = new Dim2i(this.tabSection.getLimitX(), frameDim.y(), frameDim.width() - this.tabSection.width(), frameDim.height());

        this.onSetTab = onSetTab;
        if (this.tabSectionCanScroll) {
            this.tabSectionScrollBar = new ScrollBarComponent(new Dim2i(this.tabSection.getLimitX() - 11, this.tabSection.y(), 10, this.tabSection.height()), ScrollBarComponent.ScrollDirection.VERTICAL, tabSectionY, frameDim.height(), offset -> {
                //this.buildFrame();
                tabSectionScrollBarOffset.set(offset);
                ((Dim2iAccess) ((Object) this.tabSection)).setY(frameDim.y() - this.tabSectionScrollBar.getOffset());
            }, frameDim);
            this.tabSectionScrollBar.setOffset(tabSectionScrollBarOffset.get());
        }
        this.tabSectionSelectedTab = tabSectionSelectedTab;

        if (this.tabSectionSelectedTab.get() != null) {
            this.selectedTab = this.tabs.stream().filter(tab -> tab.getTitle().getString().equals(this.tabSectionSelectedTab.get().getString())).findAny();
        }

        this.buildFrame();

        // Let's build each frame, future note for anyone: do not move this line.
        this.tabs.stream().filter(tab -> this.selectedTab.filter(value -> value != tab).isPresent()).forEach(tab -> tab.getFrameFunction().apply(this.frameSection));
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public void setTab(Optional<Tab<?>> tab) {
        this.selectedTab = tab;
        if (this.onSetTab != null) {
            this.onSetTab.run();
        }
        this.selectedTab.ifPresent(value -> {
            if (value.getPage() instanceof ExternalPage externalPage) {
                externalPage.currentScreenConsumer().accept(this.screen);
            } else {
                this.tabSectionSelectedTab.set(value.getTitle());
            }
        });
        this.buildFrame();
    }

    @Override
    public void buildFrame() {
        this.children.clear();
        this.controlElements.clear();

        if (this.selectedTab.isEmpty()) {
            if (!this.tabs.isEmpty()) {
                // Just use the first tab for now
                this.selectedTab = Optional.ofNullable(this.tabs.getFirst());
            }
        }

        this.rebuildTabs();
        this.rebuildTabFrame();

        if (this.tabSectionCanScroll) {
            this.tabSectionScrollBar.updateThumbLocation();
            this.children.add(this.tabSectionScrollBar);
        }

        super.buildFrame();
        Dim2i focusFrameDim = this.getFrameDim();
        this.registerFocusListener(element -> {
            if (element instanceof FlatButtonWidgetExtended flatButtonWidget && this.tabSectionCanScroll) {
                Dim2i dim = ((AbstractWidgetExtended) flatButtonWidget).getDim();
                int inputOffset = this.tabSectionScrollBar.getOffset();
                if (dim.y() <= focusFrameDim.y()) {
                    inputOffset += dim.y() - focusFrameDim.y();
                } else if (dim.getLimitY() >= focusFrameDim.getLimitY()) {
                    inputOffset += dim.getLimitY() - focusFrameDim.getLimitY();
                }
                this.tabSectionScrollBar.setOffset(inputOffset);
            }
        });
    }

    private void rebuildTabs() {
        int offsetY = 0;
        String lastMod = "";
        boolean firstMod = true;
        for (Tab<?> tab : this.tabs) {
            int width = this.tabSection.width() - (this.tabSectionCanScroll ? 12 : 4);

            /*
             * fixme: In theory, this should be fine and the list should be sorted by mod by default; this should never fail; but can happen.
             */
            if (!lastMod.equals(tab.getModOptions().name())) {
                Dim2i tabHeaderDim = new Dim2i(0, offsetY + (firstMod ? 0 : TAB_HEADER_PADDING), width, TAB_HEADER_HEIGHT);
                setDimPoint(tabHeaderDim, (Point2iAccess) (Object) this.tabSection);
                this.children.add(new TabHeaderComponent(tabHeaderDim, tab.getModOptions()));

                lastMod = tab.getModOptions().name();
                offsetY += TAB_HEADER_HEIGHT + (firstMod ? 0 : TAB_HEADER_PADDING);
                firstMod = false;
            }

            Dim2i tabDim = new Dim2i(0, offsetY, width, TAB_HEIGHT);
            setDimPoint(tabDim, (Point2iAccess) (Object) this.tabSection);

            ButtonTheme buttonTheme = new ButtonTheme(
                    tab.getModOptions().theme(),
                    FlatButtonWidget.DEFAULT_THEME.bgHighlight,
                    FlatButtonWidget.DEFAULT_THEME.bgDefault,
                    FlatButtonWidget.DEFAULT_THEME.bgInactive
            );

            FlatButtonWidget button = new FlatButtonWidget(tabDim, tab.getTitle(), () -> this.setTab(Optional.of(tab)), true, true, buttonTheme);
            button.setSelected(this.selectedTab.isPresent() && this.selectedTab.get() == tab && !(tab.getPage() instanceof ExternalPage));
            if (button instanceof FlatButtonWidgetExtended buttonExtended) {
                buttonExtended.setTab(true);
            }
            this.children.add(button);

            offsetY += TAB_HEIGHT;
        }
    }

    private void rebuildTabFrame() {
        if (this.selectedTab.isEmpty()) return;
        AbstractFrame frame = this.selectedTab.get().getFrameFunction().apply(this.frameSection);
        if (frame != null) {
            this.selectedFrame = frame;
            frame.buildFrame();
            this.children.add(frame);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        Dim2i frameDim = this.getFrameDim();
        this.applyScissor(guiGraphics, frameDim.x(), frameDim.y(), frameDim.width(), frameDim.height(), () -> {
            for (AbstractWidget widget : this.children) {
                if (widget != this.selectedFrame) {
                    widget.render(guiGraphics, mouseX, mouseY, delta);
                }
            }
        });
        this.selectedFrame.render(guiGraphics, mouseX, mouseY, delta);
        if (this.tabSectionCanScroll) {
            this.tabSectionScrollBar.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return (this.getFrameDim().containsCursor(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button)) || (this.tabSectionCanScroll && this.tabSectionScrollBar.mouseClicked(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) || (this.tabSectionCanScroll && this.tabSectionScrollBar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button) || (this.tabSectionCanScroll && this.tabSectionScrollBar.mouseReleased(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) || (this.tabSectionCanScroll && this.tabSectionScrollBar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount));
    }

    public List<Tab<?>> getTabs() {
        return tabs;
    }

    public AbstractFrame getSelectedFrame() {
        return selectedFrame;
    }

    public Optional<Tab<?>> getSelectedTab() {
        return selectedTab;
    }

    public static class Builder {
        private final List<Tab<?>> functions = new ArrayList<>();
        private Dim2i dim;
        private boolean renderOutline;
        private Runnable onSetTab;
        private AtomicReference<Component> tabSectionSelectedTab = new AtomicReference<>(null);
        private AtomicReference<Integer> tabSectionScrollBarOffset = new AtomicReference<>(0);
        private Screen screen;
        private ModOptions modOptions;

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

        public Builder onSetTab(Runnable onSetTab) {
            this.onSetTab = onSetTab;
            return this;
        }

        public Builder setTabSectionSelectedTab(AtomicReference<Component> tabSectionSelectedTab) {
            this.tabSectionSelectedTab = tabSectionSelectedTab;
            return this;
        }

        public Builder setTabSectionScrollBarOffset(AtomicReference<Integer> tabSectionScrollBarOffset) {
            this.tabSectionScrollBarOffset = tabSectionScrollBarOffset;
            return this;
        }

        public Builder withScreen(Screen screen) {
            this.screen = screen;
            return this;
        }

        public Builder withModOptions(ModOptions modOptions) {
            this.modOptions = modOptions;
            return this;
        }

        public TabFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");

            return new TabFrame(this.dim, this.screen, this.modOptions, this.renderOutline, this.functions, this.onSetTab, this.tabSectionSelectedTab, this.tabSectionScrollBarOffset);
        }
    }
}
