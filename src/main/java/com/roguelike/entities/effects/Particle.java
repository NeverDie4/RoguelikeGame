package com.roguelike.entities.effects;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * 单个粒子实体
 * 表示粒子效果中的一个粒子
 */
public class Particle extends Component {
    
    private Point2D velocity;           // 速度向量
    private double life;                // 生命值 (0.0 - 1.0)
    private double maxLife;             // 最大生命值
    private Color color;                // 粒子颜色
    private double size;                // 粒子大小
    private double gravity;             // 重力影响
    private boolean fadeOut;            // 是否淡出
    private double fadeOutStart;        // 淡出开始时间点
    
    private Circle visual;              // 视觉表示
    
    public Particle() {
        this.life = 1.0;
        this.maxLife = 1.0;
        this.color = Color.WHITE;
        this.size = 5.0;
        this.gravity = 0.0;
        this.fadeOut = true;
        this.fadeOutStart = 0.3; // 默认在30%生命时开始淡出
    }
    
    public Particle(Point2D velocity, double life, Color color, double size) {
        this();
        this.velocity = velocity;
        this.life = life;
        this.maxLife = life;
        this.color = color;
        this.size = size;
    }
    
    @Override
    public void onAdded() {
        // 创建视觉表示
        visual = new Circle(size, color);
        getEntity().getViewComponent().addChild(visual);
    }
    
    @Override
    public void onUpdate(double tpf) {
        // 更新位置
        if (velocity != null) {
            Entity entity = getEntity();
            entity.translate(velocity.getX() * tpf, velocity.getY() * tpf);
        }
        
        // 应用重力
        if (gravity != 0.0) {
            velocity = new Point2D(velocity.getX(), velocity.getY() + gravity * tpf);
        }
        
        // 更新生命值
        life -= tpf / maxLife;
        
        // 更新视觉效果
        updateVisual();
        
        // 检查是否应该移除
        if (life <= 0.0) {
            getEntity().removeFromWorld();
        }
    }
    
    /**
     * 更新视觉效果
     */
    private void updateVisual() {
        if (visual == null) return;
        
        // 更新透明度
        double opacity = 1.0;
        if (fadeOut && life <= fadeOutStart) {
            // 淡出效果
            opacity = Math.max(0.0, life / fadeOutStart);
        }
        
        // 确保透明度在有效范围内
        opacity = Math.max(0.0, Math.min(1.0, opacity));
        
        // 更新颜色透明度
        Color newColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
        visual.setFill(newColor);
        
        // 可选：更新大小（粒子逐渐变小）
        double currentSize = size * (0.5 + 0.5 * life); // 从原始大小到一半大小
        visual.setRadius(currentSize);
    }
    
    // Getters and Setters
    public Point2D getVelocity() { return velocity; }
    public void setVelocity(Point2D velocity) { this.velocity = velocity; }
    
    public double getLife() { return life; }
    public void setLife(double life) { this.life = life; }
    
    public double getMaxLife() { return maxLife; }
    public void setMaxLife(double maxLife) { this.maxLife = maxLife; }
    
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    
    public double getSize() { return size; }
    public void setSize(double size) { this.size = size; }
    
    public double getGravity() { return gravity; }
    public void setGravity(double gravity) { this.gravity = gravity; }
    
    public boolean isFadeOut() { return fadeOut; }
    public void setFadeOut(boolean fadeOut) { this.fadeOut = fadeOut; }
    
    public double getFadeOutStart() { return fadeOutStart; }
    public void setFadeOutStart(double fadeOutStart) { this.fadeOutStart = fadeOutStart; }
    
    /**
     * 检查粒子是否还活着
     */
    public boolean isAlive() {
        return life > 0.0;
    }
}
