package com.roguelike.ui;

import com.roguelike.entities.weapons.WeaponId;

/**
 * 武器数值变化计算工具类
 * 用于计算武器在不同等级时的数值变化
 */
public final class WeaponStatsCalculator {
    
    /**
     * 武器数值变化信息
     */
    public static class WeaponStatsChange {
        private final String damageChange;
        private final String intervalChange;
        private final String countChange;
        private final String radiusChange;
        private final String directionChange;
        
        public WeaponStatsChange(String damageChange, String intervalChange, 
                               String countChange, String radiusChange, String directionChange) {
            this.damageChange = damageChange;
            this.intervalChange = intervalChange;
            this.countChange = countChange;
            this.radiusChange = radiusChange;
            this.directionChange = directionChange;
        }
        
        public String getDamageChange() { return damageChange; }
        public String getIntervalChange() { return intervalChange; }
        public String getCountChange() { return countChange; }
        public String getRadiusChange() { return radiusChange; }
        public String getDirectionChange() { return directionChange; }
        
        /**
         * 获取格式化的数值变化描述
         */
        public String getFormattedChanges() {
            StringBuilder sb = new StringBuilder();
            
            if (damageChange != null && !damageChange.isEmpty()) {
                sb.append("伤害: ").append(damageChange);
            }
            if (intervalChange != null && !intervalChange.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("间隔: ").append(intervalChange);
            }
            if (countChange != null && !countChange.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("数量: ").append(countChange);
            }
            if (radiusChange != null && !radiusChange.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("半径: ").append(radiusChange);
            }
            if (directionChange != null && !directionChange.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("方向: ").append(directionChange);
            }
            
            return sb.toString();
        }
    }
    
    /**
     * 计算武器从当前等级升级到下一等级的数值变化
     */
    public static WeaponStatsChange calculateUpgradeChange(WeaponId weaponId, int currentLevel) {
        int nextLevel = currentLevel + 1;
        
        switch (weaponId) {
            case W01 -> {
                return calculateWeapon01Change(currentLevel, nextLevel);
            }
            case W02 -> {
                return calculateWeapon02Change(currentLevel, nextLevel);
            }
            case W03 -> {
                return calculateWeapon03Change(currentLevel, nextLevel);
            }
            case W04 -> {
                return calculateWeapon04Change(currentLevel, nextLevel);
            }
            case W05 -> {
                return calculateWeapon05Change(currentLevel, nextLevel);
            }
            case W06 -> {
                return calculateWeapon06Change(currentLevel, nextLevel);
            }
            case W07 -> {
                return calculateWeapon07Change(currentLevel, nextLevel);
            }
            case W08 -> {
                return calculateWeapon08Change(currentLevel, nextLevel);
            }
            default -> {
                return new WeaponStatsChange(null, null, null, null, null);
            }
        }
    }
    
    // 武器01（焚天炎杖）
    private static WeaponStatsChange calculateWeapon01Change(int current, int next) {
        int currentDmg = getWeapon01Damage(current);
        int nextDmg = getWeapon01Damage(next);
        double currentInterval = getWeapon01Interval(current);
        double nextInterval = getWeapon01Interval(next);
        String currentDir = getWeapon01Direction(current);
        String nextDir = getWeapon01Direction(next);
        
        return new WeaponStatsChange(
            formatChange(currentDmg, nextDmg),
            formatChange(currentInterval, nextInterval) + "s",
            null,
            null,
            currentDir.equals(nextDir) ? null : currentDir + " → " + nextDir
        );
    }
    
    // 武器02（星陨碎渊）
    private static WeaponStatsChange calculateWeapon02Change(int current, int next) {
        int currentDmg = getWeapon02Damage(current);
        int nextDmg = getWeapon02Damage(next);
        int currentCount = getWeapon02Count(current);
        int nextCount = getWeapon02Count(next);
        
        return new WeaponStatsChange(
            formatChange(currentDmg, nextDmg),
            null, // 间隔固定
            formatChange(currentCount, nextCount),
            null,
            null
        );
    }
    
