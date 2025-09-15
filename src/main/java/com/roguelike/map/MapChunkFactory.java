package com.roguelike.map;

import java.util.Map;

/**
 * 地图区块工厂类，管理不同地图的区块尺寸
 */
public class MapChunkFactory {
    
    private static final int TILE_SIZE = 32;
    
    // 不同地图的区块尺寸配置
    private static final Map<String, int[]> MAP_DIMENSIONS = Map.of(
        "test", new int[]{96, 54},      // test地图：96x54
        "square", new int[]{50, 50},    // square地图：50x50
        "test_door", new int[]{96, 54}, // test_door地图：96x54
        "test_boss", new int[]{96, 54}, // test_boss地图：96x54
        "square_door", new int[]{50, 50}, // square_door地图：50x50
        "square_boss", new int[]{50, 50}  // square_boss地图：30x30（实际文件尺寸）
    );
    
    /**
     * 获取指定地图的区块尺寸
     */
    public static int[] getMapDimensions(String mapName) {
        return MAP_DIMENSIONS.getOrDefault(mapName, new int[]{96, 54}); // 默认96x54
    }
    
    /**
     * 获取指定地图的区块宽度（瓦片数）
     */
    public static int getChunkWidth(String mapName) {
        return getMapDimensions(mapName)[0];
    }
    
    /**
     * 获取指定地图的区块高度（瓦片数）
     */
    public static int getChunkHeight(String mapName) {
        return getMapDimensions(mapName)[1];
    }
    
    /**
     * 获取指定地图的区块宽度（像素）
     */
    public static int getChunkWidthPixels(String mapName) {
        return getChunkWidth(mapName) * TILE_SIZE;
    }
    
    /**
     * 获取指定地图的区块高度（像素）
     */
    public static int getChunkHeightPixels(String mapName) {
        return getChunkHeight(mapName) * TILE_SIZE;
    }
    
    /**
     * 世界坐标转区块X坐标
     */
    public static int worldToChunkX(double worldX, String mapName) {
        return (int) Math.floor(worldX / getChunkWidthPixels(mapName));
    }
    
    /**
     * 世界坐标转区块Y坐标
     */
    public static int worldToChunkY(double worldY, String mapName) {
        return (int) Math.floor(worldY / getChunkHeightPixels(mapName));
    }
    
    /**
     * 区块X坐标转世界坐标
     */
    public static double chunkToWorldX(int chunkX, String mapName) {
        return chunkX * getChunkWidthPixels(mapName);
    }
    
    /**
     * 区块Y坐标转世界坐标
     */
    public static double chunkToWorldY(int chunkY, String mapName) {
        return chunkY * getChunkHeightPixels(mapName);
    }
}
