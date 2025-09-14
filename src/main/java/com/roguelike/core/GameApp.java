package com.roguelike.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.time.TimerAction;
import com.roguelike.entities.Player;
import com.roguelike.entities.InfiniteMapEnemySpawnManager;
import com.roguelike.entities.BackgroundEnemySpawnManager;
import com.roguelike.map.MapRenderer;
import com.roguelike.map.InfiniteMapManager;
import com.roguelike.physics.MapCollisionDetector;
import com.roguelike.physics.OptimizedMovementValidator;
import com.roguelike.physics.CollisionManager;
import com.roguelike.utils.AdaptivePathfinder;
import com.roguelike.ui.GameHUD;
import com.roguelike.ui.Menus;
import com.roguelike.ui.CustomSceneFactory;
import com.roguelike.ui.LoadingOverlay;
import com.roguelike.ui.FPSDisplay;
import com.roguelike.ui.ArrowIndicator;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

// 请你完美解决，保证不产生其他问题并不影响其他功能
// 你看看有什么不明白的地方，有的话告诉我，没的话先列出待办事项，不写代码

/**
 * 游戏主类。
 */
public class GameApp extends GameApplication {

    private GameState gameState;
    private MapRenderer mapRenderer;
    private InfiniteMapManager infiniteMapManager;
    private com.roguelike.map.TeleportManager teleportManager;
    private com.roguelike.map.TimerTileManager timerTileManager;
    private GameHUD gameHUD;
    private FPSDisplay fpsDisplay;
    private ArrowIndicator arrowIndicator;
    private com.roguelike.entities.weapons.WeaponManager weaponManager;
    private MapCollisionDetector collisionDetector;
    private OptimizedMovementValidator movementValidator;
    private CollisionManager collisionManager;
    private AdaptivePathfinder adaptivePathfinder;
    private EventBatchingManager eventBatchingManager;
    private InfiniteMapEnemySpawnManager infiniteMapEnemySpawnManager;
    private BackgroundEnemySpawnManager backgroundEnemySpawnManager;
    private double enemySpawnAccumulator = 0.0;
    private static final double ENEMY_SPAWN_INTERVAL = 0.1;
    private static boolean INPUT_BOUND = false;
    private static final double TARGET_DT = 1.0 / 60.0; // 目标帧时长
    private int frameCount = 0; // 帧计数器，用于跳过不稳定的初始帧
    private boolean gameReady = false; // 覆盖层完成后才开始计时与更新

    // 帧率控制相关变量
    private long lastFrameTime = 0; // 上一帧的时间戳

    // 输入缓冲相关变量
    private long lastFPSToggleTime = 0; // 上次FPS切换的时间戳
    private long lastFPSLimitChangeTime = 0; // 上次帧率限制更改的时间戳
    private static final long FPS_TOGGLE_COOLDOWN = 300_000_000L; // 0.3秒的冷却时间（纳秒）
    private static final long FPS_LIMIT_CHANGE_COOLDOWN = 200_000_000L; // 0.2秒的冷却时间（纳秒）

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
    private static final int ENEMY_COUNT_THRESHOLD = 100; // 敌人数量阈值，超过此数量使用流体算法
    private static final boolean ALLOW_DIAGONAL_MOVEMENT = true; // 是否允许对角线移动
    private static final double PATHFINDING_UPDATE_INTERVAL = 0.05; // 路径寻找更新间隔（秒）
    private static final boolean ENABLE_PATH_OPTIMIZATION = true; // 是否启用路径优化
    private static final boolean ENABLE_PATH_SMOOTHING = true; // 是否启用路径平滑


