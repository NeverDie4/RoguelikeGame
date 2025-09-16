package com.roguelike.core;

import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;
import com.roguelike.physics.EntityCollisionDetector.CollisionResult;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 碰撞事件批处理器
 * 专门处理碰撞相关的事件，通过批量处理提高性能
 */
public class CollisionEventBatcher {
    
    // 碰撞事件类型
    public enum CollisionEventType {
        PLAYER_ENEMY_COLLISION,
        BULLET_ENEMY_COLLISION,
        BULLET_PLAYER_COLLISION,
        ENEMY_ENEMY_COLLISION,
        PLAYER_HIT_WALL,
        ENEMY_HIT_WALL
    }
    
    // 碰撞事件数据
    public static class CollisionEventData {
        private final CollisionEventType type;
        private final Entity entity1;
        private final Entity entity2;
        private final CollisionResult collisionResult;
        private final long timestamp;
        
        public CollisionEventData(CollisionEventType type, Entity entity1, Entity entity2, CollisionResult collisionResult) {
            this.type = type;
            this.entity1 = entity1;
            this.entity2 = entity2;
            this.collisionResult = collisionResult;
            this.timestamp = System.currentTimeMillis();
        }
        
        public CollisionEventType getType() {
            return type;
        }
        
        public Entity getEntity1() {
            return entity1;
        }
        
        public Entity getEntity2() {
            return entity2;
        }
        
        public CollisionResult getCollisionResult() {
            return collisionResult;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    // 事件队列
    private final Queue<CollisionEventData> eventQueue;
    
    // 批处理配置
    private final int maxBatchSize;
    private final long maxBatchTimeMs;
    private final boolean enableCollisionDeduplication;
    
    // 碰撞去重映射（避免同一对实体重复碰撞）
    private final Map<String, Long> collisionDeduplicationMap;
    private static final long DEDUPLICATION_WINDOW_MS = 50; // 50ms去重窗口
    
    // 性能统计
    private long totalCollisionEvents = 0;
    private long totalBatchesProcessed = 0;
    private long totalProcessingTimeMs = 0;
    private long deduplicatedEvents = 0;
    
    // 调试模式
    private boolean debugMode = false;
    
    public CollisionEventBatcher() {
        this(30, 8, true); // 默认配置：最大批次30，最大时间8ms，启用去重
    }
    
    public CollisionEventBatcher(int maxBatchSize, long maxBatchTimeMs, boolean enableCollisionDeduplication) {
        this.maxBatchSize = maxBatchSize;
        this.maxBatchTimeMs = maxBatchTimeMs;
        this.enableCollisionDeduplication = enableCollisionDeduplication;
        
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.collisionDeduplicationMap = new HashMap<>();
    }
    
    /**
     * 添加碰撞事件到批处理队列
     */
    public void addCollisionEvent(CollisionEventType type, Entity entity1, Entity entity2, CollisionResult collisionResult) {
        // 碰撞去重检查
        if (enableCollisionDeduplication && isDuplicateCollision(entity1, entity2)) {
            deduplicatedEvents++;
            if (debugMode) {
                System.out.println("🔄 碰撞事件去重: " + type + " (" + getEntityType(entity1) + " vs " + getEntityType(entity2) + ")");
            }
            return;
        }
        
        CollisionEventData event = new CollisionEventData(type, entity1, entity2, collisionResult);
        eventQueue.offer(event);
        
        if (debugMode) {
            System.out.println("💥 碰撞事件添加到批处理队列: " + type + " (" + getEntityType(entity1) + " vs " + getEntityType(entity2) + ")");
        }
    }
    
    /**
     * 处理所有待处理的碰撞事件批次
     */
    public void processCollisionBatches() {
        if (eventQueue.isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        
        // 批量处理事件
        while (!eventQueue.isEmpty() && processedCount < maxBatchSize) {
            if (System.currentTimeMillis() - startTime >= maxBatchTimeMs) {
                break; // 时间限制
            }
            
            CollisionEventData event = eventQueue.poll();
            if (event != null) {
                processCollisionEvent(event);
                processedCount++;
            }
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // 更新统计信息
        totalCollisionEvents += processedCount;
        totalBatchesProcessed++;
        totalProcessingTimeMs += processingTime;
        
        if (debugMode && processedCount > 0) {
            System.out.println("⚡ 碰撞批处理完成: " + processedCount + " 个事件, 耗时: " + processingTime + "ms");
        }
    }
    
    /**
     * 处理单个碰撞事件
     */
    private void processCollisionEvent(CollisionEventData event) {
        try {
            switch (event.getType()) {
                case PLAYER_ENEMY_COLLISION:
                    handlePlayerEnemyCollision(event);
                    break;
                case BULLET_ENEMY_COLLISION:
                    handleBulletEnemyCollision(event);
                    break;
                case BULLET_PLAYER_COLLISION:
                    handleBulletPlayerCollision(event);
                    break;
                case ENEMY_ENEMY_COLLISION:
                    handleEnemyEnemyCollision(event);
                    break;
                case PLAYER_HIT_WALL:
                    handlePlayerHitWall(event);
                    break;
                case ENEMY_HIT_WALL:
                    handleEnemyHitWall(event);
                    break;
            }
        } catch (Exception e) {
            System.err.println("❌ 碰撞事件处理错误: " + event.getType() + " - " + e.getMessage());
        }
    }
    
    /**
     * 处理玩家与敌人碰撞
     */
    private void handlePlayerEnemyCollision(CollisionEventData event) {
        Player player = (Player) event.getEntity1();
        Enemy enemy = (Enemy) event.getEntity2();
        
        // 确保玩家是第一个实体
        if (!(event.getEntity1() instanceof Player)) {
            player = (Player) event.getEntity2();
            enemy = (Enemy) event.getEntity1();
        }
        
        if (player != null && enemy != null && enemy.isAlive()) {
            // 发布碰撞事件
            GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_ENEMY_COLLISION));
        }
    }
    
    /**
     * 处理子弹与敌人碰撞
     */
    private void handleBulletEnemyCollision(CollisionEventData event) {
        Bullet bullet = (Bullet) event.getEntity1();
        Enemy enemy = (Enemy) event.getEntity2();
        
        // 确保子弹是第一个实体
        if (!(event.getEntity1() instanceof Bullet)) {
            bullet = (Bullet) event.getEntity2();
            enemy = (Enemy) event.getEntity1();
        }
        
        if (bullet != null && enemy != null && bullet.isActive() && enemy.isAlive()) {
            // 发布碰撞事件
            GameEvent.post(new GameEvent(GameEvent.Type.BULLET_ENEMY_COLLISION));
        }
    }
    
    /**
     * 处理子弹与玩家碰撞
     */
    private void handleBulletPlayerCollision(CollisionEventData event) {
        Bullet bullet = (Bullet) event.getEntity1();
        Player player = (Player) event.getEntity2();
        
        // 确保子弹是第一个实体
        if (!(event.getEntity1() instanceof Bullet)) {
            bullet = (Bullet) event.getEntity2();
            player = (Player) event.getEntity1();
        }
        
        if (bullet != null && player != null && bullet.isActive()) {
            // 发布碰撞事件
            GameEvent.post(new GameEvent(GameEvent.Type.BULLET_PLAYER_COLLISION));
        }
    }
    
    /**
     * 处理敌人与敌人碰撞
     */
    private void handleEnemyEnemyCollision(CollisionEventData event) {
        Enemy enemy1 = (Enemy) event.getEntity1();
        Enemy enemy2 = (Enemy) event.getEntity2();
        
        if (enemy1 != null && enemy2 != null && enemy1.isAlive() && enemy2.isAlive()) {
            // 发布碰撞事件
            GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_ENEMY_COLLISION));
        }
    }
    
