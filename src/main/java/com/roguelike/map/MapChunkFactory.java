package com.roguelike.map;

import com.roguelike.map.config.MapConfig;
import com.roguelike.map.config.MapConfigLoader;

import java.util.Map;

/**
 * 地图区块工厂类，管理不同地图的区块尺寸（优先来自 JSON 配置）。
 */
public class MapChunkFactory {
    
    private static final int DEFAULT_TILE_SIZE = 32;
    private static final Object tmxMetaLock = new Object();
    private static java.util.Map<String, int[]> tmxDimensionsCache = new java.util.HashMap<>(); // mapName -> [w,h]
    private static java.util.Map<String, Integer> tmxTileSizeCache = new java.util.HashMap<>(); // mapName -> tileSize
    
    private static int getTileSize() {
        // 以 TMX 为准：若已缓存任何一张 TMX 的 tileSize 且一致，则返回；否则回退配置；最终默认32。
        try {
            synchronized (tmxTileSizeCache) {
                if (!tmxTileSizeCache.isEmpty()) {
                    Integer v = tmxTileSizeCache.values().iterator().next();
                    if (v != null && v > 0) return v;
                }
            }
        } catch (Throwable ignored) {}
        try {
            MapConfig cfg = MapConfigLoader.load();
            if (cfg != null && cfg.tileSize != null && cfg.tileSize > 0) {
                return cfg.tileSize;
            }
        } catch (Throwable ignored) {}
        return DEFAULT_TILE_SIZE;
    }

    private static int[] getDimensionsFromConfig(String mapName) {
        try {
            MapConfig cfg = MapConfigLoader.load();
            if (cfg == null || cfg.maps == null) return null;
            // 遍历基础地图条目，查找 dimensions 表项
            for (Map.Entry<String, MapConfig.SingleMapConfig> e : cfg.maps.entrySet()) {
                MapConfig.SingleMapConfig m = e.getValue();
                if (m != null && m.dimensions != null && m.dimensions.containsKey(mapName)) {
                    MapConfig.MapDimensions d = m.dimensions.get(mapName);
                    if (d != null && d.w != null && d.h != null) {
                        return new int[]{d.w, d.h};
                    }
                }
            }
            // 若没找到具体条目，尝试使用基础条目的默认 chunk 宽高
            if (cfg.maps.containsKey(mapName)) {
                MapConfig.SingleMapConfig base = cfg.maps.get(mapName);
                if (base != null && base.chunkWidthTiles != null && base.chunkHeightTiles != null) {
                    return new int[]{base.chunkWidthTiles, base.chunkHeightTiles};
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * 获取指定地图的区块尺寸（瓦片数）。
     */
    public static int[] getMapDimensions(String mapName) {
        // 优先：TMX 宽高（瓦片数）
        int[] fromTMX = getDimensionsFromTMX(mapName);
        if (fromTMX != null) return fromTMX;
        // 次之：配置
        int[] fromCfg = getDimensionsFromConfig(mapName);
        if (fromCfg != null) return fromCfg;
        // 兜底：与原默认一致
        return new int[]{96, 54};
    }
    
    public static int getChunkWidth(String mapName) {
        return getMapDimensions(mapName)[0];
    }
    
    public static int getChunkHeight(String mapName) {
        return getMapDimensions(mapName)[1];
    }
    
    public static int getChunkWidthPixels(String mapName) {
        return getChunkWidth(mapName) * getTileSize();
    }
    
    public static int getChunkHeightPixels(String mapName) {
        return getChunkHeight(mapName) * getTileSize();
    }
    
    public static int worldToChunkX(double worldX, String mapName) {
        return (int) Math.floor(worldX / getChunkWidthPixels(mapName));
    }
    
    public static int worldToChunkY(double worldY, String mapName) {
        return (int) Math.floor(worldY / getChunkHeightPixels(mapName));
    }
    
    public static double chunkToWorldX(int chunkX, String mapName) {
        return chunkX * getChunkWidthPixels(mapName);
    }
    
    public static double chunkToWorldY(int chunkY, String mapName) {
        return chunkY * getChunkHeightPixels(mapName);
    }

    // ===== TMX 元数据读取 =====
    private static int[] getDimensionsFromTMX(String mapName) {
        synchronized (tmxMetaLock) {
            if (tmxDimensionsCache.containsKey(mapName)) {
                return tmxDimensionsCache.get(mapName);
            }
            try {
                String dir = mapNameToAssetsDir(mapName);
                String resourcePath = "/assets/maps/" + dir + "/" + mapName + ".tmx";
                java.io.InputStream in = MapChunkFactory.class.getResourceAsStream(resourcePath);
                if (in == null) return null;
                javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document doc = builder.parse(in);
                org.w3c.dom.Element mapElement = doc.getDocumentElement();
                int w = Integer.parseInt(mapElement.getAttribute("width"));
                int h = Integer.parseInt(mapElement.getAttribute("height"));
                int tileW = Integer.parseInt(mapElement.getAttribute("tilewidth"));
                int tileH = Integer.parseInt(mapElement.getAttribute("tileheight"));
                // 校验 tileSize = 32，若不一致，打印警告但仍以 TMX 为准（并缓存该值）
                if (tileW != 32 || tileH != 32) {
                    System.out.println("⚠️ TMX 瓦片尺寸非32: " + tileW + "x" + tileH + " (map=" + mapName + ")，仍以TMX为准");
                }
                if (tileW != tileH) {
                    System.out.println("⚠️ TMX 瓦片非正方形: " + tileW + "x" + tileH + " (map=" + mapName + ")");
                }
                tmxDimensionsCache.put(mapName, new int[]{w, h});
                tmxTileSizeCache.put(mapName, tileW);
                try { in.close(); } catch (Exception ignored) {}
                return new int[]{w, h};
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static String mapNameToAssetsDir(String mapName) {
        // 与 MapChunk 内部逻辑保持一致
        if (mapName.endsWith("_door") || mapName.endsWith("_boss")) {
            String baseName = mapName.substring(0, mapName.lastIndexOf("_"));
            return switch (baseName) {
                case "test" -> "map1";
                case "square" -> "map2";
                case "dungeon" -> "map3";
                default -> baseName;
            };
        }
        return switch (mapName) {
            case "test" -> "map1";
            case "square" -> "map2";
            case "dungeon" -> "map3";
            default -> mapName;
        };
    }
}
