package com.roguelike.ui;

import com.roguelike.core.GameEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * 被动物品管理：记录每个被动的等级（0-5），并提供聚合加成查询。
 */
public final class PassiveItemManager {
    private final Map<PassiveId, Integer> levelMap = new EnumMap<>(PassiveId.class);

    public PassiveItemManager() {
        for (PassiveId id : PassiveId.values()) levelMap.put(id, 0);
    }

    public int getLevel(PassiveId id) { return levelMap.getOrDefault(id, 0); }
    public void setLevel(PassiveId id, int level) {
        int lv = Math.max(0, Math.min(5, level));
        levelMap.put(id, lv);
        GameEvent.post(new GameEvent(GameEvent.Type.PASSIVE_UPGRADED));
    }
    public void acquire(PassiveId id) {
        if (getLevel(id) == 0) {
            levelMap.put(id, 1);
            GameEvent.post(new GameEvent(GameEvent.Type.PASSIVE_ACQUIRED, id));
        }
    }

    // 聚合加成接口
    public double getAttackSpeedMultiplier() {
        int lv = getLevel(PassiveId.P01_ATTACK_SPEED);
        return lv > 0 ? PassiveRegistry.get(PassiveId.P01_ATTACK_SPEED).getValueAtLevel(lv) : 1.0;
    }
    public int getAdditionalProjectiles() {
        int lv = getLevel(PassiveId.P02_PROJECTILE_COUNT);
        return lv > 0 ? (int) PassiveRegistry.get(PassiveId.P02_PROJECTILE_COUNT).getValueAtLevel(lv) : 0;
    }
    public double getCooldownScale() {
        int lv = getLevel(PassiveId.P03_COOLDOWN_REDUCE);
        return lv > 0 ? PassiveRegistry.get(PassiveId.P03_COOLDOWN_REDUCE).getValueAtLevel(lv) : 1.0;
    }
    public int getMaxHpBonus() {
        int lv = getLevel(PassiveId.P04_MAX_HP);
        return lv > 0 ? (int) PassiveRegistry.get(PassiveId.P04_MAX_HP).getValueAtLevel(lv) : 0;
    }
    public double getDamageMultiplier() {
        int lv = getLevel(PassiveId.P05_DAMAGE_MULTIPLIER);
        return lv > 0 ? PassiveRegistry.get(PassiveId.P05_DAMAGE_MULTIPLIER).getValueAtLevel(lv) : 1.0;
    }
    public double getMoveSpeedMultiplier() {
        int lv = getLevel(PassiveId.P06_MOVE_SPEED);
        return lv > 0 ? PassiveRegistry.get(PassiveId.P06_MOVE_SPEED).getValueAtLevel(lv) : 1.0;
    }
}


