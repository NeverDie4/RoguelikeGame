package com.roguelike.entities.config;

/**
 * 敌人生成配置类
 * 统一管理所有生成相关的参数
 */
public class SpawnConfig {
    
    // ========== 生成距离配置 ==========
    
    // 屏幕边缘生成（扩大一点）
    public static final double SCREEN_EDGE_MIN = 500.0;  // 500像素
    public static final double SCREEN_EDGE_MAX = 700.0;  // 700像素
    
    // 屏幕外生成（调整到真正屏幕外）
    public static final double SCREEN_OUT_MIN = 1000.0;  // 1000像素
    public static final double SCREEN_OUT_MAX = 1400.0;  // 1400像素
    
    // 远距离生成
    public static final double FAR_SPAWN_MIN = 1100.0;   // 1100像素
    public static final double FAR_SPAWN_MAX = 1600.0;   // 1600像素
    
    // ========== 预计算配置 ==========
    
    // 预计算范围（预留可调整数值）
    public static final double PRECOMPUTE_RANGE = 1600.0;  // 1600像素
    public static final double PRECOMPUTE_RANGE_EXTENDED = 2000.0;  // 扩展范围
    public static final double PRECOMPUTE_RANGE_MINIMAL = 1200.0;   // 最小范围
    
    // 缓存网格大小（预留可调整数值）
    public static final int CACHE_GRID_SIZE = 2000;      // 2000x2000像素
    public static final int CACHE_GRID_SIZE_LARGE = 2500; // 大缓存
    public static final int CACHE_GRID_SIZE_SMALL = 1500; // 小缓存
    
    // ========== 敌人尺寸分类配置 ==========
    
    // 小敌人（≤108像素）
    public static final int SMALL_ENEMY_STEP = 16;       // 16像素步长
    public static final double SMALL_ENEMY_SAFETY = 40.0; // 40像素安全距离
    public static final double SMALL_ENEMY_MAX_SIZE = 108.0; // 最大尺寸阈值（适配72x108）
    
    // 中敌人（≤162像素）
    public static final int MEDIUM_ENEMY_STEP = 24;      // 24像素步长
    public static final double MEDIUM_ENEMY_SAFETY = 60.0; // 60像素安全距离
    public static final double MEDIUM_ENEMY_MAX_SIZE = 162.0; // 最大尺寸阈值（适配108x162）
    
    // 大敌人（≥65像素）
    public static final int LARGE_ENEMY_STEP = 32;       // 32像素步长
    public static final double LARGE_ENEMY_SAFETY = 80.0; // 80像素安全距离
    
    // ========== 生成优先级配置 ==========
    
    // 生成位置分配比例
    public static final double SCREEN_OUT_PRIORITY = 0.70;    // 70%屏幕外生成
    public static final double SCREEN_EDGE_PRIORITY = 0.25;   // 25%屏幕边缘生成
    public static final double FAR_SPAWN_PRIORITY = 0.05;     // 5%远距离生成
    
    // ========== 更新间隔配置 ==========
    
    // 智能更新间隔（毫秒）
    public static final long UPDATE_INTERVAL_STATIC = 2000;   // 静止：2秒
    public static final long UPDATE_INTERVAL_SLOW = 1000;     // 慢速：1秒
    public static final long UPDATE_INTERVAL_MEDIUM = 500;    // 中速：0.5秒
    public static final long UPDATE_INTERVAL_FAST = 250;      // 快速：0.25秒
    
    // 移动距离阈值
    public static final double MOVEMENT_THRESHOLD_STATIC = 10.0;  // 静止阈值
    public static final double MOVEMENT_THRESHOLD_SLOW = 50.0;    // 慢速阈值
    public static final double MOVEMENT_THRESHOLD_MEDIUM = 100.0; // 中速阈值
    
    // ========== 缓存配置 ==========
    
    // 缓存持续时间（毫秒）
    public static final long CACHE_DURATION = 10000;     // 10秒
    public static final long CACHE_DURATION_SHORT = 5000; // 5秒（快速更新）
    public static final long CACHE_DURATION_LONG = 15000; // 15秒（慢速更新）
    
    // 位置占用持续时间（毫秒）
    public static final long POSITION_OCCUPIED_DURATION = 5000; // 5秒

    // ========== 时间缩放与批量配置 ==========
    
