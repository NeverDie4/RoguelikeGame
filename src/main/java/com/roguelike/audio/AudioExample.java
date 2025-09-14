package com.roguelike.audio;

/**
 * 音频使用示例类
 * 展示如何在游戏中使用AudioManager
 */
public class AudioExample {
    
    private static AudioManager audioManager = AudioManager.getInstance();
    
    /**
     * 示例：播放背景音乐
     */
    public static void playBackgroundMusicExample() {
        // 播放背景音乐（需要音频文件在assets/music/目录下）
        audioManager.playBackgroundMusic("music/background_music.mp3");
    }
    
    /**
     * 示例：播放音效
     */
    public static void playSoundEffectExample() {
        // 播放音效（需要音频文件在assets/sounds/目录下）
        audioManager.playSoundEffect("sounds/button_click.wav");
        audioManager.playSoundEffect("sounds/player_move.wav");
        audioManager.playSoundEffect("sounds/enemy_hit.wav");
    }
    
    /**
     * 示例：调整音量
     */
    public static void adjustVolumeExample() {
        // 设置主音量为80%
        audioManager.setMasterVolume(0.8);
        
        // 设置音效音量为60%
        audioManager.setSoundEffectsVolume(0.6);
        
        // 设置背景音乐音量为70%
        audioManager.setMusicVolume(0.7);
    }
    
    /**
     * 示例：控制音频开关
     */
    public static void controlAudioExample() {
        // 禁用音效
        audioManager.setSoundEffectsEnabled(false);
        
        // 启用背景音乐
        audioManager.setMusicEnabled(true);
        
        // 暂停背景音乐
        audioManager.pauseBackgroundMusic();
        
        // 恢复背景音乐
        audioManager.resumeBackgroundMusic();
    }
    
    /**
     * 示例：获取当前音频设置
     */
    public static void getAudioSettingsExample() {
        System.out.println("当前音频设置:");
        System.out.println("主音量: " + audioManager.getMasterVolumePercent() + "%");
        System.out.println("音效音量: " + audioManager.getSoundEffectsVolumePercent() + "%");
        System.out.println("背景音乐音量: " + audioManager.getMusicVolumePercent() + "%");
        System.out.println("音效启用: " + audioManager.isSoundEffectsEnabled());
        System.out.println("背景音乐启用: " + audioManager.isMusicEnabled());
    }
}

