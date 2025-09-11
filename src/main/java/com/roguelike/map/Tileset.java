package com.roguelike.map;

import java.util.HashMap;
import java.util.Map;

public class Tileset {
    private int firstgid;     // 第一个全局ID
    private String name;      // 瓦片集名称
    private int tilewidth;    // 瓦片宽度
    private int tileheight;   // 瓦片高度
    private int tilecount;    // 瓦片总数
    private int columns;      // 列数
    private String image;     // 瓦片集图片路径
    private int imagewidth;   // 图片宽度
    private int imageheight;  // 图片高度
    
    // 瓦片属性映射：localId -> TileProperty
    private Map<Integer, TileProperty> tileProperties;

    // getter和setter
    public int getFirstgid() { return firstgid; }
    public void setFirstgid(int firstgid) { this.firstgid = firstgid; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getTilewidth() { return tilewidth; }
    public void setTilewidth(int tilewidth) { this.tilewidth = tilewidth; }
    
    public int getTileheight() { return tileheight; }
    public void setTileheight(int tileheight) { this.tileheight = tileheight; }
    
    public int getTilecount() { return tilecount; }
    public void setTilecount(int tilecount) { this.tilecount = tilecount; }
    
    public int getColumns() { return columns; }
    public void setColumns(int columns) { this.columns = columns; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public int getImagewidth() { return imagewidth; }
    public void setImagewidth(int imagewidth) { this.imagewidth = imagewidth; }
    
    public int getImageheight() { return imageheight; }
    public void setImageheight(int imageheight) { this.imageheight = imageheight; }
    
    // 瓦片属性相关方法
    public Map<Integer, TileProperty> getTileProperties() { 
        if (tileProperties == null) {
            tileProperties = new HashMap<>();
        }
        return tileProperties; 
    }
    
    public void setTileProperties(Map<Integer, TileProperty> tileProperties) { 
        this.tileProperties = tileProperties; 
    }
    
    /**
     * 添加瓦片属性
     */
    public void addTileProperty(int localId, TileProperty property) {
        if (tileProperties == null) {
            tileProperties = new HashMap<>();
        }
        tileProperties.put(localId, property);
    }
    
    /**
     * 获取瓦片属性
     */
    public TileProperty getTileProperty(int localId) {
        if (tileProperties == null) {
            return null;
        }
        return tileProperties.get(localId);
    }
    
    /**
     * 检查瓦片是否不可通行
     */
    public boolean isTileUnaccessible(int localId) {
        TileProperty property = getTileProperty(localId);
        return property != null && property.isUnaccessible();
    }
    
    /**
     * 检查瓦片是否可通行
     */
    public boolean isTilePassable(int localId) {
        return !isTileUnaccessible(localId);
    }
}
