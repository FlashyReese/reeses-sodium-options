package me.flashyreese.mods.reeses_sodium_options.client.gui;

import net.caffeinemc.mods.sodium.client.util.Dim2i;

public interface Dim2iAccess {
    void setPoint2i(Point2iAccess point2i);

    void setX(int x);

    void setY(int y);

    void setWidth(int width);

    void setHeight(int height);

    boolean canFitDimension(Dim2i anotherDim);

    boolean overlapWith(Dim2i anotherDim);
}
