package com.roguelike.entities.bullets;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.roguelike.entities.Bullet;
import com.roguelike.entities.components.BulletAnimationComponent;
import javafx.geometry.Point2D;

/**
 * 环绕型子弹：围绕玩家旋转，持续 4s。
 * 数值：半径约 70px，角速度约 180 度/秒；动画使用 bullets/01；尺寸统一 16x16。
 * 碰撞与友军判定后续实现。
 */
public class OrbitingBullet extends Bullet {

    private BulletAnimationComponent animationComponent;

    public OrbitingBullet(Faction faction, Point2D dummyDir, int damage, boolean piercing, double speed,
                          double radius, double angularSpeedDegPerSec, double initialAngleDeg, Entity playerEntity) {
        super(faction, damage, piercing, speed);

        setSize(16, 16);

        // 动画
        animationComponent = new BulletAnimationComponent();
        addComponent(animationComponent);
        animationComponent.setLooping(true);
        animationComponent.setFrameDuration(0.05);
        animationComponent.loadAnimationFrames("bullets/01", 10);

        // 环绕控制
        addComponent(new OrbitMovementComponent(playerEntity, radius, angularSpeedDegPerSec, initialAngleDeg, 4.0));
    }

    /**
     * 环绕运动组件：围绕目标实体中心点，以固定半径与角速度旋转，持续 durationSeconds 后移除。
     */
    private static class OrbitMovementComponent extends Component {
        private final Entity target; // 玩家实体
        private final double radius;
        private final double angularSpeed; // 度/秒
        private final double duration;
        private double angleDeg;
        private double elapsed = 0.0;

        public OrbitMovementComponent(Entity target, double radius, double angularSpeedDegPerSec, double initialAngleDeg, double durationSeconds) {
            this.target = target;
            this.radius = radius;
            this.angularSpeed = angularSpeedDegPerSec;
            this.angleDeg = initialAngleDeg;
            this.duration = durationSeconds;
        }

        @Override
        public void onUpdate(double tpf) {
            elapsed += tpf;
            if (elapsed >= duration) {
                entity.removeFromWorld();
                return;
            }

            angleDeg += angularSpeed * tpf;
            double rad = Math.toRadians(angleDeg);

            double cx = target.getCenter().getX();
            double cy = target.getCenter().getY();

            double x = cx + Math.cos(rad) * radius - entity.getWidth() / 2.0;
            double y = cy + Math.sin(rad) * radius - entity.getHeight() / 2.0;
            entity.getTransformComponent().setPosition(x, y);
        }
    }
}


