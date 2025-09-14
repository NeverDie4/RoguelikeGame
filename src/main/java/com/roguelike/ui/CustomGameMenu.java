package com.roguelike.ui;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * 自定义游戏菜单类（暂停菜单）
 */
public class CustomGameMenu extends FXGLMenu {

    public CustomGameMenu() {
        super(MenuType.GAME_MENU);
        System.out.println("暂停菜单类已创建，等待用户按ESC键显示");
        // 当暂停菜单创建时，暂停游戏时间
        com.roguelike.core.TimeService.pause();
        System.out.println("暂停菜单显示，游戏时间已暂停");
        System.out.println("开始初始化自定义游戏菜单...");
        initCustomGameMenu();
        System.out.println("自定义游戏菜单初始化完成");
    }

    private void initCustomGameMenu() {
        System.out.println("开始初始化自定义游戏菜单...");
        
        // 创建根节点StackPane，用于背景和菜单的层叠
        StackPane rootContainer = new StackPane();
        
        // 关键修复：设置覆盖层填满整个游戏场景
        rootContainer.setPrefSize(com.almasb.fxgl.dsl.FXGL.getAppWidth(), com.almasb.fxgl.dsl.FXGL.getAppHeight());
        rootContainer.setMaxSize(com.almasb.fxgl.dsl.FXGL.getAppWidth(), com.almasb.fxgl.dsl.FXGL.getAppHeight());
        rootContainer.setMinSize(com.almasb.fxgl.dsl.FXGL.getAppWidth(), com.almasb.fxgl.dsl.FXGL.getAppHeight());
        
        // 创建游戏变暗背景遮罩 - 更深的半透明黑色，让游戏背景变暗
        rootContainer.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.8);"
        );
        
        // 创建菜单容器 - 固定尺寸
        VBox menuContainer = new VBox(20);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setPadding(new Insets(50));
        
        // 设置菜单容器为固定尺寸
        menuContainer.setPrefWidth(400);
        menuContainer.setPrefHeight(500);
        menuContainer.setMaxWidth(400);
        menuContainer.setMaxHeight(500);
        menuContainer.setMinWidth(350);
        menuContainer.setMinHeight(450);
        
        // 确保菜单容器可见
        menuContainer.setVisible(true);
        menuContainer.setManaged(true);
        
        // 应用菜单容器样式 - 暗黑风格
        menuContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " +
            "rgba(20, 20, 25, 0.95), " +
            "rgba(15, 15, 20, 0.9), " +
            "rgba(25, 25, 30, 0.95), " +
            "rgba(10, 10, 15, 0.98)); " +
            "-fx-background-radius: 0; " +
            "-fx-border-color: linear-gradient(to bottom right, " +
            "rgba(60, 60, 65, 0.9), " +
            "rgba(40, 40, 45, 0.7), " +
            "rgba(60, 60, 65, 0.9)); " +
            "-fx-border-width: 4; " +
            "-fx-border-radius: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 25, 0, 0, 15);"
        );

        // 菜单标题
        Label title = new Label("游戏暂停");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        title.setTextFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 20, 0, 0, 10); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );
        title.setWrapText(true);

        // 菜单按钮容器
        VBox buttonContainer = new VBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(30));

        // 创建菜单按钮
        Button resumeButton = createStyledButton("继续游戏", () -> {
            System.out.println("继续游戏按钮被点击");
            // 恢复游戏时间
            com.roguelike.core.TimeService.resume();
            System.out.println("恢复游戏，游戏时间已恢复");
            // 调用FXGL的恢复方法
            fireResume();
        });

        Button optionsButton = createStyledButton("游戏设置", () -> {
            // 显示自定义选项菜单
            OptionsMenu.show();
        });

        Button mainMenuButton = createStyledButton("返回主菜单", () -> {
            // 显示确认弹窗，确认后直接返回主菜单
            ConfirmationDialog.show("返回主菜单", "确定要返回主菜单吗？\n当前游戏进度将会丢失。", "返回", "取消", () -> {
                // 直接调用自定义方法，绕过确认机制
                customExitToMainMenu();
            }, null);
        });

        Button exitButton = createStyledButton("退出游戏", () -> {
            // 显示确认弹窗，确认后直接退出游戏
            ConfirmationDialog.show("退出游戏", "确定要退出游戏吗？\n所有未保存的进度将会丢失。", "退出", "取消", () -> {
                // 直接调用自定义方法，绕过确认机制
                customExit();
            }, null);
        });

        buttonContainer.getChildren().addAll(resumeButton, optionsButton, mainMenuButton, exitButton);
        menuContainer.getChildren().addAll(title, buttonContainer);

        // 将菜单容器添加到根容器中，并居中显示
        StackPane.setAlignment(menuContainer, Pos.CENTER);
        rootContainer.getChildren().add(menuContainer);
        
        // 使用FXGL菜单系统的正确方法：将根容器添加到菜单的内容根节点
        getContentRoot().getChildren().add(rootContainer);
        
        // 设置菜单在最顶层
        rootContainer.setViewOrder(-1000);
        rootContainer.toFront();
        
        // 强制刷新显示
        getContentRoot().requestLayout();
        
        // 添加调试信息
        System.out.println("菜单已添加到内容根节点，子节点数量: " + getContentRoot().getChildren().size());
        System.out.println("菜单容器尺寸: " + menuContainer.getPrefWidth() + "x" + menuContainer.getPrefHeight());
        
        // 添加进入动画
        addEnterAnimation(menuContainer);
        
        System.out.println("暂停菜单已创建并居中显示");
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
        button.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        button.setWrapText(true);
        
        // 应用按钮样式 - 暗黑风格
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " +
            "rgba(30, 30, 35, 0.95), " +
            "rgba(15, 15, 20, 1.0)); " +
            "-fx-background-radius: 0; " +
            "-fx-border-color: rgba(60, 60, 65, 1.0); " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 8, 0, 0, 4); " +
            "-fx-cursor: hand;"
        );

        // 悬停效果 - 暗黑风格
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(50, 50, 55, 1.0), " +
                "rgba(35, 35, 40, 1.0)); " +
                "-fx-background-radius: 0; " +
                "-fx-border-color: rgba(80, 80, 85, 1.0); " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 1.0), 10, 0, 0, 5); " +
                "-fx-scale-x: 1.02; " +
                "-fx-scale-y: 1.02; " +
                "-fx-cursor: hand;"
            );
        });

        // 鼠标离开效果 - 暗黑风格
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(30, 30, 35, 0.95), " +
                "rgba(15, 15, 20, 1.0)); " +
                "-fx-background-radius: 0; " +
                "-fx-border-color: rgba(60, 60, 65, 1.0); " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 8, 0, 0, 4); " +
                "-fx-cursor: hand;"
            );
        });

        // 点击效果 - 暗黑风格
        button.setOnMousePressed(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(15, 15, 20, 1.0), " +
                "rgba(5, 5, 10, 1.0)); " +
                "-fx-background-radius: 0; " +
                "-fx-border-color: rgba(40, 40, 45, 1.0); " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 0; " +
                "-fx-scale-x: 0.98; " +
                "-fx-scale-y: 0.98; " +
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


    /**
     * 自定义返回主菜单方法，绕过原来的确认机制
     */
    private void customExitToMainMenu() {
        // 直接执行返回主菜单操作，不显示确认弹窗
        com.almasb.fxgl.dsl.FXGL.getGameController().gotoMainMenu();
    }

    /**
     * 自定义退出游戏方法，绕过原来的确认机制
     */
    private void customExit() {
        // 直接执行退出游戏操作，不显示确认弹窗
        com.almasb.fxgl.dsl.FXGL.getGameController().exit();
    }
}
