package me.flashyreese.mods.reeses_sodium_options.client.gui.widget;

import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public abstract class BaseWidget implements Renderable, GuiEventListener, NarratableEntry {
    protected final Font font;
    protected boolean focused;
    protected boolean hovered;
    private static boolean keyboardFocusVisible = Minecraft.getInstance().getLastInputType().isKeyboard();
    private LayoutBounds dim;

    protected BaseWidget(LayoutBounds dim) {
        this.font = Minecraft.getInstance().font;
        this.dim = dim;
    }

    public void setDim(LayoutBounds dim) {
        this.dim = dim;
    }

    public LayoutBounds getDimensions() {
        return this.dim;
    }

    public int getX() {
        return this.dim.x();
    }

    public int getY() {
        return this.dim.y();
    }

    public int getWidth() {
        return this.dim.width();
    }

    public int getHeight() {
        return this.dim.height();
    }

    public int getLimitX() {
        return this.dim.getLimitX();
    }

    public int getLimitY() {
        return this.dim.getLimitY();
    }

    public int getCenterX() {
        return this.dim.getCenterX();
    }

    public int getCenterY() {
        return this.dim.getCenterY();
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public static boolean isKeyboardFocusVisible() {
        return keyboardFocusVisible;
    }

    public static void setKeyboardFocusVisible(boolean keyboardFocusVisible) {
        BaseWidget.keyboardFocusVisible = keyboardFocusVisible;
    }

    protected boolean shouldRenderFocusBorder() {
        return this.shouldRenderFocusBorder(this.isFocused());
    }

    protected boolean shouldRenderFocusBorder(boolean focused) {
        if (!focused) {
            return false;
        }

        return switch (ReeseSodiumOptionsConfig.config().getFocusBorderMode()) {
            case ALWAYS -> true;
            case KEYBOARD -> isKeyboardFocusVisible();
            case NEVER -> false;
        };
    }

    protected static boolean isSelectionKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.contains(mouseX, mouseY);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        return this.isFocused() ? null : ComponentPath.leaf(this);
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarrationPriority.FOCUSED;
        }

        return this.isHovered() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
    }

    protected void addButtonNarration(NarrationElementOutput builder, Component label) {
        builder.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", label));
        this.addButtonUsageNarration(builder);
    }

    protected void addButtonUsageNarration(NarrationElementOutput builder) {
        this.addUsageNarration(builder, "narration.button.usage.focused", "narration.button.usage.hovered");
    }

    protected void addUsageNarration(NarrationElementOutput builder, String focusedKey, String hoveredKey) {
        if (this.isFocused()) {
            builder.add(NarratedElementType.USAGE, Component.translatable(focusedKey));
        } else if (this.isHovered()) {
            builder.add(NarratedElementType.USAGE, Component.translatable(hoveredKey));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    protected void drawString(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x, y, color);
    }

    protected void drawString(GuiGraphics guiGraphics, Component text, int x, int y, int color) {
        guiGraphics.drawString(this.font, text, x, y, color);
    }

    protected void drawCenteredString(GuiGraphics guiGraphics, Component text, int x, int y, int color) {
        guiGraphics.drawCenteredString(this.font, text, x, y, color);
    }

    protected int getStringWidth(FormattedText text) {
        return this.font.width(text);
    }

    protected void drawRect(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y2, color);
    }

    protected void drawBorder(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        border(guiGraphics, x1, y1, x2, y2, color);
    }

    public static void border(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y1 + 1, color);
        guiGraphics.fill(x1, y2 - 1, x2, y2, color);
        guiGraphics.fill(x1, y1, x1 + 1, y2, color);
        guiGraphics.fill(x2 - 1, y1, x2, y2, color);
    }

    protected void playClickSound() {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }

    protected String truncateTextToFit(String text, int maxWidth) {
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        maxWidth -= ellipsisWidth;
        int high = text.length() - ellipsis.length();
        int low = 1;
        while (high - low > 1) {
            int mid = (high + low) / 2;
            if (this.font.width(text.substring(0, mid)) > maxWidth) {
                high = mid;
            } else {
                low = mid;
            }
        }

        return text.substring(0, low).trim() + ellipsis;
    }
}
