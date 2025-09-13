package com.roguelike.core;

import com.roguelike.entities.Enemy;
import com.roguelike.physics.EntityCollisionDetector.CollisionResult;

import java.util.List;

/**
 * 事件批处理管理器
 * 统一管理所有批处理系统，提供统一的接口
 */
public class EventBatchingManager {
    
    // 批处理系统组件
    private final EventBatchingSystem eventBatchingSystem;
    private final CollisionEventBatcher collisionEventBatcher;
    private final AIUpdateBatcher aiUpdateBatcher;
    
    // 配置参数
    private final boolean enableEventBatching;
    private final boolean enableCollisionBatching;
    private final boolean enableAIBatching;
    
    // 性能统计
    private long totalProcessingTimeMs = 0;
    private long totalBatchesProcessed = 0;
    
    // 调试模式
    private boolean debugMode = false;
    
    public EventBatchingManager() {
        this(true, true, true); // 默认启用所有批处理
    }
    
    public EventBatchingManager(boolean enableEventBatching, boolean enableCollisionBatching, boolean enableAIBatching) {
        this.enableEventBatching = enableEventBatching;
        this.enableCollisionBatching = enableCollisionBatching;
        this.enableAIBatching = enableAIBatching;
        
        // 初始化批处理系统
        this.eventBatchingSystem = new EventBatchingSystem();
        this.collisionEventBatcher = new CollisionEventBatcher();
        this.aiUpdateBatcher = new AIUpdateBatcher();
    }
    
