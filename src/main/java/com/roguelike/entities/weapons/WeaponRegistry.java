package com.roguelike.entities.weapons;

import java.util.EnumMap;
import java.util.Map;

/**
 * 武器注册表：定义每种武器的最大等级（后续可扩展更多配置）。
 */
public class WeaponRegistry {
    private static final Map<WeaponId, WeaponSpec> REG = new EnumMap<>(WeaponId.class);

    static {
        // 初始默认：8 种武器，统一最大 5 级（可按你的要求后续分别调整）
        register(new WeaponSpec(WeaponId.W01, 5));
        register(new WeaponSpec(WeaponId.W02, 5));
        register(new WeaponSpec(WeaponId.W03, 5));
        register(new WeaponSpec(WeaponId.W04, 5));
        register(new WeaponSpec(WeaponId.W05, 5));
        register(new WeaponSpec(WeaponId.W06, 5));
        register(new WeaponSpec(WeaponId.W07, 5));
        register(new WeaponSpec(WeaponId.W08, 5));
    }

    public static void register(WeaponSpec spec) {
        if (spec == null) return;
        REG.put(spec.getId(), spec);
    }

    public static WeaponSpec get(WeaponId id) {
        return REG.get(id);
    }
}



