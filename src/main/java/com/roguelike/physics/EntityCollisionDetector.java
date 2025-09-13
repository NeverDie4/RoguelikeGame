package com.roguelike.physics;

import com.almasb.fxgl.entity.Entity;
import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;
import com.roguelike.core.GameEvent;
import javafx.geometry.Rectangle2D;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实体碰撞检测器 - 重新设计
 * 负责检测实体之间的碰撞，包括玩家与敌人、子弹与敌人等
 * 实现碰撞箱级别系统和空间分割优化
 */
public class EntityCollisionDetector {
    
    // 空间分割系统
    private SpatialPartitionSystem spatialSystem;
    
    // 刚性碰撞系统
    private RigidCollisionSystem rigidCollisionSystem;
    
    // 碰撞冷却时间记录
    private Map<String, Double> collisionCooldowns = new HashMap<>();
    private static final double COLLISION_COOLDOWN = 0.1; // 0.1秒冷却时间
    
    // 攻击间隔记录
    private Map<String, Double> attackCooldowns = new HashMap<>();
    private static final double ATTACK_COOLDOWN = 0.2; // 0.2秒攻击间隔
    
    // 玩家与敌人碰撞伤害间隔记录（每个敌人独立）
    private Map<String, Double> playerEnemyDamageCooldowns = new HashMap<>();
    private static final double PLAYER_DAMAGE_COOLDOWN = 0.4; // 0.4秒伤害间隔
    private static final int PLAYER_DAMAGE_AMOUNT = 10; // 每次扣血10点
    
    // 更新间隔控制
    private double lastUpdateTime = 0;
    private static final double DEFAULT_UPDATE_INTERVAL = 0.016; // 默认16ms更新间隔（约60FPS）
    
    // 调试模式
    private boolean debugMode = false;
    
    public EntityCollisionDetector() {
        this.spatialSystem = new SpatialPartitionSystem();
        this.rigidCollisionSystem = new RigidCollisionSystem();
    }
    
    /**
     * 更新碰撞检测系统
     */
    public void update(double tpf) {
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        // 控制更新频率（使用调试参数）
        double updateInterval = com.roguelike.core.GameApp.COLLISION_UPDATE_INTERVAL;
        if (currentTime - lastUpdateTime < updateInterval) {
            return;
        }
        
        lastUpdateTime = currentTime;
        
        // 更新空间分割系统
        updateSpatialPartitions();
        
        // 执行碰撞检测
        performCollisionChecks();
        
        // 清理过期的冷却记录
        cleanupExpiredCooldowns();
    }
    
    /**
     * 更新空间分割
     */
    private void updateSpatialPartitions() {
        spatialSystem.clear();
        
        // 更新玩家位置
        Player player = getPlayer();
        if (player != null) {
            spatialSystem.updateEntity(player);
        }
        
        // 更新敌人位置
        List<Enemy> enemies = getEnemies();
        for (Enemy enemy : enemies) {
            spatialSystem.updateEntity(enemy);
        }
        
        // 更新子弹位置
        List<Bullet> bullets = getBullets();
        for (Bullet bullet : bullets) {
            spatialSystem.updateEntity(bullet);
        }
    }
    
    /**
     * 执行碰撞检测
     */
    private void performCollisionChecks() {
        // 获取所有实体
        Player player = getPlayer();
        List<Enemy> enemies = getEnemies();
        List<Bullet> bullets = getBullets();
        
        // 检测玩家与敌人碰撞
        if (player != null && !enemies.isEmpty()) {
            checkPlayerEnemyCollisions(player, enemies);
        }
        
        // 检测子弹与敌人碰撞
        if (!bullets.isEmpty() && !enemies.isEmpty()) {
            checkBulletEnemyCollisions(bullets, enemies);
        }
        
        // 检测子弹与玩家碰撞
        if (!bullets.isEmpty() && player != null) {
            checkBulletPlayerCollisions(bullets, player);
        }
        
        // 检测敌人与敌人碰撞
        if (enemies.size() > 1) {
            checkEnemyEnemyCollisions(enemies);
        }
    }
    
