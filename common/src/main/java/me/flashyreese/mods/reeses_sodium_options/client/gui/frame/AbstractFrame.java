package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractFrame extends AbstractOptionList implements ContainerEventHandler {
    protected final Screen screen;
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected final List<ControlElement> controlElements = new ArrayList<>();
    protected final ModOptions modOptions;
    protected boolean renderOutline;
    private GuiEventListener focused;
    private boolean dragging;
    private Consumer<GuiEventListener> focusListener;

    public AbstractFrame(Dim2i dim, Screen screen, boolean renderOutline, ModOptions modOptions) {
        super(dim);
        this.screen = screen;
        this.renderOutline = renderOutline;
        this.modOptions = modOptions;
    }

    @Override
    public int getScrollAmount() {
        return 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (GuiEventListener element : this.children) {
            if (element instanceof AbstractFrame abstractFrame) {
                for (ControlElement controlElement : abstractFrame.controlElements) {
                    if (controlElement.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
                        return true;
                }
                if (abstractFrame.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
                    return true;
            }
            if (element instanceof ControlElement controlElement) {
                if (controlElement.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount))
                    return true;
            }
        }
        return false;
    }

    public void buildFrame() {
        for (GuiEventListener element : this.children) {
            if (element instanceof AbstractFrame abstractFrame) {
                this.controlElements.addAll(abstractFrame.controlElements);
            }
            if (element instanceof ControlElement) {
                this.controlElements.add((ControlElement) element);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (this.renderOutline) {
            this.drawBorder(drawContext, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), 0xFFAAAAAA);
        }
        for (Renderable renderable : this.children) {
            renderable.render(drawContext, mouseX, mouseY, delta);
        }
    }

    public void applyScissor(GuiGraphics guiGraphics, int x, int y, int width, int height, Runnable action) {
        guiGraphics.enableScissor(x, y, x + width, y + height);
        action.run();
        guiGraphics.disableScissor();
    }

    public void registerFocusListener(Consumer<GuiEventListener> focusListener) {
        this.focusListener = focusListener;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (this.focused != null) {
            this.focused.setFocused(false);
        }
        this.focused = focused;
        if (this.focusListener != null) {
            this.focusListener.accept(focused);
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent navigation) {
        return super.nextFocusPath(navigation);
    }
}