    // 武器03（凛冬之怒）
    private static WeaponStatsChange calculateWeapon03Change(int current, int next) {
        int currentDmg = getWeapon03Damage(current);
        int nextDmg = getWeapon03Damage(next);
        double currentInterval = getWeapon03Interval(current);
        double nextInterval = getWeapon03Interval(next);
        double currentRadius = getWeapon03Radius(current);
        double nextRadius = getWeapon03Radius(next);
        String currentDir = getWeapon03Direction(current);
        String nextDir = getWeapon03Direction(next);
        
        return new WeaponStatsChange(
            formatChange(currentDmg, nextDmg),
            formatChange(currentInterval, nextInterval) + "s",
            null,
            formatChange((int)currentRadius, (int)nextRadius),
            currentDir.equals(nextDir) ? null : currentDir + " → " + nextDir
        );
    }
    
    // 武器04（焰心轮环）
    private static WeaponStatsChange calculateWeapon04Change(int current, int next) {
        int currentDmg = getWeapon04Damage(current);
        int nextDmg = getWeapon04Damage(next);
        double currentRadius = getWeapon04Radius(current);
        double nextRadius = getWeapon04Radius(next);
        double currentInterval = getWeapon04Interval(current);
        double nextInterval = getWeapon04Interval(next);
        
        return new WeaponStatsChange(
            formatChange(currentDmg, nextDmg),
            formatChange(currentInterval, nextInterval) + "s",
            null,
            formatChange((int)currentRadius, (int)nextRadius),
            null
        );
    }
    
    // 武器05（沧澜断空）
    private static WeaponStatsChange calculateWeapon05Change(int current, int next) {
        int currentDmg = getWeapon05Damage(current);
        int nextDmg = getWeapon05Damage(next);
        String currentDir = getWeapon05Direction(current);
        String nextDir = getWeapon05Direction(next);
        
        return new WeaponStatsChange(
            formatChange(currentDmg, nextDmg),
            null, // 间隔固定3.0s
            null,
            null,
            currentDir.equals(nextDir) ? null : currentDir + " → " + nextDir
        );
    }
    
    // 武器06（雷环御霆）
    private static WeaponStatsChange calculateWeapon06Change(int current, int next) {
        int currentDmg = getWeapon06Damage(current);
        int nextDmg = getWeapon06Damage(next);
        int currentCount = getWeapon06Count(current);
        int nextCount = getWeapon06Count(next);
        double currentRadius = getWeapon06Radius(current);
        double nextRadius = getWeapon06Radius(next);
        
        return new WeaponStatsChange(
            formatChange(currentDmg, nextDmg),
            null, // 间隔固定3.0s
            formatChange(currentCount, nextCount),
            formatChange((int)currentRadius, (int)nextRadius),
            null
        );
    }
    
    // 武器07（赤湮劫轮）
    private static WeaponStatsChange calculateWeapon07Change(int current, int next) {
        int currentDmg = getWeapon07Damage(current);
        int nextDmg = getWeapon07Damage(next);
        double currentInterval = getWeapon07Interval(current);
        double nextInterval = getWeapon07Interval(next);
        double currentRadius = getWeapon07Radius(current);
        double nextRadius = getWeapon07Radius(next);
        String currentDir = getWeapon07Direction(current);
        String nextDir = getWeapon07Direction(next);
        
        return new WeaponStatsChange(
            formatChange(currentDmg, nextDmg),
            formatChange(currentInterval, nextInterval) + "s",
            null,
            formatChange((int)currentRadius, (int)nextRadius),
            currentDir.equals(nextDir) ? null : currentDir + " → " + nextDir
        );
    }
    
    // 武器08（寒渊星环）
    private static WeaponStatsChange calculateWeapon08Change(int current, int next) {
        int currentDmg = getWeapon08Damage(current);
        int nextDmg = getWeapon08Damage(next);
        int currentCount = getWeapon08Count(current);
        int nextCount = getWeapon08Count(next);
        
        return new WeaponStatsChange(
            formatChange(currentDmg, nextDmg),
            null, // 间隔固定0.5s
            formatChange(currentCount, nextCount),
            null,
            null
        );
    }
    
