package com.roguelike.core;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体缓存管理器
 * 负责缓存游戏中的实体引用，避免每帧重复查找
 * 针对30+实体的场景进行优化
 */
public class EntityCacheManager {
    private static EntityCacheManager instance;
    
    // 缓存的实体引用
    private Player cachedPlayer;
    private List<Enemy> cachedEnemies = new ArrayList<>();
    private List<Bullet> cachedBullets = new ArrayList<>();
    
    // 缓存更新控制
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 33; // 30FPS更新频率，约33ms
    
    // 性能统计
    private int cacheHitCount = 0;
    private int cacheMissCount = 0;
    
    private EntityCacheManager() {
        // 私有构造函数，单例模式
    }
    
    /**
     * 获取单例实例
     */
    public static EntityCacheManager getInstance() {
        if (instance == null) {
            instance = new EntityCacheManager();
        }
        return instance;
    }
    
    /**
     * 更新缓存
     * 使用节流机制，避免过于频繁的更新
     */
    public void updateCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            cacheHitCount++;
            return; // 节流更新
        }
        lastUpdateTime = currentTime;
        cacheMissCount++;
        
        // 批量更新，减少Stream操作开销
        List<Entity> allEntities = FXGL.getGameWorld().getEntitiesByType();
        
        // 清空旧缓存
        cachedEnemies.clear();
        cachedBullets.clear();
        cachedPlayer = null;
        
        // 遍历所有实体，分类缓存
        for (Entity entity : allEntities) {
            if (entity instanceof Player) {
                cachedPlayer = (Player) entity;
            } else if (entity instanceof Enemy) {
                Enemy enemy = (Enemy) entity;
                if (enemy.isAlive()) {
                    cachedEnemies.add(enemy);
                }
            } else if (entity instanceof Bullet) {
                Bullet bullet = (Bullet) entity;
                if (bullet.isActive()) {
                    cachedBullets.add(bullet);
                }
            }
        }
        
        // 调试信息：每100次更新输出一次统计
        if (cacheMissCount % 100 == 0) {
            System.out.println("🔄 实体缓存更新: 玩家=" + (cachedPlayer != null ? "存在" : "无") + 
                             ", 敌人=" + cachedEnemies.size() + 
                             ", 子弹=" + cachedBullets.size());
        }
    }
    
    /**
     * 强制更新缓存（用于特殊情况）
     */
    public void forceUpdateCache() {
        lastUpdateTime = 0; // 重置时间，强制更新
        updateCache();
    }
    
    /**
     * 获取缓存的玩家实体
     */
    public Player getPlayer() {
        if (cachedPlayer == null || !cachedPlayer.isActive()) {
            // 如果缓存的玩家无效，尝试更新缓存
            updateCache();
        }
        return cachedPlayer;
    }
    
    /**
     * 获取缓存的敌人实体列表
     */
    public List<Enemy> getEnemies() {
        return new ArrayList<>(cachedEnemies); // 返回副本，避免外部修改
    }
    
    /**
     * 获取缓存的子弹实体列表
     */
    public List<Bullet> getBullets() {
        return new ArrayList<>(cachedBullets); // 返回副本，避免外部修改
    }
    
    /**
     * 获取活跃的敌人数量
     */
    public int getEnemyCount() {
        return cachedEnemies.size();
    }
    
    /**
     * 获取活跃的子弹数量
     */
    public int getBulletCount() {
        return cachedBullets.size();
    }
    
    /**
     * 检查缓存是否有效
     */
    public boolean isCacheValid() {
        return cachedPlayer != null && cachedPlayer.isActive();
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        cachedPlayer = null;
        cachedEnemies.clear();
        cachedBullets.clear();
        lastUpdateTime = 0;
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        int totalRequests = cacheHitCount + cacheMissCount;
        double hitRate = totalRequests > 0 ? (double) cacheHitCount / totalRequests * 100 : 0;
        
        return String.format(
            "实体缓存统计:\n" +
            "  缓存命中率: %.1f%% (%d/%d)\n" +
            "  敌人数量: %d\n" +
            "  子弹数量: %d\n" +
            "  缓存有效性: %s",
            hitRate, cacheHitCount, totalRequests,
            getEnemyCount(), getBulletCount(),
            isCacheValid() ? "有效" : "无效"
        );
    }
    
    /**
     * 重置性能统计
     */
    public void resetStats() {
        cacheHitCount = 0;
        cacheMissCount = 0;
    }
}
