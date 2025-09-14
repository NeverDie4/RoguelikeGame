package com.roguelike.core;

/**
 * 受控的全局时间服务：由主循环以限幅 dt 累加，避免首帧或卡顿导致的时间暴增。
 * 支持游戏暂停时时间停止功能。
 */
public final class TimeService {
    private static double accumulatedSeconds = 0.0;
    private static boolean paused = false;
    private static boolean isPaused = false;
    private static boolean isLoading = true; // 新增：加载状态
    private static double pauseStartTime = 0.0;
    private static double totalPausedTime = 0.0;

    private TimeService() {}

    public static void reset() {
        accumulatedSeconds = 0.0;
        isPaused = false;
        isLoading = true; // 重置时设为加载状态
        pauseStartTime = 0.0;
        totalPausedTime = 0.0;
    }

    /**
     * 由游戏主循环在每帧调用；传入的 dt 已做限幅。
     * 如果游戏暂停或正在加载，则不更新时间。
     */
    public static void update(double dtSeconds) {
        if (dtSeconds <= 0) return;
        if (!isPaused && !isLoading && !paused) {
            accumulatedSeconds += dtSeconds;
        }
    }

    /**
     * 暂停游戏时间
     */
    public static void pause() {
        if (!isPaused) {
            isPaused = true;
            paused = true;
            pauseStartTime = accumulatedSeconds;
            System.out.println("游戏时间已暂停，当前时间: " + accumulatedSeconds + "秒");
        }
    }

    /**
     * 恢复游戏时间
     */
    public static void resume() {
        if (isPaused) {
            isPaused = false;
            paused = false;
            double pausedDuration = accumulatedSeconds - pauseStartTime;
            totalPausedTime += pausedDuration;
            System.out.println("游戏时间已恢复，暂停时长: " + pausedDuration + "秒，总暂停时长: " + totalPausedTime + "秒");
        }
    }

    /**
     * 检查游戏是否暂停
     */
    public static boolean isPaused() {
        return isPaused;
    }

    /**
     * 开始游戏（结束加载状态）
     */
    public static void startGame() {
        isLoading = false;
        System.out.println("游戏开始，时间开始计算");
    }

    /**
     * 检查是否正在加载
     */
    public static boolean isLoading() {
        return isLoading;
    }

    /**
     * 获取游戏运行时间（不包括暂停时间）
     */
    public static double getSeconds() {
        return accumulatedSeconds;
    }

    /**
     * 获取总暂停时间
     */
    public static double getTotalPausedTime() {
        return totalPausedTime;
        }
    public static void setPaused(boolean value) {
        paused = value;
    }

    /**
     * 获取实际游戏时间（包括当前暂停时间）
     */
    public static double getActualSeconds() {
        return accumulatedSeconds + (isPaused ? (accumulatedSeconds - pauseStartTime) : 0);
    }
}


