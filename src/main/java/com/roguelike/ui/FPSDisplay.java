package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * FPS显示组件 - 用于调试显示当前帧数
 */
public class FPSDisplay {
    private Label fpsLabel;
    private StackPane fpsContainer;
    private AnimationTimer fpsUpdater;
    private boolean isVisible = false;
    
    // FPS计算相关
    private long lastTime = 0;
    private int frameCount = 0;
    private double currentFPS = 0.0;
    private long lastFPSUpdate = 0;
    private static final long FPS_UPDATE_INTERVAL = 100_000_000; // 100ms更新一次FPS显示
    
    // 窗口尺寸相关
    private double currentScreenWidth;
    private double currentScreenHeight;
    
    public FPSDisplay() {
        initFPSDisplay();
    }
    
    /**
     * 初始化FPS显示组件
     */
    private void initFPSDisplay() {
        // 获取初始窗口尺寸
        updateScreenDimensions();
        
        // 创建FPS标签
        fpsLabel = new Label("FPS: --");
        fpsLabel.setTextFill(Color.LIME);
        fpsLabel.setFont(Font.font("Consolas", 14));
        
        // 添加阴影效果
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(2);
        shadow.setOffsetX(1);
        shadow.setOffsetY(1);
        fpsLabel.setEffect(shadow);
        
        // 设置背景样式
        fpsLabel.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.7); " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 4 8 4 8; " +
            "-fx-border-color: rgba(0, 255, 0, 0.5); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4;"
        );
        
        // 创建容器
        fpsContainer = new StackPane();
        fpsContainer.getChildren().add(fpsLabel);
        fpsContainer.setPickOnBounds(false);
        
        // 设置位置（右上角）
        StackPane.setAlignment(fpsContainer, Pos.TOP_RIGHT);
        fpsContainer.setTranslateX(-10); // 距离右边10像素
        fpsContainer.setTranslateY(10);  // 距离顶部10像素
        
        // 初始状态为隐藏
        fpsContainer.setVisible(false);
        
        // 将容器添加到场景中，但保持隐藏状态
        FXGL.getGameScene().addUINode(fpsContainer);
        
        // 初始化FPS更新器
        initFPSUpdater();
    }
    
    /**
     * 初始化FPS更新器
     */
    private void initFPSUpdater() {
        fpsUpdater = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isVisible) {
                    return;
                }
                
                // 计算FPS
                if (lastTime == 0) {
                    lastTime = now;
                    lastFPSUpdate = now;
                    return;
                }
                
                frameCount++;
                
                // 每100ms更新一次FPS显示
                if (now - lastFPSUpdate >= FPS_UPDATE_INTERVAL) {
                    double elapsed = (now - lastFPSUpdate) / 1_000_000_000.0; // 转换为秒
                    currentFPS = frameCount / elapsed;
                    
                    // 更新显示
                    updateFPSDisplay();
                    
                    // 重置计数器
                    frameCount = 0;
                    lastFPSUpdate = now;
                }
            }
        };
    }
    
    /**
     * 更新FPS显示
     */
    private void updateFPSDisplay() {
        if (fpsLabel != null) {
            // 根据FPS值设置不同颜色
            if (currentFPS >= 55) {
                fpsLabel.setTextFill(Color.LIME);      // 绿色 - 良好
            } else if (currentFPS >= 30) {
                fpsLabel.setTextFill(Color.YELLOW);    // 黄色 - 一般
            } else {
                fpsLabel.setTextFill(Color.RED);       // 红色 - 较差
            }
            
            // 更新文本
            fpsLabel.setText(String.format("FPS: %.1f", currentFPS));
        }
    }
    
    /**
     * 显示FPS
     */
    public void show() {
        if (!isVisible) {
            isVisible = true;
            fpsContainer.setVisible(true);
            fpsUpdater.start();
            System.out.println("📊 FPS显示已开启");
        }
    }
    
    /**
     * 隐藏FPS
     */
    public void hide() {
        if (isVisible) {
            isVisible = false;
            fpsContainer.setVisible(false);
            if (fpsUpdater != null) {
                fpsUpdater.stop();
            }
            System.out.println("📊 FPS显示已关闭");
        }
    }
    
    /**
     * 切换FPS显示状态
     */
    public void toggle() {
        if (isVisible) {
            hide();
        } else {
            show();
        }
    }
    
    /**
     * 检查是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * 获取当前FPS值
     */
    public double getCurrentFPS() {
        return currentFPS;
    }
    
    /**
     * 更新屏幕尺寸
     */
    private void updateScreenDimensions() {
        currentScreenWidth = FXGL.getAppWidth();
        currentScreenHeight = FXGL.getAppHeight();
    }
    
    /**
     * 设置窗口尺寸变化监听器
     */
    public void setupWindowResizeListener() {
        AnimationTimer resizeChecker = new AnimationTimer() {
            private double lastWidth = currentScreenWidth;
            private double lastHeight = currentScreenHeight;

            @Override
            public void handle(long now) {
                double currentWidth = FXGL.getAppWidth();
                double currentHeight = FXGL.getAppHeight();

                // 检查尺寸是否发生变化
                if (Math.abs(currentWidth - lastWidth) > 1 || Math.abs(currentHeight - lastHeight) > 1) {
                    updateScreenDimensions();
                    lastWidth = currentWidth;
                    lastHeight = currentHeight;
                }
            }
        };
        resizeChecker.start();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (fpsUpdater != null) {
            fpsUpdater.stop();
        }
        if (fpsContainer != null && FXGL.getGameScene() != null) {
            FXGL.getGameScene().removeUINode(fpsContainer);
        }
    }
}
