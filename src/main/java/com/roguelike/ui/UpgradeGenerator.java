package com.roguelike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 生成升级选项：随机3个，过滤满级，避免重复。
 */
public final class UpgradeGenerator {
    private final com.roguelike.entities.weapons.WeaponManager weaponManager;
    private final PassiveItemManager passiveManager;
    private final Random random = new Random();

    public UpgradeGenerator(com.roguelike.entities.weapons.WeaponManager weaponManager, PassiveItemManager passiveManager) {
        this.weaponManager = weaponManager;
        this.passiveManager = passiveManager;
    }

    public List<UpgradeOption> generateThree() {
        List<UpgradeOption> pool = new ArrayList<>();

        // 武器 01..08：若未满级则加入
        String[] ids = {"01","02","03","04","05","06","07","08"};
        for (String id : ids) {
            com.roguelike.entities.weapons.WeaponId wid = com.roguelike.entities.weapons.WeaponId.valueOf("W" + id);
            int cur = weaponManager.getLevel(wid);
            int max = weaponManager.getMaxLevel(wid);
            if (cur < max) {
                String desc;
                String statsChange = null;
                if (cur == 0) {
                    // 首次获得武器时显示背景故事
                    desc = wid.getLoreDescription();
                } else {
                    // 升级时显示数值描述和数值变化
                    desc = "升级至Lv." + (cur + 1);
                    // 计算数值变化
                    WeaponStatsCalculator.WeaponStatsChange change = WeaponStatsCalculator.calculateUpgradeChange(wid, cur);
                    statsChange = change.getFormattedChanges();
                }
                pool.add(UpgradeOption.weapon(id, desc, statsChange));
            }
        }

        // 被动 P01..P06：若未满级则加入
        PassiveId[] pids = PassiveId.values();
        for (PassiveId pid : pids) {
            int cur = passiveManager.getLevel(pid);
            if (cur < 5) {
                double next = PassiveRegistry.get(pid).getValueAtLevel(cur + 1);
                String desc = formatValue(pid, cur, next);
                pool.add(UpgradeOption.passive(pid, desc));
            }
        }

        if (pool.isEmpty()) return Collections.emptyList();
        Collections.shuffle(pool, random);
        List<UpgradeOption> res = new ArrayList<>();
        for (UpgradeOption opt : pool) {
            res.add(opt);
            if (res.size() == 3) break;
        }
        return res;
    }

    private String formatValue(PassiveId id, int cur, double next) {
        switch (id) {
            case P01_ATTACK_SPEED -> {
                return "飞行速度 +" + (int) Math.round((next - 1.0) * 100) + "%";
            }
            case P02_PROJECTILE_COUNT -> {
                return "发射数量 +" + ((int) next);
            }
            case P03_COOLDOWN_REDUCE -> {
                return "武器冷却 +" + (int) Math.round((1.0 - next) * 100) + "%";
            }
            case P04_MAX_HP -> {
                return "生命上限 +" + ((int) next);
            }
            case P05_DAMAGE_MULTIPLIER -> {
                return "武器伤害 +" + (int) Math.round((next - 1.0) * 100) + "%";
            }
            case P06_MOVE_SPEED -> {
                return "移动速度 +" + (int) Math.round((next - 1.0) * 100) + "%";
            }
        }
        return String.valueOf(next);
    }
}


