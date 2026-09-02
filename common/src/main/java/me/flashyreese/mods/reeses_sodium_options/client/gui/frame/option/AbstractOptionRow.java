package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuide;
import me.flashyreese.mods.reeses_sodium_options.client.gui.control.ControlGuideProvider;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionUiState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiThemes;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action.OptionActionButtonController;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action.OptionUndoAction;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract class AbstractOptionRow extends BaseWidget implements ContainerEventHandler, OptionRow, ControlGuideProvider {
    protected static final int CONTROL_RIGHT_PADDING = 6;
    private static final int SEARCH_RESULT_MARKER = 0x66FFFFFF;
    private static final int SEARCH_RESULT_MARKER_WIDTH = 2;

    protected final OptionActionButtonController actionButtons;
    protected final GuiTheme theme;
    private final OptionStateStore optionStateStore;
    private final Option option;
    private final @Nullable StatefulOption<?> statefulOption;
    private boolean rowFocused;
    private boolean dragging;

    AbstractOptionRow(LayoutBounds dim, GuiTheme theme, OptionStateStore optionStateStore, Option option) {
        super(dim);
        this.theme = theme;
        this.optionStateStore = optionStateStore;
        this.option = option;
        this.statefulOption = option instanceof StatefulOption<?> statefulOption ? statefulOption : null;
        this.actionButtons = new OptionActionButtonController(
                this::visibleBounds,
                () -> this.statefulOption,
                this::playClickSound
        );
    }

    @Override
    public Option getOption() {
        return this.option;
    }

    private int getContentWidth() {
        return this.controlContentWidth() + this.actionButtons.actionButtonWidth();
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.statefulOption != null) {
            // Collapse no-op changes (validated value back to applied) so change tracking stays
            // accurate. Done here as an explicit per-frame step rather than inside isActive/canUndo,
            // keeping those queries free of side effects.
            OptionUndoAction.normalizeEquivalentChange(this.statefulOption);
        }

        if (!isLeftMouseButtonDown()) {
            this.releaseMouseHold();
            this.actionButtons.releaseLayoutHold();
        }

        this.prepareRender(mouseX, mouseY, delta);
        this.renderRow(guiGraphics, mouseX, mouseY);
        this.renderControl(guiGraphics, mouseX, mouseY, delta);
        this.actionButtons.render(guiGraphics, mouseX, mouseY);
    }

    @Override
    public final boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return this.actionButtons.mouseClicked(event, doubleClick)
                || this.controlMouseClicked(event, doubleClick);
    }

    @Override
    public final boolean keyPressed(@NonNull KeyEvent event) {
        return this.actionButtons.keyPressedFocusedChild(event)
                || this.controlKeyPressed(event);
    }

    protected boolean controlMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.tryActivateControl(event);
    }

    protected boolean controlKeyPressed(KeyEvent event) {
        return this.rowFocused && event.isSelection() && this.activateControl();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY)
                || this.actionButtons.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isFocused() {
        return this.rowFocused || this.actionButtons.getFocused() != null;
    }

    @Override
    public void setFocused(boolean focused) {
        this.rowFocused = focused;
        if (!focused) {
            this.actionButtons.clearFocus();
            this.onControlFocusLost();
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NonNull FocusNavigationEvent navigation) {
        OptionActionButtonController.FocusPathResult result = this.actionButtons
                .nextFocusPath(this, this, this.rowFocused, navigation);
        if (result.handled()) {
            return result.path();
        }

        if (!this.getOption().isEnabled() || this.isFocused()) {
            return null;
        }

        return ComponentPath.leaf(this);
    }

    @Override
    public @Nullable ComponentPath getCurrentFocusPath() {
        return this.actionButtons.currentFocusPath(this, this, this.rowFocused);
    }

    @Override
    public @NonNull NarrationPriority narrationPriority() {
        if (this.rowFocused) {
            return NarrationPriority.FOCUSED;
        }

        return this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public @NonNull Collection<? extends NarratableEntry> getNarratables() {
        List<NarratableEntry> narratables = new ArrayList<>();
        narratables.add(this);

        for (GuiEventListener child : this.actionButtons.children()) {
            if (child instanceof NarratableEntry narratable) {
                narratables.addAll(narratable.getNarratables());
            }
        }

        return narratables;
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput builder) {
        Component value = this.narrationValue();
        Component title = value == null ? this.getOption().getName() : CommonComponents.optionNameValue(this.getOption().getName(), value);

        builder.add(NarratedElementType.TITLE, title);
        this.updateControlNarration(builder);
        this.updateTooltipNarration(builder);
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return this.actionButtons.getFocused();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        this.actionButtons.setFocused(focused);
        if (focused != null) {
            this.rowFocused = false;
            this.onControlFocusLost();
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.actionButtons.children();
    }

    @Override
    public void releaseActionButtonLayoutHold() {
        this.actionButtons.releaseLayoutHold();
    }

    @Override
    public boolean handleBackNavigation() {
        return false;
    }

    @Override
    public boolean undoFocusedActionButton() {
        return this.actionButtons.undoFocusedButton();
    }

    @Override
    public void clearActionButtonFocus() {
        this.actionButtons.clearFocus();
    }

    protected int controlLimitX() {
        return this.getLimitX() - this.actionButtons.actionButtonWidth();
    }

    protected boolean isRowFocused() {
        return this.rowFocused;
    }

    @Override
    public List<ControlGuide> controlGuides() {
        return List.of();
    }

    protected boolean canShowControlGuide() {
        return this.isRowFocused() && this.getOption().isEnabled() && this.optionShowsControl();
    }

    protected boolean isMouseOverRow(double mouseX, double mouseY) {
        return mouseX >= this.getX()
                && mouseX < this.controlLimitX()
                && mouseY >= this.getY()
                && mouseY < this.getLimitY();
    }

    protected int centeredTextY() {
        return (int) (this.getY() + Math.ceil((double) (this.getHeight() - this.font.lineHeight) / 2));
    }

    protected int rightAlignedControlX(int width) {
        return this.controlLimitX() - CONTROL_RIGHT_PADDING - width;
    }

    protected void requestPointerCursorIfHovered(GuiGraphics guiGraphics) {
        if (this.isHovered()) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    protected void prepareRender(int mouseX, int mouseY, float delta) {
    }

    protected void releaseMouseHold() {
    }

    protected void onControlFocusLost() {
    }

    protected MutableComponent formatDisabledControlValue(Component value) {
        return value.copy()
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));
    }

    protected @Nullable Component narrationValue() {
        return null;
    }

    protected void updateControlNarration(NarrationElementOutput builder) {
        if (this.getOption().isEnabled() && this.optionShowsControl()) {
            this.addButtonUsageNarration(builder);
        } else if (!this.getOption().isEnabled()) {
            builder.add(NarratedElementType.HINT, Component.translatable("rso.narration.option_unavailable"));
        }
    }

    protected boolean optionShowsControl() {
        return this.statefulOption == null || this.statefulOption.showControl();
    }

    private void updateTooltipNarration(NarrationElementOutput builder) {
        Component tooltip = this.getOption().getTooltip();

        if (tooltip != null && !tooltip.getString().isBlank()) {
            builder.add(NarratedElementType.HINT, tooltip);
        }
    }

    protected abstract int controlContentWidth();

    protected abstract void renderControl(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta);

    protected abstract boolean activateControl();

    private void renderRow(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Option option = this.getOption();
        String label = this.truncateLabel(option.getName().getString());
        String formattedLabel = this.formatLabel(option, label);
        int rowLimitX = this.controlLimitX();
        OptionUiState optionUiState = this.optionUiState();
        boolean selectedSearchResult = this.isSelectedSearchResult(optionUiState);

        this.hovered = this.isMouseOverRow(mouseX, mouseY);
        this.drawRect(
                guiGraphics,
                this.getX(),
                this.getY(),
                rowLimitX,
                this.getLimitY(),
                this.hovered || selectedSearchResult ? 0xE0000000 : 0x40000000
        );
        this.renderSearchResultMarker(guiGraphics, optionUiState);
        this.drawString(guiGraphics, formattedLabel, this.getX() + CONTROL_RIGHT_PADDING, this.centeredTextY(), 0xFFFFFFFF);

        int borderColor = this.rowBorderColor(optionUiState);
        if (borderColor != 0) {
            this.drawBorder(guiGraphics, this.getX(), this.getY(), rowLimitX, this.getLimitY(), borderColor);
        }
    }

    private void renderSearchResultMarker(GuiGraphics guiGraphics, @Nullable OptionUiState optionUiState) {
        if (optionUiState == null || !optionUiState.isHighlighted() || optionUiState.isSelected()) {
            return;
        }

        this.drawRect(guiGraphics, this.getX(), this.getY(), this.getX() + SEARCH_RESULT_MARKER_WIDTH, this.getLimitY(), SEARCH_RESULT_MARKER);
    }

    private int rowBorderColor(@Nullable OptionUiState optionUiState) {
        if (this.shouldRenderFocusBorder(this.isRowFocused())) {
            return GuiThemes.OPTION_FOCUS_BORDER;
        }

        return this.isSelectedSearchResult(optionUiState) ? GuiThemes.OPTION_FOCUS_BORDER : 0;
    }

    private boolean isSelectedSearchResult(@Nullable OptionUiState optionUiState) {
        return optionUiState != null && optionUiState.isHighlighted() && optionUiState.isSelected();
    }

    private @Nullable OptionUiState optionUiState() {
        if (!(this.option instanceof OptionExtended optionExtended)) {
            return null;
        }

        return this.optionStateStore.optionUiState(optionExtended.rso$getId());
    }

    private String truncateLabel(String label) {
        return this.truncateTextToFit(label, this.getWidth() - this.getContentWidth() - 20);
    }

    private String formatLabel(Option option, String label) {
        String formattedLabel;

        if (option.isEnabled()) {
            formattedLabel = option.hasChanged()
                    ? ChatFormatting.ITALIC + label
                    : ChatFormatting.WHITE + label;
        } else {
            formattedLabel = ChatFormatting.GRAY.toString() + ChatFormatting.STRIKETHROUGH + label;
        }

        return formattedLabel;
    }

    private boolean tryActivateControl(MouseButtonEvent event) {
        return event.button() == 0
                && this.isMouseOverRow(event.x(), event.y())
                && this.activateControl();
    }

    private LayoutBounds visibleBounds() {
        return this.getDimensions();
    }

    private static boolean isLeftMouseButtonDown() {
        long window = Minecraft.getInstance().getWindow().handle();

        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
}
