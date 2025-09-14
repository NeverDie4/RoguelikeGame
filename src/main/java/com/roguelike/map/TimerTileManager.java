package com.roguelike.map;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * 定时器瓦片管理器，管理有timer属性的瓦片
 * 这些瓦片原本不可通行，在指定时间后变为可通行
 */
public class TimerTileManager {
    
    // 定时器瓦片信息
    private static class TimerTileInfo {
        public final MapChunk chunk;
        public final int tileX;
        public final int tileY;
        public final int gid;
        public final int timerSeconds;
        public final long startTime;
        
        public TimerTileInfo(MapChunk chunk, int tileX, int tileY, int gid, int timerSeconds) {
            this.chunk = chunk;
            this.tileX = tileX;
            this.tileY = tileY;
            this.gid = gid;
            this.timerSeconds = timerSeconds;
            this.startTime = System.currentTimeMillis();
        }
        
        /**
         * 检查定时器是否已到期
         */
        public boolean isExpired() {
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - startTime) / 1000;
            return elapsedSeconds >= timerSeconds;
        }
        
        /**
         * 获取剩余时间（秒）
         */
        public int getRemainingTime() {
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - startTime) / 1000;
            return Math.max(0, timerSeconds - (int)elapsedSeconds);
        }
    }
    
    // 存储所有定时器瓦片
    private Map<String, TimerTileInfo> timerTiles = new HashMap<>();
    
    // 已过期的瓦片列表（用于清理）
    private List<String> expiredTiles = new ArrayList<>();
    
    /**
     * 扫描地图区块，查找并注册定时器瓦片
     */
    public void scanChunkForTimerTiles(MapChunk chunk) {
        if (chunk == null || chunk.getTiledMap() == null) {
            return;
        }
        
        System.out.println("🔍 扫描区块 " + chunk.getChunkX() + " 中的定时器瓦片...");
        
        // 遍历所有图层
        for (Layer layer : chunk.getTiledMap().getLayers()) {
            for (int y = 0; y < layer.getHeight(); y++) {
                for (int x = 0; x < layer.getWidth(); x++) {
                    int index = y * layer.getWidth() + x;
                    if (index < layer.getData().size()) {
                        int gid = layer.getData().get(index);
                        
                        if (gid > 0) {
                            // 查找对应的瓦片集和属性
                            for (Tileset tileset : chunk.getTiledMap().getTilesets()) {
                                if (gid >= tileset.getFirstgid() && gid < tileset.getFirstgid() + tileset.getTilecount()) {
                                    int localId = gid - tileset.getFirstgid();
                                    TileProperty property = tileset.getTileProperty(localId);
                                    
                                    if (property != null && property.isTimerTile()) {
                                        // 找到定时器瓦片，注册它
                                        registerTimerTile(chunk, x, y, gid, property.getTimer());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 注册定时器瓦片
     */
    private void registerTimerTile(MapChunk chunk, int tileX, int tileY, int gid, int timerSeconds) {
        String key = generateKey(chunk.getChunkX(), tileX, tileY);
        
        if (!timerTiles.containsKey(key)) {
            TimerTileInfo timerInfo = new TimerTileInfo(chunk, tileX, tileY, gid, timerSeconds);
            timerTiles.put(key, timerInfo);
            
            System.out.println("⏰ 注册定时器瓦片: 区块" + chunk.getChunkX() + 
                             " 位置(" + tileX + "," + tileY + ") " + 
                             "GID" + gid + " 定时" + timerSeconds + "秒");
        }
    }
    
    /**
     * 更新所有定时器瓦片
     */
    public void update() {
        expiredTiles.clear();
        
        for (Map.Entry<String, TimerTileInfo> entry : timerTiles.entrySet()) {
            TimerTileInfo timerInfo = entry.getValue();
            
            if (timerInfo.isExpired()) {
                // 定时器到期，标记为可通行
                makeTilePassable(timerInfo);
                expiredTiles.add(entry.getKey());
                
                System.out.println("✅ 定时器瓦片到期: 区块" + timerInfo.chunk.getChunkX() + 
                                 " 位置(" + timerInfo.tileX + "," + timerInfo.tileY + ")");
            }
        }
        
        // 清理已过期的瓦片
        for (String key : expiredTiles) {
            timerTiles.remove(key);
        }
    }
    
    /**
     * 使瓦片变为可通行
     */
    private void makeTilePassable(TimerTileInfo timerInfo) {
        // 这里需要修改碰撞地图，使该瓦片变为可通行
        // 由于MapChunk的碰撞地图是私有的，我们需要添加一个公共方法
        timerInfo.chunk.makeTilePassable(timerInfo.tileX, timerInfo.tileY);
    }
    
    /**
     * 生成瓦片的唯一键
     */
    private String generateKey(int chunkX, int tileX, int tileY) {
        return chunkX + "_" + tileX + "_" + tileY;
    }
    
    /**
     * 获取指定位置的定时器瓦片剩余时间
     */
    public int getRemainingTime(MapChunk chunk, int tileX, int tileY) {
        String key = generateKey(chunk.getChunkX(), tileX, tileY);
        TimerTileInfo timerInfo = timerTiles.get(key);
        
        if (timerInfo != null) {
            return timerInfo.getRemainingTime();
        }
        
        return -1; // 不是定时器瓦片
    }
    
    /**
     * 检查指定位置是否有定时器瓦片
     */
    public boolean hasTimerTile(MapChunk chunk, int tileX, int tileY) {
        String key = generateKey(chunk.getChunkX(), tileX, tileY);
        return timerTiles.containsKey(key);
    }
    
    /**
     * 获取所有定时器瓦片的数量
     */
    public int getTimerTileCount() {
        return timerTiles.size();
    }
    
    /**
     * 清理指定区块的所有定时器瓦片
     */
    public void clearChunkTimerTiles(int chunkX) {
        timerTiles.entrySet().removeIf(entry -> 
            entry.getValue().chunk.getChunkX() == chunkX);
        
        System.out.println("🧹 清理区块 " + chunkX + " 的定时器瓦片");
    }
    
    /**
     * 清理所有定时器瓦片
     */
    public void clearAll() {
        timerTiles.clear();
        expiredTiles.clear();
        System.out.println("🧹 清理所有定时器瓦片");
    }
    
    /**
     * 打印定时器瓦片状态
     */
    public void printStatus() {
        System.out.println("⏰ 定时器瓦片状态:");
        System.out.println("   总数量: " + timerTiles.size());
        
        for (TimerTileInfo timerInfo : timerTiles.values()) {
            int remaining = timerInfo.getRemainingTime();
            System.out.println("   区块" + timerInfo.chunk.getChunkX() + 
                             " 位置(" + timerInfo.tileX + "," + timerInfo.tileY + ") " +
                             "剩余" + remaining + "秒");
        }
    }
}
