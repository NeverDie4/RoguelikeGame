package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameEvent;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;
import java.util.Map;

import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * 升级弹窗：半透明遮罩 + 居中面板，显示3个选项。
 * 仅类路径加载贴图；点击播放按键音并应用升级。
 */
public final class UpgradeOverlay {
    // 避免重复弹出：全局开关与排队计数
    private static boolean OPEN = false;
    private static int PENDING = 0; // 排队的个数（最多1）
    private static long LAST_REQUEST_MS = 0L; // 去抖：短时间内重复LEVEL_UP只记一次

    public static void enqueueOrShow(com.roguelike.entities.weapons.WeaponManager wm, PassiveItemManager pm) {
        long now = System.currentTimeMillis();
        if (now - LAST_REQUEST_MS < 300) {
            // 300ms 内的重复LEVEL_UP视为同一次，直接忽略
            return;
        }
        LAST_REQUEST_MS = now;

        if (OPEN) {
            // 最多只排队一个
            if (PENDING == 0) PENDING = 1;
        } else {
            new UpgradeOverlay(wm, pm).show();
        }
    }
    private final com.roguelike.entities.weapons.WeaponManager weaponManager;
    private final PassiveItemManager passiveManager;
    private final UpgradeGenerator generator;

    private StackPane overlay;
    private VBox cardsBox;
    private VBox statsBox;
    private final List<UpgradeOption> currentOptionsList = new ArrayList<>();
    private final List<HBox> cardRows = new ArrayList<>();
    private int selectedIndex = 0;
    private Timeline statsTicker;
    private Label hpLabel;
    private Label atkLabel;
    private Label cdLabel;
    private Label dmgLabel;
    private Label projLabel;
    private Label moveLabel;

    public UpgradeOverlay(com.roguelike.entities.weapons.WeaponManager wm, PassiveItemManager pm) {
        this.weaponManager = wm;
        this.passiveManager = pm;
        this.generator = new UpgradeGenerator(wm, pm);
    }

    /**
     * 重置全局静态状态，确保新游戏或返回主菜单后不遗留弹窗状态。
     */
    public static void resetGlobalState() {
        OPEN = false;
        PENDING = 0;
        LAST_REQUEST_MS = 0L;
    }

    public void show() {
        Platform.runLater(() -> {
            if (OPEN) { if (PENDING == 0) PENDING = 1; return; }
            List<UpgradeOption> options = generator.generateThree();
            if (options.isEmpty()) return;
            currentOptionsList.clear();
            currentOptionsList.addAll(options);
            OPEN = true;

            overlay = new StackPane();
            overlay.setPickOnBounds(true);

            // 暗化背景（带入场动画）
            Rectangle dim = new Rectangle(FXGL.getAppWidth(), FXGL.getAppHeight(), Color.color(0,0,0,0.5));
            dim.setOpacity(0.0);
            overlay.getChildren().add(dim);

            // 居中面板
            VBox panel = new VBox(20); // 增加选项之间的垂直间距
            panel.setPadding(new Insets(20, 16, 20, 16)); // 增加上下内边距
            panel.setAlignment(Pos.CENTER);
            // 目标尺寸：按照示例图的长宽比例（更瘦长）
            double margin = FXGL.getAppHeight() * 0.12; // 减小上下边距，确保上边框在计时器下方
            double targetH = Math.max(200, FXGL.getAppHeight() - margin * 2);
            double targetWByScreen = FXGL.getAppWidth() * 0.46;   // 更宽：屏宽约46%
            double targetWByRatio  = targetH * 0.85;              // 宽高比约 0.85:1
            double targetW = Math.min(targetWByScreen, targetWByRatio);
            panel.setPrefHeight(targetH);
            panel.setMaxHeight(targetH);
            panel.setPrefWidth(targetW);
            panel.setMaxWidth(targetW);
            panel.setStyle(
                "-fx-background-color: rgba(40,40,55,0.95);" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: rgba(255,235,160,0.9);" +
                "-fx-border-radius: 0;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 18, 0.2, 0, 6);"
            );
            panel.setOpacity(0.0);
            panel.setScaleX(0.88);
            panel.setScaleY(0.88);

            Label title = new Label("升级！");
            title.setTextFill(Color.WHITE);
            title.setStyle("-fx-font-size: 22px;");
            VBox.setMargin(title, new Insets(0, 0, 12, 0));

            // 选项卡片容器（居中显示）
            cardsBox = new VBox(6);
            cardsBox.setAlignment(Pos.CENTER);
            cardRows.clear();
            for (int i = 0; i < options.size(); i++) {
                UpgradeOption opt = options.get(i);
                HBox row = createCard(opt, i);
                cardRows.add(row);
                cardsBox.getChildren().add(row);
            }
            panel.getChildren().addAll(title, cardsBox);
            overlay.getChildren().add(panel);
            // 垂直定位：放在屏幕中间，但顶部与底部保持与 margin 对称的空白
            StackPane.setAlignment(panel, Pos.CENTER);

            // 左侧人物数值面板：固定在屏幕左侧，不属于升级面板
            statsBox = createStatsBox();
            overlay.getChildren().add(statsBox);
            StackPane.setAlignment(statsBox, Pos.CENTER_LEFT);
            statsBox.setTranslateX(16);
            

            // 加载键盘导航样式
            try {
                overlay.getStylesheets().add(getClass().getResource("/assets/ui/keyboard-navigation.css").toExternalForm());
            } catch (Exception ignored) {}
            
            FXGL.getGameScene().addUINode(overlay);
            // 暂停游戏逻辑并屏蔽游戏输入
            com.roguelike.core.TimeService.pause();
            try { FXGL.getInput().setProcessInput(false); } catch (Throwable ignored) {}
            GameEvent.post(new GameEvent(GameEvent.Type.GAME_PAUSED));

            // 入场动画：背景淡入 + 面板淡入缩放
            FadeTransition dimFade = new FadeTransition(Duration.millis(180), dim);
            dimFade.setFromValue(0.0);
            dimFade.setToValue(1.0);
            dimFade.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition panelFade = new FadeTransition(Duration.millis(200), panel);
            panelFade.setFromValue(0.0);
            panelFade.setToValue(1.0);
            panelFade.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition panelScale = new ScaleTransition(Duration.millis(220), panel);
            panelScale.setFromX(0.88);
            panelScale.setFromY(0.88);
            panelScale.setToX(1.0);
            panelScale.setToY(1.0);
            panelScale.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(dimFade, panelFade, panelScale).play();

            // 实时刷新左侧属性
            startStatsTicker();

            // 初始选中第一个
            selectIndex(0);
            
            // 添加键盘导航
            setupKeyboardNavigation();
        });
    }

