package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 自定义加载覆盖层：用于引擎预热与用户可见的加载进度展示。
 * - 具有最短显示时长与分阶段文案
 * - 与后台预热任务并行，二者皆完成后淡出
 */
public final class LoadingOverlay {

    private final StackPane root;
    private final ProgressBar progressBar;
    private final Label messageLabel;

    private LoadingOverlay() {
        root = new StackPane();
        root.setPickOnBounds(true);
        root.setOpacity(0.0);

        Rectangle backdrop = new Rectangle(FXGL.getAppWidth(), FXGL.getAppHeight());
        backdrop.setFill(Color.color(0, 0, 0, 0.92));

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Math.max(480, FXGL.getAppWidth() * 0.55));
        progressBar.setPrefHeight(18);

        messageLabel = new Label("Initializing...");
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setFont(Font.font(18));

        VBox box = new VBox(16, messageLabel, progressBar);
        box.setPadding(new Insets(12));
        box.setAlignment(Pos.CENTER);

        root.getChildren().addAll(backdrop, box);
        StackPane.setAlignment(box, Pos.CENTER);
    }

    public Node getRoot() {
        return root;
    }

    /**
     * 显示加载覆盖层并开始预热。
     * @param minDurationMs 最短显示时长（毫秒）
     * @param onFinished    完成回调（淡出后调用）
     */
    public static void show(int minDurationMs, Runnable onFinished) {
        LoadingOverlay overlay = new LoadingOverlay();

        // 安装到 UI 场景
        FXGL.getGameScene().addUINode(overlay.getRoot());

        // 淡入
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), overlay.getRoot());
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // 1) 计时器型进度（保证最短时长与平滑过渡）
        Timeline timeline = new Timeline();
        int steps = Math.max(10, minDurationMs / 100);
        for (int i = 1; i <= steps; i++) {
            double frac = i / (double) steps;
            Duration at = Duration.millis(i * (minDurationMs / (double) steps));
            timeline.getKeyFrames().add(new KeyFrame(at,
                    new KeyValue(overlay.progressBar.progressProperty(), frac)));
        }
        // 分段文案
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(minDurationMs * 0.10), e -> overlay.setMessage("正在加载字体与样式…")));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(minDurationMs * 0.30), e -> overlay.setMessage("正在加载资源包…")));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(minDurationMs * 0.55), e -> overlay.setMessage("正在加载地图…")));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(minDurationMs * 0.70), e -> overlay.setMessage("正在准备实体系统…")));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(minDurationMs * 0.85), e -> overlay.setMessage("正在预热渲染管线…")));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(minDurationMs * 0.95), e -> overlay.setMessage("正在预热引擎…")));

        // 2) 后台预热任务（尽量不阻塞 UI 线程）
        AtomicBoolean warmDone = new AtomicBoolean(false);
        CompletableFuture.runAsync(() -> overlay.warmUp())
                .whenComplete((v, err) -> warmDone.set(true));

        // 3) 计时器完成后检查后台任务，二者都完成再结束
        timeline.setOnFinished(e -> overlay.tryFinish(warmDone, onFinished));
        timeline.play();
    }

    private void setMessage(String text) {
        messageLabel.setText(text);
    }

    private void tryFinish(AtomicBoolean warmDone, Runnable onFinished) {
        // 若后台未完成，则继续轮询等待，但不再推进进度值
        if (!warmDone.get()) {
            Timeline wait = new Timeline(new KeyFrame(Duration.millis(120), ev -> tryFinish(warmDone, onFinished)));
            wait.play();
            return;
        }

        // 淡出并移除
        FadeTransition fadeOut = new FadeTransition(Duration.millis(420), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(ev -> {
            FXGL.getGameScene().removeUINode(root);
            if (onFinished != null) onFinished.run();
        });
        fadeOut.play();
    }

    /**
     * 预热任务：轻量触发 JIT / JavaFX / FXGL 的常用路径。
     * 注意：仅做安全可重复的操作。
     */
    private void warmUp() {
        try { Thread.sleep(60); } catch (InterruptedException ignored) {}

        // 预热字体与 CSS 应用
        Platform.runLater(() -> {
            try {
                Label tmp = new Label("warm");
                tmp.applyCss();
            } catch (Exception ignored) {}
        });

        // 预热渲染：做一次轻量 snapshot 触发管线准备
        Platform.runLater(() -> {
            try {
                FXGL.getGameScene().getRoot().applyCss();
                FXGL.getGameScene().getRoot().layout();
                FXGL.getGameScene().getRoot().snapshot(null, null);
            } catch (Exception ignored) {}
        });

        // 预热常用纹理（若存在则会缓存，不存在则忽略）
//        for (int i = 0; i < 10; i++) {
//            String name = String.format("bullets/1_%03d.png", i);
//            tryLoadTexture(name);
//        }

        // 模拟引擎循环预热：短暂空转，给 JIT 时间
        busyWait(120);
    }

    private void tryLoadTexture(String name) {
        try {
            FXGL.getAssetLoader().loadTexture(name);
        } catch (Exception ignored) {}
    }

    private void busyWait(long millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            // 空转以触发 JIT
            Math.sqrt(12345.6789);
        }
    }
}


