package com.roguelike.map;

import java.util.List;
import java.util.ArrayList;

public class Layer {
    private String name;      // 图层名称
    private int width;        // 图层宽度（格数）
    private int height;       // 图层高度（格数）
    private List<Integer> data; // 瓦片ID数组（按行存储）

    public Layer() {
        this.data = new ArrayList<>();
    }

    // getter和setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    
    public List<Integer> getData() { return data; }
    public void setData(List<Integer> data) { this.data = data; }
}
