package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;

/**
 * 平滑推挤系统
 * 负责处理实体之间的平滑推挤效果，避免瞬移
 */
public class SmoothPushSystem {
    
    // 基础推挤力度（可配置）
    private static final double BASE_PUSH_FORCE = 50.0;
    
    // 最大推挤距离（可配置）
    private static final double MAX_PUSH_DISTANCE = 100.0;
    
    // 最小推挤距离（可配置）
    private static final double MIN_PUSH_DISTANCE = 1.0;
    
    // 反比例函数系数（可配置）
    private static final double INVERSE_PROPORTION_FACTOR = 10.0;
    
    // 推挤冷却时间（可配置）
    private static final double PUSH_COOLDOWN = 0.1;
    
    // 推挤冷却记录
    private java.util.Map<String, Double> pushCooldowns = new java.util.HashMap<>();
    
    /**
     * 处理两个实体之间的推挤
     * @param entity1 实体1
     * @param entity2 实体2
     * @param level1 实体1的碰撞箱级别
     * @param level2 实体2的碰撞箱级别
     */
    public void handleCollision(Entity entity1, Entity entity2, 
                               CollisionBoxLevel level1, CollisionBoxLevel level2) {
        if (entity1 == null || entity2 == null) return;
        
        // 检查推挤冷却
        if (isPushOnCooldown(entity1, entity2)) {
            return;
        }
        
        // 根据级别决定推挤方向
        if (level1.canPush(level2)) {
            pushEntity(entity2, entity1, calculatePushDirection(entity1, entity2));
            recordPushCooldown(entity1, entity2);
        } else if (level2.canPush(level1)) {
            pushEntity(entity1, entity2, calculatePushDirection(entity2, entity1));
            recordPushCooldown(entity1, entity2);
        }
    }
    
    /**
     * 推挤目标实体
     * @param target 被推挤的实体
     * @param pusher 推挤者实体
     * @param direction 推挤方向
     */
    private void pushEntity(Entity target, Entity pusher, Point2D direction) {
        if (target == null || pusher == null || direction == null) return;
        
        // 计算推挤距离
        double distance = pusher.getCenter().distance(target.getCenter());
        
        // 检查距离是否在有效范围内
        if (distance < MIN_PUSH_DISTANCE || distance > MAX_PUSH_DISTANCE) {
            return;
        }
        
        // 计算推挤力度（距离越近，力度越大）
        double force = calculatePushForce(distance);
        Point2D pushForce = direction.multiply(force);
        
        // 应用推挤力
        applyPushForce(target, pushForce);
    }
    
    /**
     * 计算推挤力度
     * 使用反比例函数：力度 = 系数 / 距离
     * @param distance 两个实体之间的距离
     * @return 推挤力度
     */
    private double calculatePushForce(double distance) {
        // 确保距离不为0，避免除零错误
        distance = Math.max(distance, MIN_PUSH_DISTANCE);
        
        // 使用反比例函数：力度 = 系数 / 距离
        // 距离越近，力度越大
        double force = INVERSE_PROPORTION_FACTOR / distance;
        
        // 限制最大力度，避免过强的推挤
        double maxForce = BASE_PUSH_FORCE;
        force = Math.min(force, maxForce);
        
        // 如果距离太远，不产生推挤力
        if (distance > MAX_PUSH_DISTANCE) {
            force = 0;
        }
        
        return force;
    }
    
    /**
     * 应用推挤力到目标实体
     * @param target 目标实体
     * @param pushForce 推挤力向量
     */
    private void applyPushForce(Entity target, Point2D pushForce) {
        if (target == null || pushForce == null) return;
        
        // 直接推挤实体位置
        // 推挤力度越大，移动距离越大
        double pushDistance = pushForce.magnitude() * 0.1; // 可配置的推挤距离系数
        
        // 限制推挤距离，避免瞬移
        double maxPushDistance = 5.0; // 可配置的最大推挤距离
        pushDistance = Math.min(pushDistance, maxPushDistance);
        
        // 计算推挤方向
        Point2D pushDirection = pushForce.normalize();
        
        // 应用推挤
        double pushX = pushDirection.getX() * pushDistance;
        double pushY = pushDirection.getY() * pushDistance;
        
        target.translate(pushX, pushY);
    }
    
    /**
     * 计算推挤方向
     * @param pusher 推挤者
     * @param target 目标
     * @return 推挤方向向量
     */
    private Point2D calculatePushDirection(Entity pusher, Entity target) {
        if (pusher == null || target == null) return new Point2D(0, 0);
        
        Point2D pusherCenter = pusher.getCenter();
        Point2D targetCenter = target.getCenter();
        
        Point2D direction = targetCenter.subtract(pusherCenter);
        
        // 归一化方向向量
        double magnitude = direction.magnitude();
        if (magnitude > 0) {
            return direction.multiply(1.0 / magnitude);
        }
        
        return new Point2D(0, 0);
    }
    
    /**
     * 检查推挤是否在冷却时间内
     * @param entity1 实体1
     * @param entity2 实体2
     * @return true如果在冷却时间内，false如果不在
     */
    private boolean isPushOnCooldown(Entity entity1, Entity entity2) {
        String key = generatePushKey(entity1, entity2);
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        if (pushCooldowns.containsKey(key)) {
            double lastPushTime = pushCooldowns.get(key);
            return (currentTime - lastPushTime) < PUSH_COOLDOWN;
        }
        
        return false;
    }
    
    /**
     * 记录推挤冷却时间
     * @param entity1 实体1
     * @param entity2 实体2
     */
    private void recordPushCooldown(Entity entity1, Entity entity2) {
        String key = generatePushKey(entity1, entity2);
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        pushCooldowns.put(key, currentTime);
    }
    
    /**
     * 生成推挤键值（确保顺序无关）
     * @param entity1 实体1
     * @param entity2 实体2
     * @return 推挤键值
     */
    private String generatePushKey(Entity entity1, Entity entity2) {
        int id1 = entity1.hashCode();
        int id2 = entity2.hashCode();
        
        if (id1 < id2) {
            return id1 + "_" + id2;
        } else {
            return id2 + "_" + id1;
        }
    }
    
    /**
     * 清理过期的冷却记录
     */
    public void cleanupExpiredCooldowns() {
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        pushCooldowns.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > PUSH_COOLDOWN * 2
        );
    }
    
    /**
     * 获取推挤系统配置信息
     * @return 配置信息字符串
     */
    public String getConfigInfo() {
        return String.format("推挤系统配置:\n" +
                           "  - 基础推挤力度: %.1f\n" +
                           "  - 反比例系数: %.1f\n" +
                           "  - 最大推挤距离: %.1f\n" +
                           "  - 最小推挤距离: %.1f\n" +
                           "  - 推挤冷却时间: %.2f秒\n" +
                           "  - 推挤函数: 力度 = %.1f / 距离",
                           BASE_PUSH_FORCE, INVERSE_PROPORTION_FACTOR, MAX_PUSH_DISTANCE, 
                           MIN_PUSH_DISTANCE, PUSH_COOLDOWN, INVERSE_PROPORTION_FACTOR);
    }
    
    /**
     * 获取当前冷却记录数量
     * @return 冷却记录数量
     */
    public int getCooldownCount() {
        return pushCooldowns.size();
    }
}
