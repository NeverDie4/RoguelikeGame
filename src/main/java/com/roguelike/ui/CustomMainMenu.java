package com.roguelike.ui;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * 自定义主菜单类
 */
public class CustomMainMenu extends FXGLMenu {

    public CustomMainMenu() {
        super(MenuType.MAIN_MENU);
        initCustomMainMenu();
    }

    private void initCustomMainMenu() {
        // 创建菜单容器 - 完全适应窗口大小
        VBox menuContainer = new VBox(20);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setPadding(new Insets(50));
        
        // 设置菜单容器完全适应窗口
        menuContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        menuContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        menuContainer.setMaxWidth(Double.MAX_VALUE);
        menuContainer.setMaxHeight(Double.MAX_VALUE);
        menuContainer.setMinWidth(350);
        menuContainer.setMinHeight(450);
        
        // 应用菜单容器样式
        menuContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " +
            "rgba(12, 76, 76, 0.9), " +
            "rgba(20, 99, 99, 0.85), " +
            "rgba(30, 132, 132, 0.9), " +
            "rgba(12, 76, 76, 0.95)); " +
            "-fx-background-radius: 25; " +
            "-fx-border-color: linear-gradient(to bottom right, " +
            "rgba(45, 212, 191, 0.8), " +
            "rgba(16, 185, 129, 0.6), " +
            "rgba(45, 212, 191, 0.8)); " +
            "-fx-border-width: 4; " +
            "-fx-border-radius: 25; " +
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.4), 25, 0, 0, 15);"
        );

        // 菜单标题 - 适应窗口大小
        Label title = new Label("Roguelike Survivor");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        title.setTextFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.6), 20, 0, 0, 10); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );
        title.setWrapText(true);

        // 副标题
        Label subtitle = new Label("Demo v0.1");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        subtitle.setTextFill(Color.LIGHTGRAY);
        subtitle.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.4), 10, 0, 0, 5); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );

        // 菜单按钮容器
        VBox buttonContainer = new VBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(30));

        // 创建菜单按钮
        Button startButton = createStyledButton("开始游戏", () -> {
            fireNewGame();
        });

        Button optionsButton = createStyledButton("游戏设置", () -> {
            System.out.println("游戏设置按钮被点击");
            // 显示自定义选项菜单
            OptionsMenu.show();
        });

        Button exitButton = createStyledButton("退出游戏", () -> {
            // 显示确认弹窗，确认后直接退出游戏
            ConfirmationDialog.show("退出游戏", "确定要退出游戏吗？\n所有未保存的进度将会丢失。", "退出", "取消", () -> {
                // 直接调用游戏控制器退出，绕过确认机制
                com.almasb.fxgl.dsl.FXGL.getGameController().exit();
            }, null);
        });

        buttonContainer.getChildren().addAll(startButton, optionsButton, exitButton);

        menuContainer.getChildren().addAll(title, subtitle, buttonContainer);

        // 使用StackPane确保菜单填满整个窗口并居中
        StackPane fullScreenContainer = new StackPane();
        fullScreenContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " +
            "rgba(12, 76, 76, 0.95), " +
            "rgba(20, 99, 99, 0.9), " +
            "rgba(30, 132, 132, 0.95), " +
            "rgba(12, 76, 76, 0.98));"
        );
        
        // 确保菜单容器在StackPane中居中
        StackPane.setAlignment(menuContainer, Pos.CENTER);
        fullScreenContainer.getChildren().add(menuContainer);
        getContentRoot().getChildren().add(fullScreenContainer);
        
        // 添加响应式布局支持
        setupResponsiveLayout(menuContainer, fullScreenContainer);
        
        // 添加进入动画
        addEnterAnimation(menuContainer);
    }

    private Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        // 按钮大小适应窗口
        button.setPrefWidth(250);
        button.setPrefHeight(60);
        button.setMinWidth(180);
        button.setMinHeight(45);
        button.setMaxWidth(320);
        button.setMaxHeight(75);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        button.setTextFill(Color.WHITE);
        button.setWrapText(true);
        
        // 应用按钮样式
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " +
            "rgba(45, 212, 191, 0.8), " +
            "rgba(16, 185, 129, 0.6)); " +
            "-fx-background-radius: 18; " +
            "-fx-border-color: rgba(45, 212, 191, 0.9); " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 18; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 4); " +
            "-fx-cursor: hand;"
        );

        // 悬停效果
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(45, 212, 191, 1.0), " +
                "rgba(16, 185, 129, 0.8)); " +
                "-fx-background-radius: 18; " +
                "-fx-border-color: rgba(45, 212, 191, 1.0); " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.8), 15, 0, 0, 8); " +
                "-fx-scale-x: 1.08; " +
                "-fx-scale-y: 1.08; " +
                "-fx-cursor: hand;"
            );
        });

        // 鼠标离开效果
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(45, 212, 191, 0.8), " +
                "rgba(16, 185, 129, 0.6)); " +
                "-fx-background-radius: 18; " +
                "-fx-border-color: rgba(45, 212, 191, 0.9); " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 4); " +
                "-fx-cursor: hand;"
            );
        });

        // 点击效果
        button.setOnMousePressed(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(16, 185, 129, 0.8), " +
                "rgba(45, 212, 191, 0.6)); " +
                "-fx-background-radius: 18; " +
                "-fx-border-color: rgba(45, 212, 191, 0.9); " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 18; " +
                "-fx-scale-x: 0.95; " +
                "-fx-scale-y: 0.95; " +
                "-fx-cursor: hand;"
            );
        });

        button.setOnAction(e -> action.run());
        return button;
    }

    private void addEnterAnimation(VBox menuContainer) {
        // 淡入动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), menuContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // 缩放动画
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), menuContainer);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        fadeIn.play();
        scaleIn.play();
    }

    private void setupResponsiveLayout(VBox menuContainer, StackPane fullScreenContainer) {
        // 监听窗口大小变化，调整菜单大小以完全适应窗口
        getContentRoot().widthProperty().addListener((obs, oldVal, newVal) -> {
            double windowWidth = newVal.doubleValue();
            // 菜单宽度适应窗口，但保持合理的最小宽度
            double menuWidth = Math.max(windowWidth * 0.6, 350);
            menuContainer.setPrefWidth(menuWidth);
            
            // 确保全屏容器也适应窗口宽度
            fullScreenContainer.setPrefWidth(windowWidth);
        });

        getContentRoot().heightProperty().addListener((obs, oldVal, newVal) -> {
            double windowHeight = newVal.doubleValue();
            // 菜单高度适应窗口，但保持合理的最小高度
            double menuHeight = Math.max(windowHeight * 0.7, 450);
            menuContainer.setPrefHeight(menuHeight);
            
            // 确保全屏容器也适应窗口高度
            fullScreenContainer.setPrefHeight(windowHeight);
        });
        
        // 初始设置
        double initialWidth = Math.max(getContentRoot().getWidth() * 0.6, 350);
        double initialHeight = Math.max(getContentRoot().getHeight() * 0.7, 450);
        menuContainer.setPrefWidth(initialWidth);
        menuContainer.setPrefHeight(initialHeight);
        
        // 设置全屏容器的初始大小
        fullScreenContainer.setPrefWidth(getContentRoot().getWidth());
        fullScreenContainer.setPrefHeight(getContentRoot().getHeight());
    }
}
