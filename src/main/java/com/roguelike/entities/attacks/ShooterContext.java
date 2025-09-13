package com.roguelike.entities.attacks;

import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.Bullet;
import javafx.geometry.Point2D;

/**
 * 发射上下文：提供发射者位置/朝向/目标等信息。
 */
public class ShooterContext {
    private final Entity shooter;
    private final Bullet.Faction faction;
    private final Point2D forward; // 归一化朝向

    public ShooterContext(Entity shooter, Bullet.Faction faction, Point2D forward) {
        this.shooter = shooter;
        this.faction = faction;
        this.forward = forward == null ? new Point2D(1, 0) : forward.normalize();
    }

    public Entity getShooter() { return shooter; }
    public Bullet.Faction getFaction() { return faction; }
    public Point2D getForward() { return forward; }
}


