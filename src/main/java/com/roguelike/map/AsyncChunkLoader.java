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
    private Map<Integer, CompletableFuture<MapChunk>> loadingTasks;
    private ChunkStateManager stateManager;
    private AtomicInteger activeLoadingTasks;
    
    // 配置参数
    private final int maxConcurrentLoads;
    private final int loadingThreadPoolSize;
    private final long maxLoadingTimeMs;
    
    public AsyncChunkLoader(ChunkStateManager stateManager) {
        this.stateManager = stateManager;
        this.loadingTasks = new HashMap<>();
        this.activeLoadingTasks = new AtomicInteger(0);
        
        // 配置参数 - 优化性能
        this.maxConcurrentLoads = 5;  // 增加并发加载数
        this.loadingThreadPoolSize = 4;  // 增加线程池大小
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
    public CompletableFuture<MapChunk> loadChunkAsync(int chunkX) {
        // 检查是否已经在加载
        if (loadingTasks.containsKey(chunkX)) {
            return loadingTasks.get(chunkX);
        }
        
        // 检查并发限制
        if (activeLoadingTasks.get() >= maxConcurrentLoads) {
            System.out.println("⚠️ 达到最大并发加载限制，区块 " + chunkX + " 加入等待队列");
            return CompletableFuture.completedFuture(null);
        }
        
        // 设置状态为加载中
        stateManager.transitionToState(chunkX, ChunkState.LOADING);
        
        // 创建异步任务
        CompletableFuture<MapChunk> future = CompletableFuture.supplyAsync(() -> {
            try {
                activeLoadingTasks.incrementAndGet();
                long startTime = System.currentTimeMillis();
                
                System.out.println("🔄 开始异步加载区块 " + chunkX);
                
                // 创建并加载区块
                MapChunk chunk = new MapChunk(chunkX);
                chunk.load();
                
                long loadTime = System.currentTimeMillis() - startTime;
                System.out.println("✅ 区块 " + chunkX + " 异步加载完成，耗时: " + loadTime + "ms");
                
                // 设置状态为已加载
                stateManager.transitionToState(chunkX, ChunkState.LOADED);
                
                return chunk;
                
            } catch (Exception e) {
                System.err.println("❌ 区块 " + chunkX + " 异步加载失败: " + e.getMessage());
                stateManager.transitionToState(chunkX, ChunkState.UNLOADED);
                return null;
            } finally {
                activeLoadingTasks.decrementAndGet();
                loadingTasks.remove(chunkX);
            }
        }, loadingExecutor)
        .orTimeout(maxLoadingTimeMs, TimeUnit.MILLISECONDS)
        .exceptionally(throwable -> {
            System.err.println("⏰ 区块 " + chunkX + " 加载超时或异常: " + throwable.getMessage());
            stateManager.transitionToState(chunkX, ChunkState.UNLOADED);
            loadingTasks.remove(chunkX);
            return null;
        });
        
        // 记录加载任务
        loadingTasks.put(chunkX, future);
        
        return future;
    }
    
    /**
     * 批量预加载区块
     */
    public void preloadChunksAsync(List<Integer> chunkCoordinates) {
        if (chunkCoordinates == null || chunkCoordinates.isEmpty()) {
            return;
        }
        
        //System.out.println("🚀 开始批量预加载 " + chunkCoordinates.size() + " 个区块: " + chunkCoordinates);
        
        List<CompletableFuture<MapChunk>> futures = new ArrayList<>();
        
        for (int chunkX : chunkCoordinates) {
            // 跳过已加载或正在加载的区块
            if (stateManager.isLoaded(chunkX) || stateManager.isLoading(chunkX)) {
                continue;
            }
            
            CompletableFuture<MapChunk> future = loadChunkAsync(chunkX);
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
    public void cancelLoading(int chunkX) {
        CompletableFuture<MapChunk> future = loadingTasks.get(chunkX);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            loadingTasks.remove(chunkX);
            stateManager.transitionToState(chunkX, ChunkState.UNLOADED);
            System.out.println("🚫 取消区块 " + chunkX + " 的加载任务");
        }
    }
    
    /**
     * 取消所有加载任务
     */
    public void cancelAllLoading() {
        System.out.println("🚫 取消所有加载任务");
        for (int chunkX : new ArrayList<>(loadingTasks.keySet())) {
            cancelLoading(chunkX);
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
    public boolean isLoading(int chunkX) {
        return loadingTasks.containsKey(chunkX) && !loadingTasks.get(chunkX).isDone();
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
