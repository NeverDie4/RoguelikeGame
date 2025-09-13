package com.roguelike.entities;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.roguelike.core.GameState;
import com.roguelike.utils.RandomUtils;
import javafx.geometry.Point2D;

import static com.almasb.fxgl.dsl.FXGL.*;

public class EntityFactory implements com.almasb.fxgl.entity.EntityFactory {

    private static GameState gameState;
    private static boolean initialized;
    private static InfiniteMapEnemySpawnManager infiniteMapSpawnManager;

    public static void setGameState(GameState state) {
        gameState = state;
        initialized = true;
    }

    public static void setInfiniteMapSpawnManager(InfiniteMapEnemySpawnManager spawnManager) {
        infiniteMapSpawnManager = spawnManager;
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

        // 使用新的无限地图生成系统
        if (infiniteMapSpawnManager != null) {
            Entity player = getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player).findFirst().orElse(null);
            
            if (player != null) {
                Point2D playerPos = player.getCenter();
                
                // 使用预定的敌人尺寸 48x64
                double enemyWidth = 48.0;
                double enemyHeight = 64.0;
                
                Point2D spawnPos = infiniteMapSpawnManager.generateEnemySpawnPosition(
                    playerPos, 
                    enemyWidth, 
                    enemyHeight,
                    200.0, // 最小距离
                    400.0  // 最大距离
                );
                
                if (spawnPos != null) {
                    enemy.setX(spawnPos.getX());
                    enemy.setY(spawnPos.getY());
                    return enemy;
                }
            }
        }
        
        // 回退到原有逻辑（如果新系统失败或未启用）
        Entity player = getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof Player).findFirst().orElse(null);
        Point2D base = player != null ? player.getCenter() : new Point2D(getAppWidth() / 2.0, getAppHeight() / 2.0);
        double angle = Math.toRadians(RandomUtils.nextInt(0, 359));
        double radius = RandomUtils.nextInt(200, 400);
        enemy.setX(base.getX() + Math.cos(angle) * radius);
        enemy.setY(base.getY() + Math.sin(angle) * radius);

        // 敌人不再自动死亡，只能通过玩家攻击或碰撞死亡
        return enemy;
    }
}


