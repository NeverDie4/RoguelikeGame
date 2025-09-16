package com.roguelike.ui;

/**
 * 武器发射音效节流管理器
 * 防止短时间内多个武器发射音效重叠播放，造成噪音
 */
public final class FireSoundThrottle {
    private static final double THROTTLE_INTERVAL = 0.15; // 150毫秒内只播放一次
    private static double lastFireSoundTime = 0.0;
    
    private FireSoundThrottle() {}
    
    /**
     * 尝试播放武器发射音效，如果距离上次播放时间太短则跳过
     * @return true表示播放了音效，false表示被节流跳过
     */
    public static boolean tryPlayFireSound() {
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        // 如果距离上次播放时间太短，则跳过
        if (currentTime - lastFireSoundTime < THROTTLE_INTERVAL) {
            return false;
        }
        
        // 播放武器发射音效
        try {
            SoundService.playFire();
            lastFireSoundTime = currentTime;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
    
    /**
     * 重置节流状态（可选，用于游戏重置时）
     */
    public static void reset() {
        lastFireSoundTime = 0.0;
    }
}
