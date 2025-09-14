package com.roguelike.entities;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import com.roguelike.entities.components.CharacterAnimationComponent;
import com.roguelike.physics.OptimizedMovementValidator;
import com.roguelike.physics.OptimizedMovementValidator.MovementResult;
import com.roguelike.physics.OptimizedMovementValidator.MovementType;
import com.roguelike.utils.AdaptivePathfinder;
import com.roguelike.utils.AdaptivePathfinder.PathfindingType;
import com.roguelike.entities.config.EnemyConfig;
import com.roguelike.entities.effects.ParticleEffectManager;
import com.roguelike.entities.effects.ParticleEffectConfig;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Enemy extends EntityBase {

    private double speed = 100;
    private int maxHP = 50;
    private int currentHP = 50;
    private int expReward = 5; // 击败敌人获得的经验值

    // 目标位置相关
    private double targetX = 0;
    private double targetY = 0;
    private double lastTargetUpdateTime = 0;
    private static final double TARGET_UPDATE_INTERVAL = 0.1; // 每0.5秒更新一次目标，减少寻路频率
    private boolean isNewlySpawned = true; // 标记是否为新生成的敌人

    // 平滑转向相关
    private double currentDirectionX = 0;
    private double currentDirectionY = 0;
    private double maxTurnRate = Math.PI * 2; // 最大转向速率

    // 碰撞检测相关
    private OptimizedMovementValidator movementValidator;

    // 路径寻找相关
    private AdaptivePathfinder adaptivePathfinder;
    private java.util.List<javafx.geometry.Point2D> currentPath;
    private int currentPathIndex = 0;

    // 动画组件
    private CharacterAnimationComponent animationComponent;
    private CharacterAnimationComponent.Direction currentDirection = CharacterAnimationComponent.Direction.RIGHT;

    // 死亡状态标记
    private boolean isDead = false;
    
    // 敌人配置引用（用于死亡效果）
    private EnemyConfig enemyConfig;
    
    // 墙壁内扣血冷却
    private double lastWallDamageTime = 0;
    private static final double WALL_DAMAGE_COOLDOWN = 0.2; // 0.2秒冷却

    public Enemy() {
        // 添加碰撞组件
        addComponent(new CollidableComponent(true));

        // 设置实体大小（根据敌人动画帧大小调整）
        setSize(32, 32);

        // 初始化动画
        initializeAnimation();

        // 设置实体锚点为中心
        getTransformComponent().setAnchoredPosition(new Point2D(0.5, 0.5));

        initenemyhpbar();
    }

    private void initenemyhpbar(){


    }
    
    /**
     * 设置碰撞箱
     * @param collision 碰撞配置
     */
    private void setupCollisionBox(EnemyConfig.EnemyCollision collision) {
        if (collision != null) {
            // 清除默认碰撞箱
            getBoundingBoxComponent().clearHitBoxes();
            
            // 添加自定义碰撞箱
            getBoundingBoxComponent().addHitBox(new com.almasb.fxgl.physics.HitBox(
                com.almasb.fxgl.physics.BoundingShape.box(collision.getWidth(), collision.getHeight())
            ));
            
            // 设置碰撞箱偏移
            if (collision.getOffsetX() != 0 || collision.getOffsetY() != 0) {
                // 注意：FXGL的HitBox偏移可能需要通过其他方式实现
                // 这里先记录偏移值，后续可能需要调整
            }
        }
    }
    
    /**
     * 基于配置初始化动画
     * @param config 敌人配置
     */
    private void initializeAnimationFromConfig(EnemyConfig config) {
        try {
            // 初始化动画组件
            animationComponent = new CharacterAnimationComponent();
            addComponent(animationComponent);
            
            EnemyConfig.EnemyAnimations animConfig = config.getAnimations();
            
            // 加载敌人行走动画帧
            animationComponent.loadPngAnimationFrames(
                animConfig.getTexturePath(),
                animConfig.getWalkFrames(),
                animConfig.getWalkPattern(),
                animConfig.getAnimationWidth(),
                animConfig.getAnimationHeight()
            );
            
            // 死亡动画已移除，改用粒子效果
            
            // 设置动画参数
            animationComponent.setFrameDuration(animConfig.getFrameDuration());
            animationComponent.setLooping(true);
            
        } catch (Exception e) {
            System.err.println("敌人动画初始化失败: " + e.getMessage());
            e.printStackTrace();
            
            // 如果动画加载失败，使用备用矩形显示
            getViewComponent().addChild(new Rectangle(64, 64, Color.CRIMSON));
        }
    }

    private void initializeAnimation() {
        try {
            // 初始化动画组件
            animationComponent = new CharacterAnimationComponent();
            addComponent(animationComponent);

            // 加载敌人行走动画帧（10帧PNG图片）
            animationComponent.loadPngAnimationFrames("assets/textures/enemy", 10, "enemy_walk_%02d.png");

            // 死亡动画已移除，改用粒子效果

            // 设置动画参数
            animationComponent.setFrameDuration(0.15); // 每帧150毫秒，比玩家稍快
            animationComponent.setLooping(true);

        } catch (Exception e) {
            System.err.println("敌人动画初始化失败: " + e.getMessage());
            e.printStackTrace();

            // 如果动画加载失败，使用备用矩形显示
            getViewComponent().addChild(new Rectangle(64, 64, Color.CRIMSON));
        }
    }

    public Enemy(int hp, int expReward) {
        this();
        this.maxHP = hp;
        this.currentHP = hp;
        this.expReward = expReward;
    }
    
    /**
     * 基于配置创建敌人
     * @param config 敌人配置
     */
    public Enemy(EnemyConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("敌人配置不能为空");
        }
        
        // 添加碰撞组件
        addComponent(new CollidableComponent(true));
        
        // 从配置设置属性
        this.maxHP = config.getStats().getMaxHP();
        this.currentHP = this.maxHP;
        this.speed = config.getStats().getSpeed();
        this.expReward = config.getStats().getExpReward();
        this.enemyConfig = config; // 保存配置引用
        
        // 设置实体大小
        setSize(config.getSize().getWidth(), config.getSize().getHeight());
        
        // 设置碰撞箱
        setupCollisionBox(config.getCollision());
        
        // 初始化动画
        initializeAnimationFromConfig(config);
        
        // 设置实体锚点为中心
        getTransformComponent().setAnchoredPosition(new Point2D(0.5, 0.5));
        
        initenemyhpbar();
        
    }

    public static void resetNavigation() {
        // 重置导航系统（现在由AdaptivePathfinder管理）
    }

    public void onUpdate(double tpf) {
        // 交由 GameApp 主循环驱动，避免双重更新导致的速度异常
    }

    // 提供给外部驱动的 AI 更新函数（由 GameApp 调用）
    public void updateAI(double tpf) {
        // 如果敌人已死亡，不再执行移动逻辑
        if (isDead) {
            return;
        }

        if (!isAlive()) {
            return;
        }

        // 检查路径寻找器是否丢失，如果丢失则尝试重新获取
        if (adaptivePathfinder == null) {
            // 尝试从EntityFactory重新获取路径寻找器
            com.roguelike.utils.AdaptivePathfinder globalPathfinder = 
                com.roguelike.entities.EntityFactory.getAdaptivePathfinder();
            if (globalPathfinder != null) {
                this.adaptivePathfinder = globalPathfinder;
            }
        }

        // 更新路径寻找系统
        if (adaptivePathfinder != null) {
            adaptivePathfinder.update(tpf);
        }

        // 更新目标位置和路径（每0.5秒更新一次，但新生成的敌人立即更新）
        double currentTime = com.roguelike.core.TimeService.getSeconds();
        if (isNewlySpawned || currentTime - lastTargetUpdateTime >= TARGET_UPDATE_INTERVAL) {
            updateTargetToPlayer();
            updatePathToTarget();
            lastTargetUpdateTime = currentTime;
            isNewlySpawned = false; // 标记为已初始化
        }

        // 始终使用A*路径移动（流场算法已移除）
        moveWithAStarPath(tpf);
        
        // 检查敌人是否处于墙壁内，如果是则扣血
        checkEnemyInWallDamage();
        
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

        // 检测水平移动方向并切换动画
        if (moveX > 0 && currentDirection != CharacterAnimationComponent.Direction.RIGHT) {
            // 向右移动
            currentDirection = CharacterAnimationComponent.Direction.RIGHT;
            if (animationComponent != null) {
                animationComponent.setDirection(currentDirection);
            }
        } else if (moveX < 0 && currentDirection != CharacterAnimationComponent.Direction.LEFT) {
            // 向左移动
            currentDirection = CharacterAnimationComponent.Direction.LEFT;
            if (animationComponent != null) {
                animationComponent.setDirection(currentDirection);
            }
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

    /**
     * 初始化目标位置
     */
    public void initializeTargetPosition() {
        Entity player = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .findFirst().orElse(null);

        if (player != null) {
            Point2D playerPos = player.getCenter();
            targetX = playerPos.getX();
            targetY = playerPos.getY();
        } else {
            // 如果玩家不存在，设置一个默认目标位置
            double currentX = getX();
            double currentY = getY();
            targetX = currentX + 100; // 向右移动100像素
            targetY = currentY;
        }
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
        // 设置死亡状态，停止移动
        isDead = true;

        // 给予玩家经验值
        GameState gameState = FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof Player)
                .findFirst()
                .map(e -> ((Player) e).getGameState())
                .orElse(null);

        if (gameState != null) {
            gameState.addScore(10);
            gameState.addExp(expReward);
            gameState.addKill(); // 添加杀敌数统计
        }

        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_DEATH));

        // 触发死亡粒子效果
        triggerDeathEffect();

        // 死亡动画已移除，直接移除实体
        removeFromWorld();
    }

    public void onDeath(GameState gameState) {
        // 设置死亡状态，停止移动
        isDead = true;

        if (gameState != null) {
            gameState.addScore(10);
            gameState.addExp(expReward);
        }
        GameEvent.post(new GameEvent(GameEvent.Type.ENEMY_DEATH));

        // 触发死亡粒子效果
        triggerDeathEffect();

        // 死亡动画已移除，直接移除实体
        removeFromWorld();
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public boolean isAlive() {
        return currentHP > 0 && !isDead;
    }
    
    /**
     * 获取敌人攻击力
     */
    public int getAttack() {
        // 这里可以从配置中获取，暂时返回默认值
        return 10;
    }
    
    /**
     * 获取敌人防御力
     */
    public int getDefense() {
        // 这里可以从配置中获取，暂时返回默认值
        return 5;
    }
    
    /**
     * 获取敌人命中率
     */
    public int getAccuracy() {
        // 这里可以从配置中获取，暂时返回默认值
        return 70;
    }
    
    /**
     * 获取敌人速度
     */
    public double getSpeed() {
        return speed;
    }
    
    /**
     * 获取经验奖励
     */
    public int getExpReward() {
        return expReward;
    }
    
    /**
     * 触发死亡粒子效果
     */
    private void triggerDeathEffect() {
        if (enemyConfig != null && enemyConfig.getDeathEffect() != null) {
            EnemyConfig.EnemyDeathEffect deathEffect = enemyConfig.getDeathEffect();
            
            // 创建粒子效果配置
            ParticleEffectConfig effectConfig = new ParticleEffectConfig();
            effectConfig.setType(deathEffect.getEffectType());
            effectConfig.setParticleCount(deathEffect.getParticleCount());
            effectConfig.setDuration(deathEffect.getDuration());
            effectConfig.setColors(deathEffect.getColors());
            effectConfig.setSize(deathEffect.getSize());
            effectConfig.setSpeed(deathEffect.getSpeed());
            effectConfig.setGravity(deathEffect.getGravity());
            effectConfig.setSpread(deathEffect.getSpread());
            effectConfig.setFadeOut(deathEffect.isFadeOut());
            effectConfig.setFadeOutDuration(deathEffect.getFadeOutDuration());
            
            // 在敌人位置创建粒子效果
            ParticleEffectManager.getInstance().createEffect(effectConfig, getX(), getY());
            
            System.out.println("🎆 触发敌人死亡粒子效果: " + enemyConfig.getName() + " - " + deathEffect.getEffectType());
        } else {
            // 使用默认爆炸效果
            ParticleEffectManager.getInstance().createEffect("explosion", getX(), getY());
            System.out.println("🎆 使用默认爆炸效果");
        }
    }

    /**
     * 检查敌人是否已死亡（包括正在播放死亡动画的状态）
     */
    public boolean isDead() {
        return isDead;
    }

    /**
     * 设置移动验证器
     */
    public void setMovementValidator(OptimizedMovementValidator validator) {
        this.movementValidator = validator;
    }

    /**
     * 获取移动验证器
     */
    public OptimizedMovementValidator getMovementValidator() {
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
            if (adaptivePathfinder != null) {
                currentPath = adaptivePathfinder.findPath(
                    currentPos.getX(), currentPos.getY(),
                    targetX, targetY
                );
                currentPathIndex = 0;
            }
        }
    }

    /**
     * 使用A*路径移动（简化版本，确保敌人始终在移动）
     */
    private void moveWithAStarPath(double tpf) {
        // 检查目标位置是否有效
        if (targetX == 0 && targetY == 0) {
            // 如果目标位置无效，尝试重新初始化
            initializeTargetPosition();
            if (targetX == 0 && targetY == 0) {
                return; // 仍然无效，不移动
            }
        }

        Point2D currentPos = getCenter();
        double distanceToPlayer = currentPos.distance(targetX, targetY);

        // 如果距离玩家很近（小于30像素），直接移动
        if (distanceToPlayer < 30.0) {
            fallbackToDirectMovement(tpf);
            return;
        }

        // 尝试使用路径寻找（如果可用）
        if (adaptivePathfinder != null) {
            // 如果路径为空或距离目标很远，重新计算路径
            if (currentPath == null || currentPath.isEmpty() || distanceToPlayer > 300.0) {
                try {
                    currentPath = adaptivePathfinder.findPath(
                            currentPos.getX(), currentPos.getY(),
                            targetX, targetY
                    );
                    currentPathIndex = 0;

                } catch (Exception e) {
                    currentPath = null;
                }
            }

            // 如果路径寻找成功，使用路径移动
            if (currentPath != null && !currentPath.isEmpty()) {
                moveAlongPath(tpf);
                return;
            }
        }

        // 如果路径寻找失败或不可用，直接朝向玩家移动
        fallbackToDirectMovement(tpf);
    }

    /**
     * 沿着路径移动
     */
    private void moveAlongPath(double tpf) {
        if (currentPath == null || currentPath.isEmpty()) {
            return;
        }

        Point2D currentPos = getCenter();

        // 跳过已经到达的路径点，增加距离阈值减少抽搐
        while (currentPathIndex < currentPath.size()) {
            Point2D targetPoint = currentPath.get(currentPathIndex);
            double distanceToTarget = currentPos.distance(targetPoint);

            if (distanceToTarget < 25.0) { // 减少距离阈值，提高路径点切换频率
                currentPathIndex++;
            } else {
                break;
            }
        }

        // 移动到下一个路径点
        if (currentPathIndex < currentPath.size()) {
            Point2D targetPoint = currentPath.get(currentPathIndex);
            double dx = targetPoint.getX() - currentPos.getX();
            double dy = targetPoint.getY() - currentPos.getY();
            double length = Math.sqrt(dx * dx + dy * dy);

            if (length > 0) {
                dx /= length;
                dy /= length;
                currentDirectionX = dx;
                currentDirectionY = dy;
                
                
                moveInCurrentDirection(tpf);
            }
        } else {
            // 路径完成，直接朝向玩家移动
            fallbackToDirectMovement(tpf);
        }
    }


    /**
     * 回退到直接移动（朝向玩家）
     */
    private void fallbackToDirectMovement(double tpf) {
        // 确保目标位置有效
        if (targetX == 0 && targetY == 0) {
            initializeTargetPosition();
            if (targetX == 0 && targetY == 0) {
                return;
            }
        }

        Point2D currentPos = getCenter();
        double dx = targetX - currentPos.getX();
        double dy = targetY - currentPos.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {
            dx /= length;
            dy /= length;
            
            // 直接设置方向，不进行平滑转向
            currentDirectionX = dx;
            currentDirectionY = dy;
            moveInCurrentDirection(tpf);
        }
    }
    
    /**
     * 检查敌人是否处于墙壁内，如果是则扣血
     */
    private void checkEnemyInWallDamage() {
        if (movementValidator != null) {
            // 检查冷却时间
            double currentTime = com.roguelike.core.TimeService.getSeconds();
            if (currentTime - lastWallDamageTime >= WALL_DAMAGE_COOLDOWN) {
                // 使用移动验证器检查墙壁碰撞
                movementValidator.checkEnemyInWallDamage(this);
                lastWallDamageTime = currentTime;
            }
        }
    }
}