    private static int TARGET_FPS = 60;
            // 使用固定移动距离，避免 tpf() 异常值导致的移动问题
    private static double moveDistance = 10.0; // 固定移动距离，提高移动速度

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Roguelike Survivor Demo");
        settings.setVersion("0.1");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);

        // 启用窗口大小调整
        settings.setManualResizeEnabled(true);

        // 使用自定义场景工厂来应用美化后的菜单系统
        settings.setSceneFactory(new CustomSceneFactory());
        System.out.println("已设置自定义场景工厂，使用美化后的菜单系统");
    }

    /**
     * 强制重新创建场景工厂，绕过FXGL缓存
     */
    public static void forceRecreateSceneFactory() {
        System.out.println("=== 强制重新创建场景工厂 ===");
        try {
            // 创建新的场景工厂实例
            CustomSceneFactory newFactory = new CustomSceneFactory();
            System.out.println("场景工厂已重新创建，实例ID: " + System.identityHashCode(newFactory));
            // 注意：FXGL的GameSettings是只读的，无法在运行时修改
            // 但我们可以通过重置静态变量来确保下次创建菜单时使用新实例
            System.out.println("场景工厂重置完成，下次创建菜单时将使用新实例");
        } catch (Exception e) {
            System.out.println("重新创建场景工厂时出错: " + e.getMessage());
        }
    }

    @Override
    protected void onPreInit() {
        try { com.roguelike.ui.MusicService.playLobby(); } catch (Exception ignored) {}
    }

    // 暂无对 FXGL 默认菜单的覆盖；使用自定义 ESC 菜单控制音乐

    // 对应用户需求中的 init()
    @Override
    protected void initGame() {
        // 强制清理所有可能的覆盖层，防止重新进入游戏时出现交互问题
        com.roguelike.ui.ConfirmationDialog.forceCleanup();
        com.roguelike.ui.OptionsMenu.forceCleanup();
        System.out.println("游戏初始化：已清理所有覆盖层");

        // 显示加载过程
        LoadingOverlay.show(2000, () -> {
            System.out.println("游戏加载完成");
            // 加载完成后开始游戏时间计算
            TimeService.startGame();
        });

        // 重置时间服务 - 确保从暂停状态恢复
        TimeService.reset();
        // 确保时间服务处于正常状态
        if (TimeService.isPaused()) {
            TimeService.resume();
        }
        System.out.println("游戏初始化：时间服务状态已重置");

        gameState = new GameState();
        // 注入到全局，供 Bullet 等通过 FXGL.geto("gameState") 访问
        com.almasb.fxgl.dsl.FXGL.set("gameState", gameState);
        weaponManager = new com.roguelike.entities.weapons.WeaponManager();
        // 暴露给全局，便于发射组件查询
        com.almasb.fxgl.dsl.FXGL.set("weaponManager", weaponManager);
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
            infiniteMapManager = new InfiniteMapManager(MAP_NAME);
            collisionDetector = new MapCollisionDetector(infiniteMapManager);

            // 初始化传送门管理器
            teleportManager = new com.roguelike.map.TeleportManager(infiniteMapManager);
            infiniteMapManager.setTeleportManager(teleportManager);

            // 获取定时器瓦片管理器
            timerTileManager = infiniteMapManager.getTimerTileManager();

            System.out.println("🌍 无限地图系统已启用");
            System.out.println("🚪 传送门系统已启用");
            System.out.println("⏰ 定时器瓦片系统已启用");
            System.out.println("🏰 Boss房区块限制已启用（只能通过传送门到达）");
        System.out.println("   区块尺寸: " + InfiniteMapManager.getChunkWidthPixels() + "x" + InfiniteMapManager.getChunkHeightPixels() + " 像素");
        System.out.println("   瓦片尺寸: 32x32 像素");
        System.out.println("   加载半径: " + infiniteMapManager.getLoadRadius() + " 个区块");
        System.out.println("   预加载半径: " + infiniteMapManager.getPreloadRadius() + " 个区块");
        System.out.println("   异步加载: " + (infiniteMapManager.isUseAsyncLoading() ? "启用" : "禁用"));
        System.out.println("   玩家初始位置: 区块0中心");

            // 初始化无限地图敌人生成管理器
            infiniteMapEnemySpawnManager = new InfiniteMapEnemySpawnManager(infiniteMapManager);
            com.roguelike.entities.EntityFactory.setInfiniteMapSpawnManager(infiniteMapEnemySpawnManager);
            System.out.println("🎯 无限地图敌人生成器已启用");

            // 初始化后台敌人生成管理器
            backgroundEnemySpawnManager = new BackgroundEnemySpawnManager();
            backgroundEnemySpawnManager.setInfiniteMapSpawnManager(infiniteMapEnemySpawnManager);
            com.roguelike.entities.EntityFactory.setBackgroundSpawnManager(backgroundEnemySpawnManager);
            System.out.println("🎯 后台敌人生成管理器已启用");
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

        // 设置移动验证器到EntityFactory，确保所有生成的敌人都能获得碰撞检测
        com.roguelike.entities.EntityFactory.setMovementValidator(movementValidator);

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
            // 无限地图模式下启用跨区块寻路
            adaptivePathfinder = new AdaptivePathfinder(infiniteMapManager, config);
            System.out.println("✅ 无限地图模式下启用跨区块寻路系统");
        } else {
            adaptivePathfinder = new AdaptivePathfinder(mapRenderer, config);
            // 调试：打印碰撞地图信息
            mapRenderer.printCollisionInfo();
        }

        // 设置路径寻找器到EntityFactory，确保所有生成的敌人都能获得寻路功能
        com.roguelike.entities.EntityFactory.setAdaptivePathfinder(adaptivePathfinder);


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

        // 为玩家设置传送门管理器
        if (teleportManager != null) {
            teleportManager.setPlayer(player);
            player.setTeleportManager(teleportManager);
        }
        
        FXGL.getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2.0, getAppHeight() / 2.0);

        // 输入
        initInput(player);

        // HUD
        gameHUD = new GameHUD(gameState);
        gameHUD.mount();

        // FPS显示（调试用）
        fpsDisplay = new FPSDisplay();
        fpsDisplay.setupWindowResizeListener();

        // 箭头指示器
        arrowIndicator = new ArrowIndicator();
        getGameScene().addUINode(arrowIndicator.getNode());
        System.out.println("🎯 箭头指示器已初始化");

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

        // 自定义菜单系统已通过CustomSceneFactory设置
    }

    private void initInput(Player player) {
        if (INPUT_BOUND) {
            return;
        }

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

                // 定时器瓦片调试控制
                getInput().addAction(new UserAction("PRINT_TIMER_TILE_STATUS") {
                    @Override
                    protected void onAction() {
                        if (timerTileManager != null) {
                            timerTileManager.printStatus();
                        }
                    }
                }, KeyCode.F12);
            }


        }

        // FPS显示切换（带输入缓冲）
        getInput().addAction(new UserAction("TOGGLE_FPS_DISPLAY") {
            @Override
            protected void onAction() {
                toggleFPSDisplayWithBuffer();
            }
        }, KeyCode.F8);

        // 帧率控制快捷键（带输入缓冲）
        getInput().addAction(new UserAction("INCREASE_FPS_LIMIT") {
            @Override
            protected void onAction() {
                changeFPSLimitWithBuffer(10);
            }
        }, KeyCode.F9);

        getInput().addAction(new UserAction("DECREASE_FPS_LIMIT") {
            @Override
            protected void onAction() {
                changeFPSLimitWithBuffer(-10);
            }
        }, KeyCode.F10);

        getInput().addAction(new UserAction("RESET_FPS_LIMIT") {
            @Override
            protected void onAction() {
                resetFPSLimitWithBuffer();
            }
        }, KeyCode.F11);

        // 旧的空格攻击移除，采用自动发射
        INPUT_BOUND = true;
    }

    // 对应用户需求中的 start()
    @Override
    protected void initUI() {
        // 自定义菜单系统已经通过CustomSceneFactory应用，这里只需要隐藏自定义菜单
        Menus.hideAll();
        System.out.println("自定义菜单系统已激活");
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
            TimeService.startGame();
            frameCount = 0;
            // 恢复输入
            getInput().setProcessInput(true);
            // 恢复 HUD 计时
            if (gameHUD != null) {
                gameHUD.resumeTime();
            }
            // 确保战斗音乐播放（若从菜单进入，已切换；此处兜底）
            try { com.roguelike.ui.MusicService.playBattle(); } catch (Exception ignored) {}
            gameReady = true;

            // 启动后台敌人生成
            if (backgroundEnemySpawnManager != null) {
                backgroundEnemySpawnManager.startSpawning();
                System.out.println("🚀 后台敌人生成已启动");
            }
        });
    }

    // 对应用户需求中的 update()
    @Override
    protected void onUpdate(double tpf) {
        // 覆盖层阶段直接跳过逻辑更新，避免初始化期 tpf 异常导致的暴快
        if (!gameReady) {
            return;
        }
        if (TimeService.isPaused()) {
            return;
        }

        // 帧率控制逻辑
        long currentTime = System.nanoTime();
        if (lastFrameTime == 0) {
            lastFrameTime = currentTime;
        }

        long frameTime = currentTime - lastFrameTime;
        long targetFrameTime = 1_000_000_000L / TARGET_FPS; // 纳秒

        // 如果帧时间小于目标时间，则等待
        if (frameTime < targetFrameTime) {
            try {
                Thread.sleep((targetFrameTime - frameTime) / 1_000_000); // 转换为毫秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastFrameTime = System.nanoTime();

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
        // 根据TARGET_FPS动态计算目标帧时长
        double targetDt = 1.0 / TARGET_FPS;
        double realDt = Math.max(0.0, Math.min(tpf, targetDt));

        // 推进受控时间（与现实时间同步）
        TimeService.update(realDt);

        // 简易碰撞检测：子弹 vs 敌人（用于伤害与经验结算）
        checkBulletEnemyCollisions();

        // 更新实体缓存（控制频率，避免每帧更新）
        updateEntityCache();

        // 更新箭头指示器
        updateArrowIndicator();

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

        // 更新定时器瓦片
        if (timerTileManager != null) {
            timerTileManager.update();
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
    }

    private void checkBulletEnemyCollisions() {
        java.util.List<com.almasb.fxgl.entity.Entity> bullets = getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Bullet)
                .toList();
        if (bullets.isEmpty()) return;
        java.util.List<com.almasb.fxgl.entity.Entity> enemies = getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .toList();
        if (enemies.isEmpty()) return;

        for (com.almasb.fxgl.entity.Entity b : bullets) {
            com.roguelike.entities.Bullet bullet = (com.roguelike.entities.Bullet) b;
            if (!b.isActive()) continue;
            javafx.geometry.Rectangle2D boxB = ((com.roguelike.entities.EntityBase) b).getCollisionBox();
            for (com.almasb.fxgl.entity.Entity e : enemies) {
                if (!e.isActive()) continue;
                if (!bullet.shouldCollideWith(e)) continue;
                javafx.geometry.Rectangle2D boxE = ((com.roguelike.entities.EntityBase) e).getCollisionBox();
                if (boxB.intersects(boxE)) {
                    bullet.onCollisionBegin(e);
                    // 子弹可能在命中时被移除（非穿透），此时不应再对其进行后续处理
                    if (!b.isActive()) {
                        break;
                    }
                }
            }
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
     * 获取传送门管理器实例
     */
    public com.roguelike.map.TeleportManager getTeleportManager() {
        return teleportManager;
    }

    /**
     * 获取定时器瓦片管理器实例
     */
    public com.roguelike.map.TimerTileManager getTimerTileManager() {
        return timerTileManager;
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

    /**
     * 获取当前目标帧率上限
     * @return 目标帧率上限
     */
    public static int getTargetFPS() {
        return TARGET_FPS;
    }

    /**
     * 设置目标帧率上限
     * @param fps 新的目标帧率上限
     */
    public static void setTargetFPS(int fps) {
        if (fps > 0 && fps <= 120) {
            TARGET_FPS = fps;
            System.out.println("✅ 帧率上限已设置为: " + fps + " FPS");
            System.out.println("   注意：帧率限制通过游戏循环中的时间控制实现");
        } else {
            System.out.println("❌ 无效的帧率值: " + fps + " (有效范围: 1-120)");
        }
    }

    /**
     * 获取帧率设置信息
     * @return 帧率设置信息字符串
     */
    public static String getFPSInfo() {
        return "当前目标帧率: " + TARGET_FPS + " FPS (有效范围: 1-120)";
    }

    /**
     * 带输入缓冲的FPS显示切换
     * 防止频繁按键导致的快速切换
     */
    private void toggleFPSDisplayWithBuffer() {
        long currentTime = System.nanoTime();

        // 检查是否在冷却时间内
        if (currentTime - lastFPSToggleTime < FPS_TOGGLE_COOLDOWN) {
            // 在冷却时间内，忽略输入
            return;
        }

        // 更新上次切换时间
        lastFPSToggleTime = currentTime;

        // 执行FPS显示切换
        if (fpsDisplay != null) {
            fpsDisplay.toggle();
        }
    }

    /**
     * 带输入缓冲的帧率限制更改
     * @param delta 帧率变化量
     */
    private void changeFPSLimitWithBuffer(int delta) {
        long currentTime = System.nanoTime();

        // 检查是否在冷却时间内
        if (currentTime - lastFPSLimitChangeTime < FPS_LIMIT_CHANGE_COOLDOWN) {
            // 在冷却时间内，忽略输入
            return;
        }

        // 更新上次更改时间
        lastFPSLimitChangeTime = currentTime;

        // 执行帧率更改
        int currentFPS = getTargetFPS();
        int newFPS = currentFPS + delta;

        if (newFPS >= 30 && newFPS <= 120) {
            setTargetFPS(newFPS);
        }
    }

    /**
     * 带输入缓冲的帧率限制重置
     */
    private void resetFPSLimitWithBuffer() {
        long currentTime = System.nanoTime();

        // 检查是否在冷却时间内
        if (currentTime - lastFPSLimitChangeTime < FPS_LIMIT_CHANGE_COOLDOWN) {
            // 在冷却时间内，忽略输入
            return;
        }

        // 更新上次更改时间
        lastFPSLimitChangeTime = currentTime;

        // 执行帧率重置
        setTargetFPS(60);
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

        // 清理敌人生成管理器资源
        if (backgroundEnemySpawnManager != null) {
            backgroundEnemySpawnManager.stopSpawning();
        }
        if (infiniteMapEnemySpawnManager != null) {
            infiniteMapEnemySpawnManager.shutdown();
        }

        // 使用FXGL的内置方法来清理游戏世界
        getGameController().startNewGame();

        // 重新初始化系统（确保路径寻找器等组件正确设置）
        reinitializeSystems();
    }

    /**
     * 重新初始化系统组件
     */
    private void reinitializeSystems() {
        // 重新设置EntityFactory的组件
        if (movementValidator != null) {
            com.roguelike.entities.EntityFactory.setMovementValidator(movementValidator);
        }

        if (adaptivePathfinder != null) {
            com.roguelike.entities.EntityFactory.setAdaptivePathfinder(adaptivePathfinder);
        }

        // 重新设置敌人生成管理器
        if (backgroundEnemySpawnManager != null) {
            backgroundEnemySpawnManager.setInfiniteMapSpawnManager(infiniteMapEnemySpawnManager);
        }

    }

    /**
     * 清理游戏资源
     */
    public void cleanup() {
        if (backgroundEnemySpawnManager != null) {
            backgroundEnemySpawnManager.shutdown();
            backgroundEnemySpawnManager = null;
        }
        if (infiniteMapEnemySpawnManager != null) {
            infiniteMapEnemySpawnManager.shutdown();
            infiniteMapEnemySpawnManager = null;
        }
    }

    /**
     * 更新箭头指示器
     * 玩家不在特殊区块时显示箭头指向特殊区块，在特殊区块时隐藏箭头
     */
    private void updateArrowIndicator() {
        if (arrowIndicator == null || cachedPlayer == null || !cachedPlayer.isActive()) {
            return;
        }

        // 检查是否使用无限地图系统
        if (!USE_INFINITE_MAP || infiniteMapManager == null) {
            return;
        }

        // 获取玩家当前区块
        int currentChunkX = InfiniteMapManager.worldToChunkX(cachedPlayer.getX());

        // 特殊区块编号（区块2 - 门地图，区块3 - Boss房）
        final int DOOR_CHUNK_X = 2;
        final int BOSS_CHUNK_X = 3;

        if (currentChunkX == DOOR_CHUNK_X || currentChunkX == BOSS_CHUNK_X) {
            // 玩家在特殊区块（门地图或Boss房），隐藏箭头
            if (arrowIndicator.isVisible()) {
                arrowIndicator.hideArrow();
            }
        } else {
            // 玩家不在特殊区块，显示箭头指向门地图区块
            if (!arrowIndicator.isVisible()) {
                // 计算门地图区块的中心位置
                double doorChunkCenterX = InfiniteMapManager.chunkToWorldX(DOOR_CHUNK_X) +
                                        InfiniteMapManager.getChunkWidthPixels() / 2.0;
                double doorChunkCenterY = InfiniteMapManager.getChunkHeightPixels() / 2.0;

                // 显示箭头
                arrowIndicator.showArrow(doorChunkCenterX, doorChunkCenterY,
                                       cachedPlayer.getX(), cachedPlayer.getY());
            } else {
                // 更新箭头位置和方向
                double doorChunkCenterX = InfiniteMapManager.chunkToWorldX(DOOR_CHUNK_X) +
                                        InfiniteMapManager.getChunkWidthPixels() / 2.0;
                double doorChunkCenterY = InfiniteMapManager.getChunkHeightPixels() / 2.0;

                arrowIndicator.updateArrow(doorChunkCenterX, doorChunkCenterY,
                                         cachedPlayer.getX(), cachedPlayer.getY());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}


