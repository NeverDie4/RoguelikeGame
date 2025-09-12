package com.roguelike.physics;

import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.map.MapRenderer;
import javafx.geometry.Point2D;

/**
 * 推离修复测试类
 * 验证推离时不会撞到不可通行方块的问题修复
 */
public class PushFixTest {
    
    public static void main(String[] args) {
        System.out.println("🔧 推离修复测试");
        System.out.println("================");
        
        testSafePushDistance();
        testPlayerEnemyCollision();
        testEnemyEnemyCollision();
        
        System.out.println("\n✅ 所有推离修复测试完成！");
    }
    
    /**
     * 测试安全推离距离计算
     */
    private static void testSafePushDistance() {
        System.out.println("\n📏 测试安全推离距离计算:");
        
        // 创建模拟的地图碰撞检测器
        MapRenderer mapRenderer = new MapRenderer("mapgrass");
        mapRenderer.init();
        MapCollisionDetector mapDetector = new MapCollisionDetector(mapRenderer);
        
        EntityCollisionDetector entityDetector = new EntityCollisionDetector();
        entityDetector.setMapCollisionDetector(mapDetector);
        
        Player player = new Player();
        player.setPosition(100, 100);
        
        System.out.println("  📍 玩家初始位置: (" + player.getX() + ", " + player.getY() + ")");
        
        // 测试不同方向的推离
        double[] directions = {1, 0, -1, 0, 0, 1, 0, -1}; // 右、左、下、上
        String[] directionNames = {"右", "左", "下", "上"};
        
        for (int i = 0; i < directions.length; i += 2) {
            double dx = directions[i];
            double dy = directions[i + 1];
            String dirName = directionNames[i / 2];
            
            // 检查推离后的位置
            double pushDistance = 20.0;
            double newX = player.getX() + dx * pushDistance;
            double newY = player.getY() + dy * pushDistance;
            
            boolean canMove = mapDetector.canMoveTo(player, newX, newY);
            System.out.println("  🧭 向" + dirName + "推离20像素: " + (canMove ? "✅ 安全" : "❌ 会撞到障碍物"));
        }
    }
    
    /**
     * 测试玩家与敌人碰撞
     */
    private static void testPlayerEnemyCollision() {
        System.out.println("\n👤 测试玩家与敌人碰撞:");
        
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
        
        System.out.println("  📍 玩家位置: (" + player.getX() + ", " + player.getY() + ")");
        System.out.println("  📍 敌人位置: (" + enemy.getX() + ", " + enemy.getY() + ")");
        
        // 检测碰撞
        EntityCollisionDetector.CollisionResult result = entityDetector.checkCollision(player, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 检测到碰撞");
            System.out.println("  📊 重叠区域: " + String.format("%.1f x %.1f", 
                result.getOverlapX(), result.getOverlapY()));
            
            // 记录推离前的位置
            Point2D beforePush = player.getPosition();
            
            // 处理碰撞
            entityDetector.handleCollision(result);
            
            // 记录推离后的位置
            Point2D afterPush = player.getPosition();
            
            // 计算推离距离
            double pushDistance = beforePush.distance(afterPush);
            System.out.println("  📐 推离距离: " + String.format("%.1f 像素", pushDistance));
            
            // 检查推离后的位置是否安全
            boolean isSafe = mapDetector.canMoveTo(player, player.getX(), player.getY());
            System.out.println("  🛡️  推离后位置安全: " + (isSafe ? "✅ 是" : "❌ 否"));
        } else {
            System.out.println("  ❌ 未检测到碰撞");
        }
    }
    
    /**
     * 测试敌人与敌人碰撞
     */
    private static void testEnemyEnemyCollision() {
        System.out.println("\n👥 测试敌人与敌人碰撞:");
        
        // 创建模拟的地图碰撞检测器
        MapRenderer mapRenderer = new MapRenderer("mapgrass");
        mapRenderer.init();
        MapCollisionDetector mapDetector = new MapCollisionDetector(mapRenderer);
        
        EntityCollisionDetector entityDetector = new EntityCollisionDetector();
        entityDetector.setMapCollisionDetector(mapDetector);
        
        Enemy enemy1 = new Enemy();
        enemy1.setPosition(300, 300);
        
        Enemy enemy2 = new Enemy();
        enemy2.setPosition(305, 305); // 重叠位置
        
        System.out.println("  📍 敌人1位置: (" + enemy1.getX() + ", " + enemy1.getY() + ")");
        System.out.println("  📍 敌人2位置: (" + enemy2.getX() + ", " + enemy2.getY() + ")");
        
        // 检测碰撞
        EntityCollisionDetector.CollisionResult result = entityDetector.checkCollision(enemy1, enemy2);
        
        if (result.hasCollision()) {
            System.out.println("  ✅ 检测到敌人碰撞");
            System.out.println("  📊 重叠区域: " + String.format("%.1f x %.1f", 
                result.getOverlapX(), result.getOverlapY()));
            
            // 记录推离前的位置
            Point2D beforePush1 = enemy1.getPosition();
            Point2D beforePush2 = enemy2.getPosition();
            
            // 处理碰撞
            entityDetector.handleCollision(result);
            
            // 记录推离后的位置
            Point2D afterPush1 = enemy1.getPosition();
            Point2D afterPush2 = enemy2.getPosition();
            
            // 计算推离距离
            double pushDistance1 = beforePush1.distance(afterPush1);
            double pushDistance2 = beforePush2.distance(afterPush2);
            
            System.out.println("  📐 敌人1推离距离: " + String.format("%.1f 像素", pushDistance1));
            System.out.println("  📐 敌人2推离距离: " + String.format("%.1f 像素", pushDistance2));
            
            // 检查推离后的位置是否安全
            boolean isSafe1 = mapDetector.canMoveTo(enemy1, enemy1.getX(), enemy1.getY());
            boolean isSafe2 = mapDetector.canMoveTo(enemy2, enemy2.getX(), enemy2.getY());
            
            System.out.println("  🛡️  敌人1推离后位置安全: " + (isSafe1 ? "✅ 是" : "❌ 否"));
            System.out.println("  🛡️  敌人2推离后位置安全: " + (isSafe2 ? "✅ 是" : "❌ 否"));
        } else {
            System.out.println("  ❌ 未检测到敌人碰撞");
        }
    }
}
