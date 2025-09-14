package com.roguelike.entities.effects;

import javafx.scene.paint.Color;
import java.util.List;

/**
 * 粒子效果配置类
 * 定义粒子效果的各种参数
 */
public class ParticleEffectConfig {
    
    private String type;                    // 效果类型
    private int particleCount;              // 粒子数量
    private double duration;                // 持续时间（秒）
    private List<String> colors;            // 颜色列表（支持渐变）
    private double size;                    // 粒子大小
    private double speed;                   // 粒子初始速度
    private double gravity;                 // 重力影响
    private double spread;                  // 扩散角度（弧度）
    private boolean fadeOut;                // 是否淡出
    private double fadeOutDuration;         // 淡出持续时间
    
    // 构造函数
    public ParticleEffectConfig() {}
    
    public ParticleEffectConfig(String type, int particleCount, double duration, 
                              List<String> colors, double size, double speed) {
        this.type = type;
        this.particleCount = particleCount;
        this.duration = duration;
        this.colors = colors;
        this.size = size;
        this.speed = speed;
        this.gravity = 0.0;
        this.spread = Math.PI; // 默认360度扩散
        this.fadeOut = true;
        this.fadeOutDuration = duration * 0.7; // 默认在70%时间开始淡出
    }
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getParticleCount() { return particleCount; }
    public void setParticleCount(int particleCount) { this.particleCount = particleCount; }
    
    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }
    
    public List<String> getColors() { return colors; }
    public void setColors(List<String> colors) { this.colors = colors; }
    
    public double getSize() { return size; }
    public void setSize(double size) { this.size = size; }
    
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    
    public double getGravity() { return gravity; }
    public void setGravity(double gravity) { this.gravity = gravity; }
    
    public double getSpread() { return spread; }
    public void setSpread(double spread) { this.spread = spread; }
    
    public boolean isFadeOut() { return fadeOut; }
    public void setFadeOut(boolean fadeOut) { this.fadeOut = fadeOut; }
    
    public double getFadeOutDuration() { return fadeOutDuration; }
    public void setFadeOutDuration(double fadeOutDuration) { this.fadeOutDuration = fadeOutDuration; }
    
    /**
     * 将颜色字符串转换为Color对象
     */
    public Color getColorAt(int index) {
        if (colors == null || colors.isEmpty()) {
            return Color.WHITE; // 默认白色
        }
        
        int colorIndex = index % colors.size();
        String colorString = colors.get(colorIndex);
        
        try {
            return Color.web(colorString);
        } catch (Exception e) {
            System.err.println("❌ 无效的颜色值: " + colorString);
            return Color.WHITE;
        }
    }
    
    /**
     * 获取随机颜色
     */
    public Color getRandomColor() {
        if (colors == null || colors.isEmpty()) {
            return Color.WHITE;
        }
        
        int randomIndex = (int) (Math.random() * colors.size());
        return getColorAt(randomIndex);
    }
    
    @Override
    public String toString() {
        return "ParticleEffectConfig{" +
                "type='" + type + '\'' +
                ", particleCount=" + particleCount +
                ", duration=" + duration +
                ", colors=" + colors +
                ", size=" + size +
                ", speed=" + speed +
                ", gravity=" + gravity +
                ", spread=" + spread +
                ", fadeOut=" + fadeOut +
                ", fadeOutDuration=" + fadeOutDuration +
                '}';
    }
}
