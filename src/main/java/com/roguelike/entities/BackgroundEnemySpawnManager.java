package com.roguelike.entities;

import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.config.EnemyConfig;
import com.roguelike.entities.config.EnemyConfigManager;
import com.roguelike.entities.config.SpawnConfig;
import javafx.geometry.Point2D;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 后台敌人生成管理器
 * 负责在后台线程中生成敌人，避免阻塞主线程
 */
public class BackgroundEnemySpawnManager {
    
    // 线程池和任务管理
    private ExecutorService backgroundExecutor;
    private ScheduledExecutorService spawnScheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger activeSpawnTasks = new AtomicInteger(0);
    
    // 生成配置（使用SpawnConfig中的配置）
    private static final int MAX_CONCURRENT_SPAWNS = SpawnConfig.MAX_CONCURRENT_SPAWNS;
    private static final long SPAWN_INTERVAL_MS = 2000; // 生成间隔（毫秒）
    private static final int MAX_ENEMIES_PER_BATCH = SpawnConfig.MAX_ENEMIES_PER_BATCH;
    
    // 生成参数（使用SpawnConfig中的默认值）
    private double minSpawnDistance = SpawnConfig.SCREEN_OUT_MIN;
    private double maxSpawnDistance = SpawnConfig.SCREEN_OUT_MAX;
    private int maxEnemiesInWorld = 50; // 世界中最大敌人数
    
    // 依赖组件
    private InfiniteMapEnemySpawnManager infiniteMapSpawnManager;
    private EnemyConfigManager configManager;
    
