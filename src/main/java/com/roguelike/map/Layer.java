package com.roguelike.map;

import java.util.List;

public class Layer {
    private String name;      // 图层名称
    private int width;        // 图层宽度（格数）
    private int height;       // 图层高度（格数）
    private List<Integer> data; // 瓦片ID数组（按行存储）

    // getter和setter
    public String getName() { return name; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Integer> getData() { return data; }
}
