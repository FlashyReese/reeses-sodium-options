package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.ScrollFrameLayout;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.ScrollBarWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.Holder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.TabHeaderWidget;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

final class TabRail {
    private final List<Tab<?>> tabs;
    private final TabGroupModel groupModel;
    private final Holder<Integer> scrollBarOffset;
    private final List<BaseWidget> tabWidgets = new ArrayList<>();
    private final Map<String, TabHeaderWidget> tabHeaderWidgets = new HashMap<>();
    private final Map<Tab<?>, TabButtonWidget> tabButtonWidgets = new HashMap<>();
    private LayoutBounds dim;
    private boolean canScroll;
    private ScrollBarWidget scrollBar;
    private @Nullable BaseWidget selectedTabWidget;

    TabRail(LayoutBounds frameDim, List<Tab<?>> tabs, TabGroupModel groupModel, Holder<Integer> scrollBarOffset) {
        this.tabs = tabs;
        this.groupModel = groupModel;
        this.scrollBarOffset = scrollBarOffset;
        this.canScroll = this.groupModel.maximumHeight() > frameDim.height();
        this.dim = new LayoutBounds(frameDim.x(), frameDim.y(), this.calculateWidth(frameDim), frameDim.height());
    }

    LayoutBounds createFrameSection(LayoutBounds frameDim) {
        return new LayoutBounds(this.dim.getLimitX(), frameDim.y(), frameDim.width() - this.dim.width(), frameDim.height());
    }

    void selectGroupFor(Optional<Tab<?>> selectedTab) {
        this.groupModel.selectGroupFor(selectedTab);
    }

    void showRestoredSelectedTab(Tab<?> tab) {
        this.groupModel.showRestoredSelectedTab(tab);
    }

    void updateScrollState(LayoutBounds frameDim) {
        int contentHeight = this.groupModel.visibleHeight();
        this.canScroll = contentHeight > frameDim.height();
        if (!this.canScroll) {
            this.setContentY(frameDim.y());
            this.scrollBarOffset.set(0);
            this.scrollBar = null;
            return;
        }

        this.scrollBar = new ScrollBarWidget(new LayoutBounds(this.dim.getLimitX() - 11, frameDim.y(), 10, frameDim.height()), ScrollBarWidget.ScrollDirection.VERTICAL, contentHeight, frameDim.height(), offset -> {
            this.scrollBarOffset.set(offset);
            this.setContentY(frameDim.y() - this.scrollBar.getOffset());
        }, frameDim);
        this.scrollBar.setOffset(this.scrollBarOffset.getOrDefault(0));
    }

    void rebuildTabs(List<GuiEventListener> children, Optional<Tab<?>> selectedTab, Consumer<Tab<?>> tabSelector, Runnable rebuildFrame) {
        this.tabWidgets.clear();
        this.selectedTabWidget = null;
        int offsetY = 0;
        boolean firstGroup = true;
        for (TabGroup group : this.groupModel.groups()) {
            int width = this.tabContentWidth();
            if (!this.groupModel.shouldShowHeaders()) {
                for (Tab<?> tab : group.tabs()) {
                    LayoutBounds tabDim = LayoutBounds.relativeTo(this.dim, 0, offsetY, width, TabFrame.TAB_HEIGHT);
                    this.addTabButton(children, tab, tabDim, selectedTab, tabSelector);
                    offsetY += TabFrame.TAB_HEIGHT;
                }
                continue;
            }

            int headerHeight = this.groupModel.headerHeight();
            int headerY = offsetY + (firstGroup ? 0 : TabFrame.TAB_HEADER_PADDING);
            boolean showTabs = this.groupModel.shouldShowTabs(group);

            LayoutBounds tabHeaderDim = LayoutBounds.relativeTo(this.dim, 0, headerY, width, headerHeight);
            this.addTabHeader(children, group, tabHeaderDim, selectedTab, tabSelector, rebuildFrame);

            offsetY += headerHeight + (firstGroup ? 0 : TabFrame.TAB_HEADER_PADDING);
            firstGroup = false;

            if (showTabs) {
                for (Tab<?> tab : group.tabs()) {
                    LayoutBounds tabDim = LayoutBounds.relativeTo(this.dim, 0, offsetY, width, TabFrame.TAB_HEIGHT);
                    this.addTabButton(children, tab, tabDim, selectedTab, tabSelector);
                    offsetY += TabFrame.TAB_HEIGHT;
                }
            }
        }
    }

    void addScrollBar(List<GuiEventListener> children) {
        if (this.canScroll) {
            this.scrollBar.updateThumbLocation();
            children.add(this.scrollBar);
        }
    }

    void scrollSelectedTabIntoView(LayoutBounds frameDim) {
        if (this.selectedTabWidget != null) {
            this.scrollFocusedWidgetIntoView(this.selectedTabWidget, frameDim);
        }
    }

    void scrollFocusedWidgetIntoView(GuiEventListener element, LayoutBounds frameDim) {
        if (!(element instanceof BaseWidget widget) || !this.canScroll) {
            return;
        }

        LayoutBounds widgetDim = widget.getDimensions();
        if (!this.contains(widgetDim)) {
            return;
        }

        this.scrollBar.setOffset(ScrollFrameLayout.scrollIntoViewOffset(frameDim, widgetDim, this.scrollBar.getOffset()));
    }

