package com.roguelike.map;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.entities.Player;

/**
 * 传送门管理器，处理地图间的传送逻辑
 * 确保Boss房只能通过传送门到达
 */
public class TeleportManager {
    
    private InfiniteMapManager infiniteMapManager;
    private Player player;
    
    // Boss房区块配置
    private static final int BOSS_CHUNK_X = 3;
    private static final String BOSS_MAP_NAME = "test_boss";
    
    // 记录Boss房是否已被传送门激活
    private boolean bossChunkActivated = false;
    
    public TeleportManager(InfiniteMapManager infiniteMapManager) {
        this.infiniteMapManager = infiniteMapManager;
    }
    
    /**
     * 设置玩家引用
     */
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    /**
     * 检查玩家是否站在传送门上，如果是则执行传送
     * @param playerX 玩家世界坐标X
     * @param playerY 玩家世界坐标Y
     * @return 是否执行了传送
     */
    public boolean checkAndTeleport(double playerX, double playerY) {
        if (player == null || infiniteMapManager == null) {
            return false;
        }
        
        // 获取玩家当前所在的区块
        int chunkX = InfiniteMapManager.worldToChunkX(playerX);
        MapChunk chunk = infiniteMapManager.getChunk(chunkX);
        
        if (chunk == null || !chunk.isLoaded()) {
            return false;
        }
        
        // 检查玩家脚下的瓦片是否是传送门
        TileProperty teleportProperty = getTeleportPropertyAtPosition(chunk, playerX, playerY);
        
        if (teleportProperty != null && teleportProperty.isTeleport()) {
            // 执行传送
            return performTeleport(teleportProperty);
        }
        
        return false;
    }
    
    /**
     * 获取指定位置的传送门属性
     */
    private TileProperty getTeleportPropertyAtPosition(MapChunk chunk, double worldX, double worldY) {
        if (chunk.getTiledMap() == null) {
            return null;
        }
        
        // 转换为区块内坐标
        double localX = worldX - chunk.getWorldOffsetX();
        int tileX = (int) (localX / 32); // 32是瓦片大小
        int tileY = (int) (worldY / 32);
        
        // 检查所有图层
        for (Layer layer : chunk.getTiledMap().getLayers()) {
            if (tileX >= 0 && tileX < layer.getWidth() && tileY >= 0 && tileY < layer.getHeight()) {
                int index = tileY * layer.getWidth() + tileX;
                if (index < layer.getData().size()) {
                    int gid = layer.getData().get(index);
                    
                    if (gid > 0) {
                        // 查找对应的瓦片集和属性
                        for (Tileset tileset : chunk.getTiledMap().getTilesets()) {
                            if (gid >= tileset.getFirstgid() && gid < tileset.getFirstgid() + tileset.getTilecount()) {
                                int localId = gid - tileset.getFirstgid();
                                TileProperty property = tileset.getTileProperty(localId);
                                
                                if (property != null && property.isTeleport()) {
                                    return property;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 执行传送
     */
    private boolean performTeleport(TileProperty teleportProperty) {
        String targetMapId = teleportProperty.getTeleportMapId();
        int targetX = teleportProperty.getTeleportX();
        int targetY = teleportProperty.getTeleportY();
        
        System.out.println("🚪 检测到传送门！");
        System.out.println("   目标地图: " + targetMapId);
        System.out.println("   目标位置: (" + targetX + ", " + targetY + ")");
        
        // 根据目标地图ID确定目标区块
        int targetChunkX = getTargetChunkX(targetMapId);
        
        if (targetChunkX == -1) {
            System.err.println("❌ 无效的目标地图ID: " + targetMapId);
            return false;
        }
        
        // 如果是传送到Boss房，需要先激活Boss房区块
        if (targetChunkX == BOSS_CHUNK_X) {
            System.out.println("🏰 激活Boss房区块...");
            activateBossChunk();
        }
        
        // 计算目标世界坐标
        // 使用地图左下角为原点，一个瓦片为单位长度
        double targetWorldX = targetChunkX * InfiniteMapManager.getChunkWidthPixels() + (targetX * 32);
        // 地图高度是54个瓦片，需要将Y坐标从左上角转换为左下角
        double targetWorldY = (54 - 1 - targetY) * 32; // 54-1-targetY 将左上角坐标转换为左下角坐标
        
        System.out.println("   目标世界坐标: (" + targetWorldX + ", " + targetWorldY + ")");
        System.out.println("   目标区块: " + targetChunkX);
        
        // 确保目标区块已加载
        if (!infiniteMapManager.getChunk(targetChunkX).isLoaded()) {
            System.out.println("📦 加载目标区块: " + targetChunkX);
            infiniteMapManager.loadChunkAsync(targetChunkX);
        }
        
        // 传送玩家
        player.setPosition(targetWorldX, targetWorldY);
        
        // 更新无限地图管理器的玩家位置
        infiniteMapManager.updateChunks(targetChunkX);
        
        // 更新摄像机跟随
        FXGL.getGameScene().getViewport().bindToEntity(player, 
            FXGL.getAppWidth() / 2.0, FXGL.getAppHeight() / 2.0);
        
        System.out.println("✅ 传送完成！");
        
        return true;
    }
    
    /**
     * 激活Boss房区块（通过传送门）
     */
    private void activateBossChunk() {
        if (!bossChunkActivated) {
            bossChunkActivated = true;
            System.out.println("🚪 Boss房区块已激活，玩家现在可以进入");
        }
    }
    
    /**
     * 检查Boss房区块是否已激活
     */
    public boolean isBossChunkActivated() {
        return bossChunkActivated;
    }
    
    /**
     * 根据地图ID获取目标区块X坐标
     * 根据命名规范：test.tmx, test_door.tmx, test_boss.tmx
     */
    private int getTargetChunkX(String mapId) {
        switch (mapId) {
            case "test":
                return 0; // 普通地图在区块0
            case "test_door":
                return 2; // 门地图在区块2
            case "test_boss":
            case "maphole": // 兼容旧的地图ID
                return BOSS_CHUNK_X; // Boss地图在区块3
            default:
                // 尝试解析地图名称，支持动态映射
                if (mapId.endsWith("_door")) {
                    return 2; // 所有_door地图都在区块2
                } else if (mapId.endsWith("_boss") || mapId.equals("maphole")) {
                    return BOSS_CHUNK_X; // 所有_boss地图都在Boss房区块
                } else {
                    return 0; // 默认普通地图在区块0
                }
        }
    }
    
    /**
     * 获取Boss房区块X坐标
     */
    public static int getBossChunkX() {
        return BOSS_CHUNK_X;
    }
    
    /**
     * 获取Boss房地图名称
     */
    public static String getBossMapName() {
        return BOSS_MAP_NAME;
    }
}
