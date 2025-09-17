package com.roguelike.map.strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图模式策略接口：抽象横向与四向无限地图的差异。
 */
public interface MapModeStrategy {

    /**
     * 规范化玩家区块坐标（例如横向模式强制 Y=0）。
     */
    default int[] normalizePlayerChunk(int chunkX, int chunkY) {
        return new int[]{chunkX, chunkY};
    }

    /**
     * 枚举以中心为半径的区块键列表（key: "x,y"）。
     */
    List<String> listChunksInRadius(int centerChunkX, int centerChunkY, int radius);

    /**
     * 判断区块是否应当卸载。
     */
    boolean shouldUnload(int chunkX, int chunkY, int playerChunkX, int playerChunkY, int loadRadius);

    /**
     * 特殊区块是否允许（横向仅允许 y=0）。
     */
    boolean isSpecialChunkAllowed(int chunkX, int chunkY);
}


