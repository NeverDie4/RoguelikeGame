package com.roguelike.entities.configs;

/**
 * 子弹配置（模板）。用于数据驱动地定义默认数值与资源路径。
 */
public class BulletSpec {

    public enum BulletType {
        STRAIGHT,
        CURVE
    }

    private final String id;
    private final String displayName;
    private final BulletType bulletType;

    // 数值
    private final int baseDamage;
    private final boolean piercing;
    private final double baseSpeed;
    private final double lifetimeSeconds; // <= 0 表示不自动销毁

    // 尺寸/碰撞（可选，<=0 表示使用默认）
    private final double width;
    private final double height;
    private final double radius;

    // 动画
    private final String animationBasePath; // 如："bullets/small_orange"
    private final int frameCount;
    private final double frameDuration;
    // 视觉缩放（仅影响渲染，不改变碰撞盒）
    private final double visualScale;

    public BulletSpec(
            String id,
            String displayName,
            BulletType bulletType,
            int baseDamage,
            boolean piercing,
            double baseSpeed,
            double lifetimeSeconds,
            double width,
            double height,
            double radius,
            String animationBasePath,
            int frameCount,
            double frameDuration,
            double visualScale
    ) {
        this.id = id;
        this.displayName = displayName;
        this.bulletType = bulletType;
        this.baseDamage = Math.max(0, baseDamage);
        this.piercing = piercing;
        this.baseSpeed = Math.max(0, baseSpeed);
        this.lifetimeSeconds = lifetimeSeconds;
        this.width = width;
        this.height = height;
        this.radius = radius;
        this.animationBasePath = animationBasePath;
        this.frameCount = Math.max(0, frameCount);
        this.frameDuration = Math.max(0, frameDuration);
        this.visualScale = Math.max(0.1, visualScale);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BulletType getBulletType() {
        return bulletType;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public boolean isPiercing() {
        return piercing;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public double getLifetimeSeconds() {
        return lifetimeSeconds;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getRadius() {
        return radius;
    }

    public String getAnimationBasePath() {
        return animationBasePath;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public double getFrameDuration() {
        return frameDuration;
    }

    public double getVisualScale() {
        return visualScale;
    }
}


