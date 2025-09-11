package com.roguelike.map;

/**
 * 碰撞系统使用示例
 * 展示如何使用新的地图碰撞检测功能
 */
public class CollisionExample {
    
    private MapRenderer mapRenderer;
    
    public CollisionExample(MapRenderer mapRenderer) {
        this.mapRenderer = mapRenderer;
    }
    
    /**
     * 示例：检查玩家移动是否合法
     */
    public boolean canPlayerMoveTo(int newX, int newY) {
        // 将世界坐标转换为瓦片坐标
        int tileX = newX / mapRenderer.getTileWidth();
        int tileY = newY / mapRenderer.getTileHeight();
        
        // 检查目标位置是否可通行
        return mapRenderer.isPassable(tileX, tileY);
    }
    
    /**
     * 示例：检查矩形区域是否可通行
     */
    public boolean isAreaPassable(int startX, int startY, int width, int height) {
        int tileStartX = startX / mapRenderer.getTileWidth();
        int tileStartY = startY / mapRenderer.getTileHeight();
        int tileWidth = width / mapRenderer.getTileWidth();
        int tileHeight = height / mapRenderer.getTileHeight();
        
        // 检查矩形区域内的所有瓦片
        for (int y = tileStartY; y < tileStartY + tileHeight; y++) {
            for (int x = tileStartX; x < tileStartX + tileWidth; x++) {
                if (!mapRenderer.isPassable(x, y)) {
                    return false; // 发现不可通行的瓦片
                }
            }
        }
        return true; // 所有瓦片都可通行
    }
    
    /**
     * 示例：查找最近的可行走位置
     */
    public int[] findNearestPassablePosition(int centerX, int centerY, int maxRadius) {
        int tileCenterX = centerX / mapRenderer.getTileWidth();
        int tileCenterY = centerY / mapRenderer.getTileHeight();
        
        // 从中心向外搜索
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int x = tileCenterX - radius; x <= tileCenterX + radius; x++) {
                for (int y = tileCenterY - radius; y <= tileCenterY + radius; y++) {
                    // 检查是否在半径范围内
                    if (Math.abs(x - tileCenterX) == radius || Math.abs(y - tileCenterY) == radius) {
                        if (mapRenderer.isPassable(x, y)) {
                            // 转换回世界坐标
                            return new int[]{
                                x * mapRenderer.getTileWidth(),
                                y * mapRenderer.getTileHeight()
                            };
                        }
                    }
                }
            }
        }
        
        return null; // 未找到可行走位置
    }
    
    /**
     * 示例：调试碰撞地图
     */
    public void debugCollisionMap() {
        System.out.println("=== 碰撞地图调试信息 ===");
        
        // 打印整体信息
        mapRenderer.printCollisionInfo();
        
        // 检查地图中心区域
        int centerX = mapRenderer.getMapWidth() / 2;
        int centerY = mapRenderer.getMapHeight() / 2;
        int checkSize = 10;
        
        System.out.println("\n=== 地图中心区域 (" + centerX + "," + centerY + ") ===");
        mapRenderer.checkAreaPassability(
            centerX - checkSize/2, 
            centerY - checkSize/2, 
            checkSize, 
            checkSize
        );
        
        // 检查地图角落
        System.out.println("\n=== 地图左上角区域 ===");
        mapRenderer.checkAreaPassability(0, 0, 10, 10);
        
        System.out.println("\n=== 地图右下角区域 ===");
        mapRenderer.checkAreaPassability(
            mapRenderer.getMapWidth() - 10, 
            mapRenderer.getMapHeight() - 10, 
            10, 
            10
        );
    }
}
