package com.roguelike.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.roguelike.entities.Player;
import com.roguelike.map.MapRenderer;
import com.roguelike.map.InfiniteMapManager;
import com.roguelike.physics.MapCollisionDetector;
import com.roguelike.physics.OptimizedMovementValidator;
import com.roguelike.physics.CollisionManager;
import com.roguelike.utils.AdaptivePathfinder;
import com.roguelike.core.EventBatchingManager;
import com.roguelike.ui.GameHUD;
import com.roguelike.ui.Menus;
import com.roguelike.ui.LoadingOverlay;
import com.roguelike.ui.FPSDisplay;
import javafx.scene.input.KeyCode;

import static com.almasb.fxgl.dsl.FXGL.*;

/*
 * 
 * 中优先级（后续实现）
智能预加载策略 - 提升用户体验
加载优先级系统 - 优化加载顺序
低优先级（长期优化）
内存优化策略 - 长期性能优化
性能监控系统 - 持续改进
配置化系统 - 灵活调整

还要注意对象的创建与销毁能不能优化
 */

/**
 * 游戏主类。
 */
public class GameApp extends GameApplication {

    private GameState gameState;
    private MapRenderer mapRenderer;
    private InfiniteMapManager infiniteMapManager;
    private GameHUD gameHUD;
    private FPSDisplay fpsDisplay;
    private MapCollisionDetector collisionDetector;
    private OptimizedMovementValidator movementValidator;
    private CollisionManager collisionManager;
    private AdaptivePathfinder adaptivePathfinder;
    private EventBatchingManager eventBatchingManager;
    private double enemySpawnAccumulator = 0.0;
    private static final double ENEMY_SPAWN_INTERVAL = 0.5;
    private static boolean INPUT_BOUND = false;
    private static final double TARGET_DT = 1.0 / 60.0; // 目标帧时长
    private int frameCount = 0; // 帧计数器，用于跳过不稳定的初始帧
    private boolean gameReady = false; // 覆盖层完成后才开始计时与更新
    
    // 性能优化：缓存玩家实体引用，避免每帧查找
    private Player cachedPlayer = null;
    
    // 实体缓存系统 - 避免每帧重复查找实体
    private java.util.List<com.roguelike.entities.Enemy> cachedEnemies = new java.util.ArrayList<>();
    private java.util.List<com.roguelike.entities.Bullet> cachedBullets = new java.util.ArrayList<>();
    private long lastEntityCacheUpdateTime = 0;
    private static final long ENTITY_CACHE_UPDATE_INTERVAL = 100; // 100ms更新一次实体缓存
    
    // 调试配置
    public static boolean DEBUG_MODE = false; // 调试模式开关
    public static boolean BULLET_DAMAGE_ENABLED = false; // 子弹伤害开关
    
    // 碰撞系统调试配置
    public static boolean COLLISION_DEBUG_MODE = false; // 碰撞调试模式
    public static double COLLISION_PUSH_FORCE_MULTIPLIER = 10.0; // 碰撞推挤力度倍数
    public static double COLLISION_UPDATE_INTERVAL = 0.016; // 碰撞更新间隔（秒）
    public static boolean COLLISION_VELOCITY_PUSH_ENABLED = true; // 是否启用速度推挤
    public static boolean COLLISION_POSITION_PUSH_ENABLED = true; // 是否启用位置推挤
    
    // 地图配置
    private static final String MAP_NAME = "test"; // 当前使用的地图名称
    private static final boolean USE_INFINITE_MAP = true; // 是否使用无限地图
    
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

        // 地图系统初始化
        if (USE_INFINITE_MAP) {
            // 使用无限地图系统
            infiniteMapManager = new InfiniteMapManager();
            collisionDetector = new MapCollisionDetector(infiniteMapManager);
            System.out.println("🌍 无限地图系统已启用");
        System.out.println("   区块尺寸: " + InfiniteMapManager.getChunkWidthPixels() + "x" + InfiniteMapManager.getChunkHeightPixels() + " 像素");
        System.out.println("   瓦片尺寸: 32x32 像素");
        System.out.println("   加载半径: " + infiniteMapManager.getLoadRadius() + " 个区块");
        System.out.println("   预加载半径: " + infiniteMapManager.getPreloadRadius() + " 个区块");
        System.out.println("   异步加载: " + (infiniteMapManager.isUseAsyncLoading() ? "启用" : "禁用"));
        System.out.println("   玩家初始位置: 区块0中心");
        } else {
            // 使用传统地图系统
            mapRenderer = new MapRenderer(MAP_NAME);
            mapRenderer.init();
            collisionDetector = new MapCollisionDetector(mapRenderer);
            System.out.println("🗺️ 传统地图系统已启用");
        }
        
