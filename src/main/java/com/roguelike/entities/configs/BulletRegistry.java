package com.roguelike.entities.configs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 子弹配置注册表。可后续扩展为从 JSON/外部文件加载。
 */
public class BulletRegistry {

    private static final Map<String, BulletSpec> REGISTRY = new HashMap<>();

    static {
        // 01 直线弹（10帧）
        register(new BulletSpec(
                "straight_01",
                "Straight Bullet 01",
                BulletSpec.BulletType.STRAIGHT,
                10,
                false,
                250.0,
                8.0,
                16, 16,
                0,
                "bullets/01",
                10,
                0.05
        ));

        // 02 直线弹（4帧）
        register(new BulletSpec(
                "straight_02",
                "Straight Bullet 02",
                BulletSpec.BulletType.STRAIGHT,
                8,
                false,
                280.0,
                8.0,
                16, 16,
                0.0,
                "bullets/02",
                4,
                0.07
        ));

        // 03 直线弹（10帧）
        register(new BulletSpec(
                "straight_03",
                "Straight Bullet 03",
                BulletSpec.BulletType.STRAIGHT,
                12,
                false,
                320.0,
                8.0,
                16, 16,
                0.0,
                "bullets/03",
                10,
                0.05
        ));

        // 04 直线弹（10帧）
        register(new BulletSpec(
                "straight_04",
                "Straight Bullet 04",
                BulletSpec.BulletType.STRAIGHT,
                15,
                true,
                300.0,
                8.0,
                16, 16,
                0.0,
                "bullets/04",
                10,
                0.05
        ));

        // 05 直线弹（10帧）
        register(new BulletSpec(
                "straight_05",
                "Straight Bullet 05",
                BulletSpec.BulletType.STRAIGHT,
                18,
                false,
                350.0,
                8.0,
                16, 16,
                0.0,
                "bullets/05",
                10,
                0.05
        ));

        // 06 直线弹（10帧）
        register(new BulletSpec(
                "straight_06",
                "Straight Bullet 06",
                BulletSpec.BulletType.STRAIGHT,
                20,
                true,
                400.0,
                8.0,
                16, 16,
                0.0,
                "bullets/06",
                10,
                0.05
        ));

        // 07 直线弹（10帧）
        register(new BulletSpec(
                "straight_07",
                "Straight Bullet 07",
                BulletSpec.BulletType.STRAIGHT,
                25,
                false,
                450.0,
                8.0,
                16, 16,
                0.0,
                "bullets/07",
                10,
                0.05
        ));
    }

    public static void register(BulletSpec spec) {
        if (spec == null || spec.getId() == null) return;
        REGISTRY.put(spec.getId(), spec);
    }

    public static BulletSpec get(String id) {
        return REGISTRY.get(id);
    }

    public static Map<String, BulletSpec> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}


