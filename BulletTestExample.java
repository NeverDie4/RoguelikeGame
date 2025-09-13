// 这是一个测试示例，展示如何使用子弹系统
// 您可以将这些代码添加到GameApp或其他地方进行测试

import com.roguelike.entities.bullets.StraightBullet;
import com.roguelike.entities.bullets.CurveBullet;
import com.roguelike.entities.Bullet;
import javafx.geometry.Point2D;

public class BulletTestExample {
    
    public void testBullets() {
        // 测试直线子弹
        testStraightBullets();
        
        // 测试曲线子弹
        testCurveBullets();
    }
    
    private void testStraightBullets() {
        // 创建玩家直线子弹
        StraightBullet playerBullet = StraightBullet.createPlayerBullet(new Point2D(1, 0));
        playerBullet.setPosition(100, 100);
        
        // 启用动画
        playerBullet.enableAnimation();
        
        // 设置动画参数
        playerBullet.setAnimationFrameDuration(0.08); // 稍慢的动画
        playerBullet.setAnimationLooping(true);
        
        // 添加到游戏世界
        // getGameWorld().addEntity(playerBullet);
        
        // 创建敌人直线子弹
        StraightBullet enemyBullet = StraightBullet.createEnemyBullet(new Point2D(-1, 0));
        enemyBullet.setPosition(200, 100);
        enemyBullet.enableAnimation();
        
        // 创建穿透子弹
        StraightBullet piercingBullet = StraightBullet.createPiercingBullet(
            Bullet.Faction.PLAYER, new Point2D(0, 1));
        piercingBullet.setPosition(150, 150);
        piercingBullet.enableAnimation();
        
        // 创建圆形子弹
        StraightBullet circularBullet = StraightBullet.createCircularBullet(
            Bullet.Faction.ENEMY, new Point2D(0, -1));
        circularBullet.setPosition(250, 150);
        circularBullet.enableAnimation();
    }
    
    private void testCurveBullets() {
        // 创建玩家曲线子弹
        CurveBullet playerCurveBullet = CurveBullet.createPlayerCurveBullet(new Point2D(1, 0));
        playerCurveBullet.setPosition(300, 100);
        playerCurveBullet.enableAnimation();
        
        // 创建敌人曲线子弹
        CurveBullet enemyCurveBullet = CurveBullet.createEnemyCurveBullet(new Point2D(-1, 0));
        enemyCurveBullet.setPosition(400, 100);
        enemyCurveBullet.enableAnimation();
        
        // 创建螺旋子弹
        CurveBullet spiralBullet = CurveBullet.createSpiralBullet(
            Bullet.Faction.PLAYER, new Point2D(1, 0));
        spiralBullet.setPosition(350, 150);
        spiralBullet.enableAnimation();
        
        // 创建锯齿波子弹
        CurveBullet zigzagBullet = CurveBullet.createZigzagBullet(
            Bullet.Faction.ENEMY, new Point2D(0, 1));
        zigzagBullet.setPosition(450, 150);
        zigzagBullet.enableAnimation();
    }
    
    private void testCustomBullets() {
        // 自定义直线子弹
        StraightBullet customBullet = new StraightBullet(
            Bullet.Faction.PLAYER,
            new Point2D(1, 0),    // 方向
            15,                   // 伤害
            true,                 // 穿透
            600.0,                // 速度
            StraightBullet.BulletShape.CIRCLE,  // 形状
            10, 10                // 大小
        );
        
        customBullet.setPosition(500, 100);
        customBullet.enableAnimation();
        customBullet.setAnimationFrameDuration(0.1); // 每帧100毫秒
        
        // 自定义曲线子弹
        CurveBullet customCurveBullet = new CurveBullet(
            Bullet.Faction.ENEMY,
            new Point2D(-1, 0),   // 方向
            12,                   // 伤害
            false,                // 穿透
            400.0,                // 速度
            50.0,                 // 曲线因子
            CurveBullet.CurveType.SPIRAL,  // 曲线类型
            3.0,                  // 曲线频率
            6.0                   // 半径
        );
        
        customCurveBullet.setPosition(600, 100);
        customCurveBullet.enableAnimation();
        customCurveBullet.setAnimationFrameDuration(0.06); // 稍快的动画
    }
}
