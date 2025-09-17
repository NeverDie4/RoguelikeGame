package com.roguelike.map;

import com.almasb.fxgl.app.scene.GameView;

/**
 * Tile 地图提供者抽象。
 * 负责：
 * - 创建/销毁地图视图（按区块粒度）
 * - 提供通行性查询（世界坐标）
 * - 提供尺寸信息（像素与瓦片尺寸）
 * - 扫描特殊瓦片（可选）
 */
public interface TileMapProvider {

    /** 添加到场景（需在 FX Application Thread 调用） */
    void addToScene();

    /** 从场景移除（需在 FX Application Thread 调用） */
    void removeFromScene();

    /**
     * 世界坐标通行性查询。
     * @param worldX 像素 X
     * @param worldY 像素 Y
     * @return 是否可通行
     */
    boolean isPassable(double worldX, double worldY);

    /** 区块像素宽 */
    int getChunkWidthPixels();

    /** 区块像素高 */
    int getChunkHeightPixels();

    /** 单瓦片像素尺寸（正方形） */
    int getTileSize();

    /** 可选：返回底层视图（便于调试或层级控制），允许返回 null */
    GameView getGameView();

    /**
     * 扫描特殊瓦片（传送/定时器等）。默认空实现。
     * 实现方应调用 TimerTileManager/TeleportManager 等上层管理器接口完成注册。
     */
    default void scanSpecialTiles() { }

    /** 可选：设置某瓦片通行性（用于定时器瓦片等动态变更） */
    default void setTilePassable(int tileX, int tileY, boolean passable) { }
}


