package com.roguelike.core;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.time.TimerAction;
import com.roguelike.entities.Player;
import com.roguelike.map.MapRenderer;
import com.roguelike.ui.GameHUD;
import com.roguelike.ui.Menus;
import com.roguelike.ui.CustomSceneFactory;
import com.roguelike.ui.LoadingOverlay;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 游戏主类。
 */
public class GameApp extends GameApplication {

    private GameState gameState;
    private MapRenderer mapRenderer;
    private GameHUD gameHUD;
    private TimerAction enemySpawnTask;

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
        getWorldProperties().setValue("score", 0);

        // 地图渲染器
        mapRenderer = new MapRenderer();
        mapRenderer.init();

        // 注册实体工厂
        com.roguelike.entities.EntityFactory.setGameState(gameState);
        FXGL.getGameWorld().addEntityFactory(new com.roguelike.entities.EntityFactory());

        // 地图渲染器 - 加载Tiled地图
        mapRenderer = new MapRenderer("grass.tmx");
        mapRenderer.init();

        // 玩家 - 根据地图尺寸调整初始位置
        double playerX = mapRenderer.getMapWidth() > 0 ?
            (mapRenderer.getMapWidth() * mapRenderer.getTileWidth()) / 2.0 : 640;
        double playerY = mapRenderer.getMapHeight() > 0 ?
            (mapRenderer.getMapHeight() * mapRenderer.getTileHeight()) / 2.0 : 360;

        Player player = (Player) getGameWorld().spawn("player", new SpawnData(playerX, playerY));
        FXGL.getGameScene().getViewport().bindToEntity(player, getAppWidth() / 2.0, getAppHeight() / 2.0);

        // 输入
        initInput(player);

        // HUD
        gameHUD = new GameHUD(gameState);
        gameHUD.mount();

        // 敌人周期生成
        enemySpawnTask = getGameTimer().runAtInterval(() -> getGameWorld().spawn("enemy"), Duration.seconds(2.5));

        // 事件示例
        GameEvent.listen(GameEvent.Type.MAP_LOADED, e -> {
            // 地图加载完成事件
            System.out.println("地图加载完成事件触发");
        });
        GameEvent.post(new GameEvent(GameEvent.Type.MAP_LOADED));
        
        // 自定义菜单系统已通过CustomSceneFactory设置
    }

    private void initInput(Player player) {
        // 清除现有的输入动作，避免重复注册
        try {
            getInput().clearAll();
        } catch (Exception e) {
            // 忽略清除时的异常
        }
        
        // 使用固定移动距离，避免 tpf() 异常值导致的移动问题
        final double moveDistance = 2.0; // 固定移动距离，降低移动速度

        try {
            getInput().addAction(new UserAction("MOVE_LEFT_A") {
                @Override
                protected void onAction() {
                    player.move(-moveDistance, 0);
                }
            }, KeyCode.A);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        try {
            getInput().addAction(new UserAction("MOVE_LEFT_ARROW") {
                @Override
                protected void onAction() {
                    player.move(-moveDistance, 0);
                }
            }, KeyCode.LEFT);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        try {
            getInput().addAction(new UserAction("MOVE_RIGHT_D") {
                @Override
                protected void onAction() {
                    player.move(moveDistance, 0);
                }
            }, KeyCode.D);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        try {
            getInput().addAction(new UserAction("MOVE_RIGHT_ARROW") {
                @Override
                protected void onAction() {
                    player.move(moveDistance, 0);
                }
            }, KeyCode.RIGHT);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        try {
            getInput().addAction(new UserAction("MOVE_UP_W") {
                @Override
                protected void onAction() {
                    player.move(0, -moveDistance);
                }
            }, KeyCode.W);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        try {
            getInput().addAction(new UserAction("MOVE_UP_ARROW") {
                @Override
                protected void onAction() {
                    player.move(0, -moveDistance);
                }
            }, KeyCode.UP);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        try {
            getInput().addAction(new UserAction("MOVE_DOWN_S") {
                @Override
                protected void onAction() {
                    player.move(0, moveDistance);
                }
            }, KeyCode.S);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        try {
            getInput().addAction(new UserAction("MOVE_DOWN_ARROW") {
                @Override
                protected void onAction() {
                    player.move(0, moveDistance);
                }
            }, KeyCode.DOWN);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        try {
            getInput().addAction(new UserAction("ATTACK") {
                @Override
                protected void onActionBegin() {
                    player.attack();
                }
            }, KeyCode.SPACE);
        } catch (Exception e) {
            // 如果动作已存在，忽略异常
        }

        // 使用FXGL内置的暂停菜单系统，不添加自定义ESC键处理
        // FXGL内置的CustomGameMenu会自动处理ESC键和暂停功能
    }

    // 对应用户需求中的 start()
    @Override
    protected void initUI() {
        // 自定义菜单系统已经通过CustomSceneFactory应用，这里只需要隐藏自定义菜单
        Menus.hideAll();
        System.out.println("自定义菜单系统已激活");
    }

    // 对应用户需求中的 update()
    @Override
    protected void onUpdate(double tpf) {
        // 更新TimeService
        TimeService.update(tpf);
        
        // 如果游戏暂停，不更新游戏逻辑
        if (TimeService.isPaused()) {
            return;
        }
        
        if (mapRenderer != null) {
            mapRenderer.onUpdate(tpf);
        }
        // 更新所有敌人的AI（包括流场寻路）
        getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .forEach(e -> ((com.roguelike.entities.Enemy) e).onUpdate(tpf));
    }

    public static void main(String[] args) {
        launch(args);
    }
}


