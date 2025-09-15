package com.roguelike.map;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.application.Platform;

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
    private static final int DEFAULT_LOAD_RADIUS = 1; // 默认加载半径：5个区块
    private static final int DEFAULT_PRELOAD_RADIUS = 2; // 默认预加载半径：5个区块
    
    // 地图类型配置
    private boolean isHorizontalInfinite; // 是否为横向无限地图
    
    // 特殊区块地图配置：2D坐标 -> 地图名称
    // 根据基础地图名称动态生成特殊地图名称
    private Map<String, String> specialChunkMaps;
    
    // Boss房区块配置：2D坐标
    private static final String BOSS_CHUNK_1 = "3,0";
    private static final String BOSS_CHUNK_2 = "0,3";
    private String bossMapName;
    
    // 传送门管理器引用
    private TeleportManager teleportManager;
    
    // 定时器瓦片管理器
    private TimerTileManager timerTileManager;
    
    public InfiniteMapManager() {
        this("square"); // 默认地图名称，使用square地图
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
        
        // 根据基础地图名称动态生成特殊地图配置
        this.specialChunkMaps = new HashMap<>();
        if (isHorizontalInfinite) {
            // 横向无限地图：只配置X方向的特殊区块
            this.specialChunkMaps.put("2,0", mapName + "_door");    // 传送门地图 (2,0)
            this.specialChunkMaps.put("3,0", mapName + "_boss");    // Boss房 (3,0)
        } else {
            // 四向无限地图：配置四个方向的特殊区块
            this.specialChunkMaps.put("2,0", mapName + "_door");    // 传送门地图 (2,0)
            this.specialChunkMaps.put("0,2", mapName + "_door");    // 传送门地图 (0,2)
            this.specialChunkMaps.put("3,0", mapName + "_boss");    // Boss房 (3,0)
            this.specialChunkMaps.put("0,3", mapName + "_boss");    // Boss房 (0,3)
        }
        this.bossMapName = mapName + "_boss";
        
        // 判断地图类型：test地图使用横向无限地图，square地图使用四向无限地图
        this.isHorizontalInfinite = "test".equals(mapName);
        
        // 初始化状态管理器
        this.stateManager = new ChunkStateManager();
        
        // 初始化异步加载器
        this.asyncLoader = new AsyncChunkLoader(stateManager, mapName);
        
        // 初始化定时器瓦片管理器
        this.timerTileManager = new TimerTileManager();
        
        // 初始加载玩家所在区块和预加载区块
        System.out.println("🔧 开始初始加载区块...");
        if (useAsyncLoading) {
            loadChunkAsync(0, 0);
            preloadChunksAsync(0, 0);
        } else {
            loadChunk(0, 0);
            preloadChunks(0, 0);
        }
        
        // 强制同步加载相邻区块以确保可用性
        System.out.println("🔧 强制加载相邻区块以确保可用性...");
        if (isHorizontalInfinite) {
            // 横向无限地图：只加载左右区块
            try {
                loadChunk(1, 0);   // 右侧区块
                System.out.println("✅ 右侧区块(1,0)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 右侧区块(1,0)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            try {
                loadChunk(-1, 0);  // 左侧区块
                System.out.println("✅ 左侧区块(-1,0)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 左侧区块(-1,0)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // 四向无限地图：加载四个方向的区块
            try {
                loadChunk(1, 0);   // 右侧区块
                System.out.println("✅ 右侧区块(1,0)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 右侧区块(1,0)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            try {
                loadChunk(-1, 0);  // 左侧区块
                System.out.println("✅ 左侧区块(-1,0)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 左侧区块(-1,0)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            try {
                loadChunk(0, 1);   // 下方区块
                System.out.println("✅ 下方区块(0,1)加载成功");
            } catch (Exception e) {
                System.err.println("❌ 下方区块(0,1)加载失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            try {
                loadChunk(0, -1);  // 上方区块
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
        return MapChunkFactory.worldToChunkY(worldY, mapName);
    }
    
    public double chunkToWorldY(int chunkY) {
        return MapChunkFactory.chunkToWorldY(chunkY, mapName);
    }
    
    /**
     * 更新区块加载状态（根据玩家位置）
     */
    public void updateChunks(int newPlayerChunkX, int newPlayerChunkY) {
        if (isHorizontalInfinite) {
            // 横向无限地图：Y坐标固定为0
            newPlayerChunkY = 0;
        }
        
        if (newPlayerChunkX == playerChunkX && newPlayerChunkY == playerChunkY) {
            return; // 玩家仍在同一区块
        }
        
        // 检查是否尝试进入Boss房区块
        String newChunkKey = chunkToKey(newPlayerChunkX, newPlayerChunkY);
        boolean isBossChunk = false;
        
        if (isHorizontalInfinite) {
            // 横向无限地图：只有(3,0)是Boss房区块
            isBossChunk = "3,0".equals(newChunkKey);
        } else {
            // 四向无限地图：(3,0)和(0,3)都是Boss房区块
            isBossChunk = newChunkKey.equals(BOSS_CHUNK_1) || newChunkKey.equals(BOSS_CHUNK_2);
        }
        
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
        ArrayList<String> chunksToUnload = new ArrayList<>();
        
        for (String chunkKey : loadedChunks.keySet()) {
            int[] chunkCoords = keyToChunk(chunkKey);
            int chunkX = chunkCoords[0];
            int chunkY = chunkCoords[1];
            
            boolean shouldUnload = false;
            
            if (isHorizontalInfinite) {
                // 横向无限地图：只检查X方向距离
                int distanceX = Math.abs(chunkX - playerChunkX);
                if (distanceX > loadRadius) {
                    shouldUnload = true;
                }
            } else {
                // 四向无限地图：检查X和Y方向距离
                int distanceX = Math.abs(chunkX - playerChunkX);
                int distanceY = Math.abs(chunkY - playerChunkY);
                if (distanceX > loadRadius || distanceY > loadRadius) {
                    shouldUnload = true;
                }
            }
            
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
        ArrayList<String> chunksToLoad = new ArrayList<>();
        
        if (isHorizontalInfinite) {
            // 横向无限地图：只加载左右区块
            for (int chunkX = playerChunkX - loadRadius; chunkX <= playerChunkX + loadRadius; chunkX++) {
                String chunkKey = chunkToKey(chunkX, 0); // Y坐标固定为0
                if (!loadedChunks.containsKey(chunkKey)) {
                    chunksToLoad.add(chunkKey);
                    // 调试信息：显示要加载的区块
                    if (chunkX > 3) {
                        System.out.println("🔍 准备加载区块: " + chunkKey + " (玩家区块: " + playerChunkX + ")");
                    }
                }
            }
        } else {
            // 四向无限地图：加载四个方向的区块
            for (int chunkY = playerChunkY - loadRadius; chunkY <= playerChunkY + loadRadius; chunkY++) {
                for (int chunkX = playerChunkX - loadRadius; chunkX <= playerChunkX + loadRadius; chunkX++) {
                    String chunkKey = chunkToKey(chunkX, chunkY);
                    if (!loadedChunks.containsKey(chunkKey)) {
                        chunksToLoad.add(chunkKey);
                    }
                }
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
        ArrayList<String> chunksToLoad = new ArrayList<>();
        
        if (isHorizontalInfinite) {
            // 横向无限地图：只加载左右区块
            for (int chunkX = playerChunkX - loadRadius; chunkX <= playerChunkX + loadRadius; chunkX++) {
                String chunkKey = chunkToKey(chunkX, 0); // Y坐标固定为0
                // 检查区块是否需要加载：未在loadedChunks中且不在加载中
                if (!loadedChunks.containsKey(chunkKey) && !stateManager.isLoading(chunkKey)) {
                    chunksToLoad.add(chunkKey);
                    // 调试信息：显示要异步加载的区块
                    if (chunkX > 3) {
                        System.out.println("🔍 准备异步加载区块: " + chunkKey + " (玩家区块: " + playerChunkX + ")");
                    }
                }
            }
        } else {
            // 四向无限地图：加载四个方向的区块
            for (int chunkY = playerChunkY - loadRadius; chunkY <= playerChunkY + loadRadius; chunkY++) {
                for (int chunkX = playerChunkX - loadRadius; chunkX <= playerChunkX + loadRadius; chunkX++) {
                    String chunkKey = chunkToKey(chunkX, chunkY);
                    // 检查区块是否需要加载：未在loadedChunks中且不在加载中
                    if (!loadedChunks.containsKey(chunkKey) && !stateManager.isLoading(chunkKey)) {
                        chunksToLoad.add(chunkKey);
                    }
                }
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
    private String getMapNameForChunk(int chunkX, int chunkY) {
        String chunkKey = chunkToKey(chunkX, chunkY);
        String mapNameForChunk = specialChunkMaps.getOrDefault(chunkKey, mapName);
        
        // 调试信息：显示区块对应的地图名称
        if (isHorizontalInfinite && chunkX > 3) {
            System.out.println("🔍 区块 (" + chunkX + "," + chunkY + ") 使用地图: " + mapNameForChunk);
        }
        
        return mapNameForChunk;
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
        
        System.out.println("🗺️ 区块 (" + chunkX + "," + chunkY + ") 使用地图: " + chunkMapName);
        
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
                        chunk.addToScene();
                        
                        // 扫描新加载区块中的定时器瓦片
                        if (timerTileManager != null) {
                            timerTileManager.scanChunkForTimerTiles(chunk);
                        }
                        
                        System.out.println("✅ 区块 (" + chunkX + "," + chunkY + ") 异步加载完成并添加到场景 (地图: " + chunkMapName + ")");
                        
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
            System.out.println("❌ 区块未找到: " + chunkKey + " (已加载区块: " + loadedChunks.keySet() + ")");
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
                // 超出区块范围，检查相邻区块
                System.out.println("🔍 跨区块检测: 世界坐标(" + String.format("%.1f", worldX) + "," + String.format("%.1f", worldY) + ") -> 区块(" + chunkX + "," + chunkY + ") 超出范围");
                
                // 重新计算正确的区块坐标
                int correctChunkX = worldToChunkX(worldX);
                int correctChunkY = worldToChunkY(worldY);
                MapChunk correctChunk = getChunk(correctChunkX, correctChunkY);
                
                if (correctChunk != null) {
                    boolean passable = correctChunk.isPassable(worldX, worldY);
                    System.out.println("🔍 跨区块检测结果: 世界坐标(" + String.format("%.1f", worldX) + "," + String.format("%.1f", worldY) + ") -> 区块(" + correctChunkX + "," + correctChunkY + ") 可通行: " + passable);
                    return passable;
                } else {
                    System.out.println("⚠️ 跨区块检测失败: 区块(" + correctChunkX + "," + correctChunkY + ") 未加载");
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
        System.out.println("🌍 " + mapType + "状态:");
        System.out.println("   地图类型: " + mapType);
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
    
    public static String getBossChunk2() {
        return BOSS_CHUNK_2;
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
