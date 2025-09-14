package com.roguelike.entities;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.roguelike.core.GameState;
import com.roguelike.utils.RandomUtils;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public class EntityFactory implements com.almasb.fxgl.entity.EntityFactory {

    private static GameState gameState;
    private static boolean initialized;

    public static void setGameState(GameState state) {
        gameState = state;
        initialized = true;
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        Player player = new Player();
        player.setX(data.getX());
        player.setY(data.getY());
        if (gameState != null) {
            player.setGameState(gameState);
        }
        return player;
    }

    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        // 根据游戏时间计算敌人血量（3,5,7,9分钟时血量增加）
        int baseHP = 50;
        int expReward = 5;

        // 获取游戏时间（秒）（改用受控时间服务，避免首帧时间暴增）
        double gameTime = com.roguelike.core.TimeService.getSeconds();
        int minutes = (int) (gameTime / 60);

        // 在3,5,7,9分钟时血量增加
        if (minutes >= 3) baseHP += 20;
        if (minutes >= 5) baseHP += 30;
        if (minutes >= 7) baseHP += 40;
        if (minutes >= 9) baseHP += 50;

        // 经验值也相应增加
        expReward += minutes / 2;

        Enemy enemy = new Enemy(baseHP, expReward);

        // 在玩家附近随机一圈生成
        Entity player = getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof Player).findFirst().orElse(null);
        Point2D base = player != null ? player.getCenter() : new Point2D(getAppWidth() / 2.0, getAppHeight() / 2.0);
        double angle = Math.toRadians(RandomUtils.nextInt(0, 359));
        double radius = RandomUtils.nextInt(200, 400);
        enemy.setX(base.getX() + Math.cos(angle) * radius);
        enemy.setY(base.getY() + Math.sin(angle) * radius);

        // 移除固定寿命定时，避免暂停时仍继续；敌人由战斗中逻辑自然消亡
        return enemy;
    }
}


