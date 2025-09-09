package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class Menus {
    private static StackPane container;

    private static void ensure() {
        if (container == null) {
            container = new StackPane();
            container.setPickOnBounds(false);
            FXGL.getGameScene().addUINode(container);
        }
    }

    public static void showStartMenu(Runnable onStart) {
        ensure();
        Button start = new Button("开始游戏");
        start.setOnAction(e -> {
            hideAll();
            if (onStart != null) onStart.run();
        });
        HBox box = new HBox(10, start);
        container.getChildren().setAll(box);
    }

    public static void showPauseMenu(Runnable onResume) {
        ensure();
        Button resume = new Button("继续");
        resume.setOnAction(e -> {
            hideAll();
            if (onResume != null) onResume.run();
        });
        container.getChildren().setAll(resume);
    }

    public static void showGameOver() {
        ensure();
        Button back = new Button("重新开始");
        back.setOnAction(e -> FXGL.getGameController().startNewGame());
        container.getChildren().setAll(back);
    }

    public static void hideAll() {
        ensure();
        container.getChildren().clear();
    }
}


