package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.flashyreese.mods.reeses_sodium_options.client.gui.AbstractWidgetExtended;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BasicFrame extends AbstractFrame {

    protected List<Function<Dim2i, AbstractWidget>> functions;

    public BasicFrame(Dim2i dim, Screen screen, boolean renderOutline, List<Function<Dim2i, AbstractWidget>> functions, ModOptions modOptions) {
        super(dim, screen, renderOutline, modOptions);
        this.functions = functions;
        this.buildFrame();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void buildFrame() {
        this.children.clear();
        this.controlElements.clear();

        this.functions.forEach(function -> this.children.add(function.apply(((AbstractWidgetExtended) this).getDim())));

        super.buildFrame();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta);
    }

    public static class Builder {
        private final List<Function<Dim2i, AbstractWidget>> functions = new ArrayList<>();
        private Dim2i dim;
        private boolean renderOutline;
        private Screen screen;
        private ModOptions modOptions;

        public Builder withDimension(Dim2i dim) {
            this.dim = dim;
            return this;
        }

        public Builder withRenderOutline(boolean renderOutline) {
            this.renderOutline = renderOutline;
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

        public Builder addChild(Function<Dim2i, AbstractWidget> function) {
            this.functions.add(function);
            return this;
        }

        public BasicFrame build() {
            Validate.notNull(this.dim, "Dimension must be specified");
            Validate.notNull(this.screen, "Screen must be specified");

            return new BasicFrame(this.dim, this.screen, this.renderOutline, this.functions, this.modOptions);
        }
    }
}