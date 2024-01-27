package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.SearchTextFieldComponent;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import me.flashyreese.mods.reeses_sodium_options.compat.IrisCompat;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.data.fingerprint.HashedFingerprint;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.jellysquid.mods.sodium.client.gui.prompt.ScreenPrompt;
import me.jellysquid.mods.sodium.client.gui.prompt.ScreenPromptable;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class SodiumVideoOptionsScreen extends Screen implements ScreenPromptable {

    private static final AtomicReference<Text> tabFrameSelectedTab = new AtomicReference<>(null);
    private static final AtomicReference<Integer> tabFrameScrollBarOffset = new AtomicReference<>(0);
    private static final AtomicReference<Integer> optionPageScrollBarOffset = new AtomicReference<>(0);

    private static final AtomicReference<String> lastSearch = new AtomicReference<>("");
    private static final AtomicReference<Integer> lastSearchIndex = new AtomicReference<>(0);

    private final Screen prevScreen;
    private final List<OptionPage> pages = new ArrayList<>();
    private AbstractFrame frame;
    private FlatButtonWidget applyButton, closeButton, undoButton;
    private FlatButtonWidget donateButton, hideDonateButton;
    private boolean hasPendingChanges;

    private SearchTextFieldComponent searchTextField;

    private @Nullable ScreenPrompt prompt;

    public SodiumVideoOptionsScreen(Screen prev, List<OptionPage> pages) {
        super(Text.literal("Reese's Sodium Menu"));
        this.prevScreen = prev;
        this.pages.addAll(pages);

        this.checkPromptTimers();
    }

    private void checkPromptTimers() {
        // Never show the prompt in developer workspaces.
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }

        var options = SodiumClientMod.options();

        // If the user has disabled the nags forcefully (by config), or has already seen the prompt, don't show it again.
        if (options.notifications.forceDisableDonationPrompts || options.notifications.hasSeenDonationPrompt) {
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

    private void openDonationPrompt(SodiumGameOptions options) {
        var prompt = new ScreenPrompt(this, DONATION_PROMPT_MESSAGE, 320, 190,
                new ScreenPrompt.Action(Text.literal("Buy us a coffee"), this::openDonationPage));
        prompt.setFocused(true);

        options.notifications.hasSeenDonationPrompt = true;

        try {
            SodiumGameOptions.writeToDisk(options);
        } catch (IOException e) {
            SodiumClientMod.logger()
                    .error("Failed to update config file", e);
        }
    }

    // Hackalicious! Rebuild UI
    public void rebuildUI() {
        this.clearAndInit();
    }

    @Override
    protected void init() {
        this.frame = this.parentFrameBuilder().build();
        this.addDrawableChild(this.frame);

        this.searchTextField.setFocused(!lastSearch.get().trim().isEmpty());
        if (this.searchTextField.isFocused()) {
            this.setFocused(this.searchTextField);
        } else {
            this.setFocused(this.frame);
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

        Text donationText = Text.translatable("sodium.options.buttons.donate");
        int donationTextWidth = this.client.textRenderer.getWidth(donationText);

        Dim2i donateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 32 - donationTextWidth, tabFrameDim.y() - 26, 10 + donationTextWidth, 20);
        Dim2i hideDonateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 20, tabFrameDim.y() - 26, 20, 20);

        this.undoButton = new FlatButtonWidget(undoButtonDim, Text.translatable("sodium.options.buttons.undo"), this::undoChanges);
        this.applyButton = new FlatButtonWidget(applyButtonDim, Text.translatable("sodium.options.buttons.apply"), this::applyChanges);
        this.closeButton = new FlatButtonWidget(closeButtonDim, Text.translatable("gui.done"), this::close);

        this.donateButton = new FlatButtonWidget(donateButtonDim, donationText, this::openDonationPage);
        this.hideDonateButton = new FlatButtonWidget(hideDonateButtonDim, Text.literal("x"), this::hideDonationButton);

        if (SodiumClientMod.options().notifications.hasClearedDonationButton || SodiumClientMod.options().notifications.forceDisableDonationPrompts) {
            this.setDonationButtonVisibility(false);
        }


        Dim2i searchTextFieldDim;
        if (SodiumClientMod.options().notifications.hasClearedDonationButton || SodiumClientMod.options().notifications.forceDisableDonationPrompts) {
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width(), 20);
        } else {
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width() - (tabFrameDim.getLimitX() - donateButtonDim.x()) - 2, 20);
        }


        basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);

        if (IrisCompat.isIrisPresent()) { // FabricLoader.getInstance().isModLoaded("iris")) {
            //int size = this.client.textRenderer.getWidth(Text.translatable(IrisApi.getInstance().getMainScreenLanguageKey()));
            int size = this.client.textRenderer.getWidth(Text.translatable(IrisCompat.getIrisShaderPacksScreenLanguageKey()));
            Dim2i shaderPackButtonDim;
            if (!(SodiumClientMod.options().notifications.hasClearedDonationButton || SodiumClientMod.options().notifications.forceDisableDonationPrompts)) {
                shaderPackButtonDim = new Dim2i(donateButtonDim.x() - 12 - size, tabFrameDim.y() - 26, 10 + size, 20);
            } else {
                shaderPackButtonDim = new Dim2i(tabFrameDim.getLimitX() - size - 10, tabFrameDim.y() - 26, 10 + size, 20);
            }
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width() - (tabFrameDim.getLimitX() - shaderPackButtonDim.x()) - 2, 20);

            //FlatButtonWidget shaderPackButton = new FlatButtonWidget(shaderPackButtonDim, Text.translatable(IrisApi.getInstance().getMainScreenLanguageKey()), () -> this.client.setScreen((Screen) IrisApi.getInstance().openMainIrisScreenObj(this)));
            FlatButtonWidget shaderPackButton = new FlatButtonWidget(shaderPackButtonDim, Text.translatable(IrisCompat.getIrisShaderPacksScreenLanguageKey()), () -> this.client.setScreen(IrisCompat.getIrisShaderPacksScreen(this)));
            basicFrameBuilder.addChild(dim -> shaderPackButton);
        }

        this.searchTextField = new SearchTextFieldComponent(searchTextFieldDim, this.pages, tabFrameSelectedTab,
                tabFrameScrollBarOffset, optionPageScrollBarOffset, tabFrameDim.height(), this, lastSearch, lastSearchIndex);

        basicFrameBuilder.addChild(dim -> this.searchTextField);

        return basicFrameBuilder;
    }

    public BasicFrame.Builder parentBasicFrameBuilder(Dim2i parentBasicFrameDim, Dim2i tabFrameDim) {
        return BasicFrame.createBuilder()
                .setDimension(parentBasicFrameDim)
                .shouldRenderOutline(false)
                .addChild(dim -> this.donateButton)
                .addChild(dim -> this.hideDonateButton)
                .addChild(parentDim -> TabFrame.createBuilder()
                        .setDimension(tabFrameDim)
                        .shouldRenderOutline(false)
                        .setTabSectionScrollBarOffset(tabFrameScrollBarOffset)
                        .setTabSectionSelectedTab(tabFrameSelectedTab)
                        .addTabs(tabs -> this.pages
                                .stream()
                                .filter(page -> !page.getGroups().isEmpty())
                                .forEach(page -> tabs.add(Tab.createBuilder().from(page, optionPageScrollBarOffset)))
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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.renderBackground(drawContext);
        this.updateControls();
        this.frame.render(drawContext, this.prompt != null ? -1 : mouseX, this.prompt != null ? -1 : mouseY, delta);
        if (this.prompt != null) {
            this.prompt.render(drawContext, mouseX, mouseY, delta);
        }
    }

    private void updateControls() {
        boolean hasChanges = this.getAllOptions()
                .anyMatch(Option::hasChanged);

        for (OptionPage page : this.pages) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    hasChanges = true;
                }
            }
        }

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
        SodiumGameOptions options = SodiumClientMod.options();
        options.notifications.hasClearedDonationButton = true;

        try {
            SodiumGameOptions.writeToDisk(options);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration", e);
        }

        this.setDonationButtonVisibility(false);


        this.rebuildUI();
    }

    private void openDonationPage() {
        Util.getOperatingSystem()
                .open("https://caffeinemc.net/donate");
    }

    private Stream<Option<?>> getAllOptions() {
        return this.pages.stream()
                .flatMap(s -> s.getOptions().stream());
    }

    private void applyChanges() {
        final HashSet<OptionStorage<?>> dirtyStorages = new HashSet<>();
        final EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

        this.getAllOptions().forEach((option -> {
            if (!option.hasChanged()) {
                return;
            }

            option.applyChanges();

            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
        }));

        MinecraftClient client = MinecraftClient.getInstance();

        if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
            client.worldRenderer.reload();
        }

        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
            client.setMipmapLevels(client.options.getMipmapLevels().getValue());
            client.reloadResourcesConcurrently();
        }

        for (OptionStorage<?> storage : dirtyStorages) {
            storage.save();
        }
    }

    private void undoChanges() {
        this.getAllOptions()
                .forEach(Option::reset);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.prompt != null) {
            return this.prompt.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.prompt != null) {
            return this.prompt.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_P && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 && !(this.searchTextField != null && this.searchTextField.isFocused())) {
            MinecraftClient.getInstance().setScreen(new VideoOptionsScreen(this.prevScreen, MinecraftClient.getInstance().options));

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    @Override
    public void close() {
        lastSearch.set("");
        lastSearchIndex.set(0);
        this.client.setScreen(this.prevScreen);
    }

    @Override
    public void setPrompt(@Nullable ScreenPrompt prompt) {
        this.prompt = prompt;
    }

    @Override
    public @Nullable ScreenPrompt getPrompt() {
        return this.prompt;
    }

    @Override
    public Dim2i getDimensions() {
        return new Dim2i(0, 0, this.width, this.height);
    }


    private static final List<StringVisitable> DONATION_PROMPT_MESSAGE;

    static {
        DONATION_PROMPT_MESSAGE = List.of(
                StringVisitable.concat(Text.literal("Hello!")),
                StringVisitable.concat(Text.literal("It seems that you've been enjoying "), Text.literal("Sodium").setStyle(Style.EMPTY.withColor(0x27eb92)), Text.literal(", the free and open-source optimization mod for Minecraft.")),
                StringVisitable.concat(Text.literal("Mods like these are complex. They require "), Text.literal("thousands of hours").setStyle(Style.EMPTY.withColor(0xff6e00)), Text.literal(" of development, debugging, and tuning to create the experience that players have come to expect.")),
                StringVisitable.concat(Text.literal("If you'd like to show your token of appreciation, and support the development of our mod in the process, then consider "), Text.literal("buying us a coffee").setStyle(Style.EMPTY.withColor(0xed49ce)), Text.literal(".")),
                StringVisitable.concat(Text.literal("And thanks again for using our mod! We hope it helps you (and your computer.)"))
        );
    }
}
