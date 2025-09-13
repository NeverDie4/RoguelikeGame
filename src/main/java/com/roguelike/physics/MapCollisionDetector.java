package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import com.roguelike.map.MapRenderer;
import com.roguelike.map.InfiniteMapManager;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

/**
 * 地图碰撞检测器
 * 负责检测实体与地图瓦片的碰撞，支持无限地图
 */
public class MapCollisionDetector {
    
    private MapRenderer mapRenderer;
    private InfiniteMapManager infiniteMapManager;
    
    public MapCollisionDetector(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
        this.infiniteMapManager = null;
    }
    
    public MapCollisionDetector(InfiniteMapManager infiniteMapManager) {
        this.mapRenderer = null;
        this.infiniteMapManager = infiniteMapManager;
    }
    
    /**
     * 检查实体是否可以移动到指定位置
     */
    public boolean canMoveTo(Entity entity, double newX, double newY) {
        if (infiniteMapManager != null) {
            // 使用无限地图管理器
            return checkInfiniteMapCollision(entity, newX, newY);
        } else if (mapRenderer != null) {
            // 使用传统地图渲染器
            Rectangle2D entityBounds = getEntityBounds(entity, newX, newY);
            return checkEntityBoundsCollision(entityBounds);
        } else {
            return true; // 没有地图时允许移动
        }
    }
    
    /**
     * 检查实体移动后的位置是否与地图碰撞
     */
    public boolean checkMovementCollision(Entity entity, double deltaX, double deltaY) {
        double newX = entity.getX() + deltaX;
        double newY = entity.getY() + deltaY;
        
        return canMoveTo(entity, newX, newY);
    }
    
    /**
     * 检查无限地图碰撞
     */
    private boolean checkInfiniteMapCollision(Entity entity, double newX, double newY) {
        // 获取实体的碰撞框
        Rectangle2D entityBounds = getEntityBounds(entity, newX, newY);
        
        // 计算实体碰撞框覆盖的瓦片范围
        int startTileX = (int) Math.floor(entityBounds.getMinX() / 32); // 瓦片尺寸32
        int startTileY = (int) Math.floor(entityBounds.getMinY() / 32);
        int endTileX = (int) Math.ceil(entityBounds.getMaxX() / 32);
        int endTileY = (int) Math.ceil(entityBounds.getMaxY() / 32);
        
        // 检查所有覆盖的瓦片
        for (int tileY = startTileY; tileY < endTileY; tileY++) {
            for (int tileX = startTileX; tileX < endTileX; tileX++) {
                double worldX = tileX * 32.0;
                double worldY = tileY * 32.0;
                
                if (infiniteMapManager.isUnaccessible(worldX, worldY)) {
                    return false; // 发现不可通行的瓦片
                }
            }
        }
        
        return true; // 所有瓦片都可通行
    }
    
    /**
     * 获取实体在指定位置的碰撞框
     */
    private Rectangle2D getEntityBounds(Entity entity, double x, double y) {
        // 使用实体的边界框组件
        if (entity.getBoundingBoxComponent() != null) {
            return new Rectangle2D(
                x + entity.getBoundingBoxComponent().getMinXLocal(),
                y + entity.getBoundingBoxComponent().getMinYLocal(),
                entity.getBoundingBoxComponent().getWidth(),
                entity.getBoundingBoxComponent().getHeight()
            );
        } else {
            // 回退到使用实体的宽度和高度
            return new Rectangle2D(x, y, entity.getWidth(), entity.getHeight());
        }
    }
    
    /**
     * 检查实体碰撞框是否与地图碰撞
     */
    private boolean checkEntityBoundsCollision(Rectangle2D entityBounds) {
        // 计算实体碰撞框覆盖的瓦片范围
        int startTileX = (int) Math.floor(entityBounds.getMinX() / mapRenderer.getTileWidth());
        int startTileY = (int) Math.floor(entityBounds.getMinY() / mapRenderer.getTileHeight());
        int endTileX = (int) Math.ceil(entityBounds.getMaxX() / mapRenderer.getTileWidth());
        int endTileY = (int) Math.ceil(entityBounds.getMaxY() / mapRenderer.getTileHeight());
        
        // 检查所有覆盖的瓦片
        for (int tileY = startTileY; tileY < endTileY; tileY++) {
            for (int tileX = startTileX; tileX < endTileX; tileX++) {
                if (!mapRenderer.isPassable(tileX, tileY)) {
                    return false; // 发现不可通行的瓦片
                }
            }
        }
        
        return true; // 所有瓦片都可通行
    }
    
