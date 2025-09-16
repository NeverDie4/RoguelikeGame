package com.roguelike.ui;

/**
 * 爆炸音效节流管理器
 * 防止短时间内多个爆炸音效重叠播放，造成噪音
 */
public final class ExplosionSoundThrottle {
    private static final double THROTTLE_INTERVAL = 0.2; // 200毫秒内只播放一次
    private static double lastExplosionSoundTime = 0.0;
    
    private ExplosionSoundThrottle() {}
    
    /**
     * 尝试播放爆炸音效，如果距离上次播放时间太短则跳过
     * @return true表示播放了音效，false表示被节流跳过
     */
    public static boolean tryPlayExplosionSound() {
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        // 如果距离上次播放时间太短，则跳过
        if (currentTime - lastExplosionSoundTime < THROTTLE_INTERVAL) {
            return false;
        }
        
        // 播放爆炸音效
        try {
            SoundService.playExplosion();
            lastExplosionSoundTime = currentTime;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
    
    /**
     * 重置节流状态（可选，用于游戏重置时）
     */
    public static void reset() {
        lastExplosionSoundTime = 0.0;
    }
}
