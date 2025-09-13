package com.roguelike.utils;

import com.roguelike.map.MapRenderer;
import com.roguelike.map.CollisionMap;
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
    private final AStarPathfinder aStarPathfinder;
    private final FlowField flowField;
    private final PathfindingConfig config;
    
    private int currentEnemyCount = 0;
    private PathfindingType currentAlgorithm = PathfindingType.ASTAR;
    private double lastUpdateTime = 0;
    
    /**
     * 构造函数
     */
    public AdaptivePathfinder(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
        this.config = new PathfindingConfig();
        
        // 初始化A*路径寻找器
        MapInterfaceAdapter mapAdapter = new MapInterfaceAdapter(mapRenderer);
        this.aStarPathfinder = new AStarPathfinder(mapAdapter, config.isAllowDiagonal());
        
        // 初始化流体算法
        this.flowField = new FlowField(
            mapRenderer.getMapWidth(), 
            mapRenderer.getTileWidth()
        );
        
        // 同步障碍物到流体算法
        syncObstaclesToFlowField();
    }
    
    /**
     * 构造函数，使用自定义配置
     */
    public AdaptivePathfinder(MapRenderer mapRenderer, PathfindingConfig config) {
        this.mapRenderer = mapRenderer;
        this.config = config;
        
        // 初始化A*路径寻找器
        MapInterfaceAdapter mapAdapter = new MapInterfaceAdapter(mapRenderer);
        this.aStarPathfinder = new AStarPathfinder(mapAdapter, config.isAllowDiagonal());
        
        // 初始化流体算法
        this.flowField = new FlowField(
            mapRenderer.getMapWidth(), 
            mapRenderer.getTileWidth()
        );
        
        // 同步障碍物到流体算法
        syncObstaclesToFlowField();
    }
    
    /**
     * 更新敌人数量并选择算法
     */
    public void updateEnemyCount(int enemyCount) {
        this.currentEnemyCount = enemyCount;
        
        PathfindingType newAlgorithm = (enemyCount < config.getEnemyCountThreshold()) 
            ? PathfindingType.ASTAR 
            : PathfindingType.FLOW_FIELD;
            
        if (newAlgorithm != currentAlgorithm) {
            currentAlgorithm = newAlgorithm;
            System.out.println("路径寻找算法切换: " + currentAlgorithm +
                             " (敌人数量: " + enemyCount + ")");
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
            // 使用流体算法
            flowField.setTarget(endTileX, endTileY);
            flowField.updateFlowField();
            
            // 流体算法返回从起点到终点的方向向量序列
            path = generateFlowFieldPath(startX, startY, endX, endY);
        }
        
        // 路径平滑处理
        if (config.isEnableSmoothing() && path.size() > 2) {
            path = smoothPath(path);
        }
        
        return path;
    }
    
    /**
     * 获取移动方向（用于流体算法）
     */
    public Point2D getMovementDirection(double currentX, double currentY) {
        if (currentAlgorithm == PathfindingType.FLOW_FIELD) {
            FlowField.Vector2D direction = flowField.getVectorAtWorldPos(currentX, currentY);
            return new Point2D(direction.x, direction.y);
        }
        return new Point2D(0, 0);
    }
    
    /**
     * 生成流体算法路径
     */
    private List<Point2D> generateFlowFieldPath(double startX, double startY, double endX, double endY) {
        List<Point2D> path = new ArrayList<>();
        double currentX = startX;
        double currentY = startY;
        
        path.add(new Point2D(currentX, currentY));
        
        // 最大步数限制，避免无限循环
        int maxSteps = 1000;
        int stepCount = 0;
        
        while (stepCount < maxSteps) {
            FlowField.Vector2D direction = flowField.getVectorAtWorldPos(currentX, currentY);
            
            if (direction.length() < 0.1) {
                break; // 到达目标或无法继续
            }
            
            // 移动一步
            double stepSize = mapRenderer.getTileWidth() * 0.5;
            currentX += direction.x * stepSize;
            currentY += direction.y * stepSize;
            
            path.add(new Point2D(currentX, currentY));
            
            // 检查是否接近目标
            double distanceToTarget = Math.sqrt(
                (currentX - endX) * (currentX - endX) + 
                (currentY - endY) * (currentY - endY)
            );
            
            if (distanceToTarget < mapRenderer.getTileWidth()) {
                path.add(new Point2D(endX, endY));
                break;
            }
            
            stepCount++;
        }
        
        return path;
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
    private void syncObstaclesToFlowField() {
        CollisionMap collisionMap = mapRenderer.getCollisionMap();
        if (collisionMap != null) {
            for (int y = 0; y < collisionMap.getHeight(); y++) {
                for (int x = 0; x < collisionMap.getWidth(); x++) {
                    boolean isObstacle = !collisionMap.isPassable(x, y);
                    flowField.setObstacle(x, y, isObstacle);
                }
            }
        }
    }
    
    /**
     * 更新路径寻找系统
     */
    public void update(double deltaTime) {
        lastUpdateTime += deltaTime;
        
        // 定期更新流体算法
        if (currentAlgorithm == PathfindingType.FLOW_FIELD && 
            lastUpdateTime >= config.getPathfindingUpdateInterval()) {
            flowField.updateFlowField();
            lastUpdateTime = 0;
        }
    }
    
    /**
     * 设置目标位置（用于流体算法）
     */
    public void setTarget(double worldX, double worldY) {
        int tileX = (int) (worldX / mapRenderer.getTileWidth());
        int tileY = (int) (worldY / mapRenderer.getTileHeight());
        flowField.setTarget(tileX, tileY);
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
     * 获取流体算法
     */
    public FlowField getFlowField() {
        return flowField;
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
}
