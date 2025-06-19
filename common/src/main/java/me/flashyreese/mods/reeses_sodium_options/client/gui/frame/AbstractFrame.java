package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractFrame extends AbstractWidget implements ContainerEventHandler {
    protected final Dim2i dim;
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected final List<ControlElement<?>> controlElements = new ArrayList<>();
    protected boolean renderOutline;
    private GuiEventListener focused;
    private boolean dragging;
    private Consumer<GuiEventListener> focusListener;

    public AbstractFrame(Dim2i dim, boolean renderOutline) {
        this.dim = dim;
        this.renderOutline = renderOutline;
    }

    public void buildFrame() {
        for (GuiEventListener element : this.children) {
            if (element instanceof AbstractFrame abstractFrame) {
                this.controlElements.addAll(abstractFrame.controlElements);
            }
            if (element instanceof ControlElement<?>) {
                this.controlElements.add((ControlElement<?>) element);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (this.renderOutline) {
            this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
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
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent navigation) {
        return ContainerEventHandler.super.nextFocusPath(navigation);
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }

    public List<ControlElement<?>> getControlElements() {
        return controlElements;
    }
}
