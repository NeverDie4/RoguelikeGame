package com.roguelike.map;

/**
 * 地图运行期配置与开关（轻量，来源：系统属性/默认值）。
 */
public final class MapRuntimeConfig {

    private MapRuntimeConfig() { }

    /** 是否使用基于 FXGL Tile 的新实现（默认 true） */
    public static boolean useFxglTileMap() {
        try {
            String v = System.getProperty("useFxglTileMap");
            if (v == null || v.isEmpty()) return true;
            return Boolean.parseBoolean(v);
        } catch (Throwable ignored) {
            return true;
        }
    }
}