    // 基于时间的刷怪加速（分钟级指数衰减到最小系数）
    public static final double TIME_ACCELERATION_DECAY = 0.90;   // 每分钟间隔乘以0.9
    public static final double TIME_ACCELERATION_MIN = 0.80;     // 下限：原间隔的15%
    
    // ========== 性能配置 ==========
    
    // 最大并发生成任务数
    public static final int MAX_CONCURRENT_SPAWNS = 3;
    
    // 每批最大敌人数
    public static final int MAX_ENEMIES_PER_BATCH = 2;
    
    // 最大生成尝试次数
    public static final int DEFAULT_MAX_ATTEMPTS = 100;
    public static final int FALLBACK_MAX_ATTEMPTS = 30;

    // ========== 批量与屏内距离（依赖于性能常量，需放在其后） ==========
    // 批量上限（普通模式随时间提升，Boss模式单独上限）
    public static final int MAX_BATCH_BASE = MAX_ENEMIES_PER_BATCH; // 基准批量
    public static final int MAX_BATCH_CAP = 10;                     // 普通模式批量上限
    public static final int BOSS_MAX_BATCH_CAP = 20;                // Boss模式批量上限
    
    // 屏内刷新时与玩家的最小距离
    public static final double SCREEN_IN_PLAYER_MIN_DIST = 140.0;   // 像素
    public static final double SCREEN_IN_PLAYER_MIN_DIST_BOSS = 180.0; // Boss时更大
    
    // ========== 工具方法 ==========
    
    /**
     * 根据敌人尺寸获取预计算步长
     */
    public static int getStepSizeForEnemy(double enemySize) {
        if (enemySize <= SMALL_ENEMY_MAX_SIZE) {
            return SMALL_ENEMY_STEP;
        } else if (enemySize <= MEDIUM_ENEMY_MAX_SIZE) {
            return MEDIUM_ENEMY_STEP;
        } else {
            return LARGE_ENEMY_STEP;
        }
    }
    
    /**
     * 根据敌人尺寸获取安全距离
     */
    public static double getSafetyDistanceForEnemy(double enemySize) {
        if (enemySize <= SMALL_ENEMY_MAX_SIZE) {
            return SMALL_ENEMY_SAFETY;
        } else if (enemySize <= MEDIUM_ENEMY_MAX_SIZE) {
            return MEDIUM_ENEMY_SAFETY;
        } else {
            return LARGE_ENEMY_SAFETY;
        }
    }
    
    /**
     * 根据玩家移动距离计算更新间隔
     */
    public static long calculateUpdateInterval(double movementDistance) {
        if (movementDistance < MOVEMENT_THRESHOLD_STATIC) {
            return UPDATE_INTERVAL_STATIC;
        } else if (movementDistance < MOVEMENT_THRESHOLD_SLOW) {
            return UPDATE_INTERVAL_SLOW;
        } else if (movementDistance < MOVEMENT_THRESHOLD_MEDIUM) {
            return UPDATE_INTERVAL_MEDIUM;
        } else {
            return UPDATE_INTERVAL_FAST;
        }
    }
    
    /**
     * 基于游戏运行时间(秒)返回一个刷怪间隔缩放因子，时间越久越快（值越小）。
     */
    public static double getTimeScaleFactor(double seconds) {
        int minutes = (int) Math.max(0, seconds / 60.0);
        double factor = Math.pow(TIME_ACCELERATION_DECAY, minutes);
        return Math.max(TIME_ACCELERATION_MIN, factor);
    }
    
    /**
     * 计算随时间增长的批量大小（向上取整但不超过cap）。
     */
    public static int getDynamicBatchSize(double seconds, int base, int cap) {
        int minutes = (int) Math.max(0, seconds / 60.0);
        int bonus = Math.max(0, minutes / 2); // 每2分钟+1
        int size = base + bonus;
        if (size > cap) size = cap;
        if (size < 1) size = 1;
        return size;
    }
    
    /**
     * 获取生成优先级配置
     */
    public static double[] getSpawnPriorities() {
        return new double[]{SCREEN_OUT_PRIORITY, SCREEN_EDGE_PRIORITY, FAR_SPAWN_PRIORITY};
    }
    
    /**
     * 获取生成距离范围
     */
    public static double[][] getSpawnDistanceRanges() {
        return new double[][]{
            {SCREEN_OUT_MIN, SCREEN_OUT_MAX},
            {SCREEN_EDGE_MIN, SCREEN_EDGE_MAX},
            {FAR_SPAWN_MIN, FAR_SPAWN_MAX}
        };
    }
}
