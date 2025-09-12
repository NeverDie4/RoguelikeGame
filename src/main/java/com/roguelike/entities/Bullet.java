package com.roguelike.entities;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.ProjectileComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

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
        // 正确地使用方向 + 速度 构造投射体，确保对角方向生效
        addComponent(new ProjectileComponent(direction.normalize(), speed));
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
        // 使用Enemy现有的onDeath方法
        //enemy.onDeath(getGameState());

        // 使用现有的GameEvent系统
        //GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_DEATH));

        if (!piercing) {
            removeFromWorld();
        }
    }


    private void handleEnemyBulletHitPlayer(Player player) {
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
        runOnce(() -> {
            if (isActive()) {
                removeFromWorld();
            }
        }, Duration.seconds(seconds));
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
     * 设置子弹的激活状态
     */
    public void setActive(boolean active) {
        if (!active && isActive()) {
            removeFromWorld();
        }
    }

    /**
     * 获取子弹的移动方向
     */
    public Point2D getDirection() {
        ProjectileComponent projectile = getComponent(ProjectileComponent.class);
        if (projectile != null) {
            return projectile.getVelocity().normalize();
        }
        return new Point2D(1, 0); // 默认向右
    }

    /**
     * 设置子弹的移动方向
     */
    public void setDirection(Point2D direction) {
        ProjectileComponent projectile = getComponent(ProjectileComponent.class);
        if (projectile != null) {
            Point2D velocity = direction.normalize().multiply(speed);
            // 移除旧组件并添加新组件
            removeComponent(ProjectileComponent.class);
            addComponent(new ProjectileComponent(velocity, 0));
        }
    }

    private GameState getGameState() {
        return FXGL.geto("gameState");
    }
}