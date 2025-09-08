package com.roguelike.entities;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.roguelike.core.GameState;
import com.roguelike.utils.RandomUtils;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public class EntityFactory implements com.almasb.fxgl.entity.EntityFactory {

    private final GameState gameState = new GameState();

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        Player player = new Player();
        player.setX(data.getX());
        player.setY(data.getY());
        return player;
    }

    @Spawns("enemy")
    public Entity newEnemy(SpawnData data) {
        Enemy enemy = new Enemy();
        // 在玩家附近随机一圈生成
        Entity player = getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof Player).findFirst().orElse(null);
        Point2D base = player != null ? player.getCenter() : new Point2D(getAppWidth() / 2.0, getAppHeight() / 2.0);
        double angle = Math.toRadians(RandomUtils.nextInt(0, 359));
        double radius = RandomUtils.nextInt(200, 400);
        enemy.setX(base.getX() + Math.cos(angle) * radius);
        enemy.setY(base.getY() + Math.sin(angle) * radius);

        // 被子弹击中时死亡的简单碰撞在 FXGL 中通常通过层次/碰撞处理，这里简化：寿命到时自删并加分
        runOnce(() -> enemy.onDeath(gameState), javafx.util.Duration.seconds(15));
        return enemy;
    }
}


