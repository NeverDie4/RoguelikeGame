package com.roguelike.entities;

import com.roguelike.entities.config.SpawnConfig;
import com.roguelike.map.InfiniteMapManager;
import com.roguelike.utils.RandomUtils;
import javafx.geometry.Point2D;
import java.util.*;
import java.util.concurrent.*;

/**
 * 无限地图敌人生成管理器
 * 基于原有 EnemySpawnManager 修改，适配无限地图系统
 * 支持敌人尺寸参数、后台预计算、失败处理等功能
 */
public class InfiniteMapEnemySpawnManager {
    
    // 配置参数（使用SpawnConfig中的配置）
    private static final int DEFAULT_MAX_ATTEMPTS = SpawnConfig.DEFAULT_MAX_ATTEMPTS;
    private static final double DEFAULT_MIN_ENEMY_DISTANCE = 50.0;
    
    // 地图系统
    private InfiniteMapManager infiniteMapManager;
    
    // 后台预计算
    private ExecutorService backgroundExecutor;
    private Map<String, Set<Point2D>> passablePositionsCache = new ConcurrentHashMap<>();
    private Map<String, Long> regionCacheTime = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = SpawnConfig.CACHE_DURATION;
    
    // 敌人位置记录（避免重叠）
    private Set<Point2D> occupiedPositions = ConcurrentHashMap.newKeySet();
    private Map<Point2D, Long> positionOccupiedTime = new ConcurrentHashMap<>();
    private static final long POSITION_OCCUPIED_DURATION = SpawnConfig.POSITION_OCCUPIED_DURATION;
    
    // 调试信息
    private boolean debugMode = false; // 关闭调试模式
    private int totalSpawnAttempts = 0;
    private int successfulSpawns = 0;
    private int failedSpawns = 0;
    
    // 预计算范围（使用SpawnConfig中的配置）
    private static final double PRECOMPUTE_RANGE = SpawnConfig.PRECOMPUTE_RANGE;
    
    public InfiniteMapEnemySpawnManager(InfiniteMapManager infiniteMapManager) {
        this.infiniteMapManager = infiniteMapManager;
        this.backgroundExecutor = Executors.newFixedThreadPool(2);
        
        System.out.println("🎯 无限地图敌人生成管理器初始化完成");
        System.out.println("   预计算范围: " + PRECOMPUTE_RANGE + " 像素");
        System.out.println("   缓存持续时间: " + CACHE_DURATION + " 毫秒");
        System.out.println("   支持分层预计算和智能生成");
    }
    
    /**
     * 生成敌人生成位置（支持敌人尺寸参数）
     * @param playerPosition 玩家位置
     * @param enemyWidth 敌人宽度
     * @param enemyHeight 敌人高度
     * @param minDistance 最小生成距离
     * @param maxDistance 最大生成距离
     * @return 生成位置，失败返回null
     */
    public Point2D generateEnemySpawnPosition(Point2D playerPosition, double enemyWidth, double enemyHeight,
                                            double minDistance, double maxDistance) {
        return generateEnemySpawnPosition(playerPosition, enemyWidth, enemyHeight, 
                                        minDistance, maxDistance, DEFAULT_MAX_ATTEMPTS);
    }
    
    /**
     * 生成敌人生成位置（带最大尝试次数）
     */
    public Point2D generateEnemySpawnPosition(Point2D playerPosition, double enemyWidth, double enemyHeight,
                                            double minDistance, double maxDistance, int maxAttempts) {
        if (debugMode) {
            System.out.println("🎯 尝试生成敌人生成位置");
            System.out.println("   玩家位置: " + playerPosition);
            System.out.println("   敌人尺寸: " + enemyWidth + "x" + enemyHeight);
            System.out.println("   生成距离: " + minDistance + " - " + maxDistance);
        }
        
        // 异步分层预计算区域
        precomputeLayeredAsync(playerPosition, Math.max(enemyWidth, enemyHeight));
        
        // 清理过期的占用位置
        cleanupExpiredOccupiedPositions();
        
        // 尝试生成位置
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            totalSpawnAttempts++;
            
            Point2D candidate = generateRandomPosition(playerPosition, minDistance, maxDistance);
            
            // 检查距离是否在要求范围内
            double distanceToPlayer = candidate.distance(playerPosition);
            if (distanceToPlayer < minDistance || distanceToPlayer > maxDistance) {
                continue; // 距离不符合要求，尝试下一个
            }
            
            if (isValidSpawnPosition(candidate, enemyWidth, enemyHeight, playerPosition)) {
                // 记录占用位置
                occupiedPositions.add(candidate);
                positionOccupiedTime.put(candidate, System.currentTimeMillis());
                
                successfulSpawns++;
                
                if (debugMode) {
                    System.out.println("✅ 成功生成敌人生成位置: " + candidate + " (尝试次数: " + (attempt + 1) + ", 距离: " + String.format("%.1f", distanceToPlayer) + ")");
                }
                
                return candidate;
            }
        }
        
