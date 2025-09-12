package com.roguelike.physics;

import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;
import com.roguelike.entities.StraightBullet;
import com.roguelike.core.GameEvent;
import javafx.geometry.Point2D;

/**
 * 碰撞检测系统演示
 * 展示各种碰撞检测功能的使用方法
 */
public class CollisionDemo {
    
    public static void main(String[] args) {
        System.out.println("🎮 碰撞检测系统演示");
        System.out.println("====================");
        
        // 设置事件监听器
        setupEventListeners();
        
        // 创建碰撞检测器
        EntityCollisionDetector detector = new EntityCollisionDetector();
        
        // 演示各种碰撞场景
        demonstratePlayerEnemyCollision(detector);
        demonstrateBulletCollisions(detector);
        demonstrateEnemyEnemyCollision(detector);
        
        System.out.println("\n✅ 演示完成！");
    }
    
    /**
     * 设置事件监听器
     */
    private static void setupEventListeners() {
        System.out.println("🔧 设置事件监听器...");
        
        GameEvent.listen(GameEvent.Type.PLAYER_ENEMY_COLLISION, event -> {
            System.out.println("  💥 玩家与敌人发生碰撞！");
        });
        
        GameEvent.listen(GameEvent.Type.BULLET_ENEMY_COLLISION, event -> {
            System.out.println("  🎯 子弹击中敌人！");
        });
        
        GameEvent.listen(GameEvent.Type.BULLET_PLAYER_COLLISION, event -> {
            System.out.println("  ⚠️  玩家被子弹击中！");
        });
        
        GameEvent.listen(GameEvent.Type.ENEMY_ENEMY_COLLISION, event -> {
            System.out.println("  🤝 敌人之间发生碰撞！");
        });
    }
    
    /**
     * 演示玩家与敌人碰撞
     */
    private static void demonstratePlayerEnemyCollision(EntityCollisionDetector detector) {
        System.out.println("\n👤 演示玩家与敌人碰撞:");
        
        Player player = new Player();
        player.setPosition(100, 100);
        System.out.println("  玩家位置: (100, 100)");
        
        Enemy enemy = new Enemy();
        enemy.setPosition(105, 105); // 重叠位置
        System.out.println("  敌人位置: (105, 105)");
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(player, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 检测到碰撞！");
            System.out.println("  📊 碰撞类型: " + result.getType());
            System.out.println("  📏 重叠区域: " + String.format("%.1f x %.1f", 
                result.getOverlapX(), result.getOverlapY()));
            
            // 处理碰撞
            detector.handleCollision(result);
        } else {
            System.out.println("  ❌ 未检测到碰撞");
        }
    }
    
    /**
     * 演示子弹碰撞
     */
    private static void demonstrateBulletCollisions(EntityCollisionDetector detector) {
        System.out.println("\n🔫 演示子弹碰撞:");
        
        // 玩家子弹击中敌人
        System.out.println("  场景1: 玩家子弹击中敌人");
        Bullet playerBullet = new StraightBullet(Bullet.Faction.PLAYER, new Point2D(1, 0), 20, false, 300);
        playerBullet.setPosition(200, 200);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(205, 205);
        
        EntityCollisionDetector.CollisionResult result1 = detector.checkCollision(playerBullet, enemy);
        if (result1.hasCollision()) {
            System.out.println("    ✅ 玩家子弹击中敌人");
            detector.handleCollision(result1);
        }
        
        // 敌人子弹击中玩家
        System.out.println("  场景2: 敌人子弹击中玩家");
        Bullet enemyBullet = new StraightBullet(Bullet.Faction.ENEMY, new Point2D(1, 0), 15, false, 250);
        enemyBullet.setPosition(300, 300);
        
        Player player = new Player();
        player.setPosition(305, 305);
        
        EntityCollisionDetector.CollisionResult result2 = detector.checkCollision(enemyBullet, player);
        if (result2.hasCollision()) {
            System.out.println("    ✅ 敌人子弹击中玩家");
            detector.handleCollision(result2);
        }
    }
    
    /**
     * 演示敌人与敌人碰撞
     */
    private static void demonstrateEnemyEnemyCollision(EntityCollisionDetector detector) {
        System.out.println("\n👥 演示敌人与敌人碰撞:");
        
        Enemy enemy1 = new Enemy();
        enemy1.setPosition(400, 400);
        System.out.println("  敌人1位置: (400, 400)");
        
        Enemy enemy2 = new Enemy();
        enemy2.setPosition(405, 405); // 重叠位置
        System.out.println("  敌人2位置: (405, 405)");
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(enemy1, enemy2);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 检测到敌人碰撞！");
            System.out.println("  📊 碰撞类型: " + result.getType());
            
            // 处理碰撞
            detector.handleCollision(result);
        } else {
            System.out.println("  ❌ 未检测到敌人碰撞");
        }
    }
}
