package com.roguelike.map;

public class Tileset {
    private String name;      // 瓦片集名称
    private int tilewidth;    // 瓦片宽度
    private int tileheight;   // 瓦片高度
    private String image;     // 瓦片集图片路径
    private int imagewidth;   // 图片宽度
    private int imageheight;  // 图片高度

    // getter和setter
    public String getName() { return name; }
    public int getTilewidth() { return tilewidth; }
    public int getTileheight() { return tileheight; }
    public String getImage() { return image; }
    public int getImagewidth() { return imagewidth; }
    public int getImageheight() { return imageheight; }
}
