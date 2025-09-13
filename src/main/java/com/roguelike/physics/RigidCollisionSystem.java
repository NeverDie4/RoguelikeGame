package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

/**
 * 刚性碰撞系统
 * 实现物理上的物块碰撞，实体之间不能重叠
 */
public class RigidCollisionSystem {
    
    // 碰撞检测精度
    private static final double COLLISION_PRECISION = 0.1;
    
    // 最大分离距离
    private static final double MAX_SEPARATION_DISTANCE = 10.0;
    
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
        
        // 根据级别决定分离方向
        if (level1.canPush(level2)) {
            // entity1 推挤 entity2
            separateEntities(entity1, entity2, bounds1, bounds2);
        } else if (level2.canPush(level1)) {
            // entity2 推挤 entity1
            separateEntities(entity2, entity1, bounds2, bounds1);
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
        
        // 计算分离距离
        double separationDistance = Math.max(overlapX, overlapY) + COLLISION_PRECISION;
        separationDistance = Math.min(separationDistance, MAX_SEPARATION_DISTANCE);
        
        // 应用分离
        double moveX = direction.getX() * separationDistance;
        double moveY = direction.getY() * separationDistance;
        
        target.translate(moveX, moveY);
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
        
        // 计算分离距离（各移动一半）
        double separationDistance = Math.max(overlapX, overlapY) / 2.0 + COLLISION_PRECISION;
        separationDistance = Math.min(separationDistance, MAX_SEPARATION_DISTANCE / 2.0);
        
        // 应用分离
        double moveX1 = -direction.getX() * separationDistance;
        double moveY1 = -direction.getY() * separationDistance;
        double moveX2 = direction.getX() * separationDistance;
        double moveY2 = direction.getY() * separationDistance;
        
        entity1.translate(moveX1, moveY1);
        entity2.translate(moveX2, moveY2);
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
     * 获取系统配置信息
     * @return 配置信息字符串
     */
    public String getConfigInfo() {
        return String.format("刚性碰撞系统配置:\n" +
                           "  - 碰撞检测精度: %.1f\n" +
                           "  - 最大分离距离: %.1f\n" +
                           "  - 碰撞类型: 刚性物块碰撞",
                           COLLISION_PRECISION, MAX_SEPARATION_DISTANCE);
    }
}
