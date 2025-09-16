package com.roguelike.ui;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.URL;

/**
 * 简单的BGM服务：管理大厅与战斗音乐（类路径加载）。
 * 资源路径：src/main/resources/assets/music/lobby.mp3, battle1.mp3
 */
public final class MusicService {

    private static MediaPlayer current;
    private static String currentId = "";
    // 记录期望播放的曲目ID（lobby.mp3 / battle1.wav），在禁用期间也会被更新
    private static String desiredId = "";
    // 全局主音量与音乐分类音量（0.0~1.0）
    private static double masterVolume = 1.0;
    private static double musicVolume = 1.0;
    private static boolean enabled = true;

    private MusicService() {}

    private static void applyVolume(MediaPlayer player) {
        if (player == null) return;
        double v = enabled ? Math.max(0.0, Math.min(1.0, masterVolume * musicVolume)) : 0.0;
        try { player.setVolume(v); } catch (Exception ignored) {}
        try { player.setMute(v <= 0.0); } catch (Exception ignored) {}
    }

    private static void playLoop(String assetId) {
        if (assetId == null || assetId.isEmpty()) return;
        if (assetId.equals(currentId)) return;
        stop();
        Platform.runLater(() -> {
            try {
                String source = null;
                URL url = Thread.currentThread().getContextClassLoader()
                        .getResource("assets/music/" + assetId);
                if (url != null) {
                    source = url.toExternalForm();
                    System.out.println("[MusicService] Using classpath URL: " + source);
                } else {
                    // 开发环境回退：尝试 src/main/resources 与 assets/music 目录
                    File dev1 = new File("src/main/resources/assets/music/" + assetId);
                    File dev2 = new File("assets/music/" + assetId);
                    if (dev1.exists()) {
                        source = dev1.toURI().toString();
                        System.out.println("[MusicService] Using dev file: " + dev1.getAbsolutePath());
                    } else if (dev2.exists()) {
                        source = dev2.toURI().toString();
                        System.out.println("[MusicService] Using dev file: " + dev2.getAbsolutePath());
                    }
                }

                if (source == null) {
                    System.out.println("[MusicService] Music not found: " + assetId +
                            ", tried classpath 'assets/music/' and local dev paths.");
                    return;
                }

                Media media = new Media(source);
                current = new MediaPlayer(media);
                current.setCycleCount(MediaPlayer.INDEFINITE);
                applyVolume(current);
                current.setOnError(() -> {
                    System.out.println("[MusicService] MediaPlayer error: " + current.getError());
                });
                media.setOnError(() -> {
                    System.out.println("[MusicService] Media error: " + media.getError());
                });
                current.setOnReady(() -> {
                    System.out.println("[MusicService] Media ready: " + assetId);
                    applyVolume(current);
                    try { current.play(); } catch (Exception ignored) {}
                });
                // 有些平台无需等待 READY，也尝试直接播放
                applyVolume(current);
                try { current.play(); } catch (Exception ignored) {}
                currentId = assetId;
            } catch (Exception ex) {
                System.out.println("[MusicService] Exception starting music: " + ex.getMessage());
            }
        });
    }

    public static void playLobby() {
        desiredId = "lobby.mp3";
        if (!enabled) { stop(); return; }
        playLoop("lobby.mp3");
    }

    public static void playBattle() {
        desiredId = "battle1.wav";
        if (!enabled) { stop(); return; }
        playLoop("battle1.wav");
    }

    public static void setMasterVolume(double v) {
        masterVolume = Math.max(0.0, Math.min(1.0, v));
        if (current != null) applyVolume(current);
    }

    public static void setMusicVolume(double v) {
        musicVolume = Math.max(0.0, Math.min(1.0, v));
        if (current != null) applyVolume(current);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            stop();
        } else {
            // 优先使用 desiredId 恢复播放；若为空则用 currentId
            String toPlay = (desiredId != null && !desiredId.isEmpty()) ? desiredId : currentId;
            if ("lobby.mp3".equals(toPlay)) playLobby();
            else if ("battle1.wav".equals(toPlay)) playBattle();
        }
    }

    public static void stop() {
        if (current != null) {
            try {
                current.stop();
            } catch (Exception ignored) {}
            try {
                current.dispose();
            } catch (Exception ignored) {}
            current = null;
            currentId = "";
        }
    }

    public static void pause() {
        if (current != null) {
            try { current.pause(); } catch (Exception ignored) {}
        }
    }

    public static void resume() {
        if (current != null) {
            try { current.play(); } catch (Exception ignored) {}
        }
    }
}


