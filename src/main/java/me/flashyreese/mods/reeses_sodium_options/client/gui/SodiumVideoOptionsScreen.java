package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class SodiumVideoOptionsScreen extends Screen {

    private AbstractFrame frame;

    private final Screen prevScreen;
    private final List<OptionPage> pages = new ArrayList<>();

    private FlatButtonWidget applyButton, closeButton, undoButton;
    private FlatButtonWidget donateButton, hideDonateButton;
    private boolean hasPendingChanges;

    public SodiumVideoOptionsScreen(Screen prev) {
        super(new LiteralText("Reese's Sodium Menu"));

        this.prevScreen = prev;

        this.pages.add(SodiumGameOptionPages.general());
        this.pages.add(SodiumGameOptionPages.quality());
        this.pages.add(SodiumGameOptionPages.advanced());
    }

    @Override
    protected void init() {
        this.frame = this.parentFrameBuilder().build();
        this.children.add(this.frame);
    }

    protected BasicFrame.Builder parentFrameBuilder() {
        BasicFrame.Builder basicFrameBuilder;

        Dim2i basicFrameDim = new Dim2i(0, 0, this.client.getWindow().getScaledWidth(), this.client.getWindow().getScaledHeight());
        Dim2i tabFrameDim = new Dim2i(basicFrameDim.getWidth() / 4 / 2, basicFrameDim.getHeight() / 4 / 2, basicFrameDim.getWidth() / 4 * 3, basicFrameDim.getHeight() / 4 * 3);

        Dim2i undoButtonDim = new Dim2i(tabFrameDim.getLimitX() - 203, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i applyButtonDim = new Dim2i(tabFrameDim.getLimitX() - 134, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i closeButtonDim = new Dim2i(tabFrameDim.getLimitX() - 65, tabFrameDim.getLimitY() + 5, 65, 20);

        Dim2i donateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 122, tabFrameDim.getOriginY() - 26, 100, 20);
        Dim2i hideDonateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 20, tabFrameDim.getOriginY() - 26, 20, 20);

        this.undoButton = new FlatButtonWidget(undoButtonDim, "Undo", this::undoChanges);
        this.applyButton = new FlatButtonWidget(applyButtonDim, "Apply", this::applyChanges);
        this.closeButton = new FlatButtonWidget(closeButtonDim, "Close", this::onClose);

        this.donateButton = new FlatButtonWidget(donateButtonDim, "Buy us a coffee!", this::openDonationPage);
        this.hideDonateButton = new FlatButtonWidget(hideDonateButtonDim, "x", this::hideDonationButton);

        if (SodiumClientMod.options().notifications.hideDonationButton) {
            this.setDonationButtonVisibility(false);
        }

        basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);

        if (FabricLoader.getInstance().isModLoaded("iris")) {
            int size = this.client.textRenderer.getWidth(new TranslatableText("options.iris.shaderPackSelection"));
            Dim2i shaderPackButtonDim;
            if (!SodiumClientMod.options().notifications.hideDonationButton) {
                shaderPackButtonDim = new Dim2i(tabFrameDim.getLimitX() - 134 - size, tabFrameDim.getOriginY() - 26, 10 + size, 20);
            } else {
                shaderPackButtonDim = new Dim2i(tabFrameDim.getLimitX() - size - 10, tabFrameDim.getOriginY() - 26, 10 + size, 20);
            }
            FlatButtonWidget shaderPackButton = new FlatButtonWidget(shaderPackButtonDim, new TranslatableText("options.iris.shaderPackSelection").asString(), () -> {

                /*this.client.openScreen(new net.coderbot.iris.gui.screen.ShaderPackScreen(this))*/
                // Let's hope Iris doesn't change the constructor nor the classpath :>
                try {
                    Class<? extends Screen> screen = (Class<? extends Screen>) Class.forName("net.coderbot.iris.gui.screen.ShaderPackScreen");
                    Screen instance = screen.getDeclaredConstructor(Screen.class).newInstance(this);
                    this.client.openScreen(instance);
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
            basicFrameBuilder.addChild(dim -> shaderPackButton);
        }

        return basicFrameBuilder;
    }

    public BasicFrame.Builder parentBasicFrameBuilder(Dim2i parentBasicFrameDim, Dim2i tabFrameDim) {
        return BasicFrame.createBuilder()
                .setDimension(parentBasicFrameDim)
                .addChild(parentDim -> TabFrame.createBuilder()
                        .setDimension(tabFrameDim)
                        .addTabs(tabs -> this.pages.forEach(page -> tabs.add(dim -> new Tab.Builder<>().from(page, dim))))
                        .build()
                )
                .addChild(dim -> this.undoButton)
                .addChild(dim -> this.applyButton)
                .addChild(dim -> this.closeButton)
                .addChild(dim -> this.donateButton)
                .addChild(dim -> this.hideDonateButton);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrices);
        this.updateControls();
        this.frame.render(matrices, mouseX, mouseY, delta);
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
        this.client.openScreen(this.prevScreen);
    }
}