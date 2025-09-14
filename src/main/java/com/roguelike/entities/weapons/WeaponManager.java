package com.roguelike.entities.weapons;

import com.roguelike.core.GameEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 管理武器等级与升级逻辑。监听 LEVEL_UP 事件，随机升级一个未满级的武器。
 */
public class WeaponManager {
    private final Map<WeaponId, Integer> weaponLevel = new EnumMap<>(WeaponId.class);
    private final Random random = new Random();

    public WeaponManager() {
        // 初始化：全部 0 级（未获得），仅 01 武器为 1 级
        for (WeaponId id : WeaponId.values()) {
            weaponLevel.put(id, 0);
        }
        weaponLevel.put(WeaponId.W01, 1);

        // 应用初始（LV1）配置，仅武器01
        try { applyWeapon01(1); } catch (Exception ignored) {}

        // 监听主角升级事件
        GameEvent.listen(GameEvent.Type.LEVEL_UP, e -> upgradeRandomWeapon());
    }

    public int getLevel(WeaponId id) {
        return weaponLevel.getOrDefault(id, 1);
    }

    public int getMaxLevel(WeaponId id) {
        WeaponSpec spec = WeaponRegistry.get(id);
        return spec != null ? spec.getMaxLevel() : 5;
    }

    public boolean isMaxed(WeaponId id) {
        return getLevel(id) >= getMaxLevel(id);
    }

    /**
     * 升级一个未满级武器；如果全部满级，则不做任何事。
     */
    public void upgradeRandomWeapon() {
        // 优先级调整：先 04（光环） -> 然后 08 -> 07 -> 06 -> 05 -> 03 -> 02 -> 01
        if (!isMaxed(WeaponId.W04)) {
            int lvl = Math.min(getLevel(WeaponId.W04) + 1, getMaxLevel(WeaponId.W04));
            weaponLevel.put(WeaponId.W04, lvl);
            applyUpgradeEffect(WeaponId.W04, lvl);
            return;
        }
        // 其余按原先顺序（08 -> 07 -> 06 -> 05 -> 03 -> 02 -> 01）
        if (!isMaxed(WeaponId.W08)) {
            int lvl = Math.min(getLevel(WeaponId.W08) + 1, getMaxLevel(WeaponId.W08));
            weaponLevel.put(WeaponId.W08, lvl);
            applyUpgradeEffect(WeaponId.W08, lvl);
            return;
        }
        if (!isMaxed(WeaponId.W07)) {
            int lvl = Math.min(getLevel(WeaponId.W07) + 1, getMaxLevel(WeaponId.W07));
            weaponLevel.put(WeaponId.W07, lvl);
            applyUpgradeEffect(WeaponId.W07, lvl);
            return;
        }
        if (!isMaxed(WeaponId.W06)) {
            int lvl = Math.min(getLevel(WeaponId.W06) + 1, getMaxLevel(WeaponId.W06));
            weaponLevel.put(WeaponId.W06, lvl);
            applyUpgradeEffect(WeaponId.W06, lvl);
            return;
        }
        if (!isMaxed(WeaponId.W05)) {
            int lvl = Math.min(getLevel(WeaponId.W05) + 1, getMaxLevel(WeaponId.W05));
            weaponLevel.put(WeaponId.W05, lvl);
            applyUpgradeEffect(WeaponId.W05, lvl);
            return;
        }
        if (!isMaxed(WeaponId.W03)) {
            int lvl = Math.min(getLevel(WeaponId.W03) + 1, getMaxLevel(WeaponId.W03));
            weaponLevel.put(WeaponId.W03, lvl);
            applyUpgradeEffect(WeaponId.W03, lvl);
            return;
        }
        if (!isMaxed(WeaponId.W02)) {
            int lvl = Math.min(getLevel(WeaponId.W02) + 1, getMaxLevel(WeaponId.W02));
            weaponLevel.put(WeaponId.W02, lvl);
            applyUpgradeEffect(WeaponId.W02, lvl);
            return;
        }
        if (!isMaxed(WeaponId.W01)) {
            int lvl = Math.min(getLevel(WeaponId.W01) + 1, getMaxLevel(WeaponId.W01));
            weaponLevel.put(WeaponId.W01, lvl);
            applyUpgradeEffect(WeaponId.W01, lvl);
            return;
        }

        List<WeaponId> candidates = new ArrayList<>();
        for (WeaponId id : WeaponId.values()) {
            if (!isMaxed(id)) candidates.add(id);
        }
        if (candidates.isEmpty()) return;

        WeaponId chosen = candidates.get(random.nextInt(candidates.size()));
        int newLevel = Math.min(getLevel(chosen) + 1, getMaxLevel(chosen));
        weaponLevel.put(chosen, newLevel);
        applyUpgradeEffect(chosen, newLevel);
    }

