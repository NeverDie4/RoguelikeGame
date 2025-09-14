package com.roguelike.entities.weapons;

/**
 * 武器ID（对应 bullets/01 ~ bullets/08）。
 */
public enum WeaponId {
    W01("straight_01", "Weapon 01"),
    W02("straight_02", "Weapon 02"),
    W03("straight_03", "Weapon 03"),
    W04("straight_04", "Weapon 04"),
    W05("straight_05", "Weapon 05"),
    W06("straight_06", "Weapon 06"),
    W07("straight_07", "Weapon 07"),
    W08("straight_08", "Weapon 08");

    private final String bulletSpecId; // 关联的子弹配置ID（用于后续应用升级时查找）
    private final String displayName;

    WeaponId(String bulletSpecId, String displayName) {
        this.bulletSpecId = bulletSpecId;
        this.displayName = displayName;
    }

    public String getBulletSpecId() {
        return bulletSpecId;
    }

    public String getDisplayName() {
        return displayName;
    }
}



