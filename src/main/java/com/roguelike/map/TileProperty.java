package com.roguelike.map;

import java.util.HashMap;
import java.util.Map;

/**
 * 瓦片属性类，用于存储TMX文件中定义的瓦片属性
 */
public class TileProperty {
    
    private Map<String, Object> properties;
    
    public TileProperty() {
        this.properties = new HashMap<>();
    }
    
    /**
     * 添加属性
     */
    public void addProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    /**
     * 获取属性值
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    /**
     * 获取布尔属性
     */
    public boolean getBooleanProperty(String name, boolean defaultValue) {
        Object value = properties.get(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    /**
     * 获取整数属性
     */
    public int getIntProperty(String name, int defaultValue) {
        Object value = properties.get(name);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取字符串属性
     */
    public String getStringProperty(String name, String defaultValue) {
        Object value = properties.get(name);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
    
    /**
     * 检查是否包含指定属性
     */
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }
    
    /**
     * 获取所有属性
     */
    public Map<String, Object> getAllProperties() {
        return new HashMap<>(properties);
    }
    
    /**
     * 检查瓦片是否不可通行
     */
    public boolean isUnaccessible() {
        return getBooleanProperty("unaccessible", false);
    }
    
    /**
     * 检查瓦片是否可通行
     */
    public boolean isPassable() {
        return !isUnaccessible();
    }
    
    /**
     * 检查瓦片是否是传送门
     */
    public boolean isTeleport() {
        return getBooleanProperty("interteleport", false);
    }
    
    /**
     * 获取传送门的目标地图ID
     */
    public String getTeleportMapId() {
        return getStringProperty("map_id", "");
    }
    
    /**
     * 获取传送门的目标X坐标（瓦片坐标）
     */
    public int getTeleportX() {
        return getIntProperty("x", 0);
    }
    
    /**
     * 获取传送门的目标Y坐标（瓦片坐标）
     */
    public int getTeleportY() {
        return getIntProperty("y", 0);
    }
    
    /**
     * 检查瓦片是否有定时器属性
     */
    public boolean hasTimer() {
        return hasProperty("timer");
    }
    
    /**
     * 获取瓦片的定时器值（秒）
     */
    public int getTimer() {
        return getIntProperty("timer", 0);
    }
    
    /**
     * 检查瓦片是否是定时器瓦片（有timer属性且unaccessible为true）
     */
    public boolean isTimerTile() {
        return hasTimer() && isUnaccessible();
    }
    
    @Override
    public String toString() {
        return "TileProperty{" + properties + "}";
    }
}
