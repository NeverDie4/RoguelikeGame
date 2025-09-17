package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import com.almasb.fxgl.dsl.FXGL;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
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
    
    // 玩家伤害冷却系统
    private Map<String, Double> playerEnemyDamageCooldowns = new HashMap<>();
    private static final double PLAYER_DAMAGE_COOLDOWN = 0.4; // 0.4秒伤害间隔
    
    public OptimizedMovementValidator(MapCollisionDetector collisionDetector) {
        this.collisionDetector = collisionDetector;
    }
    
    /**
     * 验证并执行移动 - 玩家可推动敌人版本
     */
    public MovementResult validateAndMove(Entity entity, double deltaX, double deltaY) {
        // 更新实体缓存
        updateEntityCache();
        
        // 检查地图碰撞
        if (collisionDetector.checkMovementCollision(entity, deltaX, deltaY)) {
            // 地图碰撞通过，检查实体间碰撞
            if (hasEntityCollision(entity, deltaX, deltaY)) {
                // 有实体碰撞，尝试分离移动
                MovementResult result = trySeparateMovement(entity, deltaX, deltaY);
                if (result.isSuccess()) {
                    return result;
                }
                // 如果分离移动失败，尝试滑动移动
                return trySlidingMovement(entity, deltaX, deltaY);
            }
            
            // 如果移动的是玩家，处理推动敌人的逻辑
            if (entity instanceof Player) {
                handlePlayerPushEnemies(entity, deltaX, deltaY);
            }
            // 如果移动的是敌人，处理推动其他敌人的逻辑
            else if (entity instanceof Enemy) {
                handleEnemyPushEnemies((Enemy) entity, deltaX, deltaY);
            }
            
            // 没有实体碰撞，允许完全移动
            return new MovementResult(true, deltaX, deltaY, MovementType.DIRECT);
        } else {
            // 地图碰撞失败，尝试分离移动
            MovementResult result = trySeparateMovement(entity, deltaX, deltaY);
            if (result.isSuccess()) {
                return result;
            }
            // 尝试滑动移动
            return trySlidingMovement(entity, deltaX, deltaY);
        }
    }
    
    /**
     * 检查是否有实体碰撞 - 玩家可推动敌人版本
     */
    private boolean hasEntityCollision(Entity entity, double deltaX, double deltaY) {
        double newX = entity.getX() + deltaX;
        double newY = entity.getY() + deltaY;
        
        // 获取移动后的实体边界
        Rectangle2D entityBounds = getEntityBounds(entity, newX, newY);
        
        // 如果移动的是玩家，玩家可以推动敌人，所以不检查与敌人的碰撞
        if (entity instanceof Player) {
            // 玩家可以推动敌人，不检查与敌人的碰撞
            return false;
        }
        
        // 如果移动的是敌人
        if (entity instanceof Enemy) {
            // 检查与玩家的碰撞 - 敌人不能推动玩家
            if (cachedPlayer != null && !cachedPlayer.equals(entity)) {
                Rectangle2D playerBounds = getEntityBounds(cachedPlayer, cachedPlayer.getX(), cachedPlayer.getY());
                if (entityBounds.intersects(playerBounds)) {
                    return true;
                }
            }
            
            // 检查与其他敌人的碰撞 - 敌人之间保持刚性碰撞
            for (Enemy enemy : cachedEnemies) {
                if (!enemy.equals(entity)) {
                    Rectangle2D enemyBounds = getEntityBounds(enemy, enemy.getX(), enemy.getY());
                    if (entityBounds.intersects(enemyBounds)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 处理玩家推动敌人的逻辑 - 智能推动版本
     */
    private void handlePlayerPushEnemies(Entity player, double deltaX, double deltaY) {
        double newX = player.getX() + deltaX;
        double newY = player.getY() + deltaY;
        
        // 获取玩家移动后的边界
        Rectangle2D playerBounds = getEntityBounds(player, newX, newY);
        
        // 检查与所有敌人的碰撞，推动它们
        for (Enemy enemy : cachedEnemies) {
            Rectangle2D enemyBounds = getEntityBounds(enemy, enemy.getX(), enemy.getY());
            if (playerBounds.intersects(enemyBounds)) {
                // 计算推动距离（与玩家移动距离相同）
                double pushDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                if (pushDistance < 0.1) continue;
                
                // 计算推动方向（玩家移动的方向）
                double pushX = deltaX;
                double pushY = deltaY;
                
                // 标准化推动方向
                pushX = (pushX / pushDistance) * pushDistance;
                pushY = (pushY / pushDistance) * pushDistance;
                
                // 智能推动逻辑：同时向前和向两边推动
                double finalPushX = 0;
                double finalPushY = 0;
                
                // 计算垂直方向的推动（向两边）
                double perpendicularX = -pushY; // 垂直方向
                double perpendicularY = pushX;
                
                // 标准化垂直方向
                double perpDistance = Math.sqrt(perpendicularX * perpendicularX + perpendicularY * perpendicularY);
                if (perpDistance > 0) {
                    perpendicularX = (perpendicularX / perpDistance) * pushDistance;
                    perpendicularY = (perpendicularY / perpDistance) * pushDistance;
                }
                
                // 1. 尝试向前推动（玩家移动方向）
                boolean canPushForward = collisionDetector.checkMovementCollision(enemy, pushX, pushY);
                
                // 2. 尝试向两边推动
                boolean canPushLeft = collisionDetector.checkMovementCollision(enemy, perpendicularX, perpendicularY);
                boolean canPushRight = collisionDetector.checkMovementCollision(enemy, -perpendicularX, -perpendicularY);
                
                // 3. 组合推动：向前 + 向两边
                if (canPushForward) {
                    // 可以向前推动，同时尝试向两边推动
                    if (canPushLeft && canPushRight) {
                        // 两边都可以推动，选择推动距离更大的一边
                        double leftDistance = collisionDetector.getMaxMoveDistance(enemy, perpendicularX, perpendicularY, pushDistance);
                        double rightDistance = collisionDetector.getMaxMoveDistance(enemy, -perpendicularX, -perpendicularY, pushDistance);
                        
                        if (leftDistance >= rightDistance) {
                            // 向前 + 向左
                            finalPushX = pushX + perpendicularX * 0.5; // 50%的侧向推动
                            finalPushY = pushY + perpendicularY * 0.5;
                        } else {
                            // 向前 + 向右
                            finalPushX = pushX - perpendicularX * 0.5;
                            finalPushY = pushY - perpendicularY * 0.5;
                        }
                    } else if (canPushLeft) {
                        // 向前 + 向左
                        finalPushX = pushX + perpendicularX * 0.5;
                        finalPushY = pushY + perpendicularY * 0.5;
                    } else if (canPushRight) {
                        // 向前 + 向右
                        finalPushX = pushX - perpendicularX * 0.5;
                        finalPushY = pushY - perpendicularY * 0.5;
                    } else {
                        // 只能向前推动
                        finalPushX = pushX;
                        finalPushY = pushY;
                    }
                } else {
                    // 不能向前推动，只向两边推动
                    if (canPushLeft && canPushRight) {
                        // 两边都可以推动，选择推动距离更大的一边
                        double leftDistance = collisionDetector.getMaxMoveDistance(enemy, perpendicularX, perpendicularY, pushDistance);
                        double rightDistance = collisionDetector.getMaxMoveDistance(enemy, -perpendicularX, -perpendicularY, pushDistance);
                        
                        if (leftDistance >= rightDistance) {
                            finalPushX = perpendicularX * 0.8;
                            finalPushY = perpendicularY * 0.8;
                        } else {
                            finalPushX = -perpendicularX * 0.8;
                            finalPushY = -perpendicularY * 0.8;
                        }
                    } else if (canPushLeft) {
                        // 只能向左推动
                        double maxDistance = collisionDetector.getMaxMoveDistance(enemy, perpendicularX, perpendicularY, pushDistance);
                        finalPushX = perpendicularX * (maxDistance / pushDistance) * 0.8;
                        finalPushY = perpendicularY * (maxDistance / pushDistance) * 0.8;
                    } else if (canPushRight) {
                        // 只能向右推动
                        double maxDistance = collisionDetector.getMaxMoveDistance(enemy, -perpendicularX, -perpendicularY, pushDistance);
                        finalPushX = -perpendicularX * (maxDistance / pushDistance) * 0.8;
                        finalPushY = -perpendicularY * (maxDistance / pushDistance) * 0.8;
                    } else {
                        // 两边都不能推动，跳过
                        continue;
                    }
                }
                
                // 检查推动距离是否有效
                if (Math.abs(finalPushX) < 0.1 && Math.abs(finalPushY) < 0.1) {
                    continue;
                }
                
                // 检查敌人被推动后是否会与其他敌人碰撞
                if (!hasEnemyCollisionWithOthers(enemy, finalPushX, finalPushY)) {
                    // 推动敌人
                    enemy.translate(finalPushX, finalPushY);
                    
                    // 检查推动后是否与不可通行方块重合，如果是则扣血
                    checkEnemyWallCollisionDamage(enemy);
                }
                
                // 玩家冲撞敌人时扣血
                handlePlayerCollisionDamage((Player) player, enemy);
            }
        }
    }
    
    /**
     * 检查敌人移动后是否与其他敌人碰撞（不包括玩家）
     */
    private boolean hasEnemyCollisionWithOthers(Enemy movingEnemy, double deltaX, double deltaY) {
        return hasEnemyCollisionWithOthers(movingEnemy, deltaX, deltaY, null);
    }
    
    /**
     * 检查敌人移动后是否与其他敌人碰撞（不包括玩家和指定的敌人）
     */
    private boolean hasEnemyCollisionWithOthers(Enemy movingEnemy, double deltaX, double deltaY, Enemy excludeEnemy) {
        double newX = movingEnemy.getX() + deltaX;
        double newY = movingEnemy.getY() + deltaY;
        
        // 获取移动后的敌人边界
        Rectangle2D enemyBounds = getEntityBounds(movingEnemy, newX, newY);
        
        // 检查与其他敌人的碰撞
        for (Enemy enemy : cachedEnemies) {
            if (!enemy.equals(movingEnemy) && !enemy.equals(excludeEnemy)) {
                Rectangle2D otherEnemyBounds = getEntityBounds(enemy, enemy.getX(), enemy.getY());
                if (enemyBounds.intersects(otherEnemyBounds)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 处理敌人推动其他敌人的逻辑
     */
    private void handleEnemyPushEnemies(Enemy movingEnemy, double deltaX, double deltaY) {
        double newX = movingEnemy.getX() + deltaX;
        double newY = movingEnemy.getY() + deltaY;
        
        // 获取移动敌人移动后的边界
        Rectangle2D movingEnemyBounds = getEntityBounds(movingEnemy, newX, newY);
        
        // 检查与所有其他敌人的碰撞，推动它们
        for (Enemy enemy : cachedEnemies) {
            if (!enemy.equals(movingEnemy)) {
                Rectangle2D enemyBounds = getEntityBounds(enemy, enemy.getX(), enemy.getY());
                if (movingEnemyBounds.intersects(enemyBounds)) {
                    // 计算推动方向（移动敌人的移动方向）
                    double pushX = deltaX;
                    double pushY = deltaY;
                    
                    // 计算推动距离（与移动敌人移动距离相同）
                    double pushDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    
                    // 标准化推动方向
                    if (pushDistance > 0) {
                        pushX = (pushX / pushDistance) * pushDistance;
                        pushY = (pushY / pushDistance) * pushDistance;
                    }
                    
                    // 使用更精确的推动逻辑，确保不会穿过不可通行的方块
                    double safePushX = 0;
                    double safePushY = 0;
                    
                    // 敌人推动敌人不需要检查地图碰撞，直接使用推动距离
                    safePushX = pushX;
                    safePushY = pushY;
                    
                    // 再次检查安全推动距离是否有效
                    if (Math.abs(safePushX) < 0.1 && Math.abs(safePushY) < 0.1) {
                        // 推动距离太小，跳过
                        continue;
                    }
                    
                    // 检查敌人被推动后是否会与其他敌人碰撞（不包括移动的敌人）
                    if (!hasEnemyCollisionWithOthers(enemy, safePushX, safePushY, movingEnemy)) {
                        // 推动敌人
                        enemy.translate(safePushX, safePushY);
                    }
                }
            }
        }
    }
    
    /**
     * 尝试分离移动（分别处理X和Y轴移动） - 刚性碰撞版本
     */
    private MovementResult trySeparateMovement(Entity entity, double deltaX, double deltaY) {
        // 尝试只移动X轴
        if (deltaX != 0 && collisionDetector.checkMovementCollision(entity, deltaX, 0)) {
            if (!hasEntityCollision(entity, deltaX, 0)) {
                return new MovementResult(true, deltaX, 0, MovementType.SEPARATE_X);
            }
        }
        
        // 尝试只移动Y轴
        if (deltaY != 0 && collisionDetector.checkMovementCollision(entity, 0, deltaY)) {
            if (!hasEntityCollision(entity, 0, deltaY)) {
                return new MovementResult(true, 0, deltaY, MovementType.SEPARATE_Y);
            }
        }
        
        return new MovementResult(false, 0, 0, MovementType.NONE);
    }
    
    /**
     * 尝试滑动移动（沿着障碍物表面滑动） - 刚性碰撞版本
     */
    private MovementResult trySlidingMovement(Entity entity, double deltaX, double deltaY) {
        // 计算滑动方向
        Point2D slideDirection = calculateSlideDirection(entity, deltaX, deltaY);
        
        if (slideDirection.getX() != 0 || slideDirection.getY() != 0) {
            // 计算滑动距离，使用原始距离
            double slideDistance = Math.min(Math.abs(deltaX), Math.abs(deltaY));
            double slideX = slideDirection.getX() * slideDistance;
            double slideY = slideDirection.getY() * slideDistance;
            
            if (collisionDetector.checkMovementCollision(entity, slideX, slideY) && 
                !hasEntityCollision(entity, slideX, slideY)) {
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
     * 检查敌人是否与不可通行方块重合，如果是则扣血
     */
    private void checkEnemyWallCollisionDamage(Enemy enemy) {
        // 检查敌人当前位置是否与不可通行方块重合
        if (!collisionDetector.checkMovementCollision(enemy, 0, 0)) {
            // 敌人与不可通行方块重合，扣血
            int damage = 5; // 每次扣5点血
            System.out.println("💥 敌人被挤压，扣血 " + damage + " 点，当前血量: " + enemy.getCurrentHP() + "/" + enemy.getMaxHP());
            enemy.takeDamage(damage);
        }
    }
    
    /**
     * 检查敌人是否处于墙壁内，如果是则扣血
     */
    public void checkEnemyInWallDamage(Enemy enemy) {
        // 检查敌人当前位置是否与不可通行方块重合
        if (!collisionDetector.checkMovementCollision(enemy, 0, 0)) {
            // 敌人处于墙壁内，扣血
            int damage = 5; // 每次扣5点血
            //System.out.println("💥 敌人处于墙壁内，扣血 " + damage + " 点，当前血量: " + enemy.getCurrentHP() + "/" + enemy.getMaxHP());
            enemy.takeDamage(damage, true); // 静音：不播放受击音效
        }
    }
    
    /**
     * 处理玩家冲撞敌人时的扣血逻辑
     */
    private void handlePlayerCollisionDamage(Player player, Enemy enemy) {
        // 检查伤害冷却（每个敌人独立）
        if (isPlayerDamageOnCooldown(enemy, player)) {
            return; // 伤害仍在冷却时间内
        }
        
        // 玩家冲撞敌人时扣血
        int damage = 10; // 每次扣10点血
        //System.out.println("💥 玩家冲撞敌人，玩家扣血 " + damage + " 点，当前血量: " + player.getGameState().getPlayerHP());
        player.takeDamage(damage);
        
        // 设置伤害冷却
        setPlayerDamageCooldown(enemy, player);
    }
    
    /**
     * 检查玩家伤害是否在冷却时间内
     */
    private boolean isPlayerDamageOnCooldown(Enemy enemy, Player player) {
        String key = enemy.hashCode() + "_" + player.hashCode();
        Double lastDamageTime = playerEnemyDamageCooldowns.get(key);
        if (lastDamageTime == null) {
            return false;
        }
        return (System.currentTimeMillis() / 1000.0) - lastDamageTime < PLAYER_DAMAGE_COOLDOWN;
    }
    
    /**
     * 设置玩家伤害冷却时间
     */
    private void setPlayerDamageCooldown(Enemy enemy, Player player) {
        String key = enemy.hashCode() + "_" + player.hashCode();
        playerEnemyDamageCooldowns.put(key, System.currentTimeMillis() / 1000.0);
    }
    
    /**
     * 移动类型枚举 - 刚性碰撞版本
     */
    public enum MovementType {
        DIRECT,     // 直接移动
        SEPARATE_X, // 分离移动（X轴）
        SEPARATE_Y, // 分离移动（Y轴）
        SLIDING,    // 滑动移动
        NONE        // 无法移动
    }
}