package com.roguelike.entities.attacks;

import com.roguelike.entities.configs.AttackSpec;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 扇形散射：在 forward 方向附近等角度分布。
 */
public class SpreadShot implements AttackStrategy {
    @Override
    public List<Point2D> getDirections(ShooterContext context, AttackSpec spec) {
        int n = Math.max(1, spec.getBulletsPerShot());
        double spread = spec.getSpreadAngleDegrees();
        List<Point2D> list = new ArrayList<>();

        if (n == 1 || spread <= 0.0001) {
            list.add(context.getForward());
            return list;
        }

        double half = spread / 2.0;
        double step = spread / (n - 1);

        double baseAngle = Math.atan2(context.getForward().getY(), context.getForward().getX());
        for (int i = 0; i < n; i++) {
            double offset = -half + i * step;
            double rad = Math.toRadians(offset);
            double angle = baseAngle + rad;
            list.add(new Point2D(Math.cos(angle), Math.sin(angle)));
        }
        return list;
    }
}


