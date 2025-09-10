package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

public class GameHUD {
    private final GameState gameState;
    private Rectangle hpBar;
    private Label scoreLabel;
    private Label levelLabel;
    private Label expLabel;
    private Rectangle expBar;
    private Label timerLabel;
    private javafx.scene.control.Button pauseButton;
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

        // 等级
        levelLabel = new Label("Level: 1");
        levelLabel.setTextFill(Color.WHITE);
        levelLabel.setFont(Font.font(16));

        // 经验值条
        Rectangle expBg = new Rectangle(200, 12, Color.color(0,0,0,0.5));
        expBar = new Rectangle(200, 12, Color.GOLD);
        StackPane expBox = new StackPane(expBg, expBar);

        // 经验值文本
        expLabel = new Label("Exp: 0/100");
        expLabel.setTextFill(Color.WHITE);
        expLabel.setFont(Font.font(14));

        // 顶部中央计时器
        timerLabel = new Label("00:00");
        timerLabel.setTextFill(Color.WHITE);
        timerLabel.setFont(Font.font(18));
        BorderPane topBar = new BorderPane();
        topBar.setCenter(timerLabel);
        VBox.setMargin(topBar, new Insets(6, 0, 6, 0));

        hud.getChildren().addAll(topBar, hpBox, scoreLabel, levelLabel, expBox, expLabel);
        root.getChildren().add(hud);

        FXGL.getGameScene().addUINode(root);

        // 监听事件
        GameEvent.listen(GameEvent.Type.PLAYER_HURT, e -> updateHP(gameState.getPlayerHP(), gameState.getPlayerMaxHP()));
        GameEvent.listen(GameEvent.Type.PLAYER_HP_CHANGED, e -> updateHP(gameState.getPlayerHP(), gameState.getPlayerMaxHP()));
        GameEvent.listen(GameEvent.Type.SCORE_CHANGED, e -> scoreLabel.setText("Score: " + gameState.getScore()));
        GameEvent.listen(GameEvent.Type.EXP_CHANGED, e -> updateExp());
        GameEvent.listen(GameEvent.Type.LEVEL_UP, e -> updateLevel());

        // 初始同步
        updateHP(gameState.getPlayerHP(), gameState.getPlayerMaxHP());
        scoreLabel.setText("Score: " + gameState.getScore());
        updateLevel();
        updateExp();

        // 计时器：使用受控时间服务 + FXGL 定时刷新显示（UI刷新不改变时间累积）
        com.almasb.fxgl.dsl.FXGL.getGameTimer().runAtInterval(this::refreshTimer, javafx.util.Duration.seconds(0.05));
        refreshTimer();
    }

    public void updateHP(int current, int max) {
        double ratio = max <= 0 ? 0 : (double) current / (double) max;
        hpBar.setWidth(200 * Math.max(0, Math.min(1, ratio)));
        hpBar.setFill(ratio > 0.3 ? Color.LIMEGREEN : Color.ORANGERED);
    }

    public void updateLevel() {
        levelLabel.setText("Level: " + gameState.getLevel());
    }

    public void updateExp() {
        int current = gameState.getCurrentExp();
        int max = gameState.getMaxExp();
        expLabel.setText("Exp: " + current + "/" + max);

        double ratio = max <= 0 ? 0 : (double) current / (double) max;
        expBar.setWidth(200 * Math.max(0, Math.min(1, ratio)));
    }

    private void refreshTimer() {
        double now = com.roguelike.core.TimeService.getSeconds();
        int total = (int) Math.floor(now);
        int minutes = total / 60;
        int seconds = total % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }
}


