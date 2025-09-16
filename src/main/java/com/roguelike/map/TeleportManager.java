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
    private static final String BOSS_CHUNK_1 = "3,0";
    private static final String BOSS_MAP_NAME = "square_boss";
    
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
        int chunkX = infiniteMapManager.worldToChunkX(playerX);
        int chunkY = infiniteMapManager.worldToChunkY(playerY);
        MapChunk chunk = infiniteMapManager.getChunk(chunkX, chunkY);
        
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
        double localY = worldY - chunk.getWorldOffsetY();
        int tileX = (int) (localX / 32); // 32是瓦片大小
        int tileY = (int) (localY / 32);
        
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
        
        // 计算目标区块Y坐标（根据目标地图ID）
        int targetChunkY = 0; // 默认Y坐标
        if ("1".equals(targetMapId)) {
            targetChunkY = 2; // 传送到(0,2)区块
        } else if ("2".equals(targetMapId)) {
            targetChunkY = 0; // 传送到(2,0)区块
        } else if ("3".equals(targetMapId)) {
            targetChunkY = 3; // 传送到(0,3)区块
        } else if ("4".equals(targetMapId)) {
            targetChunkY = 0; // 传送到(3,0)区块
        }
        
        // 如果是传送到Boss房，需要先激活Boss房区块
        String targetChunkKey = targetChunkX + "," + targetChunkY;
        if (targetChunkKey.equals("3,0") || targetChunkKey.equals("0,3")) {
            System.out.println("🏰 激活Boss房区块...");
            activateBossChunk();
        }
        
        // 计算目标世界坐标
        // 需要根据目标区块的地图类型使用正确的尺寸
        String targetMapName = getMapNameForChunk(targetChunkX, targetChunkY);
        double targetWorldX = targetChunkX * MapChunkFactory.getChunkWidthPixels(targetMapName) + (targetX * 32);
        double targetWorldY = targetChunkY * MapChunkFactory.getChunkHeightPixels(targetMapName) + (targetY * 32);
        
        System.out.println("   目标世界坐标: (" + targetWorldX + ", " + targetWorldY + ")");
        System.out.println("   目标区块: (" + targetChunkX + "," + targetChunkY + ")");
        
        // 确保目标区块已加载
        if (!infiniteMapManager.getChunk(targetChunkX, targetChunkY).isLoaded()) {
            System.out.println("📦 加载目标区块: (" + targetChunkX + "," + targetChunkY + ")");
            infiniteMapManager.loadChunkAsync(targetChunkX, targetChunkY);
        }
        
        // 传送玩家
        player.setPosition(targetWorldX, targetWorldY);
        
        // 更新无限地图管理器的玩家位置
        infiniteMapManager.updateChunks(targetChunkX, targetChunkY);
        
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
     * 根据命名规范：test.tmx, test_door.tmx, test_boss.tmx, dungeon.tmx, dungeon_door.tmx, dungeon_boss.tmx
     */
    private int getTargetChunkX(String mapId) {
        switch (mapId) {
            case "test":
            case "square":
            case "dungeon":
                return 0; // 普通地图在区块0
            case "test_door":
            case "square_door":
            case "dungeon_door":
                return 2; // 门地图在区块2
            case "test_boss":
            case "square_boss":
            case "dungeon_boss":
            case "maphole": // 兼容旧的地图ID
                return 3; // Boss地图在区块3
            default:
                // 尝试解析地图名称，支持动态映射
                if (mapId.endsWith("_door")) {
                    return 2; // 所有_door地图都在区块2
                } else if (mapId.endsWith("_boss") || mapId.equals("maphole")) {
                    return 3; // 所有_boss地图都在Boss房区块
                } else {
                    return 0; // 默认普通地图在区块0
                }
        }
    }
    
    /**
     * 获取Boss房区块坐标
     */
    public static String getBossChunk1() {
        return BOSS_CHUNK_1;
    }
    
    
    /**
     * 获取Boss房地图名称
     */
    public String getBossMapName() {
        return infiniteMapManager.getBossMapName();
    }
    
    /**
     * 获取指定区块对应的地图名称
     * 如果区块有特殊配置则使用特殊地图，否则使用默认地图
     */
    private String getMapNameForChunk(int chunkX, int chunkY) {
        String chunkKey = chunkX + "," + chunkY;
        
        // 获取基础地图名称
        String baseMapName = infiniteMapManager.getMapName();
        boolean isHorizontalInfinite = infiniteMapManager.isHorizontalInfinite();
        
        // 特殊区块地图配置 - 所有地图都只有X方向的特殊区块
        if ("2,0".equals(chunkKey)) {
            return baseMapName + "_door";
        } else if ("3,0".equals(chunkKey)) {
            return baseMapName + "_boss";
        }
        
        return baseMapName; // 默认地图
    }
}