    /**
     * 检测玩家与敌人的碰撞
     */
    private void checkPlayerEnemyCollisions(Player player, List<Enemy> enemies) {
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isAlive()) {
                CollisionResult result = checkCollision(player, enemy);
                if (result.hasCollision()) {
                    handleCollisionWithLevels(result);
                }
            }
        }
    }
    
    /**
     * 检测子弹与敌人的碰撞
     */
    private void checkBulletEnemyCollisions(List<Bullet> bullets, List<Enemy> enemies) {
        for (Bullet bullet : bullets) {
            if (bullet == null || !bullet.isActive()) {
                continue;
            }
            
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive()) {
                    CollisionResult result = checkCollision(bullet, enemy);
                    if (result.hasCollision()) {
                        handleBulletCollision(result);
                    }
                }
            }
        }
    }
    
    /**
     * 检测子弹与玩家的碰撞
     */
    private void checkBulletPlayerCollisions(List<Bullet> bullets, Player player) {
        for (Bullet bullet : bullets) {
            if (bullet == null || !bullet.isActive()) {
                continue;
            }
            
            CollisionResult result = checkCollision(bullet, player);
            if (result.hasCollision()) {
                handleBulletCollision(result);
            }
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
                    CollisionResult result = checkCollision(enemy1, enemy2);
                    if (result.hasCollision()) {
                        handleCollisionWithLevels(result);
                    }
                }
            }
        }
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
     * 处理带级别的碰撞
     */
    private void handleCollisionWithLevels(CollisionResult result) {
        Entity entity1 = result.getEntity1();
        Entity entity2 = result.getEntity2();
        
        CollisionBoxLevel level1 = getCollisionLevel(entity1);
        CollisionBoxLevel level2 = getCollisionLevel(entity2);
        
        // 只有当两个实体都有有效的碰撞箱级别时才进行刚性碰撞
        if (level1 != null && level2 != null) {
            // 使用刚性碰撞系统
            rigidCollisionSystem.handleRigidCollision(entity1, entity2, level1, level2);
        }
        
        // 处理其他碰撞逻辑（伤害等）
        handleCollisionEffects(result);
    }
    
    /**
     * 处理子弹碰撞（子弹击中后消失）
     */
    private void handleBulletCollision(CollisionResult result) {
        Bullet bullet = null;
        Entity target = null;
        
        if (result.getEntity1() instanceof Bullet) {
            bullet = (Bullet) result.getEntity1();
            target = result.getEntity2();
        } else if (result.getEntity2() instanceof Bullet) {
            bullet = (Bullet) result.getEntity2();
            target = result.getEntity1();
        }
        
        if (bullet != null && target != null) {
            // 子弹击中后消失
            bullet.setActive(false);
            
            // 触发后续效果（伤害等）
            if (bullet.getFaction() == Bullet.Faction.PLAYER && target instanceof Enemy) {
                ((Enemy) target).takeDamage(bullet.getDamage());
                GameEvent.post(new GameEvent(GameEvent.Type.BULLET_ENEMY_COLLISION));
            } else if (bullet.getFaction() == Bullet.Faction.ENEMY && target instanceof Player) {
                ((Player) target).takeDamage(bullet.getDamage());
                GameEvent.post(new GameEvent(GameEvent.Type.BULLET_PLAYER_COLLISION));
            }
        }
    }
    
    /**
     * 处理碰撞效果（伤害等）
     */
    private void handleCollisionEffects(CollisionResult result) {
        // 检查碰撞冷却
        if (isCollisionOnCooldown(result.getEntity1(), result.getEntity2())) {
            return;
        }
        
        switch (result.getType()) {
            case PLAYER_ENEMY:
                handlePlayerEnemyCollision(result);
                break;
            case ENEMY_ENEMY:
                handleEnemyEnemyCollision(result);
                break;
            default:
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
        
        // 检查伤害间隔（每个敌人独立）
        if (isPlayerDamageOnCooldown(enemy, player)) {
            return; // 伤害仍在冷却时间内
        }
        
        // 玩家受到伤害（固定10点伤害）
        player.takeDamage(PLAYER_DAMAGE_AMOUNT);
        
        // 发布碰撞事件
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_ENEMY_COLLISION));
    }
    
    /**
     * 处理敌人与敌人的碰撞
     */
    private void handleEnemyEnemyCollision(CollisionResult result) {
        // 发布碰撞事件
        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_ENEMY_COLLISION));
    }
    
    /**
     * 获取实体的碰撞箱级别
     */
    private CollisionBoxLevel getCollisionLevel(Entity entity) {
        if (entity instanceof Player) {
            return CollisionBoxLevel.PLAYER;
        } else if (entity instanceof Enemy) {
            return CollisionBoxLevel.ENEMY;
        } else if (entity instanceof Bullet) {
            // 子弹不参与推挤系统，返回null
            return null;
        }
        return CollisionBoxLevel.ENEMY; // 默认级别
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
     * 检查玩家伤害是否在冷却时间内（每个敌人独立）
     */
    private boolean isPlayerDamageOnCooldown(Entity enemy, Entity player) {
        String key = generateCollisionKey(enemy, player);
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        if (playerEnemyDamageCooldowns.containsKey(key)) {
            double lastDamageTime = playerEnemyDamageCooldowns.get(key);
            if (currentTime - lastDamageTime < PLAYER_DAMAGE_COOLDOWN) {
                return true; // 仍在伤害冷却时间内
            }
        }
        
        // 记录伤害时间
        playerEnemyDamageCooldowns.put(key, currentTime);
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
     * 清理过期的冷却记录
     */
    private void cleanupExpiredCooldowns() {
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        
        // 清理碰撞冷却
        collisionCooldowns.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > COLLISION_COOLDOWN * 2
        );
        
        // 清理攻击冷却
        attackCooldowns.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > ATTACK_COOLDOWN * 2
        );
        
        // 清理玩家伤害冷却
        playerEnemyDamageCooldowns.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > PLAYER_DAMAGE_COOLDOWN * 2
        );
        
        // 刚性碰撞系统不需要清理冷却记录
    }
    
    /**
     * 获取玩家实体
     */
    private Player getPlayer() {
        return com.almasb.fxgl.dsl.FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取所有敌人实体
     */
    private List<Enemy> getEnemies() {
        return com.almasb.fxgl.dsl.FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Enemy)
                .map(e -> (Enemy) e)
                .filter(Enemy::isAlive)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有子弹实体
     */
    private List<Bullet> getBullets() {
        return com.almasb.fxgl.dsl.FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Bullet)
                .map(e -> (Bullet) e)
                .filter(Bullet::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        spatialSystem.setDebugMode(debugMode);
    }
    
    /**
     * 获取调试网格
     */
    public List<javafx.scene.shape.Rectangle> getDebugGrid() {
        return spatialSystem.getDebugGrid();
    }
    
    /**
     * 获取调试信息
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("碰撞检测系统调试信息:\n");
        info.append("  - 调试模式: ").append(debugMode ? "开启" : "关闭").append("\n");
        info.append("  - 更新间隔: ").append(com.roguelike.core.GameApp.COLLISION_UPDATE_INTERVAL).append("秒\n");
        info.append("  - 推挤力度倍数: ").append(com.roguelike.core.GameApp.COLLISION_PUSH_FORCE_MULTIPLIER).append("\n");
        info.append("  - 速度推挤: ").append(com.roguelike.core.GameApp.COLLISION_VELOCITY_PUSH_ENABLED ? "开启" : "关闭").append("\n");
        info.append("  - 位置推挤: ").append(com.roguelike.core.GameApp.COLLISION_POSITION_PUSH_ENABLED ? "开启" : "关闭").append("\n");
        info.append("  - 碰撞冷却记录数: ").append(collisionCooldowns.size()).append("\n");
        info.append("  - 攻击冷却记录数: ").append(attackCooldowns.size()).append("\n");
        info.append(spatialSystem.getDebugInfo()).append("\n");
        info.append(rigidCollisionSystem.getConfigInfo());
        return info.toString();
    }
    
    /**
     * 获取空间分割系统
     */
    public SpatialPartitionSystem getSpatialSystem() {
        return spatialSystem;
    }
    
    /**
     * 获取刚性碰撞系统
     */
    public RigidCollisionSystem getRigidCollisionSystem() {
        return rigidCollisionSystem;
    }
    
    
    /**
     * 设置推挤力度系数
     * @param multiplier 力度系数
     */
    public void setPushForceMultiplier(double multiplier) {
        if (rigidCollisionSystem != null) {
            rigidCollisionSystem.setPushForceMultiplier(multiplier);
        }
    }
    
    
    /**
     * 获取当前推挤力度系数
     * @return 力度系数
     */
    public double getPushForceMultiplier() {
        if (rigidCollisionSystem != null) {
            return rigidCollisionSystem.getPushForceMultiplier();
        }
        return 1.0;
    }
    
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
}