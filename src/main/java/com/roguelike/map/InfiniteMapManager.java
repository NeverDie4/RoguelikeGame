package com.roguelike.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.roguelike.map.config.MapConfig;
import com.roguelike.map.config.MapConfigLoader;
import javafx.application.Platform;
import com.roguelike.core.GameApp;
import com.roguelike.map.strategy.MapModeStrategy;
import com.roguelike.map.strategy.HorizontalStrategy;
import com.roguelike.map.strategy.FourDirectionalStrategy;

/**
 * 无限地图管理器，管理区块的加载、卸载和坐标转换
 */
public class InfiniteMapManager {
    
    private Map<String, MapChunk> loadedChunks;   // 已加载的区块，键格式："chunkX,chunkY"
    private int playerChunkX;                     // 玩家当前所在区块X坐标
    private int playerChunkY;                     // 玩家当前所在区块Y坐标
    private int loadRadius;                       // 加载半径（区块数）
    private int preloadRadius;                    // 预加载半径（区块数）
    private String mapName;                       // 地图名称
    
    // 异步加载和状态管理
    private AsyncChunkLoader asyncLoader;         // 异步加载器
    private ChunkStateManager stateManager;       // 状态管理器
    private boolean useAsyncLoading;              // 是否使用异步加载
    
    // 玩家移动跟踪
    private long lastUpdateTime;                  // 上次更新时间
    
    // 视角预加载节流
    private long lastViewportPreloadTime;         // 上次视角预加载时间
    private static final long VIEWPORT_PRELOAD_INTERVAL = 500; // 视角预加载间隔（毫秒）- 增加到500ms减少频繁调用
    private double lastPlayerX = Double.NaN;      // 上次玩家X坐标，用于检测移动
    private double lastPlayerY = Double.NaN;      // 上次玩家Y坐标，用于检测移动
    private int lastPreloadedChunks = 0;          // 上次预加载的区块数量，用于检测变化
    private ArrayList<Integer> lastPreloadedChunkList = new ArrayList<>(); // 上次预加载的区块列表
    
    // 地图常量
    private static final int DEFAULT_LOAD_RADIUS = 1; // 默认加载半径
    private static final int DEFAULT_PRELOAD_RADIUS = 2; // 默认预加载半径
    
    // 地图类型配置
    private boolean isHorizontalInfinite; // 是否为横向无限地图
    private MapModeStrategy strategy;
    
    // 特殊区块地图配置：2D坐标 -> 地图名称（来自配置）
    private Map<String, String> specialChunkMaps;
    private Set<String> doorChunkKeys = new HashSet<>();
    private Set<String> bossChunkKeys = new HashSet<>();
    private static final String BOSS_CHUNK_1 = "3,0"; // 兼容旧接口
    private String bossMapName;
    
    // Boss房隔离模式：进入Boss区后仅加载当前区块，周围区块不再加载
    private boolean bossIsolationMode = false;

    /**
     * 对外暴露Boss房隔离模式状态，用于刷怪管理器在Boss战时切换刷怪策略。
     */
    public boolean isBossIsolationMode() {
        return bossIsolationMode;
    }
    
    // 传送门管理器引用
    private TeleportManager teleportManager;
    
    // 定时器瓦片管理器
    private TimerTileManager timerTileManager;
    
    public InfiniteMapManager() {
        this("square"); // 默认地图名称
    }
    
