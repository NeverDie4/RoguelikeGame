package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * 刚性碰撞系统
 * 实现物理上的物块碰撞，实体之间不能重叠
 */
public class RigidCollisionSystem {
    
    // 碰撞检测精度
    private static final double COLLISION_PRECISION = 0.1;
    
    // 最大分离距离
    private static final double MAX_SEPARATION_DISTANCE = 10.0;
    
    // 推挤力度系数（可配置）
    private double pushForceMultiplier = 1.0;
    
    // 推挤冷却机制
    private Map<String, Long> pushCooldowns = new HashMap<>();
    private static final long PUSH_COOLDOWN_MS = 50; // 50ms推挤冷却
    
    // 速度推挤参数
    private static final double VELOCITY_PUSH_FORCE = 80.0;  // 速度推挤力度（降低）
    private static final double MAX_VELOCITY = 150.0;        // 最大速度限制（降低）
    private static final double VELOCITY_DAMPING = 0.9;      // 速度阻尼
    private static final double POSITION_PUSH_FACTOR = 0.3;  // 位置推挤系数（降低）
    private static final double MIN_PUSH_DISTANCE = 0.15;    // 最小推挤距离（降低）
    
    /**
     * 处理两个实体之间的刚性碰撞
     * @param entity1 实体1
     * @param entity2 实体2
     * @param level1 实体1的碰撞箱级别
     * @param level2 实体2的碰撞箱级别
     */
    public void handleRigidCollision(Entity entity1, Entity entity2, 
                                   CollisionBoxLevel level1, CollisionBoxLevel level2) {
        if (entity1 == null || entity2 == null) return;
        
        // 检查是否发生碰撞
        Rectangle2D bounds1 = getEntityBounds(entity1);
        Rectangle2D bounds2 = getEntityBounds(entity2);
        
        if (!bounds1.intersects(bounds2)) {
            return; // 没有碰撞
        }
        
        // 处理刚性碰撞
        handleRigidCollisionMode(entity1, entity2, bounds1, bounds2, level1, level2);
    }
    
    /**
     * 处理刚性碰撞模式
     */
    private void handleRigidCollisionMode(Entity entity1, Entity entity2,
                                        Rectangle2D bounds1, Rectangle2D bounds2,
                                        CollisionBoxLevel level1, CollisionBoxLevel level2) {
        // 根据级别决定分离方向
        if (level1.canPush(level2)) {
            // entity1 推挤 entity2（主要推挤）
            separateEntities(entity1, entity2, bounds1, bounds2);
            // 同时给entity2一个反向推挤，防止"渗透"（降低强度）
            separateEntitiesReverse(entity2, entity1, bounds2, bounds1, 0.15);
        } else if (level2.canPush(level1)) {
            // entity2 推挤 entity1（主要推挤）
            separateEntities(entity2, entity1, bounds2, bounds1);
            // 同时给entity1一个反向推挤，防止"渗透"（降低强度）
            separateEntitiesReverse(entity1, entity2, bounds1, bounds2, 0.15);
        } else {
            // 级别相同，相互分离
            separateEntitiesMutually(entity1, entity2, bounds1, bounds2);
        }
    }
    
    
    /**
     * 分离两个实体，让pusher推挤target
     * @param pusher 推挤者
     * @param target 被推挤者
     * @param pusherBounds 推挤者边界
     * @param targetBounds 被推挤者边界
     */
    private void separateEntities(Entity pusher, Entity target, 
                                Rectangle2D pusherBounds, Rectangle2D targetBounds) {
        // 计算重叠区域
        double overlapX = Math.min(pusherBounds.getMaxX(), targetBounds.getMaxX()) - 
                         Math.max(pusherBounds.getMinX(), targetBounds.getMinX());
        double overlapY = Math.min(pusherBounds.getMaxY(), targetBounds.getMaxY()) - 
                         Math.max(pusherBounds.getMinY(), targetBounds.getMinY());
        
        // 计算推挤方向
        Point2D pusherCenter = new Point2D(
            pusherBounds.getMinX() + pusherBounds.getWidth() / 2,
            pusherBounds.getMinY() + pusherBounds.getHeight() / 2
        );
        Point2D targetCenter = new Point2D(
            targetBounds.getMinX() + targetBounds.getWidth() / 2,
            targetBounds.getMinY() + targetBounds.getHeight() / 2
        );
        Point2D direction = targetCenter.subtract(pusherCenter).normalize();
        
        // 计算推挤强度（基于重叠程度）
        double overlapAmount = Math.max(overlapX, overlapY);
        double pushStrength = (overlapAmount + COLLISION_PRECISION) * pushForceMultiplier;
        pushStrength = Math.max(pushStrength, MIN_PUSH_DISTANCE); // 最小推挤强度
        
        // 应用速度推挤
        applyVelocityPush(target, direction, pushStrength);
    }
    
