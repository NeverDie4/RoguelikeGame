package com.roguelike.entities;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.ProjectileComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.entity.components.TypeComponent;
import com.almasb.fxgl.texture.Texture;
import com.roguelike.core.GameEvent;
import com.roguelike.core.GameState;
import com.roguelike.entities.components.CharacterAnimationComponent;
import com.roguelike.physics.MovementValidator;
import com.roguelike.physics.MovementValidator.MovementResult;
import com.roguelike.physics.MovementValidator.MovementType;
import com.roguelike.entities.components.AutoFireComponent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

public class Player extends EntityBase {

    private final double speed = 200;
    private Rectangle hpBar;
    private Rectangle hpBarBackground;
    private StackPane hpBarContainer;
    private int maxHP = 100;
    private int currentHP = 100;
    private GameState gameState;
    private MovementValidator movementValidator;

    // 动画相关
    private CharacterAnimationComponent animationComponent;
    private CharacterAnimationComponent.Direction currentDirection = CharacterAnimationComponent.Direction.RIGHT;

    // 面朝方向（用于子弹发射方向对齐），默认向右
    private double forwardX = 1.0;
    private double forwardY = 0.0;

    public Player() {
        // 添加碰撞组件
        addComponent(new CollidableComponent(true));

        // 设置实体大小（根据GIF动画帧大小调整）
        setSize(32, 32);

        // 初始化动画
        initializeAnimation();

        // 初始化血条
        initHealthBar();

        // 设置实体锚点为中心
        getTransformComponent().setAnchoredPosition(new Point2D(0.5, 0.5));

        // 自动发射组件（默认 0.5s）
        addComponent(new AutoFireComponent(0.5));
    }

    private void initHealthBar() {
        // 根据角色大小计算血条尺寸
        double characterWidth = getWidth();
        double characterHeight = getHeight();

        // 血条宽度为角色宽度的1.2倍，高度为角色高度的0.2倍
        double hpBarWidth = characterWidth * 1.2;
        double hpBarHeight = characterHeight * 0.2;

        // 血条背景 - 使用圆角矩形
        hpBarBackground = new Rectangle(hpBarWidth, hpBarHeight, Color.color(0, 0, 0, 0.8));
        hpBarBackground.setArcWidth(hpBarHeight * 0.5); // 圆角半径为高度的一半
        hpBarBackground.setArcHeight(hpBarHeight * 0.5);
        hpBarBackground.setStroke(Color.color(1, 1, 1, 0.9));
        hpBarBackground.setStrokeWidth(1.5);

        // 血条 - 使用圆角矩形
        hpBar = new Rectangle(hpBarWidth, hpBarHeight, Color.LIMEGREEN);
        hpBar.setArcWidth(hpBarHeight * 0.5);
        hpBar.setArcHeight(hpBarHeight * 0.5);

        // 血条容器
        hpBarContainer = new StackPane();
        hpBarContainer.getChildren().addAll(hpBarBackground, hpBar);

        // 将血条添加到实体的视图组件中
        getViewComponent().addChild(hpBarContainer);

        // 设置血条位置（在角色脚下，保持适当距离）
        double distanceFromCharacter = characterHeight * 0.3; // 距离为角色高度的30%
        hpBarContainer.setTranslateY(characterHeight / 2 + distanceFromCharacter); // 角色中心下方
        hpBarContainer.setTranslateX(-(hpBarWidth - characterWidth) / 2); // 居中对齐

        // 监听血量变化事件
        GameEvent.listen(GameEvent.Type.PLAYER_HURT, e -> updateHealthBar());
    }

