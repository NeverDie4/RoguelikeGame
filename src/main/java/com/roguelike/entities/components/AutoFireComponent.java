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
    
    // 轮换索引（动态列表：包含 weapon01；若已获得 weapon02，则追加 weapon02）
    private int currentBulletIndex = 0;
    private static int lastWeapon02Count = 0; // 保留兼容字段
    private int currentSpawnedOrbiting = 0;

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
        if (com.roguelike.core.TimeService.isPaused()) return;
        // 以 single_01 的配置为基准，适当减慢发射频率（例如放大为 0.8s）
        // 动态决定可用的发射配置：始终包含 weapon01；当环绕数量>0时加入 weapon02
        java.util.List<String> ids = new java.util.ArrayList<>();
        ids.add("weapon01");
        if (com.roguelike.entities.weapons.WeaponManager.getWeapon02OrbitCount() > 0) {
            ids.add("weapon02");
        }
        if (com.roguelike.entities.weapons.WeaponManager.getWeapon03Level() > 0) {
            ids.add("weapon03");
        }
        if (com.roguelike.entities.weapons.WeaponManager.getWeapon04Level() > 0) {
            ids.add("weapon04");
        }
        if (com.roguelike.entities.weapons.WeaponManager.getWeapon05Level() > 0) {
            ids.add("weapon05");
        }
        if (com.roguelike.entities.weapons.WeaponManager.getWeapon06Level() > 0) {
            ids.add("weapon06");
        }
        if (com.roguelike.entities.weapons.WeaponManager.getWeapon07Level() > 0) {
            ids.add("weapon07");
        }
        if (com.roguelike.entities.weapons.WeaponManager.getWeapon08Level() > 0) {
            ids.add("weapon08");
        }
        if (ids.isEmpty()) return;
        int idx = currentBulletIndex % ids.size();
        AttackSpec base = AttackRegistry.get(ids.get(idx));
        if (base == null) return;

        double now = TimeService.getSeconds();
        double interval = Math.max(0.05, base.getFireIntervalSeconds());
        if (now - lastFireSeconds >= interval) {
            lastFireSeconds = now;

            // 交替发射当前索引的子弹（基于动态可用列表）
            String currentId = ids.get(idx);
            AttackSpec spec = AttackRegistry.get(currentId);
            if (spec != null) {
                // 播放发射音效：W01/W03/W05/W07/W08
                if ("weapon01".equals(currentId) || "weapon03".equals(currentId) ||
                    "weapon05".equals(currentId) || "weapon07".equals(currentId) ||
                    "weapon08".equals(currentId)) {
                    com.roguelike.ui.SoundService.playFire();
                }
                fire(spec);
            }

            currentBulletIndex = (idx + 1) % ids.size();
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

        // 方向列表：武器01使用 WeaponManager 中的方向定义
        List<Point2D> dirs;
        if ("straight_01".equals(bulletSpec.getId())) {
            dirs = com.roguelike.entities.weapons.WeaponManager.getWeapon01Directions();
        } else if ("straight_02".equals(bulletSpec.getId())) {
            // 未获得时不生成（LV=0 表示未获得）
            int want = com.roguelike.entities.weapons.WeaponManager.getWeapon02OrbitCount();
            if (want > 0) {
                syncOrbitingWeapon02(bulletSpec, want);
            }
            return;
        } else if ("straight_03".equals(bulletSpec.getId())) {
            // 使用武器03的方向集合发射
            java.util.List<Point2D> w3dirs = com.roguelike.entities.weapons.WeaponManager.getWeapon03Directions();
            if (w3dirs == null || w3dirs.isEmpty()) return;
            double cx = entity.getCenter().getX();
            double cy = entity.getCenter().getY();
            for (Point2D dir : w3dirs) {
                Bullet b = BulletFactory.create(Bullet.Faction.PLAYER, dir, bulletSpec);
                if (b != null) {
                    FXGL.getGameWorld().addEntity(b);
                    Point2D nd = dir.normalize();
                    double offset = Math.max(entity.getWidth(), entity.getHeight()) * 0.8 + Math.max(b.getWidth(), b.getHeight()) * 0.8 + 20.0;
                    double sx = cx + nd.getX() * offset - b.getWidth() / 2.0;
                    double sy = cy + nd.getY() * offset - b.getHeight() / 2.0;
                    b.getTransformComponent().setPosition(sx, sy);
                }
            }
            return;
        } else if ("weapon04".equals(spec.getId())) {
            // 确保光环存在且已绑定玩家（只创建一次）
            ensureAuraExists();
            return;
        } else if ("straight_05".equals(bulletSpec.getId())) {
            java.util.List<Point2D> dirs05 = com.roguelike.entities.weapons.WeaponManager.getWeapon05Directions();
            if (dirs05 == null || dirs05.isEmpty()) return;
            double cx = entity.getCenter().getX();
            double cy = entity.getCenter().getY();
            for (Point2D dir : dirs05) {
                Bullet b = BulletFactory.create(Bullet.Faction.PLAYER, dir, bulletSpec);
                if (b != null) {
                    FXGL.getGameWorld().addEntity(b);
                    Point2D nd = dir.normalize();
                    double offset = Math.max(entity.getWidth(), entity.getHeight()) * 0.8 + Math.max(b.getWidth(), b.getHeight()) * 0.8 + 20.0;
                    double sx = cx + nd.getX() * offset - b.getWidth() / 2.0;
                    double sy = cy + nd.getY() * offset - b.getHeight() / 2.0;
                    b.getTransformComponent().setPosition(sx, sy);
                }
            }
            return;
        } else if ("weapon06".equals(spec.getId())) {
            spawnLightningStrikes();
            return;
        } else if ("weapon07".equals(spec.getId())) {
            java.util.List<Point2D> w7dirs = com.roguelike.entities.weapons.WeaponManager.getWeapon07Directions();
            if (w7dirs == null || w7dirs.isEmpty()) return;
            double cx = entity.getCenter().getX();
            double cy = entity.getCenter().getY();
            for (Point2D dir : w7dirs) {
                Bullet b = BulletFactory.create(Bullet.Faction.PLAYER, dir, bulletSpec);
                if (b != null) {
                    FXGL.getGameWorld().addEntity(b);
                    Point2D nd = dir.normalize();
                    double offset = Math.max(entity.getWidth(), entity.getHeight()) * 0.8 + Math.max(b.getWidth(), b.getHeight()) * 0.8 + 20.0;
                    double sx = cx + nd.getX() * offset - b.getWidth() / 2.0;
                    double sy = cy + nd.getY() * offset - b.getHeight() / 2.0;
                    b.getTransformComponent().setPosition(sx, sy);
                }
            }
            return;
        } else if ("weapon08".equals(spec.getId())) {
            maintainDrills();
            return;
        } else {
            dirs = strategy.getDirections(new ShooterContext(entity, Bullet.Faction.PLAYER, fwd), spec);
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

    private void spawnOrbitingBullets(BulletSpec bulletSpec, int count) {
        com.almasb.fxgl.entity.Entity p = entity;
        if (!(p instanceof com.roguelike.entities.Player)) return;
        com.roguelike.entities.Player player = (com.roguelike.entities.Player) p;

        // 基础参数：2 秒一圈 -> 角速度 180 度/秒
        double periodSeconds = 2.0;
        double angularSpeed = 360.0 / periodSeconds; // 180.0
        double radius = 70.0;

        // 选择 02 的动画资源
        String animBase = "bullets/02";
        int frames = 4; // 02 目录下帧数
        double frameDur = 0.07;

        for (int i = 0; i < Math.max(1, count); i++) {
            double initialAngle = i * (360.0 / Math.max(1, count));
            com.roguelike.entities.bullets.OrbitingBullet ob =
                    new com.roguelike.entities.bullets.OrbitingBullet(
                            Bullet.Faction.PLAYER,
                            new Point2D(1, 0),
                            bulletSpec.getBaseDamage(),
                            true,
                            0.0,
                            radius,
                            angularSpeed,
                            initialAngle,
                            player,
                            animBase,
                            frames,
                            frameDur,
                            0.0 // 无限持续
                    );
            FXGL.getGameWorld().addEntity(ob);
            ob.getTransformComponent().setPosition(player.getCenter().getX(), player.getCenter().getY());
            // 应用视觉缩放（仅渲染，不改变碰撞盒）
            ob.getComponentOptional(com.roguelike.entities.components.BulletAnimationComponent.class)
                    .ifPresent(c -> c.setVisualScale(bulletSpec.getVisualScale()));
        }
    }

    // 保持武器02的环绕子弹数量，避免重复生成
    private void syncOrbitingWeapon02(BulletSpec bulletSpec, int wantCount) {
        if (wantCount <= 0) wantCount = 1;
        if (currentSpawnedOrbiting != wantCount) {
            // 数量变化：先清空旧的，再生成新的
            removeExistingOrbiting();
            spawnOrbitingBullets(bulletSpec, wantCount);
            currentSpawnedOrbiting = wantCount;
            lastWeapon02Count = wantCount;
        }
        // 若数量未变化，不重复生成，以免越堆越多
    }

    private void removeExistingOrbiting() {
        // 粗略移除：查找 OrbitingBullet 并移除（仅玩家所属的）
        var list = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.bullets.OrbitingBullet)
                .toList();
        for (var e : list) {
            e.removeFromWorld();
        }
    }

    // 玩家光环生成（只生成一次）
    private boolean auraCreated = false;
    private void ensureAuraExists() {
        if (auraCreated) return;
        com.almasb.fxgl.entity.Entity p = entity;
        if (!(p instanceof com.roguelike.entities.Player)) return;
        com.roguelike.entities.Player player = (com.roguelike.entities.Player) p;
        // 创建一个附着到玩家中心的光环实体（与玩家分离，便于独立渲染与定位）
        com.almasb.fxgl.entity.Entity aura = new com.roguelike.entities.EntityBase();
        AuraDamageComponent comp = new AuraDamageComponent();
        comp.setTarget(player);
        aura.addComponent(comp);
        aura.getTransformComponent().setPosition(player.getCenter().getX(), player.getCenter().getY());
        FXGL.getGameWorld().addEntity(aura);
        auraCreated = true;
    }

    // 08 维持在场钻头数量，不间断补发
    private final java.util.List<com.almasb.fxgl.entity.Entity> activeDrills = new java.util.ArrayList<>();
    private void maintainDrills() {
        int want = com.roguelike.entities.weapons.WeaponManager.getWeapon08Count();
        cleanupDrills();
        while (activeDrills.size() < want) {
            spawnOneDrill();
        }
    }

    private void cleanupDrills() {
        activeDrills.removeIf(e -> e == null || !e.isActive());
    }

    private void spawnOneDrill() {
        com.almasb.fxgl.entity.Entity p = entity;
        if (!(p instanceof com.roguelike.entities.Player)) return;
        com.roguelike.entities.Player player = (com.roguelike.entities.Player) p;

        com.roguelike.entities.configs.BulletSpec bulletSpec = com.roguelike.entities.configs.BulletRegistry.get("straight_08");
        if (bulletSpec == null) return;

        // 随机方向（360度）
        java.util.Random rnd = new java.util.Random();
        double ang = rnd.nextDouble() * Math.PI * 2;
        Point2D dir = new Point2D(Math.cos(ang), Math.sin(ang));

        com.roguelike.entities.Bullet b = com.roguelike.entities.factory.BulletFactory.create(com.roguelike.entities.Bullet.Faction.PLAYER, dir, bulletSpec);
        if (b == null) return;
        FXGL.getGameWorld().addEntity(b);
        double cx = player.getCenter().getX();
        double cy = player.getCenter().getY();
        Point2D nd = dir.normalize();
        double offset = Math.max(player.getWidth(), player.getHeight()) * 0.8 + Math.max(b.getWidth(), b.getHeight()) * 0.8 + 20.0;
        b.getTransformComponent().setPosition(cx + nd.getX() * offset - b.getWidth() / 2.0, cy + nd.getY() * offset - b.getHeight() / 2.0);

        // 移除直线运动与越界销毁，只依赖反弹组件移动+寿命控制
        b.removeComponent(com.roguelike.entities.components.LinearMovementComponent.class);
        b.removeComponent(com.roguelike.entities.components.OutOfViewportDestroyComponent.class);

        // 反弹+寿命（5s）。把速度大小取自 BulletSpec.baseSpeed
        double speed = Math.max(50.0, bulletSpec.getBaseSpeed());
        Point2D vel = nd.multiply(speed);
        b.addComponent(new ReboundDrillComponent(vel));
        double life = com.roguelike.entities.weapons.WeaponManager.getWeapon08Lifetime();
        b.applyLifetime(Math.max(0.1, life));

        // 按方向旋转动画以匹配发射角度（以贴图向右为基准）
        b.getComponentOptional(BulletAnimationComponent.class).ifPresent(ac -> {
            Point2D d = nd;
            double deg = Math.toDegrees(Math.atan2(d.getY(), d.getX()));
            double base = 360.0 - deg; // 右=0，上=90，左=180，下=270

            double visualDeg = base;
            boolean isHorizontal = Math.abs(d.getY()) < 1e-6;
            boolean isVertical = Math.abs(d.getX()) < 1e-6;
            if (!isHorizontal && !isVertical) {
                if (d.getX() < 0 && d.getY() < 0) { // 左上
                    visualDeg = base + 90.0; // 顺时针90
                } else if (d.getX() < 0 && d.getY() > 0) { // 左下
                    visualDeg = base - 90.0; // 逆时针90
                } else if (d.getX() > 0 && d.getY() > 0) { // 右下
                    visualDeg = base + 90.0; // 顺时针90
                } else if (d.getX() > 0 && d.getY() < 0) { // 右上
                    visualDeg = base - 90.0; // 逆时针90
                }
            } else if (isVertical) {
                // 上/下方向额外加 180° 以修正颠倒
                visualDeg = (base + 180.0);
            }

            while (visualDeg < 0) visualDeg += 360.0;
            while (visualDeg >= 360.0) visualDeg -= 360.0;
            ac.setVisualRotationDegrees(visualDeg);
        });

        activeDrills.add(b);
    }

    // 06 落雷：选择不重叠落点并生成一次性落雷
    private void spawnLightningStrikes() {
        int count = com.roguelike.entities.weapons.WeaponManager.getWeapon06Count();
        if (count <= 0) return;
        double radius = com.roguelike.entities.weapons.WeaponManager.getWeapon06Radius();
        int dmg = com.roguelike.entities.weapons.WeaponManager.getWeapon06Damage();

        // 收集敌人位置
        java.util.List<com.roguelike.entities.Enemy> enemies = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .map(e -> (com.roguelike.entities.Enemy) e)
                .toList();
        if (enemies.isEmpty()) return;

        java.util.List<javafx.geometry.Point2D> centers = new java.util.ArrayList<>();
        java.util.Random rnd = new java.util.Random();

        // 简单去重选择：
        for (int i = 0; i < enemies.size() && centers.size() < count; i++) {
            var en = enemies.get(rnd.nextInt(enemies.size()));
            javafx.geometry.Point2D c = en.getCenter();
            boolean ok = true;
            for (var p : centers) {
                if (p.distance(c) < radius * 0.9) { ok = false; break; }
            }
            if (!ok) continue;
            centers.add(c);
        }
        // 若数量仍不足，基于已有点做小偏移补齐
        while (centers.size() < count && !centers.isEmpty()) {
            var base = centers.get(rnd.nextInt(centers.size()));
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = radius;
            javafx.geometry.Point2D c2 = new javafx.geometry.Point2D(
                    base.getX() + Math.cos(ang) * dist,
                    base.getY() + Math.sin(ang) * dist
            );
            boolean ok = true;
            for (var p : centers) {
                if (p.distance(c2) < radius * 0.9) { ok = false; break; }
            }
            if (ok) centers.add(c2); else break;
        }

        // 生成落雷实体
        for (var c : centers) {
            com.almasb.fxgl.entity.Entity strike = new com.roguelike.entities.EntityBase();
            strike.addComponent(new LightningStrikeComponent(radius, dmg, 1.0));
            strike.getTransformComponent().setPosition(c.getX(), c.getY());
            FXGL.getGameWorld().addEntity(strike);
        }
    }

    public void setAttackSpecId(String attackSpecId) { this.attackSpecId = attackSpecId; }
    public void setStrategy(AttackStrategy strategy) { this.strategy = strategy; }
    public void setForward(Point2D forward) { if (forward != null) this.forward = forward.normalize(); }
}


