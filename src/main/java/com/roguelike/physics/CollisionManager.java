package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import java.util.List;
import java.util.ArrayList;

/**
 * 碰撞管理器
 * 统一管理游戏中的所有碰撞检测，包括地图碰撞和实体碰撞
 */
public class CollisionManager {
    
    private MapCollisionDetector mapCollisionDetector;
    private EntityCollisionDetector entityCollisionDetector;
    private MovementValidator movementValidator;
    
    // 碰撞检测配置
    private boolean enablePlayerEnemyCollision = true;
    private boolean enableBulletEnemyCollision = true;
    private boolean enableBulletPlayerCollision = true;
    private boolean enableEnemyEnemyCollision = true;
    
    // 碰撞检测频率控制
    private double lastCollisionCheckTime = 0;
    private static final double COLLISION_CHECK_INTERVAL = 1.0 / 30.0; // 30 FPS
    
    // 调试模式
    private boolean debugMode = false;
    
    public CollisionManager() {
        this.entityCollisionDetector = new EntityCollisionDetector();
    }
    
    /**
     * 设置地图碰撞检测器
     */
    public void setMapCollisionDetector(MapCollisionDetector detector) {
        this.mapCollisionDetector = detector;
        if (movementValidator == null) {
            this.movementValidator = new MovementValidator(detector);
        } else {
            // 更新现有验证器的碰撞检测器
            this.movementValidator = new MovementValidator(detector);
        }
        
        // 吸血鬼幸存者风格：不再需要为实体碰撞检测器设置地图碰撞检测器
        // 移除了复杂的推离逻辑
    }
    
    /**
     * 获取移动验证器
     */
    public MovementValidator getMovementValidator() {
        return movementValidator;
    }
    
    /**
     * 更新碰撞检测（由游戏主循环调用）
     */
    public void update(double tpf) {
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        // 控制碰撞检测频率
        if (currentTime - lastCollisionCheckTime < COLLISION_CHECK_INTERVAL) {
            return;
        }
        
        lastCollisionCheckTime = currentTime;
        
        // 使用新的碰撞检测系统
        if (entityCollisionDetector != null) {
            entityCollisionDetector.update(tpf);
        }
    }
    
    
    
    /**
     * 手动触发碰撞检测（用于测试或特殊情况）
     */
    public void forceCollisionCheck() {
        if (entityCollisionDetector != null) {
            entityCollisionDetector.update(0.016); // 模拟一帧的时间
        }
    }
    
    /**
     * 检查特定实体是否与地图碰撞
     */
    public boolean checkMapCollision(Entity entity, double deltaX, double deltaY) {
        if (mapCollisionDetector == null) {
            return true; // 没有地图碰撞检测器时允许移动
        }
        return mapCollisionDetector.checkMovementCollision(entity, deltaX, deltaY);
    }
    
    /**
     * 检查实体是否可以移动到指定位置
     */
    public boolean canMoveTo(Entity entity, double newX, double newY) {
        if (mapCollisionDetector == null) {
            return true; // 没有地图碰撞检测器时允许移动
        }
        return mapCollisionDetector.canMoveTo(entity, newX, newY);
    }
    
    /**
     * 获取实体可以移动的最大距离
     */
    public double getMaxMoveDistance(Entity entity, double directionX, double directionY, double maxDistance) {
        if (mapCollisionDetector == null) {
            return maxDistance; // 没有地图碰撞检测器时返回最大距离
        }
        return mapCollisionDetector.getMaxMoveDistance(entity, directionX, directionY, maxDistance);
    }
    
    /**
     * 设置碰撞检测开关
     */
    public void setPlayerEnemyCollisionEnabled(boolean enabled) {
        this.enablePlayerEnemyCollision = enabled;
    }
    
    public void setBulletEnemyCollisionEnabled(boolean enabled) {
        this.enableBulletEnemyCollision = enabled;
    }
    
    public void setBulletPlayerCollisionEnabled(boolean enabled) {
        this.enableBulletPlayerCollision = enabled;
    }
    
    public void setEnemyEnemyCollisionEnabled(boolean enabled) {
        this.enableEnemyEnemyCollision = enabled;
    }
    
    /**
     * 获取碰撞检测开关状态
     */
    public boolean isPlayerEnemyCollisionEnabled() {
        return enablePlayerEnemyCollision;
    }
    
    public boolean isBulletEnemyCollisionEnabled() {
        return enableBulletEnemyCollision;
    }
    
    public boolean isBulletPlayerCollisionEnabled() {
        return enableBulletPlayerCollision;
    }
    
    public boolean isEnemyEnemyCollisionEnabled() {
        return enableEnemyEnemyCollision;
    }
    
    /**
     * 获取实体碰撞检测器
     */
    public EntityCollisionDetector getEntityCollisionDetector() {
        return entityCollisionDetector;
    }
    
    /**
     * 获取地图碰撞检测器
     */
    public MapCollisionDetector getMapCollisionDetector() {
        return mapCollisionDetector;
    }
    
    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        if (entityCollisionDetector != null) {
            entityCollisionDetector.setDebugMode(debugMode);
        }
    }
    
    /**
     * 获取调试模式状态
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * 切换调试模式
     */
    public void toggleDebugMode() {
        setDebugMode(!debugMode);
        System.out.println("🔧 碰撞系统调试模式: " + (debugMode ? "开启" : "关闭"));
    }
    
    /**
     * 获取调试信息
     */
    public String getDebugInfo() {
        if (entityCollisionDetector != null) {
            return entityCollisionDetector.getDebugInfo();
        }
        return "碰撞检测器未初始化";
    }
    
    /**
     * 获取刚性碰撞系统
     */
    public RigidCollisionSystem getRigidCollisionSystem() {
        if (entityCollisionDetector != null) {
            return entityCollisionDetector.getRigidCollisionSystem();
        }
        return null;
    }
    
    /**
     * 获取调试网格
     */
    public List<javafx.scene.shape.Rectangle> getDebugGrid() {
        if (entityCollisionDetector != null) {
            return entityCollisionDetector.getDebugGrid();
        }
        return new ArrayList<>();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        // 清理相关资源
        mapCollisionDetector = null;
        entityCollisionDetector = null;
        movementValidator = null;
    }
}
