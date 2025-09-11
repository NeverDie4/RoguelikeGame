package com.roguelike.map;

/**
 * 碰撞地图类，用于管理地图的碰撞数据
 */
public class CollisionMap {
    
    private int width;        // 地图宽度（格数）
    private int height;       // 地图高度（格数）
    private boolean[][] collisionData; // 碰撞数据：true表示不可通行
    
    public CollisionMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.collisionData = new boolean[height][width];
    }
    
    /**
     * 设置指定位置的碰撞状态
     */
    public void setCollision(int x, int y, boolean isCollision) {
        if (isValidPosition(x, y)) {
            collisionData[y][x] = isCollision;
        }
    }
    
    /**
     * 检查指定位置是否有碰撞
     */
    public boolean hasCollision(int x, int y) {
        if (!isValidPosition(x, y)) {
            return true; // 超出边界视为碰撞
        }
        return collisionData[y][x];
    }
    
    /**
     * 检查指定位置是否可通行
     */
    public boolean isPassable(int x, int y) {
        return !hasCollision(x, y);
    }
    
    /**
     * 检查指定位置是否不可通行
     */
    public boolean isUnaccessible(int x, int y) {
        return hasCollision(x, y);
    }
    
    /**
     * 验证坐标是否有效
     */
    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
    
    /**
     * 获取地图宽度
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * 获取地图高度
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * 清空所有碰撞数据
     */
    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                collisionData[y][x] = false;
            }
        }
    }
    
    /**
     * 从TiledMap构建碰撞地图
     */
    public static CollisionMap fromTiledMap(TiledMap tiledMap) {
        if (tiledMap == null || tiledMap.getLayers().isEmpty()) {
            return new CollisionMap(0, 0);
        }
        
        CollisionMap collisionMap = new CollisionMap(tiledMap.getWidth(), tiledMap.getHeight());
        
        // 遍历所有图层
        for (Layer layer : tiledMap.getLayers()) {
            // 遍历图层中的每个瓦片
            for (int y = 0; y < layer.getHeight(); y++) {
                for (int x = 0; x < layer.getWidth(); x++) {
                    int index = y * layer.getWidth() + x;
                    int gid = layer.getData().get(index);
                    
                    if (gid > 0) {
                        // 找到对应的瓦片集
                        Tileset tileset = findTilesetForGid(tiledMap, gid);
                        if (tileset != null) {
                            int localId = gid - tileset.getFirstgid();
                            boolean isUnaccessible = tileset.isTileUnaccessible(localId);
                            
                            // 如果当前瓦片不可通行，设置碰撞
                            if (isUnaccessible) {
                                collisionMap.setCollision(x, y, true);
                            }
                        }
                    }
                }
            }
        }
        
        return collisionMap;
    }
    
    /**
     * 根据GID找到对应的瓦片集
     */
    private static Tileset findTilesetForGid(TiledMap tiledMap, int gid) {
        for (Tileset tileset : tiledMap.getTilesets()) {
            if (gid >= tileset.getFirstgid() && gid < tileset.getFirstgid() + tileset.getTilecount()) {
                return tileset;
            }
        }
        return null;
    }
    
    /**
     * 打印碰撞地图（用于调试）
     */
    public void printCollisionMap() {
        System.out.println("碰撞地图 (" + width + "x" + height + "):");
        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < width; x++) {
                line.append(collisionData[y][x] ? "X" : ".");
            }
            System.out.println(line.toString());
        }
    }
}
