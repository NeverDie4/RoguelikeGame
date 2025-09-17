package com.roguelike.ui;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义主菜单类
 */
public class CustomMainMenu extends FXGLMenu {
    
    private List<Button> menuButtons;
    private List<HBox> buttonContainers;
    private int selectedIndex = 0;

    public CustomMainMenu() {
        super(MenuType.MAIN_MENU);
        initCustomMainMenu();
    }

    private void initCustomMainMenu() {
        // 进入主菜单前，兜底清理可能遗留的覆盖层
        try { MapSelectMenu.forceCleanup(); } catch (Exception ignored) {}
        try { OptionsMenu.forceCleanup(); } catch (Exception ignored) {}
        try { ConfirmationDialog.forceCleanup(); } catch (Exception ignored) {}
        // 进入主菜单时切换到大厅音乐
        try { MusicService.playLobby(); } catch (Exception ignored) {}
        // 创建根节点StackPane，用于背景和菜单的层叠
        StackPane rootContainer = new StackPane();
        rootContainer.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        rootContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // 尝试加载背景图片 - 参考MapRenderer.java的加载方式
        try {
            // 使用Java标准资源加载方式，参考MapRenderer.java
            String resourcePath = "assets/icons/Background.jpg";
            InputStream imageStream = getClass().getResourceAsStream("/" + resourcePath);
            
            if (imageStream != null) {
                Image backgroundImage = new Image(imageStream);
                ImageView backgroundView = new ImageView(backgroundImage);
                
                // 设置背景图片覆盖整个窗口
                backgroundView.setFitWidth(FXGL.getAppWidth());
                backgroundView.setFitHeight(FXGL.getAppHeight());
                backgroundView.setPreserveRatio(false);
                backgroundView.setSmooth(true);
                
                // 将背景图片作为第一个子节点（最底层）
                rootContainer.getChildren().add(0, backgroundView);
                System.out.println("背景图片已设置为根节点，覆盖整个窗口: " + resourcePath);
                imageStream.close();
            } else {
                throw new Exception("无法找到背景图片资源: /" + resourcePath);
            }
        } catch (Exception e) {
            System.out.println("背景图片加载失败，使用渐变背景: " + e.getMessage());
            // 如果背景图片加载失败，使用渐变背景
            rootContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " +
                "rgba(12, 76, 76, 0.95), " +
                "rgba(20, 99, 99, 0.9), " +
                "rgba(30, 132, 132, 0.95), " +
                "rgba(12, 76, 76, 0.98));"
            );
        }

        
        // 创建菜单容器 - 固定合理尺寸
        VBox menuContainer = new VBox(20);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setPadding(new Insets(50));
        
        // 设置固定尺寸，避免响应式布局问题
        menuContainer.setPrefWidth(400);
        menuContainer.setPrefHeight(500);
        menuContainer.setMaxWidth(400);
        menuContainer.setMaxHeight(500);
        menuContainer.setMinWidth(350);
        menuContainer.setMinHeight(450);
        
