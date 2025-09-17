package com.roguelike.map;

import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步区块加载器
 */
public class AsyncChunkLoader {
    
    private ExecutorService loadingExecutor;
    private Map<String, CompletableFuture<MapChunk>> loadingTasks;
    private ChunkStateManager stateManager;
    private AtomicInteger activeLoadingTasks;
    private String mapName;
    
    // 配置参数
    private final int maxConcurrentLoads;
    private final int loadingThreadPoolSize;
    private final long maxLoadingTimeMs;
    
    public AsyncChunkLoader(ChunkStateManager stateManager) {
        this(stateManager, "test"); // 默认地图名称
    }
    
    public AsyncChunkLoader(ChunkStateManager stateManager, String mapName) {
        this.stateManager = stateManager;
        this.mapName = mapName;
        this.loadingTasks = new HashMap<>();
        this.activeLoadingTasks = new AtomicInteger(0);
        
        // 配置参数 - 优化性能
        this.maxConcurrentLoads = 2;  // 降低并发，避免短时间解码过多PNG
        this.loadingThreadPoolSize = 2;  // 降低线程池，减轻内存/IO压力
        this.maxLoadingTimeMs = 2000;  // 减少超时时间，更快失败
        
        // 创建线程池
        this.loadingExecutor = Executors.newFixedThreadPool(
            loadingThreadPoolSize,
            r -> {
                Thread t = new Thread(r, "ChunkLoader-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
        );
        
        System.out.println("🚀 异步区块加载器初始化完成");
        System.out.println("   最大并发加载: " + maxConcurrentLoads);
        System.out.println("   线程池大小: " + loadingThreadPoolSize);
        System.out.println("   最大加载时间: " + maxLoadingTimeMs + "ms");
    }
    
    /**
     * 异步加载区块
     */
    public CompletableFuture<MapChunk> loadChunkAsync(int chunkX, int chunkY) {
        return loadChunkAsync(chunkX, chunkY, mapName);
    }
    
    /**
     * 异步加载区块（指定地图名称）
     */
    public CompletableFuture<MapChunk> loadChunkAsync(int chunkX, int chunkY, String chunkMapName) {
        String chunkKey = chunkX + "," + chunkY;
        // 检查是否已经在加载
        if (loadingTasks.containsKey(chunkKey)) {
            return loadingTasks.get(chunkKey);
        }
        
        // 检查并发限制
        if (activeLoadingTasks.get() >= maxConcurrentLoads) {
            System.out.println("⚠️ 达到最大并发加载限制，区块 (" + chunkX + "," + chunkY + ") 加入等待队列");
            return CompletableFuture.completedFuture(null);
        }
        
        // 设置状态为加载中
        stateManager.transitionToState(chunkKey, ChunkState.LOADING);
        
        // 创建异步任务
        CompletableFuture<MapChunk> future = CompletableFuture.supplyAsync(() -> {
            try {
                activeLoadingTasks.incrementAndGet();
                long startTime = System.currentTimeMillis();
                
                System.out.println("🔄 开始异步加载区块 (" + chunkX + "," + chunkY + ")");
                
                // 创建并加载区块
                MapChunk chunk = new MapChunk(chunkX, chunkY, chunkMapName);
                chunk.load();
                
                long loadTime = System.currentTimeMillis() - startTime;
                System.out.println("✅ 区块 (" + chunkX + "," + chunkY + ") 异步加载完成，耗时: " + loadTime + "ms");
                
                // 设置状态为已加载
                stateManager.transitionToState(chunkKey, ChunkState.LOADED);
                
                return chunk;
                
            } catch (Exception e) {
                System.err.println("❌ 区块 (" + chunkX + "," + chunkY + ") 异步加载失败: " + e.getMessage());
                stateManager.transitionToState(chunkKey, ChunkState.UNLOADED);
                return null;
            } finally {
                activeLoadingTasks.decrementAndGet();
                loadingTasks.remove(chunkKey);
            }
        }, loadingExecutor)
        .orTimeout(maxLoadingTimeMs, TimeUnit.MILLISECONDS)
        .exceptionally(throwable -> {
            System.err.println("⏰ 区块 (" + chunkX + "," + chunkY + ") 加载超时或异常: " + throwable.getMessage());
            stateManager.transitionToState(chunkKey, ChunkState.UNLOADED);
            loadingTasks.remove(chunkKey);
            return null;
        });
        
        // 记录加载任务
        loadingTasks.put(chunkKey, future);
        
        return future;
    }
    
    /**
     * 批量预加载区块
     */
    public void preloadChunksAsync(List<String> chunkKeys) {
        if (chunkKeys == null || chunkKeys.isEmpty()) {
            return;
        }
        
        //System.out.println("🚀 开始批量预加载 " + chunkKeys.size() + " 个区块: " + chunkKeys);
        
        List<CompletableFuture<MapChunk>> futures = new ArrayList<>();
        
        for (String chunkKey : chunkKeys) {
            // 跳过已加载或正在加载的区块
            if (stateManager.isLoaded(chunkKey) || stateManager.isLoading(chunkKey)) {
                continue;
            }
            
            String[] coords = chunkKey.split(",");
            int chunkX = Integer.parseInt(coords[0]);
            int chunkY = Integer.parseInt(coords[1]);
            
            CompletableFuture<MapChunk> future = loadChunkAsync(chunkX, chunkY);
            if (future != null) {
                futures.add(future);
            }
        }
        
        // 不等待所有预加载完成，让它们并行进行
        if (!futures.isEmpty()) {
            System.out.println("⚡ 启动 " + futures.size() + " 个并行预加载任务");
        }
    }
    
    /**
     * 取消加载任务
     */
    public void cancelLoading(String chunkKey) {
        CompletableFuture<MapChunk> future = loadingTasks.get(chunkKey);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            loadingTasks.remove(chunkKey);
            stateManager.transitionToState(chunkKey, ChunkState.UNLOADED);
            System.out.println("🚫 取消区块 " + chunkKey + " 的加载任务");
        }
    }
    
    /**
     * 取消所有加载任务
     */
    public void cancelAllLoading() {
        System.out.println("🚫 取消所有加载任务");
        for (String chunkKey : new ArrayList<>(loadingTasks.keySet())) {
            cancelLoading(chunkKey);
        }
    }
    
    /**
     * 获取当前加载中的区块数量
     */
    public int getActiveLoadingCount() {
        return activeLoadingTasks.get();
    }
    
    /**
     * 获取等待加载的区块数量
     */
    public int getPendingLoadingCount() {
        return loadingTasks.size();
    }
    
    /**
     * 检查是否正在加载指定区块
     */
    public boolean isLoading(String chunkKey) {
        return loadingTasks.containsKey(chunkKey) && !loadingTasks.get(chunkKey).isDone();
    }
    
    /**
     * 等待指定区块加载完成
     */
    public MapChunk waitForChunk(int chunkX) {
        CompletableFuture<MapChunk> future = loadingTasks.get(chunkX);
        if (future != null) {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("❌ 等待区块 " + chunkX + " 加载时出现异常: " + e.getMessage());
                return null;
            }
        }
        return null;
    }
    
    /**
     * 获取加载统计信息
     */
    public void printLoadingStatistics() {
        System.out.println("📊 异步加载统计:");
        System.out.println("   活跃加载任务: " + getActiveLoadingCount());
        System.out.println("   等待加载任务: " + getPendingLoadingCount());
        System.out.println("   最大并发加载: " + maxConcurrentLoads);
        System.out.println("   线程池大小: " + loadingThreadPoolSize);
    }
    
    /**
     * 关闭加载器
     */
    public void shutdown() {
        System.out.println("🛑 关闭异步区块加载器");
        cancelAllLoading();
        loadingExecutor.shutdown();
        try {
            if (!loadingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                loadingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            loadingExecutor.shutdownNow();
        }
    }
}