        // 失败处理：尝试放宽条件
        Point2D fallbackPosition = tryFallbackSpawn(playerPosition, enemyWidth, enemyHeight, minDistance, maxDistance);
        if (fallbackPosition != null) {
            successfulSpawns++;
            return fallbackPosition;
        }
        
        failedSpawns++;
        
        if (debugMode) {
            System.out.println("❌ 无法找到合适的敌人生成位置 (尝试次数: " + maxAttempts + ")");
        }
        
        return null;
    }
    
    /**
     * 失败处理：尝试放宽条件生成
     */
    private Point2D tryFallbackSpawn(Point2D playerPosition, double enemyWidth, double enemyHeight,
                                   double minDistance, double maxDistance) {
        if (debugMode) {
            System.out.println("🔄 尝试失败处理生成...");
        }
        
        // 放宽距离要求
        double relaxedMinDistance = minDistance * 0.7;
        double relaxedMaxDistance = maxDistance * 1.3;
        
        // 减少敌人间距要求
        double originalSpacing = DEFAULT_MIN_ENEMY_DISTANCE;
        double relaxedSpacing = originalSpacing * 0.6;
        
        try {
            for (int attempt = 0; attempt < 30; attempt++) {
                Point2D candidate = generateRandomPosition(playerPosition, relaxedMinDistance, relaxedMaxDistance);
                
                if (isValidSpawnPositionWithRelaxedSpacing(candidate, enemyWidth, enemyHeight, playerPosition, relaxedSpacing)) {
                    occupiedPositions.add(candidate);
                    positionOccupiedTime.put(candidate, System.currentTimeMillis());
                    
                    if (debugMode) {
                        System.out.println("✅ 失败处理成功生成位置: " + candidate);
                    }
                    
                    return candidate;
                }
            }
        } catch (Exception e) {
            System.err.println("失败处理生成时出错: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 检查生成位置是否有效（考虑敌人尺寸）
     */
    private boolean isValidSpawnPosition(Point2D position, double enemyWidth, double enemyHeight, Point2D playerPosition) {
        // 检查是否可通行（考虑敌人尺寸）
        if (!isAreaPassable(position, enemyWidth, enemyHeight)) {
            return false;
        }
        
        // 检查是否与其他敌人重叠
        if (isPositionOccupied(position, enemyWidth, enemyHeight)) {
            return false;
        }
        
        // 距离检查由调用方控制，这里不进行硬编码的距离检查
        // 因为不同的生成策略可能需要不同的距离要求
        
        return true;
    }
    
    /**
     * 检查生成位置是否有效（使用放宽的间距要求）
     */
    private boolean isValidSpawnPositionWithRelaxedSpacing(Point2D position, double enemyWidth, double enemyHeight, 
                                                         Point2D playerPosition, double relaxedSpacing) {
        // 检查是否可通行（考虑敌人尺寸）
        if (!isAreaPassable(position, enemyWidth, enemyHeight)) {
            return false;
        }
        
        // 检查是否与其他敌人重叠（使用放宽的间距）
        if (isPositionOccupiedWithSpacing(position, enemyWidth, enemyHeight, relaxedSpacing)) {
            return false;
        }
        
        // 检查是否与玩家距离合适
        double distanceToPlayer = position.distance(playerPosition);
        if (distanceToPlayer < SpawnConfig.SCREEN_OUT_MIN * 0.8) { // 稍微放宽玩家距离要求
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查区域是否可通行（考虑敌人尺寸）
     */
    private boolean isAreaPassable(Point2D center, double width, double height) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        
        // 检查四个角点
        Point2D[] corners = {
            new Point2D(center.getX() - halfWidth, center.getY() - halfHeight), // 左上
            new Point2D(center.getX() + halfWidth, center.getY() - halfHeight), // 右上
            new Point2D(center.getX() - halfWidth, center.getY() + halfHeight), // 左下
            new Point2D(center.getX() + halfWidth, center.getY() + halfHeight)  // 右下
        };
        
        for (Point2D corner : corners) {
            if (infiniteMapManager.isUnaccessible(corner.getX(), corner.getY())) {
                return false;
            }
        }
        
        // 额外检查中心点
        return infiniteMapManager.isPassable(center.getX(), center.getY());
    }
    
    /**
     * 检查区域是否可通行（考虑敌人尺寸和安全距离）
     */
    private boolean isAreaPassableWithSafety(Point2D center, double width, double height, double safetyDistance) {
        double halfWidth = width / 2.0 + safetyDistance;
        double halfHeight = height / 2.0 + safetyDistance;
        
        // 检查扩展后的四个角点
        Point2D[] corners = {
            new Point2D(center.getX() - halfWidth, center.getY() - halfHeight), // 左上
            new Point2D(center.getX() + halfWidth, center.getY() - halfHeight), // 右上
            new Point2D(center.getX() - halfWidth, center.getY() + halfHeight), // 左下
            new Point2D(center.getX() + halfWidth, center.getY() + halfHeight)  // 右下
        };
        
        for (Point2D corner : corners) {
            if (infiniteMapManager.isUnaccessible(corner.getX(), corner.getY())) {
                return false;
            }
        }
        
        // 额外检查中心点
        return infiniteMapManager.isPassable(center.getX(), center.getY());
    }
    
    /**
     * 异步分层预计算区域
     */
    private void precomputeLayeredAsync(Point2D playerPosition, double enemySize) {
        String regionKey = getRegionKey(playerPosition);
        
        // 检查缓存是否有效
        if (isCacheValid(regionKey)) {
            return;
        }
        
        // 提交后台任务
        backgroundExecutor.submit(() -> {
            try {
                precomputeLayered(playerPosition, regionKey, enemySize);
            } catch (Exception e) {
                System.err.println("分层预计算区域失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 分层预计算区域（后台线程执行）
     */
    private void precomputeLayered(Point2D playerPosition, String regionKey, double enemySize) {
        Set<Point2D> passablePositions = new HashSet<>();
        
        // 根据敌人尺寸获取预计算参数
        int stepSize = SpawnConfig.getStepSizeForEnemy(enemySize);
        double safetyDistance = SpawnConfig.getSafetyDistanceForEnemy(enemySize);
        
        // 获取生成距离范围
        double[][] distanceRanges = SpawnConfig.getSpawnDistanceRanges();
        
        // 分层预计算
        for (double[] range : distanceRanges) {
            double minDist = range[0];
            double maxDist = range[1];
            
            // 计算该层的预计算范围
            for (double x = playerPosition.getX() - maxDist; x <= playerPosition.getX() + maxDist; x += stepSize) {
                for (double y = playerPosition.getY() - maxDist; y <= playerPosition.getY() + maxDist; y += stepSize) {
                    double distance = playerPosition.distance(x, y);
                    
                    // 检查是否在该层范围内
                    if (distance >= minDist && distance <= maxDist) {
                        // 检查是否可通行（考虑敌人尺寸和安全距离）
                        if (isAreaPassableWithSafety(new Point2D(x, y), enemySize, enemySize, safetyDistance)) {
                            passablePositions.add(new Point2D(x, y));
                        }
                    }
                }
            }
        }
        
        // 更新缓存
        passablePositionsCache.put(regionKey, passablePositions);
        regionCacheTime.put(regionKey, System.currentTimeMillis());
        
        if (debugMode) {
            System.out.println("🗺️ 分层预计算完成: " + regionKey + " (" + passablePositions.size() + "个位置, 步长:" + stepSize + ", 安全距离:" + safetyDistance + ")");
        }
    }
    
    /**
     * 预计算区域（后台线程执行）- 保留原方法作为备用
     */
    private void precomputeRegion(Point2D playerPosition, String regionKey) {
        Set<Point2D> passablePositions = new HashSet<>();
        
        // 使用默认步长进行预计算
        int stepSize = SpawnConfig.SMALL_ENEMY_STEP;
        
        // 计算预计算范围
        for (double x = playerPosition.getX() - PRECOMPUTE_RANGE; x <= playerPosition.getX() + PRECOMPUTE_RANGE; x += stepSize) {
            for (double y = playerPosition.getY() - PRECOMPUTE_RANGE; y <= playerPosition.getY() + PRECOMPUTE_RANGE; y += stepSize) {
                if (infiniteMapManager.isPassable(x, y)) {
                    passablePositions.add(new Point2D(x, y));
                }
            }
        }
        
        // 更新缓存
        passablePositionsCache.put(regionKey, passablePositions);
        regionCacheTime.put(regionKey, System.currentTimeMillis());
        
        if (debugMode) {
            System.out.println("🗺️ 预计算完成: " + regionKey + " (" + passablePositions.size() + "个位置)");
        }
    }
    
    /**
     * 生成随机位置
     */
    private Point2D generateRandomPosition(Point2D playerPosition, double minDistance, double maxDistance) {
        double angle = Math.toRadians(RandomUtils.nextInt(0, 359));
        double distance = minDistance + (maxDistance - minDistance) * Math.random();
        
        double x = playerPosition.getX() + Math.cos(angle) * distance;
        double y = playerPosition.getY() + Math.sin(angle) * distance;
        
        Point2D result = new Point2D(x, y);
        
        if (debugMode) {
            double actualDistance = playerPosition.distance(result);
            System.out.println("🎲 生成候选位置: " + result + " (角度: " + String.format("%.1f", Math.toDegrees(angle)) + 
                             "°, 期望距离: " + String.format("%.1f", distance) + 
                             ", 实际距离: " + String.format("%.1f", actualDistance) + ")");
        }
        
        return result;
    }
    
    /**
     * 检查位置是否被占用（考虑敌人尺寸）
     */
    private boolean isPositionOccupied(Point2D position, double enemyWidth, double enemyHeight) {
        double spacing = Math.max(DEFAULT_MIN_ENEMY_DISTANCE, Math.max(enemyWidth, enemyHeight) * 1.2);
        
        for (Point2D occupiedPos : occupiedPositions) {
            if (position.distance(occupiedPos) < spacing) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查位置是否被占用（使用自定义间距）
     */
    private boolean isPositionOccupiedWithSpacing(Point2D position, double enemyWidth, double enemyHeight, double spacing) {
        double actualSpacing = Math.max(spacing, Math.max(enemyWidth, enemyHeight) * 1.1);
        
        for (Point2D occupiedPos : occupiedPositions) {
            if (position.distance(occupiedPos) < actualSpacing) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 清理过期的占用位置
     */
    private void cleanupExpiredOccupiedPositions() {
        long currentTime = System.currentTimeMillis();
        
        occupiedPositions.removeIf(pos -> {
            Long occupiedTime = positionOccupiedTime.get(pos);
            if (occupiedTime != null && (currentTime - occupiedTime) > POSITION_OCCUPIED_DURATION) {
                positionOccupiedTime.remove(pos);
                return true;
            }
            return false;
        });
    }
    
    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(String regionKey) {
        Long cacheTime = regionCacheTime.get(regionKey);
        return cacheTime != null && (System.currentTimeMillis() - cacheTime) < CACHE_DURATION;
    }
    
    /**
     * 生成区域键
     */
    private String getRegionKey(Point2D position) {
        // 使用更小的区域分割，避免缓存过于粗糙
        double regionSize = 800.0; // 800像素一个区域
        int chunkX = (int) Math.floor(position.getX() / regionSize);
        int chunkY = (int) Math.floor(position.getY() / regionSize);
        return chunkX + "," + chunkY;
    }
    
    // ========== 调试接口 ==========
    
    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        System.out.println("🔧 敌人生成调试模式: " + (debugMode ? "开启" : "关闭"));
    }
    
    /**
     * 获取调试信息
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("🎯 无限地图敌人生成管理器调试信息:\n");
        info.append("   总生成尝试次数: ").append(totalSpawnAttempts).append("\n");
        info.append("   成功生成次数: ").append(successfulSpawns).append("\n");
        info.append("   失败生成次数: ").append(failedSpawns).append("\n");
        info.append("   当前占用位置数: ").append(occupiedPositions.size()).append("\n");
        info.append("   缓存区域数: ").append(passablePositionsCache.size()).append("\n");
        
        if (totalSpawnAttempts > 0) {
            double successRate = (double) successfulSpawns / totalSpawnAttempts * 100;
            info.append("   生成成功率: ").append(String.format("%.1f", successRate)).append("%\n");
        }
        
        return info.toString();
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalSpawnAttempts = 0;
        successfulSpawns = 0;
        failedSpawns = 0;
        occupiedPositions.clear();
        positionOccupiedTime.clear();
        System.out.println("📊 敌人生成统计信息已重置");
    }
    
    /**
     * 清理所有缓存
     */
    public void clearCache() {
        passablePositionsCache.clear();
        regionCacheTime.clear();
        System.out.println("🗑️ 敌人生成缓存已清理");
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundExecutor.shutdownNow();
            }
        }
    }
}
