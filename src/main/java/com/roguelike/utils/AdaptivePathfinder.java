package com.roguelike.utils;

import com.roguelike.map.MapRenderer;
import com.roguelike.map.CollisionMap;
import com.roguelike.map.InfiniteMapManager;
import javafx.geometry.Point2D;
import java.util.List;
import java.util.ArrayList;

/**
 * 自适应路径寻找系统
 * 根据敌人数量动态选择A*算法或流体算法
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class AdaptivePathfinder {
    
    /**
     * 路径寻找算法类型
     */
    public enum PathfindingType {
        ASTAR,      // A*算法
        FLOW_FIELD  // 流体算法
    }
    
    /**
     * 路径寻找配置
     */
    public static class PathfindingConfig {
        private int enemyCountThreshold = 10;  // 敌人数量阈值
        private boolean allowDiagonal = true;  // 是否允许对角线移动
        private double pathfindingUpdateInterval = 0.1; // 路径寻找更新间隔（秒）
        private boolean enablePathOptimization = true; // 是否启用路径优化
        private boolean enableSmoothing = true; // 是否启用路径平滑
        private boolean ignorePlayerAsObstacle = true; // 吸血鬼幸存者风格：不把玩家当作障碍物
        
        // Getters and Setters
        public int getEnemyCountThreshold() { return enemyCountThreshold; }
        public void setEnemyCountThreshold(int threshold) { this.enemyCountThreshold = threshold; }
        
        public boolean isAllowDiagonal() { return allowDiagonal; }
        public void setAllowDiagonal(boolean allowDiagonal) { this.allowDiagonal = allowDiagonal; }
        
        public double getPathfindingUpdateInterval() { return pathfindingUpdateInterval; }
        public void setPathfindingUpdateInterval(double interval) { this.pathfindingUpdateInterval = interval; }
        
        public boolean isEnablePathOptimization() { return enablePathOptimization; }
        public void setEnablePathOptimization(boolean enable) { this.enablePathOptimization = enable; }
        
        public boolean isEnableSmoothing() { return enableSmoothing; }
        public void setEnableSmoothing(boolean enable) { this.enableSmoothing = enable; }
        
        public boolean isIgnorePlayerAsObstacle() { return ignorePlayerAsObstacle; }
        public void setIgnorePlayerAsObstacle(boolean ignore) { this.ignorePlayerAsObstacle = ignore; }
    }
    
    private final MapRenderer mapRenderer;
    private final InfiniteMapManager infiniteMapManager;
    private final AStarPathfinder aStarPathfinder;
    private final PathfindingConfig config;
    
    private int currentEnemyCount = 0;
    private PathfindingType currentAlgorithm = PathfindingType.ASTAR;
    private boolean useInfiniteMap = false;
    
    /**
     * 构造函数
     */
    public AdaptivePathfinder(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
        this.infiniteMapManager = null;
        this.config = new PathfindingConfig();
        
        // 初始化A*路径寻找器
        MapInterfaceAdapter mapAdapter = new MapInterfaceAdapter(mapRenderer);
        this.aStarPathfinder = new AStarPathfinder(mapAdapter, config.isAllowDiagonal());
    }
    
    /**
     * 构造函数，使用自定义配置
     */
    public AdaptivePathfinder(MapRenderer mapRenderer, PathfindingConfig config) {
        this.mapRenderer = mapRenderer;
        this.infiniteMapManager = null;
        this.config = config;
        
        // 初始化A*路径寻找器
        MapInterfaceAdapter mapAdapter = new MapInterfaceAdapter(mapRenderer);
        this.aStarPathfinder = new AStarPathfinder(mapAdapter, config.isAllowDiagonal());
    }
    
    /**
     * 无限地图构造函数
     */
    public AdaptivePathfinder(InfiniteMapManager infiniteMapManager) {
        this.mapRenderer = null;
        this.infiniteMapManager = infiniteMapManager;
        this.config = new PathfindingConfig();
        this.useInfiniteMap = true;
        
        // 初始化A*路径寻找器（使用无限地图适配器）
        InfiniteMapInterfaceAdapter mapAdapter = new InfiniteMapInterfaceAdapter(infiniteMapManager);
        this.aStarPathfinder = new AStarPathfinder(mapAdapter, config.isAllowDiagonal());
        
        System.out.println("✅ 无限地图路径寻找系统初始化完成（仅A*算法）");
    }
    
    /**
     * 无限地图构造函数，使用自定义配置
     */
    public AdaptivePathfinder(InfiniteMapManager infiniteMapManager, PathfindingConfig config) {
        this.mapRenderer = null;
        this.infiniteMapManager = infiniteMapManager;
        this.config = config;
        this.useInfiniteMap = true;
        
        // 初始化A*路径寻找器（使用无限地图适配器）
        InfiniteMapInterfaceAdapter mapAdapter = new InfiniteMapInterfaceAdapter(infiniteMapManager);
        this.aStarPathfinder = new AStarPathfinder(mapAdapter, config.isAllowDiagonal());
        
        System.out.println("✅ 无限地图路径寻找系统初始化完成（仅A*算法）");
    }
    
    /**
     * 更新敌人数量并选择算法
     */
    public void updateEnemyCount(int enemyCount) {
        this.currentEnemyCount = enemyCount;
        
        // 始终使用A*算法，移除流场算法
        PathfindingType newAlgorithm = PathfindingType.ASTAR;
            
        if (newAlgorithm != currentAlgorithm) {
            currentAlgorithm = newAlgorithm;
        }
    }
    
    /**
     * 寻找路径
     */
    public List<Point2D> findPath(double startX, double startY, double endX, double endY) {
        return findPath(startX, startY, endX, endY, false);
    }
    
    /**
     * 寻找路径（带强制算法选择）
     */
    public List<Point2D> findPath(double startX, double startY, double endX, double endY, boolean forceAStar) {
        if (useInfiniteMap) {
            return findPathInfiniteMap(startX, startY, endX, endY, forceAStar);
        } else {
            return findPathTraditional(startX, startY, endX, endY, forceAStar);
        }
    }
    
    /**
     * 传统地图寻路
     */
    private List<Point2D> findPathTraditional(double startX, double startY, double endX, double endY, boolean forceAStar) {
        // 转换世界坐标到瓦片坐标
        int startTileX = (int) (startX / mapRenderer.getTileWidth());
        int startTileY = (int) (startY / mapRenderer.getTileHeight());
        int endTileX = (int) (endX / mapRenderer.getTileWidth());
        int endTileY = (int) (endY / mapRenderer.getTileHeight());
        
        List<Point2D> path = new ArrayList<>();
        
        if (forceAStar || currentAlgorithm == PathfindingType.ASTAR) {
            // 使用A*算法
            List<AStarPathfinder.Node> nodePath = aStarPathfinder.findPath(
                startTileX, startTileY, endTileX, endTileY);
            
            if (config.isEnablePathOptimization()) {
                nodePath = aStarPathfinder.optimizePath(nodePath);
            }
            
            // 转换为世界坐标
            for (AStarPathfinder.Node node : nodePath) {
                double worldX = node.x * mapRenderer.getTileWidth() + mapRenderer.getTileWidth() / 2.0;
                double worldY = node.y * mapRenderer.getTileHeight() + mapRenderer.getTileHeight() / 2.0;
                path.add(new Point2D(worldX, worldY));
            }
            
        } else {
            // 流场算法已移除，使用直接路径作为回退
            path = generateDirectPath(startX, startY, endX, endY);
        }
        
        // 路径平滑处理
        if (config.isEnableSmoothing() && path.size() > 2) {
            path = smoothPath(path);
        }
        
        return path;
    }
    
    /**
     * 无限地图寻路
     */
    private List<Point2D> findPathInfiniteMap(double startX, double startY, double endX, double endY, boolean forceAStar) {
        // 检查距离，如果超过一个区块距离，使用简化寻路
        double distance = Math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY));
        // 区块大小：96瓦片 * 32像素 = 3072像素宽，54瓦片 * 32像素 = 1728像素高
        // 使用半个区块宽度作为距离阈值，提高寻路响应速度
        double maxDistance = 48 * 32; // 1536像素
        
        if (distance > maxDistance) {
            // 远距离简化寻路：直接朝向目标移动
            return generateDirectPath(startX, startY, endX, endY);
        }
        
        // 近距离精细寻路：使用A*算法在邻近区块内寻路
        return findPathInNeighboringChunks(startX, startY, endX, endY);
    }
    
    /**
     * 在邻近区块内寻路
     */
    private List<Point2D> findPathInNeighboringChunks(double startX, double startY, double endX, double endY) {
        // 计算起点和终点所在的区块
        // 区块大小：96瓦片 * 32像素 = 3072像素宽，54瓦片 * 32像素 = 1728像素高
        int startChunkX = (int) (startX / (96 * 32)); // 区块X坐标
        int startChunkY = (int) (startY / (54 * 32)); // 区块Y坐标
        int endChunkX = (int) (endX / (96 * 32));     // 区块X坐标
        int endChunkY = (int) (endY / (54 * 32));     // 区块Y坐标
        
        // 检查是否在邻近区块内（3x3区块范围）
        if (Math.abs(startChunkX - endChunkX) > 1 || Math.abs(startChunkY - endChunkY) > 1) {
            // 超出邻近区块范围，使用简化寻路
            return generateDirectPath(startX, startY, endX, endY);
        }
        
        // 在邻近区块内，使用A*算法
        int startTileX = (int) (startX / 32);
        int startTileY = (int) (startY / 32);
        int endTileX = (int) (endX / 32);
        int endTileY = (int) (endY / 32);
        
        List<AStarPathfinder.Node> nodePath = aStarPathfinder.findPath(startTileX, startTileY, endTileX, endTileY);
        
        if (nodePath == null || nodePath.isEmpty()) {
            // A*寻路失败，回退到直接寻路
            return generateDirectPath(startX, startY, endX, endY);
        }
        
        // 转换为世界坐标
        List<Point2D> path = new ArrayList<>();
        for (AStarPathfinder.Node node : nodePath) {
            double worldX = node.x * 32 + 16; // 瓦片中心
            double worldY = node.y * 32 + 16;
            path.add(new Point2D(worldX, worldY));
        }
        
        
        return path;
    }
    
    /**
     * 生成直接路径（直线移动）
     */
    private List<Point2D> generateDirectPath(double startX, double startY, double endX, double endY) {
        List<Point2D> path = new ArrayList<>();
        path.add(new Point2D(startX, startY));
        path.add(new Point2D(endX, endY));
        return path;
    }
    
    /**
     * 获取移动方向（已移除流场算法，此方法保留用于兼容性）
     */
    public Point2D getMovementDirection(double currentX, double currentY) {
        // 流场算法已移除，返回零向量
        return new Point2D(0, 0);
    }
    
    
    /**
     * 路径平滑处理
     */
    private List<Point2D> smoothPath(List<Point2D> path) {
        if (path.size() < 3) {
            return path;
        }
        
        List<Point2D> smoothedPath = new ArrayList<>();
        smoothedPath.add(path.get(0)); // 添加起点
        
        for (int i = 1; i < path.size() - 1; i++) {
            Point2D prev = path.get(i - 1);
            Point2D curr = path.get(i);
            Point2D next = path.get(i + 1);
            
            // 简单的三点平滑
            double smoothX = (prev.getX() + curr.getX() + next.getX()) / 3.0;
            double smoothY = (prev.getY() + curr.getY() + next.getY()) / 3.0;
            
            smoothedPath.add(new Point2D(smoothX, smoothY));
        }
        
        smoothedPath.add(path.get(path.size() - 1)); // 添加终点
        return smoothedPath;
    }
    
    /**
     * 同步障碍物到流体算法
     */
    
    /**
     * 更新路径寻找系统
     */
    public void update(double deltaTime) {
        // 移除流场算法更新逻辑，只使用A*算法
        // 不需要定期更新
    }
    
    /**
     * 设置目标位置（已移除流场算法，此方法保留用于兼容性）
     */
    public void setTarget(double worldX, double worldY) {
        // 流场算法已移除，此方法保留用于兼容性
        // 不需要实际功能
    }
    
    /**
     * 获取当前使用的算法
     */
    public PathfindingType getCurrentAlgorithm() {
        return currentAlgorithm;
    }
    
    /**
     * 获取当前敌人数量
     */
    public int getCurrentEnemyCount() {
        return currentEnemyCount;
    }
    
    /**
     * 获取配置
     */
    public PathfindingConfig getConfig() {
        return config;
    }
    
    /**
     * 获取A*路径寻找器
     */
    public AStarPathfinder getAStarPathfinder() {
        return aStarPathfinder;
    }
    
    
    /**
     * 地图接口适配器，将MapRenderer适配到A*算法的MapInterface
     * 吸血鬼幸存者风格：不把玩家当作障碍物
     */
    private class MapInterfaceAdapter implements AStarPathfinder.MapInterface {
        private final MapRenderer mapRenderer;
        
        public MapInterfaceAdapter(MapRenderer mapRenderer) {
            this.mapRenderer = mapRenderer;
        }
        
        @Override
        public boolean isWalkable(int x, int y) {
            // 吸血鬼幸存者风格：只检查地图瓦片障碍物，不检查玩家位置
            // 敌人可以直接朝向玩家移动，不会被玩家阻挡
            return mapRenderer.isPassable(x, y);
        }
        
        @Override
        public int getMapWidth() {
            return mapRenderer.getMapWidth();
        }
        
        @Override
        public int getMapHeight() {
            return mapRenderer.getMapHeight();
        }
    }
    
    /**
     * 无限地图接口适配器，将InfiniteMapManager适配到A*算法的MapInterface
     * 支持跨区块寻路
     */
    private class InfiniteMapInterfaceAdapter implements AStarPathfinder.MapInterface {
        private final InfiniteMapManager infiniteMapManager;
        
        public InfiniteMapInterfaceAdapter(InfiniteMapManager infiniteMapManager) {
            this.infiniteMapManager = infiniteMapManager;
        }
        
        @Override
        public boolean isWalkable(int x, int y) {
            // 将瓦片坐标转换为世界坐标
            // 使用正确的瓦片大小：32像素
            double worldX = x * 32.0;
            double worldY = y * 32.0;
            
            // 检查是否在邻近区块范围内
            boolean passable = infiniteMapManager.isPassable(worldX, worldY);
            
            
            return passable;
        }
        
        @Override
        public int getMapWidth() {
            // 返回一个足够大的值，支持跨区块寻路
            return 1000; // 足够大的值
        }
        
        @Override
        public int getMapHeight() {
            // 返回一个足够大的值，支持跨区块寻路
            return 1000; // 足够大的值
        }
    }
}
