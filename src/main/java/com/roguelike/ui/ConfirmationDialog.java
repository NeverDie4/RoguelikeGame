package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 美化的确认对话框类
 */
public class ConfirmationDialog {
    
    private static StackPane overlay;
    private static boolean isShowing = false;
    private static List<Button> dialogButtons;
    private static List<HBox> buttonContainers;
    private static int selectedIndex = 0;

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
        
        // 检测当前场景类型 - 改进逻辑
        boolean isInMainMenu = false;
        try {
            // 检查主舞台场景是否包含主菜单标识
            if (FXGL.getPrimaryStage() != null && 
                FXGL.getPrimaryStage().getScene() != null) {
                
                var scene = FXGL.getPrimaryStage().getScene();
                if (scene.getRoot() != null) {
                    // 检查是否有主菜单容器标识
                    var mainMenuContainer = scene.getRoot().lookup("#main-menu-container");
                    if (mainMenuContainer != null) {
                        isInMainMenu = true;
                        System.out.println("确认弹窗检测到主菜单场景：找到主菜单容器标识");
                    } else {
                        // 检查样式类
                        if (scene.getRoot().getStyleClass().contains("main-menu")) {
                            isInMainMenu = true;
                            System.out.println("确认弹窗检测到主菜单场景：找到主菜单样式类");
                        } else {
                            // 检查按钮文本来判断场景类型
                            var buttons = scene.getRoot().lookupAll(".button");
                            boolean foundMainMenuButtons = false;
                            for (var button : buttons) {
                                if (button.toString().contains("开始游戏") || 
                                    button.toString().contains("退出游戏")) {
                                    foundMainMenuButtons = true;
                                    break;
                                }
                            }
                            if (foundMainMenuButtons) {
                                isInMainMenu = true;
                                System.out.println("确认弹窗检测到主菜单场景：找到主菜单按钮");
                            } else {
                                isInMainMenu = false;
                                System.out.println("确认弹窗检测到游戏场景：未找到主菜单特征");
                            }
                        }
                    }
                } else {
                    isInMainMenu = true;
                    System.out.println("确认弹窗检测到主菜单场景：主舞台场景根节点为空");
                }
            } else {
                isInMainMenu = true;
                System.out.println("确认弹窗检测到主菜单场景：主舞台场景为空");
            }
        } catch (Exception e) {
            System.out.println("确认弹窗检测场景类型时出错: " + e.getMessage());
            // 出错时默认认为在主菜单中（因为退出游戏通常在主菜单中调用）
            isInMainMenu = true;
        }
        
