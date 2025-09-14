package com.roguelike.entities.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;

/**
 * 反弹钻头：以速度向量移动，撞到屏幕边界反弹。外部保证寿命（5s）。
 * 暂不做敌人碰撞，仅移动与反弹。
 */
public class ReboundDrillComponent extends Component {
    private Point2D velocity;

    public ReboundDrillComponent(Point2D initialVelocity) {
        // 直接采用给定速度向量（长度即速度）
        if (initialVelocity == null || (initialVelocity.getX() == 0 && initialVelocity.getY() == 0)) {
            this.velocity = new Point2D(300, 0);
        } else {
            this.velocity = initialVelocity;
        }
    }

    @Override
    public void onUpdate(double tpf) {
        if (com.roguelike.core.TimeService.isPaused()) return;
        double dx = velocity.getX() * tpf;
        double dy = velocity.getY() * tpf;
        entity.translate(dx, dy);

        // 使用视口可见区域作为反弹边界
        var view = com.almasb.fxgl.dsl.FXGL.getGameScene().getViewport().getVisibleArea();
        double minX = view.getMinX();
        double minY = view.getMinY();
        double maxX = view.getMaxX();
        double maxY = view.getMaxY();
        double x = entity.getX();
        double y = entity.getY();
        double ew = entity.getWidth();
        double eh = entity.getHeight();

        boolean bounced = false;
        if (x <= minX || x + ew >= maxX) {
            velocity = new Point2D(-velocity.getX(), velocity.getY());
            // 夹回边界内，避免卡壁
            double nx = Math.min(Math.max(x, minX + 1), maxX - ew - 1);
            entity.setX(nx);
            bounced = true;
        }
        if (y <= minY || y + eh >= maxY) {
            velocity = new Point2D(velocity.getX(), -velocity.getY());
            double ny = Math.min(Math.max(y, minY + 1), maxY - eh - 1);
            entity.setY(ny);
            bounced = true;
        }
        if (bounced) {
            try { com.roguelike.ui.SoundService.playBounce(); } catch (Exception ignored) {}
        }
        // 保持速度恒定，直到寿命结束（不再衰减）
    }
}


