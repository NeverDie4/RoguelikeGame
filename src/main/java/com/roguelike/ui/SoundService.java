package com.roguelike.ui;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 轻量音效服务：一次性播放短音效（射击/爆炸等），使用类路径 assets/sounds/ 下的资源。
 * - 使用 AudioClip，低延迟、适合 SFX
 * - 简单缓存，避免重复IO
 * - 不参与暂停/恢复（与BGM解耦），始终一次性播放一遍
 */
public final class SoundService {
    private static final Map<String, AudioClip> CACHE = new HashMap<>();
    private static double volume = 1.0;
    private static String FIRE_SFX = "bullets/shoot.mp3";
    private static String EXPLOSION_SFX = "explosions/explosion.mp3";
    private static String BOUNCE_SFX = "bounces/bounce.mp3";

    private SoundService() {}

    public static void setVolume(double v) {
        volume = Math.max(0.0, Math.min(1.0, v));
    }

    public static void setFireSfx(String relativePath) {
        if (relativePath != null && !relativePath.isEmpty()) FIRE_SFX = relativePath;
    }

    public static void setExplosionSfx(String relativePath) {
        if (relativePath != null && !relativePath.isEmpty()) EXPLOSION_SFX = relativePath;
    }

    public static void setBounceSfx(String relativePath) {
        if (relativePath != null && !relativePath.isEmpty()) BOUNCE_SFX = relativePath;
    }

    public static void playFire() { playInternal(FIRE_SFX); }

    public static void playExplosion() { playInternal(EXPLOSION_SFX); }

    public static void playBounce() { playInternal(BOUNCE_SFX); }

    public static void playOnce(String relativePath) {
        playInternal(relativePath);
    }

    private static boolean playInternal(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return false;
        try {
            AudioClip clip = CACHE.get(relativePath);
            if (clip == null) {
                URL url = findResourceUrl(relativePath);
                if (url == null) {
                    if (!CACHE.containsKey(relativePath)) {
                        System.out.println("[SoundService] SFX not found in classpath for: " + relativePath);
                        CACHE.put(relativePath, null);
                    }
                    return false;
                }
                clip = new AudioClip(url.toExternalForm());
                clip.setCycleCount(1);
                CACHE.put(relativePath, clip);
            }
            if (clip != null) {
                clip.stop();
                clip.setVolume(volume);
                clip.play();
                return true;
            }
        } catch (Exception ignored) { return false; }
        return false;
    }

    private static URL findResourceUrl(String relativePath) {
        String[] candidates = new String[] {
                "assets/sounds/" + relativePath,
                "sounds/" + relativePath,
                relativePath
        };
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String p : candidates) {
            URL u = cl.getResource(p);
            if (u == null) {
                u = SoundService.class.getResource("/" + p);
            }
            if (u == null) {
                u = ClassLoader.getSystemResource(p);
            }
            if (u != null) return u;
        }
        return null;
    }
}


