package com.roguelike.utils;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

public final class MathUtils {
    private static final int TILE_SIZE = 64;

    private MathUtils() {}

    public static Point2D gridToScreen(int x, int y) {
        return new Point2D(x * TILE_SIZE, y * TILE_SIZE);
    }

    public static boolean isPointInRect(Point2D p, Rectangle2D rect) {
        return rect.contains(p);
    }
}


