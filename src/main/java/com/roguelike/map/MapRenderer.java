package com.roguelike.map;

import com.almasb.fxgl.app.scene.GameView;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * 极简地图渲染器：渲染一块网格背景，避免外部依赖导致运行失败。
 */
public class MapRenderer {

    private Group layer;
    private GameView mapView;

    public void init() {
        layer = new Group();

        // 生成一个简单网格背景（32px 瓦片，40x23 格），避免对外部资源的依赖
        int tileSize = 32;
        int cols = 40;
        int rows = 23;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Rectangle r = new Rectangle(tileSize, tileSize);
                r.setFill(((x + y) & 1) == 0 ? Color.web("#2e3440") : Color.web("#3b4252"));
                r.setStroke(Color.web("#434c5e"));
                r.setTranslateX(x * tileSize);
                r.setTranslateY(y * tileSize);
                layer.getChildren().add(r);
            }
        }

        mapView = new GameView(layer, 0);
        FXGL.getGameScene().addGameView(mapView);
    }

    public void onUpdate(double tpf) {
        // 视口跟随已在 GameApp 中绑定
    }

    public void render(Object graphics) {
        // FXGL 使用场景图渲染，这里保留接口
    }
}