    // 格式化数值变化
    private static String formatChange(int current, int next) {
        return current + " → " + next;
    }
    
    private static String formatChange(double current, double next) {
        return String.format("%.2f → %.2f", current, next);
    }
    
    // 武器01数值获取方法
    private static int getWeapon01Damage(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 15; case 2 -> 20; case 3 -> 30; case 4 -> 40; default -> 56;
        };
    }
    
    private static double getWeapon01Interval(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 1.0; case 2 -> 0.88; case 3 -> 0.8; case 4 -> 0.73; default -> 0.7;
        };
    }
    
    private static String getWeapon01Direction(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> "右"; case 2 -> "左右"; case 3 -> "四方向"; default -> "八方向";
        };
    }
    
    // 武器02数值获取方法
    private static int getWeapon02Damage(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 15; case 2 -> 20; case 3 -> 25; case 4 -> 30; default -> 40;
        };
    }
    
    private static int getWeapon02Count(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 1; case 2 -> 2; case 3 -> 3; case 4 -> 4; default -> 5;
        };
    }
    
    // 武器03数值获取方法
    private static int getWeapon03Damage(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 6; case 2 -> 8; case 3 -> 9; case 4 -> 10; default -> 12;
        };
    }
    
    private static double getWeapon03Interval(int level) {
        return (level == 5) ? 0.20 : 0.25;
    }
    
    private static double getWeapon03Radius(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 72.0; case 2 -> 84.0; case 3 -> 96.0; case 4 -> 108.0; default -> 120.0;
        };
    }
    
    private static String getWeapon03Direction(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> "左右"; case 2 -> "上左右"; case 3 -> "四方向"; default -> "五角星";
        };
    }
    
    // 武器04数值获取方法
    private static int getWeapon04Damage(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 16; case 2 -> 32; case 3 -> 64; case 4 -> 96; default -> 128;
        };
    }
    
    private static double getWeapon04Radius(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 80.0; case 2 -> 95.0; case 3 -> 110.0; case 4 -> 125.0; default -> 140.0;
        };
    }
    
    private static double getWeapon04Interval(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 0.45; case 2 -> 0.40; case 3 -> 0.35; case 4 -> 0.30; default -> 0.28;
        };
    }
    
    // 武器05数值获取方法
    private static int getWeapon05Damage(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 10; case 2 -> 15; case 3 -> 20; case 4 -> 25; default -> 30;
        };
    }
    
    private static String getWeapon05Direction(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1, 2 -> "右"; case 3, 4 -> "左右"; default -> "四方向";
        };
    }
    
    // 武器06数值获取方法
    private static int getWeapon06Damage(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 18; case 2 -> 23; case 3 -> 27; case 4 -> 32; default -> 36;
        };
    }
    
    private static int getWeapon06Count(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 1; case 2, 3 -> 2; default -> 3;
        };
    }
    
    private static double getWeapon06Radius(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 120.0; case 2 -> 135.0; case 3 -> 150.0; case 4 -> 165.0; default -> 180.0;
        };
    }
    
    // 武器07数值获取方法
    private static int getWeapon07Damage(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 6; case 2 -> 8; case 3 -> 9; case 4 -> 10; default -> 12;
        };
    }
    
    private static double getWeapon07Interval(int level) {
        return (level == 5) ? 0.20 : 0.25;
    }
    
    private static double getWeapon07Radius(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 32.0; case 2 -> 40.0; case 3 -> 48.0; case 4 -> 56.0; default -> 64.0;
        };
    }
    
    private static String getWeapon07Direction(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> "左右"; case 2 -> "上左右"; case 3 -> "四方向"; default -> "五角星";
        };
    }
    
    // 武器08数值获取方法
    private static int getWeapon08Damage(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1, 2 -> 10; case 3 -> 15; case 4 -> 20; default -> 25;
        };
    }
    
    private static int getWeapon08Count(int level) {
        return switch (Math.max(1, Math.min(level, 5))) {
            case 1 -> 1; case 2, 3 -> 2; default -> 3;
        };
    }
}