    public InfiniteMapManager(String mapName) {
        this.loadedChunks = new HashMap<>();
        this.playerChunkX = 0;
        this.playerChunkY = 0;
        this.loadRadius = DEFAULT_LOAD_RADIUS;
        this.preloadRadius = DEFAULT_PRELOAD_RADIUS;
        this.useAsyncLoading = true; // 默认启用异步加载
        this.lastUpdateTime = System.currentTimeMillis();
        this.mapName = mapName;
        
        // 从 JSON 配置读取模式、初始量与特殊区块
        MapConfig cfg = null;
        MapConfig.SingleMapConfig mapCfg = null;
        try {
            cfg = MapConfigLoader.load();
            if (cfg != null && cfg.maps != null) {
                mapCfg = cfg.maps.getOrDefault(mapName, null);
            }
        } catch (Throwable ignored) {}

        // 地图模式：默认规则与旧行为保持一致（test 为横向）
        if (mapCfg != null && mapCfg.mode != null) {
            this.isHorizontalInfinite = "horizontal".equalsIgnoreCase(mapCfg.mode);
        } else {
            this.isHorizontalInfinite = "test".equals(mapName);
        }
        this.strategy = this.isHorizontalInfinite ? new HorizontalStrategy() : new FourDirectionalStrategy();

        // 初始量覆盖
        if (mapCfg != null) {
            if (mapCfg.loadRadius != null) this.loadRadius = Math.max(1, mapCfg.loadRadius);
            if (mapCfg.preloadRadius != null) this.preloadRadius = Math.max(0, mapCfg.preloadRadius);
            if (mapCfg.useAsyncLoading != null) this.useAsyncLoading = mapCfg.useAsyncLoading;
        }

        // 构建特殊区块映射
        this.specialChunkMaps = new HashMap<>();
        if (mapCfg != null && mapCfg.specialChunks != null) {
            List<MapConfig.SpecialChunk> doors = mapCfg.specialChunks.get("door");
            if (doors != null) {
                for (MapConfig.SpecialChunk sc : doors) {
                    if (sc == null || sc.x == null || sc.y == null || sc.map == null) continue;
                    int yInternal = -sc.y; // 坐标系：上为正 -> 内部向下为正，取反
                    if (!strategy.isSpecialChunkAllowed(sc.x, yInternal)) continue;
                    String key = chunkToKey(sc.x, yInternal);
                    specialChunkMaps.put(key, sc.map);
                    doorChunkKeys.add(key);
                }
            }
            List<MapConfig.SpecialChunk> bosses = mapCfg.specialChunks.get("boss");
            if (bosses != null) {
                for (MapConfig.SpecialChunk sc : bosses) {
                    if (sc == null || sc.x == null || sc.y == null || sc.map == null) continue;
                    int yInternal = -sc.y; // 坐标系取反
                    if (!strategy.isSpecialChunkAllowed(sc.x, yInternal)) continue;
                    String key = chunkToKey(sc.x, yInternal);
                    specialChunkMaps.put(key, sc.map);
                    bossChunkKeys.add(key);
                    // 记录一个 boss 地图名（用于兼容旧接口）
                    if (bossMapName == null) bossMapName = sc.map;
                }
            }
        }
        if (bossMapName == null) {
            bossMapName = mapName + "_boss";
        }
        
        // 初始化状态管理器
        this.stateManager = new ChunkStateManager();
        
        // 初始化异步加载器
        this.asyncLoader = new AsyncChunkLoader(stateManager, mapName);
        
        // 初始化定时器瓦片管理器
        this.timerTileManager = new TimerTileManager();
        
        // 初始加载玩家所在区块和预加载区块
        System.out.println("🔧 开始初始加载区块...");
        // 优先同步加载中心区块，避免首帧并发解码多张 PNG 造成 OOM
        try {
            loadChunk(0, 0);
        } catch (Throwable e) {
            System.err.println("❌ 同步加载中心区块失败: " + e.getMessage());
        }
        if (useAsyncLoading) {
            preloadChunksAsync(0, 0);
        } else {
            preloadChunks(0, 0);
        }
        
        // 强制同步加载相邻区块以确保可用性
        System.out.println("🔧 强制加载相邻区块以确保可用性...");
        if (isHorizontalInfinite) {
            // 横向无限地图：只加载左右区块
            try {
                loadChunkAsync(1, 0);   // 右侧区块
                System.out.println("✅ 右侧区块(1,0)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 右侧区块(1,0)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            try {
                loadChunkAsync(-1, 0);  // 左侧区块
                System.out.println("✅ 左侧区块(-1,0)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 左侧区块(-1,0)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // 四向无限地图：加载四个方向的区块
            try {
                loadChunkAsync(1, 0);   // 右侧区块
                System.out.println("✅ 右侧区块(1,0)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 右侧区块(1,0)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            try {
                loadChunkAsync(-1, 0);  // 左侧区块
                System.out.println("✅ 左侧区块(-1,0)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 左侧区块(-1,0)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            try {
                loadChunkAsync(0, 1);   // 下方区块
                System.out.println("✅ 下方区块(0,1)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 下方区块(0,1)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            try {
                loadChunkAsync(0, -1);  // 上方区块
                System.out.println("✅ 上方区块(0,-1)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 上方区块(0,-1)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("✅ 相邻区块强制加载完成，当前已加载区块: " + getLoadedChunkCoordinates());
        
        System.out.println("🌍 无限地图管理器初始化完成");
        System.out.println("   地图名称: " + mapName);
        System.out.println("   区块尺寸: " + getChunkWidthPixels() + "x" + getChunkHeightPixels() + " 像素");
        System.out.println("   异步加载: " + (useAsyncLoading ? "启用" : "禁用"));

        // 额外一次范围填充：四向模式下补全角落，避免开场斜向移动出现空白
        try {
            loadRequiredChunks();
        } catch (Exception ignored) {}
    }
    
    /**
     * 2D坐标转换辅助方法
     */
    public static String chunkToKey(int chunkX, int chunkY) {
        return chunkX + "," + chunkY;
    }
    
    public static int[] keyToChunk(String key) {
        String[] parts = key.split(",");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
    
    public int worldToChunkY(double worldY) {
        if (isHorizontalInfinite) {
            return 0;
        }
        return MapChunkFactory.worldToChunkY(worldY, mapName);
    }
    
    public double chunkToWorldY(int chunkY) {
        return MapChunkFactory.chunkToWorldY(chunkY, mapName);
    }
    
    /**
     * 更新区块加载状态（根据玩家位置）
     */
    public void updateChunks(int newPlayerChunkX, int newPlayerChunkY) {
        int[] norm = strategy.normalizePlayerChunk(newPlayerChunkX, newPlayerChunkY);
        newPlayerChunkX = norm[0];
        newPlayerChunkY = norm[1];
        
        if (newPlayerChunkX == playerChunkX && newPlayerChunkY == playerChunkY) {
            return; // 玩家仍在同一区块
        }
        
        // 检查是否尝试进入Boss房区块（基于配置）
        String newChunkKey = chunkToKey(newPlayerChunkX, newPlayerChunkY);
        boolean isBossChunk = bossChunkKeys.contains(newChunkKey);
        
        if (isBossChunk && teleportManager != null && !teleportManager.isBossChunkActivated()) {
            System.out.println("🚫 玩家尝试进入Boss房区块，但Boss房未被激活，阻止进入");
            return; // 阻止进入Boss房
        }
        
        int oldPlayerChunkX = playerChunkX;
        int oldPlayerChunkY = playerChunkY;
        playerChunkX = newPlayerChunkX;
        playerChunkY = newPlayerChunkY;
        lastUpdateTime = System.currentTimeMillis();
        
        System.out.println("🔄 玩家从区块 (" + oldPlayerChunkX + "," + oldPlayerChunkY + ") 移动到区块 (" + playerChunkX + "," + playerChunkY + ")");
        
        // 调试信息：显示横向无限地图的玩家移动
        if (isHorizontalInfinite && playerChunkX > 3) {
            System.out.println("🔍 横向无限地图玩家移动到区块 (" + playerChunkX + "," + playerChunkY + ")");
        }
        
        // Boss房隔离模式开关：进入Boss区开启，离开关闭
        boolean enteringBossChunk = isBossChunk;
        boolean previouslyInBossChunk = bossChunkKeys.contains(chunkToKey(oldPlayerChunkX, oldPlayerChunkY));
        if (enteringBossChunk && (teleportManager == null || teleportManager.isBossChunkActivated())) {
            if (!bossIsolationMode) {
                bossIsolationMode = true;
                System.out.println("🧿 已进入Boss房隔离模式");
            }
        } else if (!enteringBossChunk && bossIsolationMode && previouslyInBossChunk) {
            bossIsolationMode = false;
            System.out.println("🧿 已退出Boss房隔离模式");
        }
        
        // 卸载远离的区块
        unloadDistantChunks();
        
        // 加载需要的区块
        if (useAsyncLoading) {
            loadRequiredChunksAsync();
        } else {
            loadRequiredChunks();
        }
        
        // 简化预加载区块（不使用智能预加载）
        if (useAsyncLoading) {
            preloadChunksAsync(newPlayerChunkX, newPlayerChunkY);
        } else {
            preloadChunks(newPlayerChunkX, newPlayerChunkY);
        }
        
    }
    
    /**
     * 主动预加载（在玩家移动过程中调用）
     * 已废弃：使用简化的预加载策略
     */
    @Deprecated
    public void proactivePreload() {
        // 不再使用智能预加载，避免频繁变向导致的性能问题
    }
    
    /**
     * 基于视角的智能预加载
     * 已废弃：使用简化的预加载策略
     */
    @Deprecated
    public void viewportBasedPreload(double playerWorldX, double playerWorldY) {
        // 不再使用智能预加载，避免频繁变向导致的性能问题
    }
    
    /**
     * 卸载远离玩家的区块
     */
    private void unloadDistantChunks() {
        // Boss房隔离：仅保留当前玩家所在区块，卸载其他所有区块
        if (bossIsolationMode) {
            ArrayList<String> chunksToUnload = new ArrayList<>();
            String keepKey = chunkToKey(playerChunkX, playerChunkY);
            for (String chunkKey : new java.util.ArrayList<>(loadedChunks.keySet())) {
                if (!chunkKey.equals(keepKey)) {
                    chunksToUnload.add(chunkKey);
                }
            }
            for (String chunkKey : chunksToUnload) {
                if (useAsyncLoading && asyncLoader.isLoading(chunkKey)) {
                    asyncLoader.cancelLoading(chunkKey);
                }
                unloadChunk(chunkKey);
            }
            if (!chunksToUnload.isEmpty()) {
                System.out.println("🗑️ [Boss隔离] 卸载了 " + chunksToUnload.size() + " 个区块: " + chunksToUnload);
            }
            return;
        }
        
        ArrayList<String> chunksToUnload = new ArrayList<>();
        
        for (String chunkKey : loadedChunks.keySet()) {
            int[] chunkCoords = keyToChunk(chunkKey);
            int chunkX = chunkCoords[0];
            int chunkY = chunkCoords[1];
            
            boolean shouldUnload = false;
            
            shouldUnload = strategy.shouldUnload(chunkX, chunkY, playerChunkX, playerChunkY, loadRadius);
            
            if (shouldUnload) {
                chunksToUnload.add(chunkKey);
            }
        }
        
        for (String chunkKey : chunksToUnload) {
            // 取消正在加载的任务
            if (useAsyncLoading && asyncLoader.isLoading(chunkKey)) {
                asyncLoader.cancelLoading(chunkKey);
            }
            unloadChunk(chunkKey);
        }
        
        if (!chunksToUnload.isEmpty()) {
            System.out.println("🗑️ 卸载了 " + chunksToUnload.size() + " 个区块: " + chunksToUnload);
        }
    }
    
    /**
     * 加载玩家周围需要的区块
     */
    private void loadRequiredChunks() {
        // Boss房隔离：仅确保当前区块被加载
        if (bossIsolationMode) {
            String currentKey = chunkToKey(playerChunkX, playerChunkY);
            if (!loadedChunks.containsKey(currentKey)) {
                int[] coords = keyToChunk(currentKey);
                try {
                    loadChunk(coords[0], coords[1]);
                    System.out.println("📦 [Boss隔离] 加载当前区块: " + currentKey);
                } catch (Exception e) {
                    System.err.println("❌ [Boss隔离] 加载当前区块失败: " + currentKey + " - " + e.getMessage());
                }
            }
            return;
        }
        
        ArrayList<String> chunksToLoad = new ArrayList<>();
        
        for (String key : strategy.listChunksInRadius(playerChunkX, playerChunkY, loadRadius)) {
            if (!loadedChunks.containsKey(key)) {
                chunksToLoad.add(key);
            }
        }
        
        for (String chunkKey : chunksToLoad) {
            int[] coords = keyToChunk(chunkKey);
            try {
                loadChunk(coords[0], coords[1]);
            } catch (Exception e) {
                System.err.println("❌ 加载区块失败: " + chunkKey + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (!chunksToLoad.isEmpty()) {
            System.out.println("📦 加载了 " + chunksToLoad.size() + " 个区块: " + chunksToLoad);
        }
    }
    
    /**
     * 异步加载玩家周围需要的区块
     */
    private void loadRequiredChunksAsync() {
        // Boss房隔离：仅确保当前区块被加载（异步）
        if (bossIsolationMode) {
            String currentKey = chunkToKey(playerChunkX, playerChunkY);
            if (!loadedChunks.containsKey(currentKey) && !stateManager.isLoading(currentKey)) {
                int[] coords = keyToChunk(currentKey);
                try {
                    loadChunkAsync(coords[0], coords[1]);
                    System.out.println("📦 [Boss隔离] 异步加载当前区块: " + currentKey);
                } catch (Exception e) {
                    System.err.println("❌ [Boss隔离] 异步加载当前区块失败: " + currentKey + " - " + e.getMessage());
                }
            }
            return;
        }
        
        ArrayList<String> chunksToLoad = new ArrayList<>();
        
        for (String key : strategy.listChunksInRadius(playerChunkX, playerChunkY, loadRadius)) {
            if (!loadedChunks.containsKey(key) && !stateManager.isLoading(key)) {
                chunksToLoad.add(key);
            }
        }
        
        if (!chunksToLoad.isEmpty()) {
            System.out.println("📦 开始异步加载 " + chunksToLoad.size() + " 个区块: " + chunksToLoad);
            for (String chunkKey : chunksToLoad) {
                int[] coords = keyToChunk(chunkKey);
                try {
                    loadChunkAsync(coords[0], coords[1]);
                } catch (Exception e) {
                    System.err.println("❌ 异步加载区块失败: " + chunkKey + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 获取指定区块对应的地图名称
     * 如果区块有特殊配置则使用特殊地图，否则使用默认地图
     */
    public String getMapNameForChunk(int chunkX, int chunkY) {
        String chunkKey = chunkToKey(chunkX, chunkY);
        String mapNameForChunk = specialChunkMaps.getOrDefault(chunkKey, mapName);
        if (isHorizontalInfinite && chunkX > 3) {
            System.out.println("🔍 区块 (" + chunkX + "," + chunkY + ") 使用地图: " + mapNameForChunk);
        }
        return mapNameForChunk;
    }

    /**
     * 获取传送门区块集合（返回副本）。
     */
    public java.util.Set<String> getDoorChunkKeys() {
        return new java.util.HashSet<>(doorChunkKeys);
    }

    /**
     * 获取Boss区块集合（返回副本）。
     */
    public java.util.Set<String> getBossChunkKeys() {
        return new java.util.HashSet<>(bossChunkKeys);
    }
    
    /**
     * 加载指定区块
     */
    public void loadChunk(int chunkX, int chunkY) {
        String chunkKey = chunkToKey(chunkX, chunkY);
        if (loadedChunks.containsKey(chunkKey)) {
            return; // 已加载
        }
        
        stateManager.transitionToState(chunkKey, ChunkState.LOADING);
        String chunkMapName = getMapNameForChunk(chunkX, chunkY);
        MapChunk chunk = new MapChunk(chunkX, chunkY, chunkMapName);
        chunk.load();
        chunk.addToScene(); // 同步加载时直接添加到场景
        loadedChunks.put(chunkKey, chunk);
        stateManager.transitionToState(chunkKey, ChunkState.LOADED);
        
        // 扫描新加载区块中的定时器瓦片
        if (timerTileManager != null) {
            timerTileManager.scanChunkForTimerTiles(chunk);
        }
        // 扫描传送瓦片
        if (teleportManager != null) {
            teleportManager.scanChunkForTeleportTiles(chunk);
        }
        
        System.out.println("🗺️ 区块 (" + chunkX + "," + chunkY + ") 使用地图: " + chunkMapName);
        
        // 调试信息：显示特殊区块的加载情况
        if (doorChunkKeys.contains(chunkKey)) {
            System.out.println("🚪 传送门区块 (" + chunkX + "," + chunkY + ") 加载完成，使用地图: " + chunkMapName);
        } else if (bossChunkKeys.contains(chunkKey)) {
            System.out.println("👹 Boss房区块 (" + chunkX + "," + chunkY + ") 加载完成，使用地图: " + chunkMapName);
        }
        
        // 调试信息：显示横向无限地图的区块加载情况
        if (isHorizontalInfinite && chunkX > 3) {
            System.out.println("🔍 横向无限地图加载区块 (" + chunkX + "," + chunkY + ") -> " + chunkMapName);
        }
    }
    
    /**
     * 异步加载指定区块
     */
    public void loadChunkAsync(int chunkX, int chunkY) {
        String chunkKey = chunkToKey(chunkX, chunkY);
        if (loadedChunks.containsKey(chunkKey) || stateManager.isLoading(chunkKey)) {
            return; // 已加载或正在加载
        }
        
        String chunkMapName = getMapNameForChunk(chunkX, chunkY);
        CompletableFuture<MapChunk> future = asyncLoader.loadChunkAsync(chunkX, chunkY, chunkMapName);
        if (future != null) {
            future.thenAccept(chunk -> {
                if (chunk != null) {
                    loadedChunks.put(chunkKey, chunk);
                    // 立即在主线程中添加地图视图到场景，减少延迟
                    Platform.runLater(() -> {
                        // Provider 渲染在 MapChunk 内部控制；此处不重复添加
                        
                        // 扫描新加载区块中的定时器瓦片
                        if (timerTileManager != null) {
                            timerTileManager.scanChunkForTimerTiles(chunk);
                        }
                        if (teleportManager != null) {
                            teleportManager.scanChunkForTeleportTiles(chunk);
                        }
                        
                        System.out.println("✅ 区块 (" + chunkX + "," + chunkY + ") 异步加载完成并添加到场景 (地图: " + chunkMapName + ")");
                        
                        // 调试信息：显示特殊区块的异步加载情况
                        if (doorChunkKeys.contains(chunkKey)) {
                            System.out.println("🚪 传送门区块 (" + chunkX + "," + chunkY + ") 异步加载完成，使用地图: " + chunkMapName);
                        } else if (bossChunkKeys.contains(chunkKey)) {
                            System.out.println("👹 Boss房区块 (" + chunkX + "," + chunkY + ") 异步加载完成，使用地图: " + chunkMapName);
                        }
                        
                        // 调试信息：显示横向无限地图的异步加载情况
                        if (isHorizontalInfinite && chunkX > 3) {
                            System.out.println("🔍 横向无限地图异步加载完成区块 (" + chunkX + "," + chunkY + ") -> " + chunkMapName);
                        }
                    });
                }
            });
        }
    }
    
    /**
     * 卸载指定区块
     */
    public void unloadChunk(String chunkKey) {
        stateManager.transitionToState(chunkKey, ChunkState.UNLOADING);
        MapChunk chunk = loadedChunks.remove(chunkKey);
        if (chunk != null) {
            // 清理区块内的敌人和子弹
            cleanupEntitiesInChunk(chunkKey);
            
            // 清理该区块的定时器瓦片
            if (timerTileManager != null) {
                timerTileManager.clearChunkTimerTiles(chunkKey);
            }
            
            chunk.unload();
        }
        stateManager.transitionToState(chunkKey, ChunkState.UNLOADED);
        // 清理注册的定时器/传送瓦片
        if (teleportManager != null) {
            teleportManager.clearChunkTeleportTiles(chunkKey);
        }
    }
    
    /**
     * 清理指定区块内的实体（敌人、子弹等）
     */
    private void cleanupEntitiesInChunk(String chunkKey) {
        int[] coords = keyToChunk(chunkKey);
        int chunkX = coords[0];
        int chunkY = coords[1];
        
        double chunkLeft = chunkToWorldX(chunkX);
        double chunkRight = chunkLeft + getChunkWidthPixels();
        double chunkTop = chunkToWorldY(chunkY);
        double chunkBottom = chunkTop + getChunkHeightPixels();
        
        // 获取所有敌人和子弹实体
        List<Entity> entitiesToRemove = new ArrayList<>();
        
        // 清理敌人
        entitiesToRemove.addAll(FXGL.getGameWorld().getEntitiesByType().stream()
            .filter(e -> e instanceof com.roguelike.entities.Enemy)
            .filter(e -> {
                double x = e.getX();
                double y = e.getY();
                return x >= chunkLeft && x < chunkRight && y >= chunkTop && y < chunkBottom;
            })
            .collect(java.util.stream.Collectors.toList()));
        
        // 清理子弹
        entitiesToRemove.addAll(FXGL.getGameWorld().getEntitiesByType().stream()
            .filter(e -> e instanceof com.roguelike.entities.Bullet)
            .filter(e -> {
                double x = e.getX();
                double y = e.getY();
                return x >= chunkLeft && x < chunkRight && y >= chunkTop && y < chunkBottom;
            })
            .collect(java.util.stream.Collectors.toList()));
        
        // 移除实体
        for (Entity entity : entitiesToRemove) {
            entity.removeFromWorld();
        }
        
        if (!entitiesToRemove.isEmpty()) {
            System.out.println("🧹 清理区块 (" + chunkX + "," + chunkY + ") 中的 " + entitiesToRemove.size() + " 个实体");
        }
    }
    
    /**
     * 预加载区块（以玩家为中心，预加载半径内的区块）
     */
    private void preloadChunks(int centerChunkX, int centerChunkY) {
        if (bossIsolationMode) {
            // Boss房隔离：不进行任何预加载
            return;
        }
        ArrayList<String> preloadedChunks = new ArrayList<>();
        
        if (isHorizontalInfinite) {
            // 横向无限地图：只预加载左右区块
            for (int chunkX = centerChunkX - preloadRadius; chunkX <= centerChunkX + preloadRadius; chunkX++) {
                String chunkKey = chunkToKey(chunkX, 0); // Y坐标固定为0
                if (!loadedChunks.containsKey(chunkKey)) {
                    loadChunk(chunkX, 0);
                    preloadedChunks.add(chunkKey);
                }
            }
        } else {
            // 四向无限地图：预加载四个方向的区块
            for (int chunkY = centerChunkY - preloadRadius; chunkY <= centerChunkY + preloadRadius; chunkY++) {
                for (int chunkX = centerChunkX - preloadRadius; chunkX <= centerChunkX + preloadRadius; chunkX++) {
                    String chunkKey = chunkToKey(chunkX, chunkY);
                    if (!loadedChunks.containsKey(chunkKey)) {
                        loadChunk(chunkX, chunkY);
                        preloadedChunks.add(chunkKey);
                    }
                }
            }
        }
        
        if (!preloadedChunks.isEmpty()) {
            System.out.println("🚀 预加载了 " + preloadedChunks.size() + " 个区块: " + preloadedChunks);
        }
    }
    
    /**
     * 异步预加载区块
     */
    private void preloadChunksAsync(int centerChunkX, int centerChunkY) {
        if (bossIsolationMode) {
            // Boss房隔离：不进行任何预加载
            return;
        }
        ArrayList<String> chunksToPreload = new ArrayList<>();
        
        if (isHorizontalInfinite) {
            // 横向无限地图：只预加载左右区块
            for (int chunkX = centerChunkX - preloadRadius; chunkX <= centerChunkX + preloadRadius; chunkX++) {
                String chunkKey = chunkToKey(chunkX, 0); // Y坐标固定为0
                if (!loadedChunks.containsKey(chunkKey) && !stateManager.isLoading(chunkKey)) {
                    chunksToPreload.add(chunkKey);
                }
            }
        } else {
            // 四向无限地图：预加载四个方向的区块
            for (int chunkY = centerChunkY - preloadRadius; chunkY <= centerChunkY + preloadRadius; chunkY++) {
                for (int chunkX = centerChunkX - preloadRadius; chunkX <= centerChunkX + preloadRadius; chunkX++) {
                    String chunkKey = chunkToKey(chunkX, chunkY);
                    if (!loadedChunks.containsKey(chunkKey) && !stateManager.isLoading(chunkKey)) {
                        chunksToPreload.add(chunkKey);
                    }
                }
            }
        }
        
        if (!chunksToPreload.isEmpty()) {
            System.out.println("🚀 开始异步预加载 " + chunksToPreload.size() + " 个区块: " + chunksToPreload);
            asyncLoader.preloadChunksAsync(chunksToPreload);
        }
    }
    
    /**
     * 智能预加载区块（基于玩家移动方向）
     * 已废弃：使用简化的预加载策略
     */
    @Deprecated
    private void smartPreloadChunks(int currentChunkX, int previousChunkX) {
        // 不再使用智能预加载，避免频繁变向导致的性能问题
    }
    
    /**
     * 获取指定区块
     */
    public MapChunk getChunk(int chunkX, int chunkY) {
        String chunkKey = chunkToKey(chunkX, chunkY);
        MapChunk chunk = loadedChunks.get(chunkKey);
        if (chunk == null) {
            if (GameApp.DEBUG_MODE) {
                System.out.println("❌ 区块未找到: " + chunkKey + " (已加载区块: " + loadedChunks.keySet() + ")");
            }
        }
        return chunk;
    }
    
    /**
     * 检查指定世界坐标是否可通行
     */
    public boolean isPassable(double worldX, double worldY) {
        int chunkX = worldToChunkX(worldX);
        int chunkY = worldToChunkY(worldY);
        MapChunk chunk = getChunk(chunkX, chunkY);
        
        if (chunk != null) {
            // 检查是否在区块范围内
            double chunkLeft = chunkToWorldX(chunkX);
            double chunkRight = chunkLeft + getChunkWidthPixels();
            double chunkTop = chunkToWorldY(chunkY);
            double chunkBottom = chunkTop + getChunkHeightPixels();
            
            if (worldX >= chunkLeft && worldX < chunkRight && worldY >= chunkTop && worldY < chunkBottom) {
                // 在区块范围内，使用区块的碰撞检测
                boolean passable = chunk.isPassable(worldX, worldY);
                // 调试信息：只在边界附近打印
                if (Math.abs(worldX - 1536) < 100 || Math.abs(worldY - 864) < 100) {
                    //System.out.println("🔍 碰撞检测: 世界坐标(" + String.format("%.1f", worldX) + "," + String.format("%.1f", worldY) + ") -> 区块(" + chunkX + "," + chunkY + ") 可通行: " + passable);
                }
                return passable;
            } else {
                // 超出区块范围，检查相邻区块（抑制日志，避免频繁I/O影响性能）
                int correctChunkX = worldToChunkX(worldX);
                int correctChunkY = worldToChunkY(worldY);
                MapChunk correctChunk = getChunk(correctChunkX, correctChunkY);
                if (correctChunk != null) {
                    return correctChunk.isPassable(worldX, worldY);
                } else {
                    if (GameApp.DEBUG_MODE) {
                        System.out.println("⚠️ 跨区块检测失败: 区块(" + correctChunkX + "," + correctChunkY + ") 未加载");
                    }
                    return false;
                }
            }
        }
        
        // 区块未加载时默认为不可通行，防止敌人穿过未加载的障碍物
        // 这确保了碰撞检测的准确性，同时依赖预加载机制保证玩家周围区块已加载
        //System.out.println("⚠️ 区块未加载: 世界坐标(" + String.format("%.1f", worldX) + "," + String.format("%.1f", worldY) + ") -> 区块(" + chunkX + "," + chunkY + ") 不可通行");
        return false;
    }
    
    /**
     * 检查指定世界坐标是否不可通行
     */
    public boolean isUnaccessible(double worldX, double worldY) {
        return !isPassable(worldX, worldY);
    }
    
    /**
     * 获取玩家当前所在区块X坐标
     */
    public int getPlayerChunkX() {
        return playerChunkX;
    }
    
    /**
     * 获取玩家当前所在区块Y坐标
     */
    public int getPlayerChunkY() {
        return playerChunkY;
    }
    
    /**
     * 获取加载半径
     */
    public int getLoadRadius() {
        return loadRadius;
    }
    
    /**
     * 设置加载半径
     */
    public void setLoadRadius(int radius) {
        this.loadRadius = Math.max(1, radius);
        System.out.println("🔧 加载半径设置为: " + this.loadRadius);
    }
    
    /**
     * 获取预加载半径
     */
    public int getPreloadRadius() {
        return preloadRadius;
    }
    
    /**
     * 设置预加载半径
     */
    public void setPreloadRadius(int radius) {
        this.preloadRadius = Math.max(0, radius);
        System.out.println("🚀 预加载半径设置为: " + this.preloadRadius);
    }
    
    /**
     * 获取状态管理器
     */
    public ChunkStateManager getStateManager() {
        return stateManager;
    }
    
    /**
     * 获取异步加载器
     */
    public AsyncChunkLoader getAsyncLoader() {
        return asyncLoader;
    }
    
    /**
     * 设置是否使用异步加载
     */
    public void setUseAsyncLoading(boolean useAsyncLoading) {
        this.useAsyncLoading = useAsyncLoading;
        System.out.println("🔄 异步加载设置为: " + (useAsyncLoading ? "启用" : "禁用"));
    }
    
    /**
     * 是否使用异步加载
     */
    public boolean isUseAsyncLoading() {
        return useAsyncLoading;
    }
    
    /**
     * 获取当前加载的区块数量
     */
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }
    
    /**
     * 获取当前加载的区块坐标列表
     */
    public ArrayList<String> getLoadedChunkCoordinates() {
        return new ArrayList<>(loadedChunks.keySet());
    }
    
    /**
     * 打印当前状态
     */
    public void printStatus() {
        String mapType = isHorizontalInfinite ? "横向无限地图" : "四向无限地图";
        String mapTypeDescription = isHorizontalInfinite ? "test地图" : (mapName.equals("square") ? "square地图" : "dungeon地图");
        System.out.println("🌍 " + mapType + "状态 (" + mapTypeDescription + "):");
        System.out.println("   地图类型: " + mapType);
        System.out.println("   基础地图: " + mapName);
        System.out.println("   玩家区块: (" + playerChunkX + "," + playerChunkY + ")");
        System.out.println("   加载半径: " + loadRadius + " (3x3区块)");
        System.out.println("   预加载半径: " + preloadRadius);
        System.out.println("   异步加载: " + (useAsyncLoading ? "启用" : "禁用"));
        System.out.println("   已加载区块: " + getLoadedChunkCount() + " 个");
        System.out.println("   区块坐标: " + getLoadedChunkCoordinates());
        
        // 打印状态统计
        stateManager.printStateStatistics();
        
        // 打印异步加载统计
        if (useAsyncLoading) {
            asyncLoader.printLoadingStatistics();
        }
    }
    
    /**
     * 清理所有区块
     */
    public void cleanup() {
        System.out.println("🧹 清理所有区块...");
        
        // 取消所有异步加载任务
        if (useAsyncLoading) {
            asyncLoader.cancelAllLoading();
        }
        
        // 卸载所有区块
        for (MapChunk chunk : loadedChunks.values()) {
            chunk.unload();
        }
        loadedChunks.clear();
        
        // 清理状态管理器
        stateManager.clearAllStates();
        
        // 关闭异步加载器
        if (useAsyncLoading) {
            asyncLoader.shutdown();
        }
        
        System.out.println("✅ 清理完成");
    }
    
    /**
     * 世界坐标转区块坐标
     */
    public int worldToChunkX(double worldX) {
        return MapChunkFactory.worldToChunkX(worldX, mapName);
    }
    
    /**
     * 区块坐标转世界坐标
     */
    public double chunkToWorldX(int chunkX) {
        return MapChunkFactory.chunkToWorldX(chunkX, mapName);
    }
    
    /**
     * 获取区块宽度（像素）
     */
    public int getChunkWidthPixels() {
        return MapChunkFactory.getChunkWidthPixels(mapName);
    }
    
    /**
     * 获取区块高度（像素）
     */
    public int getChunkHeightPixels() {
        return MapChunkFactory.getChunkHeightPixels(mapName);
    }
    
    /**
     * 获取上次预加载的区块列表
     */
    private ArrayList<Integer> getLastPreloadedChunkList() {
        return lastPreloadedChunkList;
    }
    
    /**
     * 设置上次预加载的区块列表
     */
    private void setLastPreloadedChunkList(ArrayList<Integer> chunkList) {
        this.lastPreloadedChunkList = chunkList;
    }
    
    /**
     * 设置传送门管理器
     */
    public void setTeleportManager(TeleportManager teleportManager) {
        this.teleportManager = teleportManager;
    }
    
    /**
     * 获取传送门管理器
     */
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
    
    /**
     * 获取定时器瓦片管理器
     */
    public TimerTileManager getTimerTileManager() {
        return timerTileManager;
    }
    
    /**
     * 设置定时器瓦片管理器
     */
    public void setTimerTileManager(TimerTileManager timerTileManager) {
        this.timerTileManager = timerTileManager;
    }
    
    /**
     * 获取Boss房区块坐标
     */
    public static String getBossChunk1() {
        return BOSS_CHUNK_1;
    }
    
    /**
     * 获取基础地图名称
     */
    public String getMapName() {
        return mapName;
    }
    
    /**
     * 是否为横向无限地图
     */
    public boolean isHorizontalInfinite() {
        return isHorizontalInfinite;
    }
    
    /**
     * 获取Boss房地图名称
     */
    public String getBossMapName() {
        return bossMapName;
    }
}
