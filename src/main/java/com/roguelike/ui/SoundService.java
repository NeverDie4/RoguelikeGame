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
    // 全局主音量系数（0.0~1.0）
    private static double masterVolume = 1.0;
    // 音效分类音量系数（0.0~1.0）
    private static double sfxVolume = 1.0;
    // 启用/禁用总开关
    private static boolean enabled = true;
    private static String FIRE_SFX = "bullets/shoot.mp3";
    private static String EXPLOSION_SFX = "explosions/explosion.mp3";
    private static String BOUNCE_SFX = "bounces/bounce.mp3";

    private SoundService() {}

    // 兼容旧接口：将传入值作为主音量设置
    public static void setVolume(double v) { setMasterVolume(v); }

    public static void setMasterVolume(double v) {
        masterVolume = Math.max(0.0, Math.min(1.0, v));
    }

    public static void setSfxVolume(double v) {
        sfxVolume = Math.max(0.0, Math.min(1.0, v));
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    private static double effectiveVolume() {
        double v = masterVolume * sfxVolume;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
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

    public static void playFire() { playInternal(FIRE_SFX, 1.2); } // 调高发射音效音量

    public static void playExplosion() { playInternal(EXPLOSION_SFX, 1.0); }

    public static void playBounce() { playInternal(BOUNCE_SFX, 1.0); }

    public static void playOnce(String relativePath) {
        playInternal(relativePath, 1.0);
    }

    /**
     * 播放一次性音效，带增益（相对音量系数）。
     * 最终音量 = masterVolume * sfxVolume * gain，自动夹在[0,1]。
     */
    public static void playOnce(String relativePath, double gain) {
        double g = Double.isFinite(gain) ? gain : 1.0;
        if (g <= 0) g = 0;
        playInternal(relativePath, g);
    }

    private static boolean playInternal(String relativePath, double gain) {
        if (relativePath == null || relativePath.isEmpty()) return false;
        if (!enabled) return false;
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
                double v = effectiveVolume() * Math.max(0.0, Math.min(4.0, gain));
                if (v > 1.0) v = 1.0; // AudioClip.setVolume 最大 1.0
                clip.setVolume(v);
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


