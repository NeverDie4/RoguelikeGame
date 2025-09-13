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
    
    private Map<Integer, MapChunk> loadedChunks;  // 已加载的区块
    private int playerChunkX;                     // 玩家当前所在区块X坐标
    private int loadRadius;                       // 加载半径（区块数）
    private int preloadRadius;                    // 预加载半径（区块数）
    
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
    private static final int DEFAULT_LOAD_RADIUS = 2; // 默认加载半径：左右各2个区块
    private static final int DEFAULT_PRELOAD_RADIUS = 2; // 默认预加载半径：左右各2个区块（增加预加载范围）
    
    public InfiniteMapManager() {
        this.loadedChunks = new HashMap<>();
        this.playerChunkX = 0;
        this.loadRadius = DEFAULT_LOAD_RADIUS;
        this.preloadRadius = DEFAULT_PRELOAD_RADIUS;
        this.useAsyncLoading = true; // 默认启用异步加载
        this.lastUpdateTime = System.currentTimeMillis();
        
        // 初始化状态管理器
        this.stateManager = new ChunkStateManager();
        
        // 初始化异步加载器
        this.asyncLoader = new AsyncChunkLoader(stateManager);
        
        // 初始加载玩家所在区块和预加载区块
        if (useAsyncLoading) {
            loadChunkAsync(0);
            preloadChunksAsync(0);
        } else {
            loadChunk(0);
            preloadChunks(0);
        }
        
        System.out.println("🌍 无限地图管理器初始化完成");
        System.out.println("   异步加载: " + (useAsyncLoading ? "启用" : "禁用"));
    }
    
    /**
     * 更新区块加载状态（根据玩家位置）
     */
    public void updateChunks(int newPlayerChunkX) {
        if (newPlayerChunkX == playerChunkX) {
            return; // 玩家仍在同一区块
        }
        
        int oldPlayerChunkX = playerChunkX;
        playerChunkX = newPlayerChunkX;
        lastUpdateTime = System.currentTimeMillis();
        
        System.out.println("🔄 玩家从区块 " + oldPlayerChunkX + " 移动到区块 " + playerChunkX);
        
        // 卸载远离的区块
        unloadDistantChunks();
        
        // 加载需要的区块
        if (useAsyncLoading) {
            loadRequiredChunksAsync();
        } else {
            loadRequiredChunks();
        }
        
        // 智能预加载区块（基于移动方向）
        if (useAsyncLoading) {
            smartPreloadChunks(newPlayerChunkX, oldPlayerChunkX);
        } else {
            preloadChunks(newPlayerChunkX);
        }
        
    }
    
    /**
     * 主动预加载（在玩家移动过程中调用）
     */
    public void proactivePreload() {
        if (!useAsyncLoading) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // 如果玩家在区块边界附近，预加载下一个区块
        if (currentTime - lastUpdateTime < 1000) { // 1秒内的移动
            // 这里可以根据玩家移动速度预测下一个区块
            // 暂时预加载当前区块周围的所有区块
            ArrayList<Integer> chunksToPreload = new ArrayList<>();
            
            for (int chunkX = playerChunkX - preloadRadius; chunkX <= playerChunkX + preloadRadius; chunkX++) {
                if (!loadedChunks.containsKey(chunkX) && !stateManager.isLoading(chunkX)) {
                    chunksToPreload.add(chunkX);
                }
            }
            
            if (!chunksToPreload.isEmpty()) {
                System.out.println("⚡ 主动预加载 " + chunksToPreload.size() + " 个区块: " + chunksToPreload);
                asyncLoader.preloadChunksAsync(chunksToPreload);
            }
        }
    }
    
    /**
     * 基于视角的智能预加载
     * 根据玩家当前世界坐标，预加载视角范围内的区块
     */
    public void viewportBasedPreload(double playerWorldX, double playerWorldY) {
        if (!useAsyncLoading) {
            return;
        }
        
        // 检查玩家是否移动了足够距离（避免微小移动触发预加载）
        double moveThreshold = 50.0; // 移动阈值：50像素
        if (!Double.isNaN(lastPlayerX) && !Double.isNaN(lastPlayerY)) {
            double deltaX = Math.abs(playerWorldX - lastPlayerX);
            double deltaY = Math.abs(playerWorldY - lastPlayerY);
            if (deltaX < moveThreshold && deltaY < moveThreshold) {
                return; // 移动距离太小，跳过预加载
            }
        }
        
        // 节流机制：避免过于频繁的调用
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastViewportPreloadTime < VIEWPORT_PRELOAD_INTERVAL) {
            return;
        }
        lastViewportPreloadTime = currentTime;
        
        // 更新玩家位置记录
        lastPlayerX = playerWorldX;
        lastPlayerY = playerWorldY;
        
        // 获取实际的屏幕尺寸和缩放比例
        double viewportWidth = FXGL.getAppWidth();
        double scale = FXGL.getGameScene().getViewport().getZoom();
        
        // 计算视角范围的世界坐标
        double viewportLeft = playerWorldX - (viewportWidth * scale / 2);
        double viewportRight = playerWorldX + (viewportWidth * scale / 2);
        
        // 计算需要预加载的区块范围（扩大预加载范围以确保流畅）
        int leftChunk = InfiniteMapManager.worldToChunkX(viewportLeft) - 1; // 减少预加载范围，避免过度加载
        int rightChunk = InfiniteMapManager.worldToChunkX(viewportRight) + 1; // 减少预加载范围，避免过度加载
        
        ArrayList<Integer> chunksToPreload = new ArrayList<>();
        
        for (int chunkX = leftChunk; chunkX <= rightChunk; chunkX++) {
            // 检查区块是否需要预加载：未在loadedChunks中且不在加载中
            if (!loadedChunks.containsKey(chunkX) && !stateManager.isLoading(chunkX)) {
                chunksToPreload.add(chunkX);
            }
        }
        
        if (!chunksToPreload.isEmpty()) {
            // 检查是否与上次预加载的区块相同，避免重复预加载
            if (chunksToPreload.size() != lastPreloadedChunks || 
                !chunksToPreload.equals(getLastPreloadedChunkList())) {
                System.out.println("👁️ 视角预加载 " + chunksToPreload.size() + " 个区块: " + chunksToPreload);
                
                // 立即加载视角范围内的区块并添加到场景
                for (int chunkX : chunksToPreload) {
                    loadChunkAsync(chunkX);
                }
                
                lastPreloadedChunks = chunksToPreload.size();
                setLastPreloadedChunkList(new ArrayList<>(chunksToPreload));
            }
        }
    }
    
    /**
     * 卸载远离玩家的区块
     */
    private void unloadDistantChunks() {
        ArrayList<Integer> chunksToUnload = new ArrayList<>();
        
        for (int chunkX : loadedChunks.keySet()) {
            if (Math.abs(chunkX - playerChunkX) > loadRadius) {
                chunksToUnload.add(chunkX);
            }
        }
        
        for (int chunkX : chunksToUnload) {
            // 取消正在加载的任务
            if (useAsyncLoading && asyncLoader.isLoading(chunkX)) {
                asyncLoader.cancelLoading(chunkX);
            }
            unloadChunk(chunkX);
        }
        
        if (!chunksToUnload.isEmpty()) {
            System.out.println("🗑️ 卸载了 " + chunksToUnload.size() + " 个区块: " + chunksToUnload);
        }
    }
    
    /**
     * 加载玩家周围需要的区块
     */
    private void loadRequiredChunks() {
        ArrayList<Integer> chunksToLoad = new ArrayList<>();
        
        for (int chunkX = playerChunkX - loadRadius; chunkX <= playerChunkX + loadRadius; chunkX++) {
            if (!loadedChunks.containsKey(chunkX)) {
                chunksToLoad.add(chunkX);
            }
        }
        
        for (int chunkX : chunksToLoad) {
            loadChunk(chunkX);
        }
        
        if (!chunksToLoad.isEmpty()) {
            System.out.println("📦 加载了 " + chunksToLoad.size() + " 个区块: " + chunksToLoad);
        }
    }
    
    /**
     * 异步加载玩家周围需要的区块
     */
    private void loadRequiredChunksAsync() {
        ArrayList<Integer> chunksToLoad = new ArrayList<>();
        
        for (int chunkX = playerChunkX - loadRadius; chunkX <= playerChunkX + loadRadius; chunkX++) {
            // 检查区块是否需要加载：未在loadedChunks中且不在加载中
            if (!loadedChunks.containsKey(chunkX) && !stateManager.isLoading(chunkX)) {
                chunksToLoad.add(chunkX);
            }
        }
        
        if (!chunksToLoad.isEmpty()) {
            System.out.println("📦 开始异步加载 " + chunksToLoad.size() + " 个区块: " + chunksToLoad);
            for (int chunkX : chunksToLoad) {
                loadChunkAsync(chunkX);
            }
        }
    }
    
    /**
     * 加载指定区块
     */
    public void loadChunk(int chunkX) {
        if (loadedChunks.containsKey(chunkX)) {
            return; // 已加载
        }
        
        stateManager.transitionToState(chunkX, ChunkState.LOADING);
        MapChunk chunk = new MapChunk(chunkX);
        chunk.load();
        chunk.addToScene(); // 同步加载时直接添加到场景
        loadedChunks.put(chunkX, chunk);
        stateManager.transitionToState(chunkX, ChunkState.LOADED);
    }
    
    /**
     * 异步加载指定区块
     */
    public void loadChunkAsync(int chunkX) {
        if (loadedChunks.containsKey(chunkX) || stateManager.isLoading(chunkX)) {
            return; // 已加载或正在加载
        }
        
        CompletableFuture<MapChunk> future = asyncLoader.loadChunkAsync(chunkX);
        if (future != null) {
            future.thenAccept(chunk -> {
                if (chunk != null) {
                    loadedChunks.put(chunkX, chunk);
                    // 立即在主线程中添加地图视图到场景，减少延迟
                    Platform.runLater(() -> {
                        chunk.addToScene();
                        System.out.println("✅ 区块 " + chunkX + " 异步加载完成并添加到场景");
                    });
                }
            });
        }
    }
    
    /**
     * 卸载指定区块
     */
    public void unloadChunk(int chunkX) {
        stateManager.transitionToState(chunkX, ChunkState.UNLOADING);
        MapChunk chunk = loadedChunks.remove(chunkX);
        if (chunk != null) {
            // 清理区块内的敌人和子弹
            cleanupEntitiesInChunk(chunkX);
            chunk.unload();
        }
        stateManager.transitionToState(chunkX, ChunkState.UNLOADED);
    }
    
    /**
     * 清理指定区块内的实体（敌人、子弹等）
     */
    private void cleanupEntitiesInChunk(int chunkX) {
        double chunkLeft = chunkToWorldX(chunkX);
        double chunkRight = chunkLeft + getChunkWidthPixels();
        
        // 获取所有敌人和子弹实体
        List<Entity> entitiesToRemove = new ArrayList<>();
        
        // 清理敌人
        entitiesToRemove.addAll(FXGL.getGameWorld().getEntitiesByType().stream()
            .filter(e -> e instanceof com.roguelike.entities.Enemy)
            .filter(e -> {
                double x = e.getX();
                return x >= chunkLeft && x < chunkRight;
            })
            .collect(java.util.stream.Collectors.toList()));
        
        // 清理子弹
        entitiesToRemove.addAll(FXGL.getGameWorld().getEntitiesByType().stream()
            .filter(e -> e instanceof com.roguelike.entities.Bullet)
            .filter(e -> {
                double x = e.getX();
                return x >= chunkLeft && x < chunkRight;
            })
            .collect(java.util.stream.Collectors.toList()));
        
        // 移除实体
        for (Entity entity : entitiesToRemove) {
            entity.removeFromWorld();
        }
        
        if (!entitiesToRemove.isEmpty()) {
            System.out.println("🧹 清理区块 " + chunkX + " 中的 " + entitiesToRemove.size() + " 个实体");
        }
    }
    
    /**
     * 预加载区块（以玩家为中心，预加载半径内的区块）
     */
    private void preloadChunks(int centerChunkX) {
        ArrayList<Integer> preloadedChunks = new ArrayList<>();
        
        // 预加载玩家周围的区块
        for (int chunkX = centerChunkX - preloadRadius; chunkX <= centerChunkX + preloadRadius; chunkX++) {
            if (!loadedChunks.containsKey(chunkX)) {
                loadChunk(chunkX);
                preloadedChunks.add(chunkX);
            }
        }
        
        if (!preloadedChunks.isEmpty()) {
            System.out.println("🚀 预加载了 " + preloadedChunks.size() + " 个区块: " + preloadedChunks);
        }
    }
    
    /**
     * 异步预加载区块
     */
    private void preloadChunksAsync(int centerChunkX) {
        ArrayList<Integer> chunksToPreload = new ArrayList<>();
        
        // 预加载玩家周围的区块
        for (int chunkX = centerChunkX - preloadRadius; chunkX <= centerChunkX + preloadRadius; chunkX++) {
            if (!loadedChunks.containsKey(chunkX) && !stateManager.isLoading(chunkX)) {
                chunksToPreload.add(chunkX);
            }
        }
        
        if (!chunksToPreload.isEmpty()) {
            System.out.println("🚀 开始异步预加载 " + chunksToPreload.size() + " 个区块: " + chunksToPreload);
            asyncLoader.preloadChunksAsync(chunksToPreload);
        }
    }
    
    /**
     * 智能预加载区块（基于玩家移动方向）
     */
    private void smartPreloadChunks(int currentChunkX, int previousChunkX) {
        ArrayList<Integer> chunksToPreload = new ArrayList<>();
        
        // 计算移动方向
        int direction = currentChunkX > previousChunkX ? 1 : -1;
        
        // 在移动方向上预加载更多区块
        int forwardRadius = preloadRadius + 1; // 移动方向多预加载1个区块
        int backwardRadius = preloadRadius;    // 反方向正常预加载
        
        for (int chunkX = currentChunkX - backwardRadius; chunkX <= currentChunkX + forwardRadius; chunkX++) {
            // 跳过已加载或正在加载的区块
            if (loadedChunks.containsKey(chunkX) || stateManager.isLoading(chunkX)) {
                continue;
            }
            
            // 优先加载移动方向的区块
            boolean isForwardDirection = (direction > 0 && chunkX > currentChunkX) || 
                                       (direction < 0 && chunkX < currentChunkX);
            
            if (isForwardDirection) {
                chunksToPreload.add(0, chunkX); // 添加到前面，优先加载
            } else {
                chunksToPreload.add(chunkX);
            }
        }
        
        if (!chunksToPreload.isEmpty()) {
            System.out.println("🧠 智能预加载 " + chunksToPreload.size() + " 个区块 (方向: " + 
                             (direction > 0 ? "右" : "左") + "): " + chunksToPreload);
            asyncLoader.preloadChunksAsync(chunksToPreload);
        }
    }
    
    /**
     * 获取指定区块
     */
    public MapChunk getChunk(int chunkX) {
        return loadedChunks.get(chunkX);
    }
    
    /**
     * 检查指定世界坐标是否可通行
     */
    public boolean isPassable(double worldX, double worldY) {
        int chunkX = MapChunk.worldToChunkX(worldX);
        MapChunk chunk = getChunk(chunkX);
        
        if (chunk != null) {
            return chunk.isPassable(worldX, worldY);
        }
        
        // 区块未加载时默认可通行
        return true;
    }
    
    /**
     * 检查指定世界坐标是否不可通行
     */
    public boolean isUnaccessible(double worldX, double worldY) {
        return !isPassable(worldX, worldY);
    }
    
    /**
     * 获取玩家当前所在区块
     */
    public int getPlayerChunkX() {
        return playerChunkX;
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
    public ArrayList<Integer> getLoadedChunkCoordinates() {
        return new ArrayList<>(loadedChunks.keySet());
    }
    
    /**
     * 打印当前状态
     */
    public void printStatus() {
        System.out.println("🌍 无限地图状态:");
        System.out.println("   玩家区块: " + playerChunkX);
        System.out.println("   加载半径: " + loadRadius);
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
    public static int worldToChunkX(double worldX) {
        return MapChunk.worldToChunkX(worldX);
    }
    
    /**
     * 区块坐标转世界坐标
     */
    public static double chunkToWorldX(int chunkX) {
        return MapChunk.chunkToWorldX(chunkX);
    }
    
    /**
     * 获取区块宽度（像素）
     */
    public static int getChunkWidthPixels() {
        return MapChunk.getChunkWidthPixels();
    }
    
    /**
     * 获取区块高度（像素）
     */
    public static int getChunkHeightPixels() {
        return MapChunk.getChunkHeightPixels();
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
}
