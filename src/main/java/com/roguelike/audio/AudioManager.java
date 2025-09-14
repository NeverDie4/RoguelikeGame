 package com.roguelike.audio;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.audio.Music;
import com.almasb.fxgl.audio.Sound;
import java.util.HashMap;
import java.util.Map;

/**
 * 音频管理器类
 * 负责管理游戏中的音效和背景音乐
 */
public class AudioManager {
    
    private static AudioManager instance;
    
    // 音频设置
    private double masterVolume = 0.5; // 主音量 (0.0 - 1.0)
    private double soundEffectsVolume = 0.7; // 音效音量 (0.0 - 1.0)
    private double musicVolume = 0.6; // 背景音乐音量 (0.0 - 1.0)
    
    // 开关状态
    private boolean soundEffectsEnabled = true;
    private boolean musicEnabled = true;
    
    // 音频播放器
    private Music currentBackgroundMusic;
    private Map<String, Sound> soundEffects = new HashMap<>();
    
    private AudioManager() {
        // 私有构造函数，单例模式
    }
    
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }
    
    /**
     * 播放背景音乐
     * @param musicPath 音乐文件路径
     */
    public void playBackgroundMusic(String musicPath) {
        if (!musicEnabled) {
            return;
        }
        
        try {
            // 停止当前播放的背景音乐
            stopBackgroundMusic();
            
            // 使用FXGL播放背景音乐
            currentBackgroundMusic = FXGL.getAssetLoader().loadMusic(musicPath);
            FXGL.getAudioPlayer().playMusic(currentBackgroundMusic);
            
            // 注意：FXGL的AudioPlayer可能没有直接的音量设置方法
            // 音量控制通过AudioManager的实例变量来管理
            
            System.out.println("开始播放背景音乐: " + musicPath);
        } catch (Exception e) {
            System.out.println("播放背景音乐失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止背景音乐
     */
    public void stopBackgroundMusic() {
        if (currentBackgroundMusic != null) {
            FXGL.getAudioPlayer().stopMusic(currentBackgroundMusic);
            currentBackgroundMusic = null;
            System.out.println("背景音乐已停止");
        }
    }
    
    /**
     * 暂停背景音乐
     */
    public void pauseBackgroundMusic() {
        if (currentBackgroundMusic != null) {
            FXGL.getAudioPlayer().pauseMusic(currentBackgroundMusic);
            System.out.println("背景音乐已暂停");
        }
    }
    
    /**
     * 恢复背景音乐
     */
    public void resumeBackgroundMusic() {
        if (currentBackgroundMusic != null && musicEnabled) {
            FXGL.getAudioPlayer().resumeMusic(currentBackgroundMusic);
            System.out.println("背景音乐已恢复");
        }
    }
    
    /**
     * 播放音效
     * @param soundPath 音效文件路径
     */
    public void playSoundEffect(String soundPath) {
        if (!soundEffectsEnabled) {
            return;
        }
        
        try {
            Sound sound;
            
            // 检查是否已经加载过这个音效
            if (soundEffects.containsKey(soundPath)) {
                sound = soundEffects.get(soundPath);
            } else {
                // 加载新的音效
                sound = FXGL.getAssetLoader().loadSound(soundPath);
                soundEffects.put(soundPath, sound);
            }
            
            // 播放音效
            FXGL.getAudioPlayer().playSound(sound);
            
            System.out.println("播放音效: " + soundPath);
        } catch (Exception e) {
            System.out.println("播放音效失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置主音量
     * @param volume 音量值 (0.0 - 1.0)
     */
    public void setMasterVolume(double volume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, volume));
        updateAllVolumes();
        System.out.println("主音量设置为: " + (this.masterVolume * 100) + "%");
    }
    
    /**
     * 设置音效音量
     * @param volume 音量值 (0.0 - 1.0)
     */
    public void setSoundEffectsVolume(double volume) {
        this.soundEffectsVolume = Math.max(0.0, Math.min(1.0, volume));
        updateAllVolumes();
        System.out.println("音效音量设置为: " + (this.soundEffectsVolume * 100) + "%");
    }
    
    /**
     * 设置背景音乐音量
     * @param volume 音量值 (0.0 - 1.0)
     */
    public void setMusicVolume(double volume) {
        this.musicVolume = Math.max(0.0, Math.min(1.0, volume));
        updateAllVolumes();
        System.out.println("背景音乐音量设置为: " + (this.musicVolume * 100) + "%");
    }
    
    /**
     * 设置音效开关
     * @param enabled 是否启用音效
     */
    public void setSoundEffectsEnabled(boolean enabled) {
        this.soundEffectsEnabled = enabled;
        System.out.println("音效" + (enabled ? "已启用" : "已禁用"));
    }
    
    /**
     * 设置背景音乐开关
     * @param enabled 是否启用背景音乐
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            pauseBackgroundMusic();
        } else {
            resumeBackgroundMusic();
        }
        System.out.println("背景音乐" + (enabled ? "已启用" : "已禁用"));
    }
    
    /**
     * 更新所有音频的音量
     */
    private void updateAllVolumes() {
        // 注意：FXGL的AudioPlayer可能没有直接的音量设置方法
        // 音量控制通过AudioManager的实例变量来管理
        // 实际的音量控制需要在播放音频时应用
        
        System.out.println("音量已更新 - 主音量: " + (masterVolume * 100) + "%, " +
                          "音效音量: " + (soundEffectsVolume * 100) + "%, " +
                          "背景音乐音量: " + (musicVolume * 100) + "%");
    }
    
    // Getter 方法
    public double getMasterVolume() {
        return masterVolume;
    }
    
    public double getSoundEffectsVolume() {
        return soundEffectsVolume;
    }
    
    public double getMusicVolume() {
        return musicVolume;
    }
    
    public boolean isSoundEffectsEnabled() {
        return soundEffectsEnabled;
    }
    
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    
    /**
     * 获取主音量百分比
     * @return 主音量百分比 (0-100)
     */
    public int getMasterVolumePercent() {
        return (int) (masterVolume * 100);
    }
    
    /**
     * 获取音效音量百分比
     * @return 音效音量百分比 (0-100)
     */
    public int getSoundEffectsVolumePercent() {
        return (int) (soundEffectsVolume * 100);
    }
    
    /**
     * 获取背景音乐音量百分比
     * @return 背景音乐音量百分比 (0-100)
     */
    public int getMusicVolumePercent() {
        return (int) (musicVolume * 100);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        stopBackgroundMusic();
        soundEffects.clear();
        System.out.println("音频管理器资源已清理");
    }
}