    /**
     * 处理玩家撞墙
     */
    private void handlePlayerHitWall(CollisionEventData event) {
        Player player = (Player) event.getEntity1();
        if (player != null) {
            // 发布碰撞事件
            GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_HIT_WALL));
        }
    }
    
    /**
     * 处理敌人撞墙
     */
    private void handleEnemyHitWall(CollisionEventData event) {
        Enemy enemy = (Enemy) event.getEntity1();
        if (enemy != null && enemy.isAlive()) {
            // 发布碰撞事件
            GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_HIT_WALL));
        }
    }
    
    /**
     * 检查是否为重复碰撞
     */
    private boolean isDuplicateCollision(Entity entity1, Entity entity2) {
        String key = generateCollisionKey(entity1, entity2);
        long currentTime = System.currentTimeMillis();
        
        if (collisionDeduplicationMap.containsKey(key)) {
            long lastCollisionTime = collisionDeduplicationMap.get(key);
            if (currentTime - lastCollisionTime < DEDUPLICATION_WINDOW_MS) {
                return true; // 重复碰撞
            }
        }
        
        // 记录碰撞时间
        collisionDeduplicationMap.put(key, currentTime);
        return false;
    }
    
    /**
     * 生成碰撞键值
     */
    private String generateCollisionKey(Entity entity1, Entity entity2) {
        int id1 = entity1.hashCode();
        int id2 = entity2.hashCode();
        
        if (id1 < id2) {
            return id1 + "_" + id2;
        } else {
            return id2 + "_" + id1;
        }
    }
    
    /**
     * 获取实体类型字符串
     */
    private String getEntityType(Entity entity) {
        if (entity instanceof Player) {
            return "Player";
        } else if (entity instanceof Enemy) {
            return "Enemy";
        } else if (entity instanceof Bullet) {
            return "Bullet";
        } else {
            return "Unknown";
        }
    }
    
    /**
     * 清理过期的去重记录
     */
    public void cleanupExpiredDeduplicationRecords() {
        long currentTime = System.currentTimeMillis();
        collisionDeduplicationMap.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > DEDUPLICATION_WINDOW_MS * 2
        );
    }
    
    /**
     * 获取待处理事件数量
     */
    public int getPendingEventCount() {
        return eventQueue.size();
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        if (totalBatchesProcessed == 0) {
            return "碰撞批处理统计: 暂无数据";
        }
        
        double avgEventsPerBatch = (double) totalCollisionEvents / totalBatchesProcessed;
        double avgProcessingTime = (double) totalProcessingTimeMs / totalBatchesProcessed;
        
        return String.format(
            "碰撞批处理统计:\n" +
            "  总碰撞事件数: %d\n" +
            "  总批次数: %d\n" +
            "  平均每批次事件数: %.1f\n" +
            "  平均处理时间: %.2fms\n" +
            "  去重事件数: %d\n" +
            "  待处理事件数: %d",
            totalCollisionEvents, totalBatchesProcessed, avgEventsPerBatch, 
            avgProcessingTime, deduplicatedEvents, getPendingEventCount()
        );
    }
    
    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalCollisionEvents = 0;
        totalBatchesProcessed = 0;
        totalProcessingTimeMs = 0;
        deduplicatedEvents = 0;
    }
    
    /**
     * 清空所有待处理事件
     */
    public void clearAllEvents() {
        eventQueue.clear();
    }
}
