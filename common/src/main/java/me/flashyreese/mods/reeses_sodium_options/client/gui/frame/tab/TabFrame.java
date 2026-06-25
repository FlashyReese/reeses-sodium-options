package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.OptionRow;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.Holder;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class TabFrame extends AbstractFrame {

    public static final int TAB_HEIGHT = 18;
    public static final int TAB_HEADER_HEIGHT = 18;
    public static final int TAB_HEADER_VERSION_HEIGHT = 22;
    public static final int TAB_HEADER_PADDING = 4;
    public static final int TAB_RAIL_MIN_WIDTH = 120;
    public static final int TAB_RAIL_MAX_WIDTH = 220;
    public static final int TAB_RAIL_PADDING = 24;
    public static final int TAB_RAIL_SCROLL_PADDING = 32;
    public static final double TAB_RAIL_MAX_WIDTH_RATIO = 0.35D;

    private final TabRail tabRail;
    private final TabSelectionState tabSelection;
    private final LayoutBounds frameSection;
    private final List<Tab<?>> tabs = new ArrayList<>();
    private final Runnable onSetTab;
    private final Holder<Boolean> scrollSelectedTabIntoView;

    public TabFrame(LayoutBounds dim, Screen screen, ModOptions modOptions, boolean renderOutline, List<Tab<?>> tabs, Runnable onSetTab, Holder<Component> tabRailSelectedTab, Holder<String> tabRailSelectedGroup, Holder<Integer> tabRailScrollBarOffset, Holder<Boolean> scrollSelectedTabIntoView, Set<String> manuallyCollapsedTabGroups) {
        super(dim, screen, renderOutline, modOptions);
        this.tabs.addAll(tabs);
        this.scrollSelectedTabIntoView = scrollSelectedTabIntoView;
        TabGroupModel tabGroupModel = new TabGroupModel(this.tabs, tabRailSelectedGroup, manuallyCollapsedTabGroups);
        LayoutBounds frameDim = this.getFrameDim();

        this.tabRail = new TabRail(frameDim, this.tabs, tabGroupModel, tabRailScrollBarOffset);
        this.tabSelection = new TabSelectionState(this.tabs, tabRailSelectedTab, screen);
        this.frameSection = this.tabRail.createFrameSection(frameDim);

        this.onSetTab = onSetTab;
        this.tabSelection.restorePersistedTab(this.tabRail::showRestoredSelectedTab);

        this.buildFrame();

        // Let's build each frame, future note for anyone: do not move this line.
        this.tabSelection.warmInactiveFrames(this.frameSection);
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public void setTab(Optional<Tab<?>> tab) {
        this.tabSelection.setSelectedTab(tab);
        this.tabRail.selectGroupFor(tab);
        if (this.onSetTab != null) {
            this.onSetTab.run();
        }
        this.tabSelection.activateSelectedTab();
        this.buildFrame();
    }

    public void refreshFromState() {
        this.tabSelection.restorePersistedTab(this.tabRail::showRestoredSelectedTab);
        this.tabRail.selectGroupFor(this.tabSelection.selectedTab());
        this.tabSelection.activateSelectedTab();
        this.tabSelection.refreshFrames(this.frameSection);
        this.buildFrame();
    }

    @Override
    public void buildFrame() {
        this.children.clear();
        this.optionRows.clear();

        this.tabSelection.ensureSelectedTab(this.tabRail::showRestoredSelectedTab);

        LayoutBounds frameDim = this.getFrameDim();
        this.tabRail.updateScrollState(frameDim);
        this.tabRail.rebuildTabs(this.children, this.tabSelection.selectedTab(), tab -> this.setTab(Optional.of(tab)), this::buildFrame);
        this.tabSelection.rebuildSelectedFrame(this.frameSection, this.children);
        this.tabRail.addScrollBar(this.children);

        if (this.scrollSelectedTabIntoView.getOrDefault(false)) {
            this.scrollSelectedTabIntoView.set(false);
            this.tabRail.scrollSelectedTabIntoView(frameDim);
        }

        super.buildFrame();
        this.registerFocusListener(element -> this.tabRail.scrollFocusedWidgetIntoView(element, frameDim));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        LayoutBounds frameDim = this.getFrameDim();
        AbstractFrame selectedFrame = this.tabSelection.selectedFrame();
        this.applyScissor(guiGraphics, frameDim.x(), frameDim.y(), frameDim.width(), frameDim.height(), () -> {
            for (GuiEventListener child : this.children) {
                if (child != selectedFrame && child instanceof Renderable renderable) {
                    renderable.render(guiGraphics, mouseX, mouseY, delta);
                }
            }
        });
        if (selectedFrame != null) {
            selectedFrame.render(guiGraphics, mouseX, mouseY, delta);
        }
        this.tabRail.extractScrollBar(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return (this.getFrameDim().contains(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button)) || this.tabRail.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) || this.tabRail.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button) || this.tabRail.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) || this.tabRail.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public List<Tab<?>> getTabs() {
        return tabs;
    }

    public AbstractFrame getSelectedFrame() {
        return this.tabSelection.selectedFrame();
    }

    public Optional<Tab<?>> getSelectedTab() {
        return this.tabSelection.selectedTab();
    }

    public Optional<String> getSelectedTabKey() {
        return this.tabSelection.selectedTabKey();
    }

    public OptionRow findSelectedOptionRow(ResourceLocation optionId) {
        return this.tabSelection.findSelectedOptionRow(optionId);
    }

    public OptionRow findFirstSelectedOptionRow() {
        return this.tabSelection.findFirstSelectedOptionRow();
    }

    public OptionRow findLastSelectedOptionRow() {
        return this.tabSelection.findLastSelectedOptionRow();
    }

    public OptionRow findFirstVisibleSelectedOptionRow() {
        return this.tabSelection.findFirstVisibleSelectedOptionRow();
    }

    public OptionRow findLastVisibleSelectedOptionRow() {
        return this.tabSelection.findLastVisibleSelectedOptionRow();
    }

    public boolean scrollSelectedPageToStart() {
        return this.tabSelection.scrollSelectedPageToStart();
    }

    public boolean scrollSelectedPageToEnd() {
        return this.tabSelection.scrollSelectedPageToEnd();
    }

    public boolean scrollSelectedPage(int direction) {
        return this.tabSelection.scrollSelectedPage(direction);
    }

    public static class Builder {
        private final List<Tab<?>> functions = new ArrayList<>();
        private LayoutBounds dim;
        private boolean renderOutline;
        private Runnable onSetTab;
        private Holder<Component> tabRailSelectedTab = new Holder<>(null);
        private Holder<String> tabRailSelectedGroup = new Holder<>(null);
        private Holder<Integer> tabRailScrollBarOffset = new Holder<>(0);
        private Holder<Boolean> scrollSelectedTabIntoView = new Holder<>(false);
        private Set<String> manuallyCollapsedTabGroups = new HashSet<>();
        private Screen screen;
        private ModOptions modOptions;

        public Builder setDimension(LayoutBounds dim) {
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

        public Builder setTabRailSelectedTab(Holder<Component> tabRailSelectedTab) {
            this.tabRailSelectedTab = tabRailSelectedTab;
            return this;
        }

        public Builder setTabRailSelectedGroup(Holder<String> tabRailSelectedGroup) {
            this.tabRailSelectedGroup = tabRailSelectedGroup;
            return this;
        }

        public Builder setTabRailScrollBarOffset(Holder<Integer> tabRailScrollBarOffset) {
            this.tabRailScrollBarOffset = tabRailScrollBarOffset;
            return this;
        }

        public Builder setScrollSelectedTabIntoView(Holder<Boolean> scrollSelectedTabIntoView) {
            this.scrollSelectedTabIntoView = scrollSelectedTabIntoView;
            return this;
        }

        public Builder setManuallyCollapsedTabGroups(Set<String> manuallyCollapsedTabGroups) {
            this.manuallyCollapsedTabGroups = manuallyCollapsedTabGroups;
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

            return new TabFrame(this.dim, this.screen, this.modOptions, this.renderOutline, this.functions, this.onSetTab, this.tabRailSelectedTab, this.tabRailSelectedGroup, this.tabRailScrollBarOffset, this.scrollSelectedTabIntoView, this.manuallyCollapsedTabGroups);
        }
    }
}
