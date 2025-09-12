package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class Menus {
    private static StackPane container;
    private static VBox currentMenu;

    private static void ensure() {
        if (container == null) {
            container = new StackPane();
            container.setPickOnBounds(false);
            container.getStyleClass().add("fxgl-menu-container");
            FXGL.getGameScene().addUINode(container);
        }
    }

    public static void showStartMenu(Runnable onStart) {
        ensure();
        
        // 创建主菜单面板
        VBox menuPanel = createMenuPanel();
        menuPanel.getStyleClass().add("main-menu");
        
        // 游戏标题
        Label title = new Label("Roguelike Survivor");
        title.getStyleClass().add("title");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        
        // 按钮容器
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(20));
        buttonContainer.getStyleClass().add("button-container");
        
        // 开始游戏按钮
        Button startButton = createStyledButton("开始游戏", () -> {
            hideAll();
            if (onStart != null) onStart.run();
        });
        
        // 设置按钮
        Button settingsButton = createStyledButton("游戏设置", () -> showSettingsMenu());
        
        // 退出游戏按钮
        Button exitButton = createStyledButton("退出游戏", () -> {
            FXGL.getGameController().exit();
        });
        
        buttonContainer.getChildren().addAll(startButton, settingsButton, exitButton);
        
        // 版本信息
        Label versionLabel = new Label("版本 0.1");
        versionLabel.getStyleClass().add("version-info");
        versionLabel.setFont(Font.font("Segoe UI", 12));
        
        menuPanel.getChildren().addAll(title, buttonContainer, versionLabel);
        VBox.setMargin(versionLabel, new Insets(20, 0, 0, 0));
        
        showMenuWithAnimation(menuPanel);
    }

    public static void showPauseMenu(Runnable onResume) {
        ensure();
        
        VBox menuPanel = createMenuPanel();
        menuPanel.getStyleClass().add("game-menu");
        
        Label title = new Label("游戏暂停");
        title.getStyleClass().add("title");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(20));
        
        Button resumeButton = createStyledButton("继续游戏", () -> {
            hideAll();
            if (onResume != null) onResume.run();
        });
        
        Button settingsButton = createStyledButton("游戏设置", () -> showSettingsMenu());
        
        Button mainMenuButton = createStyledButton("返回主菜单", () -> {
            hideAll();
            FXGL.getGameController().startNewGame();
        });
        
        buttonContainer.getChildren().addAll(resumeButton, settingsButton, mainMenuButton);
        menuPanel.getChildren().addAll(title, buttonContainer);
        
        showMenuWithAnimation(menuPanel);
    }

    public static void showGameOver() {
        ensure();
        
        VBox menuPanel = createMenuPanel();
        menuPanel.getStyleClass().add("game-menu");
        
        Label title = new Label("游戏结束");
        title.getStyleClass().add("title");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        
        // 显示最终分数
        int finalScore = FXGL.getWorldProperties().getInt("score");
        Label scoreLabel = new Label("最终分数: " + finalScore);
        scoreLabel.getStyleClass().add("score-display");
        scoreLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(20));
        
        Button restartButton = createStyledButton("重新开始", () -> {
            FXGL.getGameController().startNewGame();
        });
        
        Button mainMenuButton = createStyledButton("返回主菜单", () -> {
            hideAll();
            FXGL.getGameController().startNewGame();
        });
        
        buttonContainer.getChildren().addAll(restartButton, mainMenuButton);
        menuPanel.getChildren().addAll(title, scoreLabel, buttonContainer);
        
        showMenuWithAnimation(menuPanel);
    }

    public static void showSettingsMenu() {
        ensure();
        
        VBox menuPanel = createMenuPanel();
        menuPanel.getStyleClass().add("game-menu");
        
        Label title = new Label("游戏设置");
        title.getStyleClass().add("title");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        
        VBox settingsContainer = new VBox(20);
        settingsContainer.setAlignment(Pos.CENTER);
        settingsContainer.setPadding(new Insets(20));
        settingsContainer.getStyleClass().add("settings-container");
        
        // 音量设置
        Label volumeLabel = new Label("音量");
        volumeLabel.getStyleClass().add("label");
        volumeLabel.setFont(Font.font("Segoe UI", 16));
        
        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.getStyleClass().add("slider");
        volumeSlider.setPrefWidth(200);
        
        // 音效开关
        CheckBox soundCheckBox = new CheckBox("启用音效");
        soundCheckBox.getStyleClass().add("check-box");
        soundCheckBox.setSelected(true);
        
        // 返回按钮
        Button backButton = createStyledButton("返回", () -> {
            // 这里可以根据需要返回到之前的菜单
            hideAll();
        });
        
        settingsContainer.getChildren().addAll(volumeLabel, volumeSlider, soundCheckBox, backButton);
        menuPanel.getChildren().addAll(title, settingsContainer);
        
        showMenuWithAnimation(menuPanel);
    }

    private static VBox createMenuPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(40));
        panel.setMaxWidth(400);
        panel.setMaxHeight(600);
        return panel;
    }

    private static Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("fxgl-button");
        button.setPrefWidth(200);
        button.setPrefHeight(50);
        button.setOnAction(e -> action.run());
        return button;
    }

    private static void showMenuWithAnimation(VBox menuPanel) {
        currentMenu = menuPanel;
        container.getChildren().setAll(menuPanel);
        
        // 淡入动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), menuPanel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // 缩放动画
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), menuPanel);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        fadeIn.play();
        scaleIn.play();
    }

    public static void hideAll() {
        ensure();
        if (currentMenu != null) {
            // 淡出动画
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentMenu);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> container.getChildren().clear());
            fadeOut.play();
        } else {
            container.getChildren().clear();
        }
        currentMenu = null;
    }
}