    /**
     * 应用具体的武器升级效果（占位）。
     * 你告诉我每个武器在等级 2/3/4/5 应该发生的变化后，我在这里实现。
     */
    private void applyUpgradeEffect(WeaponId id, int level) {
        // 先实现 01 武器（普通火球）
        if (id == WeaponId.W01) {
            applyWeapon01(level);
            return;
        }
        if (id == WeaponId.W02) {
            applyWeapon02(level);
            return;
        }
        if (id == WeaponId.W03) {
            applyWeapon03(level);
            return;
        }
        if (id == WeaponId.W04) {
            applyWeapon04(level);
            return;
        }
        if (id == WeaponId.W05) {
            applyWeapon05(level);
            return;
        }
        if (id == WeaponId.W06) {
            applyWeapon06(level);
            return;
        }
        if (id == WeaponId.W07) {
            applyWeapon07(level);
            return;
        }
        if (id == WeaponId.W08) {
            applyWeapon08(level);
            return;
        }
        // 其他武器等待你的规则后实现
    }

    /**
     * 武器01升级规则：
     * LV1: 伤害15，方向：右，间隔1.0s
     * LV2: 伤害20，方向：左右，间隔0.88s
     * LV3: 伤害30，方向：上下左右，间隔0.8s
     * LV4: 伤害40，方向：八方向，间隔0.73s
     * LV5: 伤害56，方向：八方向，间隔0.7s
     */
    private void applyWeapon01(int level) {
        // 更新 BulletSpec（伤害）
        com.roguelike.entities.configs.BulletSpec b = com.roguelike.entities.configs.BulletRegistry.get("straight_01");
        if (b != null) {
            int newDmg = switch (Math.max(1, Math.min(level, 5))) {
                case 1 -> 15;
                case 2 -> 20;
                case 3 -> 30;
                case 4 -> 40;
                default -> 56;
            };
            // 通过重新注册一个新实例来“更新”配置
            com.roguelike.entities.configs.BulletRegistry.register(new com.roguelike.entities.configs.BulletSpec(
                    b.getId(), b.getDisplayName(), b.getBulletType(),
                    newDmg, b.isPiercing(), b.getBaseSpeed(),
                    b.getLifetimeSeconds(), b.getWidth(), b.getHeight(), b.getRadius(),
                    b.getAnimationBasePath(), b.getFrameCount(), b.getFrameDuration(),
                    b.getVisualScale()
            ));
        }

        // 更新 AttackSpec（间隔、发射方向）
        double interval = switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 1.0;
            case 2 -> 0.88;
            case 3 -> 0.8;
            case 4 -> 0.73;
            default -> 0.7;
        };

