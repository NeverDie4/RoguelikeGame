package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * 美化的确认对话框类
 */
public class ConfirmationDialog {
    
    private static StackPane overlay;
    private static boolean isShowing = false;

    /**
     * 显示确认对话框
     * @param title 对话框标题
     * @param message 确认消息
     * @param confirmText 确认按钮文本
     * @param cancelText 取消按钮文本
     * @param onConfirm 确认时的回调
     * @param onCancel 取消时的回调
     */
    public static void show(String title, String message, String confirmText, String cancelText, 
                          Runnable onConfirm, Runnable onCancel) {
        if (isShowing) {
            return;
        }
        
        isShowing = true;
        
        // 检测当前场景类型
        boolean isInMainMenu = false;
        try {
            // 检查当前是否在主菜单场景
            // 如果游戏场景为null或者游戏场景的根节点为空，说明在主菜单中
            if (FXGL.getGameScene() == null || 
                FXGL.getGameScene().getRoot() == null || 
                FXGL.getGameScene().getRoot().getChildren().isEmpty()) {
                isInMainMenu = true;
                System.out.println("确认弹窗检测到主菜单场景：游戏场景未初始化");
            } else {
                // 检查游戏场景的根节点是否包含子节点（说明游戏已开始）
                if (FXGL.getGameScene().getRoot().getChildren().size() > 0) {
                    // 进一步检查主舞台的场景根节点是否包含游戏场景的根节点
                    if (FXGL.getPrimaryStage().getScene() != null &&
                        FXGL.getPrimaryStage().getScene().getRoot() instanceof Pane) {
                        if (!((Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildrenUnmodifiable().contains(FXGL.getGameScene().getRoot())) {
                            isInMainMenu = true;
                            System.out.println("确认弹窗检测到主菜单场景：游戏场景根节点不在主舞台中");
                        } else {
                            System.out.println("确认弹窗检测到游戏场景：游戏场景根节点在主舞台中");
                        }
                    }
                } else {
                    isInMainMenu = true;
                    System.out.println("确认弹窗检测到主菜单场景：游戏场景根节点为空");
                }
            }
        } catch (Exception e) {
            System.out.println("确认弹窗检测场景类型时出错: " + e.getMessage());
            // 默认认为在主菜单中
            isInMainMenu = true;
        }
        
        // 创建覆盖层
        overlay = new StackPane();
        overlay.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.9);"
        );
        
        // 确保覆盖层在最顶层
        overlay.setViewOrder(-1000);
        
        // 创建对话框容器
        VBox dialogContainer = new VBox(25);
        dialogContainer.setAlignment(Pos.CENTER);
        dialogContainer.setPadding(new Insets(40));
        
        // 设置对话框容器样式
        dialogContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " +
            "rgba(12, 76, 76, 0.95), " +
            "rgba(20, 99, 99, 0.9), " +
            "rgba(30, 132, 132, 0.95), " +
            "rgba(12, 76, 76, 0.98)); " +
            "-fx-background-radius: 20; " +
            "-fx-border-color: linear-gradient(to bottom right, " +
            "rgba(45, 212, 191, 0.8), " +
            "rgba(16, 185, 129, 0.6), " +
            "rgba(45, 212, 191, 0.8)); " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 20; " +
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.4), 20, 0, 0, 10);"
        );
        
        dialogContainer.setMinWidth(400);
        dialogContainer.setMinHeight(250);
        dialogContainer.setMaxWidth(500);
        dialogContainer.setMaxHeight(350);

        // 标题
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.6), 15, 0, 0, 8); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );
        titleLabel.setTextAlignment(TextAlignment.CENTER);

        // 消息内容
        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        messageLabel.setTextFill(Color.LIGHTGRAY);
        messageLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.4), 10, 0, 0, 5); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);

        // 按钮容器
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(20));

        // 确认按钮
        Button confirmButton = createStyledButton(confirmText, () -> {
            hide();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        // 取消按钮
        Button cancelButton = createStyledButton(cancelText, () -> {
            hide();
            if (onCancel != null) {
                onCancel.run();
            }
        });

        buttonContainer.getChildren().addAll(confirmButton, cancelButton);
        dialogContainer.getChildren().addAll(titleLabel, messageLabel, buttonContainer);
        overlay.getChildren().add(dialogContainer);

        // 根据场景类型选择添加位置
        if (isInMainMenu) {
            // 添加到主菜单场景
            try {
                if (FXGL.getPrimaryStage().getScene() != null &&
                    FXGL.getPrimaryStage().getScene().getRoot() instanceof Pane) {
                    ((Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().add(overlay);
                    System.out.println("确认弹窗已添加到主菜单场景");
                } else {
                    System.out.println("无法获取主菜单场景");
                    return;
                }
            } catch (Exception e) {
                System.out.println("添加到主菜单场景时出错: " + e.getMessage());
                return;
            }
        } else {
            // 添加到游戏场景
            FXGL.getGameScene().getRoot().getChildren().add(overlay);
            System.out.println("确认弹窗已添加到游戏场景");
        }
        
        // 设置覆盖层填满整个场景
        overlay.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        overlay.setMaxSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        
        // 确保对话框在最顶层显示
        overlay.setViewOrder(-1000);
        overlay.toFront();
        
        // 添加进入动画
        addEnterAnimation(dialogContainer);
    }

    /**
     * 显示简单的确认对话框（只有确认和取消）
     */
    public static void show(String title, String message, Runnable onConfirm) {
        show(title, message, "确认", "取消", onConfirm, null);
    }

    /**
     * 显示退出游戏确认对话框
     */
    public static void showExitConfirmation(Runnable onConfirm) {
        show("退出游戏", "确定要退出游戏吗？\n所有未保存的进度将会丢失。", "退出", "取消", onConfirm, null);
    }

    /**
     * 显示返回主菜单确认对话框
     */
    public static void showReturnToMainMenuConfirmation(Runnable onConfirm) {
        show("返回主菜单", "确定要返回主菜单吗？\n当前游戏进度将会丢失。", "返回", "取消", onConfirm, null);
    }

    public static void hide() {
        if (!isShowing || overlay == null) {
            return;
        }
        
        isShowing = false;
        
        // 添加退出动画
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            try {
                // 首先尝试从主舞台场景移除（主菜单场景）
                if (FXGL.getPrimaryStage().getScene() != null && 
                    FXGL.getPrimaryStage().getScene().getRoot() instanceof Pane) {
                    boolean removed = ((Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
                    if (removed) {
                        System.out.println("确认弹窗已从主菜单场景移除");
                    }
                }
            } catch (Exception ex) {
                // 如果主舞台场景移除失败，尝试从游戏场景移除
                try {
                    if (FXGL.getGameScene() != null && FXGL.getGameScene().getRoot() != null) {
                        FXGL.getGameScene().getRoot().getChildren().remove(overlay);
                        System.out.println("确认弹窗已从游戏场景移除");
                    }
                } catch (Exception ex2) {
                    System.out.println("移除确认弹窗时出错: " + ex2.getMessage());
                }
            }
            overlay = null;
        });
        fadeOut.play();
    }

    private static Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setPrefWidth(120);
        button.setPrefHeight(45);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        button.setTextFill(Color.WHITE);
        
        // 应用按钮样式
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " +
            "rgba(45, 212, 191, 0.8), " +
            "rgba(16, 185, 129, 0.6)); " +
            "-fx-background-radius: 15; " +
            "-fx-border-color: rgba(45, 212, 191, 0.9); " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 15; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 6, 0, 0, 3); " +
            "-fx-cursor: hand;"
        );

        // 悬停效果
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(45, 212, 191, 1.0), " +
                "rgba(16, 185, 129, 0.8)); " +
                "-fx-background-radius: 15; " +
                "-fx-border-color: rgba(45, 212, 191, 1.0); " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.8), 10, 0, 0, 5); " +
                "-fx-scale-x: 1.05; " +
                "-fx-scale-y: 1.05; " +
                "-fx-cursor: hand;"
            );
        });

        // 鼠标离开效果
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(45, 212, 191, 0.8), " +
                "rgba(16, 185, 129, 0.6)); " +
                "-fx-background-radius: 15; " +
                "-fx-border-color: rgba(45, 212, 191, 0.9); " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 6, 0, 0, 3); " +
                "-fx-cursor: hand;"
            );
        });

        button.setOnAction(e -> action.run());
        return button;
    }

    private static void addEnterAnimation(VBox dialogContainer) {
        // 淡入动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // 缩放动画
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), dialogContainer);
        scaleIn.setFromX(0.7);
        scaleIn.setFromY(0.7);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        fadeIn.play();
        scaleIn.play();
    }
}
