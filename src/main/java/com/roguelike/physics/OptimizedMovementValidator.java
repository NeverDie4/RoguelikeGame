package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import com.almasb.fxgl.dsl.FXGL;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 优化的移动验证器 - 保持原有碰撞风格
 * 只优化性能，不改变碰撞行为
 */
public class OptimizedMovementValidator {
    
    private MapCollisionDetector collisionDetector;
    
    // 实体缓存系统 - 性能优化
    private Player cachedPlayer = null;
    private List<Enemy> cachedEnemies = new ArrayList<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 50; // 50ms更新一次缓存
    
    public OptimizedMovementValidator(MapCollisionDetector collisionDetector) {
        this.collisionDetector = collisionDetector;
    }
    
    /**
     * 验证并执行移动 - 保持原有逻辑
     */
    public MovementResult validateAndMove(Entity entity, double deltaX, double deltaY) {
        // 更新实体缓存
        updateEntityCache();
        
        // 检查地图碰撞
        if (!collisionDetector.checkMovementCollision(entity, deltaX, deltaY)) {
            // 地图碰撞失败，尝试分离移动
            MovementResult result = trySeparateMovement(entity, deltaX, deltaY);
            if (result.isSuccess()) {
                return result;
            }
            // 尝试滑动移动
            return trySlidingMovement(entity, deltaX, deltaY);
        }
        
        // 检查实体间碰撞，如果碰撞则尝试挤压移动 - 保持原有逻辑
        SqueezeResult squeezeResult = checkEntitySqueeze(entity, deltaX, deltaY);
        if (squeezeResult.hasCollision()) {
            // 有实体碰撞，应用挤压移动
            return new MovementResult(true, squeezeResult.getDeltaX(), squeezeResult.getDeltaY(), MovementType.SQUEEZING);
        }
        
        // 没有碰撞，允许移动
        return new MovementResult(true, deltaX, deltaY, MovementType.DIRECT);
    }
    
    /**
     * 尝试分离移动（分别处理X和Y轴移动） - 保持原有逻辑
     */
    private MovementResult trySeparateMovement(Entity entity, double deltaX, double deltaY) {
        // 尝试只移动X轴
        if (deltaX != 0 && collisionDetector.checkMovementCollision(entity, deltaX, 0)) {
            SqueezeResult squeezeResult = checkEntitySqueeze(entity, deltaX, 0);
            if (squeezeResult.hasCollision()) {
                return new MovementResult(true, squeezeResult.getDeltaX(), 0, MovementType.SQUEEZING);
            } else {
                return new MovementResult(true, deltaX, 0, MovementType.SEPARATE_X);
            }
        }
        
        // 尝试只移动Y轴
        if (deltaY != 0 && collisionDetector.checkMovementCollision(entity, 0, deltaY)) {
            SqueezeResult squeezeResult = checkEntitySqueeze(entity, 0, deltaY);
            if (squeezeResult.hasCollision()) {
                return new MovementResult(true, 0, squeezeResult.getDeltaY(), MovementType.SQUEEZING);
            } else {
                return new MovementResult(true, 0, deltaY, MovementType.SEPARATE_Y);
            }
        }
        
        return new MovementResult(false, 0, 0, MovementType.NONE);
    }
    
    /**
     * 尝试滑动移动（沿着障碍物表面滑动） - 保持原有逻辑
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
     * 计算滑动方向 - 保持原有逻辑
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
     * 检查实体移动后是否与其他实体碰撞，并计算挤压移动 - 保持原有逻辑，但使用缓存优化
     */
    private SqueezeResult checkEntitySqueeze(Entity entity, double deltaX, double deltaY) {
        double newX = entity.getX() + deltaX;
        double newY = entity.getY() + deltaY;
        
        // 获取移动后的实体边界
        Rectangle2D entityBounds = getEntityBounds(entity, newX, newY);
        
        double totalSqueezeFactor = 1.0; // 初始挤压系数
        double adjustedDeltaX = deltaX;
        double adjustedDeltaY = deltaY;
        
        // 检查与玩家的碰撞 - 使用缓存的玩家
        if (entity instanceof Enemy) {
            if (cachedPlayer != null && !cachedPlayer.equals(entity)) {
                Rectangle2D playerBounds = getEntityBounds(cachedPlayer, cachedPlayer.getX(), cachedPlayer.getY());
                if (entityBounds.intersects(playerBounds)) {
                    // 计算挤压系数
                    double squeezeFactor = calculateSqueezeFactor(entityBounds, playerBounds);
                    totalSqueezeFactor = Math.min(totalSqueezeFactor, squeezeFactor);
                }
            }
        }
        
        // 检查与其他敌人的碰撞 - 使用缓存的敌人列表
        if (entity instanceof Enemy) {
            for (Enemy enemy : cachedEnemies) {
                if (!enemy.equals(entity)) {
                    Rectangle2D enemyBounds = getEntityBounds(enemy, enemy.getX(), enemy.getY());
                    if (entityBounds.intersects(enemyBounds)) {
                        // 计算挤压系数
                        double squeezeFactor = calculateSqueezeFactor(entityBounds, enemyBounds);
                        totalSqueezeFactor = Math.min(totalSqueezeFactor, squeezeFactor);
                    }
                }
            }
        }
        
        // 检查玩家与其他敌人的碰撞 - 使用缓存的敌人列表
        if (entity instanceof Player) {
            for (Enemy enemy : cachedEnemies) {
                Rectangle2D enemyBounds = getEntityBounds(enemy, enemy.getX(), enemy.getY());
                if (entityBounds.intersects(enemyBounds)) {
                    // 计算挤压系数
                    double squeezeFactor = calculateSqueezeFactor(entityBounds, enemyBounds);
                    totalSqueezeFactor = Math.min(totalSqueezeFactor, squeezeFactor);
                }
            }
        }
        
        // 应用挤压系数
        if (totalSqueezeFactor < 1.0) {
            adjustedDeltaX *= totalSqueezeFactor;
            adjustedDeltaY *= totalSqueezeFactor;
            return new SqueezeResult(true, adjustedDeltaX, adjustedDeltaY, totalSqueezeFactor);
        }
        
        return new SqueezeResult(false, deltaX, deltaY, 1.0); // 无碰撞，允许完全移动
    }
    
