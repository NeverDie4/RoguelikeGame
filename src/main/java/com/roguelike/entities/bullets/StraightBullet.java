package com.roguelike.entities.bullets;

import com.roguelike.entities.Bullet;
import com.roguelike.entities.components.BulletAnimationComponent;
import com.roguelike.entities.components.OutOfViewportDestroyComponent;
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

        // 初始不创建静态视图，避免与动画纹理叠加
        setSize(width, height);

        initMovement(direction);
    }

    /**
     * 快速创建玩家子弹的静态方法（动画版本）
     */
    public static StraightBullet createPlayerBullet(Point2D direction) {
        StraightBullet b = new StraightBullet(Faction.PLAYER, direction, 10, false, 200.0);
        // 保持动画循环，使用 16x16 资源尺寸
        b.setSize(16, 16);
        // 直接使用静态工厂创建的情况，添加越界销毁
        b.addComponent(new OutOfViewportDestroyComponent(600));
        return b;
    }

    /**
     * 创建穿透子弹
     */
    public static StraightBullet createPiercingBullet(Faction faction, Point2D direction) {
        StraightBullet b = new StraightBullet(faction, direction, 15, true, 400.0);
        b.addComponent(new OutOfViewportDestroyComponent(600));
        return b;
    }

    /**
     * 创建圆形子弹
     */
    public static StraightBullet createCircularBullet(Faction faction, Point2D direction) {
        StraightBullet b = new StraightBullet(faction, direction, 8, false, 350.0, 
                                 BulletShape.CIRCLE, 6, 6);
        b.addComponent(new OutOfViewportDestroyComponent(600));
        return b;
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
            // 检查是否已有动画组件，避免重复添加
            animationComponent = getComponentOptional(BulletAnimationComponent.class).orElse(null);
            if (animationComponent == null) {
                animationComponent = new BulletAnimationComponent();
                addComponent(animationComponent);
            }
            // 不在此处加载默认路径的动画帧，交由外部（如 BulletFactory）根据配置加载
            // 预设通用参数
            animationComponent.setFrameDuration(0.05);
            animationComponent.setLooping(true);
            // 统一碰撞盒为 16x16（动画帧尺寸）
            setSize(16, 16);
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


