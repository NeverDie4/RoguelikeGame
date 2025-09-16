package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Duration;

import java.io.InputStream;

/**
 * 关卡选择覆盖层：进入游戏后先弹此界面，选择地图后进入游戏。
 */
public final class MapSelectMenu {

    public interface OnSelect {
        void onChoose(String mapId);
        void onCancel();
    }

    private static StackPane overlay;
    private static boolean showing = false;
    private static List<BorderPane> mapCards;
    private static Button backButton;
    private static int selectedIndex = 0;
    private static boolean isBackButtonSelected = false;

    public static void show(OnSelect callback) {
        // 确保在 JavaFX 应用线程执行
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(callback));
            return;
        }
        if (showing) return;
        showing = true;

        overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        overlay.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        overlay.setMaxSize(FXGL.getAppWidth(), FXGL.getAppHeight());
        
        // 加载地图选择界面专用样式
        try {
            overlay.getStylesheets().add(MapSelectMenu.class.getResource("/assets/ui/map-select-styles.css").toExternalForm());
        } catch (Exception ignored) {}

        // 添加背景图片
        try {
            ImageView backgroundImage = new ImageView();
            InputStream bgStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/icons/Background.jpg");
            if (bgStream == null) {
                bgStream = MapSelectMenu.class.getResourceAsStream("/assets/icons/Background.jpg");
            }
            if (bgStream != null) {
                backgroundImage.setImage(new Image(bgStream));
                bgStream.close();
                backgroundImage.setFitWidth(FXGL.getAppWidth());
                backgroundImage.setFitHeight(FXGL.getAppHeight());
                backgroundImage.setPreserveRatio(false);
                backgroundImage.setOpacity(0.3); // 设置透明度，让背景不会太突出
                overlay.getChildren().add(backgroundImage);
            }
        } catch (Exception ignored) {}

        // 创建主容器，不占据全屏，左右留出更多空间
        VBox mainContainer = new VBox();
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(40, 200, 40, 200)); // 左右留出200px空间，上下留出40px空间

        // 创建内容面板，限制最大宽度，让界面更加瘦长
        VBox contentPanel = new VBox(15);
        contentPanel.setAlignment(Pos.TOP_CENTER);
        contentPanel.setMaxWidth(500); // 进一步限制最大宽度为500px，让界面更瘦长
        contentPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 15; -fx-border-color: #FFD700; -fx-border-width: 3; -fx-border-radius: 15;");
        contentPanel.setPadding(new Insets(30, 30, 30, 30));

        Label title = new Label("关卡选择");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        title.setTextFill(Color.WHITE);
        contentPanel.getChildren().add(title);

        VBox list = new VBox(15); // 减小卡片之间的间距
        list.setFillWidth(true);
        
        // 初始化列表
        mapCards = new ArrayList<>();
        
        for (MapRegistry.MapInfo info : MapRegistry.getMaps()) {
            BorderPane card = createCard(info, callback);
            mapCards.add(card);
            list.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(450); // 增加高度，让界面更瘦长
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS); // 确保垂直滚动条始终显示
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // 隐藏水平滚动条

        contentPanel.getChildren().add(scroll);

        // 底部返回按钮
        HBox bottom = new HBox();
        bottom.setAlignment(Pos.CENTER_RIGHT);
        backButton = new Button("返回");
        backButton.setPrefWidth(140);
        backButton.setPrefHeight(46);
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> {
            try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
            hide();
            if (callback != null) callback.onCancel();
        });
        bottom.getChildren().add(backButton);
        contentPanel.getChildren().add(bottom);

        // 将内容面板添加到主容器，主容器添加到overlay
        mainContainer.getChildren().add(contentPanel);
        overlay.getChildren().add(mainContainer);

        // 加载键盘导航样式
        try {
            overlay.getStylesheets().add(MapSelectMenu.class.getResource("/assets/ui/keyboard-navigation.css").toExternalForm());
        } catch (Exception ignored) {}
        
        // 加入场景（使用FXGL UI层，避免根节点类型不匹配导致无法挂载）
        FXGL.getGameScene().addUINode(overlay);

        // 添加键盘导航
        setupKeyboardNavigation(callback);
        
        // 设置初始选中状态
        updateSelection();

        // 动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), contentPanel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(250), contentPanel);
        scale.setFromX(0.96); scale.setFromY(0.96);
        scale.setToX(1.0); scale.setToY(1.0);
        fadeIn.play();
        scale.play();

        overlay.requestFocus();
    }

    private static BorderPane createCard(MapRegistry.MapInfo info, OnSelect callback) {
        BorderPane card = new BorderPane();
        card.setPadding(new Insets(8)); // 减小内边距，让卡片更紧凑
        card.getStyleClass().add("map-card");


        // 左侧缩略图容器，添加金色边框
        StackPane thumbContainer = new StackPane();
        thumbContainer.getStyleClass().add("map-preview-container");
        thumbContainer.setPadding(new Insets(2));
        
        ImageView thumb = new ImageView();
        thumb.setFitWidth(110); // 继续减小预览图宽度
        thumb.setFitHeight(80); // 相应调整高度
        thumb.setPreserveRatio(false);
        try {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(info.preview);
            if (is == null) {
                is = MapSelectMenu.class.getResourceAsStream("/" + info.preview);
            }
            if (is != null) {
                thumb.setImage(new Image(is));
                is.close();
            }
        } catch (Exception ignored) {}
        
        thumbContainer.getChildren().add(thumb);
        card.setLeft(thumbContainer);
        BorderPane.setMargin(thumbContainer, new Insets(5, 15, 5, 5));

        // 右侧标题+描述
        VBox right = new VBox(5); // 进一步减小文字间距
        Label title = new Label(info.title);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20)); // 缩小标题字体
        title.setTextFill(Color.web("#ffd166"));
        Label stage = new Label(info.stage);
        stage.setFont(Font.font("Consolas", FontWeight.BOLD, 16)); // 缩小Stage标签字体
        stage.setTextFill(Color.web("#f0f0f0"));
        Label desc = new Label(info.desc);
        desc.setFont(Font.font("Segoe UI", 12)); // 缩小描述文字字体
        desc.setWrapText(true);
        desc.setTextFill(Color.WHITE);
        right.getChildren().addAll(title, desc, stage);
        card.setCenter(right);

        // 点击区域
        card.setOnMouseClicked(e -> doChoose(info, callback));
        card.setOnMouseEntered(e -> card.getStyleClass().add("map-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("map-card-hover"));

        return card;
    }

    private static void doChoose(MapRegistry.MapInfo info, OnSelect callback) {
                    try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
        hide();
        if (callback != null) callback.onChoose(info.id);
    }

    public static void hide() {
        if (!showing) return;
        showing = false;
        try { FXGL.getGameScene().removeUINode(overlay); } catch (Exception ignored) {}
        overlay = null;
        mapCards = null;
        backButton = null;
        selectedIndex = 0;
        isBackButtonSelected = false;
    }
    
    /**
     * 设置键盘导航
     */
    private static void setupKeyboardNavigation(OnSelect callback) {
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
                    if (isBackButtonSelected) {
                        hide();
                        if (callback != null) callback.onCancel();
                    } else {
                        activateSelectedMap(callback);
                    }
                }
                case ESCAPE -> {
                    try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
                    hide();
                    if (callback != null) callback.onCancel();
                }
            }
        });
        
        // 确保可以接收键盘事件
        overlay.setFocusTraversable(true);
        overlay.requestFocus();
    }
    
    /**
     * 选择上一个地图
     */
    private static void selectPrevious() {
        if (isBackButtonSelected) {
            // 从返回按钮回到最后一个地图
            isBackButtonSelected = false;
            selectedIndex = mapCards.size() - 1;
        } else if (selectedIndex > 0) {
            selectedIndex--;
        } else {
            // 从第一个地图回到返回按钮
            isBackButtonSelected = true;
        }
        updateSelection();
    }
    
    /**
     * 选择下一个地图
     */
    private static void selectNext() {
        if (isBackButtonSelected) {
            // 从返回按钮回到第一个地图
            isBackButtonSelected = false;
            selectedIndex = 0;
        } else if (selectedIndex < mapCards.size() - 1) {
            selectedIndex++;
        } else {
            // 从最后一个地图到返回按钮
            isBackButtonSelected = true;
        }
        updateSelection();
    }
    
    /**
     * 更新选中状态
     */
    private static void updateSelection() {
        // 清除所有卡片的选中状态
        for (BorderPane card : mapCards) {
            card.getStyleClass().remove("keyboard-selected");
        }
        
        // 清除返回按钮的选中状态
        if (backButton != null) {
            backButton.getStyleClass().remove("keyboard-selected");
        }
        
        if (isBackButtonSelected) {
            // 高亮返回按钮
            if (backButton != null) {
                backButton.getStyleClass().add("keyboard-selected");
            }
        } else {
            // 设置当前选中卡片的状态
            if (selectedIndex >= 0 && selectedIndex < mapCards.size()) {
                BorderPane selectedCard = mapCards.get(selectedIndex);
                selectedCard.getStyleClass().add("keyboard-selected");
            }
        }
    }
    
    
    /**
     * 激活选中的地图
     */
    private static void activateSelectedMap(OnSelect callback) {
        if (selectedIndex >= 0 && selectedIndex < mapCards.size()) {
            List<MapRegistry.MapInfo> maps = MapRegistry.getMaps();
            if (selectedIndex < maps.size()) {
                doChoose(maps.get(selectedIndex), callback);
            }
        }
    }
}