        // 应用菜单容器样式 - 透明背景，无边框
        menuContainer.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background-radius: 25; " +
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.4), 25, 0, 0, 15);"
        );

        // 菜单标题 - 适应窗口大小
        Label title = new Label("Roguelike Survivor");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        title.setTextFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.6), 20, 0, 0, 10); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );
        title.setWrapText(true);

        // 副标题
        Label subtitle = new Label("Demo v0.1");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        subtitle.setTextFill(Color.LIGHTGRAY);
        subtitle.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.4), 10, 0, 0, 5); " +
            "-fx-text-alignment: center; " +
            "-fx-alignment: center;"
        );

        // 菜单按钮容器
        VBox buttonContainer = new VBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(30));

        // 初始化按钮列表
        menuButtons = new ArrayList<>();
        buttonContainers = new ArrayList<>();

        // 创建菜单按钮
        Button startButton = createStyledButton("开始游戏", () -> {
            System.out.println("=== 开始游戏按钮被点击 ===");
            try { 
                MusicService.playBattle(); 
                System.out.println("战斗音乐播放成功");
            } catch (Exception e) {
                System.out.println("战斗音乐播放失败: " + e.getMessage());
            }
            
            // 先显示地图选择菜单，而不是直接开始游戏
            // 直接显示地图选择菜单，不调用Menus.hideAll()避免重复添加节点
            System.out.println("准备显示地图选择菜单...");
            try {
                MapSelectMenu.show(new MapSelectMenu.OnSelect() {
                    @Override
                    public void onChoose(String mapId) {
                        System.out.println("=== 地图选择完成 ===");
                        // 设置选择的地图
                        if (mapId != null && !mapId.isEmpty()) {
                            // 通过FXGL设置全局变量，让GameApp能够获取到选择的地图
                            FXGL.set("selectedMapName", mapId);
                            try { com.roguelike.core.GameApp.setSelectedMapName(mapId); } catch (Exception ignored) {}
                            try { System.setProperty("selectedMapName", mapId); } catch (Exception ignored) {}
                            System.out.println("已选择地图: " + mapId);
                        }
                        // 地图选择完成后，开始游戏
                        System.out.println("开始调用fireNewGame()...");
                        try { com.roguelike.ui.MapSelectMenu.forceCleanup(); } catch (Exception ignored) {}
                        fireNewGame();
                    }
                    @Override
                    public void onCancel() {
                        System.out.println("=== 地图选择被取消 ===");
                        // 取消选择，返回主菜单
                        // 不需要重新显示主菜单，因为CustomMainMenu仍然存在
                        try { 
                            MusicService.playLobby(); 
                            System.out.println("大厅音乐播放成功");
                        } catch (Exception e) {
                            System.out.println("大厅音乐播放失败: " + e.getMessage());
                        }
                    }
                });
                System.out.println("MapSelectMenu.show()调用完成");
            } catch (Exception e) {
                System.err.println("显示地图选择菜单失败: " + e.getMessage());
                e.printStackTrace();
            }
        });

        Button multiplayerButton = createStyledButton("多人游戏", () -> {
            System.out.println("多人游戏按钮被点击");
            // 显示网络菜单
            NetworkMenu.getInstance().show();
        });

        Button optionsButton = createStyledButton("游戏设置", () -> {
            System.out.println("游戏设置按钮被点击");
            // 显示自定义选项菜单
            OptionsMenu.show();
        });

        Button exitButton = createStyledButton("退出游戏", () -> {
            // 显示确认弹窗，确认后直接退出游戏
            ConfirmationDialog.show("退出游戏", "确定要退出游戏吗？\n所有未保存的进度将会丢失。", "退出", "取消", () -> {
                // 清理网络资源
                try {
                    com.roguelike.network.NetworkManager networkManager = com.roguelike.network.NetworkManager.getInstance();
                    if (networkManager != null) {
                        networkManager.cleanup();
                    }
                } catch (Exception e) {
                    System.err.println("清理网络资源时出错: " + e.getMessage());
                }
                // 直接调用游戏控制器退出，绕过确认机制
                com.almasb.fxgl.dsl.FXGL.getGameController().exit();
            }, null);
        });

        // 将按钮添加到列表
        menuButtons.add(startButton);
        menuButtons.add(multiplayerButton);
        menuButtons.add(optionsButton);
        menuButtons.add(exitButton);

        // 为每个按钮创建带箭头的容器
        for (Button button : menuButtons) {
            HBox buttonWithArrows = createButtonWithArrows(button);
            buttonContainers.add(buttonWithArrows);
            buttonContainer.getChildren().add(buttonWithArrows);
        }

        menuContainer.getChildren().addAll(title, subtitle, buttonContainer);

        // 将菜单容器添加到根容器中，并居中显示
        StackPane.setAlignment(menuContainer, Pos.CENTER);
        rootContainer.getChildren().add(menuContainer);
        
        // 添加主菜单标识到菜单容器本身
        menuContainer.getStyleClass().add("main-menu");
        menuContainer.setId("main-menu-container");
        
        // 加载键盘导航样式
        try {
            getContentRoot().getStylesheets().add(getClass().getResource("/assets/ui/keyboard-navigation.css").toExternalForm());
        } catch (Exception ignored) {}
        
        // 将根容器添加到FXGL的内容根节点
        getContentRoot().getChildren().add(rootContainer);
        
        // 添加键盘导航
        setupKeyboardNavigation();
        
        // 设置初始选中状态
        updateSelection();
        
        // 添加进入动画
        addEnterAnimation(menuContainer);
    }

    private Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        // 按钮大小适应窗口
        button.setPrefWidth(250);
        button.setPrefHeight(60);
        button.setMinWidth(180);
        button.setMinHeight(45);
        button.setMaxWidth(320);
        button.setMaxHeight(75);
        button.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        button.setWrapText(true);
        
        // 应用按钮样式 - 暗黑风格
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " +
            "rgba(30, 30, 35, 0.95), " +
            "rgba(15, 15, 20, 1.0)); " +
            "-fx-background-radius: 0; " +
            "-fx-border-color: rgba(60, 60, 65, 1.0); " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 10, 0, 0, 5); " +
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
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 1.0), 12, 0, 0, 6); " +
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
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 10, 0, 0, 5); " +
                "-fx-cursor: hand;"
            );
        });

        // 点击效果 - 暗黑风格
        button.setOnMousePressed(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(15, 15, 20, 1.0), " +
                "rgba(5, 5, 10, 1.0)); " +
                "-fx-background-radius: 0; " +
                "-fx-border-color: rgba(40, 40, 45, 1.0); " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 0; " +
                "-fx-scale-x: 0.98; " +
                "-fx-scale-y: 0.98; " +
                "-fx-cursor: hand;"
            );
        });

        button.setOnAction(e -> {
            try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
            action.run();
        });
        return button;
    }

    private void addEnterAnimation(VBox menuContainer) {
        // 淡入动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), menuContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // 缩放动画
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(400), menuContainer);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        fadeIn.play();
        scaleIn.play();
    }
    
    /**
     * 创建带箭头的按钮容器
     */
    private HBox createButtonWithArrows(Button button) {
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
    private void setupKeyboardNavigation() {
        getContentRoot().setOnKeyPressed(event -> {
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
            }
        });
        
        // 确保可以接收键盘事件
        getContentRoot().setFocusTraversable(true);
        getContentRoot().requestFocus();
    }
    
    /**
     * 选择上一个按钮
     */
    private void selectPrevious() {
        if (selectedIndex > 0) {
            selectedIndex--;
        } else {
            selectedIndex = menuButtons.size() - 1; // 循环到最后一个
        }
        updateSelection();
    }
    
    /**
     * 选择下一个按钮
     */
    private void selectNext() {
        if (selectedIndex < menuButtons.size() - 1) {
            selectedIndex++;
        } else {
            selectedIndex = 0; // 循环到第一个
        }
        updateSelection();
    }
    
    /**
     * 更新选中状态
     */
    private void updateSelection() {
        // 清除所有按钮的选中状态
        for (Button button : menuButtons) {
            button.getStyleClass().remove("keyboard-selected");
            KeyboardArrow leftArrow = (KeyboardArrow) button.getProperties().get("leftArrow");
            KeyboardArrow rightArrow = (KeyboardArrow) button.getProperties().get("rightArrow");
            if (leftArrow != null) leftArrow.hide();
            if (rightArrow != null) rightArrow.hide();
        }
        
        // 设置当前选中按钮的状态
        if (selectedIndex >= 0 && selectedIndex < menuButtons.size()) {
            Button selectedButton = menuButtons.get(selectedIndex);
            selectedButton.getStyleClass().add("keyboard-selected");
            
            KeyboardArrow leftArrow = (KeyboardArrow) selectedButton.getProperties().get("leftArrow");
            KeyboardArrow rightArrow = (KeyboardArrow) selectedButton.getProperties().get("rightArrow");
            if (leftArrow != null) leftArrow.show();
            if (rightArrow != null) rightArrow.show();
        }
    }
    
    /**
     * 激活选中的按钮
     */
    private void activateSelectedButton() {
        if (selectedIndex >= 0 && selectedIndex < menuButtons.size()) {
            Button selectedButton = menuButtons.get(selectedIndex);
            selectedButton.fire(); // 触发按钮的点击事件
        }
    }

}