    // 统计信息
    private final AtomicInteger totalSpawned = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);
    
    // 智能间隔调整
    private Point2D lastPlayerPosition = new Point2D(0, 0);
    private long lastUpdateTime = 0;
    
    public BackgroundEnemySpawnManager() {
        // 创建线程池
        this.backgroundExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_SPAWNS, r -> {
            Thread t = new Thread(r, "EnemySpawnWorker");
            t.setDaemon(true);
            return t;
        });
        
        this.spawnScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "EnemySpawnScheduler");
            t.setDaemon(true);
            return t;
        });
        
        // 初始化配置管理器
        this.configManager = EnemyConfigManager.getInstance();
        if (!configManager.isInitialized()) {
            configManager.initialize();
        }
        
        System.out.println("🎯 后台敌人生成管理器初始化完成");
        System.out.println("   最大并发生成任务: " + MAX_CONCURRENT_SPAWNS);
        System.out.println("   生成间隔: " + SPAWN_INTERVAL_MS + " 毫秒");
        System.out.println("   每批最大敌人数: " + MAX_ENEMIES_PER_BATCH);
    }
    
    /**
     * 设置无限地图生成管理器
     */
    public void setInfiniteMapSpawnManager(InfiniteMapEnemySpawnManager spawnManager) {
        this.infiniteMapSpawnManager = spawnManager;
    }
    
    /**
     * 开始后台生成
     */
    public void startSpawning() {
        if (isRunning.compareAndSet(false, true)) {
            System.out.println("🚀 开始后台敌人生成...");
            
            // 启动定时生成任务
            spawnScheduler.scheduleAtFixedRate(this::scheduleEnemySpawn, 
                1000, SPAWN_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 停止后台生成
     */
    public void stopSpawning() {
        if (isRunning.compareAndSet(true, false)) {
            System.out.println("⏹️ 停止后台敌人生成...");
            
            // 停止调度器
            spawnScheduler.shutdown();
            
            // 等待所有任务完成
            try {
                if (!spawnScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    spawnScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                spawnScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 调度敌人生成任务（智能间隔调整）
     */
    private void scheduleEnemySpawn() {
        if (!isRunning.get()) {
            return;
        }
        
        // 检查当前敌人数
        int currentEnemyCount = getCurrentEnemyCount();
        if (currentEnemyCount >= maxEnemiesInWorld) {
            return; // 敌人数已达上限
        }
        
        // 检查并发任务数
        if (activeSpawnTasks.get() >= MAX_CONCURRENT_SPAWNS) {
            return; // 并发任务数已达上限
        }
        
        // 智能间隔调整
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < calculateSmartInterval()) {
            return; // 间隔时间未到
        }
        
        // 提交生成任务
        backgroundExecutor.submit(() -> {
            try {
                activeSpawnTasks.incrementAndGet();
                spawnEnemyBatch();
                lastUpdateTime = currentTime;
            } catch (Exception e) {
                System.err.println("❌ 敌人生成任务失败: " + e.getMessage());
                totalFailed.incrementAndGet();
            } finally {
                activeSpawnTasks.decrementAndGet();
            }
        });
    }
    
    /**
     * 计算智能更新间隔
     */
    private long calculateSmartInterval() {
        // 获取玩家当前位置
        Entity player = getGameWorld().getEntitiesByType().stream()
            .filter(e -> e instanceof Player)
            .findFirst()
            .orElse(null);
        
        if (player == null) {
            return SpawnConfig.UPDATE_INTERVAL_STATIC;
        }
        
        Point2D currentPlayerPos = player.getCenter();
        double movementDistance = lastPlayerPosition.distance(currentPlayerPos);
        
        // 更新玩家位置
        lastPlayerPosition = currentPlayerPos;
        
        // 根据移动距离计算间隔
        return SpawnConfig.calculateUpdateInterval(movementDistance);
    }
    
    /**
     * 生成一批敌人
     */
    private void spawnEnemyBatch() {
        // 获取玩家位置
        Entity player = getGameWorld().getEntitiesByType().stream()
            .filter(e -> e instanceof Player)
            .findFirst()
            .orElse(null);
        
        if (player == null) {
            return; // 没有玩家，不生成敌人
        }
        
        Point2D playerPos = player.getCenter();
        
        // 生成1-2个敌人
        int enemiesToSpawn = Math.min(MAX_ENEMIES_PER_BATCH, 
            maxEnemiesInWorld - getCurrentEnemyCount());
        
        for (int i = 0; i < enemiesToSpawn; i++) {
            if (!isRunning.get()) {
                break; // 如果已停止，退出循环
            }
            
            spawnSingleEnemy(playerPos);
        }
    }
    
    /**
     * 生成单个敌人
     */
    private void spawnSingleEnemy(Point2D playerPos) {
        try {
            // 随机选择敌人配置
            EnemyConfig config = configManager.getRandomEnemyConfig();
            if (config == null) {
                System.err.println("⚠️ 无法获取敌人配置");
                return;
            }
            
            // 生成生成位置
            Point2D spawnPos = null;
            if (infiniteMapSpawnManager != null) {
                spawnPos = infiniteMapSpawnManager.generateEnemySpawnPosition(
                    playerPos,
                    config.getSize().getWidth(),
                    config.getSize().getHeight(),
                    minSpawnDistance,
                    maxSpawnDistance
                );
            }
            
            if (spawnPos == null) {
                // 回退到简单随机位置
                spawnPos = generateRandomPosition(playerPos, minSpawnDistance, maxSpawnDistance);
            }
            
            // 在JavaFX应用线程中创建敌人实体，使用高优先级确保及时执行
            final EnemyConfig finalConfig = config;
            final Point2D finalSpawnPos = spawnPos;
            javafx.application.Platform.runLater(() -> {
                try {
                    Entity enemy = spawn("enemy", finalSpawnPos.getX(), finalSpawnPos.getY());
                    if (enemy != null) {
                        totalSpawned.incrementAndGet();
                        
                        // 确保新生成的敌人立即开始寻路
                        if (enemy instanceof Enemy) {
                            ((Enemy) enemy).initializeTargetPosition();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ 创建敌人实体失败: " + e.getMessage());
                    totalFailed.incrementAndGet();
                }
            });
            
        } catch (Exception e) {
            System.err.println("❌ 生成敌人失败: " + e.getMessage());
            totalFailed.incrementAndGet();
        }
    }
    
    /**
     * 生成随机位置（回退方案）
     */
    private Point2D generateRandomPosition(Point2D playerPos, double minDistance, double maxDistance) {
        double angle = Math.toRadians(Math.random() * 360);
        double distance = minDistance + (maxDistance - minDistance) * Math.random();
        
        double x = playerPos.getX() + Math.cos(angle) * distance;
        double y = playerPos.getY() + Math.sin(angle) * distance;
        
        return new Point2D(x, y);
    }
    
    /**
     * 获取当前敌人数
     */
    private int getCurrentEnemyCount() {
        return getGameWorld().getEntitiesByType().stream()
            .filter(e -> e instanceof Enemy)
            .mapToInt(e -> 1)
            .sum();
    }
    
    /**
     * 设置生成参数
     */
    public void setSpawnParameters(double minDistance, double maxDistance, int maxEnemies) {
        this.minSpawnDistance = minDistance;
        this.maxSpawnDistance = maxDistance;
        this.maxEnemiesInWorld = maxEnemies;
        
        System.out.println("🔧 更新生成参数:");
        System.out.println("   生成距离: " + minDistance + " - " + maxDistance);
        System.out.println("   最大敌人数: " + maxEnemies);
        System.out.println("   使用智能间隔调整");
    }
    
    /**
     * 使用SpawnConfig中的默认参数
     */
    public void useDefaultSpawnParameters() {
        this.minSpawnDistance = SpawnConfig.SCREEN_OUT_MIN;
        this.maxSpawnDistance = SpawnConfig.SCREEN_OUT_MAX;
        
        System.out.println("🔧 使用默认生成参数:");
        System.out.println("   生成距离: " + minSpawnDistance + " - " + maxSpawnDistance);
        System.out.println("   最大敌人数: " + maxEnemiesInWorld);
        System.out.println("   使用智能间隔调整");
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("🎯 后台敌人生成统计:\n");
        stats.append("   运行状态: ").append(isRunning.get() ? "运行中" : "已停止").append("\n");
        stats.append("   活跃生成任务: ").append(activeSpawnTasks.get()).append("\n");
        stats.append("   当前敌人数: ").append(getCurrentEnemyCount()).append("\n");
        stats.append("   总生成数: ").append(totalSpawned.get()).append("\n");
        stats.append("   总失败数: ").append(totalFailed.get()).append("\n");
        
        if (totalSpawned.get() > 0) {
            double successRate = (double) totalSpawned.get() / (totalSpawned.get() + totalFailed.get()) * 100;
            stats.append("   成功率: ").append(String.format("%.1f", successRate)).append("%\n");
        }
        
        return stats.toString();
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalSpawned.set(0);
        totalFailed.set(0);
        System.out.println("📊 后台敌人生成统计已重置");
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        stopSpawning();
        
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("🗑️ 后台敌人生成管理器已关闭");
    }
}
