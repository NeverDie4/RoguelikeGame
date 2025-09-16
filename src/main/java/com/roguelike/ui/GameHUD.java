package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class GameHUD {
    private final GameState gameState;
    private Label scoresLabel;
    private Label coinLabel;
    private Label timeLabel;
    private StackPane root;
    private AnimationTimer timeUpdater;

    // 窗口尺寸相关
    private double currentScreenWidth;
    private double currentScreenHeight;

    // 经验条相关组件
    private Rectangle expBarBackground;
    private Rectangle expBar;
    private Label levelLabel;
    private StackPane expBarContainer;

    // 常量值（后续可改为动态）
    private static final int INITIAL_KILLS = 0; // 击杀数
    private static final int INITIAL_LEVEL = 1;

    public GameHUD(GameState state) {
        this.gameState = state;
    }

    public void mount() {
        root = new StackPane();
        root.setPickOnBounds(false);

        // 获取初始窗口尺寸
        updateScreenDimensions();

        // 上方经验条
        initExperienceBar();
        root.getChildren().add(expBarContainer);

        // 时间显示（在经验条下方）
        initTimeDisplay();
        root.getChildren().add(timeContainer);

        // 金币显示（在时间右侧）
        initCoinDisplay();
        root.getChildren().add(coinContainer);

        // 击杀数显示（左上角）
        initKillDisplay();
        root.getChildren().add(killContainer);

        FXGL.getGameScene().addUINode(root);

        // 左上角武器/被动面板
        try {
            WeaponPassivePanel panel = new WeaponPassivePanel();
            panel.attachToScene();
        } catch (Throwable ignored) {}

        // 添加窗口尺寸变化监听
        setupWindowResizeListener();

        // 初始化显示（使用常量值）
        updateKills();
        updateCoins();
        updateTime();

        updateExperienceBar();

        // 启动时间自动更新器（时间保持动态）
        startTimeUpdater();

        // 预留事件监听接口（后续可启用）
        setupEventListeners();
    }

    // UI容器
    private StackPane timeContainer;
    private StackPane coinContainer;
    private StackPane killContainer;

    // 初始化经验条（上方中间）
    private void initExperienceBar() {
        // 计算经验条尺寸（屏幕宽度的80%，高度根据屏幕尺寸调整）
        double expBarWidth = currentScreenWidth * 0.8;
        double expBarHeight = Math.max(16, currentScreenHeight * 0.025); // 增加高度以容纳更多装饰

        // 创建多层背景效果
        Rectangle outerFrame = new Rectangle(expBarWidth + 8, expBarHeight + 8, Color.TRANSPARENT);
        outerFrame.setStroke(createGradientStroke());
        outerFrame.setStrokeWidth(3);
        outerFrame.setArcWidth(expBarHeight * 0.6);
        outerFrame.setArcHeight(expBarHeight * 0.6);

        // 添加外框阴影效果
        DropShadow outerShadow = new DropShadow();
        outerShadow.setColor(Color.rgb(0, 0, 0, 0.6));
        outerShadow.setRadius(8);
        outerShadow.setOffsetX(2);
        outerShadow.setOffsetY(2);
        outerFrame.setEffect(outerShadow);

        // 经验条背景 - 深色内框
        expBarBackground = new Rectangle(expBarWidth, expBarHeight, Color.TRANSPARENT);
        expBarBackground.setStroke(createDarkStroke());
        expBarBackground.setStrokeWidth(2);
        expBarBackground.setArcWidth(expBarHeight * 0.5);
        expBarBackground.setArcHeight(expBarHeight * 0.5);

        // 添加内阴影效果
        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setColor(Color.rgb(0, 0, 0, 0.4));
        innerShadow.setRadius(4);
        innerShadow.setOffsetX(1);
        innerShadow.setOffsetY(1);
        expBarBackground.setEffect(innerShadow);

        // 经验条 - 渐变填充
        expBar = new Rectangle(expBarWidth, expBarHeight, createExperienceGradient());
        expBar.setArcWidth(expBarHeight * 0.5);
        expBar.setArcHeight(expBarHeight * 0.5);

        // 添加发光效果
        Glow glowEffect = new Glow();
        glowEffect.setLevel(0.3);
        expBar.setEffect(glowEffect);

        // 等级标签 - 美化样式
        levelLabel = new Label("Lv." + INITIAL_LEVEL);
        levelLabel.setTextFill(Color.WHITE);
        levelLabel.setFont(Font.font("Arial Bold", Math.max(12, currentScreenHeight * 0.018)));

        // 等级标签背景和效果
        levelLabel.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(0,50,100,0.9), rgba(0,30,60,0.9)); " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 4 12 4 12; " +
            "-fx-border-color: rgba(100,150,255,0.8); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8;"
        );

        // 添加等级标签阴影
        DropShadow labelShadow = new DropShadow();
        labelShadow.setColor(Color.rgb(0, 0, 0, 0.7));
        labelShadow.setRadius(4);
        labelShadow.setOffsetX(1);
        labelShadow.setOffsetY(1);
        levelLabel.setEffect(labelShadow);

        // 经验条容器
        expBarContainer = new StackPane();
        expBarContainer.getChildren().addAll(outerFrame, expBarBackground, expBar, levelLabel);

        // 设置经验条位置（顶部中间）
        StackPane.setAlignment(expBarContainer, Pos.TOP_CENTER);
        expBarContainer.setTranslateY(currentScreenHeight * 0.02);// 距离顶部屏幕高度的2%
        expBarContainer.setTranslateX(currentScreenWidth * 0.1);

        // 关键修复：设置经验条从左端对齐，确保从左向右增长
        StackPane.setAlignment(expBar, Pos.CENTER_LEFT);
        
        // 设置背景和外框居中对齐
        StackPane.setAlignment(expBarBackground, Pos.CENTER);
        StackPane.setAlignment(outerFrame, Pos.CENTER);

        // 设置等级标签位置（右侧）
        StackPane.setAlignment(levelLabel, Pos.CENTER_RIGHT);
        levelLabel.setTranslateX(-12);

        expBarContainer.setVisible(true);
        expBarContainer.setPickOnBounds(false);
    }

    // 初始化时间显示（经验条下方）
    private void initTimeDisplay() {
        timeLabel = new Label("00:00");
        timeLabel.setTextFill(Color.WHITE);
        timeLabel.setFont(Font.font("Arial", Math.max(12, currentScreenHeight * 0.05))); // 字体大小根据屏幕高度调整
        timeLabel.setStyle("-fx-padding: 4 8 4 8;");

        timeContainer = new StackPane();
        timeContainer.getChildren().add(timeLabel);

        // 设置时间位置（经验条下方居中）
        StackPane.setAlignment(timeContainer, Pos.TOP_CENTER);
        timeContainer.setTranslateY(currentScreenHeight * 0.08); // 经验条下方，屏幕高度的8%
        timeContainer.setTranslateX(currentScreenWidth * 0.1);
        timeContainer.setVisible(true);
        timeContainer.setPickOnBounds(false);
    }

    // 初始化金币显示（时间右侧）
    private void initCoinDisplay() {
       
        coinLabel = new Label("金币: " + gameState.getCoins());
        coinLabel.setTextFill(Color.WHITE);
        coinLabel.setFont(Font.font("Arial", Math.max(12, currentScreenHeight * 0.02))); // 字体大小根据屏幕高度调整
        coinLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 4 8 4 8;");

        coinContainer = new StackPane();
        coinContainer.getChildren().add(coinLabel);

        // 设置金币位置（时间右侧一定距离）
        StackPane.setAlignment(coinContainer, Pos.TOP_CENTER);
        coinContainer.setTranslateY(currentScreenHeight * 0.08); // 与时间同一水平线
        coinContainer.setTranslateX(currentScreenWidth * 0.30); // 时间右侧，屏幕宽度的30%

        coinContainer.setVisible(true);
        coinContainer.setPickOnBounds(false);
    }

    // 初始化击杀数显示（左上角）
    private void initKillDisplay() {
        scoresLabel = new Label("击杀: " + INITIAL_KILLS);
        scoresLabel.setTextFill(Color.WHITE);
        scoresLabel.setFont(Font.font("Arial", Math.max(12, currentScreenHeight * 0.02))); // 字体大小根据屏幕高度调整
        scoresLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 4 8 4 8;");

        killContainer = new StackPane();
        killContainer.getChildren().add(scoresLabel);

        // 设置击杀数位置（左上角）
        StackPane.setAlignment(killContainer, Pos.TOP_LEFT);
        killContainer.setTranslateX(currentScreenWidth * 0.40); // 距离左边屏幕宽度的50%
        killContainer.setTranslateY(currentScreenHeight * 0.08); // 距离顶部屏幕高度的1%

        killContainer.setVisible(true);
        killContainer.setPickOnBounds(false);
    }


    // 更新方法（使用常量值，但保留动态更新接口）
    private void updateKills() {
        if (scoresLabel != null) {
            // 当前使用常量值，后续可改为 gameState.getKills()
            scoresLabel.setText("击杀: " + INITIAL_KILLS);
        }
    }

    private void updateCoins() {
        if (coinLabel != null) {
            // 使用GameState中的动态金币值
            coinLabel.setText("金币: " + gameState.getCoins());
        }
    }

    private void updateTime() {
        if (timeLabel != null) {
            // 使用TimeService获取游戏时间
            double gameTime = com.roguelike.core.TimeService.getSeconds();
            long seconds = (long) gameTime;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            
            // 根据游戏状态显示不同的时间格式
            String timeText;
            if (com.roguelike.core.TimeService.isLoading()) {
                timeText = "加载中...";
            } else {
                // 升级界面弹出时也保持时间数字不变，不显示"暂停"字样
                timeText = String.format("%02d:%02d", minutes, seconds);
            }
            
            timeLabel.setText(timeText);
        }
    }

    private void updateExperienceBar() {
        if (expBar != null && levelLabel != null && expBarBackground != null) {
            // 使用当前屏幕宽度计算经验条宽度
            double expBarWidth = currentScreenWidth * 0.8;

            // 更新经验条背景宽度（确保背景始终是完整宽度）
            expBarBackground.setWidth(expBarWidth);

            // 更新经验条宽度（使用GameState中的动态值计算进度）
            int currentExp = gameState.getExperience();
            int maxExp = gameState.getExperienceToNextLevel();
            double progress = maxExp <= 0 ? 1.0 : (double) currentExp / (double) maxExp;
            expBar.setWidth(expBarWidth * progress);

            // 更新等级显示（使用GameState中的动态值）
            int newLevel = gameState.getCurrentLevel();
            String newLevelText = "Lv." + newLevel;

            // 检查是否升级
            boolean leveledUp = !levelLabel.getText().equals(newLevelText) &&
                               !levelLabel.getText().equals("Lv." + INITIAL_LEVEL);

            levelLabel.setText(newLevelText);

            // 根据经验进度改变渐变颜色和效果
            if (progress >= 0.9) {
                expBar.setFill(createLevelUpGradient()); // 接近升级时显示金色渐变
                // 增强发光效果
                Glow glowEffect = new Glow();
                glowEffect.setLevel(0.6);
                expBar.setEffect(glowEffect);
            } else if (progress >= 0.7) {
                expBar.setFill(createHighExpGradient()); // 高经验时显示青色渐变
                // 中等发光效果
                Glow glowEffect = new Glow();
                glowEffect.setLevel(0.4);
                expBar.setEffect(glowEffect);
            } else {
                expBar.setFill(createExperienceGradient()); // 默认蓝色渐变
                // 基础发光效果
                Glow glowEffect = new Glow();
                glowEffect.setLevel(0.3);
                expBar.setEffect(glowEffect);
            }

            // 如果升级了，播放升级动画
            if (leveledUp) {
                playLevelUpAnimation();
            }
        }
    }


    // 启动或恢复时间自动更新器（幂等）
    public void startTimeUpdater() {
        if (timeUpdater == null) {
            timeUpdater = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    updateTime();
                }
            };
        }
        timeUpdater.start();
    }

    // 预留事件监听接口（后续可启用动态更新）
    private void setupEventListeners() {
        // 注释掉的事件监听器，后续需要动态更新时可取消注释

        GameEvent.listen(GameEvent.Type.ENEMY_DEATH, e -> updateKills());
        GameEvent.listen(GameEvent.Type.COINS_CHANGED, e -> updateCoins());
        GameEvent.listen(GameEvent.Type.EXPERIENCE_CHANGED, e -> updateExperienceBar());
        GameEvent.listen(GameEvent.Type.LEVEL_UP, e -> updateExperienceBar());

    }

    // 更新屏幕尺寸
    private void updateScreenDimensions() {
        currentScreenWidth = FXGL.getAppWidth();
        currentScreenHeight = FXGL.getAppHeight();
    }

    // 设置窗口尺寸变化监听器
    private void setupWindowResizeListener() {
        // 使用AnimationTimer定期检查窗口尺寸变化
        AnimationTimer resizeChecker = new AnimationTimer() {
            private double lastWidth = currentScreenWidth;
            private double lastHeight = currentScreenHeight;

            @Override
            public void handle(long now) {
                double currentWidth = FXGL.getAppWidth();
                double currentHeight = FXGL.getAppHeight();

                // 检查尺寸是否发生变化
                if (Math.abs(currentWidth - lastWidth) > 1 || Math.abs(currentHeight - lastHeight) > 1) {
                    updateScreenDimensions();
                    resizeUI();
                    lastWidth = currentWidth;
                    lastHeight = currentHeight;
                }
            }
        };
        resizeChecker.start();
    }

    // 调整UI元素以适应新的窗口尺寸
    private void resizeUI() {
        if (expBarContainer != null && timeContainer != null && coinContainer != null && killContainer != null) {
            // 重新计算经验条尺寸
            double expBarWidth = currentScreenWidth * 0.8;
            double expBarHeight = Math.max(16, currentScreenHeight * 0.025);

            // 更新外框尺寸
            if (expBarContainer.getChildren().size() > 0) {
                Rectangle outerFrame = (Rectangle) expBarContainer.getChildren().get(0);
                outerFrame.setWidth(expBarWidth + 8);
                outerFrame.setHeight(expBarHeight + 8);
                outerFrame.setArcWidth(expBarHeight * 0.6);
                outerFrame.setArcHeight(expBarHeight * 0.6);
            }

            if (expBarBackground != null) {
                expBarBackground.setWidth(expBarWidth);
                expBarBackground.setHeight(expBarHeight);
                expBarBackground.setArcWidth(expBarHeight * 0.5);
                expBarBackground.setArcHeight(expBarHeight * 0.5);
            }
            if (expBar != null) {
                int currentExp = gameState.getExperience();
                int maxExp = gameState.getExperienceToNextLevel();
                double progress = maxExp <= 0 ? 1.0 : (double) currentExp / (double) maxExp;
                expBar.setWidth(expBarWidth * progress);
                expBar.setHeight(expBarHeight);
                expBar.setArcWidth(expBarHeight * 0.5);
                expBar.setArcHeight(expBarHeight * 0.5);
                
                // 重新设置对齐方式，确保从左端增长
                StackPane.setAlignment(expBar, Pos.CENTER_LEFT);
            }

            // 重新定位经验条
            expBarContainer.setTranslateY(currentScreenHeight * 0.02);

            // 重新定位时间显示
            timeContainer.setTranslateY(currentScreenHeight * 0.08);

            // 重新定位金币显示
            coinContainer.setTranslateY(currentScreenHeight * 0.08);
            coinContainer.setTranslateX(currentScreenWidth * 0.1);

            // 重新定位击杀数显示
            killContainer.setTranslateX(currentScreenWidth * 0.01);
            killContainer.setTranslateY(currentScreenHeight * 0.01);

            // 调整字体大小
            double fontSize = Math.max(12, currentScreenHeight * 0.02);
            if (timeLabel != null) {
                timeLabel.setFont(Font.font("Arial", fontSize));
            }
            if (coinLabel != null) {
                coinLabel.setFont(Font.font("Arial", fontSize));
            }
            if (scoresLabel != null) {
                scoresLabel.setFont(Font.font("Arial", fontSize));
            }
            if (levelLabel != null) {
                levelLabel.setFont(Font.font("Arial Bold", Math.max(12, currentScreenHeight * 0.018)));
            }
        }
    }

    // 停止时间更新器
    public void stopTimeUpdater() {
        if (timeUpdater != null) {
            timeUpdater.stop();
        }
    }

    // 暂停计时（语义化包装）
    public void pauseTime() {
        stopTimeUpdater();
    }

    // 恢复计时（语义化包装）
    public void resumeTime() {
        startTimeUpdater();
    }

    // 公共方法：获取当前显示的信息
    public String getCurrentInfo() {
        return String.format("击杀: %d, 金币: %d, 时间: %s, 等级: %d, 经验: %d/%d",
                           INITIAL_KILLS,
                           gameState.getCoins(),
                           "动态时间",
                           gameState.getCurrentLevel(),
                           gameState.getExperience(),
                           gameState.getExperienceToNextLevel());
    }

    // 公共方法：为后续动态更新预留的接口
    public void setKills(int kills) {
        if (scoresLabel != null) {
            scoresLabel.setText("击杀: " + kills);
        }
    }

    public void setCoins(int coins) {
        if (coinLabel != null) {
            coinLabel.setText("金币: " + coins);
        }
    }

    public void setExperience(int experience, int maxExperience, int level) {
        if (expBar != null && levelLabel != null && expBarBackground != null) {
            double expBarWidth = currentScreenWidth * 0.8;

            expBarBackground.setWidth(expBarWidth);

            double progress = maxExperience <= 0 ? 1.0 : (double) experience / (double) maxExperience;
            expBar.setWidth(expBarWidth * progress);

            // 检查是否升级
            String newLevelText = "Lv." + level;
            boolean leveledUp = !levelLabel.getText().equals(newLevelText) &&
                               !levelLabel.getText().equals("Lv." + INITIAL_LEVEL);

            levelLabel.setText(newLevelText);

            // 根据经验进度改变渐变颜色和效果
            if (progress >= 0.9) {
                expBar.setFill(createLevelUpGradient());
                // 增强发光效果
                Glow glowEffect = new Glow();
                glowEffect.setLevel(0.6);
                expBar.setEffect(glowEffect);
            } else if (progress >= 0.7) {
                expBar.setFill(createHighExpGradient());
                // 中等发光效果
                Glow glowEffect = new Glow();
                glowEffect.setLevel(0.4);
                expBar.setEffect(glowEffect);
            } else {
                expBar.setFill(createExperienceGradient());
                // 基础发光效果
                Glow glowEffect = new Glow();
                glowEffect.setLevel(0.3);
                expBar.setEffect(glowEffect);
            }

            // 如果升级了，播放升级动画
            if (leveledUp) {
                playLevelUpAnimation();
            }
        }
    }


    // 公共方法：手动更新所有UI元素
    public void refreshAll() {
        updateKills();
        updateCoins();
        updateTime();
        updateExperienceBar();
    }

    // 创建渐变描边效果
    private Color createGradientStroke() {
        return Color.rgb(100, 150, 255, 0.8);
    }

    // 创建深色描边效果
    private Color createDarkStroke() {
        return Color.rgb(0, 0, 0, 0.6);
    }

    // 创建经验条渐变填充
    private LinearGradient createExperienceGradient() {
        return new LinearGradient(
            0, 0, 1, 0, true, null,
            new Stop(0, Color.rgb(50, 150, 255)),
            new Stop(0.5, Color.rgb(100, 200, 255)),
            new Stop(1, Color.rgb(150, 250, 255))
        );
    }

    // 创建升级时的金色渐变
    private LinearGradient createLevelUpGradient() {
        return new LinearGradient(
            0, 0, 1, 0, true, null,
            new Stop(0, Color.rgb(255, 215, 0)),
            new Stop(0.5, Color.rgb(255, 255, 100)),
            new Stop(1, Color.rgb(255, 200, 0))
        );
    }

    // 创建高经验时的青色渐变
    private LinearGradient createHighExpGradient() {
        return new LinearGradient(
            0, 0, 1, 0, true, null,
            new Stop(0, Color.rgb(0, 200, 200)),
            new Stop(0.5, Color.rgb(100, 255, 255)),
            new Stop(1, Color.rgb(200, 255, 255))
        );
    }

    // 播放升级动画效果
    private void playLevelUpAnimation() {
        // 播放升级音效（类路径加载）
        try { com.roguelike.ui.SoundService.playOnce("levelups/levelup.wav", 3.0); } catch (Throwable ignored) {} // 调高升级音效音量
        if (expBar != null) {
            // 缩放动画
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(300), expBar);
            scaleTransition.setFromX(1.0);
            scaleTransition.setFromY(1.0);
            scaleTransition.setToX(1.1);
            scaleTransition.setToY(1.1);
            scaleTransition.setAutoReverse(true);
            scaleTransition.setCycleCount(2);

            // 淡入淡出动画
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(150), expBar);
            fadeTransition.setFromValue(1.0);
            fadeTransition.setToValue(0.5);
            fadeTransition.setAutoReverse(true);
            fadeTransition.setCycleCount(4);

            scaleTransition.play();
            fadeTransition.play();
        }
    }
}

