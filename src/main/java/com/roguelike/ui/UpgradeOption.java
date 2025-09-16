package com.roguelike.ui;

/**
 * 升级选项模型：可表示武器或被动物品的升级。
 */
public final class UpgradeOption {
    public enum Kind { WEAPON, PASSIVE }

    private final Kind kind;
    private final String weaponIdx2;   // "01".."08"（当 kind=WEAPON）
    private final PassiveId passiveId; // 当 kind=PASSIVE
    private final String title;        // 用ID占位
    private final String desc;         // 升级增量描述
    private final String iconPath;     // 类路径
    private final String statsChange;  // 武器数值变化描述（仅武器升级时使用）

    private UpgradeOption(Kind kind, String weaponIdx2, PassiveId passiveId, String title, String desc, String iconPath, String statsChange) {
        this.kind = kind;
        this.weaponIdx2 = weaponIdx2;
        this.passiveId = passiveId;
        this.title = title;
        this.desc = desc;
        this.iconPath = iconPath;
        this.statsChange = statsChange;
    }

    public static UpgradeOption weapon(String idx2, String desc) {
        String icon = WeaponIconProvider.pathFor(idx2);
        com.roguelike.entities.weapons.WeaponId weaponId = com.roguelike.entities.weapons.WeaponId.valueOf("W" + idx2);
        String displayName = weaponId.getDisplayName();
        return new UpgradeOption(Kind.WEAPON, idx2, null, displayName, desc, icon, null);
    }

    public static UpgradeOption weapon(String idx2, String desc, String statsChange) {
        String icon = WeaponIconProvider.pathFor(idx2);
        com.roguelike.entities.weapons.WeaponId weaponId = com.roguelike.entities.weapons.WeaponId.valueOf("W" + idx2);
        String displayName = weaponId.getDisplayName();
        return new UpgradeOption(Kind.WEAPON, idx2, null, displayName, desc, icon, statsChange);
    }

    public static UpgradeOption passive(PassiveId id, String desc) {
        String icon = PassiveIconProvider.pathFor(id);
        String displayName = PassiveRegistry.get(id).getDisplayName();
        return new UpgradeOption(Kind.PASSIVE, null, id, displayName, desc, icon, null);
    }

    public Kind getKind() { return kind; }
    public String getWeaponIdx2() { return weaponIdx2; }
    public PassiveId getPassiveId() { return passiveId; }
    public String getTitle() { return title; }
    public String getDesc() { return desc; }
    public String getIconPath() { return iconPath; }
    public String getStatsChange() { return statsChange; }
}
