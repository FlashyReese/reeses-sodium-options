package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.SearchResultOrder;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.LabelWidget;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class PageFrame extends AbstractFrame {
    protected final Page page;
    private final OptionStateStore optionStateStore;
    private final OptionRowFactory optionRowFactory;
    private final OptionTooltipController tooltipController;
    private final Map<ResourceLocation, LabelWidget> groupHeaderWidgets = new HashMap<>();
    private final Map<ResourceLocation, OptionRow> optionRowWidgets = new HashMap<>();
    private @Nullable Consumer<ResourceLocation> groupToggleRebuildHandler;
    private PageLayout layout;

    public PageFrame(Screen screen, LayoutBounds dim, boolean renderOutline, Page page, ModOptions modOptions, OptionStateStore optionStateStore) {
        super(dim, screen, renderOutline, modOptions);
        this.page = page;
        this.optionStateStore = optionStateStore;
        this.optionRowFactory = new OptionRowFactory(screen, this.optionRowTheme(), this.optionStateStore);
        this.tooltipController = new OptionTooltipController(dim, modOptions, new OptionTooltipController.BoxRenderer() {
            @Override
            public void drawRect(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
                PageFrame.this.drawRect(guiGraphics, x1, y1, x2, y2, color);
            }

            @Override
            public void drawBorder(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
                PageFrame.this.drawBorder(guiGraphics, x1, y1, x2, y2, color);
            }
        });
        this.setupFrame();
        this.buildFrame();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setupFrame() {
        this.layout = PageLayout.create(this.page, ReeseSodiumOptionsConfig.config().isHideNonMatchingOptions() && this.optionStateStore.searchActive(),
                this.optionStateStore.searchResults(), SearchResultOrder.DEFAULT,
                ReeseSodiumOptionsConfig.config().isCollapsibleGroups(), this.optionStateStore.collapsedOptionGroups());
        this.setContentHeight(this.layout.contentHeight());
        this.optionRowFactory.registerParentBounds(this.layout, this.getFrameDim());
    }

    @Override
    public void rebuildFrameContent() {
        this.setupFrame();
        this.buildFrame();
    }

    @Override
    public void buildFrame() {
        this.children.clear();
        this.optionRows.clear();

        for (PageLayout.Row row : this.layout.rows()) {
            if (row instanceof PageLayout.LabelRow labelRow) {
                this.children.add(this.createLabelWidget(labelRow));
            } else if (row instanceof PageLayout.OptionRow optionRow) {
                this.addOptionRow(optionRow);
            }
        }

        super.buildFrame();
    }

    private LabelWidget createLabelWidget(PageLayout.LabelRow labelRow) {
        LayoutBounds dim = this.createRowDimension(labelRow.y());

        if (!labelRow.collapsible()) {
            return new LabelWidget(dim, labelRow.text(), 0xFFFFFFFF);
        }

        ResourceLocation collapseKey = labelRow.collapseKey();
        LabelWidget widget = this.groupHeaderWidgets.computeIfAbsent(collapseKey, key ->
                new LabelWidget(dim, labelRow.text(), 0xFFFFFFFF, this.optionRowTheme(), key, () -> this.toggleGroup(key), labelRow.collapsed()));
        widget.setDim(dim);
        widget.setCollapsed(labelRow.collapsed());

        return widget;
    }

    private void toggleGroup(ResourceLocation collapseKey) {
        Set<ResourceLocation> collapsed = this.optionStateStore.collapsedOptionGroups();
        if (!collapsed.remove(collapseKey)) {
            collapsed.add(collapseKey);
        }
        if (this.groupToggleRebuildHandler != null) {
            this.groupToggleRebuildHandler.accept(collapseKey);
        } else {
            this.rebuildFrameContent();
            this.focusGroupHeader(collapseKey);
        }
    }

    public void setGroupToggleRebuildHandler(@Nullable Consumer<ResourceLocation> groupToggleRebuildHandler) {
        this.groupToggleRebuildHandler = groupToggleRebuildHandler;
    }

    public boolean focusGroupHeader(ResourceLocation collapseKey) {
        for (GuiEventListener child : this.children) {
            if (child instanceof LabelWidget labelWidget && collapseKey.equals(labelWidget.collapseKey())) {
                this.setFocused(labelWidget);
                return true;
            }
        }

        return false;
    }

    private void addOptionRow(PageLayout.OptionRow row) {
        LayoutBounds rowDim = this.createRowDimension(row.y());
        this.children.add(this.getOptionRow(row.option(), rowDim));
    }

    private OptionRow getOptionRow(Option option, LayoutBounds dim) {
        ResourceLocation optionId = optionId(option);
        if (optionId == null) {
            return this.optionRowFactory.create(option, dim);
        }

        OptionRow optionRow = this.optionRowWidgets.get(optionId);
        if (optionRow == null || optionRow.getOption() != option) {
            optionRow = this.optionRowFactory.create(option, dim);
            this.optionRowWidgets.put(optionId, optionRow);
            return optionRow;
        }

        if (optionRow instanceof BaseWidget widget) {
            widget.setDim(dim);
        }
        this.optionRowFactory.registerOptionBounds(optionRow, dim);

        return optionRow;
    }

    private static @Nullable ResourceLocation optionId(Option option) {
        return option instanceof OptionExtended optionExtended ? optionExtended.rso$getId() : null;
    }

    private LayoutBounds createRowDimension(int y) {
        return LayoutBounds.relativeTo(this.getFrameDim(), 0, y, this.getWidth(), PageLayout.ROW_HEIGHT);
    }

    private void setContentHeight(int height) {
        this.setDim(this.getDimensions().withHeight(height));
    }

    @Override
    public void updateFrameDim(LayoutBounds dim) {
        this.setDim(dim);
        this.relayoutRows();
    }

    private void relayoutRows() {
        this.optionRowFactory.registerParentBounds(this.layout, this.getFrameDim());

        int childIndex = 0;
        for (PageLayout.Row row : this.layout.rows()) {
            if (childIndex >= this.children.size()) {
                return;
            }

            GuiEventListener child = this.children.get(childIndex++);
            if (child instanceof BaseWidget widget) {
                widget.setDim(this.createRowDimension(row.y()));
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.tooltipController.render(guiGraphics, this.optionRows, mouseX, mouseY);
    }

    private GuiTheme optionRowTheme() {
        return ReeseSodiumOptionsConfig.config().isColorThemes() ? GuiThemes.fromSodium(this.modOptions.theme()) : GuiThemes.DEFAULT_BUTTON;
    }

    public static class Builder {
        private LayoutBounds dim;
        private boolean renderOutline;
        private Page page;
        private Screen screen;
        private ModOptions modOptions;
        private OptionStateStore optionStateStore;

        public Builder withDimension(LayoutBounds dim) {
            this.dim = dim;
            return this;
        }

        public Builder withRenderOutline(boolean renderOutline) {
            this.renderOutline = renderOutline;
            return this;
        }

        public Builder withPage(Page page) {
            this.page = page;
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

        public Builder withOptionStateStore(OptionStateStore optionStateStore) {
            this.optionStateStore = optionStateStore;
            return this;
        }

        public PageFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");
            Validate.notNull(this.page, "Option Page must be specified");
            Validate.notNull(this.screen, "Screen must be specified");
            Validate.notNull(this.modOptions, "Mod Options must be specified");
            Validate.notNull(this.optionStateStore, "Option state store must be specified");

            return new PageFrame(this.screen, this.dim, this.renderOutline, this.page, this.modOptions, this.optionStateStore);
        }
    }
}
