package com.roguelike.map.config;

import java.util.List;
import java.util.Map;

/**
 * 地图配置模型（与 configs/maps/map_config.json 对应）。
 */
public class MapConfig {
    public Integer tileSize;
    public Map<String, SingleMapConfig> maps;

    public static class SingleMapConfig {
        public String mode; // horizontal 或 four_direction

        // 作为默认区块尺寸（瓦片数）；若 dimensions 中存在更具体的条目，则以具体条目为准
        public Integer chunkWidthTiles;
        public Integer chunkHeightTiles;

        // 资源目录（可选），用于替代硬编码 map1/map2/map3 等
        public String assetsDir;

        // 初始量（可选）
        public Boolean useAsyncLoading;
        public Integer loadRadius;
        public Integer preloadRadius;

        // 特殊区块配置：door / boss -> 列表
        public Map<String, List<SpecialChunk>> specialChunks;

        // 维度表：mapName -> 维度
        public Map<String, MapDimensions> dimensions;
    }

    public static class MapDimensions {
        public Integer w; // 宽（瓦片）
        public Integer h; // 高（瓦片）
    }

    public static class SpecialChunk {
        public Integer x;
        public Integer y;
        public String map; // 该区块所用地图名（如 square_door / dungeon_boss）
        public String type; // 可选：door 或 boss（冗余，便于调试）
    }
}


