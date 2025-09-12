package com.roguelike.physics;

import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.map.MapRenderer;
import javafx.geometry.Point2D;

/**
 * 玩家推离修复测试类
 * 验证敌人无法推动玩家，只有敌人被推离
 */
public class PlayerPushFixTest {
    
    public static void main(String[] args) {
        System.out.println("👤 玩家推离修复测试");
        System.out.println("====================");
        
        testPlayerEnemyCollisionPush();
        testMultipleEnemiesCollision();
        
        System.out.println("\n✅ 所有玩家推离修复测试完成！");
    }
    
    /**
     * 测试玩家与敌人碰撞推离
     */
    private static void testPlayerEnemyCollisionPush() {
        System.out.println("\n👤 测试玩家与敌人碰撞推离:");
        
        // 创建模拟的地图碰撞检测器
        MapRenderer mapRenderer = new MapRenderer("mapgrass");
        mapRenderer.init();
        MapCollisionDetector mapDetector = new MapCollisionDetector(mapRenderer);
        
        EntityCollisionDetector entityDetector = new EntityCollisionDetector();
        entityDetector.setMapCollisionDetector(mapDetector);
        
        Player player = new Player();
        player.setPosition(200, 200);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(205, 205); // 重叠位置
        
        System.out.println("  📍 玩家初始位置: (" + player.getX() + ", " + player.getY() + ")");
        System.out.println("  📍 敌人初始位置: (" + enemy.getX() + ", " + enemy.getY() + ")");
        
        // 检测碰撞
        EntityCollisionDetector.CollisionResult result = entityDetector.checkCollision(player, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 检测到碰撞");
            System.out.println("  📊 重叠区域: " + String.format("%.1f x %.1f", 
                result.getOverlapX(), result.getOverlapY()));
            
            // 记录碰撞前的位置
            Point2D playerBeforePos = player.getPosition();
            Point2D enemyBeforePos = enemy.getPosition();
            
            // 处理碰撞
            entityDetector.handleCollision(result);
            
            // 记录碰撞后的位置
            Point2D playerAfterPos = player.getPosition();
            Point2D enemyAfterPos = enemy.getPosition();
            
            // 计算位置变化
            double playerMoveDistance = playerBeforePos.distance(playerAfterPos);
            double enemyMoveDistance = enemyBeforePos.distance(enemyAfterPos);
            
            System.out.println("  📐 玩家移动距离: " + String.format("%.1f 像素", playerMoveDistance));
            System.out.println("  📐 敌人移动距离: " + String.format("%.1f 像素", enemyMoveDistance));
            
            // 验证结果
            if (playerMoveDistance < 1.0) {
                System.out.println("  ✅ 玩家未被推动（符合预期）");
            } else {
                System.out.println("  ❌ 玩家被推动了（不符合预期）");
            }
            
            if (enemyMoveDistance > 1.0) {
                System.out.println("  ✅ 敌人被推离（符合预期）");
            } else {
                System.out.println("  ❌ 敌人未被推离（不符合预期）");
            }
            
            // 检查推离后的位置是否安全
            boolean playerSafe = mapDetector.canMoveTo(player, player.getX(), player.getY());
            boolean enemySafe = mapDetector.canMoveTo(enemy, enemy.getX(), enemy.getY());
            
            System.out.println("  🛡️  玩家位置安全: " + (playerSafe ? "✅ 是" : "❌ 否"));
            System.out.println("  🛡️  敌人位置安全: " + (enemySafe ? "✅ 是" : "❌ 否"));
        } else {
            System.out.println("  ❌ 未检测到碰撞");
        }
    }
    
    /**
     * 测试多个敌人碰撞
     */
    private static void testMultipleEnemiesCollision() {
        System.out.println("\n👥 测试多个敌人碰撞:");
        
        // 创建模拟的地图碰撞检测器
        MapRenderer mapRenderer = new MapRenderer("mapgrass");
        mapRenderer.init();
        MapCollisionDetector mapDetector = new MapCollisionDetector(mapRenderer);
        
        EntityCollisionDetector entityDetector = new EntityCollisionDetector();
        entityDetector.setMapCollisionDetector(mapDetector);
        
        Player player = new Player();
        player.setPosition(300, 300);
        
        Enemy enemy1 = new Enemy();
        enemy1.setPosition(305, 305); // 重叠位置
        
        Enemy enemy2 = new Enemy();
        enemy2.setPosition(310, 310); // 另一个敌人
        
        System.out.println("  📍 玩家位置: (" + player.getX() + ", " + player.getY() + ")");
        System.out.println("  📍 敌人1位置: (" + enemy1.getX() + ", " + enemy1.getY() + ")");
        System.out.println("  📍 敌人2位置: (" + enemy2.getX() + ", " + enemy2.getY() + ")");
        
        // 记录初始位置
        Point2D playerInitialPos = player.getPosition();
        Point2D enemy1InitialPos = enemy1.getPosition();
        Point2D enemy2InitialPos = enemy2.getPosition();
        
        // 处理玩家与敌人1的碰撞
        EntityCollisionDetector.CollisionResult result1 = entityDetector.checkCollision(player, enemy1);
        if (result1.hasCollision()) {
            System.out.println("  ✅ 检测到玩家与敌人1碰撞");
            entityDetector.handleCollision(result1);
        }
        
        // 处理玩家与敌人2的碰撞
        EntityCollisionDetector.CollisionResult result2 = entityDetector.checkCollision(player, enemy2);
        if (result2.hasCollision()) {
            System.out.println("  ✅ 检测到玩家与敌人2碰撞");
            entityDetector.handleCollision(result2);
        }
        
        // 计算位置变化
        double playerMoveDistance = playerInitialPos.distance(player.getPosition());
        double enemy1MoveDistance = enemy1InitialPos.distance(enemy1.getPosition());
        double enemy2MoveDistance = enemy2InitialPos.distance(enemy2.getPosition());
        
        System.out.println("  📐 玩家总移动距离: " + String.format("%.1f 像素", playerMoveDistance));
        System.out.println("  📐 敌人1总移动距离: " + String.format("%.1f 像素", enemy1MoveDistance));
        System.out.println("  📐 敌人2总移动距离: " + String.format("%.1f 像素", enemy2MoveDistance));
        
        // 验证结果
        if (playerMoveDistance < 2.0) {
            System.out.println("  ✅ 玩家基本未被推动（符合预期）");
        } else {
            System.out.println("  ❌ 玩家被推动了（不符合预期）");
        }
        
        if (enemy1MoveDistance > 1.0 || enemy2MoveDistance > 1.0) {
            System.out.println("  ✅ 敌人被推离（符合预期）");
        } else {
            System.out.println("  ❌ 敌人未被推离（不符合预期）");
        }
    }
}
