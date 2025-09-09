package com.roguelike.entities;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.ProjectileComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.entity.components.TypeComponent;
import com.almasb.fxgl.texture.Texture;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public class Player extends EntityBase {

    private final double speed = 200;
    private GameState gameState;

    public Player() {
        Rectangle view = new Rectangle(32, 32, Color.DODGERBLUE);
        getViewComponent().addChild(view);
        addComponent(new CollidableComponent(true));
        setSize(32, 32);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void move(double dx, double dy) {
        translate(dx, dy);
        // 移除重复的事件发送，避免在输入处理中重复触发
    }

    public void attack() { //这个函数有问题，后面新建一个子弹类用于区分友方子弹和敌方子弹，再传入entityBuilder()里
        // 简单攻击：发射一个向右的投射体
        Entity bullet = entityBuilder()
                // 从玩家位置出发（基于玩家中心调整）
                .at(getCenter().subtract(0, 2))
                .viewWithBBox(new Rectangle(8, 4, Color.ORANGE))
                .with(new CollidableComponent(true))
                .with(new ProjectileComponent(new Point2D(1, 0), 500))
                .buildAndAttach();
    }

    public void takeDamage(int damage) {
        if (gameState != null) {
            gameState.damagePlayer(damage);
            GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_HURT));

            if (gameState.getPlayerHP() <= 0) {
                onDeath();
            }
        }
    }

    public void onDeath() {
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_DEATH));
        // 可以在这里添加死亡逻辑，比如显示游戏结束界面
    }

    public Point2D getPositionVec() {
        return getPosition();
    }

    public static class Types {
        public static final String PLAYER = "PLAYER";
    }
}


