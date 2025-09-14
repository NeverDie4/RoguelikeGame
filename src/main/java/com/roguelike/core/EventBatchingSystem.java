package com.roguelike.core;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * 事件批处理系统
 * 用于优化高频事件的处理性能，通过批量处理减少函数调用开销
 */
public class EventBatchingSystem {
    
    // 事件优先级枚举
    public enum EventPriority {
        CRITICAL(0),    // 关键事件：玩家死亡、游戏结束
        HIGH(1),        // 高优先级：碰撞、伤害
        NORMAL(2),      // 普通优先级：AI更新、移动
        LOW(3);         // 低优先级：UI更新、统计
        
        private final int level;
        
        EventPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    // 批处理事件包装类
    public static class BatchedEvent {
        private final GameEvent.Type eventType;
        private final Object data;
        private final EventPriority priority;
        private final long timestamp;
        
        public BatchedEvent(GameEvent.Type eventType, Object data, EventPriority priority) {
            this.eventType = eventType;
            this.data = data;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
        }
        
        public GameEvent.Type getEventType() {
            return eventType;
        }
        
        public Object getData() {
            return data;
        }
        
        public EventPriority getPriority() {
            return priority;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    // 事件队列 - 按优先级分组
    private final Map<EventPriority, Queue<BatchedEvent>> eventQueues;
    
    // 事件处理器映射
    private final Map<GameEvent.Type, List<Consumer<GameEvent>>> eventHandlers;
    
    // 批处理配置
    private final int maxBatchSize;
    private final long maxBatchTimeMs;
    private final boolean enablePriorityProcessing;
    
    // 性能统计
    private long totalEventsProcessed = 0;
    private long totalBatchesProcessed = 0;
    private long totalProcessingTimeMs = 0;
    
    // 调试模式
    private boolean debugMode = false;
    
    public EventBatchingSystem() {
        this(50, 16, true); // 默认配置：最大批次50，最大时间16ms，启用优先级处理
    }
    
    public EventBatchingSystem(int maxBatchSize, long maxBatchTimeMs, boolean enablePriorityProcessing) {
        this.maxBatchSize = maxBatchSize;
        this.maxBatchTimeMs = maxBatchTimeMs;
        this.enablePriorityProcessing = enablePriorityProcessing;
        
        // 初始化事件队列
        this.eventQueues = new EnumMap<>(EventPriority.class);
        for (EventPriority priority : EventPriority.values()) {
            this.eventQueues.put(priority, new ConcurrentLinkedQueue<>());
        }
        
        // 初始化事件处理器
        this.eventHandlers = new HashMap<>();
    }
    
    /**
     * 添加事件到批处理队列
     */
    public void addEvent(GameEvent.Type eventType, Object data, EventPriority priority) {
        BatchedEvent event = new BatchedEvent(eventType, data, priority);
        eventQueues.get(priority).offer(event);
        
        if (debugMode) {
            System.out.println("📦 事件添加到批处理队列: " + eventType + " (优先级: " + priority + ")");
        }
    }
    
    /**
     * 添加事件到批处理队列（使用默认优先级）
     */
    public void addEvent(GameEvent.Type eventType, Object data) {
        EventPriority priority = getDefaultPriority(eventType);
        addEvent(eventType, data, priority);
    }
    
    /**
     * 注册事件处理器
     */
    public void registerHandler(GameEvent.Type eventType, Consumer<GameEvent> handler) {
        eventHandlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }
    
    /**
     * 处理所有待处理的事件批次
     */
    public void processBatches() {
        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        
        if (enablePriorityProcessing) {
            // 按优先级处理事件
            totalProcessed = processEventsByPriority();
        } else {
            // 按时间处理事件
            totalProcessed = processEventsByTime();
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // 更新统计信息
        totalEventsProcessed += totalProcessed;
        totalBatchesProcessed++;
        totalProcessingTimeMs += processingTime;
        
        if (debugMode && totalProcessed > 0) {
            System.out.println("⚡ 批处理完成: " + totalProcessed + " 个事件, 耗时: " + processingTime + "ms");
        }
    }
    
    /**
     * 按优先级处理事件
     */
    private int processEventsByPriority() {
        int totalProcessed = 0;
        
        // 按优先级顺序处理
        for (EventPriority priority : EventPriority.values()) {
            Queue<BatchedEvent> queue = eventQueues.get(priority);
            int batchSize = 0;
            
            while (!queue.isEmpty() && batchSize < maxBatchSize) {
                BatchedEvent event = queue.poll();
                if (event != null) {
                    processEvent(event);
                    totalProcessed++;
                    batchSize++;
                }
            }
            
            // 如果处理了关键事件，立即返回
            if (priority == EventPriority.CRITICAL && totalProcessed > 0) {
                break;
            }
        }
        
        return totalProcessed;
    }
    
    /**
     * 按时间处理事件
     */
    private int processEventsByTime() {
        int totalProcessed = 0;
        long startTime = System.currentTimeMillis();
        
        // 收集所有事件并按优先级排序
        List<BatchedEvent> allEvents = new ArrayList<>();
        for (Queue<BatchedEvent> queue : eventQueues.values()) {
            allEvents.addAll(queue);
            queue.clear();
        }
        
        // 按优先级排序
        allEvents.sort(Comparator.comparingInt(e -> e.getPriority().getLevel()));
        
        // 处理事件直到达到时间限制或批次大小限制
        for (BatchedEvent event : allEvents) {
            if (totalProcessed >= maxBatchSize) break;
            if (System.currentTimeMillis() - startTime >= maxBatchTimeMs) break;
            
            processEvent(event);
            totalProcessed++;
        }
        
        return totalProcessed;
    }
    
    /**
     * 处理单个事件
     */
    private void processEvent(BatchedEvent event) {
        List<Consumer<GameEvent>> handlers = eventHandlers.get(event.getEventType());
        if (handlers != null) {
            GameEvent gameEvent = new GameEvent(event.getEventType());
            for (Consumer<GameEvent> handler : handlers) {
                try {
                    handler.accept(gameEvent);
                } catch (Exception e) {
                    System.err.println("❌ 事件处理错误: " + event.getEventType() + " - " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 获取事件类型的默认优先级
     */
    private EventPriority getDefaultPriority(GameEvent.Type eventType) {
        switch (eventType) {
            case PLAYER_DEATH:
            case ENEMY_DEATH:
                return EventPriority.CRITICAL;
            case PLAYER_ENEMY_COLLISION:
            case BULLET_ENEMY_COLLISION:
            case BULLET_PLAYER_COLLISION:
            case PLAYER_HURT:
            case ENEMY_HP_CHANGED:
                return EventPriority.HIGH;
            case PLAYER_MOVE:
            case ENEMY_ENEMY_COLLISION:
            case MAP_LOADED:
                return EventPriority.NORMAL;
            case SCORE_CHANGED:
            case COINS_CHANGED:
            case TIME_CHANGED:
            case EXPERIENCE_CHANGED:
            case LEVEL_UP:
                return EventPriority.LOW;
            default:
                return EventPriority.NORMAL;
        }
    }
    
    /**
     * 获取待处理事件数量
     */
    public int getPendingEventCount() {
        return eventQueues.values().stream()
                .mapToInt(Queue::size)
                .sum();
    }
    
    /**
     * 获取指定优先级的事件数量
     */
    public int getPendingEventCount(EventPriority priority) {
        return eventQueues.get(priority).size();
    }
    
    /**
     * 清空所有待处理事件
     */
    public void clearAllEvents() {
        for (Queue<BatchedEvent> queue : eventQueues.values()) {
            queue.clear();
        }
    }
    
    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        if (totalBatchesProcessed == 0) {
            return "批处理系统统计: 暂无数据";
        }
        
        double avgEventsPerBatch = (double) totalEventsProcessed / totalBatchesProcessed;
        double avgProcessingTime = (double) totalProcessingTimeMs / totalBatchesProcessed;
        
        return String.format(
            "批处理系统统计:\n" +
            "  总事件数: %d\n" +
            "  总批次数: %d\n" +
            "  平均每批次事件数: %.1f\n" +
            "  平均处理时间: %.2fms\n" +
            "  待处理事件数: %d",
            totalEventsProcessed, totalBatchesProcessed, avgEventsPerBatch, 
            avgProcessingTime, getPendingEventCount()
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
        totalEventsProcessed = 0;
        totalBatchesProcessed = 0;
        totalProcessingTimeMs = 0;
    }
}
