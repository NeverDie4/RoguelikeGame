package com.roguelike.map.strategy;

import java.util.ArrayList;
import java.util.List;

public class FourDirectionalStrategy implements MapModeStrategy {
    @Override
    public List<String> listChunksInRadius(int centerChunkX, int centerChunkY, int radius) {
        List<String> keys = new ArrayList<>();
        for (int y = centerChunkY - radius; y <= centerChunkY + radius; y++) {
            for (int x = centerChunkX - radius; x <= centerChunkX + radius; x++) {
                keys.add(x + "," + y);
            }
        }
        return keys;
    }

    @Override
    public boolean shouldUnload(int chunkX, int chunkY, int playerChunkX, int playerChunkY, int loadRadius) {
        int dx = Math.abs(chunkX - playerChunkX);
        int dy = Math.abs(chunkY - playerChunkY);
        return dx > loadRadius || dy > loadRadius;
    }

    @Override
    public boolean isSpecialChunkAllowed(int chunkX, int chunkY) {
        return true;
    }
}


