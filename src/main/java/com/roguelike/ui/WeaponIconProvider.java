package com.roguelike.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * 提供武器图标路径（支持 webp 与 png）。
 */
public final class WeaponIconProvider {
    private static final Map<String, String> MAP = new HashMap<>();
    static {
        // 约定：01/06 为 webp；其余为 png
        MAP.put("01", "assets/images/weapon/01.webp");
        MAP.put("02", "assets/images/weapon/02.png");
        MAP.put("03", "assets/images/weapon/03.png");
        MAP.put("04", "assets/images/weapon/04.png");
        MAP.put("05", "assets/images/weapon/05.png");
        MAP.put("06", "assets/images/weapon/06.webp");
        MAP.put("07", "assets/images/weapon/07.png");
        MAP.put("08", "assets/images/weapon/08.png");
    }

    public static String pathFor(String weaponIdx2) {
        return MAP.get(weaponIdx2);
    }
}


