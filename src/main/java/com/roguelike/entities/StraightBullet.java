package com.roguelike.entities;

import com.roguelike.entities.components.BulletAnimationComponent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;

/**
 * 直线子弹，支持多种形状和大小
 */
public class StraightBullet extends Bullet {

    /**
     * 子弹形状枚举
     */
    public enum BulletShape {
        RECTANGLE,  // 矩形
        CIRCLE      // 圆形
    }

    private BulletShape shape;
    private double width;
    private double height;
    private boolean useAnimation = false;
    private BulletAnimationComponent animationComponent;

    /**
     * 默认构造函数 - 矩形子弹（默认使用动画）
     */
    public StraightBullet(Faction faction, Point2D direction,
                          int damage, boolean piercing, double speed) {
        this(faction, direction, damage, piercing, speed, BulletShape.RECTANGLE, 8, 4);
        // 默认启用动画
        enableAnimation();
    }

    /**
     * 完整构造函数
     */
    public StraightBullet(Faction faction, Point2D direction,
                          int damage, boolean piercing, double speed,
                          BulletShape shape, double width, double height) {
        super(faction, damage, piercing, speed);
        
        this.shape = shape;
        this.width = width;
        this.height = height;

        // 根据形状创建视图
        createView();
        setSize(width, height);

        initMovement(direction);
        setLifeTime(3);
    }

    /**
     * 快速创建玩家子弹的静态方法（动画版本）
     */
    public static StraightBullet createPlayerBullet(Point2D direction) {
        return new StraightBullet(Faction.PLAYER, direction, 10, false, 500.0);
    }

    /**
     * 快速创建敌人子弹的静态方法（动画版本）
     */
    public static StraightBullet createEnemyBullet(Point2D direction) {
        return new StraightBullet(Faction.ENEMY, direction, 5, false, 300.0);
    }

    /**
     * 创建穿透子弹
     */
    public static StraightBullet createPiercingBullet(Faction faction, Point2D direction) {
        return new StraightBullet(faction, direction, 15, true, 400.0);
    }

    /**
     * 创建圆形子弹
     */
    public static StraightBullet createCircularBullet(Faction faction, Point2D direction) {
        return new StraightBullet(faction, direction, 8, false, 350.0, 
                                 BulletShape.CIRCLE, 6, 6);
    }


    /**
     * 创建视图
     */
    private void createView() {
        if (useAnimation) {
            // 动画模式：不创建静态视图，由动画组件处理
            return;
        }
        
        Color color = getFactionColor();
        
        if (shape == BulletShape.CIRCLE) {
            Circle view = new Circle(width / 2);
            initView(view, color);
        } else {
            Rectangle view = new Rectangle(width, height);
            initView(view, color);
        }
    }

    /**
     * 根据阵营获取颜色
     */
    private Color getFactionColor() {
        switch (faction) {
            case PLAYER:
                return Color.ORANGE;
            case ENEMY:
                return Color.RED;
            default:
                return Color.WHITE;
        }
    }

    /**
     * 设置子弹形状
     */
    public void setShape(BulletShape shape) {
        this.shape = shape;
        getViewComponent().clearChildren();
        createView();
    }

    /**
     * 设置子弹大小
     */
    public void setBulletSize(double width, double height) {
        this.width = width;
        this.height = height;
        setSize(width, height);
        getViewComponent().clearChildren();
        createView();
    }

    // Getter方法
    public BulletShape getShape() {
        return shape;
    }

    public double getBulletWidth() {
        return width;
    }

    public double getBulletHeight() {
        return height;
    }

    /**
     * 启用动画
     */
    public void enableAnimation() {
        if (!useAnimation) {
            useAnimation = true;
            animationComponent = new BulletAnimationComponent();
            addComponent(animationComponent);
            
            // 加载动画帧
            String basePath = "assets/textures/bullets";
            animationComponent.loadAnimationFrames(basePath, 10);
            
            // 设置动画参数
            animationComponent.setFrameDuration(0.05); // 每帧50毫秒
            animationComponent.setLooping(true);
            
            // 重新设置大小以适应动画
            setSize(32, 32); // 动画图片的尺寸
        }
    }

    /**
     * 禁用动画
     */
    public void disableAnimation() {
        if (useAnimation) {
            useAnimation = false;
            if (animationComponent != null) {
                removeComponent(BulletAnimationComponent.class);
                animationComponent = null;
            }
            
            // 重新创建静态视图
            getViewComponent().clearChildren();
            createView();
        }
    }

    /**
     * 设置动画帧持续时间
     */
    public void setAnimationFrameDuration(double duration) {
        if (animationComponent != null) {
            animationComponent.setFrameDuration(duration);
        }
    }

    /**
     * 设置动画是否循环
     */
    public void setAnimationLooping(boolean looping) {
        if (animationComponent != null) {
            animationComponent.setLooping(looping);
        }
    }

    /**
     * 检查是否使用动画
     */
    public boolean isUsingAnimation() {
        return useAnimation;
    }

    /**
     * 获取动画组件
     */
    public BulletAnimationComponent getAnimationComponent() {
        return animationComponent;
    }
}