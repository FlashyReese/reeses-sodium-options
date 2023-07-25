package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.SearchTextFieldComponent;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import me.flashyreese.mods.reeses_sodium_options.compat.IrisCompat;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class SodiumVideoOptionsScreen extends Screen {

    private static final AtomicReference<String> tabFrameSelectedTab = new AtomicReference<>(null);
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

    public SodiumVideoOptionsScreen(Screen prev, List<OptionPage> pages) {
        super(new LiteralText("Reese's Sodium Menu"));
        this.prevScreen = prev;
        this.pages.addAll(pages);
    }

    // Hackalicious! Rebuild UI
    public void rebuildUI() {
        this.children.clear();
        this.init();
    }

    @Override
    protected void init() {
        this.frame = this.parentFrameBuilder().build();
        this.children.add(this.frame);

        this.searchTextField.setFocused(!lastSearch.get().trim().isEmpty());
        if (this.searchTextField.isFocused()) {
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
        Dim2i tabFrameDim = new Dim2i(basicFrameDim.getOriginX() + basicFrameDim.getWidth() / 20 / 2, basicFrameDim.getOriginY() + basicFrameDim.getHeight() / 4 / 2, basicFrameDim.getWidth() - (basicFrameDim.getWidth() / 20), basicFrameDim.getHeight() / 4 * 3);

        Dim2i undoButtonDim = new Dim2i(tabFrameDim.getLimitX() - 203, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i applyButtonDim = new Dim2i(tabFrameDim.getLimitX() - 134, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i closeButtonDim = new Dim2i(tabFrameDim.getLimitX() - 65, tabFrameDim.getLimitY() + 5, 65, 20);

        String donationText = "Buy us a coffee!";
        int donationTextWidth = this.client.textRenderer.getWidth(donationText);

        Dim2i donateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 32 - donationTextWidth, tabFrameDim.getOriginY() - 26, 10 + donationTextWidth, 20);
        Dim2i hideDonateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 20, tabFrameDim.getOriginY() - 26, 20, 20);

        this.undoButton = new FlatButtonWidget(undoButtonDim, "Undo", this::undoChanges);
        this.applyButton = new FlatButtonWidget(applyButtonDim, "Apply", this::applyChanges);
        this.closeButton = new FlatButtonWidget(closeButtonDim, "Close", this::onClose);

        this.donateButton = new FlatButtonWidget(donateButtonDim, donationText, this::openDonationPage);
        this.hideDonateButton = new FlatButtonWidget(hideDonateButtonDim, "x", this::hideDonationButton);

        if (SodiumClientMod.options().notifications.hideDonationButton) {
            this.setDonationButtonVisibility(false);
        }


        Dim2i searchTextFieldDim;
        if (SodiumClientMod.options().notifications.hideDonationButton) {
            searchTextFieldDim = new Dim2i(tabFrameDim.getOriginX(), tabFrameDim.getOriginY() - 26, tabFrameDim.getWidth(), 20);
        } else {
            searchTextFieldDim = new Dim2i(tabFrameDim.getOriginX(), tabFrameDim.getOriginY() - 26, donateButtonDim.getOriginX() - 12, 20);
        }


        basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);

        if (IrisCompat.isIrisPresent()) { // FabricLoader.getInstance().isModLoaded("iris")) {
            //int size = this.client.textRenderer.getWidth(new TranslatableText(IrisApi.getInstance().getMainScreenLanguageKey()));
            int size = this.client.textRenderer.getWidth(new TranslatableText(IrisCompat.getIrisShaderPacksScreenLanguageKey()));
            Dim2i shaderPackButtonDim;
            if (!SodiumClientMod.options().notifications.hideDonationButton) {
                shaderPackButtonDim = new Dim2i(donateButtonDim.getOriginX() - 12 - size, tabFrameDim.getOriginY() - 26, 10 + size, 20);
                searchTextFieldDim = new Dim2i(tabFrameDim.getOriginX(), tabFrameDim.getOriginY() - 26, donateButtonDim.getOriginX() - 12 - size - 12, 20);
            } else {
                shaderPackButtonDim = new Dim2i(tabFrameDim.getLimitX() - size - 10, tabFrameDim.getOriginY() - 26, 10 + size, 20);
                searchTextFieldDim = new Dim2i(tabFrameDim.getOriginX(), tabFrameDim.getOriginY() - 26, tabFrameDim.getLimitX() - size - 10 - 12, 20);
            }

            //FlatButtonWidget shaderPackButton = new FlatButtonWidget(shaderPackButtonDim, new TranslatableText(IrisApi.getInstance().getMainScreenLanguageKey()), () -> this.client.setScreen((Screen) IrisApi.getInstance().openMainIrisScreenObj(this)));
            FlatButtonWidget shaderPackButton = new FlatButtonWidget(shaderPackButtonDim, new TranslatableText(IrisCompat.getIrisShaderPacksScreenLanguageKey()).toString(), () -> this.client.openScreen(IrisCompat.getIrisShaderPacksScreen(this)));
            basicFrameBuilder.addChild(dim -> shaderPackButton);
        }

        this.searchTextField = new SearchTextFieldComponent(searchTextFieldDim, this.pages, tabFrameSelectedTab,
                tabFrameScrollBarOffset, optionPageScrollBarOffset, tabFrameDim.getHeight(), this, lastSearch, lastSearchIndex);

        basicFrameBuilder.addChild(dim -> this.searchTextField);

        return basicFrameBuilder;
    }

    public BasicFrame.Builder parentBasicFrameBuilder(Dim2i parentBasicFrameDim, Dim2i tabFrameDim) {
        return BasicFrame.createBuilder()
                .setDimension(parentBasicFrameDim)
                .shouldRenderOutline(false)
                .addChild(dim -> this.undoButton)
                .addChild(dim -> this.applyButton)
                .addChild(dim -> this.closeButton)
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
                );
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrixStack);
        this.updateControls();
        this.frame.render(matrixStack, mouseX, mouseY, delta);
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
        options.notifications.hideDonationButton = true;

        try {
            options.writeChanges();
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
            client.setMipmapLevels(client.options.mipmapLevels);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            MinecraftClient.getInstance().openScreen(new VideoOptionsScreen(this.prevScreen, MinecraftClient.getInstance().options));

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    @Override
    public void onClose() {
        lastSearch.set("");
        lastSearchIndex.set(0);
        this.client.openScreen(this.prevScreen);
    }
}