    /**
     * 计算挤压系数 - 保持原有逻辑
     */
    private double calculateSqueezeFactor(Rectangle2D entityBounds, Rectangle2D obstacleBounds) {
        // 计算重叠区域
        double overlapX = Math.min(entityBounds.getMaxX(), obstacleBounds.getMaxX()) - 
                         Math.max(entityBounds.getMinX(), obstacleBounds.getMinX());
        double overlapY = Math.min(entityBounds.getMaxY(), obstacleBounds.getMaxY()) - 
                         Math.max(entityBounds.getMinY(), obstacleBounds.getMinY());
        
        // 计算重叠比例
        double overlapRatioX = overlapX / entityBounds.getWidth();
        double overlapRatioY = overlapY / entityBounds.getHeight();
        
        // 使用最大重叠比例作为挤压系数
        double maxOverlapRatio = Math.max(overlapRatioX, overlapRatioY);
        
        // 挤压系数：重叠越多，移动越慢
        // 完全重叠时系数为0.1（几乎停止），无重叠时系数为1.0（正常移动）
        double squeezeFactor = Math.max(0.1, 1.0 - maxOverlapRatio * 0.9);
        
        return squeezeFactor;
    }
    
    /**
     * 更新实体缓存 - 性能优化
     */
    private void updateEntityCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate < CACHE_UPDATE_INTERVAL) {
            return;
        }
        
        // 更新玩家缓存
        cachedPlayer = getPlayer();
        
        // 更新敌人缓存
        cachedEnemies = getEnemies();
        
        lastCacheUpdate = currentTime;
    }
    
    /**
     * 获取实体在指定位置的边界框
     */
    private Rectangle2D getEntityBounds(Entity entity, double x, double y) {
        if (entity.getBoundingBoxComponent() != null) {
            return new Rectangle2D(
                x + entity.getBoundingBoxComponent().getMinXLocal(),
                y + entity.getBoundingBoxComponent().getMinYLocal(),
                entity.getBoundingBoxComponent().getWidth(),
                entity.getBoundingBoxComponent().getHeight()
            );
        } else {
            return new Rectangle2D(x, y, entity.getWidth(), entity.getHeight());
        }
    }
    
    /**
     * 获取玩家实体
     */
    private Player getPlayer() {
        return FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取所有敌人实体
     */
    private List<Enemy> getEnemies() {
        return FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Enemy)
                .map(e -> (Enemy) e)
                .filter(Enemy::isAlive)
                .collect(Collectors.toList());
    }
    
    /**
     * 移动结果类 - 保持原有结构
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
     * 挤压结果类 - 保持原有结构
     */
    public static class SqueezeResult {
        private final boolean hasCollision;
        private final double deltaX;
        private final double deltaY;
        private final double squeezeFactor; // 挤压系数 (0.0-1.0)
        
        public SqueezeResult(boolean hasCollision, double deltaX, double deltaY, double squeezeFactor) {
            this.hasCollision = hasCollision;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.squeezeFactor = squeezeFactor;
        }
        
        public boolean hasCollision() {
            return hasCollision;
        }
        
        public double getDeltaX() {
            return deltaX;
        }
        
        public double getDeltaY() {
            return deltaY;
        }
        
        public double getSqueezeFactor() {
            return squeezeFactor;
        }
        
        @Override
        public String toString() {
            return String.format("SqueezeResult{collision=%s, deltaX=%.2f, deltaY=%.2f, factor=%.2f}", 
                               hasCollision, deltaX, deltaY, squeezeFactor);
        }
    }
    
    /**
     * 移动类型枚举 - 保持原有类型
     */
    public enum MovementType {
        DIRECT,     // 直接移动
        SEPARATE_X, // 分离移动（X轴）
        SEPARATE_Y, // 分离移动（Y轴）
        SLIDING,    // 滑动移动
        SQUEEZING,  // 挤压移动
        NONE        // 无法移动
    }
}