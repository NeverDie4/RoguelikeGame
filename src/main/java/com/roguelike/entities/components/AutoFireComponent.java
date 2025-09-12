package com.roguelike.entities.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.roguelike.core.TimeService;
import com.roguelike.entities.Bullet;
import com.roguelike.entities.EntityBase;
import com.roguelike.entities.StraightBullet;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

/**
 * 玩家自动发射子弹组件。
 * - 使用受控时间 TimeService 作为时间源
 * - 默认每 0.5s 发射一次，预留可调属性
 * - 每次按对角四方向各发射一枚
 */
public class AutoFireComponent extends Component {

    private double fireIntervalSeconds = 0.5;
    private double lastFireSeconds = 0.0;

    public AutoFireComponent() {}

    public AutoFireComponent(double fireIntervalSeconds) {
        this.fireIntervalSeconds = Math.max(0.05, fireIntervalSeconds);
    }

    @Override
    public void onUpdate(double tpf) {
        double now = TimeService.getSeconds();
        if (now - lastFireSeconds >= fireIntervalSeconds) {
            lastFireSeconds = now;
            fireFourDiagonalBullets();
        }
    }

    private void fireFourDiagonalBullets() {
        // 直接使用实体中心（FXGL 基于实体宽高计算）
        double cx = entity.getCenter().getX();
        double cy = entity.getCenter().getY();

        spawnBullet(new Point2D(-1, -1), cx, cy);
        spawnBullet(new Point2D(-1,  1), cx, cy);
        spawnBullet(new Point2D( 1, -1), cx, cy);
        spawnBullet(new Point2D( 1,  1), cx, cy);
    }

    private void spawnBullet(Point2D dir, double x, double y) {
        StraightBullet bullet = StraightBullet.createPlayerBullet(dir);
        // 先加入世界，再将位置设置为中心减半尺寸（动画帧为 16x16）
        FXGL.getGameWorld().addEntity(bullet);
        bullet.getTransformComponent().setPosition(x - 8.0, y - 8.0);
    }

    public double getFireIntervalSeconds() {
        return fireIntervalSeconds;
    }

    public void setFireIntervalSeconds(double fireIntervalSeconds) {
        this.fireIntervalSeconds = Math.max(0.05, fireIntervalSeconds);
    }
}


