package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameState;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class GameOverScreen {
    
    public static void show(GameState gameState, Runnable onContinue) {
        // 创建主容器
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");
        
        // 游戏结束标题
        Label titleLabel = new Label("游戏结束");
        titleLabel.setTextFill(Color.RED);
        titleLabel.setFont(Font.font("Arial Bold", 48));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // 统计数据
        VBox statsContainer = new VBox(10);
        statsContainer.setAlignment(Pos.CENTER);
        
        Label levelLabel = new Label("等级: " + gameState.getCurrentLevel());
        Label expLabel = new Label("经验: " + gameState.getExperience() + "/" + gameState.getExperienceToNextLevel());
        Label killsLabel = new Label("杀敌数: " + gameState.getKillCount());
        Label timeLabel = new Label("存活时间: " + gameState.getFormattedTime());
        Label coinsLabel = new Label("获得金币: " + gameState.getCoins());
        
        // 设置统计标签样式
        Font statsFont = Font.font("Arial", 24);
        Color statsColor = Color.WHITE;
        
        levelLabel.setFont(statsFont);
        levelLabel.setTextFill(statsColor);
        expLabel.setFont(statsFont);
        expLabel.setTextFill(statsColor);
        killsLabel.setFont(statsFont);
        killsLabel.setTextFill(statsColor);
        timeLabel.setFont(statsFont);
        timeLabel.setTextFill(statsColor);
        coinsLabel.setFont(statsFont);
        coinsLabel.setTextFill(statsColor);
        
        statsContainer.getChildren().addAll(levelLabel, expLabel, killsLabel, timeLabel, coinsLabel);
        
        // 按钮容器
        HBox buttonContainer = new HBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        
        // 主界面按钮
        Button mainMenuButton = new Button("主界面");
        mainMenuButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2196F3, #1976D2); " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 20px; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-background-radius: 5;"
        );
        mainMenuButton.setOnAction(e -> {
            hide();
            // 先恢复游戏引擎，然后回到主界面
            com.almasb.fxgl.dsl.FXGL.getGameController().resumeEngine();
            com.almasb.fxgl.dsl.FXGL.getGameController().gotoMainMenu();
        });
        
        // 重新开始按钮
        Button restartButton = new Button("重新开始");
        restartButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #4CAF50, #45a049); " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 20px; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-background-radius: 5;"
        );
        restartButton.setOnAction(e -> {
            hide();
            if (onContinue != null) {
                onContinue.run();
            }
        });
        
        buttonContainer.getChildren().addAll(mainMenuButton, restartButton);
        
        // 组装界面
        container.getChildren().addAll(titleLabel, statsContainer, buttonContainer);
        
        // 添加到游戏场景
        FXGL.getGameScene().addUINode(container);
    }
    
    public static void hide() {
        // 移除游戏结束界面
        var uiNodes = FXGL.getGameScene().getUINodes();
        var nodesToRemove = uiNodes.stream()
            .filter(node -> node instanceof VBox && 
                ((VBox) node).getChildren().stream()
                    .anyMatch(child -> child instanceof Label && 
                        "游戏结束".equals(((Label) child).getText())))
            .toList();
        
        // 逐个移除节点
        for (var node : nodesToRemove) {
            FXGL.getGameScene().removeUINode(node);
        }
    }
}
