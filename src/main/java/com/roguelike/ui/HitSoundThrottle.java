package com.roguelike.ui;

/**
 * 受击音效节流管理器
 * 防止短时间内多个敌人受击音效重叠播放，造成噪音
 */
public final class HitSoundThrottle {
    private static final double THROTTLE_INTERVAL = 0.1; // 100毫秒内只播放一次
    private static double lastHitSoundTime = 0.0;
    
    private HitSoundThrottle() {}
    
    /**
     * 尝试播放受击音效，如果距离上次播放时间太短则跳过
     * @return true表示播放了音效，false表示被节流跳过
     */
    public static boolean tryPlayHitSound() {
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        // 如果距离上次播放时间太短，则跳过
        if (currentTime - lastHitSoundTime < THROTTLE_INTERVAL) {
            return false;
        }
        
        // 播放受击音效
        try {
            com.roguelike.ui.SoundService.playOnce("hits/hit.mp3", 3.0);
            lastHitSoundTime = currentTime;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
    
    /**
     * 重置节流状态（可选，用于游戏重置时）
     */
    public static void reset() {
        lastHitSoundTime = 0.0;
    }
}
