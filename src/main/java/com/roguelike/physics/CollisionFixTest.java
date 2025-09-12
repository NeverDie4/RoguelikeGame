package com.roguelike.physics;

import com.roguelike.entities.Player;
import com.roguelike.entities.Enemy;
import com.roguelike.entities.Bullet;
import com.roguelike.entities.StraightBullet;
import com.roguelike.core.GameEvent;
import javafx.geometry.Point2D;

/**
 * 碰撞修复测试类
 * 验证血条显示和碰撞瞬移问题的修复
 */
public class CollisionFixTest {
    
    public static void main(String[] args) {
        System.out.println("🔧 碰撞修复测试");
        System.out.println("================");
        
        testHealthBarPersistence();
        testCollisionCooldown();
        testPushDistance();
        
        System.out.println("\n✅ 所有修复测试完成！");
    }
    
    /**
     * 测试血条持久性
     */
    private static void testHealthBarPersistence() {
        System.out.println("\n🩸 测试血条持久性:");
        
        Player player = new Player();
        player.setPosition(100, 100);
        
        // 检查血条容器是否存在
        javafx.scene.Node healthBar = player.getHealthBarContainer();
        if (healthBar != null) {
            System.out.println("  ✅ 血条容器存在");
            System.out.println("  📊 血条类型: " + healthBar.getClass().getSimpleName());
        } else {
            System.out.println("  ❌ 血条容器不存在");
        }
        
        // 模拟血量变化
        player.takeDamage(20);
        System.out.println("  📉 玩家受到20点伤害");
        System.out.println("  💚 当前血量: " + player.getCurrentHP() + "/" + player.getMaxHP());
    }
    
    /**
     * 测试碰撞冷却机制
     */
    private static void testCollisionCooldown() {
        System.out.println("\n⏰ 测试碰撞冷却机制:");
        
        EntityCollisionDetector detector = new EntityCollisionDetector();
        
        Player player = new Player();
        player.setPosition(100, 100);
        
        Enemy enemy = new Enemy();
        enemy.setPosition(105, 105); // 重叠位置
        
        // 第一次碰撞
        EntityCollisionDetector.CollisionResult result1 = detector.checkCollision(player, enemy);
        if (result1.hasCollision()) {
            System.out.println("  ✅ 第一次碰撞检测成功");
            detector.handleCollision(result1);
        }
        
        // 立即进行第二次碰撞（应该被冷却机制阻止）
        EntityCollisionDetector.CollisionResult result2 = detector.checkCollision(player, enemy);
        if (result2.hasCollision()) {
            System.out.println("  🔄 第二次碰撞检测到，但应该被冷却机制阻止");
            detector.handleCollision(result2);
        }
        
        System.out.println("  ⏱️  碰撞冷却时间: 0.5秒");
    }
    
    /**
     * 测试推离距离
     */
    private static void testPushDistance() {
        System.out.println("\n📏 测试推离距离:");
        
        Player player = new Player();
        player.setPosition(100, 100);
        Point2D originalPos = player.getPosition();
        
        Enemy enemy = new Enemy();
        enemy.setPosition(105, 105); // 重叠位置
        
        EntityCollisionDetector detector = new EntityCollisionDetector();
        EntityCollisionDetector.CollisionResult result = detector.checkCollision(player, enemy);
        
        if (result.hasCollision()) {
            System.out.println("  📊 重叠区域: " + String.format("%.1f x %.1f", 
                result.getOverlapX(), result.getOverlapY()));
            
            // 记录推离前的位置
            Point2D beforePush = player.getPosition();
            
            // 处理碰撞（会触发推离）
            detector.handleCollision(result);
            
            // 记录推离后的位置
            Point2D afterPush = player.getPosition();
            
            // 计算推离距离
            double pushDistance = beforePush.distance(afterPush);
            System.out.println("  📐 推离距离: " + String.format("%.1f 像素", pushDistance));
            
            if (pushDistance <= 10.0) {
                System.out.println("  ✅ 推离距离合理，无瞬移问题");
            } else {
                System.out.println("  ⚠️  推离距离过大，可能存在瞬移问题");
            }
        }
    }
    
    /**
     * 测试事件系统
     */
    private static void testEventSystem() {
        System.out.println("\n📢 测试事件系统:");
        
        // 设置事件监听器
        GameEvent.listen(GameEvent.Type.PLAYER_ENEMY_COLLISION, event -> {
            System.out.println("  💥 收到玩家与敌人碰撞事件");
        });
        
        GameEvent.listen(GameEvent.Type.PLAYER_HURT, event -> {
            System.out.println("  🩸 收到玩家受伤事件");
        });
        
        System.out.println("  ✅ 事件监听器设置完成");
    }
}
