package com.roguelike.physics;

import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.map.MapRenderer;

/**
 * 血量系统测试类
 * 验证玩家血量、敌人攻击间隔和伤害值
 */
public class HealthSystemTest {
    
    public static void main(String[] args) {
        System.out.println("❤️ 血量系统测试");
        System.out.println("================");
        
        testPlayerHealth();
        testEnemyDamage();
        testAttackInterval();
        testNoPushback();
        
        System.out.println("\n✅ 所有血量系统测试完成！");
    }
    
    /**
     * 测试玩家血量
     */
    private static void testPlayerHealth() {
        System.out.println("\n💚 测试玩家血量:");
        
        Player player = new Player();
        
        System.out.println("  📊 玩家最大血量: " + player.getMaxHP());
        System.out.println("  📊 玩家当前血量: " + player.getCurrentHP());
        
        if (player.getMaxHP() == 200 && player.getCurrentHP() == 200) {
            System.out.println("  ✅ 玩家血量设置正确（200/200）");
        } else {
            System.out.println("  ❌ 玩家血量设置错误，期望200/200，实际" + 
                player.getCurrentHP() + "/" + player.getMaxHP());
        }
    }
    
    /**
     * 测试敌人伤害
     */
    private static void testEnemyDamage() {
        System.out.println("\n⚔️ 测试敌人伤害:");
        
        // 创建模拟的地图碰撞检测器
        MapRenderer mapRenderer = new MapRenderer("mapgrass");
        mapRenderer.init();
        MapCollisionDetector mapDetector = new MapCollisionDetector(mapRenderer);
        
        Player player = new Player();
        player.setPosition(200, 200);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(205, 205); // 重叠位置
        
        EntityCollisionDetector detector = new EntityCollisionDetector();
        detector.setMapCollisionDetector(mapDetector);
        
        System.out.println("  📊 碰撞前玩家血量: " + player.getCurrentHP());
        
        // 检测碰撞
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(player, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 检测到碰撞");
            
            // 处理碰撞
            detector.handleCollision(result);
            
            System.out.println("  📊 碰撞后玩家血量: " + player.getCurrentHP());
            
            int expectedHP = 200 - 10; // 200 - 10 = 190
            if (player.getCurrentHP() == expectedHP) {
                System.out.println("  ✅ 敌人伤害正确（10点伤害）");
            } else {
                System.out.println("  ❌ 敌人伤害错误，期望" + expectedHP + "，实际" + player.getCurrentHP());
            }
        } else {
            System.out.println("  ❌ 未检测到碰撞");
        }
    }
    
    /**
     * 测试攻击间隔
     */
    private static void testAttackInterval() {
        System.out.println("\n⏰ 测试攻击间隔:");
        
        // 创建模拟的地图碰撞检测器
        MapRenderer mapRenderer = new MapRenderer("mapgrass");
        mapRenderer.init();
        MapCollisionDetector mapDetector = new MapCollisionDetector(mapRenderer);
        
        Player player = new Player();
        player.setPosition(300, 300);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(305, 305); // 重叠位置
        
        EntityCollisionDetector detector = new EntityCollisionDetector();
        detector.setMapCollisionDetector(mapDetector);
        
        System.out.println("  📊 第一次攻击前玩家血量: " + player.getCurrentHP());
        
        // 第一次碰撞
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(player, enemy);
        if (result.hasCollision()) {
            detector.handleCollision(result);
            System.out.println("  📊 第一次攻击后玩家血量: " + player.getCurrentHP());
        }
        
        // 立即进行第二次碰撞（应该被攻击间隔阻止）
        result = detector.checkCollision(player, enemy);
        if (result.hasCollision()) {
            detector.handleCollision(result);
            System.out.println("  📊 第二次攻击后玩家血量: " + player.getCurrentHP());
            
            int expectedHP = 200 - 10; // 应该还是190，因为第二次攻击被阻止
            if (player.getCurrentHP() == expectedHP) {
                System.out.println("  ✅ 攻击间隔生效（第二次攻击被阻止）");
            } else {
                System.out.println("  ❌ 攻击间隔失效（第二次攻击未被阻止）");
            }
        }
    }
    
    /**
     * 测试无推离效果
     */
    private static void testNoPushback() {
        System.out.println("\n🚫 测试无推离效果:");
        
        // 创建模拟的地图碰撞检测器
        MapRenderer mapRenderer = new MapRenderer("mapgrass");
        mapRenderer.init();
        MapCollisionDetector mapDetector = new MapCollisionDetector(mapRenderer);
        
        Player player = new Player();
        player.setPosition(400, 400);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(405, 405); // 重叠位置
        
        EntityCollisionDetector detector = new EntityCollisionDetector();
        detector.setMapCollisionDetector(mapDetector);
        
        // 记录碰撞前的位置
        double playerXBefore = player.getX();
        double playerYBefore = player.getY();
        double enemyXBefore = enemy.getX();
        double enemyYBefore = enemy.getY();
        
        System.out.println("  📍 碰撞前玩家位置: (" + playerXBefore + ", " + playerYBefore + ")");
        System.out.println("  📍 碰撞前敌人位置: (" + enemyXBefore + ", " + enemyYBefore + ")");
        
        // 检测碰撞
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(player, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 检测到碰撞");
            
            // 处理碰撞
            detector.handleCollision(result);
            
            // 记录碰撞后的位置
            double playerXAfter = player.getX();
            double playerYAfter = player.getY();
            double enemyXAfter = enemy.getX();
            double enemyYAfter = enemy.getY();
            
            System.out.println("  📍 碰撞后玩家位置: (" + playerXAfter + ", " + playerYAfter + ")");
            System.out.println("  📍 碰撞后敌人位置: (" + enemyXAfter + ", " + enemyYAfter + ")");
            
            // 计算位置变化
            double playerMoveDistance = Math.sqrt(
                Math.pow(playerXAfter - playerXBefore, 2) + 
                Math.pow(playerYAfter - playerYBefore, 2)
            );
            double enemyMoveDistance = Math.sqrt(
                Math.pow(enemyXAfter - enemyXBefore, 2) + 
                Math.pow(enemyYAfter - enemyYBefore, 2)
            );
            
            System.out.println("  📐 玩家移动距离: " + String.format("%.1f 像素", playerMoveDistance));
            System.out.println("  📐 敌人移动距离: " + String.format("%.1f 像素", enemyMoveDistance));
            
            // 验证结果
            if (playerMoveDistance < 1.0 && enemyMoveDistance < 1.0) {
                System.out.println("  ✅ 碰撞不产生推离效果（符合预期）");
            } else {
                System.out.println("  ❌ 碰撞产生了推离效果（不符合预期）");
            }
        } else {
            System.out.println("  ❌ 未检测到碰撞");
        }
    }
}
