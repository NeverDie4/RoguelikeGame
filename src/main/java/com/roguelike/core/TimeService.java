package com.roguelike.core;

/**
 * 受控的全局时间服务：由主循环以限幅 dt 累加，避免首帧或卡顿导致的时间暴增。
 */
public final class TimeService {
    private static double accumulatedSeconds = 0.0;
    private static boolean paused = false;

    private TimeService() {}

    public static void reset() {
        accumulatedSeconds = 0.0;
    }

    /**
     * 由游戏主循环在每帧调用；传入的 dt 已做限幅。
     */
    public static void update(double dtSeconds) {
        if (dtSeconds <= 0) return;
        if (paused) return;
        accumulatedSeconds += dtSeconds;
    }

    public static double getSeconds() {
        return accumulatedSeconds;
    }

    public static void setPaused(boolean value) {
        paused = value;
    }

    public static boolean isPaused() {
        return paused;
    }
}


