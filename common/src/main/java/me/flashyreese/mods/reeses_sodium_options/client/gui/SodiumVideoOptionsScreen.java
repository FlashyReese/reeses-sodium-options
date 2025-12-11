package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.SearchTextFieldComponent;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.data.fingerprint.HashedFingerprint;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPrompt;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPromptable;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SodiumVideoOptionsScreen extends Screen implements ScreenPromptable {

    private static final AtomicReference<Component> tabFrameSelectedTab = new AtomicReference<>(null);
    private static final AtomicReference<Integer> tabFrameScrollBarOffset = new AtomicReference<>(0);
    private static final AtomicReference<Integer> optionPageScrollBarOffset = new AtomicReference<>(0);

    private static final AtomicReference<String> lastSearch = new AtomicReference<>("");
    private static final AtomicReference<Integer> lastSearchIndex = new AtomicReference<>(0);
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
    private final List<OptionPage> pages = new ArrayList<>();
    private FlatButtonWidget applyButton, closeButton, undoButton;
    private FlatButtonWidget donateButton, hideDonateButton;
    private boolean hasPendingChanges;
    private SearchTextFieldComponent searchTextField;
    private @Nullable ScreenPrompt prompt;

    public SodiumVideoOptionsScreen(Screen prev) {
        super(Component.literal("Reese's Sodium Menu"));
        this.prevScreen = prev;

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
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();

        ConfigManager.CONFIG.invalidateGlobalRebuildDependents();

        AbstractFrame frame = this.parentFrameBuilder().build();
        this.addRenderableWidget(frame);

        this.searchTextField.setFocused(!lastSearch.get().trim().isEmpty());
        if (this.searchTextField.isFocused()) {
            this.setFocused(this.searchTextField);
        } else {
            this.setFocused(frame);
        }

        if (this.prompt != null) {
            this.prompt.init();
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

        /*
        // No longer necessary with API
        if (IrisCompat.isIrisPresent()) { // FabricLoader.getInstance().isModLoaded("iris")) {
            //int size = this.client.textRenderer.getWidth(Text.translatable(IrisApi.getInstance().getMainScreenLanguageKey()));
            int size = this.minecraft.font.width(Component.translatable(Objects.requireNonNull(IrisCompat.getIrisShaderPacksScreenLanguageKey())));
            Dim2i shaderPackButtonDim;
            if (!(SodiumClientMod.options().notifications.hasClearedDonationButton)) {
                shaderPackButtonDim = new Dim2i(donateButtonDim.x() - 12 - size, tabFrameDim.y() - 26, 10 + size, 20);
            } else {
                shaderPackButtonDim = new Dim2i(tabFrameDim.getLimitX() - size - 10, tabFrameDim.y() - 26, 10 + size, 20);
            }
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width() - (tabFrameDim.getLimitX() - shaderPackButtonDim.x()) - 2, 20);

            //FlatButtonWidget shaderPackButton = new FlatButtonWidget(shaderPackButtonDim, Text.translatable(IrisApi.getInstance().getMainScreenLanguageKey()), () -> this.client.setScreen((Screen) IrisApi.getInstance().openMainIrisScreenObj(this)));
            FlatButtonWidget shaderPackButton = new FlatButtonWidget(shaderPackButtonDim, Component.translatable(IrisCompat.getIrisShaderPacksScreenLanguageKey()), () -> this.minecraft.setScreen(IrisCompat.getIrisShaderPacksScreen(this)));
            basicFrameBuilder.addChild(dim -> shaderPackButton);
        }*/

        this.searchTextField = new SearchTextFieldComponent(searchTextFieldDim, ConfigManager.CONFIG.getModOptions().stream().flatMap(modOptions -> modOptions.pages().stream()).toList(), tabFrameSelectedTab,
                tabFrameScrollBarOffset, optionPageScrollBarOffset, tabFrameDim.height(), this, lastSearch, lastSearchIndex);

        basicFrameBuilder.addChild(dim -> this.searchTextField);

        return basicFrameBuilder;
    }

    public BasicFrame.Builder parentBasicFrameBuilder(Dim2i parentBasicFrameDim, Dim2i tabFrameDim) {
        return BasicFrame.builder()
                .withDimension(parentBasicFrameDim)
                .withRenderOutline(false)
                .withScreen(this)
                .addChild(parentDim -> TabFrame.createBuilder()
                        .setDimension(tabFrameDim)
                        .shouldRenderOutline(false)
                        .setTabSectionScrollBarOffset(tabFrameScrollBarOffset)
                        .setTabSectionSelectedTab(tabFrameSelectedTab)
                        .addTabs(tabs -> ConfigManager.CONFIG
                                .getModOptions()
                                .forEach(config -> config.pages().stream()
                                        .filter(page -> !page.groups().isEmpty())
                                        .forEach(page -> tabs.add(Tab.builder().from(this, config, page, optionPageScrollBarOffset))))
                        )
                        .onSetTab(() -> {
                            optionPageScrollBarOffset.set(0);
                        })
                        .build()
                )
                .addChild(dim -> this.undoButton)
                .addChild(dim -> this.applyButton)
                .addChild(dim -> this.closeButton);
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
    public boolean mouseClicked(MouseButtonEvent event, boolean repeated) {
        if (this.prompt != null) {
            return this.prompt.mouseClicked(event, repeated);
        }

        return super.mouseClicked(event, repeated);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.prompt != null) {
            return this.prompt.keyPressed(event);
        }

        if (event.key() == GLFW.GLFW_KEY_P && (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 && !(this.searchTextField != null && this.searchTextField.isFocused())) {
            Minecraft.getInstance().setScreen(new VideoSettingsScreen(this.prevScreen, Minecraft.getInstance(), Minecraft.getInstance().options));

            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    @Override
    public void onClose() {
        lastSearch.set("");
        lastSearchIndex.set(0);
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
}
