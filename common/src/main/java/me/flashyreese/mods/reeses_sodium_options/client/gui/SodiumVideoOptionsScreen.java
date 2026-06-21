package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonControl;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.OptionUndoButtonRenderer;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.SearchTextFieldComponent;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.data.fingerprint.HashedFingerprint;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPrompt;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPromptable;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class SodiumVideoOptionsScreen extends Screen implements ScreenPromptable {

    private static final UiState SHARED_UI_STATE = new UiState();
    private static final String RSO_CONFIG_ID = "reeses-sodium-options";
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
    private final UiState uiState;
    private final List<OptionPage> pages = new ArrayList<>();
    private FlatButtonWidget applyButton, closeButton, undoButton;
    private FlatButtonWidget donateButton, hideDonateButton;
    private boolean hasPendingChanges;
    private SearchTextFieldComponent searchTextField;
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

    @Override
    protected void init() {
        super.init();

        ConfigManager.CONFIG.invalidateGlobalRebuildDependents();

        this.rootFrame = this.parentFrameBuilder().build();
        this.addRenderableWidget(this.rootFrame);

        //this.searchTextField.setFocused(!this.uiState.lastSearch().get().trim().isEmpty());
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

    protected BasicFrame.Builder parentFrameBuilder() {
        BasicFrame.Builder basicFrameBuilder;

        // Calculates if resolution exceeds 16:9 ratio, force 16:9
        int newWidth = this.width;
        if ((float) this.width / (float) this.height > 1.77777777778) {
            newWidth = (int) (this.height * 1.77777777778);
        }

        Dim2i basicFrameDim = new Dim2i((this.width - newWidth) / 2, 0, newWidth, this.height);
        Dim2i tabFrameDim = new Dim2i(basicFrameDim.x() + basicFrameDim.width() / 20 / 2, basicFrameDim.y() + basicFrameDim.height() / 4 / 2, basicFrameDim.width() - (basicFrameDim.width() / 20), basicFrameDim.height() / 4 * 3);

        Dim2i undoButtonDim = new Dim2i(tabFrameDim.getLimitX() - 203, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i applyButtonDim = new Dim2i(tabFrameDim.getLimitX() - 134, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i closeButtonDim = new Dim2i(tabFrameDim.getLimitX() - 65, tabFrameDim.getLimitY() + 5, 65, 20);

        Component donationText = Component.translatable("sodium.options.buttons.donate");
        int donationTextWidth = this.minecraft.font.width(donationText);

        Dim2i donateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 32 - donationTextWidth, tabFrameDim.y() - 26, 10 + donationTextWidth, 20);
        Dim2i hideDonateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 20, tabFrameDim.y() - 26, 20, 20);

        this.undoButton = new FlatButtonWidget(undoButtonDim, Component.translatable("sodium.options.buttons.undo"), ConfigManager.CONFIG::resetAllOptionsFromBindings, true, false);
        this.applyButton = new FlatButtonWidget(applyButtonDim, Component.translatable("sodium.options.buttons.apply"), ConfigManager.CONFIG::applyAllOptions, true, false);
        this.closeButton = new FlatButtonWidget(closeButtonDim, Component.translatable("gui.done"), this::onClose, true, false);

        this.donateButton = new FlatButtonWidget(donateButtonDim, donationText, this::openDonationPage, true, false);
        this.hideDonateButton = new FlatButtonWidget(hideDonateButtonDim, Component.literal("x"), this::hideDonationButton, true, false);

        if (SodiumClientMod.options().notifications.hasClearedDonationButton) {
            this.setDonationButtonVisibility(false);
        }


        basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);


        Dim2i searchTextFieldDim;
        if (SodiumClientMod.options().notifications.hasClearedDonationButton) {
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width(), 20);
        } else {
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width() - (tabFrameDim.getLimitX() - donateButtonDim.x()) - 2, 20);

            basicFrameBuilder
                    .addChild(dim -> this.donateButton)
                    .addChild(dim -> this.hideDonateButton);
        }


        this.searchTextField = new SearchTextFieldComponent(searchTextFieldDim, getOrderedModOptions().stream().flatMap(modOptions -> modOptions.pages().stream()).toList(), this.uiState,
                tabFrameDim.height(), this);

        basicFrameBuilder.addChild(dim -> this.searchTextField);

        return basicFrameBuilder;
    }

    public BasicFrame.Builder parentBasicFrameBuilder(Dim2i parentBasicFrameDim, Dim2i tabFrameDim) {
        return BasicFrame.builder()
                .withDimension(parentBasicFrameDim)
                .withRenderOutline(false)
                .withScreen(this)
                .addChild(parentDim -> {
                    this.tabFrame = TabFrame.createBuilder()
                            .setDimension(tabFrameDim)
                            .withScreen(this)
                            .shouldRenderOutline(false)
                            .setTabSectionScrollBarOffset(this.uiState.tabFrameScrollBarOffset())
                            .setTabSectionSelectedTab(this.uiState.tabFrameSelectedTab())
                            .setTabSectionSelectedGroup(this.uiState.tabFrameSelectedGroup())
                            .setManuallyCollapsedTabGroups(this.uiState.manuallyCollapsedTabGroups())
                            .addTabs(tabs -> getOrderedModOptions()
                                    .forEach(config -> config.pages()
                                            .forEach(page -> tabs.add(Tab.builder().from(this, config, page, this.uiState.optionPageScrollBarOffset()))))
                            )
                            .onSetTab(() -> {
                                this.uiState.optionPageScrollBarOffset().set(0);
                            })
                            .build();
                    return this.tabFrame;
                })
                .addChild(dim -> this.undoButton)
                .addChild(dim -> this.applyButton)
                .addChild(dim -> this.closeButton);
    }

    private static List<ModOptions> getOrderedModOptions() {
        return ConfigManager.CONFIG.getModOptions().stream()
                .sorted((left, right) -> Boolean.compare(isOwnConfig(left), isOwnConfig(right)))
                .toList();
    }

    private static boolean isOwnConfig(ModOptions modOptions) {
        return RSO_CONFIG_ID.equals(modOptions.configId());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.updateControls();
        super.render(guiGraphics, this.prompt != null ? -1 : mouseX, this.prompt != null ? -1 : mouseY, delta);
        if (this.prompt != null) {
            this.prompt.render(guiGraphics, mouseX, mouseY, delta);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.clearArrowNavigationMemory();
        String previousTabKey = this.getSelectedTabKey();

        if (this.prompt != null) {
            return this.prompt.mouseClicked(mouseX, mouseY, button);
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        this.afterInput(previousTabKey);

        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);

        if (button == 0 && this.rootFrame != null) {
            this.rootFrame.releaseUndoButtonLayoutHolds();
        }

        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.prompt != null) {
            return this.prompt.keyPressed(keyCode, scanCode, modifiers);
        }

        String previousTabKey = this.getSelectedTabKey();

        if (this.isSearchShortcut(keyCode)) {
            this.focusSearchTextField();
            this.searchTextField.selectAllText();
            this.clearArrowNavigationMemory();

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.clearSearchText()) {
            this.clearArrowNavigationMemory();

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_P && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 && !(this.searchTextField != null && this.searchTextField.isFocused())) {
            this.minecraft.setScreen(new VideoSettingsScreen(this.prevScreen, this.minecraft, this.minecraft.options));

            return true;
        }

        if (!this.isSearchTextFieldFocused()) {
            if (this.isUndoShortcut(keyCode) && this.undoFocusedOption()) {
                this.clearArrowNavigationMemory();
                this.afterInput(previousTabKey);

                return true;
            }

            if (this.keyPressedOptionListNavigation(keyCode)) {
                this.clearArrowNavigationMemory();
                this.afterInput(previousTabKey);

                return true;
            }

            if (this.isApplyShortcut(keyCode)) {
                GuiEventListener focused = this.getFocused();
                if (focused != null && focused.keyPressed(keyCode, scanCode, modifiers)) {
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

        ScreenDirection arrowDirection = getArrowDirection(keyCode);
        if (arrowDirection != null) {
            return this.keyPressedArrow(keyCode, scanCode, modifiers, arrowDirection);
        }

        this.clearArrowNavigationMemory();

        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (handled) {
            this.afterInput(previousTabKey);
        }

        return handled;
    }

    private boolean isSearchShortcut(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_F && Screen.hasControlDown();
    }

    private boolean isUndoShortcut(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_Z && Screen.hasControlDown();
    }

    private boolean isApplyShortcut(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
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

    private boolean keyPressedOptionListNavigation(int keyCode) {
        if (this.tabFrame == null || this.rootFrame == null) {
            return false;
        }

        ControlElement target;
        boolean handled = true;

        switch (keyCode) {
            case GLFW.GLFW_KEY_HOME -> {
                this.tabFrame.scrollSelectedPageToStart();
                target = this.tabFrame.findFirstSelectedControl();
            }
            case GLFW.GLFW_KEY_END -> {
                this.tabFrame.scrollSelectedPageToEnd();
                target = this.tabFrame.findLastSelectedControl();
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                handled = this.tabFrame.scrollSelectedPage(-1);
                target = this.tabFrame.findFirstVisibleSelectedControl();
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                handled = this.tabFrame.scrollSelectedPage(1);
                target = this.tabFrame.findLastVisibleSelectedControl();
            }
            default -> {
                return false;
            }
        }

        return this.focusControlElement(target) || handled;
    }

    private boolean undoFocusedOption() {
        ControlElement focusedControl = this.getFocusedControlElement();
        if (focusedControl == null || !(focusedControl.getOption() instanceof StatefulOption<?> option)) {
            return false;
        }

        if (focusedControl instanceof OptionUndoButtonControl undoButtonControl
                && undoButtonControl.rso$getUndoButtonElement().undo()) {
            this.updateControls();
            return true;
        }

        if (!OptionUndoButtonRenderer.canUndo(option)) {
            return false;
        }

        OptionUndoButtonRenderer.undoChanges(option);
        if (focusedControl instanceof OptionUndoButtonControl undoButtonControl) {
            undoButtonControl.rso$clearUndoButtonFocus();
        }
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

        ResourceLocation optionId = this.uiState.focusedOptionIdsByTab().get(tabKey);

        return optionId != null && this.focusControlElement(this.tabFrame.findSelectedControl(optionId));
    }

    private boolean focusControlElement(@Nullable ControlElement controlElement) {
        if (controlElement == null || this.rootFrame == null || !this.rootFrame.focusControlElement(controlElement)) {
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
        ControlElement focusedControl = this.getFocusedControlElement();
        String tabKey = this.getSelectedTabKey();

        if (tabKey != null && focusedControl != null && focusedControl.getOption() instanceof OptionExtended optionExtended) {
            this.uiState.focusedOptionIdsByTab().put(tabKey, optionExtended.getId());
        }
    }

    private @Nullable ControlElement getFocusedControlElement() {
        return this.rootFrame == null ? null : findFocusedControlElement(this.rootFrame);
    }

    private static @Nullable ControlElement findFocusedControlElement(GuiEventListener listener) {
        if (listener instanceof ControlElement controlElement && controlElement.isFocused()) {
            return controlElement;
        }

        if (listener instanceof ContainerEventHandler container) {
            GuiEventListener focused = container.getFocused();
            if (focused != null) {
                return findFocusedControlElement(focused);
            }
        }

        return null;
    }

    private @Nullable String getSelectedTabKey() {
        return this.tabFrame == null ? null : this.tabFrame.getSelectedTabKey().orElse(null);
    }

    private boolean keyPressedArrow(int keyCode, int scanCode, int modifiers, ScreenDirection direction) {
        GuiEventListener focused = this.getFocused();
        if (focused != null && focused.keyPressed(keyCode, scanCode, modifiers)) {
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
                || leafComponent(currentFocusPath) != this.currentArrowFocusLeaf
                || !this.containsFocusLeaf(leafComponent(this.previousArrowFocusPath))) {
            return false;
        }

        ComponentPath previousPath = this.previousArrowFocusPath;
        this.changeFocus(previousPath);
        this.previousArrowFocusPath = currentFocusPath;
        this.currentArrowFocusLeaf = leafComponent(previousPath);
        this.lastArrowDirection = direction;

        return true;
    }

    private void rememberArrowNavigation(ScreenDirection direction, @Nullable ComponentPath previousPath, ComponentPath currentPath) {
        if (previousPath == null || leafComponent(previousPath) == leafComponent(currentPath)) {
            this.clearArrowNavigationMemory();
            return;
        }

        this.previousArrowFocusPath = previousPath;
        this.currentArrowFocusLeaf = leafComponent(currentPath);
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

    private static GuiEventListener leafComponent(ComponentPath path) {
        return path instanceof ComponentPath.Path containerPath ? leafComponent(containerPath.childPath()) : path.component();
    }

    private static @Nullable ScreenDirection getArrowDirection(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> ScreenDirection.LEFT;
            case GLFW.GLFW_KEY_RIGHT -> ScreenDirection.RIGHT;
            case GLFW.GLFW_KEY_UP -> ScreenDirection.UP;
            case GLFW.GLFW_KEY_DOWN -> ScreenDirection.DOWN;
            default -> null;
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
        this.uiState.focusedOptionIdsByTab().clear();
        this.minecraft.setScreen(this.prevScreen);
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

    public static UiState sharedUiState() {
        return SHARED_UI_STATE;
    }

    public static final class UiState {
        private final AtomicReference<Component> tabFrameSelectedTab = new AtomicReference<>(null);
        private final AtomicReference<String> tabFrameSelectedGroup = new AtomicReference<>(null);
        private final AtomicReference<Integer> tabFrameScrollBarOffset = new AtomicReference<>(0);
        private final AtomicReference<Integer> optionPageScrollBarOffset = new AtomicReference<>(0);
        private final AtomicReference<String> lastSearch = new AtomicReference<>("");
        private final AtomicReference<Integer> lastSearchIndex = new AtomicReference<>(0);
        private final List<ResourceLocation> searchResultIds = new ArrayList<>();
        private final Set<String> manuallyCollapsedTabGroups = new HashSet<>();
        private final Map<String, ResourceLocation> focusedOptionIdsByTab = new HashMap<>();

        public AtomicReference<Component> tabFrameSelectedTab() {
            return tabFrameSelectedTab;
        }

        public AtomicReference<String> tabFrameSelectedGroup() {
            return tabFrameSelectedGroup;
        }

        public AtomicReference<Integer> tabFrameScrollBarOffset() {
            return tabFrameScrollBarOffset;
        }

        public AtomicReference<Integer> optionPageScrollBarOffset() {
            return optionPageScrollBarOffset;
        }

        public Set<String> manuallyCollapsedTabGroups() {
            return manuallyCollapsedTabGroups;
        }

        public Map<String, ResourceLocation> focusedOptionIdsByTab() {
            return focusedOptionIdsByTab;
        }

        public AtomicReference<String> lastSearch() {
            return lastSearch;
        }

        public AtomicReference<Integer> lastSearchIndex() {
            return lastSearchIndex;
        }

        public List<ResourceLocation> searchResultIds() {
            return List.copyOf(searchResultIds);
        }

        public boolean updateSearchResults(List<ResourceLocation> ids) {
            if (this.searchResultIds.equals(ids)) {
                return false;
            }
            this.searchResultIds.clear();
            this.searchResultIds.addAll(ids);
            return true;
        }
    }
}