    /**
     * 处理所有批处理系统
     */
    public void processAllBatches() {
        long startTime = System.currentTimeMillis();
        
        // 处理碰撞事件批次
        if (enableCollisionBatching) {
            collisionEventBatcher.processCollisionBatches();
        }
        
        // 处理AI更新批次
        if (enableAIBatching) {
            aiUpdateBatcher.processAIUpdateBatches();
        }
        
        // 处理通用事件批次
        if (enableEventBatching) {
            eventBatchingSystem.processBatches();
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // 更新统计信息
        totalProcessingTimeMs += processingTime;
        totalBatchesProcessed++;
        
        if (debugMode && processingTime > 0) {
            System.out.println("🎯 批处理管理器完成: 总耗时 " + processingTime + "ms");
        }
    }
    
    /**
     * 添加碰撞事件
     */
    public void addCollisionEvent(CollisionEventBatcher.CollisionEventType type, 
                                 com.almasb.fxgl.entity.Entity entity1, 
                                 com.almasb.fxgl.entity.Entity entity2, 
                                 CollisionResult collisionResult) {
        if (enableCollisionBatching) {
            collisionEventBatcher.addCollisionEvent(type, entity1, entity2, collisionResult);
        } else {
            // 直接处理事件
            processCollisionEventDirectly(type, entity1, entity2);
        }
    }
    
    /**
     * 添加AI更新任务
     */
    public void addAIUpdateTask(Enemy enemy, double deltaTime) {
        if (enableAIBatching) {
            aiUpdateBatcher.addAIUpdateTask(enemy, deltaTime);
        } else {
            // 直接更新AI
            if (enemy != null && enemy.isAlive()) {
                enemy.updateAI(deltaTime);
            }
        }
    }
    
    /**
     * 批量添加AI更新任务
     */
    public void addAIUpdateTasks(List<Enemy> enemies, double deltaTime) {
        if (enableAIBatching) {
            aiUpdateBatcher.addAIUpdateTasks(enemies, deltaTime);
        } else {
            // 直接更新所有AI
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive()) {
                    enemy.updateAI(deltaTime);
                }
            }
        }
    }
    
    /**
     * 添加通用事件
     */
    public void addEvent(GameEvent.Type eventType, Object data, EventBatchingSystem.EventPriority priority) {
        if (enableEventBatching) {
            eventBatchingSystem.addEvent(eventType, data, priority);
        } else {
            // 直接发布事件
            GameEvent.post(new GameEvent(eventType));
        }
    }
    
    /**
     * 注册事件处理器
     */
    public void registerEventHandler(GameEvent.Type eventType, java.util.function.Consumer<GameEvent> handler) {
        if (enableEventBatching) {
            eventBatchingSystem.registerHandler(eventType, handler);
        }
    }
    
    /**
     * 直接处理碰撞事件（非批处理模式）
     */
    private void processCollisionEventDirectly(CollisionEventBatcher.CollisionEventType type, 
                                              com.almasb.fxgl.entity.Entity entity1, 
                                              com.almasb.fxgl.entity.Entity entity2) {
        switch (type) {
            case PLAYER_ENEMY_COLLISION:
                GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_ENEMY_COLLISION));
                break;
            case BULLET_ENEMY_COLLISION:
                GameEvent.post(new GameEvent(GameEvent.Type.BULLET_ENEMY_COLLISION));
                break;
            case BULLET_PLAYER_COLLISION:
                GameEvent.post(new GameEvent(GameEvent.Type.BULLET_PLAYER_COLLISION));
                break;
            case ENEMY_ENEMY_COLLISION:
                GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_ENEMY_COLLISION));
                break;
            case PLAYER_HIT_WALL:
                GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_HIT_WALL));
                break;
            case ENEMY_HIT_WALL:
                GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_HIT_WALL));
                break;
        }
    }
    
    /**
     * 清理过期的记录
     */
    public void cleanupExpiredRecords() {
        if (enableCollisionBatching) {
            collisionEventBatcher.cleanupExpiredDeduplicationRecords();
        }
    }
    
    /**
     * 获取待处理事件总数
     */
    public int getTotalPendingEvents() {
        int total = 0;
        
        if (enableEventBatching) {
            total += eventBatchingSystem.getPendingEventCount();
        }
        
        if (enableCollisionBatching) {
            total += collisionEventBatcher.getPendingEventCount();
        }
        
        if (enableAIBatching) {
            total += aiUpdateBatcher.getPendingTaskCount();
        }
        
        return total;
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== 事件批处理管理器统计 ===\n");
        
        if (enableEventBatching) {
            stats.append(eventBatchingSystem.getPerformanceStats()).append("\n\n");
        }
        
        if (enableCollisionBatching) {
            stats.append(collisionEventBatcher.getPerformanceStats()).append("\n\n");
        }
        
        if (enableAIBatching) {
            stats.append(aiUpdateBatcher.getPerformanceStats()).append("\n\n");
        }
        
        if (totalBatchesProcessed > 0) {
            double avgProcessingTime = (double) totalProcessingTimeMs / totalBatchesProcessed;
            stats.append(String.format("管理器总统计:\n" +
                "  总批次数: %d\n" +
                "  平均处理时间: %.2fms\n" +
                "  待处理事件总数: %d",
                totalBatchesProcessed, avgProcessingTime, getTotalPendingEvents()));
        }
        
        return stats.toString();
    }
    
    /**
     * 获取配置信息
     */
    public String getConfigInfo() {
        return String.format(
            "批处理管理器配置:\n" +
            "  事件批处理: %s\n" +
            "  碰撞批处理: %s\n" +
            "  AI批处理: %s\n" +
            "  调试模式: %s",
            enableEventBatching ? "启用" : "禁用",
            enableCollisionBatching ? "启用" : "禁用",
            enableAIBatching ? "启用" : "禁用",
            debugMode ? "启用" : "禁用"
        );
    }
    
    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        
        if (enableEventBatching) {
            eventBatchingSystem.setDebugMode(debugMode);
        }
        
        if (enableCollisionBatching) {
            collisionEventBatcher.setDebugMode(debugMode);
        }
        
        if (enableAIBatching) {
            aiUpdateBatcher.setDebugMode(debugMode);
        }
    }
    
    /**
     * 重置所有统计信息
     */
    public void resetAllStats() {
        totalProcessingTimeMs = 0;
        totalBatchesProcessed = 0;
        
        if (enableEventBatching) {
            eventBatchingSystem.resetStats();
        }
        
        if (enableCollisionBatching) {
            collisionEventBatcher.resetStats();
        }
        
        if (enableAIBatching) {
            aiUpdateBatcher.resetStats();
        }
    }
    
    /**
     * 清空所有待处理事件
     */
    public void clearAllPendingEvents() {
        if (enableEventBatching) {
            eventBatchingSystem.clearAllEvents();
        }
        
        if (enableCollisionBatching) {
            collisionEventBatcher.clearAllEvents();
        }
        
        if (enableAIBatching) {
            aiUpdateBatcher.clearAllTasks();
        }
    }
    
    // Getter方法
    public EventBatchingSystem getEventBatchingSystem() {
        return eventBatchingSystem;
    }
    
    public CollisionEventBatcher getCollisionEventBatcher() {
        return collisionEventBatcher;
    }
    
    public AIUpdateBatcher getAIUpdateBatcher() {
        return aiUpdateBatcher;
    }
    
    public boolean isEventBatchingEnabled() {
        return enableEventBatching;
    }
    
    public boolean isCollisionBatchingEnabled() {
        return enableCollisionBatching;
    }
    
    public boolean isAIBatchingEnabled() {
        return enableAIBatching;
    }
}
