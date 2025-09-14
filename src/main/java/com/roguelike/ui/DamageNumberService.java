package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * 动态伤害数字服务：在屏幕上方 UI 层生成飘字数字。
 * 颜色规则：0-20 白色，21-100 红色，101+ 紫色。
 */
public final class DamageNumberService {

    private static Pane overlayRoot;
    private static final Deque<Text> pool = new ArrayDeque<>();
    private static final Random random = new Random();

    // 可调参数
    private static double fontSize = 20.0;
    private static double floatDistance = 50.0;
    private static double durationMs = 750.0;

    private DamageNumberService() {}

    private static void ensureOverlay() {
        if (overlayRoot == null) {
            overlayRoot = new Pane();
            overlayRoot.setPickOnBounds(false);
            FXGL.getGameScene().addUINode(overlayRoot);
            return;
        }
        // 重新开始游戏后，如果UI未挂载到当前场景，重新挂载
        if (overlayRoot.getScene() == null || overlayRoot.getScene() != FXGL.getGameScene().getRoot().getScene()) {
            FXGL.getGameScene().addUINode(overlayRoot);
        }
    }

    /**
     * 在世界坐标 (x,y) 位置生成伤害数字。
     */
    public static void spawn(double x, double y, int amount) {
        ensureOverlay();

        // 世界坐标到屏幕坐标（使用可视区域偏移）
        javafx.geometry.Rectangle2D visible = FXGL.getGameScene().getViewport().getVisibleArea();
        Point2D screen = new Point2D(x - visible.getMinX(), y - visible.getMinY());
        Text node = obtain();
        node.setText(String.valueOf(amount));
        node.setFont(Font.font("Arial Black", fontSize));
        node.setFill(pickColor(amount));

        // 轻描边提升可读性
        DropShadow outline = new DropShadow();
        outline.setColor(Color.color(0, 0, 0, 0.9));
        outline.setRadius(2.5);
        outline.setSpread(0.8);
        node.setEffect(outline);

        // 随机微偏移，避免重叠
        double offsetX = randomRange(-8, 8);
        double offsetY = randomRange(-6, 2);

        node.setTranslateX(screen.getX() + offsetX);
        node.setTranslateY(screen.getY() + offsetY);
        node.setScaleX(0.9);
        node.setScaleY(0.9);
        node.setOpacity(1.0);

        overlayRoot.getChildren().add(node);

        // 动画：放大，向上飘，淡出
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), node);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1.1);
        scale.setToY(1.1);

        TranslateTransition move = new TranslateTransition(Duration.millis(durationMs), node);
        move.setByY(-floatDistance);

        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(scale, move, fade);
        pt.setOnFinished(e -> release(node));
        pt.play();
    }

    private static Text obtain() {
        Text t = pool.pollFirst();
        if (t == null) {
            t = new Text();
            t.setMouseTransparent(true);
        }
        return t;
    }

    private static void release(Text t) {
        Node parent = t.getParent();
        if (parent instanceof Pane) {
            ((Pane) parent).getChildren().remove(t);
        }
        pool.offerLast(t);
    }

    private static Color pickColor(int amount) {
        if (amount <= 20) {
            return Color.WHITE;
        } else if (amount <= 100) {
            return Color.RED;
        } else {
            return Color.web("#A64DFF"); // 紫色
        }
    }

    private static double randomRange(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    // 供外部调整参数（可选）
    public static void configure(double newFontSize, double newFloatDistance, double newDurationMs) {
        fontSize = newFontSize;
        floatDistance = newFloatDistance;
        durationMs = newDurationMs;
    }
}


