package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.util.Duration;

/**
 * 可复用的箭头指示器，用于引导玩家找到特殊区块
 */
public class ArrowIndicator {
    
    private Group arrowGroup;
    private Polygon arrowHead;
    private Circle arrowTail;
    private boolean isVisible = false;
    private boolean isAnimating = false;
    
    // 箭头样式配置
    private static final double ARROW_SIZE = 50.0;
    private static final double ARROW_TAIL_RADIUS = 12.0;
    private static final Color ARROW_COLOR = Color.RED;
    private static final Color ARROW_GLOW_COLOR = Color.PINK;
    private static final double ARROW_OPACITY = 0.9;
    
    // 动画配置
    private static final Duration FADE_DURATION = Duration.millis(400);
    private static final Duration PULSE_DURATION = Duration.millis(1200);
    private static final Duration ROTATE_DURATION = Duration.millis(1800);
    
    public ArrowIndicator() {
        createArrow();
        setupAnimations();
    }
    
    /**
     * 创建箭头图形
     */
    private void createArrow() {
        arrowGroup = new Group();
        
        // 创建箭头头部（更现代的三角形）
        arrowHead = new Polygon();
        arrowHead.getPoints().addAll(
            0.0, -ARROW_SIZE / 2,           // 上顶点
            ARROW_SIZE * 0.8, -ARROW_SIZE * 0.15,  // 右上点
            ARROW_SIZE, 0.0,                // 右顶点
            ARROW_SIZE * 0.8, ARROW_SIZE * 0.15,   // 右下点
            0.0, ARROW_SIZE / 2             // 下顶点
        );
        arrowHead.setFill(ARROW_COLOR);
        arrowHead.setStroke(Color.WHITE);
        arrowHead.setStrokeWidth(3.0);
        
        // 创建箭头尾部（更现代的圆形）
        arrowTail = new Circle(ARROW_TAIL_RADIUS);
        arrowTail.setFill(ARROW_COLOR);
        arrowTail.setStroke(Color.WHITE);
        arrowTail.setStrokeWidth(3.0);
        arrowTail.setTranslateX(-ARROW_SIZE * 0.6);
        
        // 添加发光效果
        DropShadow glow = new DropShadow();
        glow.setColor(ARROW_GLOW_COLOR);
        glow.setRadius(15.0);
        glow.setSpread(0.5);
        arrowHead.setEffect(glow);
        arrowTail.setEffect(glow);
        
        // 设置透明度
        arrowGroup.setOpacity(ARROW_OPACITY);
        
        // 组合箭头元素
        arrowGroup.getChildren().addAll(arrowTail, arrowHead);
        
        // 初始状态隐藏
        arrowGroup.setVisible(false);
    }
    
