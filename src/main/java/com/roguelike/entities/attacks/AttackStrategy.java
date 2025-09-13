package com.roguelike.entities.attacks;

import com.roguelike.entities.configs.AttackSpec;
import javafx.geometry.Point2D;

import java.util.List;

/**
 * 发射策略接口：根据 AttackSpec 产出子弹方向集合。
 */
public interface AttackStrategy {
    List<Point2D> getDirections(ShooterContext context, AttackSpec spec);

    default double getFireIntervalSeconds(AttackSpec spec) {
        return spec.getFireIntervalSeconds();
    }
}