        // 初始化移动验证和碰撞管理
        movementValidator = new OptimizedMovementValidator(collisionDetector);
        collisionManager = new CollisionManager();
        collisionManager.setMapCollisionDetector(collisionDetector);
        
        // 初始化事件批处理管理器
        eventBatchingManager = new EventBatchingManager();
        eventBatchingManager.setDebugMode(DEBUG_MODE);
        
        // 初始化自适应路径寻找系统
        AdaptivePathfinder.PathfindingConfig config = new AdaptivePathfinder.PathfindingConfig();
        config.setEnemyCountThreshold(ENEMY_COUNT_THRESHOLD);
        config.setAllowDiagonal(ALLOW_DIAGONAL_MOVEMENT);
        config.setPathfindingUpdateInterval(PATHFINDING_UPDATE_INTERVAL);
        config.setEnablePathOptimization(ENABLE_PATH_OPTIMIZATION);
        config.setEnableSmoothing(ENABLE_PATH_SMOOTHING);
        
        // 初始化路径寻找系统
        if (USE_INFINITE_MAP) {
            // 无限地图模式下暂时禁用路径寻找（后续实现）
            adaptivePathfinder = null;
            System.out.println("⚠️ 无限地图模式下路径寻找系统暂时禁用");
        } else {
            adaptivePathfinder = new AdaptivePathfinder(mapRenderer, config);
            // 调试：打印碰撞地图信息
            mapRenderer.printCollisionInfo();
        }
        
        // 调试：打印路径寻找配置
        System.out.println("🎯 路径寻找系统配置:");
        System.out.println("   - 敌人数量阈值: " + ENEMY_COUNT_THRESHOLD);
        System.out.println("   - 允许对角线移动: " + ALLOW_DIAGONAL_MOVEMENT);
        System.out.println("   - 路径更新间隔: " + PATHFINDING_UPDATE_INTERVAL + "秒");
        System.out.println("   - 路径优化: " + ENABLE_PATH_OPTIMIZATION);
        System.out.println("   - 路径平滑: " + ENABLE_PATH_SMOOTHING);

        // 玩家 - 根据地图系统设置初始位置
        double playerX, playerY;
        if (USE_INFINITE_MAP) {
            // 无限地图：玩家出生在区块0的中心
            playerX = InfiniteMapManager.getChunkWidthPixels() / 2.0; // 区块0的中心X
            playerY = InfiniteMapManager.getChunkHeightPixels() / 2.0; // 区块0的中心Y
        } else {
            // 传统地图：根据地图尺寸调整初始位置
            playerX = mapRenderer.getMapWidth() > 0 ?
                (mapRenderer.getMapWidth() * mapRenderer.getTileWidth()) / 2.0 : 640;
            playerY = mapRenderer.getMapHeight() > 0 ?
                (mapRenderer.getMapHeight() * mapRenderer.getTileHeight()) / 2.0 : 360;
        }

        Player player = (Player) getGameWorld().spawn("player", new SpawnData(playerX, playerY));
        
        // 缓存玩家引用，避免每帧查找
        cachedPlayer = player;
        
        // 为玩家设置移动验证器（防止与敌人重叠）
        player.setMovementValidator(collisionManager.getMovementValidator());
        
