package com.roguelike.map;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * 区块状态管理器
 */
public class ChunkStateManager {
    
    private Map<String, ChunkState> chunkStates;
    private Map<String, Long> stateChangeTimes; // 状态变更时间戳
    
    public ChunkStateManager() {
        this.chunkStates = new HashMap<>();
        this.stateChangeTimes = new HashMap<>();
    }
    
    /**
     * 状态转换管理
     */
    public void transitionToState(String chunkKey, ChunkState newState) {
        ChunkState oldState = chunkStates.get(chunkKey);
        chunkStates.put(chunkKey, newState);
        stateChangeTimes.put(chunkKey, System.currentTimeMillis());
        
        if (oldState != null && oldState != newState) {
            System.out.println("🔄 区块 " + chunkKey + " 状态变更: " + oldState + " -> " + newState);
        }
    }
    
    /**
     * 获取区块状态
     */
    public ChunkState getChunkState(String chunkKey) {
        return chunkStates.getOrDefault(chunkKey, ChunkState.UNLOADED);
    }
    
    /**
     * 检查区块是否处于指定状态
     */
    public boolean isInState(String chunkKey, ChunkState state) {
        return getChunkState(chunkKey) == state;
    }
    
    /**
     * 检查区块是否已加载（包括LOADED和CACHED状态）
     */
    public boolean isLoaded(String chunkKey) {
        ChunkState state = getChunkState(chunkKey);
        return state == ChunkState.LOADED || state == ChunkState.CACHED;
    }
    
    /**
     * 检查区块是否正在加载
     */
    public boolean isLoading(String chunkKey) {
        return isInState(chunkKey, ChunkState.LOADING);
    }
    
    /**
     * 检查区块是否正在卸载
     */
    public boolean isUnloading(String chunkKey) {
        return isInState(chunkKey, ChunkState.UNLOADING);
    }
    
    /**
     * 获取指定状态的所有区块
     */
    public List<String> getChunksInState(ChunkState state) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, ChunkState> entry : chunkStates.entrySet()) {
            if (entry.getValue() == state) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * 获取状态变更时间
     */
    public long getStateChangeTime(String chunkKey) {
        return stateChangeTimes.getOrDefault(chunkKey, 0L);
    }
    
    /**
     * 清理指定区块的状态
     */
    public void clearChunkState(String chunkKey) {
        chunkStates.remove(chunkKey);
        stateChangeTimes.remove(chunkKey);
    }
    
    /**
     * 清理所有状态
     */
    public void clearAllStates() {
        chunkStates.clear();
        stateChangeTimes.clear();
    }
    
    /**
     * 获取状态统计信息
     */
    public Map<ChunkState, Integer> getStateStatistics() {
        Map<ChunkState, Integer> stats = new HashMap<>();
        for (ChunkState state : ChunkState.values()) {
            stats.put(state, 0);
        }
        
        for (ChunkState state : chunkStates.values()) {
            stats.put(state, stats.get(state) + 1);
        }
        
        return stats;
    }
    
    /**
     * 打印状态统计
     */
    public void printStateStatistics() {
        Map<ChunkState, Integer> stats = getStateStatistics();
        System.out.println("📊 区块状态统计:");
        for (Map.Entry<ChunkState, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.println("   " + entry.getKey() + ": " + entry.getValue() + " 个");
            }
        }
    }
    
    /**
     * 获取所有已跟踪的区块
     */
    public List<String> getAllTrackedChunks() {
        return new ArrayList<>(chunkStates.keySet());
    }
}