    /**
     * 获取实体可以移动的最大距离（在指定方向上）
     */
    public double getMaxMoveDistance(Entity entity, double directionX, double directionY, double maxDistance) {
        if (directionX == 0 && directionY == 0) {
            return 0;
        }
        
        // 归一化方向向量
        double length = Math.sqrt(directionX * directionX + directionY * directionY);
        directionX /= length;
        directionY /= length;
        
        // 使用步进方式检查
        double step = maxDistance / 20.0; // 分成20步检查，提高精度
        double maxDistanceFound = 0;
        
        for (double distance = 0; distance <= maxDistance; distance += step) {
            double testX = entity.getX() + directionX * distance;
            double testY = entity.getY() + directionY * distance;
            
            if (canMoveTo(entity, testX, testY)) {
                maxDistanceFound = distance;
            } else {
                break; // 遇到障碍物，停止
            }
        }
        
        return maxDistanceFound;
    }
    
    /**
     * 将实体推离最近的障碍物
     */
    public Point2D pushAwayFromObstacles(Entity entity) {
        Rectangle2D entityBounds = getEntityBounds(entity, entity.getX(), entity.getY());
        
        // 计算实体碰撞框覆盖的瓦片范围
        int startTileX = (int) Math.floor(entityBounds.getMinX() / mapRenderer.getTileWidth());
        int startTileY = (int) Math.floor(entityBounds.getMinY() / mapRenderer.getTileHeight());
        int endTileX = (int) Math.ceil(entityBounds.getMaxX() / mapRenderer.getTileWidth());
        int endTileY = (int) Math.ceil(entityBounds.getMaxY() / mapRenderer.getTileHeight());
        
        double pushX = 0;
        double pushY = 0;
        
        // 检查所有覆盖的瓦片，计算推离向量
        for (int tileY = startTileY; tileY < endTileY; tileY++) {
            for (int tileX = startTileX; tileX < endTileX; tileX++) {
                if (!mapRenderer.isPassable(tileX, tileY)) {
                    // 计算瓦片中心
                    double tileCenterX = (tileX + 0.5) * mapRenderer.getTileWidth();
                    double tileCenterY = (tileY + 0.5) * mapRenderer.getTileHeight();
                    
                    // 计算实体中心
                    double entityCenterX = entityBounds.getMinX() + entityBounds.getWidth() / 2;
                    double entityCenterY = entityBounds.getMinY() + entityBounds.getHeight() / 2;
                    
                    // 计算推离方向
                    double deltaX = entityCenterX - tileCenterX;
                    double deltaY = entityCenterY - tileCenterY;
                    
                    // 归一化并累加推离向量
                    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    if (distance > 0) {
                        pushX += deltaX / distance;
                        pushY += deltaY / distance;
                    }
                }
            }
        }
        
        // 归一化推离向量
        double pushLength = Math.sqrt(pushX * pushX + pushY * pushY);
        if (pushLength > 0) {
            pushX /= pushLength;
            pushY /= pushLength;
            
            // 应用推离距离
            double pushDistance = 2.0; // 推离2个像素
            return new Point2D(pushX * pushDistance, pushY * pushDistance);
        }
        
        return new Point2D(0, 0);
    }
    
    /**
     * 检查实体是否完全在地图边界内
     */
    public boolean isWithinMapBounds(Entity entity, double x, double y) {
        Rectangle2D entityBounds = getEntityBounds(entity, x, y);
        
        if (infiniteMapManager != null) {
            // 无限地图：只检查Y轴边界（上下封死）
            double mapHeight = InfiniteMapManager.getChunkHeightPixels();
            return entityBounds.getMinY() >= 0 && entityBounds.getMaxY() <= mapHeight;
        } else if (mapRenderer != null) {
            // 传统地图：检查完整边界
            double mapWidth = mapRenderer.getMapWidth() * mapRenderer.getTileWidth();
            double mapHeight = mapRenderer.getMapHeight() * mapRenderer.getTileHeight();
            
            return entityBounds.getMinX() >= 0 && 
                   entityBounds.getMinY() >= 0 && 
                   entityBounds.getMaxX() <= mapWidth && 
                   entityBounds.getMaxY() <= mapHeight;
        }
        
        return true; // 没有地图时允许移动
    }
    
    /**
     * 获取MapRenderer实例
     */
    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }
    
    /**
     * 设置MapRenderer实例
     */
    public void setMapRenderer(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
        this.infiniteMapManager = null; // 切换到传统地图模式
    }
    
    /**
     * 获取InfiniteMapManager实例
     */
    public InfiniteMapManager getInfiniteMapManager() {
        return infiniteMapManager;
    }
    
    /**
     * 设置InfiniteMapManager实例
     */
    public void setInfiniteMapManager(InfiniteMapManager infiniteMapManager) {
        this.infiniteMapManager = infiniteMapManager;
        this.mapRenderer = null; // 切换到无限地图模式
    }
}
