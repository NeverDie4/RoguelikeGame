package com.roguelike.physics;

/**
 * 碰撞箱级别枚举
 * 定义了实体之间的推挤优先级关系
 */
public enum CollisionBoxLevel {
    PLAYER(3),   // 最高级别 - 不能被其他实体推挤
    ELITE(2),    // 中等级别 - 不能被enemy推挤，但可以被player推挤（预留）
    ENEMY(1);    // 最低级别 - 可以被player和elite推挤
    
    private final int priority;
    
    CollisionBoxLevel(int priority) {
        this.priority = priority;
    }
    
    /**
     * 获取优先级数值
     * 数值越高，优先级越高
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 检查当前级别是否可以推挤目标级别
     * @param other 目标级别
     * @return true如果可以推挤，false如果不能推挤
     */
    public boolean canPush(CollisionBoxLevel other) {
        return this.priority > other.priority;
    }
    
    /**
     * 检查当前级别是否可以被目标级别推挤
     * @param other 目标级别
     * @return true如果可以被推挤，false如果不能被推挤
     */
    public boolean canBePushedBy(CollisionBoxLevel other) {
        return other.priority > this.priority;
    }
    
    /**
     * 获取级别名称
     */
    @Override
    public String toString() {
        return name() + "(" + priority + ")";
    }
}
