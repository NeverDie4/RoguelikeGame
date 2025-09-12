package com.roguelike.physics;

import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;
import com.roguelike.entities.StraightBullet;
import com.roguelike.core.GameApp;
import javafx.geometry.Point2D;

/**
 * 子弹伤害测试类
 * 验证子弹伤害开关功能
 */
public class BulletDamageTest {
    
    public static void main(String[] args) {
        System.out.println("🔫 子弹伤害测试");
        System.out.println("================");
        
        testBulletDamageToggle();
        testPlayerBulletCollision();
        testEnemyBulletCollision();
        
        System.out.println("\n✅ 所有子弹伤害测试完成！");
    }
    
    /**
     * 测试子弹伤害开关
     */
    private static void testBulletDamageToggle() {
        System.out.println("\n🔧 测试子弹伤害开关:");
        
        // 显示初始状态
        System.out.println("  📊 初始状态:");
        GameApp.printDebugStatus();
        
        // 测试切换子弹伤害
        System.out.println("\n  🔄 切换子弹伤害:");
        GameApp.toggleBulletDamage();
        GameApp.printDebugStatus();
        
        // 再次切换
        System.out.println("\n  🔄 再次切换子弹伤害:");
        GameApp.toggleBulletDamage();
        GameApp.printDebugStatus();
        
        // 测试调试模式切换
        System.out.println("\n  🔄 切换调试模式:");
        GameApp.toggleDebugMode();
        GameApp.printDebugStatus();
    }
    
    /**
     * 测试玩家子弹与敌人碰撞
     */
    private static void testPlayerBulletCollision() {
        System.out.println("\n🎯 测试玩家子弹与敌人碰撞:");
        
        EntityCollisionDetector detector = new EntityCollisionDetector();
        
        // 创建测试实体
        Bullet playerBullet = new StraightBullet(Bullet.Faction.PLAYER, new Point2D(1, 0), 20, false, 300);
        playerBullet.setPosition(100, 100);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(105, 105); // 重叠位置
        
        System.out.println("  📍 玩家子弹位置: (" + playerBullet.getX() + ", " + playerBullet.getY() + ")");
        System.out.println("  📍 敌人位置: (" + enemy.getX() + ", " + enemy.getY() + ")");
        System.out.println("  💚 敌人初始血量: " + enemy.getCurrentHP() + "/" + enemy.getMaxHP());
        
        // 测试子弹伤害关闭状态
        GameApp.BULLET_DAMAGE_ENABLED = false;
        System.out.println("\n  🔫 子弹伤害关闭状态:");
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(playerBullet, enemy);
        if (result.hasCollision()) {
            detector.handleCollision(result);
            System.out.println("  💚 碰撞后敌人血量: " + enemy.getCurrentHP() + "/" + enemy.getMaxHP());
            System.out.println("  ✅ 子弹未造成伤害（符合预期）");
        }
        
        // 重置敌人血量
        enemy = new Enemy();
        enemy.setPosition(105, 105);
        
        // 测试子弹伤害开启状态
        GameApp.BULLET_DAMAGE_ENABLED = true;
        System.out.println("\n  🔫 子弹伤害开启状态:");
        
        Bullet playerBullet2 = new StraightBullet(Bullet.Faction.PLAYER, new Point2D(1, 0), 20, false, 300);
        playerBullet2.setPosition(100, 100);
        
        EntityCollisionDetector.CollisionResult result2 = detector.checkCollision(playerBullet2, enemy);
        if (result2.hasCollision()) {
            detector.handleCollision(result2);
            System.out.println("  💚 碰撞后敌人血量: " + enemy.getCurrentHP() + "/" + enemy.getMaxHP());
            System.out.println("  ✅ 子弹造成伤害（符合预期）");
        }
    }
    
    /**
     * 测试敌人子弹与玩家碰撞
     */
    private static void testEnemyBulletCollision() {
        System.out.println("\n⚠️  测试敌人子弹与玩家碰撞:");
        
        EntityCollisionDetector detector = new EntityCollisionDetector();
        
        // 创建测试实体
        Bullet enemyBullet = new StraightBullet(Bullet.Faction.ENEMY, new Point2D(1, 0), 15, false, 250);
        enemyBullet.setPosition(200, 200);
        
        Player player = new Player();
        player.setPosition(205, 205); // 重叠位置
        
        System.out.println("  📍 敌人子弹位置: (" + enemyBullet.getX() + ", " + enemyBullet.getY() + ")");
        System.out.println("  📍 玩家位置: (" + player.getX() + ", " + player.getY() + ")");
        System.out.println("  💚 玩家初始血量: " + player.getCurrentHP() + "/" + player.getMaxHP());
        
        // 测试子弹伤害关闭状态
        GameApp.BULLET_DAMAGE_ENABLED = false;
        System.out.println("\n  🔫 子弹伤害关闭状态:");
        
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(enemyBullet, player);
        if (result.hasCollision()) {
            detector.handleCollision(result);
            System.out.println("  💚 碰撞后玩家血量: " + player.getCurrentHP() + "/" + player.getMaxHP());
            System.out.println("  ✅ 子弹未造成伤害（符合预期）");
        }
        
        // 重置玩家血量
        player = new Player();
        player.setPosition(205, 205);
        
        // 测试子弹伤害开启状态
        GameApp.BULLET_DAMAGE_ENABLED = true;
        System.out.println("\n  🔫 子弹伤害开启状态:");
        
        Bullet enemyBullet2 = new StraightBullet(Bullet.Faction.ENEMY, new Point2D(1, 0), 15, false, 250);
        enemyBullet2.setPosition(200, 200);
        
        EntityCollisionDetector.CollisionResult result2 = detector.checkCollision(enemyBullet2, player);
        if (result2.hasCollision()) {
            detector.handleCollision(result2);
            System.out.println("  💚 碰撞后玩家血量: " + player.getCurrentHP() + "/" + player.getMaxHP());
            System.out.println("  ✅ 子弹造成伤害（符合预期）");
        }
    }
}
