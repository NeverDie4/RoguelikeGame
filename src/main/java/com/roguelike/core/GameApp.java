package com.roguelike.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.roguelike.entities.Player;
import com.roguelike.map.MapRenderer;
import com.roguelike.physics.MapCollisionDetector;
import com.roguelike.physics.MovementValidator;
import com.roguelike.physics.CollisionManager;
import com.roguelike.physics.RigidCollisionSystem;
import com.roguelike.utils.AdaptivePathfinder;
import com.roguelike.ui.GameHUD;
import com.roguelike.ui.Menus;
import com.roguelike.ui.LoadingOverlay;
import javafx.scene.input.KeyCode;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 游戏主类。
 */
public class GameApp extends GameApplication {

    private GameState gameState;
    private MapRenderer mapRenderer;
    private GameHUD gameHUD;
    private MapCollisionDetector collisionDetector;
    private MovementValidator movementValidator;
    private CollisionManager collisionManager;
    private AdaptivePathfinder adaptivePathfinder;
    private double enemySpawnAccumulator = 0.0;
    private static final double ENEMY_SPAWN_INTERVAL = 0.5;
    private static boolean INPUT_BOUND = false;
    private static final double TARGET_DT = 1.0 / 60.0; // 目标帧时长
    private int frameCount = 0; // 帧计数器，用于跳过不稳定的初始帧
    private boolean gameReady = false; // 覆盖层完成后才开始计时与更新
    
    // 调试配置
    public static boolean DEBUG_MODE = false; // 调试模式开关
    public static boolean BULLET_DAMAGE_ENABLED = true; // 子弹伤害开关
    
    // 碰撞系统调试配置
    public static boolean COLLISION_DEBUG_MODE = false; // 碰撞调试模式
    public static double COLLISION_PUSH_FORCE_MULTIPLIER = 1.0; // 碰撞推挤力度倍数
    public static double COLLISION_UPDATE_INTERVAL = 0.016; // 碰撞更新间隔（秒）
    public static boolean COLLISION_VELOCITY_PUSH_ENABLED = true; // 是否启用速度推挤
    public static boolean COLLISION_POSITION_PUSH_ENABLED = true; // 是否启用位置推挤
    
    // 地图配置
    private static final String MAP_NAME = "mapgrass"; // 当前使用的地图名称
    
