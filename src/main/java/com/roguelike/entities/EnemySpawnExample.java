package com.roguelike.entities;

import javafx.geometry.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * 敌人生成系统使用示例
 * 展示如何使用新的敌人生成功能
 */
public class EnemySpawnExample {
    
    private EnemySpawnManager spawnManager;
    
    public EnemySpawnExample(EnemySpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }
    
    /**
     * 示例1：基本敌人生成
     */
    public void basicEnemySpawn() {
        Point2D playerPosition = new Point2D(100, 100);
        
        // 生成敌人生成位置
        Point2D spawnPosition = spawnManager.generateEnemySpawnPosition(
            playerPosition, 
            200.0,  // 最小距离
            400.0   // 最大距离
        );
        
        if (spawnPosition != null) {
            System.out.println("✅ 敌人生成位置: " + spawnPosition);
            // 在这里创建敌人实体
        } else {
            System.out.println("❌ 无法找到合适的生成位置");
        }
    }
    
    /**
     * 示例2：自定义生成参数
     */
    public void customEnemySpawn() {
        Point2D playerPosition = new Point2D(200, 200);
        
        // 使用自定义参数生成位置
        Point2D spawnPosition = spawnManager.generateEnemySpawnPosition(
            playerPosition,
            150.0,  // 更小的最小距离
            300.0,  // 更小的最大距离
            50      // 更少的尝试次数
        );
        
        if (spawnPosition != null) {
            System.out.println("✅ 自定义敌人生成位置: " + spawnPosition);
        }
    }
    
    /**
     * 示例3：配置参数使用
     */
    public void configExample() {
        // 创建配置映射
        Map<String, Object> config = new HashMap<>();
        config.put("minSpawnDistance", 180.0);
        config.put("maxSpawnDistance", 350.0);
        config.put("maxSpawnAttempts", 80);
        config.put("minEnemyDistance", 60.0);
        config.put("debugMode", true);
        
        // 应用配置
        EnemySpawnConfig.applyConfig(config);
        
        // 现在生成敌人会使用新的配置
        Point2D playerPosition = new Point2D(300, 300);
        Point2D spawnPosition = spawnManager.generateEnemySpawnPosition(playerPosition, 0, 0);
        
        if (spawnPosition != null) {
            System.out.println("✅ 配置化敌人生成位置: " + spawnPosition);
        }
    }
    
    /**
     * 示例4：调试信息获取
     */
    public void debugExample() {
        // 启用调试模式
        spawnManager.setDebugMode(true);
        
        // 生成一些敌人
        Point2D playerPosition = new Point2D(400, 400);
        for (int i = 0; i < 5; i++) {
            Point2D spawnPosition = spawnManager.generateEnemySpawnPosition(playerPosition, 0, 0);
            if (spawnPosition != null) {
                System.out.println("生成敌人 " + (i + 1) + ": " + spawnPosition);
            }
        }
        
        // 获取调试信息
        System.out.println(spawnManager.getDebugInfo());
        
        // 重置统计
        spawnManager.resetStatistics();
    }
    
    /**
     * 示例5：性能测试
     */
    public void performanceTest() {
        Point2D playerPosition = new Point2D(500, 500);
        int testCount = 100;
        
        long startTime = System.currentTimeMillis();
        
        int successCount = 0;
        for (int i = 0; i < testCount; i++) {
            Point2D spawnPosition = spawnManager.generateEnemySpawnPosition(playerPosition, 0, 0);
            if (spawnPosition != null) {
                successCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("性能测试结果:");
        System.out.println("  测试次数: " + testCount);
        System.out.println("  成功次数: " + successCount);
        System.out.println("  成功率: " + (successCount * 100.0 / testCount) + "%");
        System.out.println("  总耗时: " + duration + "ms");
        System.out.println("  平均耗时: " + (duration / (double) testCount) + "ms/次");
    }
}

