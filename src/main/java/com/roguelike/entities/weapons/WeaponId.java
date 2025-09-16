package com.roguelike.entities.weapons;

/**
 * 武器ID（对应 bullets/01 ~ bullets/08）。
 */
public enum WeaponId {
    W01("straight_01", "焚天炎杖", "焚天炎杖，发射炽热火球，传为上古火神遗留神物。"),
    W02("straight_02", "星陨碎渊", "星陨碎渊，环绕玩家伤敌，来自宇宙深渊的神秘造物。"),
    W03("straight_03", "凛冬之怒", "凛冬之怒，发射冰爆弹，乃远古冰灵族圣物。"),
    W04("straight_04", "焰心轮环", "焰心轮环，环绕伤敌，是炎域古族的守护圣环。"),
    W05("straight_05", "沧澜断空", "沧澜断空，发射沧澜水刃，为上古水神裁决之器。"),
    W06("straight_06", "雷环御霆", "雷环御霆，召雷霆电击，为远古雷神遗留神环。"),
    W07("straight_07", "赤湮劫轮", "赤湮劫轮，发射即爆敌，乃上古灾厄遗落的灭世之器。"),
    W08("straight_08", "寒渊星环", "寒渊星环，发射穿反弹冰环，源自极北永冻秘境。");

    private final String bulletSpecId; // 关联的子弹配置ID（用于后续应用升级时查找）
    private final String displayName;
    private final String loreDescription; // 武器背景故事描述

    WeaponId(String bulletSpecId, String displayName, String loreDescription) {
        this.bulletSpecId = bulletSpecId;
        this.displayName = displayName;
        this.loreDescription = loreDescription;
    }

    public String getBulletSpecId() {
        return bulletSpecId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLoreDescription() {
        return loreDescription;
    }
}



