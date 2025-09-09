package com.roguelike.entities;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import com.roguelike.utils.FlowField;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Enemy extends EntityBase {

    private double speed = 80;
    private int maxHP = 50;
    private int currentHP = 50;
    private int expReward = 5; // 击败敌人获得的经验值

    // 流场寻路相关
    private static FlowField flowField;
    private static boolean flowFieldInitialized = false;
    private double lastTargetUpdateTime = 0;
    private static final double TARGET_UPDATE_INTERVAL = 0.5; // 每0.5秒更新一次目标

    // 平滑转向相关
    private double currentDirectionX = 0;
    private double currentDirectionY = 0;
    private double turnSpeed = 5.0; // 转向速度（弧度/秒）
    private double maxTurnRate = Math.PI / 2; // 最大转向速率

    public Enemy() {
        getViewComponent().addChild(new Rectangle(28, 28, Color.CRIMSON));
        addComponent(new CollidableComponent(true));
        setSize(28, 28);
    }

    public Enemy(int hp, int expReward) {
        this();
        this.maxHP = hp;
        this.currentHP = hp;
        this.expReward = expReward;
    }

    public void onUpdate(double tpf) {
        // 限制首帧 tpf，避免刚进入游戏时速度过快
        double dt = Math.min(tpf, 1.0 / 60.0);

        // 初始化流场（如果还没有初始化）
        if (!flowFieldInitialized) {
            initializeFlowField();
        }

        // 使用流场寻路AI
        followFlowField(dt);
    }

    // 提供给外部驱动的 AI 更新函数，避免与 FXGL 内部 onUpdate 冲突
    public void updateAI(double tpf) {
        onUpdate(tpf);
    }

    private static void initializeFlowField() {
        // 创建流场，假设游戏世界大小为2000x2000，每个格子32像素
        int worldSize = 2000;
        int cellSize = 32;
        int gridSize = worldSize / cellSize;

        flowField = new FlowField(gridSize, cellSize);
        flowFieldInitialized = true;
    }

    private void followFlowField(double tpf) {
        if (flowField == null) return;

        // 定期更新目标（玩家位置）
        double currentTime = FXGL.getGameTimer().getNow();
        if (currentTime - lastTargetUpdateTime > TARGET_UPDATE_INTERVAL) {
            updateTargetToPlayer();
            lastTargetUpdateTime = currentTime;
        }

        // 获取当前位置的流场向量
        Point2D currentPos = getCenter();
        FlowField.Vector2D flowVector = flowField.getVectorAtWorldPos(currentPos.getX(), currentPos.getY());

        // 如果流场向量有效，使用平滑转向
        if (flowVector.length() > 0) {
            smoothTurnToDirection(flowVector.x, flowVector.y, tpf);
            moveInCurrentDirection(tpf);
        } else {
            // 如果流场无效，回退到简单的朝向玩家移动
            fallbackToDirectMovement(tpf);
        }
    }

    private void smoothTurnToDirection(double targetX, double targetY, double tpf) {
        // 计算目标方向
        double targetLength = Math.sqrt(targetX * targetX + targetY * targetY);
        if (targetLength == 0) return;

        double normalizedTargetX = targetX / targetLength;
        double normalizedTargetY = targetY / targetLength;

        // 计算当前方向
        double currentLength = Math.sqrt(currentDirectionX * currentDirectionX + currentDirectionY * currentDirectionY);
        if (currentLength == 0) {
            // 如果当前没有方向，直接设置目标方向
            currentDirectionX = normalizedTargetX;
            currentDirectionY = normalizedTargetY;
            return;
        }

        double normalizedCurrentX = currentDirectionX / currentLength;
        double normalizedCurrentY = currentDirectionY / currentLength;

        // 计算角度差
        double dotProduct = normalizedCurrentX * normalizedTargetX + normalizedCurrentY * normalizedTargetY;
        dotProduct = Math.max(-1, Math.min(1, dotProduct)); // 限制在[-1,1]范围内
        double angleDiff = Math.acos(dotProduct);

        // 计算转向方向（顺时针或逆时针）
        double crossProduct = normalizedCurrentX * normalizedTargetY - normalizedCurrentY * normalizedTargetX;
        double turnDirection = crossProduct > 0 ? 1 : -1;

        // 限制转向速率
        double maxTurnThisFrame = maxTurnRate * tpf;
        double actualTurn = Math.min(angleDiff, maxTurnThisFrame);

        // 应用转向
        double cosTurn = Math.cos(actualTurn * turnDirection);
        double sinTurn = Math.sin(actualTurn * turnDirection);

        double newX = normalizedCurrentX * cosTurn - normalizedCurrentY * sinTurn;
        double newY = normalizedCurrentX * sinTurn + normalizedCurrentY * cosTurn;

        // 更新当前方向
        currentDirectionX = newX;
        currentDirectionY = newY;
    }

    private void moveInCurrentDirection(double tpf) {
        // 按照当前方向移动
        double moveDistance = speed * tpf;
        double moveX = currentDirectionX * moveDistance;
        double moveY = currentDirectionY * moveDistance;

        translate(moveX, moveY);
    }

    private void updateTargetToPlayer() {
        Entity player = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .findFirst().orElse(null);

        if (player != null) {
            Point2D playerPos = player.getCenter();
            flowField.setTargetFromWorldPos(playerPos.getX(), playerPos.getY());
            flowField.updateFlowField();
        }
    }

    private void fallbackToDirectMovement(double tpf) {
        // 回退到简单的朝向玩家移动
        Entity player = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .findFirst().orElse(null);
        if (player != null) {
            Point2D dir = player.getCenter().subtract(getCenter());
            if (dir.magnitude() > 1e-3) {
                dir = dir.normalize().multiply(speed * tpf);
                translate(dir);
            }
        }
    }

    // 静态方法，用于设置流场障碍物（比如地图中的墙壁）
    public static void setObstacle(double worldX, double worldY, boolean isObstacle) {
        if (flowField != null) {
            flowField.setObstacleFromWorldPos(worldX, worldY, isObstacle);
            flowField.updateFlowField();
        }
    }

    // 静态方法，用于获取流场实例（用于调试或可视化）
    public static FlowField getFlowField() {
        return flowField;
    }

    public void takeDamage(int damage) {
        if (damage <= 0) return;
        currentHP -= damage;
        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_HP_CHANGED));

        if (currentHP <= 0) {
            onDeath();
        }
    }

    public void onDeath() {
        // 给予玩家经验值
        GameState gameState = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .findFirst()
                .map(e -> ((Player) e).getGameState())
                .orElse(null);

        if (gameState != null) {
            gameState.addScore(10);
            gameState.addExp(expReward);
        }

        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_DEATH));
        removeFromWorld();
    }

    public void onDeath(GameState gameState) {
        if (gameState != null) {
            gameState.addScore(10);
            gameState.addExp(expReward);
        }
        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_DEATH));
        removeFromWorld();
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public boolean isAlive() {
        return currentHP > 0;
    }
}


