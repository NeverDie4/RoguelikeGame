package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

public class GameHUD {
    private final GameState gameState;
    private Rectangle hpBar;
    private Label scoreLabel;
    private StackPane root;

    public GameHUD(GameState state) {
        this.gameState = state;
    }

    public void mount() {
        root = new StackPane();
        root.setPickOnBounds(false);

        VBox hud = new VBox(8);
        hud.setPadding(new Insets(10));

        // HP 条
        Rectangle hpBg = new Rectangle(200, 16, Color.color(0,0,0,0.5));
        hpBar = new Rectangle(200, 16, Color.LIMEGREEN);
        StackPane hpBox = new StackPane(hpBg, hpBar);

        // 分数
        scoreLabel = new Label("Score: 0");
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(Font.font(16));

        hud.getChildren().addAll(hpBox, scoreLabel);
        root.getChildren().add(hud);

        FXGL.getGameScene().addUINode(root);

        // 监听事件
        GameEvent.listen(GameEvent.Type.PLAYER_HURT, e -> updateHP(gameState.getPlayerHP(), gameState.getPlayerMaxHP()));
        GameEvent.listen(GameEvent.Type.SCORE_CHANGED, e -> scoreLabel.setText("Score: " + gameState.getScore()));

        // 初始同步
        updateHP(gameState.getPlayerHP(), gameState.getPlayerMaxHP());
        scoreLabel.setText("Score: " + gameState.getScore());
    }

    public void updateHP(int current, int max) {
        double ratio = max <= 0 ? 0 : (double) current / (double) max;
        hpBar.setWidth(200 * Math.max(0, Math.min(1, ratio)));
        hpBar.setFill(ratio > 0.3 ? Color.LIMEGREEN : Color.ORANGERED);
    }
}


