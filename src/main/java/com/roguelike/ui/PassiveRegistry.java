package com.roguelike.ui;

import java.util.EnumMap;
import java.util.Map;

public final class PassiveRegistry {
    private static final Map<PassiveId, PassiveItemSpec> MAP = new EnumMap<>(PassiveId.class);

    static {
        // 01 攻速（倍率）
        register(new PassiveItemSpec(
                PassiveId.P01_ATTACK_SPEED,
                "迅腕疾锋",
                "assets/images/passive item/p01.png",
                new double[]{1.05, 1.10, 1.16, 1.22, 1.30}
        ));
        // 02 数量（+发射数）
        register(new PassiveItemSpec(
                PassiveId.P02_PROJECTILE_COUNT,
                "双孪宝镯",
                "assets/images/passive item/p02.png",
                new double[]{1, 1, 2, 2, 3}
        ));
        // 03 冷却缩放（倍率越小越快）
        register(new PassiveItemSpec(
                PassiveId.P03_COOLDOWN_REDUCE,
                "瞬元圣典",
                "assets/images/passive item/p03.png",
                new double[]{0.97, 0.94, 0.90, 0.86, 0.82}
        ));
        // 04 HP 上限（加值）
        register(new PassiveItemSpec(
                PassiveId.P04_MAX_HP,
                "黯御护心",
                "assets/images/passive item/p04.png",
                new double[]{10, 20, 35, 55, 80}
        ));
        // 05 伤害（倍率）
        register(new PassiveItemSpec(
                PassiveId.P05_DAMAGE_MULTIPLIER,
                "翠羽锐芒",
                "assets/images/passive item/p05.png",
                new double[]{1.05, 1.10, 1.16, 1.22, 1.30}
        ));
        // 06 移速（倍率）
        register(new PassiveItemSpec(
                PassiveId.P06_MOVE_SPEED,
                "流光疾翼",
                "assets/images/passive item/p06.png",
                new double[]{1.05, 1.10, 1.15, 1.20, 1.25}
        ));
    }

    public static void register(PassiveItemSpec spec) {
        if (spec != null) MAP.put(spec.getId(), spec);
    }

    public static PassiveItemSpec get(PassiveId id) {
        return MAP.get(id);
    }
}


