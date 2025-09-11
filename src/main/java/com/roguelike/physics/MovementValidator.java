package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;

/**
 * 移动验证器
 * 提供高级的移动验证和响应功能
 */
public class MovementValidator {
    
    private MapCollisionDetector collisionDetector;
    
    public MovementValidator(MapCollisionDetector collisionDetector) {
        this.collisionDetector = collisionDetector;
    }
    
    /**
     * 验证并执行移动，如果遇到障碍物则尝试滑动
     */
    public MovementResult validateAndMove(Entity entity, double deltaX, double deltaY) {
        // 首先尝试直接移动
        if (collisionDetector.checkMovementCollision(entity, deltaX, deltaY)) {
            return new MovementResult(true, deltaX, deltaY, MovementType.DIRECT);
        }
        
        // 如果直接移动失败，尝试分离移动（先X后Y或先Y后X）
        MovementResult result = trySeparateMovement(entity, deltaX, deltaY);
        if (result.isSuccess()) {
            return result;
        }
        
        // 如果分离移动也失败，尝试滑动移动
        return trySlidingMovement(entity, deltaX, deltaY);
    }
    
    /**
     * 尝试分离移动（分别处理X和Y轴移动）
     */
    private MovementResult trySeparateMovement(Entity entity, double deltaX, double deltaY) {
        // 尝试只移动X轴
        if (deltaX != 0 && collisionDetector.checkMovementCollision(entity, deltaX, 0)) {
            return new MovementResult(true, deltaX, 0, MovementType.SEPARATE_X);
        }
        
        // 尝试只移动Y轴
        if (deltaY != 0 && collisionDetector.checkMovementCollision(entity, 0, deltaY)) {
            return new MovementResult(true, 0, deltaY, MovementType.SEPARATE_Y);
        }
        
        return new MovementResult(false, 0, 0, MovementType.NONE);
    }
    
    /**
     * 尝试滑动移动（沿着障碍物表面滑动）
     */
    private MovementResult trySlidingMovement(Entity entity, double deltaX, double deltaY) {
        // 计算滑动方向
        Point2D slideDirection = calculateSlideDirection(entity, deltaX, deltaY);
        
        if (slideDirection.getX() != 0 || slideDirection.getY() != 0) {
            // 计算滑动距离
            double slideDistance = Math.min(Math.abs(deltaX), Math.abs(deltaY));
            double slideX = slideDirection.getX() * slideDistance;
            double slideY = slideDirection.getY() * slideDistance;
            
            if (collisionDetector.checkMovementCollision(entity, slideX, slideY)) {
                return new MovementResult(true, slideX, slideY, MovementType.SLIDING);
            }
        }
        
        return new MovementResult(false, 0, 0, MovementType.NONE);
    }
    
    /**
     * 计算滑动方向
     */
    private Point2D calculateSlideDirection(Entity entity, double deltaX, double deltaY) {
        // 检查X轴方向是否可以移动
        boolean canMoveX = collisionDetector.checkMovementCollision(entity, deltaX, 0);
        // 检查Y轴方向是否可以移动
        boolean canMoveY = collisionDetector.checkMovementCollision(entity, 0, deltaY);
        
        if (canMoveX && !canMoveY) {
            // 只能沿X轴移动
            return new Point2D(deltaX > 0 ? 1 : -1, 0);
        } else if (!canMoveX && canMoveY) {
            // 只能沿Y轴移动
            return new Point2D(0, deltaY > 0 ? 1 : -1);
        } else if (canMoveX && canMoveY) {
            // 两个方向都可以移动，返回原始方向
            return new Point2D(deltaX, deltaY).normalize();
        } else {
            // 两个方向都不能移动
            return new Point2D(0, 0);
        }
    }
    
    /**
     * 获取实体可以移动的最大距离
     */
    public double getMaxMoveDistance(Entity entity, double directionX, double directionY, double maxDistance) {
        return collisionDetector.getMaxMoveDistance(entity, directionX, directionY, maxDistance);
    }
    
    /**
     * 检查实体是否可以移动到指定位置
     */
    public boolean canMoveTo(Entity entity, double newX, double newY) {
        return collisionDetector.canMoveTo(entity, newX, newY);
    }
    
    /**
     * 移动结果类
     */
    public static class MovementResult {
        private final boolean success;
        private final double deltaX;
        private final double deltaY;
        private final MovementType type;
        
        public MovementResult(boolean success, double deltaX, double deltaY, MovementType type) {
            this.success = success;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.type = type;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public double getDeltaX() {
            return deltaX;
        }
        
        public double getDeltaY() {
            return deltaY;
        }
        
        public MovementType getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return String.format("MovementResult{success=%s, deltaX=%.2f, deltaY=%.2f, type=%s}", 
                               success, deltaX, deltaY, type);
        }
    }
    
    /**
     * 移动类型枚举
     */
    public enum MovementType {
        DIRECT,     // 直接移动
        SEPARATE_X, // 分离移动（X轴）
        SEPARATE_Y, // 分离移动（Y轴）
        SLIDING,    // 滑动移动
        NONE        // 无法移动
    }
}
