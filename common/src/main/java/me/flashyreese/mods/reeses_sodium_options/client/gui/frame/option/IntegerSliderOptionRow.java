package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.option;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.flashyreese.mods.reeses_sodium_options.client.config.ReeseSodiumOptionsConfig;
import me.flashyreese.mods.reeses_sodium_options.client.gui.layout.LayoutBounds;
import me.flashyreese.mods.reeses_sodium_options.client.gui.state.OptionStateStore;
import me.flashyreese.mods.reeses_sodium_options.client.gui.theme.GuiTheme;
import me.flashyreese.mods.reeses_sodium_options.client.gui.widget.BaseWidget;
import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

final class IntegerSliderOptionRow extends AbstractOptionRow {
    private static final int SLIDER_WIDTH = 90;
    private static final int TRACK_HEIGHT = 10;
    private static final int THUMB_WIDTH = 4;
    private static final int VALUE_GAP = 6;

    private final IntegerOption option;
    private double thumbPosition;
    private boolean sliderHeld;
    private boolean editMode;
    private boolean drawSlider;
    private int contentWidth;

    IntegerSliderOptionRow(LayoutBounds dim, GuiTheme theme, OptionStateStore optionStateStore, IntegerOption option) {
        super(dim, theme, optionStateStore, option);
        this.option = option;
        this.thumbPosition = this.thumbPositionForValue(option.getValidatedValue());
        this.contentWidth = this.valueWidth();
    }

    @Override
    public IntegerOption getOption() {
        return this.option;
    }

    @Override
    protected void prepareRender(int mouseX, int mouseY, float delta) {
        boolean canDrawSlider = this.option.isEnabled() && this.option.showControl();
        this.drawSlider = canDrawSlider && (this.isMouseOverRow(mouseX, mouseY) || this.isRowFocused() || this.sliderHeld);
        int valueWidth = this.valueWidth();
        this.contentWidth = this.drawSlider ? SLIDER_WIDTH + VALUE_GAP + valueWidth : valueWidth;
    }

    @Override
    protected int controlContentWidth() {
        return this.contentWidth;
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        if (!this.option.showControl()) {
            return;
        }

        Component value = this.displayValue();
        int valueWidth = this.font.width(value);
        int sliderX = this.sliderX();
        int sliderY = this.sliderY();

        if (this.drawSlider) {
            if (!this.sliderHeld) {
                this.thumbPosition = this.thumbPositionForValue(this.option.getValidatedValue());
            }

            int thumbX = sliderX + (int) (this.thumbPosition * SLIDER_WIDTH) - (THUMB_WIDTH / 2);
            int trackY = (int) (sliderY + (TRACK_HEIGHT / 2.0F) - 0.5D);

            this.drawRect(guiGraphics, sliderX, trackY, sliderX + SLIDER_WIDTH, trackY + 1, this.theme.themeLighter);
            this.drawRect(guiGraphics, thumbX, sliderY, thumbX + THUMB_WIDTH, sliderY + TRACK_HEIGHT, 0xFFFFFFFF);
            if (this.isRowFocused() && this.editMode && BaseWidget.isKeyboardFocusVisible()) {
                this.drawBorder(guiGraphics, thumbX - 1, sliderY - 1, thumbX + THUMB_WIDTH + 1, sliderY + TRACK_HEIGHT + 1, 0xFFFFFFFF);
            }

            this.drawString(guiGraphics, value, sliderX - valueWidth - VALUE_GAP, sliderY + (TRACK_HEIGHT / 2) - 4, 0xFFFFFFFF);
        } else {
            this.drawString(guiGraphics, value, sliderX + SLIDER_WIDTH - valueWidth, sliderY + (TRACK_HEIGHT / 2) - 4, 0xFFFFFFFF);
        }

        if (this.isMouseOverSlider(mouseX, mouseY)) {
            guiGraphics.requestCursor(this.sliderHeld ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND);
        }
    }

