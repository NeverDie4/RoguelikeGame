package com.roguelike.entities;

import com.roguelike.map.MapRenderer;
import com.roguelike.map.CollisionMap;
import com.roguelike.utils.RandomUtils;
import javafx.geometry.Point2D;

import java.util.*;

/**
 * 敌人生成位置管理器
 * 负责在无限地图的可通行位置生成敌人，避免重叠生成
 */
public class EnemySpawnManager {
    
    // 配置参数
    private static final double DEFAULT_MIN_SPAWN_DISTANCE = 200.0;
    private static final double DEFAULT_MAX_SPAWN_DISTANCE = 400.0;
    private static final int DEFAULT_MAX_ATTEMPTS = 100;
    private static final double DEFAULT_MIN_ENEMY_DISTANCE = 50.0; // 敌人之间的最小距离
    private static final double CACHE_UPDATE_INTERVAL = 2.0; // 缓存更新间隔（秒）
    
    // 地图相关
    private MapRenderer mapRenderer;
    private CollisionMap collisionMap;
    
    // 缓存系统
    private Map<String, Set<Point2D>> passablePositionsCache = new HashMap<>();
    private Point2D lastPlayerChunk = null;
    private double lastCacheUpdateTime = 0;
    
    // 敌人位置记录（避免重叠）
    private Set<Point2D> occupiedPositions = new HashSet<>();
    private Map<Point2D, Long> positionOccupiedTime = new HashMap<>();
    private static final long POSITION_OCCUPIED_DURATION = 5000; // 位置占用持续时间（毫秒）
    
    // 调试信息
    private boolean debugMode = false;
    private int totalSpawnAttempts = 0;
    private int successfulSpawns = 0;
    private int failedSpawns = 0;
    
    public EnemySpawnManager(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
        this.collisionMap = mapRenderer.getCollisionMap();
        
        System.out.println("🎯 敌人生成管理器初始化完成");
        System.out.println("   地图尺寸: " + collisionMap.getWidth() + "x" + collisionMap.getHeight());
    }
    
    /**
     * 生成敌人生成位置
     * @param playerPosition 玩家位置
     * @param minDistance 最小生成距离
     * @param maxDistance 最大生成距离
     * @return 生成位置，如果找不到合适位置返回null
     */
    public Point2D generateEnemySpawnPosition(Point2D playerPosition, double minDistance, double maxDistance) {
        return generateEnemySpawnPosition(playerPosition, minDistance, maxDistance, DEFAULT_MAX_ATTEMPTS);
    }
    
    /**
     * 生成敌人生成位置（带最大尝试次数）
     * @param playerPosition 玩家位置
     * @param minDistance 最小生成距离
     * @param maxDistance 最大生成距离
     * @param maxAttempts 最大尝试次数
     * @return 生成位置，如果找不到合适位置返回null
     */
    public Point2D generateEnemySpawnPosition(Point2D playerPosition, double minDistance, double maxDistance, int maxAttempts) {
        if (debugMode) {
            System.out.println("🎯 尝试生成敌人生成位置");
            System.out.println("   玩家位置: " + playerPosition);
            System.out.println("   生成距离: " + minDistance + " - " + maxDistance);
        }
        
        // 更新缓存
        updatePassablePositionsCache(playerPosition);
        
        // 清理过期的占用位置
        cleanupExpiredOccupiedPositions();
        
        // 尝试生成位置
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            totalSpawnAttempts++;
            
            Point2D candidate = generateRandomPosition(playerPosition, minDistance, maxDistance);
            
            if (isValidSpawnPosition(candidate, playerPosition)) {
                // 记录占用位置
                occupiedPositions.add(candidate);
                positionOccupiedTime.put(candidate, System.currentTimeMillis());
                
                successfulSpawns++;
                
                if (debugMode) {
                    System.out.println("✅ 成功生成敌人生成位置: " + candidate + " (尝试次数: " + (attempt + 1) + ")");
                }
                
                return candidate;
            }
        }
        
        failedSpawns++;
        
        if (debugMode) {
            System.out.println("❌ 无法找到合适的敌人生成位置 (尝试次数: " + maxAttempts + ")");
        }
        
