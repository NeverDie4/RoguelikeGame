package com.roguelike.entities;

import com.almasb.fxgl.core.math.Vec2;
import com.almasb.fxgl.entity.component.Component;
import com.roguelike.entities.components.BulletAnimationComponent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/**
 * 弧形子弹，支持多种曲线模式
 */
public class CurveBullet extends Bullet {

    /**
     * 曲线类型枚举
     */
    public enum CurveType {
        SINE,       // 正弦波
        COSINE,     // 余弦波
        SPIRAL,     // 螺旋
        ZIGZAG      // 锯齿波
    }

    private double curveFactor;
    private double curveFrequency;
    private CurveType curveType;
    private double radius;
    private boolean useAnimation = false;
    private BulletAnimationComponent animationComponent;

    /**
     * 默认构造函数 - 正弦波曲线
     */
    public CurveBullet(Faction faction, Point2D initialDirection,
                       int damage, boolean piercing, double speed, double curveFactor) {
        this(faction, initialDirection, damage, piercing, speed, curveFactor, 
             CurveType.SINE, 2.0, 4.0);
    }

    /**
     * 完整构造函数
     */
    public CurveBullet(Faction faction, Point2D initialDirection,
                       int damage, boolean piercing, double speed, double curveFactor,
                       CurveType curveType, double curveFrequency, double radius) {
        super(faction, damage, piercing, speed);
        
        this.curveFactor = curveFactor;
        this.curveType = curveType;
        this.curveFrequency = curveFrequency;
        this.radius = radius;

        // 创建视图
        createView();
        setSize(radius * 2, radius * 2);

        // 添加自定义运动组件
        addComponent(new CurveMovementComponent(initialDirection, speed, curveFactor, 
                                              curveType, curveFrequency));
        setLifeTime(5);
    }

    /**
     * 快速创建玩家弧形子弹
     */
    public static CurveBullet createPlayerCurveBullet(Point2D direction) {
        return new CurveBullet(Faction.PLAYER, direction, 8, false, 400.0, 30.0);
    }

    

    /**
     * 创建螺旋子弹
     */
    public static CurveBullet createSpiralBullet(Faction faction, Point2D direction) {
        return new CurveBullet(faction, direction, 10, false, 350.0, 40.0, 
                              CurveType.SPIRAL, 3.0, 5.0);
    }

    /**
     * 创建锯齿波子弹
     */
    public static CurveBullet createZigzagBullet(Faction faction, Point2D direction) {
        return new CurveBullet(faction, direction, 7, false, 450.0, 35.0, 
                              CurveType.ZIGZAG, 4.0, 4.0);
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
        
        if (curveType == CurveType.SPIRAL) {
            // 螺旋子弹使用三角形
            Polygon view = new Polygon();
            view.getPoints().addAll(
                0.0, -radius,
                -radius * 0.5, radius * 0.5,
                radius * 0.5, radius * 0.5
            );
            initView(view, color);
        } else {
            // 其他类型使用圆形
            Circle view = new Circle(radius);
            initView(view, color);
        }
    }

    /**
     * 根据阵营获取颜色
     */
    private Color getFactionColor() {
        switch (faction) {
            case PLAYER:
                return Color.CYAN;
            case ENEMY:
                return Color.PURPLE;
            default:
                return Color.WHITE;
        }
    }

    /**
     * 弧形运动组件，支持多种曲线模式
     */
    private static class CurveMovementComponent extends Component {
        private final Vec2 baseVelocity;
        private final double curveFactor;
        private final CurveType curveType;
        private final double curveFrequency;
        private double time = 0;

        public CurveMovementComponent(Point2D initialDirection, double speed, double curveFactor,
                                    CurveType curveType, double curveFrequency) {
            this.baseVelocity = new Vec2(initialDirection).normalizeLocal().mulLocal((float)speed);
            this.curveFactor = curveFactor;
            this.curveType = curveType;
            this.curveFrequency = curveFrequency;
        }

        @Override
        public void onUpdate(double tpf) {
            time += tpf;
            
            double offsetX = 0;
            double offsetY = 0;
            
            switch (curveType) {
                case SINE:
                    offsetX = Math.sin(time * curveFrequency) * curveFactor;
                    break;
                case COSINE:
                    offsetY = Math.cos(time * curveFrequency) * curveFactor;
                    break;
                case SPIRAL:
                    double spiralRadius = time * 10;
                    offsetX = Math.cos(time * curveFrequency) * spiralRadius;
                    offsetY = Math.sin(time * curveFrequency) * spiralRadius;
                    break;
                case ZIGZAG:
                    offsetX = Math.sin(time * curveFrequency * 2) * curveFactor;
                    offsetY = Math.sin(time * curveFrequency) * curveFactor * 0.5;
                    break;
            }
            
            entity.translate(baseVelocity.x * tpf + offsetX * tpf, 
                           baseVelocity.y * tpf + offsetY * tpf);
        }
    }

    // Getter和Setter方法
    public double getCurveFactor() {
        return curveFactor;
    }

    public void setCurveFactor(double curveFactor) {
        this.curveFactor = curveFactor;
    }

    public CurveType getCurveType() {
        return curveType;
    }

    public void setCurveType(CurveType curveType) {
        this.curveType = curveType;
        getViewComponent().clearChildren();
        createView();
    }

    public double getCurveFrequency() {
        return curveFrequency;
    }

    public void setCurveFrequency(double curveFrequency) {
        this.curveFrequency = curveFrequency;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
        setSize(radius * 2, radius * 2);
        getViewComponent().clearChildren();
        createView();
    }

    /**
     * 启用动画
     */
    public void enableAnimation() {
        if (!useAnimation) {
            useAnimation = true;
            animationComponent = new BulletAnimationComponent();
            addComponent(animationComponent);
            
            // 加载动画帧（使用 FXGL 相对路径，指向 assets/textures/bullets）
            String basePath = "bullets";
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