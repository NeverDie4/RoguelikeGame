package com.roguelike.entities;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import com.roguelike.physics.MovementValidator;
import com.roguelike.physics.MovementValidator.MovementResult;
import com.roguelike.physics.MovementValidator.MovementType;
import com.roguelike.utils.AdaptivePathfinder;
import com.roguelike.utils.AdaptivePathfinder.PathfindingType;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Enemy extends EntityBase {

    private double speed = 200;
    private int maxHP = 50;
    private int currentHP = 50;
    private int expReward = 5; // 击败敌人获得的经验值

    // 目标位置相关
    private double targetX = 0;
    private double targetY = 0;
    private double lastTargetUpdateTime = 0;
    private static final double TARGET_UPDATE_INTERVAL = 0.5; // 每0.5秒更新一次目标

    // 平滑转向相关
    private double currentDirectionX = 0;
    private double currentDirectionY = 0;
    private double maxTurnRate = Math.PI * 2; // 最大转向速率
    
    // 碰撞检测相关
    private MovementValidator movementValidator;
    
    // 路径寻找相关
    private AdaptivePathfinder adaptivePathfinder;
    private java.util.List<javafx.geometry.Point2D> currentPath;
    private int currentPathIndex = 0;

    public Enemy() {
        getViewComponent().addChild(new Rectangle(28, 28, Color.CRIMSON));
        addComponent(new CollidableComponent(true));
        setSize(28, 28);
      initenemyhpbar();

    }

    private void initenemyhpbar(){


    }

    public Enemy(int hp, int expReward) {
        this();
        this.maxHP = hp;
        this.currentHP = hp;
        this.expReward = expReward;
    }

    public static void resetNavigation() {
        // 重置导航系统（现在由AdaptivePathfinder管理）
    }

    public void onUpdate(double tpf) {
        // 交由 GameApp 主循环驱动，避免双重更新导致的速度异常
    }

    // 提供给外部驱动的 AI 更新函数（由 GameApp 调用）
    public void updateAI(double tpf) {
        if (!isAlive()) {
            return;
        }

        // 更新路径寻找系统
        if (adaptivePathfinder != null) {
            adaptivePathfinder.update(tpf);
        }

        // 更新目标位置和路径（每0.5秒更新一次）
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        if (currentTime - lastTargetUpdateTime >= TARGET_UPDATE_INTERVAL) {
            updateTargetToPlayer();
            updatePathToTarget();
            lastTargetUpdateTime = currentTime;
        }

        // 根据当前算法选择移动方式
        if (adaptivePathfinder != null && adaptivePathfinder.getCurrentAlgorithm() == PathfindingType.FLOW_FIELD) {
            // 使用流体算法移动
            moveWithFlowField(tpf);
        } else {
            // 使用A*路径移动
            moveWithAStarPath(tpf);
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

        // 使用碰撞检测进行移动
        if (movementValidator != null) {
            MovementResult result = movementValidator.validateAndMove(this, moveX, moveY);
            
            if (result.isSuccess()) {
                translate(result.getDeltaX(), result.getDeltaY());
                
                // 如果发生滑动，可能需要调整移动方向
                if (result.getType() == MovementType.SLIDING) {
                    adjustDirectionAfterSliding(result);
                }
            } else {
                // 移动被阻挡，尝试改变方向
                handleMovementBlocked();
            }
        } else {
            // 没有碰撞检测时直接移动
            translate(moveX, moveY);
        }
    }
    
    /**
     * 滑动后调整移动方向
     */
    private void adjustDirectionAfterSliding(MovementResult result) {
        // 根据滑动结果调整方向
        double newDirectionX = result.getDeltaX();
        double newDirectionY = result.getDeltaY();
        
        double length = Math.sqrt(newDirectionX * newDirectionX + newDirectionY * newDirectionY);
        if (length > 0) {
            currentDirectionX = newDirectionX / length;
            currentDirectionY = newDirectionY / length;
        }
    }
    
    /**
     * 处理移动被阻挡的情况
     */
    private void handleMovementBlocked() {
        // 随机改变方向
        double angle = Math.random() * Math.PI * 2;
        currentDirectionX = Math.cos(angle);
        currentDirectionY = Math.sin(angle);
        
        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_HIT_WALL));
    }

    private void updateTargetToPlayer() {
        Entity player = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .findFirst().orElse(null);

        if (player != null) {
            Point2D playerPos = player.getCenter();
            targetX = playerPos.getX();
            targetY = playerPos.getY();
        }
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
    
    /**
     * 设置移动验证器
     */
    public void setMovementValidator(MovementValidator validator) {
        this.movementValidator = validator;
    }
    
    /**
     * 获取移动验证器
     */
    public MovementValidator getMovementValidator() {
        return movementValidator;
    }
    
    /**
     * 设置自适应路径寻找器
     */
    public void setAdaptivePathfinder(AdaptivePathfinder pathfinder) {
        this.adaptivePathfinder = pathfinder;
    }
    
    /**
     * 获取自适应路径寻找器
     */
    public AdaptivePathfinder getAdaptivePathfinder() {
        return adaptivePathfinder;
    }
    
    /**
     * 更新路径到目标（优化版本，减少不必要的重新计算）
     */
    private void updatePathToTarget() {
        if (adaptivePathfinder == null || targetX == 0 || targetY == 0) {
            return;
        }
        
        Point2D currentPos = getCenter();
        
        // 检查是否需要更新路径（距离目标太远或路径为空）
        double distanceToTarget = currentPos.distance(targetX, targetY);
        if (currentPath == null || currentPath.isEmpty() || distanceToTarget > 100.0) {
            currentPath = adaptivePathfinder.findPath(
                currentPos.getX(), currentPos.getY(), 
                targetX, targetY
            );
            currentPathIndex = 0;
        }
    }
    
    /**
     * 使用A*路径移动（修复转圈问题）
     */
    private void moveWithAStarPath(double tpf) {
        if (currentPath == null || currentPath.isEmpty()) {
            // 没有路径时回退到直接移动
            fallbackToDirectMovement(tpf);
            return;
        }
        
        Point2D currentPos = getCenter();
        
        // 跳过已经到达的路径点，避免在路径点附近震荡
        while (currentPathIndex < currentPath.size()) {
            Point2D targetPoint = currentPath.get(currentPathIndex);
            double distanceToTarget = currentPos.distance(targetPoint);
            
            // 增加到达判断距离，从10.0改为25.0，避免转圈
            if (distanceToTarget < 25.0) {
                currentPathIndex++;
            } else {
                break;
            }
        }
        
        // 如果还有路径点要到达
        if (currentPathIndex < currentPath.size()) {
            Point2D targetPoint = currentPath.get(currentPathIndex);
            double dx = targetPoint.getX() - currentPos.getX();
            double dy = targetPoint.getY() - currentPos.getY();
            double length = Math.sqrt(dx * dx + dy * dy);
            
            if (length > 0) {
                dx /= length;
                dy /= length;
                
                // 直接设置方向，避免平滑转向导致的震荡
                currentDirectionX = dx;
                currentDirectionY = dy;
                moveInCurrentDirection(tpf);
            }
        } else {
            // 路径完成，回退到直接移动
            fallbackToDirectMovement(tpf);
        }
    }
    
    /**
     * 使用流体算法移动
     */
    private void moveWithFlowField(double tpf) {
        if (adaptivePathfinder == null) {
            fallbackToDirectMovement(tpf);
            return;
        }
        
        Point2D currentPos = getCenter();
        Point2D direction = adaptivePathfinder.getMovementDirection(
            currentPos.getX(), currentPos.getY()
        );
        
        if (direction.getX() != 0 || direction.getY() != 0) {
            smoothTurnToDirection(direction.getX(), direction.getY(), tpf);
            moveInCurrentDirection(tpf);
        } else {
            fallbackToDirectMovement(tpf);
        }
    }
    
    /**
     * 回退到直接移动（朝向玩家）
     */
    private void fallbackToDirectMovement(double tpf) {
        if (targetX == 0 || targetY == 0) {
            return;
        }
        
        Point2D currentPos = getCenter();
        double dx = targetX - currentPos.getX();
        double dy = targetY - currentPos.getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        
        if (length > 0) {
            dx /= length;
            dy /= length;
            smoothTurnToDirection(dx, dy, tpf);
            moveInCurrentDirection(tpf);
        }
    }
}



