package com.roguelike.core;

import com.roguelike.entities.Enemy;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AI更新批处理器
 * 通过分批处理AI更新来提高性能，避免每帧更新所有敌人
 */
public class AIUpdateBatcher {
    
    // AI更新任务
    public static class AIUpdateTask {
        private final Enemy enemy;
        private final double deltaTime;
        private final long timestamp;
        private final int priority;
        
        public AIUpdateTask(Enemy enemy, double deltaTime, int priority) {
            this.enemy = enemy;
            this.deltaTime = deltaTime;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Enemy getEnemy() {
            return enemy;
        }
        
        public double getDeltaTime() {
            return deltaTime;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    // AI更新优先级
    public enum AIPriority {
        HIGH(0),    // 高优先级：靠近玩家的敌人
        NORMAL(1),  // 普通优先级：中等距离的敌人
        LOW(2);     // 低优先级：远离玩家的敌人
        
        private final int level;
        
        AIPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    // 更新队列
    private final Queue<AIUpdateTask> updateQueue;
    
    // 批处理配置
    private final int maxBatchSize;
    private final long maxBatchTimeMs;
    private final boolean enablePriorityProcessing;
    private final boolean enableDistanceBasedPriority;
    
    // 距离阈值配置
    private static final double HIGH_PRIORITY_DISTANCE = 200.0;   // 高优先级距离
    private static final double NORMAL_PRIORITY_DISTANCE = 400.0; // 普通优先级距离
    
    // 性能统计
    private long totalAIUpdates = 0;
    private long totalBatchesProcessed = 0;
    private long totalProcessingTimeMs = 0;
    private long skippedUpdates = 0;
    
    // 调试模式
    private boolean debugMode = false;
    
    public AIUpdateBatcher() {
        this(20, 8, true, true); // 默认配置：最大批次20，最大时间8ms，启用优先级和距离处理
    }
    
    public AIUpdateBatcher(int maxBatchSize, long maxBatchTimeMs, boolean enablePriorityProcessing, boolean enableDistanceBasedPriority) {
        this.maxBatchSize = maxBatchSize;
        this.maxBatchTimeMs = maxBatchTimeMs;
        this.enablePriorityProcessing = enablePriorityProcessing;
        this.enableDistanceBasedPriority = enableDistanceBasedPriority;
        
        this.updateQueue = new ConcurrentLinkedQueue<>();
    }
    
    /**
     * 添加AI更新任务到批处理队列
     */
    public void addAIUpdateTask(Enemy enemy, double deltaTime) {
        if (enemy == null || !enemy.isAlive()) {
            skippedUpdates++;
            return;
        }
        
        int priority = calculatePriority(enemy);
        AIUpdateTask task = new AIUpdateTask(enemy, deltaTime, priority);
        updateQueue.offer(task);
        
        if (debugMode) {
            System.out.println("🤖 AI更新任务添加到批处理队列: " + enemy.getClass().getSimpleName() + " (优先级: " + priority + ")");
        }
    }
    
    /**
     * 批量添加AI更新任务
     */
    public void addAIUpdateTasks(List<Enemy> enemies, double deltaTime) {
        for (Enemy enemy : enemies) {
            addAIUpdateTask(enemy, deltaTime);
        }
    }
    
    /**
     * 处理所有待处理的AI更新批次
     */
    public void processAIUpdateBatches() {
        if (updateQueue.isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        
        if (enablePriorityProcessing) {
            // 按优先级处理
            processedCount = processUpdatesByPriority(startTime);
        } else {
            // 按时间处理
            processedCount = processUpdatesByTime(startTime);
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // 更新统计信息
        totalAIUpdates += processedCount;
        totalBatchesProcessed++;
        totalProcessingTimeMs += processingTime;
        
        if (debugMode && processedCount > 0) {
            System.out.println("⚡ AI批处理完成: " + processedCount + " 个更新, 耗时: " + processingTime + "ms");
        }
    }
    
    /**
     * 按优先级处理AI更新
     */
    private int processUpdatesByPriority(long startTime) {
        int processedCount = 0;
        
        // 收集所有任务并按优先级排序
        List<AIUpdateTask> allTasks = new ArrayList<>();
        while (!updateQueue.isEmpty()) {
            AIUpdateTask task = updateQueue.poll();
            if (task != null) {
                allTasks.add(task);
            }
        }
        
        // 按优先级排序
        allTasks.sort(Comparator.comparingInt(AIUpdateTask::getPriority));
        
        // 处理任务直到达到限制
        for (AIUpdateTask task : allTasks) {
            if (processedCount >= maxBatchSize) break;
            if (System.currentTimeMillis() - startTime >= maxBatchTimeMs) break;
            
            processAIUpdateTask(task);
            processedCount++;
        }
        
        // 将未处理的任务重新加入队列
        for (int i = processedCount; i < allTasks.size(); i++) {
            updateQueue.offer(allTasks.get(i));
        }
        
        return processedCount;
    }
    
    /**
     * 按时间处理AI更新
     */
    private int processUpdatesByTime(long startTime) {
        int processedCount = 0;
        
        while (!updateQueue.isEmpty() && processedCount < maxBatchSize) {
            if (System.currentTimeMillis() - startTime >= maxBatchTimeMs) {
                break; // 时间限制
            }
            
            AIUpdateTask task = updateQueue.poll();
            if (task != null) {
                processAIUpdateTask(task);
                processedCount++;
            }
        }
        
        return processedCount;
    }
    
    /**
     * 处理单个AI更新任务
     */
    private void processAIUpdateTask(AIUpdateTask task) {
        try {
            Enemy enemy = task.getEnemy();
            if (enemy != null && enemy.isAlive()) {
                enemy.updateAI(task.getDeltaTime());
            }
        } catch (Exception e) {
            System.err.println("❌ AI更新错误: " + e.getMessage());
        }
    }
    
    /**
     * 计算AI更新优先级
     */
    private int calculatePriority(Enemy enemy) {
        if (!enableDistanceBasedPriority) {
            return AIPriority.NORMAL.getLevel();
        }
        
        // 这里需要玩家位置来计算距离，暂时使用默认优先级
        // 在实际集成时，需要传入玩家位置或通过其他方式获取
        return AIPriority.NORMAL.getLevel();
    }
    
    /**
     * 计算基于距离的AI更新优先级
     */
    public int calculateDistanceBasedPriority(Enemy enemy, double playerX, double playerY) {
        if (!enableDistanceBasedPriority) {
            return AIPriority.NORMAL.getLevel();
        }
        
        double distance = Math.sqrt(
            Math.pow(enemy.getX() - playerX, 2) + 
            Math.pow(enemy.getY() - playerY, 2)
        );
        
        if (distance <= HIGH_PRIORITY_DISTANCE) {
            return AIPriority.HIGH.getLevel();
        } else if (distance <= NORMAL_PRIORITY_DISTANCE) {
            return AIPriority.NORMAL.getLevel();
        } else {
            return AIPriority.LOW.getLevel();
        }
    }
    
    /**
     * 获取待处理任务数量
     */
    public int getPendingTaskCount() {
        return updateQueue.size();
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        if (totalBatchesProcessed == 0) {
            return "AI批处理统计: 暂无数据";
        }
        
        double avgUpdatesPerBatch = (double) totalAIUpdates / totalBatchesProcessed;
        double avgProcessingTime = (double) totalProcessingTimeMs / totalBatchesProcessed;
        
        return String.format(
            "AI批处理统计:\n" +
            "  总AI更新数: %d\n" +
            "  总批次数: %d\n" +
            "  平均每批次更新数: %.1f\n" +
            "  平均处理时间: %.2fms\n" +
            "  跳过更新数: %d\n" +
            "  待处理任务数: %d",
            totalAIUpdates, totalBatchesProcessed, avgUpdatesPerBatch, 
            avgProcessingTime, skippedUpdates, getPendingTaskCount()
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
        totalAIUpdates = 0;
        totalBatchesProcessed = 0;
        totalProcessingTimeMs = 0;
        skippedUpdates = 0;
    }
    
    /**
     * 清空所有待处理任务
     */
    public void clearAllTasks() {
        updateQueue.clear();
    }
    
    /**
     * 获取批处理配置信息
     */
    public String getConfigInfo() {
        return String.format(
            "AI批处理配置:\n" +
            "  最大批次大小: %d\n" +
            "  最大批处理时间: %dms\n" +
            "  优先级处理: %s\n" +
            "  距离优先级: %s\n" +
            "  高优先级距离: %.1f\n" +
            "  普通优先级距离: %.1f",
            maxBatchSize, maxBatchTimeMs, 
            enablePriorityProcessing ? "启用" : "禁用",
            enableDistanceBasedPriority ? "启用" : "禁用",
            HIGH_PRIORITY_DISTANCE, NORMAL_PRIORITY_DISTANCE
        );
    }
}
