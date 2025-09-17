package com.roguelike.map;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.entities.Player;
import com.roguelike.map.config.MapConfig;
import com.roguelike.map.config.MapConfigLoader;

/**
 * 传送门管理器，处理地图间的传送逻辑
 * 确保Boss房只能通过传送门到达
 */
public class TeleportManager {
    
    private InfiniteMapManager infiniteMapManager;
    private Player player;
    // 预扫描注册的传送瓦片：chunkKey_tileX_tileY -> TileProperty
    private final java.util.Map<String, TileProperty> teleportTiles = new java.util.HashMap<>();
    
    // Boss房区块配置（兼容旧接口，不再使用硬编码坐标）
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
     * 扫描区块中的传送瓦片并注册，加速查询。
     */
    public void scanChunkForTeleportTiles(MapChunk chunk) {
        if (chunk == null || chunk.getTiledMap() == null) return;
        String baseKey = chunk.getChunkX() + "," + chunk.getChunkY() + "_";
        for (Layer layer : chunk.getTiledMap().getLayers()) {
            for (int y = 0; y < layer.getHeight(); y++) {
                for (int x = 0; x < layer.getWidth(); x++) {
                    int index = y * layer.getWidth() + x;
                    if (index >= layer.getData().size()) continue;
                    int gid = layer.getData().get(index);
                    if (gid <= 0) continue;
                    for (Tileset tileset : chunk.getTiledMap().getTilesets()) {
                        if (gid >= tileset.getFirstgid() && gid < tileset.getFirstgid() + tileset.getTilecount()) {
                            int localId = gid - tileset.getFirstgid();
                            TileProperty property = tileset.getTileProperty(localId);
                            if (property != null && property.isTeleport()) {
                                teleportTiles.put(baseKey + x + "_" + y, property);
                            }
                            break;
                        }
                    }
                }
            }
        }
        System.out.println("🚪 已扫描并注册传送瓦片: 区块(" + chunk.getChunkX() + "," + chunk.getChunkY() + ")");
    }

    /**
     * 清理指定区块的传送瓦片注册。
     */
    public void clearChunkTeleportTiles(String chunkKey) {
        java.util.Iterator<java.util.Map.Entry<String, TileProperty>> it = teleportTiles.entrySet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            if (key.startsWith(chunkKey + "_")) {
                it.remove();
            }
        }
        System.out.println("🧹 清理区块 " + chunkKey + " 的传送瓦片注册");
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
        if (chunk.getTiledMap() == null) return null;
        int tileX = (int) ((worldX - chunk.getWorldOffsetX()) / 32);
        int tileY = (int) ((worldY - chunk.getWorldOffsetY()) / 32);
        String key = chunk.getChunkX() + "," + chunk.getChunkY() + "_" + tileX + "_" + tileY;
        TileProperty cached = teleportTiles.get(key);
        if (cached != null) return cached;
        // 兜底：直接扫描当前位置（与旧逻辑一致）
        for (Layer layer : chunk.getTiledMap().getLayers()) {
            if (tileX >= 0 && tileX < layer.getWidth() && tileY >= 0 && tileY < layer.getHeight()) {
                int index = tileY * layer.getWidth() + tileX;
                if (index < layer.getData().size()) {
                    int gid = layer.getData().get(index);
                    if (gid > 0) {
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
        
        // 优先从 JSON 配置解析目标区块 (x,y)
        int[] xyFromCfg = resolveTargetChunkFromConfig(targetMapId);
        int targetChunkX = xyFromCfg[0];
        
        if (targetChunkX == Integer.MIN_VALUE) {
            System.err.println("❌ 无效的目标地图ID: " + targetMapId);
            return false;
        }
        
        // 计算目标区块Y坐标（已由配置解析，若未命中则落回旧逻辑）
        int targetChunkY = xyFromCfg[1];
        
        // 如果是传送到Boss房，需要先激活Boss房区块（根据配置集合）
        String targetChunkKey = targetChunkX + "," + targetChunkY;
        if (isBossChunkByConfig(targetChunkKey) || targetChunkKey.equals("3,0") || targetChunkKey.equals("0,3")) {
            System.out.println("🏰 激活Boss房区块...");
            activateBossChunk();
        }
        
        // 计算目标世界坐标（将左下原点的 y 翻转为 TMX 顶左原点）
        String targetMapName = infiniteMapManager.getMapNameForChunk(targetChunkX, targetChunkY);
        int[] dims = MapChunkFactory.getMapDimensions(targetMapName);
        int H = dims != null && dims.length == 2 ? dims[1] : 0;
        int tmxY = Math.max(0, H - 1 - targetY);
        double targetWorldX = targetChunkX * MapChunkFactory.getChunkWidthPixels(targetMapName) + (targetX * 32) + 16;
        double targetWorldY = targetChunkY * MapChunkFactory.getChunkHeightPixels(targetMapName) + (tmxY * 32) + 16;
        
        System.out.println("   目标世界坐标: (" + targetWorldX + ", " + targetWorldY + ")");
        System.out.println("   目标区块: (" + targetChunkX + "," + targetChunkY + ")");
        
        // 确保目标区块已加载（空值判定 + 同步加载，避免NPE与竞态）
        MapChunk targetChunk = infiniteMapManager.getChunk(targetChunkX, targetChunkY);
        if (targetChunk == null || !targetChunk.isLoaded()) {
            System.out.println("📦 加载目标区块(同步): (" + targetChunkX + "," + targetChunkY + ")");
            try {
                infiniteMapManager.loadChunk(targetChunkX, targetChunkY);
            } catch (Exception e) {
                System.err.println("❌ 同步加载目标区块失败: (" + targetChunkX + "," + targetChunkY + ") - " + e.getMessage());
                // 尝试异步兜底
                infiniteMapManager.loadChunkAsync(targetChunkX, targetChunkY);
            }
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
    
    public static String getBossChunk1() { return BOSS_CHUNK_1; }
    
    
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
    // 从 JSON 配置解析目标区块 (x,y)。未命中则按旧规则推断，仅返回 X=2/3、Y=0/2/3 的兼容值。
    private int[] resolveTargetChunkFromConfig(String mapId) {
        try {
            MapConfig cfg = MapConfigLoader.load();
            if (cfg != null && cfg.maps != null) {
                String base = infiniteMapManager != null ? infiniteMapManager.getMapName() : null;
                MapConfig.SingleMapConfig m = base != null ? cfg.maps.get(base) : null;
                if (m != null && m.specialChunks != null) {
                    for (java.util.Map.Entry<String, java.util.List<MapConfig.SpecialChunk>> e : m.specialChunks.entrySet()) {
                        for (MapConfig.SpecialChunk sc : e.getValue()) {
                            if (sc != null && mapId.equals(sc.map) && sc.x != null && sc.y != null) {
                                // 将外部坐标系(y向上为正)转换为内部坐标系(y向下为正)
                                return new int[]{sc.x, -sc.y};
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 旧规则（兼容）
        int x = getTargetChunkX(mapId);
        if (x == -1) return new int[]{Integer.MIN_VALUE, 0};
        int y = 0;
        if ("1".equals(mapId)) y = 2;
        else if ("2".equals(mapId)) y = 0;
        else if ("3".equals(mapId)) y = 3;
        else if ("4".equals(mapId)) y = 0;
        return new int[]{x, y};
    }

    private boolean isBossChunkByConfig(String chunkKey) {
        try {
            // 通过公开方法判断
            return infiniteMapManager != null &&
                   infiniteMapManager.getBossChunkKeys().contains(chunkKey);
        } catch (Throwable ignored) {}
        return false;
    }
}
