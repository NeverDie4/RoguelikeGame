package com.roguelike.entities.configs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 攻击配置注册表。
 */
public class AttackRegistry {

    private static final Map<String, AttackSpec> REGISTRY = new HashMap<>();

    static {
        // 单发直射（使用 01 直线弹）
        register(new AttackSpec(
                "single_01",
                "Single Shot 01",
                "straight_01",
                0.5,
                1,
                0.0
        ));

        // 单发直射（使用 02 直线弹）
        register(new AttackSpec(
                "single_02",
                "Single Shot 02",
                "straight_02",
                0.5,
                1,
                0.0
        ));

        // 单发直射（使用 03 直线弹）
        register(new AttackSpec(
                "single_03",
                "Single Shot 03",
                "straight_03",
                0.5,
                1,
                0.0
        ));

        // 单发直射（使用 04 直线弹）
        register(new AttackSpec(
                "single_04",
                "Single Shot 04",
                "straight_04",
                0.5,
                1,
                0.0
        ));

        // 单发直射（使用 05 直线弹）
        register(new AttackSpec(
                "single_05",
                "Single Shot 05",
                "straight_05",
                0.5,
                1,
                0.0
        ));

        // 单发直射（使用 06 直线弹）
        register(new AttackSpec(
                "single_06",
                "Single Shot 06",
                "straight_06",
                0.5,
                1,
                0.0
        ));

        // 单发直射（使用 07 直线弹）
        register(new AttackSpec(
                "single_07",
                "Single Shot 07",
                "straight_07",
                0.5,
                1,
                0.0
        ));
    }

    public static void register(AttackSpec spec) {
        if (spec == null || spec.getId() == null) return;
        REGISTRY.put(spec.getId(), spec);
    }

    public static AttackSpec get(String id) { return REGISTRY.get(id); }

    public static Map<String, AttackSpec> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}


