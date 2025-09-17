package com.roguelike.map.strategy;

import java.util.ArrayList;
import java.util.List;

public class HorizontalStrategy implements MapModeStrategy {
    @Override
    public int[] normalizePlayerChunk(int chunkX, int chunkY) {
        return new int[]{chunkX, 0};
    }

    @Override
    public List<String> listChunksInRadius(int centerChunkX, int centerChunkY, int radius) {
        List<String> keys = new ArrayList<>();
        for (int x = centerChunkX - radius; x <= centerChunkX + radius; x++) {
            keys.add(x + ",0");
        }
        return keys;
    }

    @Override
    public boolean shouldUnload(int chunkX, int chunkY, int playerChunkX, int playerChunkY, int loadRadius) {
        int dx = Math.abs(chunkX - playerChunkX);
        return dx > loadRadius;
    }

    @Override
    public boolean isSpecialChunkAllowed(int chunkX, int chunkY) {
        return chunkY == 0;
    }
}