        return null;
    }
    
    /**
     * 更新可通行位置缓存
     */
    private void updatePassablePositionsCache(Point2D playerPosition) {
        // 获取当前区块坐标（假设区块大小为地图大小）
        Point2D currentChunk = getChunkCoordinate(playerPosition);
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        // 检查是否需要更新缓存
        if (shouldUpdateCache(currentChunk, currentTime)) {
            precomputePassablePositions(currentChunk);
            lastPlayerChunk = currentChunk;
            lastCacheUpdateTime = currentTime;
        }
    }
    
    /**
     * 判断是否需要更新缓存
     */
    private boolean shouldUpdateCache(Point2D currentChunk, double currentTime) {
        return !currentChunk.equals(lastPlayerChunk) || 
               (currentTime - lastCacheUpdateTime) > CACHE_UPDATE_INTERVAL;
    }
    
    /**
     * 预计算可通行位置
     */
    private void precomputePassablePositions(Point2D chunkCoord) {
        String chunkKey = getChunkKey(chunkCoord);
        Set<Point2D> passablePositions = new HashSet<>();
        
        // 遍历地图内的所有瓦片
        for (int y = 0; y < collisionMap.getHeight(); y++) {
            for (int x = 0; x < collisionMap.getWidth(); x++) {
                if (collisionMap.isPassable(x, y)) {
                    // 转换为世界坐标
                    Point2D worldPos = getWorldPosition(chunkCoord, x, y);
                    passablePositions.add(worldPos);
                }
            }
        }
        
        passablePositionsCache.put(chunkKey, passablePositions);
        
        if (debugMode) {
            System.out.println("🗺️ 预计算可通行位置: " + chunkKey + 
                             " (共" + passablePositions.size() + "个位置)");
        }
    }
    
    /**
     * 生成随机位置
     */
    private Point2D generateRandomPosition(Point2D playerPosition, double minDistance, double maxDistance) {
        // 在玩家周围环形区域生成随机位置
        double angle = Math.toRadians(RandomUtils.nextInt(0, 359));
        double distance = minDistance + (maxDistance - minDistance) * Math.random();
        
        double x = playerPosition.getX() + Math.cos(angle) * distance;
        double y = playerPosition.getY() + Math.sin(angle) * distance;
        
        return new Point2D(x, y);
    }
    
    /**
     * 检查生成位置是否有效
     */
    private boolean isValidSpawnPosition(Point2D position, Point2D playerPosition) {
        // 检查是否可通行
        if (!isPositionPassable(position)) {
            return false;
        }
        
        // 检查是否与其他敌人重叠
        if (isPositionOccupied(position)) {
            return false;
        }
        
        // 检查是否与玩家距离合适
        double distanceToPlayer = position.distance(playerPosition);
        if (distanceToPlayer < DEFAULT_MIN_SPAWN_DISTANCE) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查位置是否可通行
     */
    private boolean isPositionPassable(Point2D worldPosition) {
        // 获取当前区块坐标
        Point2D chunkCoord = getChunkCoordinate(worldPosition);
        String chunkKey = getChunkKey(chunkCoord);
        
        // 检查缓存
        Set<Point2D> passablePositions = passablePositionsCache.get(chunkKey);
        if (passablePositions != null) {
            return passablePositions.contains(worldPosition);
        }
        
        // 回退到实时检测
        Point2D localPos = getLocalPositionInChunk(worldPosition);
        int tileX = (int) (localPos.getX() / mapRenderer.getTileWidth());
        int tileY = (int) (localPos.getY() / mapRenderer.getTileHeight());
        
        return collisionMap.isPassable(tileX, tileY);
    }
    
    /**
     * 检查位置是否被占用
     */
    private boolean isPositionOccupied(Point2D position) {
        for (Point2D occupiedPos : occupiedPositions) {
            if (position.distance(occupiedPos) < DEFAULT_MIN_ENEMY_DISTANCE) {
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
    
    // ========== 工具方法 ==========
    
    /**
     * 获取区块坐标
     */
    private Point2D getChunkCoordinate(Point2D worldPosition) {
        int chunkX = (int) Math.floor(worldPosition.getX() / (collisionMap.getWidth() * mapRenderer.getTileWidth()));
        int chunkY = (int) Math.floor(worldPosition.getY() / (collisionMap.getHeight() * mapRenderer.getTileHeight()));
        return new Point2D(chunkX, chunkY);
    }
    
    /**
     * 获取区块内的本地坐标
     */
    private Point2D getLocalPositionInChunk(Point2D worldPosition) {
        Point2D chunkCoord = getChunkCoordinate(worldPosition);
        double localX = worldPosition.getX() - chunkCoord.getX() * collisionMap.getWidth() * mapRenderer.getTileWidth();
        double localY = worldPosition.getY() - chunkCoord.getY() * collisionMap.getHeight() * mapRenderer.getTileHeight();
        return new Point2D(localX, localY);
    }
    
    /**
     * 获取世界坐标
     */
    private Point2D getWorldPosition(Point2D chunkCoord, int localX, int localY) {
        return new Point2D(
            chunkCoord.getX() * collisionMap.getWidth() * mapRenderer.getTileWidth() + localX * mapRenderer.getTileWidth(),
            chunkCoord.getY() * collisionMap.getHeight() * mapRenderer.getTileHeight() + localY * mapRenderer.getTileHeight()
        );
    }
    
    /**
     * 生成区块键
     */
    private String getChunkKey(Point2D chunkCoord) {
        return (int)chunkCoord.getX() + "," + (int)chunkCoord.getY();
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
        info.append("🎯 敌人生成管理器调试信息:\n");
        info.append("   总生成尝试次数: ").append(totalSpawnAttempts).append("\n");
        info.append("   成功生成次数: ").append(successfulSpawns).append("\n");
        info.append("   失败生成次数: ").append(failedSpawns).append("\n");
        info.append("   当前占用位置数: ").append(occupiedPositions.size()).append("\n");
        info.append("   缓存区块数: ").append(passablePositionsCache.size()).append("\n");
        
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
        lastPlayerChunk = null;
        lastCacheUpdateTime = 0;
        System.out.println("🗑️ 敌人生成缓存已清理");
    }
}