    void extractScrollBar(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.canScroll) {
            this.scrollBar.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    @Nullable
    BaseWidget scrollBar() {
        return this.canScroll ? this.scrollBar : null;
    }

    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.canScroll && this.scrollBar.mouseClicked(event, doubleClick);
    }

    boolean mouseDragged(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
        return this.canScroll && this.scrollBar.mouseDragged(event, deltaX, deltaY);
    }

    boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return this.canScroll && this.scrollBar.mouseReleased(event);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.canScroll && this.scrollBar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void activateHeader(TabGroup group, Consumer<Tab<?>> tabSelector, Runnable rebuildFrame) {
        switch (this.groupModel.collapseMode()) {
            case SELECTED_GROUP -> {
                this.groupModel.selectGroup(group);
                tabSelector.accept(group.tabs().getFirst());
            }
            case ALL_EXPANDED -> tabSelector.accept(group.tabs().getFirst());
            case MANUAL -> {
                if (this.groupModel.isSinglePageGroupCollapsed(group)) {
                    tabSelector.accept(group.tabs().getFirst());
                } else {
                    this.groupModel.toggleManualGroup(group);
                    rebuildFrame.run();
                }
            }
        }
    }

    private void addTabButton(List<GuiEventListener> children, Tab<?> tab, LayoutBounds tabDim, Optional<Tab<?>> selectedTab, Consumer<Tab<?>> tabSelector) {
        boolean selected = this.isSelectedTab(tab, selectedTab);
        TabButtonWidget button = this.tabButtonWidgets.computeIfAbsent(tab, key ->
                new TabButtonWidget(tabDim, key, selected, () -> tabSelector.accept(key)));
        button.setDim(tabDim);
        button.setSelected(selected);
        this.addTabChild(children, button);
        if (selected) {
            this.selectedTabWidget = button;
        }
    }

    private void addTabHeader(List<GuiEventListener> children, TabGroup group, LayoutBounds tabHeaderDim, Optional<Tab<?>> selectedTab, Consumer<Tab<?>> tabSelector, Runnable rebuildFrame) {
        boolean selected = this.groupModel.isHeaderSelected(group, selectedTab);
        TabHeaderWidget header = this.tabHeaderWidgets.computeIfAbsent(group.id(), unused ->
                new TabHeaderWidget(tabHeaderDim, group.modOptions(), () -> this.activateHeader(group, tabSelector, rebuildFrame), selected));
        header.setDim(tabHeaderDim);
        header.setSelected(selected);
        this.addTabChild(children, header);
    }

    private boolean isSelectedTab(Tab<?> tab, Optional<Tab<?>> selectedTab) {
        return selectedTab.filter(value -> value == tab).isPresent() && !(tab.getPage() instanceof ExternalPage);
    }

    private void addTabChild(List<GuiEventListener> children, BaseWidget widget) {
        children.add(widget);
        this.tabWidgets.add(widget);
    }

    private void setContentY(int y) {
        int delta = y - this.dim.y();
        if (delta == 0) {
            return;
        }

        this.dim = this.dim.withY(y);
        for (BaseWidget widget : this.tabWidgets) {
            LayoutBounds widgetDim = widget.getDimensions();
            widget.setDim(widgetDim.translate(0, delta));
        }
    }

    private boolean contains(LayoutBounds widgetDim) {
        return widgetDim.x() >= this.dim.x()
                && widgetDim.getLimitX() <= this.dim.getLimitX()
                && widgetDim.height() <= this.dim.height();
    }

    private int tabContentWidth() {
        return this.dim.width() - (this.canScroll ? 12 : 4);
    }

    private int calculateWidth(LayoutBounds frameDim) {
        int textWidth = this.tabs.stream()
                .mapToInt(this::getTextWidth)
                .max()
                .orElse(0);
        int preferredWidth = textWidth + (this.canScroll ? TabFrame.TAB_RAIL_SCROLL_PADDING : TabFrame.TAB_RAIL_PADDING);
        int maximumWidth = Math.max(TabFrame.TAB_RAIL_MIN_WIDTH,
                Math.min(TabFrame.TAB_RAIL_MAX_WIDTH, (int) (frameDim.width() * TabFrame.TAB_RAIL_MAX_WIDTH_RATIO)));

        return Math.clamp(preferredWidth, TabFrame.TAB_RAIL_MIN_WIDTH, maximumWidth);
    }

    private int getTextWidth(Tab<?> tab) {
        int tabTitleWidth = Minecraft.getInstance().font.width(tab.getTitle());
        if (!this.groupModel.shouldShowHeaders()) {
            return tabTitleWidth;
        }

        int headerTextWidth = Minecraft.getInstance().font.width(tab.getModOptions().name());
        if (ReeseSodiumOptionsConfig.config().isTabHeaderVersionLabels()) {
            headerTextWidth = Math.max(headerTextWidth, Minecraft.getInstance().font.width(tab.getModOptions().version()));
        }
        if (ReeseSodiumOptionsConfig.config().isTabHeaderIcons() && tab.getModOptions().icon() != null) {
            headerTextWidth += this.groupModel.headerHeight();
        }

        return Math.max(tabTitleWidth, headerTextWidth);
    }
}