    /**
     * 设置动画效果
     */
    private void setupAnimations() {
        // 脉冲动画（大小变化）
        Timeline pulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(arrowGroup.scaleXProperty(), 1.0)),
            new KeyFrame(PULSE_DURATION.divide(3), new KeyValue(arrowGroup.scaleXProperty(), 1.15)),
            new KeyFrame(PULSE_DURATION.divide(2), new KeyValue(arrowGroup.scaleXProperty(), 1.25)),
            new KeyFrame(PULSE_DURATION, new KeyValue(arrowGroup.scaleXProperty(), 1.0))
        );
        pulseTimeline.setCycleCount(Timeline.INDEFINITE);
        
        // 旋转动画（轻微摆动）
        RotateTransition rotateTransition = new RotateTransition(ROTATE_DURATION, arrowGroup);
        rotateTransition.setByAngle(10);
        rotateTransition.setAutoReverse(true);
        rotateTransition.setCycleCount(Timeline.INDEFINITE);
        
        // 启动动画
        pulseTimeline.play();
        rotateTransition.play();
    }
    
    /**
     * 显示箭头并指向指定位置
     * @param targetX 目标X坐标
     * @param targetY 目标Y坐标
     * @param playerX 玩家X坐标
     * @param playerY 玩家Y坐标
     */
    public void showArrow(double targetX, double targetY, double playerX, double playerY) {
        if (isVisible) {
            return; // 已经显示
        }
        
        // 计算箭头位置（屏幕边缘）
        Point2D arrowPosition = calculateArrowPosition(targetX, targetY, playerX, playerY);
        
        // 设置箭头位置
        arrowGroup.setTranslateX(arrowPosition.getX());
        arrowGroup.setTranslateY(arrowPosition.getY());
        
        // 计算箭头旋转角度
        double angle = calculateArrowAngle(targetX, targetY, playerX, playerY);
        arrowGroup.setRotate(angle);
        
        // 显示箭头
        arrowGroup.setVisible(true);
        isVisible = true;
        
        // 淡入动画
        FadeTransition fadeIn = new FadeTransition(FADE_DURATION, arrowGroup);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(ARROW_OPACITY);
        fadeIn.play();
        
        System.out.println("🎯 箭头指示器显示，指向位置: (" + targetX + ", " + targetY + ")");
    }
    
    /**
     * 隐藏箭头
     */
    public void hideArrow() {
        if (!isVisible) {
            return; // 已经隐藏
        }
        
        // 淡出动画
        FadeTransition fadeOut = new FadeTransition(FADE_DURATION, arrowGroup);
        fadeOut.setFromValue(ARROW_OPACITY);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            arrowGroup.setVisible(false);
            isVisible = false;
        });
        fadeOut.play();
        
        System.out.println("🎯 箭头指示器隐藏");
    }
    
    /**
     * 计算箭头在屏幕上的位置（屏幕边缘）
     */
    private Point2D calculateArrowPosition(double targetX, double targetY, double playerX, double playerY) {
        double screenWidth = FXGL.getAppWidth();
        double screenHeight = FXGL.getAppHeight();
        
        // 计算从玩家到目标的方向向量
        double dx = targetX - playerX;
        double dy = targetY - playerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance == 0) {
            return new Point2D(screenWidth / 2, screenHeight / 2);
        }
        
        // 归一化方向向量
        double normalizedDx = dx / distance;
        double normalizedDy = dy / distance;
        
        // 计算箭头在屏幕边缘的位置
        double arrowX, arrowY;
        
        // 根据方向确定箭头位置
        if (Math.abs(normalizedDx) > Math.abs(normalizedDy)) {
            // 水平方向为主
            if (normalizedDx > 0) {
                // 向右
                arrowX = screenWidth - 60;
                arrowY = screenHeight / 2 + normalizedDy * (screenHeight / 2 - 60);
            } else {
                // 向左
                arrowX = 60;
                arrowY = screenHeight / 2 + normalizedDy * (screenHeight / 2 - 60);
            }
        } else {
            // 垂直方向为主
            if (normalizedDy > 0) {
                // 向下
                arrowY = screenHeight - 60;
                arrowX = screenWidth / 2 + normalizedDx * (screenWidth / 2 - 60);
            } else {
                // 向上
                arrowY = 60;
                arrowX = screenWidth / 2 + normalizedDx * (screenWidth / 2 - 60);
            }
        }
        
        return new Point2D(arrowX, arrowY);
    }
    
    /**
     * 计算箭头旋转角度
     */
    private double calculateArrowAngle(double targetX, double targetY, double playerX, double playerY) {
        double dx = targetX - playerX;
        double dy = targetY - playerY;
        return Math.toDegrees(Math.atan2(dy, dx));
    }
    
    /**
     * 更新箭头位置和方向
     * @param targetX 目标X坐标
     * @param targetY 目标Y坐标
     * @param playerX 玩家X坐标
     * @param playerY 玩家Y坐标
     */
    public void updateArrow(double targetX, double targetY, double playerX, double playerY) {
        if (!isVisible) {
            return;
        }
        
        // 计算新的位置和角度
        Point2D newPosition = calculateArrowPosition(targetX, targetY, playerX, playerY);
        double newAngle = calculateArrowAngle(targetX, targetY, playerX, playerY);
        
        // 平滑更新位置和角度
        arrowGroup.setTranslateX(newPosition.getX());
        arrowGroup.setTranslateY(newPosition.getY());
        arrowGroup.setRotate(newAngle);
    }
    
    /**
     * 获取箭头UI节点
     */
    public Node getNode() {
        return arrowGroup;
    }
    
    /**
     * 检查箭头是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * 设置箭头颜色
     */
    public void setArrowColor(Color color) {
        arrowHead.setFill(color);
        arrowTail.setFill(color);
    }
    
    /**
     * 设置箭头大小
     */
    public void setArrowSize(double size) {
        double scale = size / ARROW_SIZE;
        arrowGroup.setScaleX(scale);
        arrowGroup.setScaleY(scale);
    }
}
