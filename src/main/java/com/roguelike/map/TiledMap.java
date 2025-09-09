package com.roguelike.map;

import java.util.List;

public class TiledMap {
    private int tilewidth;    // 瓦片宽度
    private int tileheight;   // 瓦片高度
    private List<Layer> layers; // 图层列表
    private List<Tileset> tilesets; // 瓦片集列表

    // getter和setter
    public int getTilewidth() { return tilewidth; }
    public int getTileheight() { return tileheight; }
    public List<Layer> getLayers() { return layers; }
    public List<Tileset> getTilesets() { return tilesets; }
}