    // 路径寻找配置
    private static final int ENEMY_COUNT_THRESHOLD = 20; // 敌人数量阈值，超过此数量使用流体算法
    private static final boolean ALLOW_DIAGONAL_MOVEMENT = true; // 是否允许对角线移动
    private static final double PATHFINDING_UPDATE_INTERVAL = 0.05; // 路径寻找更新间隔（秒）
    private static final boolean ENABLE_PATH_OPTIMIZATION = true; // 是否启用路径优化
    private static final boolean ENABLE_PATH_SMOOTHING = true; // 是否启用路径平滑

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Roguelike Survivor Demo");
        settings.setVersion("0.1");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);
    }

    // 对应用户需求中的 init()
    @Override
    protected void initGame() {
        gameState = new GameState();
        getWorldProperties().setValue("score", 0);
        TimeService.reset();
        frameCount = 0;
        com.roguelike.entities.Enemy.resetNavigation();

        // 注册实体工厂：每次新游戏都注册，确保 GameWorld 持有工厂
        com.roguelike.entities.EntityFactory.setGameState(gameState);
        FXGL.getGameWorld().addEntityFactory(new com.roguelike.entities.EntityFactory());

        // 地图渲染器 - 加载Tiled地图
        mapRenderer = new MapRenderer(MAP_NAME);
        mapRenderer.init();
        
        // 初始化碰撞检测系统
        collisionDetector = new MapCollisionDetector(mapRenderer);
        movementValidator = new MovementValidator(collisionDetector);
        collisionManager = new CollisionManager();
        collisionManager.setMapCollisionDetector(collisionDetector);
        
        // 初始化自适应路径寻找系统
        AdaptivePathfinder.PathfindingConfig config = new AdaptivePathfinder.PathfindingConfig();
        config.setEnemyCountThreshold(ENEMY_COUNT_THRESHOLD);
        config.setAllowDiagonal(ALLOW_DIAGONAL_MOVEMENT);
        config.setPathfindingUpdateInterval(PATHFINDING_UPDATE_INTERVAL);
        config.setEnablePathOptimization(ENABLE_PATH_OPTIMIZATION);
        config.setEnableSmoothing(ENABLE_PATH_SMOOTHING);
        
        adaptivePathfinder = new AdaptivePathfinder(mapRenderer, config);
        
        // 调试：打印碰撞地图信息
        mapRenderer.printCollisionInfo();
        
        // 调试：打印路径寻找配置
        System.out.println("🎯 路径寻找系统配置:");
        System.out.println("   - 敌人数量阈值: " + ENEMY_COUNT_THRESHOLD);
        System.out.println("   - 允许对角线移动: " + ALLOW_DIAGONAL_MOVEMENT);
        System.out.println("   - 路径更新间隔: " + PATHFINDING_UPDATE_INTERVAL + "秒");
        System.out.println("   - 路径优化: " + ENABLE_PATH_OPTIMIZATION);
        System.out.println("   - 路径平滑: " + ENABLE_PATH_SMOOTHING);

        // 玩家 - 根据地图尺寸调整初始位置
        double playerX = mapRenderer.getMapWidth() > 0 ?
            (mapRenderer.getMapWidth() * mapRenderer.getTileWidth()) / 2.0 : 640;
        double playerY = mapRenderer.getMapHeight() > 0 ?
            (mapRenderer.getMapHeight() * mapRenderer.getTileHeight()) / 2.0 : 360;

        Player player = (Player) getGameWorld().spawn("player", new SpawnData(playerX, playerY));
        
        // 为玩家设置移动验证器（防止与敌人重叠）
        player.setMovementValidator(collisionManager.getMovementValidator());
        
        FXGL.getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2.0, getAppHeight() / 2.0);

        // 输入
        initInput(player);

        // HUD
        gameHUD = new GameHUD(gameState);
        gameHUD.mount();

        // 敌人周期生成计时器改为基于受控时间的累积器
        enemySpawnAccumulator = 0.0;

        // 事件示例
        GameEvent.listen(GameEvent.Type.MAP_LOADED, e -> {
            // 地图加载完成事件
            System.out.println("地图加载完成事件触发");
        });
        
        // 游戏结束事件监听
        GameEvent.listen(GameEvent.Type.PLAYER_DEATH, e -> {
            showGameOverScreen();
        });
        
        GameEvent.post(new GameEvent(GameEvent.Type.MAP_LOADED));
    }

    private void initInput(Player player) {
        if (INPUT_BOUND) {
            return;
        }
        // 使用固定移动距离，避免 tpf() 异常值导致的移动问题
        final double moveDistance = 2.0; // 固定移动距离，降低移动速度
        
        getInput().addAction(new UserAction("MOVE_LEFT_A") {
            @Override
            protected void onAction() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).move(-moveDistance, 0);
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("MOVE_LEFT_ARROW") {
            @Override
            protected void onAction() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).move(-moveDistance, 0);
            }
        }, KeyCode.LEFT);

        getInput().addAction(new UserAction("MOVE_RIGHT_D") {
            @Override
            protected void onAction() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).move(moveDistance, 0);
            }
        }, KeyCode.D);

        getInput().addAction(new UserAction("MOVE_RIGHT_ARROW") {
            @Override
            protected void onAction() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).move(moveDistance, 0);
            }
        }, KeyCode.RIGHT);

        getInput().addAction(new UserAction("MOVE_UP_W") {
            @Override
            protected void onAction() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).move(0, -moveDistance);
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("MOVE_UP_ARROW") {
            @Override
            protected void onAction() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).move(0, -moveDistance);
            }
        }, KeyCode.UP);

        getInput().addAction(new UserAction("MOVE_DOWN_S") {
            @Override
            protected void onAction() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).move(0, moveDistance);
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("MOVE_DOWN_ARROW") {
            @Override
            protected void onAction() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).move(0, moveDistance);
            }
        }, KeyCode.DOWN);
        

        if (COLLISION_DEBUG_MODE) {
            getInput().addAction(new UserAction("INCREASE_PUSH_FORCE") {
                @Override
                protected void onAction() {
                    adjustPushForce(0.05);
                }
            }, KeyCode.EQUALS);

            getInput().addAction(new UserAction("DECREASE_PUSH_FORCE") {
                @Override
                protected void onAction() {
                    adjustPushForce(-0.05);
                }
            }, KeyCode.MINUS);

            getInput().addAction(new UserAction("RESET_PUSH_FORCE") {
                @Override
                protected void onAction() {
                    if (collisionManager != null) {
                        collisionManager.setPushForceMultiplier(1.0);
                        System.out.println("⚡ 推挤力度重置为: 1.0");
                    }
                }
            }, KeyCode.R);

            getInput().addAction(new UserAction("PRINT_DEBUG_INFO") {
                @Override
                protected void onAction() {
                    printCollisionDebugInfo();
                }
            }, KeyCode.I);

            // 碰撞调试控制
            getInput().addAction(new UserAction("TOGGLE_COLLISION_DEBUG") {
                @Override
                protected void onAction() {
                    toggleCollisionDebugMode();
                }
            }, KeyCode.F1);

            getInput().addAction(new UserAction("INCREASE_UPDATE_INTERVAL") {
                @Override
                protected void onAction() {
                    adjustCollisionUpdateInterval(0.005);
                }
            }, KeyCode.F2);

            getInput().addAction(new UserAction("DECREASE_UPDATE_INTERVAL") {
                @Override
                protected void onAction() {
                    adjustCollisionUpdateInterval(-0.005);
                }
            }, KeyCode.F3);

            getInput().addAction(new UserAction("TOGGLE_VELOCITY_PUSH") {
                @Override
                protected void onAction() {
                    toggleVelocityPushMode();
                }
            }, KeyCode.F4);

            getInput().addAction(new UserAction("TOGGLE_POSITION_PUSH") {
                @Override
                protected void onAction() {
                    togglePositionPushMode();
                }
            }, KeyCode.F5);

            getInput().addAction(new UserAction("RESET_COLLISION_DEBUG") {
                @Override
                protected void onAction() {
                    resetCollisionDebugSettings();
                }
            }, KeyCode.F6);
        }

        // 旧的空格攻击移除，采用自动发射
        INPUT_BOUND = true;
    }

    // 对应用户需求中的 start()
    @Override
    protected void initUI() {
        Menus.hideAll();
        gameReady = false;
        // 加载阶段禁用输入，避免主角可被移动
        getInput().setProcessInput(false);
        // 暂停 HUD 计时展示，避免在加载阶段累加
        if (gameHUD != null) {
            gameHUD.pauseTime();
        }
        // 显示自定义加载覆盖层：最短 3 秒，完成后淡出并开始计时
        LoadingOverlay.show(3000, () -> {
            // 加载完成后重置游戏计时起点
            if (gameState != null) {
                gameState.resetGameTime();
            }
            TimeService.reset();
            frameCount = 0;
            // 恢复输入
            getInput().setProcessInput(true);
            // 恢复 HUD 计时
            if (gameHUD != null) {
                gameHUD.resumeTime();
            }
            gameReady = true;
        });
    }

    // 对应用户需求中的 update()
    @Override
    protected void onUpdate(double tpf) {
        // 覆盖层阶段直接跳过逻辑更新，避免初始化期 tpf 异常导致的暴快
        if (!gameReady) {
            return;
        }
        if (mapRenderer != null) {
            mapRenderer.onUpdate(tpf);
        }
        frameCount++;

        // 跳过前几帧的不稳定时期，避免首帧暴快
        if (frameCount <= 5) {
            return;
        }

        // 使用实际时间推进，但严格限幅避免首帧异常
        double realDt = Math.max(0.0, Math.min(tpf, TARGET_DT));

        // 推进受控时间（与现实时间同步）
        TimeService.update(realDt);

        // 更新碰撞管理器
        if (collisionManager != null) {
            collisionManager.update(realDt);
        }

        // 更新敌人数量并选择路径寻找算法
        int enemyCount = (int) getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .count();
        adaptivePathfinder.updateEnemyCount(enemyCount);
        
        // 敌人 AI 更新（使用相同的时间步长保持一致性）
        final double step = realDt;
        getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .forEach(e -> ((com.roguelike.entities.Enemy) e).updateAI(step));

        // 基于受控时间的刷怪逻辑
        enemySpawnAccumulator += realDt;
        while (enemySpawnAccumulator >= ENEMY_SPAWN_INTERVAL) {
            Entity newEnemy = getGameWorld().spawn("enemy");
            // 为新创建的敌人设置移动验证器和路径寻找器
            if (newEnemy instanceof com.roguelike.entities.Enemy) {
                ((com.roguelike.entities.Enemy) newEnemy).setMovementValidator(collisionManager.getMovementValidator());
                ((com.roguelike.entities.Enemy) newEnemy).setAdaptivePathfinder(adaptivePathfinder);
            }
            enemySpawnAccumulator -= ENEMY_SPAWN_INTERVAL;
        }
    }

    /**
     * 获取碰撞检测器实例
     */
    public MapCollisionDetector getCollisionDetector() {
        return collisionDetector;
    }
    
    /**
     * 获取移动验证器实例
     */
    public MovementValidator getMovementValidator() {
        return movementValidator;
    }
    
    /**
     * 获取碰撞管理器实例
     */
    public CollisionManager getCollisionManager() {
        return collisionManager;
    }
    
    /**
     * 获取地图渲染器实例
     */
    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }
    
    /**
     * 调试方法：切换调试模式
     */
    public static void toggleDebugMode() {
        DEBUG_MODE = !DEBUG_MODE;
        System.out.println("🔧 调试模式: " + (DEBUG_MODE ? "开启" : "关闭"));
    }
    
    /**
     * 调试方法：切换子弹伤害
     */
    public static void toggleBulletDamage() {
        BULLET_DAMAGE_ENABLED = !BULLET_DAMAGE_ENABLED;
        System.out.println("🔫 子弹伤害: " + (BULLET_DAMAGE_ENABLED ? "开启" : "关闭"));
    }
    
    /**
     * 调试方法：获取当前调试状态
     */
    public static void printDebugStatus() {
        System.out.println("🔧 当前调试状态:");
        System.out.println("  - 调试模式: " + (DEBUG_MODE ? "开启" : "关闭"));
        System.out.println("  - 子弹伤害: " + (BULLET_DAMAGE_ENABLED ? "开启" : "关闭"));
    }
    
    
    /**
     * 调试方法：获取碰撞系统调试信息
     */
    public void printCollisionDebugInfo() {
        if (collisionManager != null) {
            System.out.println(collisionManager.getDebugInfo());
        } else {
            System.out.println("碰撞管理器未初始化");
        }
    }
    
    
    /**
     * 调整推挤力度
     * @param delta 力度变化量
     */
    public void adjustPushForce(double delta) {
        if (collisionManager != null) {
            double currentForce = collisionManager.getPushForceMultiplier();
            double newForce = Math.max(0.1, Math.min(2.0, currentForce + delta));
            collisionManager.setPushForceMultiplier(newForce);
            COLLISION_PUSH_FORCE_MULTIPLIER = newForce;
            System.out.println("⚡ 推挤力度调整为: " + newForce);
        }
    }
    
    /**
     * 切换碰撞调试模式
     */
    public void toggleCollisionDebugMode() {
        COLLISION_DEBUG_MODE = !COLLISION_DEBUG_MODE;
        if (collisionManager != null) {
            collisionManager.setDebugMode(COLLISION_DEBUG_MODE);
        }
        System.out.println("🔧 碰撞调试模式: " + (COLLISION_DEBUG_MODE ? "开启" : "关闭"));
    }
    
    /**
     * 调整碰撞更新间隔
     * @param delta 间隔变化量（秒）
     */
    public void adjustCollisionUpdateInterval(double delta) {
        double newInterval = Math.max(0.005, Math.min(0.1, COLLISION_UPDATE_INTERVAL + delta));
        COLLISION_UPDATE_INTERVAL = newInterval;
        System.out.println("⏱️ 碰撞更新间隔调整为: " + (newInterval * 1000) + "ms");
    }
    
    /**
     * 切换速度推挤模式
     */
    public void toggleVelocityPushMode() {
        COLLISION_VELOCITY_PUSH_ENABLED = !COLLISION_VELOCITY_PUSH_ENABLED;
        System.out.println("🚀 速度推挤模式: " + (COLLISION_VELOCITY_PUSH_ENABLED ? "开启" : "关闭"));
    }
    
    /**
     * 切换位置推挤模式
     */
    public void togglePositionPushMode() {
        COLLISION_POSITION_PUSH_ENABLED = !COLLISION_POSITION_PUSH_ENABLED;
        System.out.println("📍 位置推挤模式: " + (COLLISION_POSITION_PUSH_ENABLED ? "开启" : "关闭"));
    }
    
    /**
     * 重置所有碰撞调试参数
     */
    public void resetCollisionDebugSettings() {
        COLLISION_DEBUG_MODE = false;
        COLLISION_PUSH_FORCE_MULTIPLIER = 1.0;
        COLLISION_UPDATE_INTERVAL = 0.016;
        COLLISION_VELOCITY_PUSH_ENABLED = true;
        COLLISION_POSITION_PUSH_ENABLED = true;
        
        if (collisionManager != null) {
            collisionManager.setDebugMode(false);
            collisionManager.setPushForceMultiplier(1.0);
        }
        
        System.out.println("🔄 碰撞调试参数已重置");
    }
    
    /**
     * 获取自适应路径寻找器实例
     */
    public AdaptivePathfinder getAdaptivePathfinder() {
        return adaptivePathfinder;
    }

    private void showGameOverScreen() {
        // 暂停游戏
        getGameController().pauseEngine();
        
        // 显示游戏结束界面
        com.roguelike.ui.GameOverScreen.show(gameState, () -> {
            // 点击继续后的处理
            com.roguelike.ui.GameOverScreen.hide();
            
            // 重置游戏状态而不是重新启动整个游戏
            resetGameState();
        });
    }
    
    private void resetGameState() {
        // 恢复游戏引擎
        getGameController().resumeEngine();
        
        // 重置游戏状态
        if (gameState != null) {
            gameState = new GameState();
        }
        
        // 使用FXGL的内置方法来清理游戏世界
        getGameController().startNewGame();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


