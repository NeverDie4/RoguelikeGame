package com.roguelike.entities;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.roguelike.core.GameState;
import com.roguelike.utils.RandomUtils;
import com.roguelike.entities.config.EnemyConfig;
import com.roguelike.entities.config.EnemyConfigManager;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public class EntityFactory implements com.almasb.fxgl.entity.EntityFactory {

    private static GameState gameState;
    private static boolean initialized;
    private static InfiniteMapEnemySpawnManager infiniteMapSpawnManager;
    private static BackgroundEnemySpawnManager backgroundSpawnManager;
    private static com.roguelike.physics.OptimizedMovementValidator movementValidator;
    private static com.roguelike.utils.AdaptivePathfinder adaptivePathfinder;

    public static void setGameState(GameState state) {
        gameState = state;
        initialized = true;
    }

    public static void setInfiniteMapSpawnManager(InfiniteMapEnemySpawnManager spawnManager) {
        infiniteMapSpawnManager = spawnManager;
    }

    public static void setBackgroundSpawnManager(BackgroundEnemySpawnManager spawnManager) {
        backgroundSpawnManager = spawnManager;
    }

    public static void setMovementValidator(com.roguelike.physics.OptimizedMovementValidator validator) {
        movementValidator = validator;
    }

    public static void setAdaptivePathfinder(com.roguelike.utils.AdaptivePathfinder pathfinder) {
        adaptivePathfinder = pathfinder;
    }

    /**
     * 获取自适应路径寻找器
     */
    public static com.roguelike.utils.AdaptivePathfinder getAdaptivePathfinder() {
        return adaptivePathfinder;
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
        // 获取敌人配置管理器
        EnemyConfigManager configManager = EnemyConfigManager.getInstance();

        // 如果配置管理器未初始化，先初始化
        if (!configManager.isInitialized()) {
            configManager.initialize();
        }

        // 随机选择一个敌人配置
        EnemyConfig config = configManager.getRandomEnemyConfig();

        Enemy enemy;
        if (config != null) {
            // 使用配置创建敌人
            enemy = new Enemy(config);

            // 根据游戏时间调整属性（可选）
            adjustEnemyStatsByGameTime(enemy, config);
        } else {
            // 回退到原有逻辑
            System.err.println("⚠️ 无法获取敌人配置，使用默认配置");
            int baseHP = 50;
            int expReward = 5;

            // 获取游戏时间（秒）
            double gameTime = com.roguelike.core.TimeService.getSeconds();
            int minutes = (int) (gameTime / 60);

            // 在3,5,7,9分钟时血量增加
            if (minutes >= 3) baseHP += 20;
            if (minutes >= 5) baseHP += 30;
            if (minutes >= 7) baseHP += 40;
            if (minutes >= 9) baseHP += 50;

            // 经验值也相应增加
            expReward += minutes / 2;

            enemy = new Enemy(baseHP, expReward);
        }

        // 设置位置（由后台生成管理器传入）
        enemy.setX(data.getX());
        enemy.setY(data.getY());

        // 设置移动验证器（如果可用）
        if (movementValidator != null) {
            enemy.setMovementValidator(movementValidator);
        }

        // 设置路径寻找器（如果可用）
        if (adaptivePathfinder != null) {
            enemy.setAdaptivePathfinder(adaptivePathfinder);
        }

        // 在寻路器设置后立即初始化目标位置，确保新生成的敌人能立即开始寻路
        enemy.initializeTargetPosition();

        // 移除固定寿命定时，避免暂停时仍继续；敌人由战斗中逻辑自然消亡
        return enemy;
    }

    @Spawns("particle")
    public Entity newParticle(SpawnData data) {
        // 创建粒子实体（粒子组件会在ParticleEffectManager中添加）
        Entity particle = new Entity();
        particle.setX(data.getX());
        particle.setY(data.getY());
        return particle;
    }

    /**
     * 根据游戏时间调整敌人属性
     * @param enemy 敌人实体
     * @param config 敌人配置
     */
    private void adjustEnemyStatsByGameTime(Enemy enemy, EnemyConfig config) {
        if (config == null) return;

        // 获取游戏时间（秒）
        double gameTime = com.roguelike.core.TimeService.getSeconds();
        int minutes = (int) (gameTime / 60);

        // 根据游戏时间调整血量（可选）
        if (minutes >= 3) {
            // 可以在这里调整敌人的属性，比如增加血量
            // 由于Enemy类目前没有提供setter方法，这里先记录日志
        }
    }
}


