package com.roguelike.utils;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtils {
    private RandomUtils() {}

    public static int nextInt(int min, int max) {
        if (max < min) throw new IllegalArgumentException("max < min");
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static boolean nextBool(double chance) {
        if (chance <= 0) return false;
        if (chance >= 1) return true;
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public static boolean chance(double probability) {
        return nextBool(probability);
    }
}