    /**
     * 反向推挤（防止渗透）
     * @param target 被推挤的实体
     * @param pusher 推挤者
     * @param targetBounds 被推挤者边界
     * @param pusherBounds 推挤者边界
     * @param strength 推挤强度系数
     */
    private void separateEntitiesReverse(Entity target, Entity pusher,
                                       Rectangle2D targetBounds, Rectangle2D pusherBounds,
                                       double strength) {
        // 计算重叠区域
        double overlapX = Math.min(pusherBounds.getMaxX(), targetBounds.getMaxX()) - 
                         Math.max(pusherBounds.getMinX(), targetBounds.getMinX());
        double overlapY = Math.min(pusherBounds.getMaxY(), targetBounds.getMaxY()) - 
                         Math.max(pusherBounds.getMinY(), targetBounds.getMinY());
        
        // 计算推挤方向（从pusher指向target）
        Point2D pusherCenter = new Point2D(
            pusherBounds.getMinX() + pusherBounds.getWidth() / 2,
            pusherBounds.getMinY() + pusherBounds.getHeight() / 2
        );
        Point2D targetCenter = new Point2D(
            targetBounds.getMinX() + targetBounds.getWidth() / 2,
            targetBounds.getMinY() + targetBounds.getHeight() / 2
        );
        Point2D direction = targetCenter.subtract(pusherCenter).normalize();
        
        // 计算推挤强度（基于重叠程度，使用较小的强度和平滑算法）
        double overlapAmount = Math.max(overlapX, overlapY);
        double pushStrength = (overlapAmount + COLLISION_PRECISION) * pushForceMultiplier * strength;
        
        // 使用平方根函数让反向推挤更平滑
        pushStrength = Math.sqrt(pushStrength) * 0.4;
        
        pushStrength = Math.max(pushStrength, MIN_PUSH_DISTANCE * 0.3);
        
        // 应用反向推挤
        applyVelocityPush(target, direction, pushStrength);
    }
    
    /**
     * 相互分离两个实体
     * @param entity1 实体1
     * @param entity2 实体2
     * @param bounds1 实体1边界
     * @param bounds2 实体2边界
     */
    private void separateEntitiesMutually(Entity entity1, Entity entity2,
                                        Rectangle2D bounds1, Rectangle2D bounds2) {
        // 计算重叠区域
        double overlapX = Math.min(bounds1.getMaxX(), bounds2.getMaxX()) - 
                         Math.max(bounds1.getMinX(), bounds2.getMinX());
        double overlapY = Math.min(bounds1.getMaxY(), bounds2.getMaxY()) - 
                         Math.max(bounds1.getMinY(), bounds2.getMinY());
        
        // 计算分离方向
        Point2D center1 = new Point2D(
            bounds1.getMinX() + bounds1.getWidth() / 2,
            bounds1.getMinY() + bounds1.getHeight() / 2
        );
        Point2D center2 = new Point2D(
            bounds2.getMinX() + bounds2.getWidth() / 2,
            bounds2.getMinY() + bounds2.getHeight() / 2
        );
        Point2D direction = center2.subtract(center1).normalize();
        
        // 计算推挤强度（各推挤一半，使用平滑算法）
        double overlapAmount = Math.max(overlapX, overlapY);
        double pushStrength = (overlapAmount / 2.0 + COLLISION_PRECISION) * pushForceMultiplier;
        
        // 使用平方根函数让推挤更平滑
        pushStrength = Math.sqrt(pushStrength) * 0.6;
        
        pushStrength = Math.max(pushStrength, MIN_PUSH_DISTANCE / 2.0); // 最小推挤强度
        
        // 应用速度推挤
        applyVelocityPush(entity1, direction.multiply(-1), pushStrength);
        applyVelocityPush(entity2, direction, pushStrength);
    }
    
    /**
     * 检查两个实体是否发生碰撞
     * @param entity1 实体1
     * @param entity2 实体2
     * @return 是否发生碰撞
     */
    public boolean checkCollision(Entity entity1, Entity entity2) {
        if (entity1 == null || entity2 == null) return false;
        
        Rectangle2D bounds1 = getEntityBounds(entity1);
        Rectangle2D bounds2 = getEntityBounds(entity2);
        
        return bounds1.intersects(bounds2);
    }
    
