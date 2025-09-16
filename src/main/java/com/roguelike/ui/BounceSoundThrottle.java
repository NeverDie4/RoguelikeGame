package com.roguelike.ui;

/**
 * 反弹音效节流管理器
 * 防止短时间内多个反弹音效重叠播放，造成噪音
 */
public final class BounceSoundThrottle {
    private static final double THROTTLE_INTERVAL = 0.08; // 80毫秒内只播放一次
    private static double lastBounceSoundTime = 0.0;
    
    private BounceSoundThrottle() {}
    
    /**
     * 尝试播放反弹音效，如果距离上次播放时间太短则跳过
     * @return true表示播放了音效，false表示被节流跳过
     */
    public static boolean tryPlayBounceSound() {
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        // 如果距离上次播放时间太短，则跳过
        if (currentTime - lastBounceSoundTime < THROTTLE_INTERVAL) {
            return false;
        }
        
        // 播放反弹音效
        try {
            SoundService.playBounce();
            lastBounceSoundTime = currentTime;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
    
    /**
     * 重置节流状态（可选，用于游戏重置时）
     */
    public static void reset() {
        lastBounceSoundTime = 0.0;
    }
}
