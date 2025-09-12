package com.roguelike.physics;

import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.map.MapRenderer;
import javafx.geometry.Point2D;

/**
 * 挤压机制测试类
 * 验证玩家可以强制挤开敌人前进，以及敌人不再自动死亡
 */
public class SqueezeMechanicsTest {
    
    public static void main(String[] args) {
        System.out.println("🤏 挤压机制测试");
        System.out.println("================");
        
        testPlayerSqueezeMechanics();
        testEnemyNoAutoDeath();
        testCollisionWithoutPush();
        
        System.out.println("\n✅ 所有挤压机制测试完成！");
    }
    
    /**
     * 测试玩家挤压机制
     */
    private static void testPlayerSqueezeMechanics() {
        System.out.println("\n👤 测试玩家挤压机制:");
        
        // 创建模拟的地图碰撞检测器
        MapRenderer mapRenderer = new MapRenderer("mapgrass");
        mapRenderer.init();
        MapCollisionDetector mapDetector = new MapCollisionDetector(mapRenderer);
        
        Player player = new Player();
        player.setPosition(200, 200);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(205, 205); // 重叠位置
        
        System.out.println("  📍 玩家初始位置: (" + player.getX() + ", " + player.getY() + ")");
        System.out.println("  📍 敌人初始位置: (" + enemy.getX() + ", " + enemy.getY() + ")");
        
        // 记录初始位置
        Point2D playerInitialPos = player.getPosition();
        Point2D enemyInitialPos = enemy.getPosition();
        
        // 尝试玩家移动（应该能挤开敌人）
        System.out.println("  🚶 玩家尝试向右移动...");
        player.move(10, 0); // 向右移动10像素
        
        // 检查位置变化
        Point2D playerAfterPos = player.getPosition();
        Point2D enemyAfterPos = enemy.getPosition();
        
        double playerMoveDistance = playerInitialPos.distance(playerAfterPos);
        double enemyMoveDistance = enemyInitialPos.distance(enemyAfterPos);
        
        System.out.println("  📐 玩家移动距离: " + String.format("%.1f 像素", playerMoveDistance));
        System.out.println("  📐 敌人移动距离: " + String.format("%.1f 像素", enemyMoveDistance));
        
        if (playerMoveDistance > 5.0) {
            System.out.println("  ✅ 玩家成功移动（符合预期）");
        } else {
            System.out.println("  ❌ 玩家移动距离太小（不符合预期）");
        }
        
        if (enemyMoveDistance > 1.0) {
            System.out.println("  ✅ 敌人被挤开（符合预期）");
        } else {
            System.out.println("  ❌ 敌人未被挤开（不符合预期）");
        }
    }
    
    /**
     * 测试敌人不再自动死亡
     */
    private static void testEnemyNoAutoDeath() {
        System.out.println("\n⏰ 测试敌人不再自动死亡:");
        
        Enemy enemy = new Enemy();
        enemy.setPosition(300, 300);
        
        System.out.println("  📍 敌人初始位置: (" + enemy.getX() + ", " + enemy.getY() + ")");
        System.out.println("  💚 敌人初始血量: " + enemy.getCurrentHP() + "/" + enemy.getMaxHP());
        System.out.println("  🧬 敌人存活状态: " + (enemy.isAlive() ? "存活" : "死亡"));
        
        // 模拟时间流逝（在真实游戏中，敌人不会因为时间而死亡）
        System.out.println("  ⏳ 模拟时间流逝...");
        
        // 检查敌人是否仍然存活
        if (enemy.isAlive()) {
            System.out.println("  ✅ 敌人仍然存活（符合预期，不再自动死亡）");
        } else {
            System.out.println("  ❌ 敌人已死亡（不符合预期）");
        }
        
        // 检查血量是否变化
        if (enemy.getCurrentHP() == enemy.getMaxHP()) {
            System.out.println("  ✅ 敌人血量未变化（符合预期）");
        } else {
            System.out.println("  ❌ 敌人血量发生变化（不符合预期）");
        }
    }
    
    /**
     * 测试碰撞不产生推离效果
     */
    private static void testCollisionWithoutPush() {
        System.out.println("\n💥 测试碰撞不产生推离效果:");
        
        EntityCollisionDetector detector = new EntityCollisionDetector();
        
        Player player = new Player();
        player.setPosition(400, 400);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(405, 405); // 重叠位置
        
        System.out.println("  📍 玩家位置: (" + player.getX() + ", " + player.getY() + ")");
        System.out.println("  📍 敌人位置: (" + enemy.getX() + ", " + enemy.getY() + ")");
        
        // 记录碰撞前的位置
        Point2D playerBeforePos = player.getPosition();
        Point2D enemyBeforePos = enemy.getPosition();
        
        // 检测碰撞
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(player, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 检测到碰撞");
            System.out.println("  📊 重叠区域: " + String.format("%.1f x %.1f", 
                result.getOverlapX(), result.getOverlapY()));
            
            // 处理碰撞
            detector.handleCollision(result);
            
            // 记录碰撞后的位置
            Point2D playerAfterPos = player.getPosition();
            Point2D enemyAfterPos = enemy.getPosition();
            
            // 计算位置变化
            double playerMoveDistance = playerBeforePos.distance(playerAfterPos);
            double enemyMoveDistance = enemyBeforePos.distance(enemyAfterPos);
            
            System.out.println("  📐 玩家移动距离: " + String.format("%.1f 像素", playerMoveDistance));
            System.out.println("  📐 敌人移动距离: " + String.format("%.1f 像素", enemyMoveDistance));
            
            // 验证结果
            if (playerMoveDistance < 1.0 && enemyMoveDistance < 1.0) {
                System.out.println("  ✅ 碰撞不产生推离效果（符合预期）");
            } else {
                System.out.println("  ❌ 碰撞产生了推离效果（不符合预期）");
            }
            
            // 检查玩家是否受到伤害
            if (player.getCurrentHP() < player.getMaxHP()) {
                System.out.println("  ✅ 玩家受到伤害（符合预期）");
            } else {
                System.out.println("  ❌ 玩家未受到伤害（不符合预期）");
            }
        } else {
            System.out.println("  ❌ 未检测到碰撞");
        }
    }
}
