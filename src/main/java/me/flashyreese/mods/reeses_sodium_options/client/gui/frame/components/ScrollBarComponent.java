package me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components;

import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class ScrollBarComponent extends AbstractWidget {

    protected final Dim2i dim;

    private final Mode mode;
    private final int frameLength;
    private final int viewPortLength;
    private final int maxScrollBarOffset;
    private final Runnable onSetOffset;
    private int offset = 0;

    private Dim2i scrollThumb = null;
    private int scrollThumbClickOffset = -1;

    private Dim2i extendedScrollArea = null;

    public ScrollBarComponent(Dim2i scrollBarArea, Mode mode, int frameLength, int viewPortLength, Runnable onSetOffset) {
        this.dim = scrollBarArea;
        this.mode = mode;
        this.frameLength = frameLength;
        this.viewPortLength = viewPortLength;
        this.onSetOffset = onSetOffset;
        this.maxScrollBarOffset = this.frameLength - this.viewPortLength;
    }

    public ScrollBarComponent(Dim2i scrollBarArea, Mode mode, int frameLength, int viewPortLength, Runnable onSetOffset, Dim2i extendedScrollArea) {
        this.dim = scrollBarArea;
        this.mode = mode;
        this.frameLength = frameLength;
        this.viewPortLength = viewPortLength;
        this.extendedScrollArea = extendedScrollArea;
        this.onSetOffset = onSetOffset;
        this.maxScrollBarOffset = this.frameLength - this.viewPortLength;
    }

    public void updateThumbPosition() {
        int scrollThumbLength = (this.viewPortLength * (this.mode == Mode.VERTICAL ? this.dim.getHeight() : this.dim.getWidth() - 6)) / this.frameLength;
        int maximumScrollThumbOffset = this.viewPortLength - scrollThumbLength;
        int scrollThumbOffset = this.offset * maximumScrollThumbOffset / this.maxScrollBarOffset;
        this.scrollThumb = new Dim2i(this.dim.getOriginX() + 2 + (this.mode == Mode.HORIZONTAL ? scrollThumbOffset : 0), this.dim.getOriginY() + 2 + (this.mode == Mode.VERTICAL ? scrollThumbOffset : 0), (this.mode == Mode.VERTICAL ? this.dim.getWidth() : scrollThumbLength) - 4, (this.mode == Mode.VERTICAL ? scrollThumbLength : this.dim.getHeight()) - 4);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.drawRectOutline(this.dim.getOriginX(), this.dim.getOriginY(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
        this.drawRectangle(this.scrollThumb.getOriginX(), this.scrollThumb.getOriginY(), this.scrollThumb.getLimitX(), this.scrollThumb.getLimitY(), 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.dim.containsCursor(mouseX, mouseY)) {
            if (this.scrollThumb.containsCursor(mouseX, mouseY)) {
                this.scrollThumbClickOffset = this.mode == Mode.VERTICAL ? (int) mouseY - this.scrollThumb.getOriginY() : (int) mouseX - this.scrollThumb.getOriginX();
            }else{
                int value = this.mode == Mode.VERTICAL ? (int) mouseY - this.dim.getOriginY(): (int) mouseX - this.dim.getOriginX();
                this.setOffset(value);
            }
            return true;
        } else {
            this.scrollThumbClickOffset = -1;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrollThumbClickOffset > -1) {
            int value = this.mode == Mode.VERTICAL ? (int) mouseY - this.scrollThumbClickOffset : (int) mouseX - this.scrollThumbClickOffset;
            this.setOffset(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.dim.containsCursor(mouseX, mouseY) || this.extendedScrollArea != null && this.extendedScrollArea.containsCursor(mouseX, mouseY)) {
            if (this.offset <= this.maxScrollBarOffset && this.offset >= 0) {
                int value = (int) (this.offset - amount * 6);
                this.setOffset(value);
                return true;
            }
        }
        return false;
    }

    private void setOffset(int value) {
        this.offset = MathHelper.clamp(value, 0, this.maxScrollBarOffset);
        this.updateThumbPosition();
        this.onSetOffset.run();
    }

    public int getOffset() {
        return this.offset;
    }

    public enum Mode {
        HORIZONTAL,
        VERTICAL
    }

    //Fixme: Duplicated, fix in 1.17 when Sodium release.
    protected void drawRectangle(double x1, double y1, double x2, double y2, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        this.drawQuads(vertices -> this.addQuad(vertices, x1, y1, x2, y2, a, r, g, b));
    }

    protected void drawRectOutline(double x, double y, double w, double h, int color) {
        final float a = (float) (color >> 24 & 255) / 255.0F;
        final float r = (float) (color >> 16 & 255) / 255.0F;
        final float g = (float) (color >> 8 & 255) / 255.0F;
        final float b = (float) (color & 255) / 255.0F;

        this.drawQuads(vertices -> {
            this.addQuad(vertices, x, y, w, y + 1, a, r, g, b);
            this.addQuad(vertices, x, h - 1, w, h, a, r, g, b);
            this.addQuad(vertices, x, y, x + 1, h, a, r, g, b);
            this.addQuad(vertices, w - 1, y, w, h, a, r, g, b);
        });
    }

    protected void addQuad(VertexConsumer consumer, double x1, double y1, double x2, double y2, float a, float r, float g, float b) {
        consumer.vertex(x2, y1, 0.0D).color(r, g, b, a).next();
        consumer.vertex(x1, y1, 0.0D).color(r, g, b, a).next();
        consumer.vertex(x1, y2, 0.0D).color(r, g, b, a).next();
        consumer.vertex(x2, y2, 0.0D).color(r, g, b, a).next();
    }
}