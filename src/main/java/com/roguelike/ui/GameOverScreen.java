package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameState;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class GameOverScreen {

    private static StackPane overlayRoot;

    public static void show(GameState gameState, Runnable onContinue) {
        hide();

        // 覆盖层根节点
        overlayRoot = new StackPane();
        overlayRoot.setPickOnBounds(true);
        overlayRoot.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());

        // 红色过渡遮罩（从透明淡入）
        Rectangle redOverlay = new Rectangle(FXGL.getAppWidth(), FXGL.getAppHeight());
        redOverlay.setFill(Color.color(0.7, 0.0, 0.0));
        redOverlay.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), redOverlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(0.6);
        fadeIn.play();

        // 主布局：顶部标题与底部按钮，保持上下边距相同
        BorderPane layout = new BorderPane();
        layout.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());

        // 顶部“Game Over”标题（金色）
        Label title = new Label("Game Over");
        title.setTextFill(Color.web("#FFD700"));
        title.setFont(Font.font("Segoe UI", 72));

        VBox topBox = new VBox(title);
        topBox.setAlignment(Pos.TOP_CENTER);
        BorderPane.setMargin(topBox, new Insets(80, 0, 0, 0)); // 距离上边界
        layout.setTop(topBox);

        // 底部“返回菜单”按钮（复用主界面按钮样式）
        Button backToMenu = new Button("返回菜单");
        backToMenu.getStyleClass().add("fxgl-button");
        backToMenu.setPrefWidth(200);
        backToMenu.setPrefHeight(50);
        backToMenu.setDefaultButton(true); // 支持 Enter
        backToMenu.setOnAction(e -> {
            try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
            hide();
            // 恢复引擎并返回主菜单
            com.almasb.fxgl.dsl.FXGL.getGameController().resumeEngine();
            com.almasb.fxgl.dsl.FXGL.getGameController().gotoMainMenu();
        });

        VBox bottomBox = new VBox(backToMenu);
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);
        BorderPane.setMargin(bottomBox, new Insets(0, 0, 80, 0)); // 距离下边界，与上边界一致
        layout.setBottom(bottomBox);

        // 中间保持足够空间，避免上下元素过近（这里留空中心区域即可）

        overlayRoot.getChildren().addAll(redOverlay, layout);

        // 处理 Enter 键（若按钮未聚焦也可触发）
        overlayRoot.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                backToMenu.fire();
            }
        });

        // 将覆盖层添加到场景并请求焦点
        FXGL.getGameScene().addUINode(overlayRoot);
        overlayRoot.requestFocus();
    }

    public static void hide() {
        if (overlayRoot != null) {
            FXGL.getGameScene().removeUINode(overlayRoot);
            overlayRoot = null;
        }
    }
}
