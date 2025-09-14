package com.roguelike.entities.weapons;

/**
 * 武器规格：包含最大等级与名称等。
 */
public class WeaponSpec {
    private final WeaponId id;
    private final int maxLevel;

    public WeaponSpec(WeaponId id, int maxLevel) {
        this.id = id;
        this.maxLevel = Math.max(1, maxLevel);
    }

    public WeaponId getId() {
        return id;
    }

    public int getMaxLevel() {
        return maxLevel;
    }
}



