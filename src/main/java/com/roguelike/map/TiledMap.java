package com.roguelike.map;

import java.util.List;
import java.util.ArrayList;

public class TiledMap {
    private int width;        // 地图宽度（格数）
    private int height;       // 地图高度（格数）
    private int tilewidth;    // 瓦片宽度
    private int tileheight;   // 瓦片高度
    private List<Layer> layers; // 图层列表
    private List<Tileset> tilesets; // 瓦片集列表

    public TiledMap() {
        this.layers = new ArrayList<>();
        this.tilesets = new ArrayList<>();
    }

    // getter和setter
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    
    public int getTilewidth() { return tilewidth; }
    public void setTilewidth(int tilewidth) { this.tilewidth = tilewidth; }
    
    public int getTileheight() { return tileheight; }
    public void setTileheight(int tileheight) { this.tileheight = tileheight; }
    
    public List<Layer> getLayers() { return layers; }
    public void setLayers(List<Layer> layers) { this.layers = layers; }
    
    public List<Tileset> getTilesets() { return tilesets; }
    public void setTilesets(List<Tileset> tilesets) { this.tilesets = tilesets; }
}
