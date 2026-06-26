package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.OptionRow;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option.action.OptionUndoAction;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.FlatButtonWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.search.SearchTextFieldWidget;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.option.OptionExtended;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionsScreenUiState;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.data.fingerprint.HashedFingerprint;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPrompt;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPromptable;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SodiumVideoOptionsScreen extends Screen implements ScreenPromptable, PreviousScreenHolder {

    private static final OptionsScreenUiState SHARED_UI_STATE = new OptionsScreenUiState();
    private static final Set<String> RSO_CONFIG_IDS = Set.of("reeses-sodium-options", "reeses_sodium_options");

    private static final double ASPECT_RATIO_16_9 = 16.0 / 9.0;
    private static final int TOOLBAR_BUTTON_WIDTH = 65;
    private static final int TOOLBAR_BUTTON_GAP = 4;
    private static final int TOOLBAR_BUTTON_Y_GAP = 5;
    private static final int TOP_ROW_HEIGHT = 20;
    private static final int TOP_ROW_Y_GAP = 26;

    private static final List<FormattedText> DONATION_PROMPT_MESSAGE;

    static {
        DONATION_PROMPT_MESSAGE = List.of(
                FormattedText.composite(Component.literal("Hello!")),
                FormattedText.composite(Component.literal("It seems that you've been enjoying "), Component.literal("Sodium").withColor(0x27eb92), Component.literal(", the free and open-source optimization mod for Minecraft.")),
                FormattedText.composite(Component.literal("Mods like these are complex. They require "), Component.literal("thousands of hours").withColor(0xff6e00), Component.literal(" of development, debugging, and tuning to create the experience that players have come to expect.")),
                FormattedText.composite(Component.literal("If you'd like to show your token of appreciation, and support the development of our mod in the process, then consider "), Component.literal("buying us a coffee").withColor(0xed49ce), Component.literal(".")),
                FormattedText.composite(Component.literal("And thanks again for using our mod! We hope it helps you (and your computer.)"))
        );
    }

    private final Screen prevScreen;
    private final OptionsScreenUiState uiState;
    private FlatButtonWidget applyButton, closeButton, undoButton;
    private FlatButtonWidget donateButton, hideDonateButton;
    private boolean hasPendingChanges;
    private SearchTextFieldWidget searchTextField;
    private AbstractFrame rootFrame;
    private TabFrame tabFrame;
    private @Nullable ScreenPrompt prompt;
    private @Nullable ComponentPath previousArrowFocusPath;
    private @Nullable GuiEventListener currentArrowFocusLeaf;
    private @Nullable ScreenDirection lastArrowDirection;

    public SodiumVideoOptionsScreen(Screen prev) {
        super(Component.literal("Reese's Sodium Menu"));
        this.prevScreen = prev;
        this.uiState = SHARED_UI_STATE;

        this.checkPromptTimers();

        ConfigManager.CONFIG.resetAllOptionsFromBindings();
    }

    @Override
    public Screen rso$previousScreen() {
        return this.prevScreen;
    }

    public @Nullable FlatButtonWidget rso$getApplyButton() {
        return this.applyButton;
    }

    public @Nullable FlatButtonWidget rso$getCloseButton() {
        return this.closeButton;
    }

    public @Nullable FlatButtonWidget rso$getUndoButton() {
        return this.undoButton;
    }

    public @Nullable SearchTextFieldWidget rso$getSearchTextField() {
        return this.searchTextField;
    }

    public @Nullable TabFrame rso$getTabFrame() {
        return this.tabFrame;
    }

    private void checkPromptTimers() {
        // Never show the prompt in developer workspaces.
        if (PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment()) {
            return;
        }

        var options = SodiumClientMod.options();

        // If the user has disabled the nags forcefully (by config), or has already seen the prompt, don't show it again.
        if (options.notifications.hasSeenDonationPrompt) {
            return;
        }

        HashedFingerprint fingerprint = null;

        try {
            fingerprint = HashedFingerprint.loadFromDisk();
        } catch (Throwable t) {
            SodiumClientMod.logger()
                    .error("Failed to read the fingerprint from disk", t);
        }

        // If the fingerprint doesn't exist, or failed to be loaded, abort.
        if (fingerprint == null) {
            return;
        }

        // The fingerprint records the installation time. If it's been a while since installation, show the user
        // a prompt asking for them to consider donating.
        var now = Instant.now();
        var threshold = Instant.ofEpochSecond(fingerprint.timestamp())
                .plus(3, ChronoUnit.DAYS);

        if (now.isAfter(threshold)) {
            this.openDonationPrompt(options);
        }
    }

    private void openDonationPrompt(SodiumOptions options) {
        var prompt = new ScreenPrompt(this, DONATION_PROMPT_MESSAGE, 320, 190,
                new ScreenPrompt.Action(Component.literal("Buy us a coffee"), this::openDonationPage));
        prompt.setFocused(true);

        options.notifications.hasSeenDonationPrompt = true;

        try {
            SodiumOptions.writeToDisk(options);
        } catch (IOException e) {
            SodiumClientMod.logger()
                    .error("Failed to update config file", e);
        }
    }

    // Hackalicious! Rebuild UI
    public void rebuildUI() {
        boolean wasSearchBarFocused = this.searchTextField.isFocused();
        this.clearArrowNavigationMemory();
        this.rebuildWidgets();
        if (wasSearchBarFocused) this.focusSearchTextField();
    }

    private void refreshSearchResults() {
        this.clearArrowNavigationMemory();
        if (this.tabFrame != null) {
            this.tabFrame.refreshFromState();
        }
    }

    @Override
    protected void init() {
        super.init();

        BaseWidget.setKeyboardFocusVisible(this.minecraft.getLastInputType().isKeyboard());
        ConfigManager.CONFIG.invalidateGlobalRebuildDependents();

        this.rootFrame = this.parentFrameBuilder().build();
        this.addRenderableWidget(this.rootFrame);

        if (this.searchTextField.isFocused()) {
            this.focusSearchTextField();
        } else if (this.restoreFocusedOptionForSelectedTab()) {
            this.rememberCurrentOptionFocus();
        } else {
            this.setFocused(this.rootFrame);
        }

        if (this.prompt != null) {
            this.prompt.init();
        }
    }

    private void focusSearchTextField() {
        this.searchTextField.setFocused(true);

        if (this.rootFrame != null) {
            this.rootFrame.setFocused(this.searchTextField);
            this.setFocused(this.rootFrame);
        } else {
            this.setFocused(this.searchTextField);
        }
    }

    private BasicFrame.Builder parentFrameBuilder() {
        boolean donationCleared = SodiumClientMod.options().notifications.hasClearedDonationButton;

        // Calculates if resolution exceeds 16:9 ratio, force 16:9
        int newWidth = this.width;
        if ((float) this.width / (float) this.height > ASPECT_RATIO_16_9) {
            newWidth = (int) (this.height * ASPECT_RATIO_16_9);
        }

        LayoutBounds basicFrameDim = new LayoutBounds((this.width - newWidth) / 2, 0, newWidth, this.height);
        LayoutBounds tabFrameDim = new LayoutBounds(basicFrameDim.x() + basicFrameDim.width() / 20 / 2, basicFrameDim.y() + basicFrameDim.height() / 4 / 2, basicFrameDim.width() - (basicFrameDim.width() / 20), basicFrameDim.height() / 4 * 3);

        int toolbarY = tabFrameDim.getLimitY() + TOOLBAR_BUTTON_Y_GAP;
        LayoutBounds closeButtonDim = new LayoutBounds(toolbarButtonX(tabFrameDim, 0), toolbarY, TOOLBAR_BUTTON_WIDTH, TOP_ROW_HEIGHT);
        LayoutBounds applyButtonDim = new LayoutBounds(toolbarButtonX(tabFrameDim, 1), toolbarY, TOOLBAR_BUTTON_WIDTH, TOP_ROW_HEIGHT);
        LayoutBounds undoButtonDim = new LayoutBounds(toolbarButtonX(tabFrameDim, 2), toolbarY, TOOLBAR_BUTTON_WIDTH, TOP_ROW_HEIGHT);

        Component donationText = Component.translatable("sodium.options.buttons.donate");
        int donationTextWidth = this.minecraft.font.width(donationText);

        int topRowY = tabFrameDim.y() - TOP_ROW_Y_GAP;
        LayoutBounds donateButtonDim = new LayoutBounds(tabFrameDim.getLimitX() - 32 - donationTextWidth, topRowY, 10 + donationTextWidth, TOP_ROW_HEIGHT);
        LayoutBounds hideDonateButtonDim = new LayoutBounds(tabFrameDim.getLimitX() - TOP_ROW_HEIGHT, topRowY, TOP_ROW_HEIGHT, TOP_ROW_HEIGHT);

        this.undoButton = new FlatButtonWidget(undoButtonDim, Component.translatable("sodium.options.buttons.undo"), ConfigManager.CONFIG::resetAllOptionsFromBindings, true, false);
        this.applyButton = new FlatButtonWidget(applyButtonDim, Component.translatable("sodium.options.buttons.apply"), ConfigManager.CONFIG::applyAllOptions, true, false);
        this.closeButton = new FlatButtonWidget(closeButtonDim, Component.translatable("gui.done"), this::onClose, true, false);

        this.donateButton = new FlatButtonWidget(donateButtonDim, donationText, this::openDonationPage, true, false);
        this.hideDonateButton = new FlatButtonWidget(hideDonateButtonDim, Component.literal("x"), this::hideDonationButton, true, false);

        if (donationCleared) {
            this.setDonationButtonVisibility(false);
        }

        BasicFrame.Builder basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);

        LayoutBounds searchTextFieldDim;
        if (donationCleared) {
            searchTextFieldDim = new LayoutBounds(tabFrameDim.x(), topRowY, tabFrameDim.width(), TOP_ROW_HEIGHT);
        } else {
            searchTextFieldDim = new LayoutBounds(tabFrameDim.x(), topRowY, tabFrameDim.width() - (tabFrameDim.getLimitX() - donateButtonDim.x()) - 2, TOP_ROW_HEIGHT);

            basicFrameBuilder
                    .addChild(() -> this.donateButton)
                    .addChild(() -> this.hideDonateButton);
        }

        this.searchTextField = new SearchTextFieldWidget(searchTextFieldDim, getOrderedModOptions(), this.uiState,
                tabFrameDim.height(), this::refreshSearchResults);

        basicFrameBuilder.addChild(() -> this.searchTextField);

        return basicFrameBuilder;
    }

    // Toolbar buttons are laid out right-to-left: slot 0 is the rightmost (close).
    private static int toolbarButtonX(LayoutBounds tabFrameDim, int slotFromRight) {
        return tabFrameDim.getLimitX() - (slotFromRight + 1) * TOOLBAR_BUTTON_WIDTH - slotFromRight * TOOLBAR_BUTTON_GAP;
    }

    private BasicFrame.Builder parentBasicFrameBuilder(LayoutBounds parentBasicFrameDim, LayoutBounds tabFrameDim) {
        return BasicFrame.builder()
                .withDimension(parentBasicFrameDim)
                .withRenderOutline(false)
                .withScreen(this)
                .addChild(() -> {
                    this.tabFrame = TabFrame.createBuilder()
                            .setDimension(tabFrameDim)
                            .withScreen(this)
                            .shouldRenderOutline(false)
                            .setTabRailScrollBarOffset(this.uiState.tabFrameScrollBarOffset())
                            .setScrollSelectedTabIntoView(this.uiState.scrollSelectedTabIntoView())
                            .setTabRailSelectedTab(this.uiState.tabFrameSelectedTab())
                            .setTabRailSelectedGroup(this.uiState.tabFrameSelectedGroup())
                            .setManuallyCollapsedTabGroups(this.uiState.manuallyCollapsedTabGroups())
                            .addTabs(tabs -> getOrderedModOptions()
                                    .forEach(config -> config.pages()
                                            .forEach(page -> tabs.add(Tab.builder().from(this, config, page, this.uiState.optionPageScrollBarOffset(), this.uiState))))
                            )
                            .onSetTab(() -> {
                                this.uiState.optionPageScrollBarOffset().set(0);
                            })
                            .build();
                    return this.tabFrame;
                })
                .addChild(() -> this.undoButton)
                .addChild(() -> this.applyButton)
                .addChild(() -> this.closeButton);
    }

    private static List<ModOptions> getOrderedModOptions() {
        return ConfigManager.CONFIG.getModOptions().stream()
                .sorted((left, right) -> Boolean.compare(isOwnConfig(left), isOwnConfig(right)))
                .toList();
    }

    private static boolean isOwnConfig(ModOptions modOptions) {
        return RSO_CONFIG_IDS.contains(modOptions.configId());
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        this.updateControls();
        super.extractRenderState(guiGraphics, this.prompt != null ? -1 : mouseX, this.prompt != null ? -1 : mouseY, delta);
        if (this.prompt != null) {
            this.prompt.extractRenderState(guiGraphics, mouseX, mouseY, delta);
        }
    }

    private void updateControls() {
        boolean hasChanges = ConfigManager.CONFIG.anyOptionChanged();

        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        this.hasPendingChanges = hasChanges;
    }

    private void setDonationButtonVisibility(boolean value) {
        this.donateButton.setVisible(value);
        this.hideDonateButton.setVisible(value);
    }

    private void hideDonationButton() {
        SodiumOptions options = SodiumClientMod.options();
        options.notifications.hasClearedDonationButton = true;

        try {
            SodiumOptions.writeToDisk(options);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration", e);
        }

        this.setDonationButtonVisibility(false);


        this.rebuildUI();
    }

    private void openDonationPage() {
        Util.getPlatform()
                .openUri("https://caffeinemc.net/donate");
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean repeated) {
        this.clearArrowNavigationMemory();
        String previousTabKey = this.getSelectedTabKey();

        if (this.prompt != null) {
            return this.prompt.mouseClicked(event, repeated);
        }

        boolean handled = super.mouseClicked(event, repeated);
        this.afterInput(previousTabKey);

        return handled;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        boolean handled = super.mouseReleased(event);

        if (event.button() == 0 && this.rootFrame != null) {
            this.rootFrame.releaseActionButtonLayoutHolds();
        }

        return handled;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        BaseWidget.setKeyboardFocusVisible(true);

        if (this.prompt != null) {
            return this.prompt.keyPressed(event);
        }

        String previousTabKey = this.getSelectedTabKey();

        if (this.isSearchShortcut(event)) {
            this.focusSearchTextField();
            this.searchTextField.selectAllText();
            this.clearArrowNavigationMemory();

            return true;
        }

        if (event.isEscape() && this.handleFocusedOptionBackNavigation()) {
            this.clearArrowNavigationMemory();
            this.afterInput(previousTabKey);

            return true;
        }

        if (event.isEscape() && this.clearSearchText()) {
            this.clearArrowNavigationMemory();

            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_P && event.hasShiftDown() && !this.isSearchTextFieldFocused()) {
            this.minecraft.gui.setScreen(new VideoSettingsScreen(this.prevScreen, this.minecraft, this.minecraft.options));

            return true;
        }

        if (!this.isSearchTextFieldFocused()) {
            if (this.isUndoShortcut(event) && this.undoFocusedOption()) {
                this.clearArrowNavigationMemory();
                this.afterInput(previousTabKey);

                return true;
            }

            if (this.keyPressedOptionListNavigation(event)) {
                this.clearArrowNavigationMemory();
                this.afterInput(previousTabKey);

                return true;
            }

            if (this.isApplyShortcut(event)) {
                GuiEventListener focused = this.getFocused();
                if (focused != null && focused.keyPressed(event)) {
                    this.clearArrowNavigationMemory();
                    this.afterInput(previousTabKey);

                    return true;
                }

                if (ConfigManager.CONFIG.anyOptionChanged()) {
                    ConfigManager.CONFIG.applyAllOptions();
                    this.updateControls();
                    this.clearArrowNavigationMemory();
                    this.afterInput(previousTabKey);

                    return true;
                }
            }
        }

        ScreenDirection arrowDirection = getArrowDirection(event);
        if (arrowDirection != null) {
            return this.keyPressedArrow(event, arrowDirection);
        }

        this.clearArrowNavigationMemory();

        boolean handled = super.keyPressed(event);
        if (handled) {
            this.afterInput(previousTabKey);
        }

        return handled;
    }

    private boolean isSearchShortcut(KeyEvent event) {
        return event.key() == GLFW.GLFW_KEY_F && event.hasControlDown();
    }

    private boolean isUndoShortcut(KeyEvent event) {
        return event.key() == GLFW.GLFW_KEY_Z && event.hasControlDown();
    }

    private boolean isApplyShortcut(KeyEvent event) {
        return event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER;
    }

    private boolean isSearchTextFieldFocused() {
        return this.searchTextField != null && this.searchTextField.isFocused();
    }

    private boolean clearSearchText() {
        if (this.searchTextField == null || !this.searchTextField.hasText()) {
            return false;
        }

        this.searchTextField.clearText();
        this.focusSearchTextField();

        return true;
    }

    private boolean keyPressedOptionListNavigation(KeyEvent event) {
        if (this.tabFrame == null || this.rootFrame == null) {
            return false;
        }

        OptionRow target;
        boolean handled = true;

        switch (event.key()) {
            case GLFW.GLFW_KEY_HOME -> {
                this.tabFrame.scrollSelectedPageToStart();
                target = this.tabFrame.findFirstSelectedOptionRow();
            }
            case GLFW.GLFW_KEY_END -> {
                this.tabFrame.scrollSelectedPageToEnd();
                target = this.tabFrame.findLastSelectedOptionRow();
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                handled = this.tabFrame.scrollSelectedPage(-1);
                target = this.tabFrame.findFirstVisibleSelectedOptionRow();
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                handled = this.tabFrame.scrollSelectedPage(1);
                target = this.tabFrame.findLastVisibleSelectedOptionRow();
            }
            default -> {
                return false;
            }
        }

        return this.focusOptionRow(target) || handled;
    }

    private boolean undoFocusedOption() {
        OptionRow focusedOptionRow = this.getFocusedOptionRow();
        if (focusedOptionRow == null || !(focusedOptionRow.getOption() instanceof StatefulOption<?> option)) {
            return false;
        }

        if (focusedOptionRow.undoFocusedActionButton()) {
            this.updateControls();
            return true;
        }

        if (!OptionUndoAction.canUndo(option)) {
            return false;
        }

        OptionUndoAction.undoChanges(option);
        focusedOptionRow.clearActionButtonFocus();
        this.updateControls();

        return true;
    }

    private boolean restoreFocusedOptionForSelectedTab() {
        if (this.tabFrame == null) {
            return false;
        }

        String tabKey = this.getSelectedTabKey();
        if (tabKey == null) {
            return false;
        }

        Identifier optionId = this.uiState.focusedOptionIdsByTab().get(tabKey);

        return optionId != null && this.focusOptionRow(this.tabFrame.findSelectedOptionRow(optionId));
    }

    private boolean focusOptionRow(@Nullable OptionRow optionRow) {
        if (optionRow == null || this.rootFrame == null || !this.rootFrame.focusOptionRow(optionRow)) {
            return false;
        }

        this.setFocused(this.rootFrame);

        return true;
    }

    private void afterInput(@Nullable String previousTabKey) {
        if (this.isSearchTextFieldFocused()) {
            return;
        }

        if (!Objects.equals(previousTabKey, this.getSelectedTabKey()) && this.restoreFocusedOptionForSelectedTab()) {
            return;
        }

        this.rememberCurrentOptionFocus();
    }

    private void rememberCurrentOptionFocus() {
        OptionRow focusedOptionRow = this.getFocusedOptionRow();
        String tabKey = this.getSelectedTabKey();

        if (tabKey != null && focusedOptionRow != null && focusedOptionRow.getOption() instanceof OptionExtended optionExtended) {
            this.uiState.focusedOptionIdsByTab().put(tabKey, optionExtended.rso$getId());
        }
    }

    private @Nullable OptionRow getFocusedOptionRow() {
        return this.rootFrame == null ? null : findFocusedOptionRow(this.rootFrame);
    }

    private boolean handleFocusedOptionBackNavigation() {
        OptionRow focusedOptionRow = this.getFocusedOptionRow();

        return focusedOptionRow != null && focusedOptionRow.handleBackNavigation();
    }

    private static @Nullable OptionRow findFocusedOptionRow(GuiEventListener listener) {
        if (listener instanceof OptionRow optionRow && optionRow.isFocused()) {
            return optionRow;
        }

        if (listener instanceof ContainerEventHandler container) {
            GuiEventListener focused = container.getFocused();
            if (focused != null) {
                return findFocusedOptionRow(focused);
            }
        }

        return null;
    }

    private @Nullable String getSelectedTabKey() {
        return this.tabFrame == null ? null : this.tabFrame.getSelectedTabKey().orElse(null);
    }

    public @Nullable String rso$getSelectedTabKey() {
        return this.getSelectedTabKey();
    }

    public boolean rso$cycleTab(int direction) {
        BaseWidget.setKeyboardFocusVisible(true);

        if (this.tabFrame == null || direction == 0) {
            return false;
        }

        List<Tab<?>> tabs = this.tabFrame.getTabs();
        if (tabs.isEmpty()) {
            return false;
        }

        int currentIndex = this.tabFrame.getSelectedTab()
                .map(tabs::indexOf)
                .filter(index -> index >= 0)
                .orElse(0);

        for (int offset = 1; offset <= tabs.size(); offset++) {
            int nextIndex = Math.floorMod(currentIndex + direction * offset, tabs.size());
            Tab<?> nextTab = tabs.get(nextIndex);
            if (nextTab.getPage() instanceof ExternalPage) {
                continue;
            }

            this.clearArrowNavigationMemory();
            this.tabFrame.setTab(Optional.of(nextTab));
            this.focusFirstOptionInSelectedTab();
            this.rememberCurrentOptionFocus();
            return true;
        }

        return false;
    }

    public boolean rso$focusFirstOptionInSelectedTab() {
        BaseWidget.setKeyboardFocusVisible(true);

        return this.focusFirstOptionInSelectedTab();
    }

    private boolean focusFirstOptionInSelectedTab() {
        if (this.tabFrame == null) {
            return false;
        }

        return this.focusOptionRow(this.tabFrame.findFirstSelectedOptionRow());
    }

    public boolean rso$navigateController(ScreenDirection direction) {
        BaseWidget.setKeyboardFocusVisible(true);

        if (this.prompt != null) {
            return this.keyPressed(new KeyEvent(keyForDirection(direction), 0, 0));
        }

        return this.keyPressedArrow(new KeyEvent(keyForDirection(direction), 0, 0), direction);
    }

    public boolean rso$handleControllerBack() {
        BaseWidget.setKeyboardFocusVisible(true);

        return this.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0));
    }

    public boolean rso$handleControllerPress() {
        BaseWidget.setKeyboardFocusVisible(true);

        return this.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
    }

    public void rso$afterControllerInput(@Nullable String previousTabKey) {
        this.afterInput(previousTabKey);
    }

    public @Nullable GuiEventListener rso$getFocusedLeaf() {
        return focusedLeaf(this.getFocused());
    }

    private static @Nullable GuiEventListener focusedLeaf(@Nullable GuiEventListener listener) {
        if (listener instanceof ContainerEventHandler container) {
            GuiEventListener focused = container.getFocused();
            if (focused != null) {
                return focusedLeaf(focused);
            }
        }

        return listener;
    }

    private boolean keyPressedArrow(KeyEvent event, ScreenDirection direction) {
        GuiEventListener focused = this.getFocused();
        if (focused != null && focused.keyPressed(event)) {
            this.clearArrowNavigationMemory();
            this.rememberCurrentOptionFocus();
            return true;
        }

        ComponentPath currentFocusPath = this.getCurrentFocusPath();
        if (this.restorePreviousArrowFocus(direction, currentFocusPath)) {
            this.rememberCurrentOptionFocus();
            return true;
        }

        ComponentPath nextFocusPath = this.nextFocusPath(new FocusNavigationEvent.ArrowNavigation(direction));
        if (nextFocusPath == null) {
            this.clearArrowNavigationMemory();
            return false;
        }

        this.changeFocus(nextFocusPath);
        this.rememberArrowNavigation(direction, currentFocusPath, nextFocusPath);
        this.rememberCurrentOptionFocus();

        return true;
    }

    private boolean restorePreviousArrowFocus(ScreenDirection direction, @Nullable ComponentPath currentFocusPath) {
        if (this.previousArrowFocusPath == null
                || this.lastArrowDirection == null
                || currentFocusPath == null
                || direction != this.lastArrowDirection.getOpposite()
                || currentFocusPath.leafComponent() != this.currentArrowFocusLeaf
                || !this.containsFocusLeaf(this.previousArrowFocusPath.leafComponent())) {
            return false;
        }

        ComponentPath previousPath = this.previousArrowFocusPath;
        this.changeFocus(previousPath);
        this.previousArrowFocusPath = currentFocusPath;
        this.currentArrowFocusLeaf = previousPath.leafComponent();
        this.lastArrowDirection = direction;

        return true;
    }

    private void rememberArrowNavigation(ScreenDirection direction, @Nullable ComponentPath previousPath, ComponentPath currentPath) {
        if (previousPath == null || previousPath.leafComponent() == currentPath.leafComponent()) {
            this.clearArrowNavigationMemory();
            return;
        }

        this.previousArrowFocusPath = previousPath;
        this.currentArrowFocusLeaf = currentPath.leafComponent();
        this.lastArrowDirection = direction;
    }

    private void clearArrowNavigationMemory() {
        this.previousArrowFocusPath = null;
        this.currentArrowFocusLeaf = null;
        this.lastArrowDirection = null;
    }

    private boolean containsFocusLeaf(GuiEventListener leaf) {
        for (GuiEventListener child : this.children()) {
            if (containsFocusLeaf(child, leaf)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsFocusLeaf(GuiEventListener component, GuiEventListener leaf) {
        if (component == leaf) {
            return true;
        }

        if (component instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                if (containsFocusLeaf(child, leaf)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static @Nullable ScreenDirection getArrowDirection(KeyEvent event) {
        return switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT -> ScreenDirection.LEFT;
            case GLFW.GLFW_KEY_RIGHT -> ScreenDirection.RIGHT;
            case GLFW.GLFW_KEY_UP -> ScreenDirection.UP;
            case GLFW.GLFW_KEY_DOWN -> ScreenDirection.DOWN;
            default -> null;
        };
    }

    private static int keyForDirection(ScreenDirection direction) {
        return switch (direction) {
            case LEFT -> GLFW.GLFW_KEY_LEFT;
            case RIGHT -> GLFW.GLFW_KEY_RIGHT;
            case UP -> GLFW.GLFW_KEY_UP;
            case DOWN -> GLFW.GLFW_KEY_DOWN;
        };
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    @Override
    public void onClose() {
        this.uiState.lastSearch().set("");
        this.uiState.lastSearchIndex().set(0);
        this.uiState.updateSearchResults(List.of());
        this.uiState.clearOptionUiStates();
        this.uiState.focusedOptionIdsByTab().clear();
        this.minecraft.gui.setScreen(this.prevScreen);
    }

    @Override
    public @Nullable ScreenPrompt getPrompt() {
        return this.prompt;
    }

    @Override
    public void setPrompt(@Nullable ScreenPrompt prompt) {
        this.prompt = prompt;
    }

    @Override
    public Dim2i getDimensions() {
        return new Dim2i(0, 0, this.width, this.height);
    }
}
