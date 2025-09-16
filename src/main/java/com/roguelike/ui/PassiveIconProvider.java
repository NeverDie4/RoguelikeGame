package com.roguelike.ui;

import java.util.EnumMap;
import java.util.Map;

/**
 * 被动物品图标路径（仅类路径）。
 */
public final class PassiveIconProvider {
    private static final Map<PassiveId, String> MAP = new EnumMap<>(PassiveId.class);
    static {
        MAP.put(PassiveId.P01_ATTACK_SPEED, "assets/images/passive item/p01.webp");
        MAP.put(PassiveId.P02_PROJECTILE_COUNT, "assets/images/passive item/p02.webp");
        MAP.put(PassiveId.P03_COOLDOWN_REDUCE, "assets/images/passive item/p03.webp");
        MAP.put(PassiveId.P04_MAX_HP, "assets/images/passive item/p04.webp");
        MAP.put(PassiveId.P05_DAMAGE_MULTIPLIER, "assets/images/passive item/p05.webp");
        MAP.put(PassiveId.P06_MOVE_SPEED, "assets/images/passive item/p06.webp");
    }

    public static String pathFor(PassiveId id) {
        return MAP.get(id);
    }
}


