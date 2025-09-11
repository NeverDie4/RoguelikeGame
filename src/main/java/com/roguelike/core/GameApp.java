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
    private double enemySpawnAccumulator = 0.0;
    private static final double ENEMY_SPAWN_INTERVAL = 0.5;
    private static boolean INPUT_BOUND = false;
    private static final double TARGET_DT = 1.0 / 60.0; // 目标帧时长
    private int frameCount = 0; // 帧计数器，用于跳过不稳定的初始帧
    private boolean gameReady = false; // 覆盖层完成后才开始计时与更新
    
    // 地图配置
    private static final String MAP_NAME = "mapgrass"; // 当前使用的地图名称

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
        
        // 调试：打印碰撞地图信息
        mapRenderer.printCollisionInfo();

        // 玩家 - 根据地图尺寸调整初始位置
        double playerX = mapRenderer.getMapWidth() > 0 ?
            (mapRenderer.getMapWidth() * mapRenderer.getTileWidth()) / 2.0 : 640;
        double playerY = mapRenderer.getMapHeight() > 0 ?
            (mapRenderer.getMapHeight() * mapRenderer.getTileHeight()) / 2.0 : 360;

        Player player = (Player) getGameWorld().spawn("player", new SpawnData(playerX, playerY));
        
        // 为玩家设置移动验证器
        player.setMovementValidator(movementValidator);
        
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

        getInput().addAction(new UserAction("ATTACK") {
            @Override
            protected void onActionBegin() {
                com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                if (p != null) ((com.roguelike.entities.Player) p).attack();
            }
        }, KeyCode.SPACE);
        INPUT_BOUND = true;
    }

    // 对应用户需求中的 start()
    @Override
    protected void initUI() {
        Menus.hideAll();
        gameReady = false;
        // 加载阶段禁用输入，避免主角可被移动
        getInput().setProcessInput(false);
        // 显示自定义加载覆盖层：最短 3 秒，完成后淡出并开始计时
        LoadingOverlay.show(3000, () -> {
            TimeService.reset();
            frameCount = 0;
            // 恢复输入
            getInput().setProcessInput(true);
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

        // 敌人 AI 更新（使用相同的时间步长保持一致性）
        final double step = realDt;
        getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .forEach(e -> ((com.roguelike.entities.Enemy) e).updateAI(step));

        // 基于受控时间的刷怪逻辑
        enemySpawnAccumulator += realDt;
        while (enemySpawnAccumulator >= ENEMY_SPAWN_INTERVAL) {
            Entity newEnemy = getGameWorld().spawn("enemy");
            // 为新创建的敌人设置移动验证器
            if (newEnemy instanceof com.roguelike.entities.Enemy) {
                ((com.roguelike.entities.Enemy) newEnemy).setMovementValidator(movementValidator);
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
     * 获取地图渲染器实例
     */
    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public static void main(String[] args) {
        launch(args);
    }
}


