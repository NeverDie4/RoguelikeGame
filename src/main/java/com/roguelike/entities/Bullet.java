package com.roguelike.entities;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import com.roguelike.entities.components.LinearMovementComponent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import com.roguelike.entities.configs.BulletSpec;


/**
 * 子弹基类，完全基于EntityBase的碰撞检测机制
 */
public abstract class Bullet extends EntityBase {

    public enum Faction {
        PLAYER,  // 玩家阵营
        ENEMY    // 敌人阵营
    }

    protected Faction faction;
    protected int damage;
    protected boolean piercing;
    protected double speed;
    // 元数据
    protected String id = "unknown";
    protected String displayName = "Unknown Bullet";
    
    // 穿透子弹伤害判定时间间隔
    private double lastDamageTime = 0.0;
    private static final double PIERCING_DAMAGE_INTERVAL = 0.1; // 100ms间隔

    public Bullet(Faction faction, int damage, boolean piercing, double speed) {
        this.faction = faction;
        this.damage = damage;
        this.piercing = piercing;
        this.speed = speed;

        // 使用EntityBase的现有方法设置碰撞
        getBoundingBoxComponent().addHitBox(new HitBox(BoundingShape.box(8, 8)));
        addComponent(new CollidableComponent(true));
        // 在构造方法中添加 ViewComponent（恢复与原逻辑一致）
        //addAndGetComponent(new ViewComponent());
        // 默认大小
        setSize(8, 8);
    }

    /**
     * 应用配置模板到当前子弹实例（不处理动画，动画交由具体子类或组件）。
     */
    public void applySpec(BulletSpec spec) {
        if (spec == null) return;
        this.id = spec.getId() != null ? spec.getId() : this.id;
        this.displayName = spec.getDisplayName() != null ? spec.getDisplayName() : this.displayName;
        this.damage = Math.max(0, spec.getBaseDamage());
        this.piercing = spec.isPiercing();
        this.speed = Math.max(0, spec.getBaseSpeed());

        if (spec.getRadius() > 0) {
            setSize(spec.getRadius() * 2, spec.getRadius() * 2);
        } else if (spec.getWidth() > 0 && spec.getHeight() > 0) {
            setSize(spec.getWidth(), spec.getHeight());
        }

        if (spec.getLifetimeSeconds() > 0) {
            setLifeTime(spec.getLifetimeSeconds());
        }
    }

    /**
     * 初始化视图 (使用EntityBase的ViewComponent)
     */
    protected void initView(Shape shape, Color color) {
        shape.setFill(color);
        getViewComponent().addChild(shape);
    }

    /**
     * 初始化运动 (使用FXGL现有ProjectileComponent)
     */
    protected void initMovement(Point2D direction) {
        // 使用自定义直线运动组件，完全去除默认射程限制
        addComponent(new LinearMovementComponent(direction, speed));
    }

    /**
     * 碰撞处理回调 (由EntityBase的碰撞检测触发)
     */
    public void onCollisionBegin(Entity other) {
        if (faction == Faction.PLAYER && other instanceof Enemy) {
            handlePlayerBulletHitEnemy((Enemy) other);
        } else if (faction == Faction.ENEMY && other instanceof Player) {
            handleEnemyBulletHitPlayer((Player) other);
        }
    }

    private void handlePlayerBulletHitEnemy(Enemy enemy) {
        // 对于穿透子弹，检查伤害判定时间间隔
        if (piercing) {
            double currentTime = com.roguelike.core.TimeService.getSeconds();
            if (currentTime - lastDamageTime < PIERCING_DAMAGE_INTERVAL) {
                return; // 跳过这次伤害判定
            }
            lastDamageTime = currentTime;
        }
        
        // 统一通过受伤入口处理伤害与死亡
        enemy.takeDamage(damage);

        if (!piercing) {
            removeFromWorld();
        }
    }

    private void handleEnemyBulletHitPlayer(Player player) {
        // 对于穿透子弹，检查伤害判定时间间隔
        if (piercing) {
            double currentTime = com.roguelike.core.TimeService.getSeconds();
            if (currentTime - lastDamageTime < PIERCING_DAMAGE_INTERVAL) {
                return; // 跳过这次伤害判定
            }
            lastDamageTime = currentTime;
        }
        
        // 使用Player现有的damage方法
        getGameState().damagePlayer(damage);

        // 使用现有的GameEvent系统
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_HURT));

        if (!piercing) {
            removeFromWorld();
        }
    }

    /**
     * 设置生命周期 (使用FXGL现有计时器)
     */
    protected void setLifeTime(double seconds) {
        // 将寿命基于 TimeService 的受控时间推进
        final double expireAt = com.roguelike.core.TimeService.getSeconds() + Math.max(0.0, seconds);
        FXGL.run(() -> {
            if (com.roguelike.core.TimeService.getSeconds() >= expireAt) {
                if (isActive()) removeFromWorld();
            }
        }, Duration.millis(50));
    }

    /**
     * 公共接口：应用寿命（秒）。
     */
    public void applyLifetime(double seconds) {
        if (seconds > 0) {
            setLifeTime(seconds);
        }
    }

    // Getter方法
    public Faction getFaction() {
        return faction;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isPiercing() {
        return piercing;
    }

    public double getSpeed() {
        return speed;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Setter方法
    public void setDamage(int damage) {
        this.damage = Math.max(0, damage);
    }

    public void setPiercing(boolean piercing) {
        this.piercing = piercing;
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(0, speed);
    }

    public void setMeta(String id, String displayName) {
        if (id != null && !id.isEmpty()) this.id = id;
        if (displayName != null && !displayName.isEmpty()) this.displayName = displayName;
    }

    /**
     * 检查子弹是否与指定实体属于同一阵营
     */
    public boolean isSameFaction(Entity other) {
        if (other instanceof Bullet) {
            return this.faction == ((Bullet) other).faction;
        }
        if (other instanceof Player) {
            return this.faction == Faction.PLAYER;
        }
        if (other instanceof Enemy) {
            return this.faction == Faction.ENEMY;
        }
        return false;
    }

    /**
     * 检查子弹是否应该与指定实体发生碰撞
     */
    public boolean shouldCollideWith(Entity other) {
        return !isSameFaction(other);
    }

    /**
     * 强制销毁子弹
     */
    public void destroy() {
        if (isActive()) {
            removeFromWorld();
        }
    }

    /**
     * 获取子弹的移动方向
     */
    public Point2D getDirection() {
        LinearMovementComponent lm = getComponentOptional(LinearMovementComponent.class).orElse(null);
        if (lm != null) return lm.getDirection();
        return new Point2D(1, 0);
    }

    /**
     * 设置子弹的移动方向
     */
    public void setDirection(Point2D direction) {
        LinearMovementComponent lm = getComponentOptional(LinearMovementComponent.class).orElse(null);
        if (lm != null && direction != null) lm.setDirection(direction);
    }

    private GameState getGameState() {
        return FXGL.geto("gameState");
    }
}