        FXGL.getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2.0, getAppHeight() / 2.0);

        // 输入
        initInput(player);

        // HUD
        gameHUD = new GameHUD(gameState);
        gameHUD.mount();
        
        // FPS显示（调试用）
        fpsDisplay = new FPSDisplay();
        fpsDisplay.setupWindowResizeListener();

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
            
            // 无限地图调试控制
            if (USE_INFINITE_MAP) {
                getInput().addAction(new UserAction("PRINT_INFINITE_MAP_STATUS") {
                    @Override
                    protected void onAction() {
                        if (infiniteMapManager != null) {
                            infiniteMapManager.printStatus();
                        }
                    }
                }, KeyCode.F7);
            }
            
            
        }

        // FPS显示切换
        getInput().addAction(new UserAction("TOGGLE_FPS_DISPLAY") {
            @Override
            protected void onAction() {
                if (fpsDisplay != null) {
                    fpsDisplay.toggle();
                }
            }
        }, KeyCode.F8);

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
        
        // 更新地图系统
        if (USE_INFINITE_MAP && infiniteMapManager != null) {
            // 使用缓存的玩家引用，避免每帧查找
            if (cachedPlayer != null && cachedPlayer.isActive()) {
                int currentChunkX = InfiniteMapManager.worldToChunkX(cachedPlayer.getX());
                if (currentChunkX != infiniteMapManager.getPlayerChunkX()) {
                    System.out.println("🚶 玩家跨越区块边界: " + infiniteMapManager.getPlayerChunkX() + " -> " + currentChunkX);
                    System.out.println("   世界坐标: " + String.format("%.1f", cachedPlayer.getX()) + ", " + String.format("%.1f", cachedPlayer.getY()));
                    infiniteMapManager.updateChunks(currentChunkX);
                } else {
                    // 玩家在同一区块内移动时，基于视角进行智能预加载
                    infiniteMapManager.viewportBasedPreload(cachedPlayer.getX(), cachedPlayer.getY());
                }
            } else {
                // 如果缓存的玩家无效，重新查找
                cachedPlayer = (Player) getGameWorld().getEntitiesByType().stream()
                    .filter(e -> e instanceof Player)
                    .findFirst()
                    .orElse(null);
            }
        } else if (mapRenderer != null) {
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

        // 更新实体缓存（控制频率，避免每帧更新）
        updateEntityCache();

        // 更新碰撞管理器
        if (collisionManager != null) {
            // 将缓存的实体传递给碰撞管理器
            collisionManager.updateEntityCache(cachedPlayer, cachedEnemies, cachedBullets);
            collisionManager.update(realDt);
        }
        
        // 处理所有批处理事件
        if (eventBatchingManager != null) {
            eventBatchingManager.processAllBatches();
        }

        // 使用缓存的敌人数量，避免每帧遍历所有实体
        int enemyCount = cachedEnemies.size();
        
        if (adaptivePathfinder != null) {
            adaptivePathfinder.updateEnemyCount(enemyCount);
        }
        
        // 使用批处理系统进行AI更新，提高性能
        if (eventBatchingManager.isAIBatchingEnabled()) {
            eventBatchingManager.addAIUpdateTasks(cachedEnemies, realDt);
        } else {
            // 直接更新AI（非批处理模式）
            final double step = realDt;
            for (com.roguelike.entities.Enemy enemy : cachedEnemies) {
                if (enemy != null && enemy.isActive()) {
                    enemy.updateAI(step);
                }
            }
        }

        // 基于受控时间的刷怪逻辑
        enemySpawnAccumulator += realDt;
        while (enemySpawnAccumulator >= ENEMY_SPAWN_INTERVAL) {
            Entity newEnemy = getGameWorld().spawn("enemy");
            // 为新创建的敌人设置移动验证器和路径寻找器
            if (newEnemy instanceof com.roguelike.entities.Enemy) {
                ((com.roguelike.entities.Enemy) newEnemy).setMovementValidator(collisionManager.getMovementValidator());
                if (adaptivePathfinder != null) {
                    ((com.roguelike.entities.Enemy) newEnemy).setAdaptivePathfinder(adaptivePathfinder);
                }
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
    public OptimizedMovementValidator getMovementValidator() {
        return movementValidator;
    }
    
    /**
     * 获取事件批处理管理器实例
     */
    public EventBatchingManager getEventBatchingManager() {
        return eventBatchingManager;
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
     * 获取无限地图管理器实例
     */
    public InfiniteMapManager getInfiniteMapManager() {
        return infiniteMapManager;
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
    
    /**
     * 更新实体缓存 - 避免每帧重复查找实体
     */
    private void updateEntityCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEntityCacheUpdateTime < ENTITY_CACHE_UPDATE_INTERVAL) {
            return; // 使用缓存，避免频繁更新
        }
        
        // 清空旧缓存
        cachedEnemies.clear();
        cachedBullets.clear();
        
        // 重新收集实体
        getGameWorld().getEntitiesByType().forEach(entity -> {
            if (entity instanceof com.roguelike.entities.Enemy) {
                cachedEnemies.add((com.roguelike.entities.Enemy) entity);
            } else if (entity instanceof com.roguelike.entities.Bullet) {
                cachedBullets.add((com.roguelike.entities.Bullet) entity);
            }
        });
        
        lastEntityCacheUpdateTime = currentTime;
        
        // 调试信息（可选）
        if (DEBUG_MODE && frameCount % 300 == 0) { // 每5秒打印一次
            System.out.println("🔄 实体缓存更新: 敌人=" + cachedEnemies.size() + ", 子弹=" + cachedBullets.size());
        }
    }
    
    /**
     * 获取缓存的敌人列表
     */
    public java.util.List<com.roguelike.entities.Enemy> getCachedEnemies() {
        return new java.util.ArrayList<>(cachedEnemies); // 返回副本避免外部修改
    }
    
    /**
     * 获取缓存的子弹列表
     */
    public java.util.List<com.roguelike.entities.Bullet> getCachedBullets() {
        return new java.util.ArrayList<>(cachedBullets); // 返回副本避免外部修改
    }
    
    /**
     * 获取FPS显示组件
     */
    public FPSDisplay getFPSDisplay() {
        return fpsDisplay;
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
        
        // 清理FPS显示资源
        if (fpsDisplay != null) {
            fpsDisplay.cleanup();
        }
        
        // 使用FXGL的内置方法来清理游戏世界
        getGameController().startNewGame();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