    /**
     * 应用速度推挤
     * @param entity 目标实体
     * @param direction 推挤方向
     * @param pushStrength 推挤强度
     */
    private void applyVelocityPush(Entity entity, Point2D direction, double pushStrength) {
        if (entity == null || direction == null) return;
        
        // 检查推挤冷却（使用实体位置作为唯一标识）
        String entityId = String.format("%.1f,%.1f", entity.getX(), entity.getY());
        long currentTime = System.currentTimeMillis();
        Long lastPushTime = pushCooldowns.get(entityId);
        
        if (lastPushTime != null && (currentTime - lastPushTime) < PUSH_COOLDOWN_MS) {
            return; // 还在冷却中，跳过此次推挤
        }
        
        // 记录推挤时间
        pushCooldowns.put(entityId, currentTime);
        
        // 计算推挤距离（基于推挤强度，使用更平滑的算法）
        double pushDistance = pushStrength * pushForceMultiplier * POSITION_PUSH_FACTOR;
        
        // 使用平方根函数让推挤更平滑，避免突然的大幅度移动
        pushDistance = Math.sqrt(pushDistance) * 0.8;
        
        pushDistance = Math.min(pushDistance, 2.0); // 限制最大推挤距离
        pushDistance = Math.max(pushDistance, MIN_PUSH_DISTANCE); // 最小推挤距离
        
        // 计算移动向量
        double moveX = direction.getX() * pushDistance;
        double moveY = direction.getY() * pushDistance;
        
        // 检查是否启用速度推挤
        if (com.roguelike.core.GameApp.COLLISION_VELOCITY_PUSH_ENABLED) {
            // 尝试获取物理组件
            try {
                PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
                if (physics != null) {
                    // 如果有物理组件，使用速度推挤
                    double pushVelocity = pushDistance * VELOCITY_PUSH_FORCE;
                    double velocityX = direction.getX() * pushVelocity;
                    double velocityY = direction.getY() * pushVelocity;
                    
                    // 累加到现有速度
                    physics.setVelocityX(physics.getVelocityX() + velocityX);
                    physics.setVelocityY(physics.getVelocityY() + velocityY);
                    
                    // 限制总速度
                    double totalVelocity = Math.sqrt(physics.getVelocityX() * physics.getVelocityX() + 
                                                   physics.getVelocityY() * physics.getVelocityY());
                    if (totalVelocity > MAX_VELOCITY) {
                        double scale = MAX_VELOCITY / totalVelocity;
                        physics.setVelocityX(physics.getVelocityX() * scale);
                        physics.setVelocityY(physics.getVelocityY() * scale);
                    }
                    return;
                }
            } catch (Exception e) {
                // 如果获取物理组件失败，继续使用位置推挤
            }
        }
        
        // 如果启用位置推挤，使用直接位置变化
        if (com.roguelike.core.GameApp.COLLISION_POSITION_PUSH_ENABLED) {
            entity.translate(moveX, moveY);
        }
    }
    
    /**
     * 获取实体的边界框
     * @param entity 实体
     * @return 边界框
     */
    private Rectangle2D getEntityBounds(Entity entity) {
        if (entity.getBoundingBoxComponent() != null) {
            return new Rectangle2D(
                entity.getX() + entity.getBoundingBoxComponent().getMinXLocal(),
                entity.getY() + entity.getBoundingBoxComponent().getMinYLocal(),
                entity.getBoundingBoxComponent().getWidth(),
                entity.getBoundingBoxComponent().getHeight()
            );
        } else {
            return new Rectangle2D(entity.getX(), entity.getY(), entity.getWidth(), entity.getHeight());
        }
    }
    
    
    /**
     * 设置推挤力度系数
     * @param multiplier 力度系数 (0.1-2.0)
     */
    public void setPushForceMultiplier(double multiplier) {
        this.pushForceMultiplier = Math.max(0.1, Math.min(2.0, multiplier));
        System.out.println("⚡ 推挤力度系数设置为: " + this.pushForceMultiplier);
    }
    
    
    /**
     * 获取当前推挤力度系数
     * @return 力度系数
     */
    public double getPushForceMultiplier() {
        return pushForceMultiplier;
    }
    
    /**
     * 获取系统配置信息
     * @return 配置信息字符串
     */
    public String getConfigInfo() {
        return String.format("刚性碰撞系统配置:\n" +
                           "  - 碰撞检测精度: %.1f\n" +
                           "  - 最大分离距离: %.1f\n" +
                           "  - 推挤力度系数: %.2f\n" +
                           "  - 速度推挤力度: %.1f\n" +
                           "  - 最大速度限制: %.1f\n" +
                           "  - 位置推挤系数: %.2f",
                           COLLISION_PRECISION, MAX_SEPARATION_DISTANCE, pushForceMultiplier, 
                           VELOCITY_PUSH_FORCE, MAX_VELOCITY, POSITION_PUSH_FACTOR);
    }
}
