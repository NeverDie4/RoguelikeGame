package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameApp;
import com.roguelike.map.MapRenderer;
import javafx.geometry.Rectangle2D;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体碰撞检测器
 * 负责检测实体之间的碰撞，包括玩家与敌人、子弹与敌人等
 */
public class EntityCollisionDetector {
    
    // 碰撞冷却时间记录
    private Map<String, Double> collisionCooldowns = new HashMap<>();
    private static final double COLLISION_COOLDOWN = 0.5; // 0.5秒冷却时间
    
    // 攻击间隔记录
    private Map<String, Double> attackCooldowns = new HashMap<>();
    private static final double ATTACK_COOLDOWN = 1.0; // 1秒攻击间隔
    
    // 地图碰撞检测器
    private MapCollisionDetector mapCollisionDetector;
    
    /**
     * 碰撞结果类
     */
    public static class CollisionResult {
        private final boolean hasCollision;
        private final Entity entity1;
        private final Entity entity2;
        private final CollisionType type;
        private final double overlapX;
        private final double overlapY;
        
        public CollisionResult(boolean hasCollision, Entity entity1, Entity entity2, 
                             CollisionType type, double overlapX, double overlapY) {
            this.hasCollision = hasCollision;
            this.entity1 = entity1;
            this.entity2 = entity2;
            this.type = type;
            this.overlapX = overlapX;
            this.overlapY = overlapY;
        }
        
        public boolean hasCollision() {
            return hasCollision;
        }
        
        public Entity getEntity1() {
            return entity1;
        }
        
        public Entity getEntity2() {
            return entity2;
        }
        
        public CollisionType getType() {
            return type;
        }
        
        public double getOverlapX() {
            return overlapX;
        }
        
        public double getOverlapY() {
            return overlapY;
        }
    }
    
    /**
     * 碰撞类型枚举
     */
    public enum CollisionType {
        PLAYER_ENEMY,    // 玩家与敌人
        BULLET_ENEMY,    // 子弹与敌人
        BULLET_PLAYER,   // 子弹与玩家
        ENEMY_ENEMY,     // 敌人与敌人
        PLAYER_PLAYER    // 玩家与玩家（通常不会发生）
    }
    
    /**
     * 设置地图碰撞检测器
     */
    public void setMapCollisionDetector(MapCollisionDetector detector) {
        this.mapCollisionDetector = detector;
    }
    
    /**
     * 检测两个实体是否发生碰撞
     */
    public CollisionResult checkCollision(Entity entity1, Entity entity2) {
        if (entity1 == null || entity2 == null || entity1 == entity2) {
            return new CollisionResult(false, entity1, entity2, null, 0, 0);
        }
        
        // 获取实体的边界框
        Rectangle2D bounds1 = getEntityBounds(entity1);
        Rectangle2D bounds2 = getEntityBounds(entity2);
        
        // 检查是否重叠
        if (!bounds1.intersects(bounds2)) {
            return new CollisionResult(false, entity1, entity2, null, 0, 0);
        }
        
        // 计算重叠区域
        double overlapX = Math.min(bounds1.getMaxX(), bounds2.getMaxX()) - 
                         Math.max(bounds1.getMinX(), bounds2.getMinX());
        double overlapY = Math.min(bounds1.getMaxY(), bounds2.getMaxY()) - 
                         Math.max(bounds1.getMinY(), bounds2.getMinY());
        
        // 确定碰撞类型
        CollisionType type = determineCollisionType(entity1, entity2);
        
        return new CollisionResult(true, entity1, entity2, type, overlapX, overlapY);
    }
    