    private void initializeAnimation() {
        try {
            // 初始化动画组件
            animationComponent = new CharacterAnimationComponent();
            addComponent(animationComponent);

            // 加载向右方向的GIF动画帧（6帧，每帧128x128像素）
            animationComponent.loadGifAnimationFrames("assets/textures/player", 6);

            // 加载向左方向的GIF动画帧（6帧，每帧128x128像素）
            animationComponent.loadLeftGifAnimationFrames("assets/textures/player", 6);

            // 设置动画参数
            animationComponent.setFrameDuration(0.2); // 每帧200毫秒
            animationComponent.setLooping(true);

            System.out.println("玩家动画初始化完成（支持左右转向）");

            // 测试：3秒后强制显示第0帧
            FXGL.runOnce(() -> {
                //System.out.println("🧪 3秒后测试显示第0帧");
                animationComponent.testShowFrame(0);
            }, Duration.seconds(3));
        } catch (Exception e) {
            System.err.println("玩家动画初始化失败: " + e.getMessage());
            e.printStackTrace();

            // 如果动画加载失败，使用备用矩形显示
            Rectangle view = new Rectangle(32, 32, Color.DODGERBLUE);
            getViewComponent().addChild(view);
        }
    }

    public void updateHealthBar() {
        double ratio = maxHP <= 0 ? 0 : (double) currentHP / (double) maxHP;
        hpBar.setWidth(44 * Math.max(0, Math.min(1, ratio)));

        // 根据血量改变颜色，使用更丰富的颜色渐变
        if (ratio > 0.7) {
            hpBar.setFill(Color.LIMEGREEN);
        } else if (ratio > 0.4) {
            hpBar.setFill(Color.ORANGE);
        } else if (ratio > 0.2) {
            hpBar.setFill(Color.color(1.0, 0.5, 0.0)); // 深橙色
        } else {
            hpBar.setFill(Color.RED);
        }
    }

    public void heal(int amount) {
        currentHP = Math.min(maxHP, currentHP + amount);
        updateHealthBar();
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    // 测试方法：模拟受到伤害
    public void testTakeDamage() {
        takeDamage(20);
    }

    // 测试方法：模拟治疗
    public void testHeal() {
        heal(15);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void move(double dx, double dy) {
        // 先记录面朝方向（即使移动被阻挡也更新朝向）
        if (dx != 0 || dy != 0) {
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len > 0) {
                forwardX = dx / len;
                forwardY = dy / len;
            }
        }
        if (movementValidator != null) {
            // 使用移动验证器进行碰撞检测
            MovementResult result = movementValidator.validateAndMove(this, dx, dy);

            if (result.isSuccess()) {
                // 移动成功
                translate(result.getDeltaX(), result.getDeltaY());
                GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_MOVE));

                // 根据移动类型触发相应事件
                if (result.getType() == MovementType.SLIDING) {
                    GameEvent.post(new GameEvent(GameEvent.Type.MOVEMENT_SLIDING));
                }
            } else {
                // 移动被阻挡
                handleMovementBlocked();
            }
        } else {
            // 没有移动验证器时允许自由移动
            translate(dx, dy);
            GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_MOVE));
        }

        // 检测水平移动方向并切换动画
        if (dx > 0 && currentDirection != CharacterAnimationComponent.Direction.RIGHT) {
            // 向右移动
            currentDirection = CharacterAnimationComponent.Direction.RIGHT;
            if (animationComponent != null) {
                animationComponent.setDirection(currentDirection);
            }
        } else if (dx < 0 && currentDirection != CharacterAnimationComponent.Direction.LEFT) {
            // 向左移动
            currentDirection = CharacterAnimationComponent.Direction.LEFT;
            if (animationComponent != null) {
                animationComponent.setDirection(currentDirection);
            }
        }
    }

    /**
     * 获取玩家当前面朝方向（单位向量）。当玩家静止时为上一次移动方向。
     */
    public Point2D getForward() {
        return new Point2D(forwardX, forwardY);
    }

    /**
     * 处理移动被阻挡的情况
     */
    private void handleMovementBlocked() {
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_HIT_WALL));
        // 可以在这里添加音效、震动等效果
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

    // 旧的按键攻击已移除，发射逻辑由 AutoFireComponent 负责

    public void takeDamage(int damage) {
        if (gameState != null) {
            gameState.damagePlayer(damage);
            GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_HURT));

            if (gameState.getPlayerHP() <= 0) {
                onDeath();
            }
        }
    }

    public void onDeath() {
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_DEATH));
        // 可以在这里添加死亡逻辑，比如显示游戏结束界面
    }

    public Point2D getPositionVec() {
        return getPosition();
    }

    public static class Types {
        public static final String PLAYER = "PLAYER";
    }
}