        // 我们用 bulletsPerShot + spreadAngle=0，配合自定义策略控制“固定方向集合”
        // 为避免全局影响，注册/覆盖专用 AttackSpec ID：weapon01
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon01", "Weapon 01", "straight_01", interval,
                1, 0.0
        ));

        // 保存本级的方向集合到静态处，供策略读取
        setWeapon01Directions(level);

        // 同步注册武器01的 AttackSpec（供 AutoFire 使用）
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon01", "Weapon 01", "straight_01", interval,
                1, 0.0
        ));
    }

    private static java.util.List<javafx.geometry.Point2D> weapon01Dirs = java.util.List.of(new javafx.geometry.Point2D(1, 0));

    private void setWeapon01Directions(int level) {
        switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> weapon01Dirs = java.util.List.of(new javafx.geometry.Point2D(1, 0));
            case 2 -> weapon01Dirs = java.util.List.of(new javafx.geometry.Point2D(1, 0), new javafx.geometry.Point2D(-1, 0));
            case 3 -> weapon01Dirs = java.util.List.of(
                    new javafx.geometry.Point2D(1, 0), new javafx.geometry.Point2D(-1, 0),
                    new javafx.geometry.Point2D(0, -1), new javafx.geometry.Point2D(0, 1)
            );
            case 4, 5 -> weapon01Dirs = java.util.List.of(
                    new javafx.geometry.Point2D(1, 0), new javafx.geometry.Point2D(-1, 0),
                    new javafx.geometry.Point2D(0, -1), new javafx.geometry.Point2D(0, 1),
                    new javafx.geometry.Point2D(1, -1).normalize(), new javafx.geometry.Point2D(1, 1).normalize(),
                    new javafx.geometry.Point2D(-1, -1).normalize(), new javafx.geometry.Point2D(-1, 1).normalize()
            );
        }
    }

    public static java.util.List<javafx.geometry.Point2D> getWeapon01Directions() {
        return weapon01Dirs;
    }

    // ===== 武器03：正面炸裂弹（直线，命中销毁引爆）=====
    private static java.util.List<javafx.geometry.Point2D> weapon03Dirs = java.util.List.of();
    private static int weapon03Level = 0;
    private static double weapon03ExplosionRadius = 0.0;
    private static double weapon03ExplosionScale = 1.0;

    public static java.util.List<javafx.geometry.Point2D> getWeapon03Directions() { return weapon03Dirs; }
    public static int getWeapon03Level() { return weapon03Level; }
    public static double getWeapon03ExplosionRadius() { return weapon03ExplosionRadius; }
    public static double getWeapon03ExplosionScale() { return weapon03ExplosionScale; }

    private void applyWeapon03(int level) {
        int lv = Math.max(1, Math.min(level, 5));
        weapon03Level = lv;

        // 伤害映射
        int dmg = switch (lv) {
            case 1 -> 6;
            case 2 -> 8;
            case 3 -> 9;
            case 4 -> 10;
            default -> 12;
        };
        // 写回 BulletSpec(straight_03)
        var b = com.roguelike.entities.configs.BulletRegistry.get("straight_03");
        if (b != null) {
            com.roguelike.entities.configs.BulletRegistry.register(new com.roguelike.entities.configs.BulletSpec(
                    b.getId(), b.getDisplayName(), b.getBulletType(),
                    dmg, b.isPiercing(), b.getBaseSpeed(),
                    b.getLifetimeSeconds(), b.getWidth(), b.getHeight(), b.getRadius(),
                    b.getAnimationBasePath(), b.getFrameCount(), b.getFrameDuration(),
                    b.getVisualScale()
            ));
        }

        // 攻击间隔
        double interval = (lv == 5) ? 0.20 : 0.25;
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon03", "Weapon 03", "straight_03", interval,
                1, 0.0
        ));

        // 方向集合
        switch (lv) {
            case 1 -> weapon03Dirs = java.util.List.of(
                    new javafx.geometry.Point2D(1, 0), // 右
                    new javafx.geometry.Point2D(-1, 0) // 左
            );
            case 2 -> weapon03Dirs = java.util.List.of(
                    new javafx.geometry.Point2D(0, -1), // 上（从正右开始约定，上为 -Y）
                    new javafx.geometry.Point2D(1, 0),
                    new javafx.geometry.Point2D(-1, 0)
            );
            case 3 -> weapon03Dirs = java.util.List.of(
                    new javafx.geometry.Point2D(1, 0),
                    new javafx.geometry.Point2D(-1, 0),
                    new javafx.geometry.Point2D(0, -1),
                    new javafx.geometry.Point2D(0, 1)
            );
            default -> { // 4 与 5：正五角星，从正上开始每 72°
                java.util.ArrayList<javafx.geometry.Point2D> list = new java.util.ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    double deg = -90.0 + i * 72.0; // -90, -18, 54, 126, 198 （即上方开始）
                    double rad = Math.toRadians(deg);
                    list.add(new javafx.geometry.Point2D(Math.cos(rad), Math.sin(rad)));
                }
                weapon03Dirs = java.util.List.copyOf(list);
            }
        }

        // 爆炸半径与爆炸动画缩放（随等级增长）
        // 进一步减小爆炸半径与动画增幅
        weapon03ExplosionRadius = switch (lv) {
            case 1 -> 72.0;
            case 2 -> 84.0;
            case 3 -> 96.0;
            case 4 -> 108.0;
            default -> 120.0;
        };
        weapon03ExplosionScale = switch (lv) {
            case 1 -> 1.10;
            case 2 -> 1.20;
            case 3 -> 1.30;
            case 4 -> 1.40;
            default -> 1.50;
        };
    }

    // ===== 武器04：围绕玩家的光环范围伤害 =====
    private static int weapon04Level = 0;
    private static double weapon04Radius = 0.0;
    private static double weapon04TickInterval = 0.25;
    private static int weapon04Damage = 0;

    public static int getWeapon04Level() { return weapon04Level; }
    public static double getWeapon04Radius() { return weapon04Radius; }
    public static double getWeapon04TickInterval() { return weapon04TickInterval; }
    public static int getWeapon04Damage() { return weapon04Damage; }

    private void applyWeapon04(int level) {
        int lv = Math.max(1, Math.min(level, 5));
        weapon04Level = lv;

        // 上调伤害以便触发三档飘字颜色：Lv1<=20(白)，Lv2-4 介于21-100(红)，Lv5>=101(紫)
        weapon04Damage = switch (lv) {
            case 1 -> 16;   // 白色
            case 2 -> 32;   // 红色
            case 3 -> 64;   // 红色
            case 4 -> 96;   // 红色
            default -> 128; // 紫色
        };

        // 半径：适度增加（像素）
        weapon04Radius = switch (lv) {
            case 1 -> 110.0;
            case 2 -> 130.0;
            case 3 -> 150.0;
            case 4 -> 170.0;
            default -> 190.0;
        };

        // 增加tick间隔以降低DPS（原: 0.25/0.25/0.25/0.25/0.20）
        weapon04TickInterval = switch (lv) {
            case 1 -> 0.40;
            case 2 -> 0.35;
            case 3 -> 0.35;
            case 4 -> 0.30;
            default -> 0.25;
        };

        // 注册 weapon04（作为触发标识）
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon04", "Weapon 04", "straight_04", weapon04TickInterval, 1, 0.0
        ));
    }

    // ===== 武器05：定向重击弹 =====
    private static java.util.List<javafx.geometry.Point2D> weapon05Dirs = java.util.List.of();
    private static int weapon05Level = 0;
    public static java.util.List<javafx.geometry.Point2D> getWeapon05Directions() { return weapon05Dirs; }
    public static int getWeapon05Level() { return weapon05Level; }

    private void applyWeapon05(int level) {
        int lv = Math.max(1, Math.min(level, 5));
        weapon05Level = lv;

        // 伤害：10/15/20/25/30
        int dmg = switch (lv) {
            case 1 -> 10;
            case 2 -> 15;
            case 3 -> 20;
            case 4 -> 25;
            default -> 30;
        };
        var b = com.roguelike.entities.configs.BulletRegistry.get("straight_05");
        if (b != null) {
            com.roguelike.entities.configs.BulletRegistry.register(new com.roguelike.entities.configs.BulletSpec(
                    b.getId(), b.getDisplayName(), b.getBulletType(),
                    dmg, b.isPiercing(), b.getBaseSpeed(),
                    b.getLifetimeSeconds(), b.getWidth(), b.getHeight(), b.getRadius(),
                    b.getAnimationBasePath(), b.getFrameCount(), b.getFrameDuration(),
                    b.getVisualScale()
            ));
        }

        // 发射间隔恒定 3.0s
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon05", "Weapon 05", "straight_05", 3.0, 1, 0.0
        ));

        // 方向集合
        switch (lv) {
            case 1, 2 -> weapon05Dirs = java.util.List.of(new javafx.geometry.Point2D(1, 0)); // 右
            case 3, 4 -> weapon05Dirs = java.util.List.of(
                    new javafx.geometry.Point2D(1, 0), new javafx.geometry.Point2D(-1, 0)
            );
            default -> weapon05Dirs = java.util.List.of(
                    new javafx.geometry.Point2D(1, 0), new javafx.geometry.Point2D(-1, 0),
                    new javafx.geometry.Point2D(0, -1), new javafx.geometry.Point2D(0, 1)
            );
        }
    }

    // ===== 武器06：随机落雷（一次性AOE）=====
    private static int weapon06Level = 0;
    private static int weapon06Count = 0;
    private static double weapon06Radius = 0.0;
    private static int weapon06Damage = 0;
    public static int getWeapon06Level() { return weapon06Level; }
    public static int getWeapon06Count() { return weapon06Count; }
    public static double getWeapon06Radius() { return weapon06Radius; }
    public static int getWeapon06Damage() { return weapon06Damage; }

    private void applyWeapon06(int level) {
        int lv = Math.max(1, Math.min(level, 5));
        weapon06Level = lv;

        // 伤害：18/23/27/32/36
        weapon06Damage = switch (lv) {
            case 1 -> 18;
            case 2 -> 23;
            case 3 -> 27;
            case 4 -> 32;
            default -> 36;
        };

        // 个数：1/2/2/3/3
        weapon06Count = switch (lv) {
            case 1 -> 1;
            case 2, 3 -> 2;
            default -> 3;
        };

        // 半径（像素）：120/135/150/165/180（可调整）
        weapon06Radius = switch (lv) {
            case 1 -> 120.0;
            case 2 -> 135.0;
            case 3 -> 150.0;
            case 4 -> 165.0;
            default -> 180.0;
        };

        // 发射间隔恒定 3.0s
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon06", "Weapon 06", "straight_06", 3.0, 1, 0.0
        ));
    }

    // ===== 武器07：复刻 03 的直线炸裂弹，爆炸改用 bullets/10 =====
    private static java.util.List<javafx.geometry.Point2D> weapon07Dirs = java.util.List.of();
    private static int weapon07Level = 0;
    private static double weapon07ExplosionRadius = 0.0;
    private static double weapon07ExplosionScale = 1.0;
    public static java.util.List<javafx.geometry.Point2D> getWeapon07Directions() { return weapon07Dirs; }
    public static int getWeapon07Level() { return weapon07Level; }
    public static double getWeapon07ExplosionRadius() { return weapon07ExplosionRadius; }
    public static double getWeapon07ExplosionScale() { return weapon07ExplosionScale; }

    private void applyWeapon07(int level) {
        int lv = Math.max(1, Math.min(level, 5));
        weapon07Level = lv;

        // 伤害同 03：6/8/9/10/12（若需不同再调整）
        int dmg = switch (lv) {
            case 1 -> 6;
            case 2 -> 8;
            case 3 -> 9;
            case 4 -> 10;
            default -> 12;
        };
        var b = com.roguelike.entities.configs.BulletRegistry.get("straight_07");
        if (b != null) {
            com.roguelike.entities.configs.BulletRegistry.register(new com.roguelike.entities.configs.BulletSpec(
                    b.getId(), b.getDisplayName(), b.getBulletType(),
                    dmg, b.isPiercing(), b.getBaseSpeed(),
                    b.getLifetimeSeconds(), b.getWidth(), b.getHeight(), b.getRadius(),
                    b.getAnimationBasePath(), b.getFrameCount(), b.getFrameDuration(),
                    b.getVisualScale()
            ));
        }

        // 发射间隔同 03：LV5=0.20，否则 0.25
        double interval = (lv == 5) ? 0.20 : 0.25;
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon07", "Weapon 07", "straight_07", interval, 1, 0.0
        ));

        // 方向集合同 03
        switch (lv) {
            case 1 -> weapon07Dirs = java.util.List.of(new javafx.geometry.Point2D(1, 0), new javafx.geometry.Point2D(-1, 0));
            case 2 -> weapon07Dirs = java.util.List.of(new javafx.geometry.Point2D(0, -1), new javafx.geometry.Point2D(1, 0), new javafx.geometry.Point2D(-1, 0));
            case 3 -> weapon07Dirs = java.util.List.of(new javafx.geometry.Point2D(1, 0), new javafx.geometry.Point2D(-1, 0), new javafx.geometry.Point2D(0, -1), new javafx.geometry.Point2D(0, 1));
            default -> {
                java.util.ArrayList<javafx.geometry.Point2D> list = new java.util.ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    double deg = -90.0 + i * 72.0;
                    double rad = Math.toRadians(deg);
                    list.add(new javafx.geometry.Point2D(Math.cos(rad), Math.sin(rad)));
                }
                weapon07Dirs = java.util.List.copyOf(list);
            }
        }

        // 再次减小 10 爆炸的范围与显示缩放
        weapon07ExplosionRadius = switch (lv) {
            case 1 -> 32.0;
            case 2 -> 40.0;
            case 3 -> 48.0;
            case 4 -> 56.0;
            default -> 64.0;
        };
        weapon07ExplosionScale = switch (lv) {
            case 1 -> 0.75;
            case 2 -> 0.85;
            case 3 -> 0.95;
            case 4 -> 1.00;
            default -> 1.05;
        };
    }

    // ===== 武器08：反弹（可穿透，不间断，寿命固定5s）=====
    private static int weapon08Level = 0;
    private static int weapon08Count = 0;
    private static int weapon08Damage = 0;
    private static double weapon08Lifetime = 5.0;
    public static int getWeapon08Level() { return weapon08Level; }
    public static int getWeapon08Count() { return weapon08Count; }
    public static int getWeapon08Damage() { return weapon08Damage; }
    public static double getWeapon08Lifetime() { return weapon08Lifetime; }

    private void applyWeapon08(int level) {
        int lv = Math.max(1, Math.min(level, 5));
        weapon08Level = lv;

        // 伤害：10/10/15/20/25
        weapon08Damage = switch (lv) {
            case 1, 2 -> 10;
            case 3 -> 15;
            case 4 -> 20;
            default -> 25;
        };

        // 在场数量：1/2/2/3/3
        weapon08Count = switch (lv) {
            case 1 -> 1;
            case 2, 3 -> 2;
            default -> 3;
        };

        weapon08Lifetime = 5.0; // 固定5s

        // 注册 weapon08 作为触发标识（间隔依赖于维持逻辑，不用于真正的节奏）
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon08", "Weapon 08", "straight_08", 0.5, 1, 0.0
        ));

        // 写回 BulletSpec(straight_08) 的伤害（若条目存在）
        var b = com.roguelike.entities.configs.BulletRegistry.get("straight_08");
        if (b != null) {
            com.roguelike.entities.configs.BulletRegistry.register(new com.roguelike.entities.configs.BulletSpec(
                    b.getId(), b.getDisplayName(), b.getBulletType(),
                    weapon08Damage, b.isPiercing(), b.getBaseSpeed(),
                    b.getLifetimeSeconds(), b.getWidth(), b.getHeight(), b.getRadius(),
                    b.getAnimationBasePath(), b.getFrameCount(), b.getFrameDuration(),
                    b.getVisualScale()
            ));
        }
    }

    /**
     * 武器02（环绕）：控制个数与伤害，间隔恒定为 2s 旋转一圈。
     */
    private void applyWeapon02(int level) {
        // 更新 BulletSpec（伤害）
        com.roguelike.entities.configs.BulletSpec b = com.roguelike.entities.configs.BulletRegistry.get("straight_02");
        if (b != null) {
            int dmg = switch (Math.max(1, Math.min(level, 5))) {
                case 1 -> 15;
                case 2 -> 20;
                case 3 -> 25;
                case 4 -> 30;
                default -> 40;
            };
            com.roguelike.entities.configs.BulletRegistry.register(new com.roguelike.entities.configs.BulletSpec(
                    b.getId(), b.getDisplayName(), b.getBulletType(),
                    dmg, b.isPiercing(), b.getBaseSpeed(),
                    b.getLifetimeSeconds(), b.getWidth(), b.getHeight(), b.getRadius(),
                    b.getAnimationBasePath(), b.getFrameCount(), b.getFrameDuration(),
                    b.getVisualScale()
            ));
        }

        // 保存环绕个数（2/3/4/5/5）到全局，供 AutoFire 使用
        int count = switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            case 4 -> 5;
            default -> 5;
        };
        setWeapon02OrbitCount(count);
        // 02 的 AttackSpec 用于触发刷新（间隔取 weapon01 也可以，这里给 0.3s 以便尽快同步）
        com.roguelike.entities.configs.AttackRegistry.register(new com.roguelike.entities.configs.AttackSpec(
                "weapon02", "Weapon 02", "straight_02", 0.3,
                1, 0.0
        ));
    }

    private static int weapon02OrbitCount = 0;
    public static int getWeapon02OrbitCount() { return weapon02OrbitCount; }
    private static void setWeapon02OrbitCount(int c) { weapon02OrbitCount = Math.max(1, c); }
}