    /**
     * 检测玩家与所有敌人的碰撞
     */
    public List<CollisionResult> checkPlayerEnemyCollisions(Player player, List<Enemy> enemies) {
        List<CollisionResult> results = new ArrayList<>();
        
        if (player == null || enemies == null) {
            return results;
        }
        
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                CollisionResult result = checkCollision(player, enemy);
                if (result.hasCollision()) {
                    results.add(result);
                }
            }
        }
        
        return results;
    }
    
    /**
     * 检测子弹与所有敌人的碰撞
     */
    public List<CollisionResult> checkBulletEnemyCollisions(List<Bullet> bullets, List<Enemy> enemies) {
        List<CollisionResult> results = new ArrayList<>();
        
        if (bullets == null || enemies == null) {
            return results;
        }
        
        for (Bullet bullet : bullets) {
            if (bullet == null || !bullet.isActive()) {
                continue;
            }
            
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive()) {
                    CollisionResult result = checkCollision(bullet, enemy);
                    if (result.hasCollision()) {
                        results.add(result);
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * 检测子弹与玩家的碰撞
     */
    public List<CollisionResult> checkBulletPlayerCollisions(List<Bullet> bullets, Player player) {
        List<CollisionResult> results = new ArrayList<>();
        
        if (bullets == null || player == null) {
            return results;
        }
        
        for (Bullet bullet : bullets) {
            if (bullet == null || !bullet.isActive()) {
                continue;
            }
            
            CollisionResult result = checkCollision(bullet, player);
            if (result.hasCollision()) {
                results.add(result);
            }
        }
        
        return results;
    }
    
    /**
     * 处理碰撞结果
     */
    public void handleCollision(CollisionResult result) {
        if (!result.hasCollision()) {
            return;
        }
        
        // 检查碰撞冷却
        if (isCollisionOnCooldown(result.getEntity1(), result.getEntity2())) {
            return;
        }
        
        switch (result.getType()) {
            case PLAYER_ENEMY:
                handlePlayerEnemyCollision(result);
                break;
            case BULLET_ENEMY:
                handleBulletEnemyCollision(result);
                break;
            case BULLET_PLAYER:
                handleBulletPlayerCollision(result);
                break;
            case ENEMY_ENEMY:
                handleEnemyEnemyCollision(result);
                break;
            case PLAYER_PLAYER:
                // 玩家与玩家碰撞通常不会发生，但可以在这里处理
                break;
        }
    }
    
    /**
     * 处理玩家与敌人的碰撞
     */
    private void handlePlayerEnemyCollision(CollisionResult result) {
        Player player = (Player) result.getEntity1();
        Enemy enemy = (Enemy) result.getEntity2();
        
        // 确保玩家是第一个实体
        if (!(result.getEntity1() instanceof Player)) {
            player = (Player) result.getEntity2();
            enemy = (Enemy) result.getEntity1();
        }
        
        // 检查实体是否仍然有效
        if (player == null || enemy == null || !enemy.isAlive()) {
            return;
        }
        
        // 检查攻击间隔
        if (isAttackOnCooldown(enemy, player)) {
            return; // 攻击仍在冷却时间内
        }
        
        // 玩家受到伤害（固定10点伤害）
        int damage = 10;
        player.takeDamage(damage);
        
        // 发布碰撞事件
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_ENEMY_COLLISION));
        
        // 不进行推离，只产生挤压效果
        // 玩家可以通过移动强制挤开敌人
    }
    
    /**
     * 处理子弹与敌人的碰撞
     */
    private void handleBulletEnemyCollision(CollisionResult result) {
        Bullet bullet = (Bullet) result.getEntity1();
        Enemy enemy = (Enemy) result.getEntity2();
        
        // 确保子弹是第一个实体
        if (!(result.getEntity1() instanceof Bullet)) {
            bullet = (Bullet) result.getEntity2();
            enemy = (Enemy) result.getEntity1();
        }
        
        // 根据调试开关决定是否造成伤害
        if (bullet.getFaction() == Bullet.Faction.PLAYER) {
            if (GameApp.BULLET_DAMAGE_ENABLED) {
                // 子弹造成伤害
                int damage = calculateBulletDamage(bullet);
                enemy.takeDamage(damage);
            }
            
            // 子弹击中后销毁
            bullet.setActive(false);
            
            // 发布碰撞事件
            GameEvent.post(new GameEvent(GameEvent.Type.BULLET_ENEMY_COLLISION));
        }
    }
    
    /**
     * 处理子弹与玩家的碰撞
     */
    private void handleBulletPlayerCollision(CollisionResult result) {
        Bullet bullet = (Bullet) result.getEntity1();
        Player player = (Player) result.getEntity2();
        
        // 确保子弹是第一个实体
        if (!(result.getEntity1() instanceof Bullet)) {
            bullet = (Bullet) result.getEntity2();
            player = (Player) result.getEntity1();
        }
        
        // 根据调试开关决定是否造成伤害
        if (bullet.getFaction() == Bullet.Faction.ENEMY) {
            if (GameApp.BULLET_DAMAGE_ENABLED) {
                // 子弹造成伤害
                int damage = calculateBulletDamage(bullet);
                player.takeDamage(damage);
            }
            
            // 子弹击中后销毁
            bullet.setActive(false);
            
            // 发布碰撞事件
            GameEvent.post(new GameEvent(GameEvent.Type.BULLET_PLAYER_COLLISION));
        }
    }
    
    /**
     * 处理敌人与敌人的碰撞（避免重叠）
     */
    private void handleEnemyEnemyCollision(CollisionResult result) {
        Enemy enemy1 = (Enemy) result.getEntity1();
        Enemy enemy2 = (Enemy) result.getEntity2();
        
        // 将两个敌人推离
        pushEntitiesAway(enemy1, enemy2, result);
        
        // 发布碰撞事件
        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_ENEMY_COLLISION));
    }
    
    /**
     * 将敌人推离玩家（玩家不会被推离）
     */
    private void pushEnemyAwayFromPlayer(Player player, Enemy enemy, CollisionResult result) {
        // 计算推离方向（从玩家指向敌人）
        double playerCenterX = player.getCenter().getX();
        double playerCenterY = player.getCenter().getY();
        double enemyCenterX = enemy.getCenter().getX();
        double enemyCenterY = enemy.getCenter().getY();
        
        double dx = enemyCenterX - playerCenterX; // 敌人远离玩家的方向
        double dy = enemyCenterY - playerCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            // 归一化方向
            dx /= distance;
            dy /= distance;
            
            // 推离距离 - 使用较小的固定距离避免瞬移
            double pushDistance = Math.min(Math.max(result.getOverlapX(), result.getOverlapY()) + 2.0, 8.0);
            
            // 检查推离后的位置是否与地图碰撞
            double newX = enemy.getX() + dx * pushDistance;
            double newY = enemy.getY() + dy * pushDistance;
            
            if (mapCollisionDetector != null && !mapCollisionDetector.canMoveTo(enemy, newX, newY)) {
                // 如果推离后会撞到障碍物，尝试找到安全的推离位置
                pushDistance = findSafePushDistance(enemy, dx, dy, pushDistance);
            }
            
            // 应用推离（只推离敌人）
            if (pushDistance > 0) {
                enemy.translate(dx * pushDistance, dy * pushDistance);
            }
        }
    }
    
    /**
     * 将玩家推离敌人（保留原方法，可能在其他地方使用）
     */
    private void pushPlayerAwayFromEnemy(Player player, Enemy enemy, CollisionResult result) {
        // 计算推离方向
        double playerCenterX = player.getCenter().getX();
        double playerCenterY = player.getCenter().getY();
        double enemyCenterX = enemy.getCenter().getX();
        double enemyCenterY = enemy.getCenter().getY();
        
        double dx = playerCenterX - enemyCenterX;
        double dy = playerCenterY - enemyCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            // 归一化方向
            dx /= distance;
            dy /= distance;
            
            // 推离距离 - 使用较小的固定距离避免瞬移
            double pushDistance = Math.min(Math.max(result.getOverlapX(), result.getOverlapY()) + 2.0, 10.0);
            
            // 检查推离后的位置是否与地图碰撞
            double newX = player.getX() + dx * pushDistance;
            double newY = player.getY() + dy * pushDistance;
            
            if (mapCollisionDetector != null && !mapCollisionDetector.canMoveTo(player, newX, newY)) {
                // 如果推离后会撞到障碍物，尝试找到安全的推离位置
                pushDistance = findSafePushDistance(player, dx, dy, pushDistance);
            }
            
            // 应用推离
            if (pushDistance > 0) {
                player.translate(dx * pushDistance, dy * pushDistance);
            }
        }
    }
    
    /**
     * 将两个实体推离
     */
    private void pushEntitiesAway(Entity entity1, Entity entity2, CollisionResult result) {
        // 计算推离方向
        double center1X = entity1.getCenter().getX();
        double center1Y = entity1.getCenter().getY();
        double center2X = entity2.getCenter().getX();
        double center2Y = entity2.getCenter().getY();
        
        double dx = center1X - center2X;
        double dy = center1Y - center2Y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            // 归一化方向
            dx /= distance;
            dy /= distance;
            
            // 推离距离 - 使用较小的固定距离避免瞬移
            double pushDistance = Math.min(Math.max(result.getOverlapX(), result.getOverlapY()) / 2.0 + 1.0, 5.0);
            
            // 检查推离后的位置是否与地图碰撞
            double newX1 = entity1.getX() + dx * pushDistance;
            double newY1 = entity1.getY() + dy * pushDistance;
            double newX2 = entity2.getX() - dx * pushDistance;
            double newY2 = entity2.getY() - dy * pushDistance;
            
            // 计算每个实体的安全推离距离
            double safePushDistance1 = pushDistance;
            double safePushDistance2 = pushDistance;
            
            if (mapCollisionDetector != null) {
                if (!mapCollisionDetector.canMoveTo(entity1, newX1, newY1)) {
                    safePushDistance1 = findSafePushDistance(entity1, dx, dy, pushDistance);
                }
                if (!mapCollisionDetector.canMoveTo(entity2, newX2, newY2)) {
                    safePushDistance2 = findSafePushDistance(entity2, -dx, -dy, pushDistance);
                }
            }
            
            // 应用推离
            if (safePushDistance1 > 0) {
                entity1.translate(dx * safePushDistance1, dy * safePushDistance1);
            }
            if (safePushDistance2 > 0) {
                entity2.translate(-dx * safePushDistance2, -dy * safePushDistance2);
            }
        }
    }
    
    /**
     * 计算敌人对玩家的伤害
     */
    private int calculateEnemyDamage(Enemy enemy) {
        // 基础伤害，可以根据敌人类型调整
        return 10;
    }
    
    /**
     * 计算子弹伤害
     */
    private int calculateBulletDamage(Bullet bullet) {
        // 基础伤害，可以根据子弹类型调整
        return 20;
    }
    
    /**
     * 获取实体的边界框
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
     * 确定碰撞类型
     */
    private CollisionType determineCollisionType(Entity entity1, Entity entity2) {
        boolean isPlayer1 = entity1 instanceof Player;
        boolean isPlayer2 = entity2 instanceof Player;
        boolean isEnemy1 = entity1 instanceof Enemy;
        boolean isEnemy2 = entity2 instanceof Enemy;
        boolean isBullet1 = entity1 instanceof Bullet;
        boolean isBullet2 = entity2 instanceof Bullet;
        
        if (isPlayer1 && isEnemy2) {
            return CollisionType.PLAYER_ENEMY;
        } else if (isEnemy1 && isPlayer2) {
            return CollisionType.PLAYER_ENEMY;
        } else if (isBullet1 && isEnemy2) {
            return CollisionType.BULLET_ENEMY;
        } else if (isEnemy1 && isBullet2) {
            return CollisionType.BULLET_ENEMY;
        } else if (isBullet1 && isPlayer2) {
            return CollisionType.BULLET_PLAYER;
        } else if (isPlayer1 && isBullet2) {
            return CollisionType.BULLET_PLAYER;
        } else if (isEnemy1 && isEnemy2) {
            return CollisionType.ENEMY_ENEMY;
        } else if (isPlayer1 && isPlayer2) {
            return CollisionType.PLAYER_PLAYER;
        }
        
        return null;
    }
    
    /**
     * 检查碰撞是否在冷却时间内
     */
    private boolean isCollisionOnCooldown(Entity entity1, Entity entity2) {
        String key = generateCollisionKey(entity1, entity2);
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        if (collisionCooldowns.containsKey(key)) {
            double lastCollisionTime = collisionCooldowns.get(key);
            if (currentTime - lastCollisionTime < COLLISION_COOLDOWN) {
                return true; // 仍在冷却时间内
            }
        }
        
        // 记录碰撞时间
        collisionCooldowns.put(key, currentTime);
        return false;
    }
    
    /**
     * 检查攻击是否在冷却时间内
     */
    private boolean isAttackOnCooldown(Entity attacker, Entity target) {
        String key = generateCollisionKey(attacker, target);
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        if (attackCooldowns.containsKey(key)) {
            double lastAttackTime = attackCooldowns.get(key);
            if (currentTime - lastAttackTime < ATTACK_COOLDOWN) {
                return true; // 仍在攻击冷却时间内
            }
        }
        
        // 记录攻击时间
        attackCooldowns.put(key, currentTime);
        return false;
    }
    
    /**
     * 生成碰撞键值（确保顺序无关）
     */
    private String generateCollisionKey(Entity entity1, Entity entity2) {
        // 使用实体ID确保唯一性，并排序确保顺序无关
        int id1 = entity1.hashCode();
        int id2 = entity2.hashCode();
        
        if (id1 < id2) {
            return id1 + "_" + id2;
        } else {
            return id2 + "_" + id1;
        }
    }
    
    /**
     * 找到安全的推离距离，避免撞到障碍物
     */
    private double findSafePushDistance(Entity entity, double directionX, double directionY, double maxDistance) {
        if (mapCollisionDetector == null) {
            return maxDistance;
        }
        
        // 使用二分查找找到最大安全距离
        double minDistance = 0;
        double maxDistanceFound = 0;
        double step = maxDistance / 10.0; // 分成10步检查
        
        for (double distance = 0; distance <= maxDistance; distance += step) {
            double testX = entity.getX() + directionX * distance;
            double testY = entity.getY() + directionY * distance;
            
            if (mapCollisionDetector.canMoveTo(entity, testX, testY)) {
                maxDistanceFound = distance;
            } else {
                break; // 遇到障碍物，停止
            }
        }
        
        return maxDistanceFound;
    }
}
