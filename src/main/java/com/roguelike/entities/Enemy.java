package com.roguelike.entities;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Enemy extends EntityBase {

    private double speed = 80;

    public Enemy() {
        getViewComponent().addChild(new Rectangle(28, 28, Color.CRIMSON));
        addComponent(new CollidableComponent(true));
        setSize(28, 28);
      initenemyhpbar();

    }

    private void initenemyhpbar(){


    }

    public void onUpdate(double tpf) {
        // 简单 AI：朝向玩家移动
        Entity player = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .findFirst().orElse(null);
        if (player != null) {
            Point2D dir = player.getCenter().subtract(getCenter());
            if (dir.magnitude() > 1e-3) {
                dir = dir.normalize().multiply(speed * tpf);
                translate(dir);
            }
        }
    }

    public void onDeath(GameState gameState) {
        gameState.addScore(10);
        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_DEATH));
        removeFromWorld();
    }
}


