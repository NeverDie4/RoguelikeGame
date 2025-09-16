package com.roguelike.ui;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.layout.StackPane;

/**
 * 键盘导航箭头组件
 * 用于显示键盘选中状态的指示箭头
 */
public class KeyboardArrow extends StackPane {
    
    public enum ArrowDirection {
        LEFT,   // 左箭头（指向右）
        RIGHT   // 右箭头（指向左）
    }
    
    private final Polygon arrowShape;
    private final ArrowDirection direction;
    
    public KeyboardArrow(ArrowDirection direction) {
        this.direction = direction;
        this.arrowShape = new Polygon();
        
        // 设置箭头颜色 - 不过于鲜艳的金色
        arrowShape.setFill(Color.rgb(218, 165, 32, 0.9)); // 暗金色，带透明度
        arrowShape.setStroke(Color.rgb(184, 134, 11, 0.8)); // 边框颜色稍深
        arrowShape.setStrokeWidth(1.0);
        
        // 根据方向创建三角形
        createArrowShape();
        
        // 设置默认不可见
        setVisible(false);
        
        getChildren().add(arrowShape);
    }
    
    private void createArrowShape() {
        double[] points;
        
        if (direction == ArrowDirection.LEFT) {
            // 左箭头（指向右）：三角形顶点在右侧
            points = new double[]{
                0, 0,      // 左上角
                0, 20,     // 左下角  
                15, 10     // 右侧顶点
            };
        } else {
            // 右箭头（指向左）：三角形顶点在左侧
            points = new double[]{
                15, 0,     // 右上角
                15, 20,    // 右下角
                0, 10      // 左侧顶点
            };
        }
        
        arrowShape.getPoints().addAll(Double.valueOf(points[0]), Double.valueOf(points[1]), Double.valueOf(points[2]), Double.valueOf(points[3]), Double.valueOf(points[4]), Double.valueOf(points[5]));
    }
    
    /**
     * 显示箭头
     */
    public void show() {
        setVisible(true);
    }
    
    /**
     * 隐藏箭头
     */
    public void hide() {
        setVisible(false);
    }
    
    /**
     * 设置箭头大小
     * @param size 箭头大小（影响三角形的高度和宽度）
     */
    public void setSize(double size) {
        double[] points;
        double height = size;
        double width = size * 0.75; // 宽度是高度的75%
        
        if (direction == ArrowDirection.LEFT) {
            // 左箭头（指向右）
            points = new double[]{
                0, 0,                    // 左上角
                0, height,               // 左下角  
                width, height / 2        // 右侧顶点
            };
        } else {
            // 右箭头（指向左）
            points = new double[]{
                width, 0,                // 右上角
                width, height,           // 右下角
                0, height / 2            // 左侧顶点
            };
        }
        
        arrowShape.getPoints().clear();
        arrowShape.getPoints().addAll(Double.valueOf(points[0]), Double.valueOf(points[1]), Double.valueOf(points[2]), Double.valueOf(points[3]), Double.valueOf(points[4]), Double.valueOf(points[5]));
    }
    
    /**
     * 设置箭头颜色
     * @param color 箭头颜色
     */
    public void setArrowColor(Color color) {
        arrowShape.setFill(color);
    }
    
    /**
     * 获取箭头方向
     * @return 箭头方向
     */
    public ArrowDirection getDirection() {
        return direction;
    }
}
