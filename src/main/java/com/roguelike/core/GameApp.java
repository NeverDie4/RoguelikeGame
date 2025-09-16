package com.roguelike.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.time.TimerAction;
import com.roguelike.entities.Player;
import com.roguelike.map.MapRenderer;
import com.roguelike.physics.MapCollisionDetector;
import com.roguelike.physics.MovementValidator;
import com.roguelike.utils.AdaptivePathfinder;
import com.roguelike.ui.GameHUD;
import com.roguelike.ui.Menus;
import com.roguelike.ui.CustomSceneFactory;
import com.roguelike.ui.LoadingOverlay;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

// 请你完美解决，保证不产生其他问题并不影响其他功能
// 你看看有什么不明白的地方，有的话告诉我，没的话先列出待办事项，先不写代码

/**
 * 游戏主类。
 */
public class GameApp extends GameApplication {

    private GameState gameState;
    private MapRenderer mapRenderer;
    private GameHUD gameHUD;
    private com.roguelike.ui.PassiveItemManager passiveManager;
    private TimerAction enemySpawnTask;

    private com.roguelike.entities.weapons.WeaponManager weaponManager;
    private MapCollisionDetector collisionDetector;
    private MovementValidator movementValidator;
    private AdaptivePathfinder adaptivePathfinder;
    private double enemySpawnAccumulator = 0.0;
    private static final double ENEMY_SPAWN_INTERVAL = 2;
    private static boolean INPUT_BOUND = false;
    private static final double TARGET_DT = 1.0 / 60.0; // 目标帧时长
    private int frameCount = 0; // 帧计数器，用于跳过不稳定的初始帧
    private boolean gameReady = false; // 覆盖层完成后才开始计时与更新

    // 地图配置（由关卡选择界面赋值）
    private static String selectedMapName = "map1"; // 默认地图

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

        // 取消初期加载覆盖层：改为先进行关卡选择，选择后再进入游戏

        // 重置时间服务 - 确保从暂停状态恢复
        TimeService.reset();
        // 确保时间服务处于正常状态
        if (TimeService.isPaused()) {
            TimeService.resume();
        }
        System.out.println("游戏初始化：时间服务状态已重置");

        // 初始化音频管理器，确保音量设置正确应用
        try {
            com.roguelike.audio.AudioManager audioManager = com.roguelike.audio.AudioManager.getInstance();
            // 触发音量设置，确保MusicService使用正确的音量
            audioManager.setMusicVolume(audioManager.getMusicVolume());
            audioManager.setSoundEffectsVolume(audioManager.getSoundEffectsVolume());
            audioManager.setMasterVolume(audioManager.getMasterVolume());
            System.out.println("游戏初始化：音频管理器已初始化，音乐音量: " + (audioManager.getMusicVolume() * 100) + "%");
        } catch (Throwable ignored) {}

        gameState = new GameState();
        // 注入到全局，供 Bullet 等通过 FXGL.geto("gameState") 访问
        com.almasb.fxgl.dsl.FXGL.set("gameState", gameState);
        
        // 重置武器和被动物品获得顺序
        com.roguelike.ui.ObtainedWeaponsOrder.INSTANCE.reset();
        com.roguelike.ui.ObtainedPassivesOrder.INSTANCE.reset();
        System.out.println("游戏初始化：已重置武器和被动物品获得顺序");
        
        weaponManager = new com.roguelike.entities.weapons.WeaponManager();
        passiveManager = new com.roguelike.ui.PassiveItemManager();
        // 暴露给全局，便于发射组件查询
        com.almasb.fxgl.dsl.FXGL.set("weaponManager", weaponManager);
        com.almasb.fxgl.dsl.FXGL.set("passiveManager", passiveManager);
        // 暴露 gameState 给子弹等逻辑使用（如击杀加经验）
        com.almasb.fxgl.dsl.FXGL.set("gameState", gameState);
        getWorldProperties().setValue("score", 0);
        TimeService.reset();
        frameCount = 0;
        com.roguelike.entities.Enemy.resetNavigation();

        // 注册实体工厂：每次新游戏都注册，确保 GameWorld 持有工厂
        com.roguelike.entities.EntityFactory.setGameState(gameState);
        FXGL.getGameWorld().addEntityFactory(new com.roguelike.entities.EntityFactory());

