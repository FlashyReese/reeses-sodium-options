package me.flashyreese.mods.reeses_sodium_options.client.gui;

import me.jellysquid.mods.sodium.client.util.Dim2i;

public interface Dim2iExtended {
    void setPoint2i(Point2i point2i);

    void setX(int x);

    void setY(int y);

    void setWidth(int width);

    void setHeight(int height);

    boolean canFitDimension(Dim2i anotherDim);

    boolean overlapWith(Dim2i anotherDim);
}
