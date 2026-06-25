package me.flashyreese.mods.reeses_sodium_options.client.gui.layout;

public record LayoutBounds(int x, int y, int width, int height) {
    public static LayoutBounds relativeTo(LayoutBounds parent, int x, int y, int width, int height) {
        return new LayoutBounds(parent.x() + x, parent.y() + y, width, height);
    }

    public boolean contains(double x, double y) {
        return x >= this.x && x < this.getLimitX() && y >= this.y && y < this.getLimitY();
    }

    public boolean overlaps(LayoutBounds other) {
        return this.x < other.getLimitX() && this.getLimitX() > other.x && this.y < other.getLimitY() && this.getLimitY() > other.y;
    }

    public int getLimitX() {
        return this.x + this.width;
    }

    public int getLimitY() {
        return this.y + this.height;
    }

    public int getCenterX() {
        return this.x + this.width / 2;
    }

    public int getCenterY() {
        return this.y + this.height / 2;
    }

    public LayoutBounds withX(int x) {
        return new LayoutBounds(x, this.y, this.width, this.height);
    }

    public LayoutBounds withY(int y) {
        return new LayoutBounds(this.x, y, this.width, this.height);
    }

    public LayoutBounds withWidth(int width) {
        return new LayoutBounds(this.x, this.y, width, this.height);
    }

    public LayoutBounds withHeight(int height) {
        return new LayoutBounds(this.x, this.y, this.width, height);
    }

    public LayoutBounds withSize(int width, int height) {
        return new LayoutBounds(this.x, this.y, width, height);
    }

    public LayoutBounds translate(int deltaX, int deltaY) {
        return new LayoutBounds(this.x + deltaX, this.y + deltaY, this.width, this.height);
    }
}