        // 启动前先选择地图（确保在FX线程）
        javafx.application.Platform.runLater(() -> com.roguelike.ui.MapSelectMenu.show(new com.roguelike.ui.MapSelectMenu.OnSelect() {
            @Override
            public void onChoose(String mapId) {
                selectedMapName = mapId != null && !mapId.isEmpty() ? mapId : selectedMapName;
                // 地图渲染器 - 加载Tiled地图
                mapRenderer = new MapRenderer(selectedMapName);
                mapRenderer.init();
                afterMapReady();
            }

            @Override
            public void onCancel() {
                // 返回主菜单
                getGameController().gotoMainMenu();
                try { com.roguelike.ui.MusicService.playLobby(); } catch (Exception ignored) {}
            }
        }));

        // 碰撞、路径与调试打印均推迟到地图加载完成后创建（afterMapReady）

        // 调试：打印路径寻找配置
        System.out.println("🎯 路径寻找系统配置:");
        System.out.println("   - 敌人数量阈值: " + ENEMY_COUNT_THRESHOLD);
        System.out.println("   - 允许对角线移动: " + ALLOW_DIAGONAL_MOVEMENT);
        System.out.println("   - 路径更新间隔: " + PATHFINDING_UPDATE_INTERVAL + "秒");
        System.out.println("   - 路径优化: " + ENABLE_PATH_OPTIMIZATION);
        System.out.println("   - 路径平滑: " + ENABLE_PATH_SMOOTHING);

        // 其余初始化推迟到地图加载后

        // 事件示例
        GameEvent.listen(GameEvent.Type.MAP_LOADED, e -> {
            // 地图加载完成事件
            System.out.println("地图加载完成事件触发");
        });
        GameEvent.post(new GameEvent(GameEvent.Type.MAP_LOADED));

        // 监听升级事件，弹出升级界面（暂停游戏），并防止重复弹出
        GameEvent.listen(GameEvent.Type.LEVEL_UP, e -> {
            try {
                com.roguelike.ui.UpgradeOverlay.enqueueOrShow(weaponManager, passiveManager);
            } catch (Throwable ignored) {}
        });

