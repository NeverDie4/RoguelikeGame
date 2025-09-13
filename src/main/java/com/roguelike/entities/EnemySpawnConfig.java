package com.roguelike.entities;

import java.util.Map;

/**
 * 敌人生成配置参数
 */
public class EnemySpawnConfig {
    
    // 生成距离配置
    public static double MIN_SPAWN_DISTANCE = 200.0;
    public static double MAX_SPAWN_DISTANCE = 400.0;
    
    // 生成尝试配置
    public static int MAX_SPAWN_ATTEMPTS = 100;
    
    // 敌人间距配置
    public static double MIN_ENEMY_DISTANCE = 50.0;
    public static long POSITION_OCCUPIED_DURATION = 5000; // 毫秒
    
    // 缓存配置
    public static double CACHE_UPDATE_INTERVAL = 2.0; // 秒
    
    // 调试配置
    public static boolean DEBUG_MODE = false;
    public static boolean ENABLE_STATISTICS = true;
    
    /**
     * 应用配置
     * @param config 配置映射
     */
    public static void applyConfig(Map<String, Object> config) {
        MIN_SPAWN_DISTANCE = (Double) config.getOrDefault("minSpawnDistance", MIN_SPAWN_DISTANCE);
        MAX_SPAWN_DISTANCE = (Double) config.getOrDefault("maxSpawnDistance", MAX_SPAWN_DISTANCE);
        MAX_SPAWN_ATTEMPTS = (Integer) config.getOrDefault("maxSpawnAttempts", MAX_SPAWN_ATTEMPTS);
        MIN_ENEMY_DISTANCE = (Double) config.getOrDefault("minEnemyDistance", MIN_ENEMY_DISTANCE);
        POSITION_OCCUPIED_DURATION = (Long) config.getOrDefault("positionOccupiedDuration", POSITION_OCCUPIED_DURATION);
        CACHE_UPDATE_INTERVAL = (Double) config.getOrDefault("cacheUpdateInterval", CACHE_UPDATE_INTERVAL);
        DEBUG_MODE = (Boolean) config.getOrDefault("debugMode", DEBUG_MODE);
        ENABLE_STATISTICS = (Boolean) config.getOrDefault("enableStatistics", ENABLE_STATISTICS);
        
        System.out.println("⚙️ 敌人生成配置已应用");
        printCurrentConfig();
    }
    
    /**
     * 打印当前配置
     */
    public static void printCurrentConfig() {
        System.out.println("📋 当前敌人生成配置:");
        System.out.println("   最小生成距离: " + MIN_SPAWN_DISTANCE);
        System.out.println("   最大生成距离: " + MAX_SPAWN_DISTANCE);
        System.out.println("   最大尝试次数: " + MAX_SPAWN_ATTEMPTS);
        System.out.println("   最小敌人距离: " + MIN_ENEMY_DISTANCE);
        System.out.println("   位置占用时间: " + POSITION_OCCUPIED_DURATION + "ms");
        System.out.println("   缓存更新间隔: " + CACHE_UPDATE_INTERVAL + "s");
        System.out.println("   调试模式: " + (DEBUG_MODE ? "开启" : "关闭"));
        System.out.println("   统计功能: " + (ENABLE_STATISTICS ? "开启" : "关闭"));
    }
    
    /**
     * 重置为默认配置
     */
    public static void resetToDefaults() {
        MIN_SPAWN_DISTANCE = 200.0;
        MAX_SPAWN_DISTANCE = 400.0;
        MAX_SPAWN_ATTEMPTS = 100;
        MIN_ENEMY_DISTANCE = 50.0;
        POSITION_OCCUPIED_DURATION = 5000;
        CACHE_UPDATE_INTERVAL = 2.0;
        DEBUG_MODE = false;
        ENABLE_STATISTICS = true;
        
        System.out.println("🔄 敌人生成配置已重置为默认值");
    }
    
    /**
     * 获取配置摘要
     */
    public static String getConfigSummary() {
        return String.format(
            "敌人生成配置: 距离[%.0f-%.0f] 尝试[%d] 间距[%.0f] 调试[%s]",
            MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE, MAX_SPAWN_ATTEMPTS, 
            MIN_ENEMY_DISTANCE, DEBUG_MODE ? "开启" : "关闭"
        );
    }
}

