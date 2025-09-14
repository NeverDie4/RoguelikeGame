package com.roguelike.map;

/**
 * 区块状态枚举
 */
public enum ChunkState {
    UNLOADED,      // 未加载
    LOADING,       // 加载中
    LOADED,        // 已加载
    UNLOADING,     // 卸载中
    CACHED         // 缓存中（保留数据但移除渲染）
}