        // 自定义菜单系统已通过CustomSceneFactory设置
    }

    private void afterMapReady() {
        // 玩家 - 根据地图尺寸调整初始位置
        double playerX = mapRenderer.getMapWidth() > 0 ?
            (mapRenderer.getMapWidth() * mapRenderer.getTileWidth()) / 2.0 : 640;
        double playerY = mapRenderer.getMapHeight() > 0 ?
            (mapRenderer.getMapHeight() * mapRenderer.getTileHeight()) / 2.0 : 360;

        // 初始化碰撞检测系统（依赖 mapRenderer）
        collisionDetector = new MapCollisionDetector(mapRenderer);
        movementValidator = new MovementValidator(collisionDetector);

        // 调试：打印碰撞地图信息
        mapRenderer.printCollisionInfo();

        // 初始化自适应路径寻找系统（此时 mapRenderer 已可用）
        AdaptivePathfinder.PathfindingConfig config = new AdaptivePathfinder.PathfindingConfig();
        config.setEnemyCountThreshold(ENEMY_COUNT_THRESHOLD);
        config.setAllowDiagonal(ALLOW_DIAGONAL_MOVEMENT);
        config.setPathfindingUpdateInterval(PATHFINDING_UPDATE_INTERVAL);
        config.setEnablePathOptimization(ENABLE_PATH_OPTIMIZATION);
        config.setEnableSmoothing(ENABLE_PATH_SMOOTHING);
        adaptivePathfinder = new AdaptivePathfinder(mapRenderer, config);

        Player player = (Player) getGameWorld().spawn("player", new SpawnData(playerX, playerY));
        // 应用被动：最大HP加成（P04）
        try {
            Object pmObj = com.almasb.fxgl.dsl.FXGL.geto("passiveManager");
            if (pmObj instanceof com.roguelike.ui.PassiveItemManager pm) {
                int bonus = pm.getMaxHpBonus();
                if (bonus > 0) {
                    gameState.setPlayerMaxHP(Math.max(1, gameState.getPlayerMaxHP() + bonus));
                }
            }
        } catch (Throwable ignored) {}

        // 为玩家设置移动验证器
        player.setMovementValidator(movementValidator);

        FXGL.getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2.0, getAppHeight() / 2.0);

        // 输入
        initInput(player);

        // HUD
        gameHUD = new GameHUD(gameState);
        gameHUD.mount();

        // 敌人周期生成
        enemySpawnTask = getGameTimer().runAtInterval(() -> getGameWorld().spawn("enemy"), Duration.seconds(2.5));

        // 切到战斗音乐
        try { com.roguelike.ui.MusicService.playBattle(); } catch (Exception ignored) {}

        // 地图加载完成后，恢复输入与计时
        getInput().setProcessInput(true);
        if (gameHUD != null) { gameHUD.resumeTime(); }
        TimeService.reset();
        TimeService.startGame();
        frameCount = 0;
        gameReady = true;
    }

    private void initInput(Player player) {
        if (INPUT_BOUND) {
            return;
        }
        // 使用固定移动距离，避免 tpf() 异常值导致的移动问题
        final double moveDistance = 2.0; // 增加移动距离，提高转向速度

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

        // ESC 键：根据游戏状态播放相应的点击音效
        getInput().addAction(new UserAction("PAUSE_RESUME_SFX") {
            @Override
            protected void onActionBegin() {
                // 检测当前游戏状态并播放相应的音效
                if (com.roguelike.core.TimeService.isPaused()) {
                    // 游戏处于暂停状态，按ESC会恢复游戏
                    try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
                    System.out.println("ESC键恢复游戏，播放按键音效");
                } else {
                    // 游戏正在运行，按ESC会暂停游戏
                    try { com.roguelike.ui.SoundService.playOnce("clicks/click.mp3"); } catch (Throwable ignored) {}
                    System.out.println("ESC键暂停游戏，播放按键音效");
                }
            }
        }, KeyCode.ESCAPE);

        // 旧的空格攻击移除，采用自动发射
        INPUT_BOUND = true;
    }

    // 对应用户需求中的 start()
    @Override
    protected void initUI() {
        // 自定义菜单系统已经通过CustomSceneFactory应用，这里只需要隐藏自定义菜单
        try { Menus.hideAll(); } catch (Exception ignored) {}
        System.out.println("自定义菜单系统已激活");
        gameReady = false;
        // 进入游戏前先进行关卡选择，此处不再显示加载覆盖层
        // 禁用输入，待地图加载完成后在 afterMapReady 中开启
        getInput().setProcessInput(false);
        if (gameHUD != null) { gameHUD.pauseTime(); }
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

        // 更新敌人数量并选择路径寻找算法
        int enemyCount = (int) getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .count();
        if (adaptivePathfinder != null) {
            adaptivePathfinder.updateEnemyCount(enemyCount);
        }

        // 敌人 AI 更新（使用相同的时间步长保持一致性）
        final double step = realDt;
        getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .forEach(e -> ((com.roguelike.entities.Enemy) e).updateAI(step));

        // 简易碰撞检测：子弹 vs 敌人（用于伤害与经验结算）
        checkBulletEnemyCollisions();

        // 基于受控时间的刷怪逻辑
        enemySpawnAccumulator += realDt;
        while (enemySpawnAccumulator >= ENEMY_SPAWN_INTERVAL) {
            Entity newEnemy = getGameWorld().spawn("enemy");
            // 为新创建的敌人设置移动验证器和路径寻找器
            if (newEnemy instanceof com.roguelike.entities.Enemy) {
                ((com.roguelike.entities.Enemy) newEnemy).setMovementValidator(movementValidator);
                ((com.roguelike.entities.Enemy) newEnemy).setAdaptivePathfinder(adaptivePathfinder);
            }
            enemySpawnAccumulator -= ENEMY_SPAWN_INTERVAL;
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
    public MovementValidator getMovementValidator() {
        return movementValidator;
    }

    /**
     * 获取地图渲染器实例
     */
    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    /**
     * 获取自适应路径寻找器实例
     */
    public AdaptivePathfinder getAdaptivePathfinder() {
        return adaptivePathfinder;
    }

    public static void main(String[] args) {
        launch(args);
    }
}


