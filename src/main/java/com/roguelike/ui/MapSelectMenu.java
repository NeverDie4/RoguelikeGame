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
    private static javafx.scene.Parent overlayParent;
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
        overlay.setId("map-select-overlay");
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
        contentPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 0; -fx-border-color: #FFD700; -fx-border-width: 3; -fx-border-radius: 0;");
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
        
        // 检测当前场景类型并添加到正确的场景
        boolean isInGameScene = false;
        try {
            // 检查当前是否在游戏场景中
            if (FXGL.getGameScene() != null && 
                FXGL.getPrimaryStage().getScene() != null) {
                // 检查游戏场景的根节点是否包含子节点（说明游戏已开始）
                if (FXGL.getGameScene().getRoot().getChildren().size() > 0) {
                    // 进一步检查主舞台的场景根节点是否包含游戏场景的根节点
                    if (FXGL.getPrimaryStage().getScene().getRoot().getChildrenUnmodifiable().contains(FXGL.getGameScene().getRoot())) {
                        isInGameScene = true;
                        System.out.println("[MapSelectMenu] 检测到游戏场景：游戏场景根节点在主舞台中");
                    } else {
                        System.out.println("[MapSelectMenu] 检测到主菜单场景：游戏场景根节点不在主舞台中");
                    }
                } else {
                    System.out.println("[MapSelectMenu] 检测到主菜单场景：游戏场景根节点为空");
                }
            } else {
                System.out.println("[MapSelectMenu] 检测到主菜单场景：游戏场景或主舞台为空");
            }
        } catch (Exception e) {
            System.out.println("[MapSelectMenu] 检测场景类型时出错: " + e.getMessage());
            isInGameScene = false;
        }
        
        if (!isInGameScene) {
            System.out.println("[MapSelectMenu] 当前在主菜单场景，添加到主舞台场景");
            // 在主菜单场景中，添加到主舞台场景
            if (FXGL.getPrimaryStage().getScene() != null && 
                FXGL.getPrimaryStage().getScene().getRoot() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) FXGL.getPrimaryStage().getScene().getRoot();
                pane.getChildren().add(overlay);
                overlayParent = pane;
            } else if (FXGL.getPrimaryStage().getScene() != null &&
                       FXGL.getPrimaryStage().getScene().getRoot() instanceof javafx.scene.Group) {
                javafx.scene.Group group = (javafx.scene.Group) FXGL.getPrimaryStage().getScene().getRoot();
                group.getChildren().add(overlay);
                overlayParent = group;
            }
        } else {
            System.out.println("[MapSelectMenu] 当前在游戏场景，添加到游戏场景");
            // 在游戏场景中，使用FXGL UI层
            FXGL.getGameScene().addUINode(overlay);
            overlayParent = FXGL.getGameScene().getRoot();
        }

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
        
        // 尝试从不同场景移除overlay
        try {
            boolean removed = false;
            try {
                FXGL.getGameScene().removeUINode(overlay);
                System.out.println("[MapSelectMenu] 已从游戏场景移除");
                removed = true;
            } catch (Exception ignored) {}
            if (!removed && overlay != null && overlay.getParent() != null) {
                javafx.scene.Parent parent = overlay.getParent();
                try {
                    if (parent instanceof javafx.scene.layout.Pane pane) {
                        removed = pane.getChildren().remove(overlay);
                    } else if (parent instanceof javafx.scene.Group group) {
                        removed = group.getChildren().remove(overlay);
                    }
                    if (removed) {
                        System.out.println("[MapSelectMenu] 已从父节点移除 overlay");
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e1) {
            try {
                // 如果游戏场景移除失败，尝试从主舞台场景移除
                if (FXGL.getPrimaryStage().getScene() != null) {
                    javafx.scene.Parent root = FXGL.getPrimaryStage().getScene().getRoot();
                    boolean removed = false;
                    if (root instanceof javafx.scene.layout.Pane pane) {
                        removed = pane.getChildren().remove(overlay);
                    } else if (root instanceof javafx.scene.Group group) {
                        removed = group.getChildren().remove(overlay);
                    }
                    if (removed) System.out.println("[MapSelectMenu] 已从主舞台场景移除");
                }
            } catch (Exception e2) {
                System.out.println("[MapSelectMenu] 移除overlay时出错: " + e2.getMessage());
            }
        }
        
        overlay = null;
        overlayParent = null;
        mapCards = null;
        backButton = null;
        selectedIndex = 0;
        isBackButtonSelected = false;
    }

    /**
     * 强制清理覆盖层（无论当前showing状态如何）。
     * 用于场景切换后遗留UI的兜底清理。
     */
    public static void forceCleanup() {
        try {
            if (overlay != null) {
                try {
                    FXGL.getGameScene().removeUINode(overlay);
                    System.out.println("[MapSelectMenu] forceCleanup: 已从游戏场景移除");
                } catch (Exception ignored) {}
                try {
                    if (overlay.getParent() != null) {
                        javafx.scene.Parent parent = overlay.getParent();
                        if (parent instanceof javafx.scene.layout.Pane pane) {
                            pane.getChildren().remove(overlay);
                        } else if (parent instanceof javafx.scene.Group group) {
                            group.getChildren().remove(overlay);
                        }
                        System.out.println("[MapSelectMenu] forceCleanup: 已从父节点移除");
                    } else if (FXGL.getPrimaryStage().getScene() != null) {
                        javafx.scene.Parent root = FXGL.getPrimaryStage().getScene().getRoot();
                        if (root instanceof javafx.scene.layout.Pane pane) {
                            pane.getChildren().remove(overlay);
                        } else if (root instanceof javafx.scene.Group group) {
                            group.getChildren().remove(overlay);
                        }
                        System.out.println("[MapSelectMenu] forceCleanup: 已从主舞台根节点尝试移除");
                    }
                } catch (Exception ignored) {}
            } else {
                // 尝试通过ID扫描并移除遗留节点
                try {
                    javafx.scene.Parent gameRoot = FXGL.getGameScene() != null ? FXGL.getGameScene().getRoot() : null;
                    if (gameRoot != null) {
                        javafx.scene.Node orphan = gameRoot.lookup("#map-select-overlay");
                        if (orphan != null && orphan.getParent() instanceof javafx.scene.layout.Pane pane) {
                            pane.getChildren().remove(orphan);
                            System.out.println("[MapSelectMenu] forceCleanup: 通过ID从GameScene移除遗留overlay");
                        }
                    }
                } catch (Exception ignored) {}
                try {
                    if (FXGL.getPrimaryStage().getScene() != null) {
                        javafx.scene.Parent root = FXGL.getPrimaryStage().getScene().getRoot();
                        javafx.scene.Node orphan = root.lookup("#map-select-overlay");
                        if (orphan != null) {
                            if (orphan.getParent() instanceof javafx.scene.layout.Pane pane) {
                                pane.getChildren().remove(orphan);
                            } else if (orphan.getParent() instanceof javafx.scene.Group group) {
                                group.getChildren().remove(orphan);
                            }
                            System.out.println("[MapSelectMenu] forceCleanup: 通过ID从主舞台根节点移除遗留overlay");
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("[MapSelectMenu] forceCleanup 出错: " + e.getMessage());
        } finally {
            overlay = null;
            overlayParent = null;
            mapCards = null;
            backButton = null;
            selectedIndex = 0;
            isBackButtonSelected = false;
            showing = false;
        }
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


