package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.*;

/**
 * 空间分割系统
 * 使用网格分割来优化碰撞检测性能
 */
public class SpatialPartitionSystem {
    
    // 网格大小（可配置）
    private static final int GRID_SIZE = 100;
    
    // 空间网格存储
    private Map<String, List<Entity>> spatialGrid = new HashMap<>();
    
    // 调试模式
    private boolean debugMode = false;
    private List<Rectangle> debugGrid = new ArrayList<>();
    
    /**
     * 更新实体在空间网格中的位置
     */
    public void updateEntity(Entity entity) {
        if (entity == null) return;
        
        String gridKey = getGridKey(entity.getX(), entity.getY());
        spatialGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(entity);
    }
    
    /**
     * 获取指定实体附近的实体列表
     * @param entity 中心实体
     * @param radius 搜索半径
     * @return 附近的实体列表
     */
    public List<Entity> getNearbyEntities(Entity entity, double radius) {
        if (entity == null) return new ArrayList<>();
        
        List<Entity> nearby = new ArrayList<>();
        int gridRadius = (int) Math.ceil(radius / GRID_SIZE);
        
        // 获取中心网格坐标
        int centerGridX = (int) (entity.getX() / GRID_SIZE);
        int centerGridY = (int) (entity.getY() / GRID_SIZE);
        
        // 搜索周围网格
        for (int x = centerGridX - gridRadius; x <= centerGridX + gridRadius; x++) {
            for (int y = centerGridY - gridRadius; y <= centerGridY + gridRadius; y++) {
                String key = x + "," + y;
                List<Entity> entities = spatialGrid.get(key);
                if (entities != null) {
                    // 过滤掉自身，并检查实际距离
                    for (Entity nearbyEntity : entities) {
                        if (!nearbyEntity.equals(entity)) {
                            double distance = entity.getCenter().distance(nearbyEntity.getCenter());
                            if (distance <= radius) {
                                nearby.add(nearbyEntity);
                            }
                        }
                    }
                }
            }
        }
        
        return nearby;
    }
    
    /**
     * 获取指定位置附近的实体列表
     * @param x 中心X坐标
     * @param y 中心Y坐标
     * @param radius 搜索半径
     * @return 附近的实体列表
     */
    public List<Entity> getNearbyEntities(double x, double y, double radius) {
        List<Entity> nearby = new ArrayList<>();
        int gridRadius = (int) Math.ceil(radius / GRID_SIZE);
        
        // 获取中心网格坐标
        int centerGridX = (int) (x / GRID_SIZE);
        int centerGridY = (int) (y / GRID_SIZE);
        
        // 搜索周围网格
        for (int gridX = centerGridX - gridRadius; gridX <= centerGridX + gridRadius; gridX++) {
            for (int gridY = centerGridY - gridRadius; gridY <= centerGridY + gridRadius; gridY++) {
                String key = gridX + "," + gridY;
                List<Entity> entities = spatialGrid.get(key);
                if (entities != null) {
                    // 检查实际距离
                    for (Entity entity : entities) {
                        double distance = Math.sqrt(
                            Math.pow(entity.getX() - x, 2) + Math.pow(entity.getY() - y, 2)
                        );
                        if (distance <= radius) {
                            nearby.add(entity);
                        }
                    }
                }
            }
        }
        
        return nearby;
    }
    
    /**
     * 获取指定网格中的所有实体
     * @param gridX 网格X坐标
     * @param gridY 网格Y坐标
     * @return 该网格中的实体列表
     */
    public List<Entity> getEntitiesInGrid(int gridX, int gridY) {
        String key = gridX + "," + gridY;
        List<Entity> entities = spatialGrid.get(key);
        return entities != null ? new ArrayList<>(entities) : new ArrayList<>();
    }
    
    /**
     * 获取指定网格中的所有实体
     * @param worldX 世界X坐标
     * @param worldY 世界Y坐标
     * @return 该网格中的实体列表
     */
    public List<Entity> getEntitiesInGrid(double worldX, double worldY) {
        int gridX = (int) (worldX / GRID_SIZE);
        int gridY = (int) (worldY / GRID_SIZE);
        return getEntitiesInGrid(gridX, gridY);
    }
    
    /**
     * 清除所有空间网格数据
     */
    public void clear() {
        spatialGrid.clear();
    }
    
    /**
     * 获取网格大小
     */
    public static int getGridSize() {
        return GRID_SIZE;
    }
    
    /**
     * 将世界坐标转换为网格坐标
     */
    public static int worldToGrid(double worldCoord) {
        return (int) (worldCoord / GRID_SIZE);
    }
    
    /**
     * 将网格坐标转换为世界坐标
     */
    public static double gridToWorld(int gridCoord) {
        return gridCoord * GRID_SIZE;
    }
    
    /**
     * 生成网格键值
     */
    private String getGridKey(double x, double y) {
        int gridX = (int) (x / GRID_SIZE);
        int gridY = (int) (y / GRID_SIZE);
        return gridX + "," + gridY;
    }
    
    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        if (!debugMode) {
            clearDebugGrid();
        }
    }
    
    /**
     * 更新调试网格可视化
     */
    public void updateDebugGrid() {
        if (!debugMode) return;
        
        clearDebugGrid();
        
        // 获取当前视口范围
        double viewportWidth = 1280; // 假设视口宽度
        double viewportHeight = 720; // 假设视口高度
        
        // 计算需要显示的网格范围
        int startGridX = 0;
        int startGridY = 0;
        int endGridX = (int) Math.ceil(viewportWidth / GRID_SIZE);
        int endGridY = (int) Math.ceil(viewportHeight / GRID_SIZE);
        
        // 创建调试网格
        for (int x = startGridX; x <= endGridX; x++) {
            for (int y = startGridY; y <= endGridY; y++) {
                Rectangle gridRect = new Rectangle(GRID_SIZE, GRID_SIZE);
                gridRect.setFill(Color.TRANSPARENT);
                gridRect.setStroke(Color.YELLOW);
                gridRect.setStrokeWidth(1);
                gridRect.setX(x * GRID_SIZE);
                gridRect.setY(y * GRID_SIZE);
                gridRect.setOpacity(0.3);
                debugGrid.add(gridRect);
            }
        }
    }
    
    /**
     * 清除调试网格
     */
    private void clearDebugGrid() {
        debugGrid.clear();
    }
    
    /**
     * 获取调试网格
     */
    public List<Rectangle> getDebugGrid() {
        return debugGrid;
    }
    
    /**
     * 获取调试信息
     */
    public String getDebugInfo() {
        int totalEntities = spatialGrid.values().stream()
            .mapToInt(List::size)
            .sum();
        
        int occupiedGrids = spatialGrid.size();
        
        return String.format("空间分割调试信息:\n" +
                           "  - 网格大小: %d x %d\n" +
                           "  - 占用网格数: %d\n" +
                           "  - 总实体数: %d\n" +
                           "  - 平均每网格实体数: %.2f",
                           GRID_SIZE, GRID_SIZE, occupiedGrids, totalEntities,
                           occupiedGrids > 0 ? (double) totalEntities / occupiedGrids : 0);
    }
}