        // 创建覆盖层 - 增强版本，完全阻止底层交互
        overlay = new StackPane();
        overlay.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.7);"
        );
        
        // 设置覆盖层填满整个场景 - 确保完全覆盖
        overlay.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        overlay.setMaxSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        overlay.setMinSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        
        // 添加鼠标事件处理，确保覆盖层能够拦截所有鼠标事件
        overlay.setOnMouseClicked(e -> {
            // 点击覆盖层背景时不关闭弹窗，保持弹窗显示
            e.consume();
        });
        
        // 阻止所有鼠标事件传播到底层
        overlay.setOnMousePressed(e -> e.consume());
        overlay.setOnMouseReleased(e -> e.consume());
        overlay.setOnMouseMoved(e -> e.consume());
        overlay.setOnMouseDragged(e -> e.consume());
        overlay.setOnMouseEntered(e -> e.consume());
        overlay.setOnMouseExited(e -> e.consume());
        
        // 确保覆盖层在最顶层 - 使用更高的优先级
        overlay.setViewOrder(-2000);
        
        // 创建对话框容器
        VBox dialogContainer = new VBox(25);
        dialogContainer.setAlignment(Pos.CENTER);
        dialogContainer.setPadding(new Insets(40));
        
        // 设置对话框容器样式 - 暗黑风格
        dialogContainer.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " +
            "rgba(20, 20, 25, 0.95), " +
            "rgba(15, 15, 20, 0.9), " +
            "rgba(25, 25, 30, 0.95), " +
            "rgba(10, 10, 15, 0.98)); " +
            "-fx-background-radius: 0; " +
            "-fx-border-color: linear-gradient(to bottom right, " +
            "rgba(60, 60, 65, 0.8), " +
            "rgba(40, 40, 45, 0.6), " +
            "rgba(60, 60, 65, 0.8)); " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 20, 0, 0, 10);"
        );
        
        // 设置对话框容器为响应式尺寸 - 基于窗口大小
        double windowWidth = FXGL.getAppWidth();
        double windowHeight = FXGL.getAppHeight();
        
        // 计算响应式尺寸：窗口的25-35%宽度，25-35%高度
        double minWidth = Math.max(300, windowWidth * 0.25);
        double maxWidth = Math.min(500, windowWidth * 0.35);
        double minHeight = Math.max(200, windowHeight * 0.25);
        double maxHeight = Math.min(400, windowHeight * 0.35);
        
        dialogContainer.setMinWidth(minWidth);
        dialogContainer.setMinHeight(minHeight);
        dialogContainer.setMaxWidth(maxWidth);
        dialogContainer.setMaxHeight(maxHeight);
        dialogContainer.setPrefWidth((minWidth + maxWidth) / 2);
        dialogContainer.setPrefHeight((minHeight + maxHeight) / 2);

        // 标题
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 15, 0, 0, 8); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );
        titleLabel.setTextAlignment(TextAlignment.CENTER);

        // 消息内容
        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        messageLabel.setTextFill(Color.LIGHTGRAY);
        messageLabel.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 10, 0, 0, 5); " +
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

        // 初始化按钮列表
        dialogButtons = new ArrayList<>();
        buttonContainers = new ArrayList<>();
        selectedIndex = 0;

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

        // 创建带箭头的按钮容器
        HBox confirmContainer = createButtonWithArrows(confirmButton);
        HBox cancelContainer = createButtonWithArrows(cancelButton);

        // 添加到列表
        dialogButtons.add(confirmButton);
        dialogButtons.add(cancelButton);
        buttonContainers.add(confirmContainer);
        buttonContainers.add(cancelContainer);

        buttonContainer.getChildren().addAll(confirmContainer, cancelContainer);
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
        
        // 确保对话框在最顶层显示 - 使用更高的优先级
        overlay.setViewOrder(-2000);
        overlay.toFront();
        
        // 加载键盘导航样式
        try {
            overlay.getStylesheets().add(ConfirmationDialog.class.getResource("/assets/ui/keyboard-navigation.css").toExternalForm());
        } catch (Exception ignored) {}
        
        // 设置键盘导航
        setupKeyboardNavigation();
        
        // 初始化选中状态
        updateSelection();
        
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

    /**
     * 创建带箭头的按钮容器
     */
    private static HBox createButtonWithArrows(Button button) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("menu-arrow-container");
        
        // 创建左右箭头
        KeyboardArrow leftArrow = new KeyboardArrow(KeyboardArrow.ArrowDirection.LEFT);
        KeyboardArrow rightArrow = new KeyboardArrow(KeyboardArrow.ArrowDirection.RIGHT);
        
        // 设置箭头大小
        leftArrow.setSize(16);
        rightArrow.setSize(16);
        
        // 添加样式类
        leftArrow.getStyleClass().add("menu-arrow-left");
        rightArrow.getStyleClass().add("menu-arrow-right");
        
        // 将箭头和按钮添加到容器
        container.getChildren().addAll(leftArrow, button, rightArrow);
        
        // 存储箭头引用到按钮的用户数据中
        button.getProperties().put("leftArrow", leftArrow);
        button.getProperties().put("rightArrow", rightArrow);
        
        return container;
    }
    
    /**
     * 设置键盘导航
     */
    private static void setupKeyboardNavigation() {
        overlay.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case UP, W -> {
                    try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
                    selectPrevious();
                }
                case DOWN, S -> {
                    try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
                    selectNext();
                }
                case ENTER -> {
                    try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
                    activateSelectedButton();
                }
                default -> {
                    // 忽略其他按键
                }
            }
        });
        
        // 确保可以接收键盘事件
        overlay.setFocusTraversable(true);
        overlay.requestFocus();
    }
    
    /**
     * 选择上一个按钮
     */
    private static void selectPrevious() {
        if (selectedIndex > 0) {
            selectedIndex--;
        } else {
            selectedIndex = dialogButtons.size() - 1; // 循环到最后一个
        }
        updateSelection();
    }
    
    /**
     * 选择下一个按钮
     */
    private static void selectNext() {
        if (selectedIndex < dialogButtons.size() - 1) {
            selectedIndex++;
        } else {
            selectedIndex = 0; // 循环到第一个
        }
        updateSelection();
    }
    
    /**
     * 更新选中状态
     */
    private static void updateSelection() {
        // 清除所有按钮的选中状态
        for (Button button : dialogButtons) {
            button.getStyleClass().remove("keyboard-selected");
            // 恢复默认样式
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
            KeyboardArrow leftArrow = (KeyboardArrow) button.getProperties().get("leftArrow");
            KeyboardArrow rightArrow = (KeyboardArrow) button.getProperties().get("rightArrow");
            if (leftArrow != null) leftArrow.hide();
            if (rightArrow != null) rightArrow.hide();
        }
        
        // 设置当前选中按钮的状态
        if (selectedIndex >= 0 && selectedIndex < dialogButtons.size()) {
            Button selectedButton = dialogButtons.get(selectedIndex);
            selectedButton.getStyleClass().add("keyboard-selected");
            // 应用悬停效果样式
            selectedButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(50, 50, 55, 1.0), " +
                "rgba(35, 35, 40, 1.0)); " +
                "-fx-background-radius: 0; " +
                "-fx-border-color: rgba(80, 80, 85, 1.0); " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 1.0), 12, 0, 0, 6); " +
                "-fx-scale-x: 1.02; " +
                "-fx-scale-y: 1.02; " +
                "-fx-cursor: hand;"
            );
            
            KeyboardArrow leftArrow = (KeyboardArrow) selectedButton.getProperties().get("leftArrow");
            KeyboardArrow rightArrow = (KeyboardArrow) selectedButton.getProperties().get("rightArrow");
            if (leftArrow != null) leftArrow.show();
            if (rightArrow != null) rightArrow.show();
        }
    }
    
    /**
     * 激活选中的按钮
     */
    private static void activateSelectedButton() {
        if (selectedIndex >= 0 && selectedIndex < dialogButtons.size()) {
            Button selectedButton = dialogButtons.get(selectedIndex);
            selectedButton.fire();
        }
    }

    /**
     * 强制清理所有覆盖层，用于游戏重新开始时
     */
    public static void forceCleanup() {
        System.out.println("强制清理确认弹窗覆盖层...");
        isShowing = false;
        if (overlay != null) {
            try {
                // 尝试从主舞台场景移除
                if (FXGL.getPrimaryStage().getScene() != null && 
                    FXGL.getPrimaryStage().getScene().getRoot() instanceof Pane) {
                    ((Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
                    System.out.println("确认弹窗已从主菜单场景强制移除");
                }
                
                // 尝试从游戏场景移除
                if (FXGL.getGameScene() != null && FXGL.getGameScene().getRoot() != null) {
                    FXGL.getGameScene().getRoot().getChildren().remove(overlay);
                    System.out.println("确认弹窗已从游戏场景强制移除");
                }
            } catch (Exception e) {
                System.out.println("强制清理确认弹窗时出错: " + e.getMessage());
            }
            overlay = null;
        }
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
                // 尝试从主舞台场景移除（主菜单场景）
                if (FXGL.getPrimaryStage().getScene() != null && 
                    FXGL.getPrimaryStage().getScene().getRoot() instanceof Pane) {
                    boolean removed = ((Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
                    if (removed) {
                        System.out.println("确认弹窗已从主菜单场景移除");
                    }
                }
                
                // 同时尝试从游戏场景移除（确保完全清理）
                if (FXGL.getGameScene() != null && FXGL.getGameScene().getRoot() != null) {
                    boolean removed = FXGL.getGameScene().getRoot().getChildren().remove(overlay);
                    if (removed) {
                        System.out.println("确认弹窗已从游戏场景移除");
                    }
                }
            } catch (Exception ex) {
                System.out.println("移除确认弹窗时出错: " + ex.getMessage());
            }
            overlay = null;
        });
        fadeOut.play();
    }

    private static Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        // 按钮大小响应式设计 - 基于窗口大小
        double windowWidth = FXGL.getAppWidth();
        double windowHeight = FXGL.getAppHeight();
        
        // 计算响应式按钮尺寸：窗口宽度的10-20%，高度的4-6%
        double buttonMinWidth = Math.max(80, windowWidth * 0.1);
        double buttonMaxWidth = Math.min(200, windowWidth * 0.2);
        double buttonMinHeight = Math.max(35, windowHeight * 0.04);
        double buttonMaxHeight = Math.min(60, windowHeight * 0.06);
        
        button.setMinWidth(buttonMinWidth);
        button.setMinHeight(buttonMinHeight);
        button.setMaxWidth(buttonMaxWidth);
        button.setMaxHeight(buttonMaxHeight);
        button.setPrefWidth((buttonMinWidth + buttonMaxWidth) / 2);
        button.setPrefHeight((buttonMinHeight + buttonMaxHeight) / 2);
        button.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        
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

        button.setOnAction(e -> {
            try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
            action.run();
        });
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
