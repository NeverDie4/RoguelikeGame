package com.roguelike.entities.configs;

/**
 * 攻击配置（模板）。描述发射节奏与形态参数，不直接包含弹体数值。
 */
public class AttackSpec {

    private final String id;
    private final String displayName;
    private final String bulletSpecId; // 关联的弹体模板

    // 发射节奏
    private final double fireIntervalSeconds; // 开火间隔
    private final int bulletsPerShot; // 每次发射的子弹数量
    private final double spreadAngleDegrees; // 扇形总角度（用于散射）

    public AttackSpec(String id,
                      String displayName,
                      String bulletSpecId,
                      double fireIntervalSeconds,
                      int bulletsPerShot,
                      double spreadAngleDegrees) {
        this.id = id;
        this.displayName = displayName;
        this.bulletSpecId = bulletSpecId;
        this.fireIntervalSeconds = Math.max(0.01, fireIntervalSeconds);
        this.bulletsPerShot = Math.max(1, bulletsPerShot);
        this.spreadAngleDegrees = Math.max(0.0, spreadAngleDegrees);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getBulletSpecId() { return bulletSpecId; }
    public double getFireIntervalSeconds() { return fireIntervalSeconds; }
    public int getBulletsPerShot() { return bulletsPerShot; }
    public double getSpreadAngleDegrees() { return spreadAngleDegrees; }
}


