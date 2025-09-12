package com.roguelike.physics;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final double COLLISION_CHECK_INTERVAL = 1.0 / 60.0; // 60 FPS
    
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
        
        // 为实体碰撞检测器设置地图碰撞检测器
        if (entityCollisionDetector != null) {
            entityCollisionDetector.setMapCollisionDetector(detector);
        }
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
        
        // 执行所有碰撞检测
        performAllCollisionChecks();
    }
    
    /**
     * 执行所有碰撞检测
     */
    private void performAllCollisionChecks() {
        // 分类实体
        Player player = getPlayer();
        List<Enemy> enemies = getEnemies();
        List<Bullet> bullets = getBullets();
        
        // 执行各种碰撞检测
        if (enablePlayerEnemyCollision && player != null && !enemies.isEmpty()) {
            checkPlayerEnemyCollisions(player, enemies);
        }
        
        if (enableBulletEnemyCollision && !bullets.isEmpty() && !enemies.isEmpty()) {
            checkBulletEnemyCollisions(bullets, enemies);
        }
        
        if (enableBulletPlayerCollision && !bullets.isEmpty() && player != null) {
            checkBulletPlayerCollisions(bullets, player);
        }
        
        if (enableEnemyEnemyCollision && enemies.size() > 1) {
            checkEnemyEnemyCollisions(enemies);
        }
    }
    
    /**
     * 检测玩家与敌人的碰撞
     */
    private void checkPlayerEnemyCollisions(Player player, List<Enemy> enemies) {
        List<EntityCollisionDetector.CollisionResult> results = 
            entityCollisionDetector.checkPlayerEnemyCollisions(player, enemies);
        
        for (EntityCollisionDetector.CollisionResult result : results) {
            entityCollisionDetector.handleCollision(result);
        }
    }
    
    /**
     * 检测子弹与敌人的碰撞
     */
    private void checkBulletEnemyCollisions(List<Bullet> bullets, List<Enemy> enemies) {
        List<EntityCollisionDetector.CollisionResult> results = 
            entityCollisionDetector.checkBulletEnemyCollisions(bullets, enemies);
        
        for (EntityCollisionDetector.CollisionResult result : results) {
            entityCollisionDetector.handleCollision(result);
        }
    }
    
    /**
     * 检测子弹与玩家的碰撞
     */
    private void checkBulletPlayerCollisions(List<Bullet> bullets, Player player) {
        List<EntityCollisionDetector.CollisionResult> results = 
            entityCollisionDetector.checkBulletPlayerCollisions(bullets, player);
        
        for (EntityCollisionDetector.CollisionResult result : results) {
            entityCollisionDetector.handleCollision(result);
        }
    }
    
    /**
     * 检测敌人与敌人的碰撞
     */
    private void checkEnemyEnemyCollisions(List<Enemy> enemies) {
        for (int i = 0; i < enemies.size(); i++) {
            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy enemy1 = enemies.get(i);
                Enemy enemy2 = enemies.get(j);
                
                if (enemy1.isAlive() && enemy2.isAlive()) {
                    EntityCollisionDetector.CollisionResult result = 
                        entityCollisionDetector.checkCollision(enemy1, enemy2);
                    
                    if (result.hasCollision()) {
                        entityCollisionDetector.handleCollision(result);
                    }
                }
            }
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
     * 获取所有子弹实体
     */
    private List<Bullet> getBullets() {
        return FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Bullet)
                .map(e -> (Bullet) e)
                .filter(Bullet::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * 手动触发碰撞检测（用于测试或特殊情况）
     */
    public void forceCollisionCheck() {
        performAllCollisionChecks();
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
     * 清理资源
     */
    public void cleanup() {
        // 清理相关资源
        mapCollisionDetector = null;
        entityCollisionDetector = null;
        movementValidator = null;
    }
}
