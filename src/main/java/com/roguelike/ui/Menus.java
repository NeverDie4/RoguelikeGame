package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class Menus {
    private static StackPane container;

    private static void ensure() {
        if (container == null) {
            container = new StackPane();
            container.setPickOnBounds(false);
            FXGL.getGameScene().addUINode(container);
            // 初次构建菜单容器时，确保大厅音乐播放
            try { MusicService.playLobby(); } catch (Exception ignored) {}
            return;
        }
        // 如果场景已重建（重新开始游戏后），容器未挂载到当前场景则重新挂载
        if (container.getScene() == null || container.getScene() != FXGL.getGameScene().getRoot().getScene()) {
            FXGL.getGameScene().addUINode(container);
        }
    }

    public static void showStartMenu(Runnable onStart) {
        ensure();
        // 播放大厅音乐
        try { MusicService.playLobby(); } catch (Exception ignored) {}
        Button start = new Button("开始游戏");
        start.setOnAction(e -> {
            hideAll();
            // 切换战斗音乐
            try { MusicService.playBattle(); } catch (Exception ignored) {}
            if (onStart != null) onStart.run();
        });
        HBox box = new HBox(10, start);
        container.getChildren().setAll(box);
    }

    public static void showPauseMenu(Runnable onResume) {
        ensure();
        // 暂停音乐
        try { MusicService.pause(); } catch (Exception ignored) {}
        // 广播暂停事件
        com.roguelike.core.GameEvent.post(new com.roguelike.core.GameEvent(com.roguelike.core.GameEvent.Type.GAME_PAUSED));
        // 冻结全局计时
        com.roguelike.core.TimeService.setPaused(true);
        // 暂停游戏逻辑可在此扩展
        Button resume = new Button("继续");
        resume.setOnAction(e -> {
            hideAll();
            // 恢复音乐
            try { MusicService.resume(); } catch (Exception ignored) {}
            // 广播恢复事件
            com.roguelike.core.GameEvent.post(new com.roguelike.core.GameEvent(com.roguelike.core.GameEvent.Type.GAME_RESUMED));
            // 解除全局计时冻结
            com.roguelike.core.TimeService.setPaused(false);
            // 恢复游戏逻辑可在此扩展
            if (onResume != null) onResume.run();
        });
        Button back = new Button("返回主菜单");
        back.setOnAction(e -> {
            hideAll();
            try { MusicService.playLobby(); } catch (Exception ignored) {}
            // 回主菜单：重启游戏（FXGL简化处理）
            FXGL.getGameController().gotoMainMenu();
        });
        HBox box = new HBox(10, resume, back);
        container.getChildren().setAll(box);
    }

    public static void showGameOver() {
        ensure();
        Button back = new Button("重新开始");
        back.setOnAction(e -> {
            // 返回主菜单：切回大厅音乐
            try { MusicService.playLobby(); } catch (Exception ignored) {}
            FXGL.getGameController().startNewGame();
        });
        container.getChildren().setAll(back);
    }

    public static void hideAll() {
        ensure();
        container.getChildren().clear();
    }
}


