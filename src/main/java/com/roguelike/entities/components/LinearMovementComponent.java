package com.roguelike.entities.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

/**
 * 简单的直线运动组件：以固定速度沿固定方向移动，无射程上限。
 */
public class LinearMovementComponent extends Component {

    private Point2D direction = new Point2D(1, 0);
    private double speed = 0.0; // 像素/秒

    public LinearMovementComponent(Point2D direction, double speed) {
        if (direction != null) {
            Point2D d = direction;
            if (d.magnitude() == 0) {
                d = new Point2D(1, 0);
            }
            this.direction = d.normalize();
        }
        this.speed = Math.max(0.0, speed);
    }

    @Override
    public void onUpdate(double tpf) {
        if (speed <= 0) return;
        double dx = direction.getX() * speed * tpf;
        double dy = direction.getY() * speed * tpf;
        entity.translate(dx, dy);
    }

    public void setDirection(Point2D newDirection) {
        if (newDirection == null || newDirection.magnitude() == 0) return;
        this.direction = newDirection.normalize();
    }

    public void setSpeed(double newSpeed) {
        this.speed = Math.max(0.0, newSpeed);
    }

    public Point2D getDirection() {
        return direction;
    }

    public double getSpeed() {
        return speed;
    }
}


