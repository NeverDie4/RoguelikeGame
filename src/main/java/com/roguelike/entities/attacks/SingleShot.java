package com.roguelike.entities.attacks;

import com.roguelike.entities.configs.AttackSpec;
import javafx.geometry.Point2D;

import java.util.Collections;
import java.util.List;

/**
 * 单发直射。
 */
public class SingleShot implements AttackStrategy {
    @Override
    public List<Point2D> getDirections(ShooterContext context, AttackSpec spec) {
        return Collections.singletonList(context.getForward());
    }
}


