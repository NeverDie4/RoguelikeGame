package com.roguelike.physics;

import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;
import com.roguelike.entities.StraightBullet;
import com.roguelike.core.GameEvent;
import javafx.geometry.Point2D;

/**
 * 碰撞检测系统测试类
 * 用于验证各种碰撞检测功能是否正常工作
 */
public class CollisionTest {
    
    private EntityCollisionDetector detector;
    
    public CollisionTest() {
        this.detector = new EntityCollisionDetector();
    }
    
    /**
     * 运行所有碰撞测试
     */
    public void runAllTests() {
        System.out.println("🧪 开始碰撞检测系统测试...");
        
        testPlayerEnemyCollision();
        testBulletEnemyCollision();
        testBulletPlayerCollision();
        testEnemyEnemyCollision();
        testNoCollision();
        
        System.out.println("✅ 所有碰撞检测测试完成！");
    }
    
    /**
     * 测试玩家与敌人的碰撞
     */
    private void testPlayerEnemyCollision() {
        System.out.println("🔍 测试玩家与敌人碰撞...");
        
        Player player = new Player();
        player.setPosition(100, 100);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(105, 105); // 重叠位置
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(player, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 玩家与敌人碰撞检测成功");
            System.out.println("  📊 碰撞类型: " + result.getType());
            System.out.println("  📏 重叠区域: " + result.getOverlapX() + " x " + result.getOverlapY());
        } else {
            System.out.println("  ❌ 玩家与敌人碰撞检测失败");
        }
    }
    
    /**
     * 测试子弹与敌人的碰撞
     */
    private void testBulletEnemyCollision() {
        System.out.println("🔍 测试子弹与敌人碰撞...");
        
        Bullet bullet = new StraightBullet(Bullet.Faction.PLAYER, new Point2D(1, 0), 20, false, 300);
        bullet.setPosition(200, 200);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(205, 205); // 重叠位置
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(bullet, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 子弹与敌人碰撞检测成功");
            System.out.println("  📊 碰撞类型: " + result.getType());
        } else {
            System.out.println("  ❌ 子弹与敌人碰撞检测失败");
        }
    }
    
    /**
     * 测试子弹与玩家的碰撞
     */
    private void testBulletPlayerCollision() {
        System.out.println("🔍 测试子弹与玩家碰撞...");
        
        Bullet bullet = new StraightBullet(Bullet.Faction.ENEMY, new Point2D(1, 0), 15, false, 250);
        bullet.setPosition(300, 300);
        
        Player player = new Player();
        player.setPosition(305, 305); // 重叠位置
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(bullet, player);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 子弹与玩家碰撞检测成功");
            System.out.println("  📊 碰撞类型: " + result.getType());
        } else {
            System.out.println("  ❌ 子弹与玩家碰撞检测失败");
        }
    }
    
    /**
     * 测试敌人与敌人的碰撞
     */
    private void testEnemyEnemyCollision() {
        System.out.println("🔍 测试敌人与敌人碰撞...");
        
        Enemy enemy1 = new Enemy();
        enemy1.setPosition(400, 400);
        
        Enemy enemy2 = new Enemy();
        enemy2.setPosition(405, 405); // 重叠位置
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(enemy1, enemy2);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 敌人与敌人碰撞检测成功");
            System.out.println("  📊 碰撞类型: " + result.getType());
        } else {
            System.out.println("  ❌ 敌人与敌人碰撞检测失败");
        }
    }
    
    /**
     * 测试无碰撞情况
     */
    private void testNoCollision() {
        System.out.println("🔍 测试无碰撞情况...");
        
        Player player = new Player();
        player.setPosition(500, 500);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(600, 600); // 距离较远，不应碰撞
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(player, enemy);
        
        if (!result.hasCollision()) {
            System.out.println("  ✅ 无碰撞检测成功");
        } else {
            System.out.println("  ❌ 无碰撞检测失败");
        }
    }
    
    /**
     * 测试碰撞事件监听
     */
    public void testCollisionEvents() {
        System.out.println("🔍 测试碰撞事件系统...");
        
        // 监听碰撞事件
        GameEvent.listen(GameEvent.Type.PLAYER_ENEMY_COLLISION, event -> {
            System.out.println("  📢 收到玩家与敌人碰撞事件");
        });
        
        GameEvent.listen(GameEvent.Type.BULLET_ENEMY_COLLISION, event -> {
            System.out.println("  📢 收到子弹与敌人碰撞事件");
        });
        
        GameEvent.listen(GameEvent.Type.BULLET_PLAYER_COLLISION, event -> {
            System.out.println("  📢 收到子弹与玩家碰撞事件");
        });
        
        GameEvent.listen(GameEvent.Type.ENEMY_ENEMY_COLLISION, event -> {
            System.out.println("  📢 收到敌人与敌人碰撞事件");
        });
        
        System.out.println("  ✅ 碰撞事件监听器设置完成");
    }
    
    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        CollisionTest test = new CollisionTest();
        test.testCollisionEvents();
        test.runAllTests();
    }
}
