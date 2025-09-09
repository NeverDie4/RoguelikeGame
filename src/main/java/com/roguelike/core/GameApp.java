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
    }

    // 对应用户需求中的 init()
    @Override
    protected void initGame() {
        gameState = new GameState();
        getWorldProperties().setValue("score", 0);

        // 注册实体工厂
        FXGL.getGameWorld().addEntityFactory(new com.roguelike.entities.GameEntityFactory());

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
    }

    private void initInput(Player player) {
        // 使用固定移动距离，避免 tpf() 异常值导致的移动问题
        final double moveDistance = 2.0; // 固定移动距离，降低移动速度
        
        getInput().addAction(new UserAction("MOVE_LEFT_A") {
            @Override
            protected void onAction() {
                player.move(-moveDistance, 0);
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("MOVE_LEFT_ARROW") {
            @Override
            protected void onAction() {
                player.move(-moveDistance, 0);
            }
        }, KeyCode.LEFT);

        getInput().addAction(new UserAction("MOVE_RIGHT_D") {
            @Override
            protected void onAction() {
                player.move(moveDistance, 0);
            }
        }, KeyCode.D);

        getInput().addAction(new UserAction("MOVE_RIGHT_ARROW") {
            @Override
            protected void onAction() {
                player.move(moveDistance, 0);
            }
        }, KeyCode.RIGHT);

        getInput().addAction(new UserAction("MOVE_UP_W") {
            @Override
            protected void onAction() {
                player.move(0, -moveDistance);
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("MOVE_UP_ARROW") {
            @Override
            protected void onAction() {
                player.move(0, -moveDistance);
            }
        }, KeyCode.UP);

        getInput().addAction(new UserAction("MOVE_DOWN_S") {
            @Override
            protected void onAction() {
                player.move(0, moveDistance);
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("MOVE_DOWN_ARROW") {
            @Override
            protected void onAction() {
                player.move(0, moveDistance);
            }
        }, KeyCode.DOWN);

        // 攻击输入将在后续添加
    }

    // 对应用户需求中的 start()
    @Override
    protected void initUI() {
        Menus.hideAll();
    }

    // 对应用户需求中的 update()
    @Override
    protected void onUpdate(double tpf) {
        if (mapRenderer != null) {
            mapRenderer.onUpdate(tpf);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}