    @Override
    protected boolean controlMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.sliderHeld = false;
        if (!this.option.isEnabled()
                || !this.option.showControl()
                || event.button() != 0
                || !this.isMouseOverRow(event.x(), event.y())) {
            return false;
        }

        if (this.isMouseOverSlider(event.x(), event.y())) {
            this.setValueFromMouse(event.x());
            this.sliderHeld = true;
            this.actionButtons.holdLayout(true);
        }

        return true;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
        if (!this.sliderHeld || event.button() != 0 || !this.option.isEnabled()) {
            return false;
        }

        this.actionButtons.holdLayout(true);
        this.setValueFromMouse(event.x());

        return true;
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (event.button() != 0 || !this.sliderHeld) {
            return false;
        }

        this.sliderHeld = false;
        this.actionButtons.releaseLayoutHold();
        this.playClickSound();

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!ReeseSodiumOptionsConfig.config().isShiftScrollSliderAdjustments()
                || !this.option.isEnabled()
                || !this.option.showControl()
                || !Minecraft.getInstance().hasShiftDown()
                || !this.isMouseOverSlider(mouseX, mouseY)) {
            return false;
        }

        return this.adjustValue((int) verticalAmount);
    }

    @Override
    protected boolean controlKeyPressed(KeyEvent event) {
        if (!this.isRowFocused()) {
            return false;
        }

        if (event.isSelection()) {
            this.editMode = !this.editMode;
            return true;
        }

        if (this.editMode) {
            if (event.isLeft()) {
                return this.adjustValue(-1);
            } else if (event.isRight()) {
                return this.adjustValue(1);
            }
        }

        return false;
    }

    @Override
    protected boolean activateControl() {
        this.editMode = !this.editMode;

        return true;
    }

    @Override
    protected void releaseMouseHold() {
        this.sliderHeld = false;
    }

    @Override
    protected void onControlFocusLost() {
        this.editMode = false;
    }

    private Component displayValue() {
        Component value = this.option.formatValue(this.option.getValidatedValue());

        return this.option.isEnabled() ? value : this.formatDisabledControlValue(value);
    }

    private int valueWidth() {
        return this.font.width(this.displayValue());
    }

    private int sliderX() {
        return this.rightAlignedControlX(SLIDER_WIDTH);
    }

    private int sliderY() {
        return this.getDimensions().getCenterY() - TRACK_HEIGHT / 2;
    }

    private boolean isMouseOverSlider(double mouseX, double mouseY) {
        int sliderX = this.sliderX();
        int sliderY = this.sliderY();

        return mouseX >= sliderX
                && mouseX < sliderX + SLIDER_WIDTH
                && mouseY >= sliderY
                && mouseY < sliderY + TRACK_HEIGHT;
    }

    private double thumbPositionForValue(int value) {
        SteppedValidator validator = this.option.getSteppedValidator();
        int min = validator.min();
        int max = validator.max();

        if (max == min) {
            return 0.0D;
        }

        return Mth.clamp((double) (value - min) / (double) (max - min), 0.0D, 1.0D);
    }

    private int valueForThumbPosition() {
        SteppedValidator validator = this.option.getSteppedValidator();
        int step = validator.step();
        int min = validator.min();
        int max = validator.max();

        return Mth.clamp(min + step * (int) Math.round(this.thumbPosition * (max - min) / (double) step), min, max);
    }

    private void setValueFromMouse(double mouseX) {
        this.thumbPosition = Mth.clamp((mouseX - this.sliderX()) / (double) SLIDER_WIDTH, 0.0D, 1.0D);
        this.option.modifyValue(this.valueForThumbPosition());
    }

    private boolean adjustValue(int direction) {
        if (direction == 0) {
            return false;
        }

        SteppedValidator validator = this.option.getSteppedValidator();
        int value = this.option.getValidatedValue();
        int nextValue = Mth.clamp(value + validator.step() * direction, validator.min(), validator.max());

        if (nextValue == value) {
            return false;
        }

        this.option.modifyValue(nextValue);
        this.thumbPosition = this.thumbPositionForValue(this.option.getValidatedValue());

        return true;
    }
}
