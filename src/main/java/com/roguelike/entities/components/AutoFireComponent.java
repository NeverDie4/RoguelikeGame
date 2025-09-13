package com.roguelike.entities.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.roguelike.core.TimeService;
import com.roguelike.entities.Bullet;
import com.roguelike.entities.attacks.AttackStrategy;
import com.roguelike.entities.attacks.SingleShot;
import com.roguelike.entities.attacks.ShooterContext;
import com.roguelike.entities.configs.AttackRegistry;
import com.roguelike.entities.configs.AttackSpec;
import com.roguelike.entities.configs.BulletRegistry;
import com.roguelike.entities.configs.BulletSpec;
import com.roguelike.entities.factory.BulletFactory;
import javafx.geometry.Point2D;

import java.util.List;

/**
 * 自动发射组件：基于 AttackSpec + AttackStrategy + BulletFactory。
 */
public class AutoFireComponent extends Component {

    // 保留用于兼容性，但实际使用 BULLET_IDS 数组
    @SuppressWarnings("unused")
    private String attackSpecId = "single_01";
    private AttackStrategy strategy = new SingleShot();
    private double lastFireSeconds = 0.0;
    private Point2D forward = new Point2D(1, 0);
    
    // 7种子弹的ID列表，用于交替发射
    private static final String[] BULLET_IDS = {
        "single_01", "single_02", "single_03", "single_04", 
        "single_05", "single_06", "single_07"
    };
    private int currentBulletIndex = 0;

    public AutoFireComponent() {}

    public AutoFireComponent(String attackSpecId, AttackStrategy strategy) {
        if (attackSpecId != null) this.attackSpecId = attackSpecId;
        if (strategy != null) this.strategy = strategy;
    }

    public AutoFireComponent(double intervalSeconds) {
        // 兼容旧构造：修改默认 AttackSpec 的间隔
        AttackSpec spec = AttackRegistry.get("single_01");
        if (spec != null) {
            AttackRegistry.register(new AttackSpec(
                    spec.getId(), spec.getDisplayName(), spec.getBulletSpecId(),
                    Math.max(0.05, intervalSeconds), spec.getBulletsPerShot(), spec.getSpreadAngleDegrees()
            ));
        }
    }

    @Override
    public void onUpdate(double tpf) {
        // 以 single_01 的配置为基准，适当减慢发射频率（例如放大为 0.8s）
        AttackSpec base = AttackRegistry.get("single_01");
        if (base == null) return;

        double now = TimeService.getSeconds();
        double interval = Math.max(0.05, Math.max(base.getFireIntervalSeconds(), 0.8));
        if (now - lastFireSeconds >= interval) {
            lastFireSeconds = now;

            // 交替发射当前索引的子弹
            String currentId = BULLET_IDS[currentBulletIndex];
            AttackSpec spec = AttackRegistry.get(currentId);
            if (spec != null) {
                fire(spec);
            }

            currentBulletIndex = (currentBulletIndex + 1) % BULLET_IDS.length;
        }
    }

    private void fire(AttackSpec spec) {
        // 从玩家实体获取面朝方向
        Point2D fwd = forward;
        if (entity instanceof com.roguelike.entities.Player) {
            Point2D pf = ((com.roguelike.entities.Player) entity).getForward();
            if (pf != null && (pf.getX() != 0 || pf.getY() != 0)) {
                fwd = pf.normalize();
            }
        }
        BulletSpec bulletSpec = BulletRegistry.get(spec.getBulletSpecId());
        if (bulletSpec == null) return;

        // 方向列表：对 01 子弹特殊处理为四个对角方向，其余使用策略默认方向
        // 针对 07 子弹：不要与人物朝向一致，使用组件默认 forward（世界向右）
        Point2D usedForward = fwd;
        if ("straight_07".equals(bulletSpec.getId())) {
            usedForward = forward.normalize();
        }

        ShooterContext context = new ShooterContext(entity, Bullet.Faction.PLAYER, usedForward);

        List<Point2D> dirs;
        if ("straight_01".equals(bulletSpec.getId())) {
            // 以面朝方向 fwd 为基，构造四向（与 fwd 垂直向量为 (fy, -fx)）
            Point2D right = new Point2D(usedForward.getY(), -usedForward.getX());
            Point2D f1 = usedForward.add(right).normalize();
            Point2D f2 = usedForward.subtract(right).normalize();
            Point2D f3 = usedForward.multiply(-1).add(right).normalize();
            Point2D f4 = usedForward.multiply(-1).subtract(right).normalize();
            dirs = java.util.Arrays.asList(f1, f2, f3, f4);
        } else {
            dirs = strategy.getDirections(context, spec);
        }

        double cx = entity.getCenter().getX();
        double cy = entity.getCenter().getY();

        for (Point2D dir : dirs) {
            Bullet bullet = BulletFactory.create(Bullet.Faction.PLAYER, dir, bulletSpec);
            if (bullet != null) {
                FXGL.getGameWorld().addEntity(bullet);
                Point2D nd = dir.normalize();
                // 偏移：确保子弹出生不与玩家重叠
                double offset = Math.max(entity.getWidth(), entity.getHeight()) * 0.8 + Math.max(bullet.getWidth(), bullet.getHeight()) * 0.8 + 20.0;
                double sx = cx + nd.getX() * offset - bullet.getWidth() / 2.0;
                double sy = cy + nd.getY() * offset - bullet.getHeight() / 2.0;
                // 对 02 子弹额外上移，修正“偏下”问题（再次上调）
                if ("straight_02".equals(bulletSpec.getId())) {
                    sy -= 20.0;
                }
                bullet.getTransformComponent().setPosition(sx, sy);
            }
        }
    }

    public void setAttackSpecId(String attackSpecId) { this.attackSpecId = attackSpecId; }
    public void setStrategy(AttackStrategy strategy) { this.strategy = strategy; }
    public void setForward(Point2D forward) { if (forward != null) this.forward = forward.normalize(); }
}


