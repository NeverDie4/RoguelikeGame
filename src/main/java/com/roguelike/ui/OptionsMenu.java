package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.audio.AudioManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * 游戏设置菜单类
 */
public class OptionsMenu {
    
    private static StackPane overlay;
    private static boolean isShowing = false;

    public static void show() {
        System.out.println("尝试显示设置菜单...");
        
        if (isShowing) {
            System.out.println("设置菜单已经在显示中");
            return;
        }
        
        // 检测当前场景类型 - 使用更准确的方法
        boolean isInGameScene = false;
        try {
            // 检查当前是否在游戏场景中
            // 通过检查游戏状态来判断是否在游戏场景中
            if (FXGL.getGameScene() != null && 
                FXGL.getPrimaryStage().getScene() != null) {
                // 检查游戏场景的根节点是否包含子节点（说明游戏已开始）
                if (FXGL.getGameScene().getRoot().getChildren().size() > 0) {
                    // 进一步检查主舞台的场景根节点是否包含游戏场景的根节点
                    if (FXGL.getPrimaryStage().getScene().getRoot().getChildrenUnmodifiable().contains(FXGL.getGameScene().getRoot())) {
                        isInGameScene = true;
                        System.out.println("检测到游戏场景：游戏场景根节点在主舞台中");
                    } else {
                        System.out.println("检测到主菜单场景：游戏场景根节点不在主舞台中");
                    }
                } else {
                    System.out.println("检测到主菜单场景：游戏场景根节点为空");
                }
            } else {
                System.out.println("检测到主菜单场景：游戏场景或主舞台为空");
            }
        } catch (Exception e) {
            System.out.println("检测场景类型时出错: " + e.getMessage());
            // 出错时默认认为在主菜单中
            isInGameScene = false;
        }
        
        if (!isInGameScene) {
            System.out.println("当前在主菜单场景，使用主菜单设置");
            showInMainMenu();
            return;
        }
        
        System.out.println("当前在游戏场景，开始创建设置菜单");
        isShowing = true;
        
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
            // 点击覆盖层背景时不关闭菜单，保持菜单显示
            e.consume();
        });
        
        // 阻止所有鼠标事件传播到底层
        overlay.setOnMousePressed(e -> e.consume());
        overlay.setOnMouseReleased(e -> e.consume());
        overlay.setOnMouseMoved(e -> e.consume());
        overlay.setOnMouseDragged(e -> e.consume());
        overlay.setOnMouseEntered(e -> e.consume());
        overlay.setOnMouseExited(e -> e.consume());
        
        // 创建设置菜单容器
        VBox menuContainer = new VBox(20);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setPadding(new Insets(40));
        
        // 设置菜单容器样式 - 暗黑风格
        menuContainer.setStyle(
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
        
        // 改进菜单容器的响应式尺寸设置
        menuContainer.setMinWidth(350);
        menuContainer.setMinHeight(450);
        menuContainer.setMaxWidth(Region.USE_COMPUTED_SIZE);
        menuContainer.setMaxHeight(Region.USE_COMPUTED_SIZE);
        menuContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        menuContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 标题
        Label title = new Label("游戏设置");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 15, 0, 0, 8); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );

        // 设置选项容器
        VBox optionsContainer = new VBox(25);
        optionsContainer.setAlignment(Pos.CENTER);
        optionsContainer.setPadding(new Insets(20));

        // 创建音频控制组件
        Object[] audioControls = createAudioControls(optionsContainer);
        Label masterVolumeLabel = (Label) audioControls[0];
        Slider masterVolumeSlider = (Slider) audioControls[1];
        Label soundEffectsVolumeLabel = (Label) audioControls[2];
        Slider soundEffectsVolumeSlider = (Slider) audioControls[3];
        Label musicVolumeLabel = (Label) audioControls[4];
        Slider musicVolumeSlider = (Slider) audioControls[5];
        CheckBox soundEffectsCheckBox = (CheckBox) audioControls[6];
        CheckBox musicCheckBox = (CheckBox) audioControls[7];

        // 全屏开关
        CheckBox fullscreenCheckBox = new CheckBox("全屏模式");
        fullscreenCheckBox.setSelected(FXGL.getPrimaryStage().isFullScreen());
        fullscreenCheckBox.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        fullscreenCheckBox.setTextFill(Color.WHITE);
        fullscreenCheckBox.setStyle(
            "-fx-text-fill: white; " +
            "-fx-mark-color: rgba(45, 212, 191, 0.8);"
        );

        // 按钮容器
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(20));

        // 应用按钮
        Button applyButton = createStyledButton("应用设置", () -> {
            // 应用全屏设置
            try {
                if (fullscreenCheckBox.isSelected()) {
                    // 切换到全屏模式
                    FXGL.getPrimaryStage().setFullScreen(true);
                    System.out.println("已应用全屏模式设置");
                } else {
                    // 切换到窗口模式
                    FXGL.getPrimaryStage().setFullScreen(false);
                    System.out.println("已应用窗口模式设置");
                }
            } catch (Exception e) {
                System.out.println("应用全屏设置时出错: " + e.getMessage());
            }
            System.out.println("应用设置");
            hide();
        });

        // 取消按钮
        Button cancelButton = createStyledButton("取消", () -> {
            hide();
        });

        buttonContainer.getChildren().addAll(applyButton, cancelButton);
        optionsContainer.getChildren().addAll(
            masterVolumeLabel, masterVolumeSlider,
            soundEffectsVolumeLabel, soundEffectsVolumeSlider,
            musicVolumeLabel, musicVolumeSlider,
            soundEffectsCheckBox, musicCheckBox, fullscreenCheckBox
        );

        menuContainer.getChildren().addAll(title, optionsContainer, buttonContainer);
        overlay.getChildren().add(menuContainer);

        // 添加到游戏场景根节点，确保在最顶层显示
        FXGL.getGameScene().getRoot().getChildren().add(overlay);
        System.out.println("设置菜单已添加到游戏场景");
        
        // 确保覆盖层在最顶层 - 使用更高的优先级
        overlay.setViewOrder(-2000);
        overlay.toFront();
        System.out.println("设置菜单已设置到最顶层");
        
        // 添加进入动画
        addEnterAnimation(menuContainer);
        System.out.println("设置菜单显示完成");
    }

    /**
     * 强制清理所有覆盖层，用于游戏重新开始时
     */
    public static void forceCleanup() {
        System.out.println("强制清理设置菜单覆盖层...");
        isShowing = false;
        if (overlay != null) {
            try {
                // 尝试从主舞台场景移除
                if (FXGL.getPrimaryStage().getScene() != null && 
                    FXGL.getPrimaryStage().getScene().getRoot() instanceof Pane) {
                    ((Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
                    System.out.println("设置菜单已从主菜单场景强制移除");
                }
                
                // 尝试从游戏场景移除
                if (FXGL.getGameScene() != null && FXGL.getGameScene().getRoot() != null) {
                    FXGL.getGameScene().getRoot().getChildren().remove(overlay);
                    System.out.println("设置菜单已从游戏场景强制移除");
                }
            } catch (Exception e) {
                System.out.println("强制清理设置菜单时出错: " + e.getMessage());
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
                // 首先尝试从主舞台场景移除（主菜单场景）
                if (FXGL.getPrimaryStage().getScene() != null && 
                    FXGL.getPrimaryStage().getScene().getRoot() instanceof Pane) {
                    boolean removed = ((Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
                    if (removed) {
                        System.out.println("设置菜单已从主菜单场景移除");
                    }
                }
                
                // 如果主舞台场景移除失败，尝试从游戏场景移除
                if (FXGL.getGameScene() != null && FXGL.getGameScene().getRoot() != null) {
                    boolean removed = FXGL.getGameScene().getRoot().getChildren().remove(overlay);
                    if (removed) {
                        System.out.println("设置菜单已从游戏场景移除");
                    }
                }
            } catch (Exception ex) {
                System.out.println("移除设置菜单时出错: " + ex.getMessage());
            }
            overlay = null;
        });
        fadeOut.play();
    }

    private static Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        // 改进按钮的响应式尺寸设置
        button.setPrefWidth(Region.USE_COMPUTED_SIZE);
        button.setPrefHeight(Region.USE_COMPUTED_SIZE);
        button.setMinWidth(120);
        button.setMinHeight(40);
        button.setMaxWidth(Region.USE_COMPUTED_SIZE);
        button.setMaxHeight(Region.USE_COMPUTED_SIZE);
        button.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        button.setPadding(new Insets(10, 20, 10, 20));
        
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

        button.setOnAction(e -> action.run());
        return button;
    }

    private static void addEnterAnimation(VBox menuContainer) {
        // 淡入动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // 缩放动画
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), menuContainer);
        scaleIn.setFromX(0.7);
        scaleIn.setFromY(0.7);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        fadeIn.play();
        scaleIn.play();
    }
    
    /**
     * 创建音频控制组件
     * @param optionsContainer 选项容器
     * @return 音频控制组件的数组 [masterVolumeLabel, masterVolumeSlider, soundEffectsVolumeLabel, soundEffectsVolumeSlider, musicVolumeLabel, musicVolumeSlider, soundEffectsCheckBox, musicCheckBox]
     */
    private static Object[] createAudioControls(VBox optionsContainer) {
        // 获取音频管理器实例
        AudioManager audioManager = AudioManager.getInstance();
        
        // 主音量设置
        Label masterVolumeLabel = new Label("主音量: " + audioManager.getMasterVolumePercent() + "%");
        masterVolumeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        masterVolumeLabel.setTextFill(Color.WHITE);
        
        Slider masterVolumeSlider = new Slider(0, 100, audioManager.getMasterVolumePercent());
        masterVolumeSlider.setPrefWidth(200);
        masterVolumeSlider.setStyle(
            "-fx-control-inner-background: rgba(45, 212, 191, 0.3); " +
            "-fx-background-color: rgba(45, 212, 191, 0.2);"
        );
        
        // 音效音量设置
        Label soundEffectsVolumeLabel = new Label("音效音量: " + audioManager.getSoundEffectsVolumePercent() + "%");
        soundEffectsVolumeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        soundEffectsVolumeLabel.setTextFill(Color.WHITE);
        
        Slider soundEffectsVolumeSlider = new Slider(0, 100, audioManager.getSoundEffectsVolumePercent());
        soundEffectsVolumeSlider.setPrefWidth(200);
        soundEffectsVolumeSlider.setStyle(
            "-fx-control-inner-background: rgba(45, 212, 191, 0.3); " +
            "-fx-background-color: rgba(45, 212, 191, 0.2);"
        );
        
        // 背景音乐音量设置
        Label musicVolumeLabel = new Label("背景音乐音量: " + audioManager.getMusicVolumePercent() + "%");
        musicVolumeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        musicVolumeLabel.setTextFill(Color.WHITE);
        
        Slider musicVolumeSlider = new Slider(0, 100, audioManager.getMusicVolumePercent());
        musicVolumeSlider.setPrefWidth(200);
        musicVolumeSlider.setStyle(
            "-fx-control-inner-background: rgba(45, 212, 191, 0.3); " +
            "-fx-background-color: rgba(45, 212, 191, 0.2);"
        );

        // 音效开关
        CheckBox soundEffectsCheckBox = new CheckBox("音效");
        soundEffectsCheckBox.setSelected(audioManager.isSoundEffectsEnabled());
        soundEffectsCheckBox.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        soundEffectsCheckBox.setTextFill(Color.WHITE);
        soundEffectsCheckBox.setStyle(
            "-fx-text-fill: white; " +
            "-fx-mark-color: rgba(45, 212, 191, 0.8);"
        );

        // 音乐开关
        CheckBox musicCheckBox = new CheckBox("背景音乐");
        musicCheckBox.setSelected(audioManager.isMusicEnabled());
        musicCheckBox.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        musicCheckBox.setTextFill(Color.WHITE);
        musicCheckBox.setStyle(
            "-fx-text-fill: white; " +
            "-fx-mark-color: rgba(45, 212, 191, 0.8);"
        );
        
        // 添加滑块值变化监听器
        masterVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int volume = newVal.intValue();
            masterVolumeLabel.setText("主音量: " + volume + "%");
            audioManager.setMasterVolume(volume / 100.0);
        });
        
        soundEffectsVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int volume = newVal.intValue();
            soundEffectsVolumeLabel.setText("音效音量: " + volume + "%");
            audioManager.setSoundEffectsVolume(volume / 100.0);
        });
        
        musicVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int volume = newVal.intValue();
            musicVolumeLabel.setText("背景音乐音量: " + volume + "%");
            audioManager.setMusicVolume(volume / 100.0);
        });
        
        // 添加复选框监听器
        soundEffectsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            audioManager.setSoundEffectsEnabled(newVal);
        });
        
        musicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            audioManager.setMusicEnabled(newVal);
        });
        
        return new Object[]{
            masterVolumeLabel, masterVolumeSlider,
            soundEffectsVolumeLabel, soundEffectsVolumeSlider,
            musicVolumeLabel, musicVolumeSlider,
            soundEffectsCheckBox, musicCheckBox
        };
    }
    
    /**
     * 在主菜单中显示设置菜单
     */
    public static void showInMainMenu() {
        System.out.println("在主菜单中显示设置菜单...");
        
        if (isShowing) {
            System.out.println("设置菜单已经在显示中");
            return;
        }
        
        isShowing = true;
        
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
            // 点击覆盖层背景时不关闭菜单，保持菜单显示
            e.consume();
        });
        
        // 阻止所有鼠标事件传播到底层
        overlay.setOnMousePressed(e -> e.consume());
        overlay.setOnMouseReleased(e -> e.consume());
        overlay.setOnMouseMoved(e -> e.consume());
        overlay.setOnMouseDragged(e -> e.consume());
        overlay.setOnMouseEntered(e -> e.consume());
        overlay.setOnMouseExited(e -> e.consume());
        
        // 创建设置菜单容器
        VBox menuContainer = new VBox(20);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setPadding(new Insets(40));
        
        // 设置菜单容器样式 - 暗黑风格
        menuContainer.setStyle(
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
        
        // 改进菜单容器的响应式尺寸设置
        menuContainer.setMinWidth(350);
        menuContainer.setMinHeight(450);
        menuContainer.setMaxWidth(Region.USE_COMPUTED_SIZE);
        menuContainer.setMaxHeight(Region.USE_COMPUTED_SIZE);
        menuContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        menuContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 标题
        Label title = new Label("游戏设置");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 15, 0, 0, 8); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );

        // 设置选项容器
        VBox optionsContainer = new VBox(25);
        optionsContainer.setAlignment(Pos.CENTER);
        optionsContainer.setPadding(new Insets(20));

        // 创建音频控制组件
        Object[] audioControls = createAudioControls(optionsContainer);
        Label masterVolumeLabel = (Label) audioControls[0];
        Slider masterVolumeSlider = (Slider) audioControls[1];
        Label soundEffectsVolumeLabel = (Label) audioControls[2];
        Slider soundEffectsVolumeSlider = (Slider) audioControls[3];
        Label musicVolumeLabel = (Label) audioControls[4];
        Slider musicVolumeSlider = (Slider) audioControls[5];
        CheckBox soundEffectsCheckBox = (CheckBox) audioControls[6];
        CheckBox musicCheckBox = (CheckBox) audioControls[7];

        // 全屏开关
        CheckBox fullscreenCheckBox = new CheckBox("全屏模式");
        fullscreenCheckBox.setSelected(FXGL.getPrimaryStage().isFullScreen());
        fullscreenCheckBox.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        fullscreenCheckBox.setTextFill(Color.WHITE);
        fullscreenCheckBox.setStyle(
            "-fx-text-fill: white; " +
            "-fx-mark-color: rgba(45, 212, 191, 0.8);"
        );

        // 按钮容器
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(20));

        // 应用按钮
        Button applyButton = createStyledButton("应用设置", () -> {
            // 应用全屏设置
            try {
                if (fullscreenCheckBox.isSelected()) {
                    // 切换到全屏模式
                    FXGL.getPrimaryStage().setFullScreen(true);
                    System.out.println("已应用全屏模式设置");
                } else {
                    // 切换到窗口模式
                    FXGL.getPrimaryStage().setFullScreen(false);
                    System.out.println("已应用窗口模式设置");
                }
            } catch (Exception e) {
                System.out.println("应用全屏设置时出错: " + e.getMessage());
            }
            System.out.println("应用设置");
            hide();
        });

        // 取消按钮
        Button cancelButton = createStyledButton("取消", () -> {
            hide();
        });

        buttonContainer.getChildren().addAll(applyButton, cancelButton);
        optionsContainer.getChildren().addAll(
            masterVolumeLabel, masterVolumeSlider,
            soundEffectsVolumeLabel, soundEffectsVolumeSlider,
            musicVolumeLabel, musicVolumeSlider,
            soundEffectsCheckBox, musicCheckBox, fullscreenCheckBox
        );

        menuContainer.getChildren().addAll(title, optionsContainer, buttonContainer);
        overlay.getChildren().add(menuContainer);
        overlay.setAlignment(Pos.CENTER);
        // 添加到主菜单场景
        try {
            // 使用主舞台的场景
            if (FXGL.getPrimaryStage().getScene() != null && 
                FXGL.getPrimaryStage().getScene().getRoot() instanceof Pane) {
                ((Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().add(overlay);
                System.out.println("设置菜单已添加到主菜单场景");
            } else {
                System.out.println("无法获取主菜单场景");
                return;
            }
        } catch (Exception e) {
            System.out.println("添加到主菜单场景时出错: " + e.getMessage());
            return;
        }
        
        // 确保覆盖层在最顶层 - 使用更高的优先级
        overlay.setViewOrder(-2000);
        overlay.toFront();
        System.out.println("设置菜单已设置到最顶层");
        
        // 添加进入动画
        addEnterAnimation(menuContainer);
        System.out.println("主菜单设置菜单显示完成");
    }
}