    private HBox createCard(UpgradeOption opt, int index) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(12, 24, 12, 24));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(defaultRowStyle());
        row.setMaxWidth(Double.MAX_VALUE);


        // 左上角图片 + 右侧标题 + 下方描述：使用 GridPane 实现两列、两行布局
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(6);
        javafx.scene.layout.ColumnConstraints c0 = new javafx.scene.layout.ColumnConstraints();
        c0.setMinWidth(70); c0.setPrefWidth(70); // 更紧凑，让名称更靠近图标
        javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
        c1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(60, 60);
        ImageView icon = new ImageView();
        
        // 为特定武器设置更大的贴图尺寸
        int iconSize = getIconSizeForWeapon(opt);
        icon.setFitWidth(iconSize); 
        icon.setFitHeight(iconSize); 
        icon.setPreserveRatio(true); 
        icon.setSmooth(true);
        
        Image img = loadImage(opt.getIconPath());
        if (img != null && img.getWidth() > 0) icon.setImage(img);
        javafx.scene.shape.Rectangle frame = new javafx.scene.shape.Rectangle(36, 36);
        frame.setFill(javafx.scene.paint.Color.color(0,0,0,0));
        frame.setStroke(javafx.scene.paint.Color.GOLD);
        frame.setStrokeWidth(2);
        StackPane.setAlignment(frame, Pos.CENTER);
        StackPane.setAlignment(icon, Pos.CENTER);
        iconPane.getChildren().addAll(frame, icon);
        
        // 添加等级数字角标
        int level = getCurrentLevel(opt);
        if (level > 0) {
            Label levelLabel = new Label(String.valueOf(level));
            levelLabel.setTextFill(Color.WHITE);
            levelLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 1 3 1 3;");
            StackPane.setAlignment(levelLabel, Pos.BOTTOM_RIGHT);
            iconPane.getChildren().add(levelLabel);
        }

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        titleLine.setMaxWidth(Double.MAX_VALUE); // 确保行占满可用宽度
        Label name = new Label(opt.getTitle());
        name.setTextFill(Color.WHITE);
        name.setStyle("-fx-font-size: 16px;");
        Label newTag = new Label("新宝物！");
        newTag.setTextFill(Color.GOLD);
        newTag.setStyle("-fx-font-size: 14px;");
        newTag.setVisible(isNewTreasure(opt));
        // 新宝物 紧靠右侧边界，使用固定宽度确保对齐
        javafx.scene.layout.Region rightSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(rightSpacer, javafx.scene.layout.Priority.ALWAYS);
        titleLine.getChildren().addAll(name, rightSpacer, newTag);
        // 设置新宝物标签的固定宽度和居中对齐，确保所有标签在同一水平线上
        newTag.setMinWidth(60);
        newTag.setPrefWidth(60);
        newTag.setMaxWidth(60);
        newTag.setAlignment(Pos.CENTER);
        HBox.setMargin(newTag, new Insets(0, 0, 0, 0));

        Label desc = new Label(opt.getDesc());
        desc.setTextFill(Color.WHITE);
        desc.setStyle("-fx-font-size: 14px;");
        desc.setWrapText(true);
        javafx.scene.layout.GridPane.setMargin(desc, new Insets(6, 0, 0, 0));

        // 添加数值变化信息（仅武器升级时显示）
        VBox descContainer = new VBox(4);
        descContainer.getChildren().add(desc);
        
        if (opt.getKind() == UpgradeOption.Kind.WEAPON && opt.getStatsChange() != null && !opt.getStatsChange().isEmpty()) {
            Label statsLabel = new Label(opt.getStatsChange());
            statsLabel.setTextFill(Color.rgb(255, 220, 130)); // 金色
            statsLabel.setStyle("-fx-font-size: 12px;");
            statsLabel.setWrapText(true);
            descContainer.getChildren().add(statsLabel);
        }

        grid.add(iconPane, 0, 0);
        grid.add(titleLine, 1, 0);
        grid.add(descContainer, 0, 1, 2, 1); // 描述容器横跨两列，位于下方

        row.getChildren().add(grid);

        // 点击选择
        row.setOnMouseClicked(e -> {
            selectedIndex = index;
            updateSelectionStyles();
            try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
            apply(opt);
            close();
        });

        // 在刷新选中样式时更新此箭头的位置与显示
        row.sceneProperty().addListener((obs, o, n) -> updateSelectionStyles());
        return row;
    }

    private void selectIndex(int idx) {
        selectedIndex = Math.max(0, Math.min(idx, cardRows.size() - 1));
        updateSelectionStyles();
    }

    private void updateSelectionStyles() {
        for (int i = 0; i < cardRows.size(); i++) {
            HBox r = cardRows.get(i);
            r.setStyle(i == selectedIndex ? selectedRowStyle() : defaultRowStyle());
        }
    }

    
    /**
     * 设置键盘导航
     */
    private void setupKeyboardNavigation() {
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
                    activateSelectedOption();
                }
            }
        });
        
        // 确保可以接收键盘事件
        overlay.setFocusTraversable(true);
        overlay.requestFocus();
    }
    
    /**
     * 选择上一个选项
     */
    private void selectPrevious() {
        if (selectedIndex > 0) {
            selectedIndex--;
        } else {
            selectedIndex = cardRows.size() - 1; // 循环到最后一个
        }
        updateSelectionStyles();
    }
    
    /**
     * 选择下一个选项
     */
    private void selectNext() {
        if (selectedIndex < cardRows.size() - 1) {
            selectedIndex++;
        } else {
            selectedIndex = 0; // 循环到第一个
        }
        updateSelectionStyles();
    }
    
    /**
     * 激活选中的选项
     */
    private void activateSelectedOption() {
        if (selectedIndex >= 0 && selectedIndex < currentOptionsList.size()) {
            UpgradeOption selectedOption = currentOptionsList.get(selectedIndex);
            apply(selectedOption);
            close();
        }
    }

    private boolean isNewTreasure(UpgradeOption opt) {
        if (opt.getKind() == UpgradeOption.Kind.WEAPON) {
            com.roguelike.entities.weapons.WeaponId wid = com.roguelike.entities.weapons.WeaponId.valueOf("W" + opt.getWeaponIdx2());
            return weaponManager.getLevel(wid) <= 0;
        } else {
            return passiveManager.getLevel(opt.getPassiveId()) <= 0;
        }
    }

    private int getIconSizeForWeapon(UpgradeOption opt) {
        if (opt.getKind() == UpgradeOption.Kind.WEAPON) {
            String weaponIdx = opt.getWeaponIdx2();
            // 为02、03、07、08武器设置更大的贴图尺寸
            if ("02".equals(weaponIdx) || "03".equals(weaponIdx) || 
                "07".equals(weaponIdx) || "08".equals(weaponIdx)) {
                return 32; // 从24增加到32像素
            }
        }
        return 24; // 默认尺寸
    }

    private int getCurrentLevel(UpgradeOption opt) {
        if (opt.getKind() == UpgradeOption.Kind.WEAPON) {
            com.roguelike.entities.weapons.WeaponId wid = com.roguelike.entities.weapons.WeaponId.valueOf("W" + opt.getWeaponIdx2());
            return weaponManager.getLevel(wid);
        } else {
            return passiveManager.getLevel(opt.getPassiveId());
        }
    }

    private String defaultRowStyle() {
        return "-fx-background-color: rgba(70,70,80,0.9); -fx-background-radius: 0; -fx-border-color: rgba(230,210,140,0.7); -fx-border-radius: 0; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 10, 0.2, 0, 2);";
    }

    private String selectedRowStyle() {
        return "-fx-background-color: rgba(85,85,110,0.98); -fx-background-radius: 0; -fx-border-color: rgba(255,235,160,1.0); -fx-border-radius: 0; -fx-border-width: 3; -fx-effect: dropshadow(gaussian, rgba(255,235,160,0.35), 12, 0.2, 0, 0);";
    }

    private VBox createStatsBox() {
        VBox mainBox = new VBox(12);
        mainBox.setAlignment(Pos.TOP_LEFT);
        mainBox.setMinWidth(260);
        mainBox.setMaxWidth(260);

        // 上方小框：装备区域
        VBox equipmentBox = createEquipmentBox();
        
        // 下方大框：玩家数值区域
        VBox statsBox = createPlayerStatsBox();

        mainBox.getChildren().addAll(equipmentBox, statsBox);
        return mainBox;
    }

    private VBox createEquipmentBox() {
        VBox equipmentBox = new VBox(6);
        equipmentBox.setPadding(new Insets(8));
        equipmentBox.setAlignment(Pos.TOP_LEFT);
        equipmentBox.setStyle(
            "-fx-background-color: rgba(28,28,36,0.78);" +
            "-fx-background-radius: 0;" +
            "-fx-border-color: rgba(255,235,160,0.85);" +
            "-fx-border-radius: 0;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 14, 0.2, 0, 4);"
        );
        
        // 标题
        Label title = new Label("当前装备");
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // 武器网格
        javafx.scene.layout.GridPane weaponGrid = new javafx.scene.layout.GridPane();
        weaponGrid.setHgap(4);
        weaponGrid.setVgap(4);
        
        // 被动物品网格
        javafx.scene.layout.GridPane passiveGrid = new javafx.scene.layout.GridPane();
        passiveGrid.setHgap(4);
        passiveGrid.setVgap(4);
        
        // 填充武器槽位
        java.util.List<String> weaponOrder = com.roguelike.ui.ObtainedWeaponsOrder.INSTANCE.getOrder();
        for (int i = 0; i < 8; i++) {
            javafx.scene.Node slot = createInventorySlot(i < weaponOrder.size() ? weaponOrder.get(i) : null, true);
            weaponGrid.add(slot, i, 0);
        }
        
        // 填充被动物品槽位
        java.util.List<PassiveId> passiveOrder = com.roguelike.ui.ObtainedPassivesOrder.INSTANCE.getOrder();
        for (int i = 0; i < 6; i++) {
            javafx.scene.Node slot = createInventorySlot(i < passiveOrder.size() ? passiveOrder.get(i) : null, false);
            passiveGrid.add(slot, i, 0);
        }
        
        equipmentBox.getChildren().addAll(title, weaponGrid, passiveGrid);
        return equipmentBox;
    }

    private VBox createPlayerStatsBox() {
        VBox statsBox = new VBox(8);
        statsBox.setPadding(new Insets(12, 10, 12, 10));
        statsBox.setAlignment(Pos.TOP_LEFT);
        statsBox.setMinHeight(300); // 增加最小高度
        statsBox.setStyle(
            "-fx-background-color: rgba(28,28,36,0.78);" +
            "-fx-background-radius: 0;" +
            "-fx-border-color: rgba(255,235,160,0.85);" +
            "-fx-border-radius: 0;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 14, 0.2, 0, 4);"
        );

        // 人物属性标题
        Label head = new Label("人物属性");
        head.setTextFill(Color.WHITE);
        head.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        hpLabel = createStatLabelWithIcon("生命上限", "assets/images/passive item/p04.webp");
        atkLabel = createStatLabelWithIcon("攻速", "assets/images/passive item/p01.webp");
        cdLabel = createStatLabelWithIcon("冷却缩短", "assets/images/passive item/p03.webp");
        dmgLabel = createStatLabelWithIcon("伤害", "assets/images/passive item/p05.webp");
        projLabel = createStatLabelWithIcon("发射数量", "assets/images/passive item/p02.webp");
        moveLabel = createStatLabelWithIcon("移动速度", "assets/images/passive item/p06.webp");

        updateStatsOnce();
        statsBox.getChildren().addAll(head, hpLabel, atkLabel, cdLabel, dmgLabel, projLabel, moveLabel);
        return statsBox;
    }

    private Label createStatLabelWithIcon(String statName, String iconPath) {
        HBox container = new HBox(6);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMinHeight(24); // 设置最小高度确保垂直居中
        container.setPrefHeight(24);
        
        // 如果有图标路径，添加图标
        if (iconPath != null) {
            Image img = loadImage(iconPath);
            if (img != null && img.getWidth() > 0) {
                ImageView icon = new ImageView(img);
                icon.setFitWidth(16);
                icon.setFitHeight(16);
                icon.setPreserveRatio(true);
                icon.setSmooth(true);
                container.getChildren().add(icon);
            }
        }
        
        // 创建标签
        Label label = new Label();
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 14px;");
        label.setAlignment(Pos.CENTER_LEFT); // 确保文字左对齐
        
        // 将标签添加到容器中
        container.getChildren().add(label);
        
        // 创建一个包装标签，将HBox作为图形
        Label wrapperLabel = new Label();
        wrapperLabel.setGraphic(container);
        wrapperLabel.setUserData(label); // 存储实际标签的引用
        wrapperLabel.setAlignment(Pos.CENTER_LEFT); // 确保整体左对齐
        
        return wrapperLabel;
    }

    private javafx.scene.Node createInventorySlot(Object item, boolean isWeapon) {
        StackPane slot = new StackPane();
        slot.setPrefSize(36, 36);
        
        // 背景和边框
        javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle(36, 36, Color.color(0,0,0,0.3));
        bg.setStroke(Color.GOLD);
        bg.setStrokeWidth(2.0);
        slot.getChildren().add(bg);
        
        if (item != null) {
            String iconPath = null;
            int level = 0;
            
            if (isWeapon && item instanceof String) {
                String weaponIdx = (String) item;
                iconPath = com.roguelike.ui.WeaponIconProvider.pathFor(weaponIdx);
                com.roguelike.entities.weapons.WeaponId wid = com.roguelike.entities.weapons.WeaponId.valueOf("W" + weaponIdx);
                level = weaponManager.getLevel(wid);
            } else if (!isWeapon && item instanceof PassiveId) {
                PassiveId passiveId = (PassiveId) item;
                iconPath = com.roguelike.ui.PassiveIconProvider.pathFor(passiveId);
                level = passiveManager.getLevel(passiveId);
            }
            
            if (iconPath != null) {
                Image img = loadImage(iconPath);
                if (img != null && img.getWidth() > 0) {
                    ImageView icon = new ImageView(img);
                    icon.setFitWidth(32);
                    icon.setFitHeight(32);
                    icon.setPreserveRatio(true);
                    icon.setSmooth(true);
                    slot.getChildren().add(icon);
                }
            }
            
            // 等级数字角标
            if (level > 0) {
                Label levelLabel = new Label(String.valueOf(level));
                levelLabel.setTextFill(Color.WHITE);
                levelLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 1 3 1 3;");
                StackPane.setAlignment(levelLabel, Pos.BOTTOM_RIGHT);
                slot.getChildren().add(levelLabel);
            }
        }
        
        return slot;
    }

    private void startStatsTicker() {
        stopStatsTicker();
        statsTicker = new Timeline(new KeyFrame(Duration.millis(250), e -> updateStatsOnce()));
        statsTicker.setCycleCount(Timeline.INDEFINITE);
        statsTicker.play();
    }

    private void stopStatsTicker() {
        if (statsTicker != null) {
            statsTicker.stop();
            statsTicker = null;
        }
    }

    private void updateStatsOnce() {
        try {
            Object pmObj = FXGL.geto("passiveManager");
            com.roguelike.ui.PassiveItemManager pm = (pmObj instanceof com.roguelike.ui.PassiveItemManager) ? (com.roguelike.ui.PassiveItemManager) pmObj : null;

            int hpBonus = pm != null ? pm.getMaxHpBonus() : 0;
            updateStatLabel(hpLabel, "生命上限: +" + hpBonus);

            double atkMul = pm != null ? pm.getAttackSpeedMultiplier() : 1.0;
            double cdScale = pm != null ? pm.getCooldownScale() : 1.0;
            double dmgMul = pm != null ? pm.getDamageMultiplier() : 1.0;
            int extra = pm != null ? pm.getAdditionalProjectiles() : 0;
            double moveMul = pm != null ? pm.getMoveSpeedMultiplier() : 1.0;

            updateStatLabel(atkLabel, "攻速: +" + (int) Math.round((atkMul - 1.0) * 100) + "%");
            updateStatLabel(cdLabel, "冷却缩短: +" + (int) Math.round((1.0 - cdScale) * 100) + "%");
            updateStatLabel(dmgLabel, "伤害: +" + (int) Math.round((dmgMul - 1.0) * 100) + "%");
            updateStatLabel(projLabel, "发射数量: +" + Math.max(0, extra));
            updateStatLabel(moveLabel, "移动速度: +" + (int) Math.round((moveMul - 1.0) * 100) + "%");
        } catch (Throwable ignored) {}
    }

    private void updateStatLabel(Label wrapperLabel, String text) {
        Object userData = wrapperLabel.getUserData();
        if (userData instanceof Label actualLabel) {
            actualLabel.setText(text);
        } else {
            // 如果没有用户数据，说明是普通标签，直接设置文本
            wrapperLabel.setText(text);
        }
    }

    private void apply(UpgradeOption opt) {
        if (opt.getKind() == UpgradeOption.Kind.WEAPON) {
            com.roguelike.entities.weapons.WeaponId wid = com.roguelike.entities.weapons.WeaponId.valueOf("W" + opt.getWeaponIdx2());
            int cur = weaponManager.getLevel(wid);
            weaponManager.upgradeSpecific(opt.getWeaponIdx2());
            if (cur == 0) {
                com.roguelike.core.GameEvent.post(new com.roguelike.core.GameEvent(com.roguelike.core.GameEvent.Type.WEAPON_UPGRADED, opt.getWeaponIdx2()));
            }
        } else {
            PassiveId pid = opt.getPassiveId();
            int cur = passiveManager.getLevel(pid);
            if (cur == 0) passiveManager.acquire(pid); else passiveManager.setLevel(pid, cur + 1);
        }
    }

    private void close() {
        Platform.runLater(() -> {
            if (overlay != null) {
                FXGL.getGameScene().removeUINode(overlay);
                overlay = null;
            }
            // 恢复逻辑与输入
            com.roguelike.core.TimeService.resume();
            try { FXGL.getInput().setProcessInput(true); } catch (Throwable ignored) {}
            GameEvent.post(new GameEvent(GameEvent.Type.GAME_RESUMED));
            stopStatsTicker();
            // 若有排队的升级，依次弹出
            OPEN = false;
            if (PENDING > 0) {
                PENDING = 0;
                new UpgradeOverlay(weaponManager, passiveManager).show();
            }
        });
    }

    private Image loadImage(String path) {
        // 完整的类路径加载：多 ClassLoader、带/不带前导斜杠，失败则 ImageIO 回退
        InputStream is = null;
        try {
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null) is = ctx.getResourceAsStream(path);
            if (is == null) is = UpgradeOverlay.class.getClassLoader().getResourceAsStream(path);
            if (is == null) is = UpgradeOverlay.class.getResourceAsStream("/" + path);
            if (is == null) is = ClassLoader.getSystemResourceAsStream(path);
            if (is != null) {
                Image img = new Image(is);
                try { is.close(); } catch (Exception ignored) {}
                if (img != null && img.getWidth() > 0 && !img.isError()) return img;
            }
        } catch (Exception ignored) {
            try { if (is != null) is.close(); } catch (Exception ignore2) {}
        }

        try {
            javax.imageio.ImageIO.scanForPlugins();
            java.net.URL url = null;
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null) url = ctx.getResource(path);
            if (url == null) url = UpgradeOverlay.class.getClassLoader().getResource(path);
            if (url == null) url = UpgradeOverlay.class.getResource("/" + path);
            if (url == null) url = ClassLoader.getSystemResource(path);
            if (url != null) {
                java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(url);
                if (bi != null) return javafx.embed.swing.SwingFXUtils.toFXImage(bi, null);
            }
        } catch (Exception ignored) {}
        return null;
    }
}


