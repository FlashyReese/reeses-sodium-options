package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.AbstractWidgetExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.Dim2iAccess;
import me.flashyreese.mods.reeses_sodium_options.client.gui.FlatButtonWidgetExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.Point2iAccess;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.ScrollableFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.ScrollBarComponent;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.TabHeaderComponent;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.ButtonTheme;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TabFrame extends AbstractFrame {

    public static final int TAB_HEIGHT = 18;
    public static final int TAB_HEADER_HEIGHT = 18;
    public static final int TAB_HEADER_VERSION_HEIGHT = 22;
    public static final int TAB_HEADER_PADDING = 4;
    public static final int TAB_SECTION_MIN_WIDTH = 120;
    public static final int TAB_SECTION_MAX_WIDTH = 220;
    public static final int TAB_SECTION_PADDING = 24;
    public static final int TAB_SECTION_SCROLL_PADDING = 32;
    public static final double TAB_SECTION_MAX_WIDTH_RATIO = 0.35D;

    private boolean tabSectionCanScroll;
    private final Dim2i tabSection;
    private final Dim2i frameSection;
    private final List<Tab<?>> tabs = new ArrayList<>();
    private final Runnable onSetTab;
    private final AtomicReference<Component> tabSectionSelectedTab;
    private final AtomicReference<String> tabSectionSelectedGroup;
    private final AtomicReference<Integer> tabSectionScrollBarOffset;
    private final Set<String> manuallyCollapsedTabGroups;
    private ScrollBarComponent tabSectionScrollBar = null;
    private Optional<Tab<?>> selectedTab = Optional.empty();
    private AbstractFrame selectedFrame;

    public TabFrame(Dim2i dim, Screen screen, ModOptions modOptions, boolean renderOutline, List<Tab<?>> tabs, Runnable onSetTab, AtomicReference<Component> tabSectionSelectedTab, AtomicReference<String> tabSectionSelectedGroup, AtomicReference<Integer> tabSectionScrollBarOffset, Set<String> manuallyCollapsedTabGroups) {
        super(dim, screen, renderOutline, modOptions);
        this.tabs.addAll(tabs);
        List<TabGroup> tabGroups = this.buildTabGroups();
        int tabSectionY = this.calculateMaximumTabSectionHeight(tabGroups);
        Dim2i frameDim = this.getFrameDim();
        this.tabSectionCanScroll = tabSectionY > frameDim.height();

        this.tabSection = new Dim2i(frameDim.x(), frameDim.y(), this.calculateTabSectionWidth(frameDim), frameDim.height());
        this.frameSection = new Dim2i(this.tabSection.getLimitX(), frameDim.y(), frameDim.width() - this.tabSection.width(), frameDim.height());

        this.onSetTab = onSetTab;
        this.tabSectionSelectedTab = tabSectionSelectedTab;
        this.tabSectionSelectedGroup = tabSectionSelectedGroup;
        this.tabSectionScrollBarOffset = tabSectionScrollBarOffset;
        this.manuallyCollapsedTabGroups = manuallyCollapsedTabGroups;

        if (this.tabSectionSelectedTab.get() != null) {
            this.selectedTab = this.tabs.stream().filter(tab -> tab.getTitle().getString().equals(this.tabSectionSelectedTab.get().getString())).findAny();
            this.selectedTab.ifPresent(this::showRestoredSelectedTab);
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
        if (this.collapseMode() == ReeseSodiumOptionsConfig.TabHeaderCollapseMode.SELECTED_GROUP) {
            tab.ifPresent(value -> this.tabSectionSelectedGroup.set(value.getModOptions().configId()));
        }
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
                this.selectedTab.ifPresent(this::showRestoredSelectedTab);
            }
        }

        int tabSectionY = this.calculateVisibleTabSectionHeight(this.buildTabGroups());
        Dim2i frameDim = this.getFrameDim();
        this.tabSectionCanScroll = tabSectionY > frameDim.height();
        if (!this.tabSectionCanScroll) {
            ((Dim2iAccess) (Object) this.tabSection).setY(frameDim.y());
            this.tabSectionScrollBarOffset.set(0);
            this.tabSectionScrollBar = null;
        } else {
            this.tabSectionScrollBar = new ScrollBarComponent(new Dim2i(this.tabSection.getLimitX() - 11, frameDim.y(), 10, frameDim.height()), ScrollBarComponent.ScrollDirection.VERTICAL, tabSectionY, frameDim.height(), offset -> {
                this.tabSectionScrollBarOffset.set(offset);
                ((Dim2iAccess) (Object) this.tabSection).setY(frameDim.y() - this.tabSectionScrollBar.getOffset());
            }, frameDim);
            this.tabSectionScrollBar.setOffset(this.tabSectionScrollBarOffset.get());
        }

        this.rebuildTabs();
        this.rebuildTabFrame();

        if (this.tabSectionCanScroll) {
            this.tabSectionScrollBar.updateThumbLocation();
            this.children.add(this.tabSectionScrollBar);
        }

        super.buildFrame();
        Dim2i focusFrameDim = frameDim;
        this.registerFocusListener(element -> {
            if (element instanceof AbstractWidgetExtended widget && this.tabSectionCanScroll) {
                Dim2i dim = widget.getDim();
                if (!this.tabSectionContains(dim)) {
                    return;
                }

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

    private boolean tabSectionContains(Dim2i dim) {
        return dim.x() >= this.tabSection.x()
                && dim.getLimitX() <= this.tabSection.getLimitX()
                && dim.height() <= this.tabSection.height();
    }

    private void rebuildTabs() {
        int offsetY = 0;
        boolean firstGroup = true;
        for (TabGroup group : this.buildTabGroups()) {
            int width = this.tabSection.width() - (this.tabSectionCanScroll ? 12 : 4);
            if (!this.shouldShowTabHeaders()) {
                for (Tab<?> tab : group.tabs()) {
                    Dim2i tabDim = new Dim2i(0, offsetY, width, TAB_HEIGHT);
                    setDimPoint(tabDim, (Point2iAccess) (Object) this.tabSection);
                    this.children.add(this.createTabButton(tab, tabDim));
                    offsetY += TAB_HEIGHT;
                }
                continue;
            }

            int headerHeight = this.getTabHeaderHeight();
            int headerY = offsetY + (firstGroup ? 0 : TAB_HEADER_PADDING);
            boolean collapsedSinglePageGroup = group.tabs().size() == 1 && this.shouldCollapseSinglePageGroups();
            boolean expanded = this.isGroupExpanded(group);
            Tab<?> firstTab = group.tabs().getFirst();

            Dim2i tabHeaderDim = new Dim2i(0, headerY, width, headerHeight);
            setDimPoint(tabHeaderDim, (Point2iAccess) (Object) this.tabSection);
            this.children.add(new TabHeaderComponent(tabHeaderDim, group.modOptions(), () -> this.activateHeader(group), this.isHeaderSelected(group, expanded)));

            offsetY += headerHeight + (firstGroup ? 0 : TAB_HEADER_PADDING);
            firstGroup = false;

            if (!collapsedSinglePageGroup && expanded) {
                for (Tab<?> tab : group.tabs()) {
                    Dim2i tabDim = new Dim2i(0, offsetY, width, TAB_HEIGHT);
                    setDimPoint(tabDim, (Point2iAccess) (Object) this.tabSection);
                    this.children.add(this.createTabButton(tab, tabDim));
                    offsetY += TAB_HEIGHT;
                }
            }
        }
    }

    private FlatButtonWidget createTabButton(Tab<?> tab, Dim2i tabDim) {
        ButtonTheme buttonTheme = ReeseSodiumOptionsConfig.config().isColorThemes()
                ? new ButtonTheme(
                        tab.getModOptions().theme(),
                        FlatButtonWidget.DEFAULT_THEME.bgHighlight,
                        FlatButtonWidget.DEFAULT_THEME.bgDefault,
                        FlatButtonWidget.DEFAULT_THEME.bgInactive
                )
                : FlatButtonWidget.DEFAULT_THEME;
        FlatButtonWidget button = new FlatButtonWidget(tabDim, tab.getTitle(), () -> this.setTab(Optional.of(tab)), true, true, buttonTheme);
        button.setSelected(this.isSelected(tab) && !(tab.getPage() instanceof ExternalPage));
        if (ReeseSodiumOptionsConfig.config().isColorThemes() && button instanceof FlatButtonWidgetExtended buttonExtended) {
            buttonExtended.setTab(true);
        }
        return button;
    }

    private boolean isSelected(Tab<?> tab) {
        return this.selectedTab.isPresent() && this.selectedTab.get() == tab;
    }

    private boolean isGroupExpanded(TabGroup group) {
        if (group.tabs().size() <= 1) {
            return this.collapseMode() == ReeseSodiumOptionsConfig.TabHeaderCollapseMode.SELECTED_GROUP
                    ? group.id().equals(this.tabSectionSelectedGroup.get())
                    : !this.shouldCollapseSinglePageGroups();
        }

        return switch (this.collapseMode()) {
            case SELECTED_GROUP -> group.id().equals(this.tabSectionSelectedGroup.get());
            case ALL_EXPANDED -> true;
            case MANUAL -> !this.manuallyCollapsedTabGroups.contains(group.id());
        };
    }

    private boolean isHeaderSelected(TabGroup group, boolean expanded) {
        Tab<?> firstTab = group.tabs().getFirst();
        if (group.tabs().size() == 1) {
            return this.shouldCollapseSinglePageGroups() && this.isSelected(firstTab);
        }

        return !expanded && this.selectedTab.filter(group.tabs()::contains).isPresent();
    }

    private void activateHeader(TabGroup group) {
        switch (this.collapseMode()) {
            case SELECTED_GROUP -> {
                this.tabSectionSelectedGroup.set(group.id());
                this.setTab(Optional.of(group.tabs().getFirst()));
            }
            case ALL_EXPANDED -> this.setTab(Optional.of(group.tabs().getFirst()));
            case MANUAL -> {
                if (group.tabs().size() == 1) {
                    this.setTab(Optional.of(group.tabs().getFirst()));
                } else {
                    this.toggleManualGroup(group);
                }
            }
        }
    }

    private void toggleManualGroup(TabGroup group) {
        if (!this.manuallyCollapsedTabGroups.remove(group.id())) {
            this.manuallyCollapsedTabGroups.add(group.id());
        }

        this.buildFrame();
    }

    private void showRestoredSelectedTab(Tab<?> tab) {
        for (TabGroup group : this.buildTabGroups()) {
            if (group.id().equals(tab.getModOptions().configId())) {
                switch (this.collapseMode()) {
                    case SELECTED_GROUP -> this.tabSectionSelectedGroup.set(group.id());
                    case MANUAL -> this.manuallyCollapsedTabGroups.remove(group.id());
                    case ALL_EXPANDED -> {
                    }
                }
            }
        }
    }

    private List<TabGroup> buildTabGroups() {
        Map<String, TabGroup> tabGroups = new LinkedHashMap<>();
        for (Tab<?> tab : this.tabs) {
            String id = tab.getModOptions().configId();
            tabGroups.computeIfAbsent(id, ignored -> new TabGroup(id, tab.getModOptions(), new ArrayList<>())).tabs().add(tab);
        }

        return List.copyOf(tabGroups.values());
    }

    private int calculateMaximumTabSectionHeight(List<TabGroup> tabGroups) {
        if (tabGroups.isEmpty()) {
            return 0;
        }

        return tabGroups.stream()
                .mapToInt(group -> (this.shouldShowTabHeaders() ? this.getTabHeaderHeight() : 0) + group.tabs().size() * TAB_HEIGHT)
                .sum() + (this.shouldShowTabHeaders() ? (tabGroups.size() - 1) * TAB_HEADER_PADDING : 0);
    }

    private int calculateVisibleTabSectionHeight(List<TabGroup> tabGroups) {
        if (tabGroups.isEmpty()) {
            return 0;
        }

        return tabGroups.stream()
                .mapToInt(group -> {
                    if (!this.shouldShowTabHeaders()) {
                        return group.tabs().size() * TAB_HEIGHT;
                    }

                    return this.getTabHeaderHeight() + (this.isGroupExpanded(group) ? group.tabs().size() * TAB_HEIGHT : 0);
                })
                .sum() + (this.shouldShowTabHeaders() ? (tabGroups.size() - 1) * TAB_HEADER_PADDING : 0);
    }

    private ReeseSodiumOptionsConfig.TabHeaderCollapseMode collapseMode() {
        return ReeseSodiumOptionsConfig.config().getTabHeaderCollapseMode();
    }

    private boolean shouldShowTabHeaders() {
        return this.collapseMode() != ReeseSodiumOptionsConfig.TabHeaderCollapseMode.ALL_EXPANDED || ReeseSodiumOptionsConfig.config().isTabHeaders();
    }

    private boolean shouldCollapseSinglePageGroups() {
        return ReeseSodiumOptionsConfig.config().isCollapseSinglePageGroups();
    }

    private int getTabHeaderHeight() {
        return ReeseSodiumOptionsConfig.config().isTabHeaderVersionLabels() ? TAB_HEADER_VERSION_HEIGHT : TAB_HEADER_HEIGHT;
    }

    private int calculateTabSectionWidth(Dim2i frameDim) {
        int textWidth = this.tabs.stream()
                .mapToInt(this::getTabSectionTextWidth)
                .max()
                .orElse(0);
        int preferredWidth = textWidth + (this.tabSectionCanScroll ? TAB_SECTION_SCROLL_PADDING : TAB_SECTION_PADDING);
        int maximumWidth = Math.min(TAB_SECTION_MAX_WIDTH, (int) (frameDim.width() * TAB_SECTION_MAX_WIDTH_RATIO));

        return clamp(preferredWidth, TAB_SECTION_MIN_WIDTH, maximumWidth);
    }

    private int getTabSectionTextWidth(Tab<?> tab) {
        int tabTitleWidth = this.getStringWidth(tab.getTitle());
        if (!this.shouldShowTabHeaders()) {
            return tabTitleWidth;
        }

        int headerTextWidth = Minecraft.getInstance().font.width(tab.getModOptions().name());
        if (ReeseSodiumOptionsConfig.config().isTabHeaderVersionLabels()) {
            headerTextWidth = Math.max(headerTextWidth, Minecraft.getInstance().font.width(tab.getModOptions().version()));
        }
        if (ReeseSodiumOptionsConfig.config().isTabHeaderIcons() && tab.getModOptions().icon() != null) {
            headerTextWidth += this.getTabHeaderHeight();
        }

        return Math.max(tabTitleWidth, headerTextWidth);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, Math.max(min, max)));
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

    public Optional<String> getSelectedTabKey() {
        return this.selectedTab.map(TabFrame::getTabKey);
    }

    public ControlElement findSelectedControl(ResourceLocation optionId) {
        if (this.selectedFrame == null) {
            return null;
        }

        return this.selectedFrame.findFirstControlElement(control -> control.getOption() instanceof OptionExtended optionExtended
                && optionExtended.getId().equals(optionId));
    }

    public ControlElement findFirstSelectedControl() {
        if (this.selectedFrame instanceof ScrollableFrame scrollableFrame) {
            return scrollableFrame.findFirstControlElement();
        }

        return this.selectedFrame == null ? null : this.selectedFrame.findFirstControlElement(control -> true);
    }

    public ControlElement findLastSelectedControl() {
        if (this.selectedFrame instanceof ScrollableFrame scrollableFrame) {
            return scrollableFrame.findLastControlElement();
        }

        return this.selectedFrame == null ? null : this.selectedFrame.findLastControlElement(control -> true);
    }

    public ControlElement findFirstVisibleSelectedControl() {
        if (this.selectedFrame instanceof ScrollableFrame scrollableFrame) {
            return scrollableFrame.findFirstVisibleControlElement();
        }

        return this.findFirstSelectedControl();
    }

    public ControlElement findLastVisibleSelectedControl() {
        if (this.selectedFrame instanceof ScrollableFrame scrollableFrame) {
            return scrollableFrame.findLastVisibleControlElement();
        }

        return this.findLastSelectedControl();
    }

    public boolean scrollSelectedPageToStart() {
        return this.selectedFrame instanceof ScrollableFrame scrollableFrame && scrollableFrame.scrollToStart();
    }

    public boolean scrollSelectedPageToEnd() {
        return this.selectedFrame instanceof ScrollableFrame scrollableFrame && scrollableFrame.scrollToEnd();
    }

    public boolean scrollSelectedPage(int direction) {
        return this.selectedFrame instanceof ScrollableFrame scrollableFrame && scrollableFrame.scrollPage(direction);
    }

    private static String getTabKey(Tab<?> tab) {
        return tab.getModOptions().configId() + ":" + tab.getTitle().getString();
    }

    private record TabGroup(String id, ModOptions modOptions, List<Tab<?>> tabs) {
    }

    public static class Builder {
        private final List<Tab<?>> functions = new ArrayList<>();
        private Dim2i dim;
        private boolean renderOutline;
        private Runnable onSetTab;
        private AtomicReference<Component> tabSectionSelectedTab = new AtomicReference<>(null);
        private AtomicReference<String> tabSectionSelectedGroup = new AtomicReference<>(null);
        private AtomicReference<Integer> tabSectionScrollBarOffset = new AtomicReference<>(0);
        private Set<String> manuallyCollapsedTabGroups = new HashSet<>();
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

        public Builder setTabSectionSelectedGroup(AtomicReference<String> tabSectionSelectedGroup) {
            this.tabSectionSelectedGroup = tabSectionSelectedGroup;
            return this;
        }

        public Builder setTabSectionScrollBarOffset(AtomicReference<Integer> tabSectionScrollBarOffset) {
            this.tabSectionScrollBarOffset = tabSectionScrollBarOffset;
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

            return new TabFrame(this.dim, this.screen, this.modOptions, this.renderOutline, this.functions, this.onSetTab, this.tabSectionSelectedTab, this.tabSectionSelectedGroup, this.tabSectionScrollBarOffset, this.manuallyCollapsedTabGroups);
        }
    }
}
