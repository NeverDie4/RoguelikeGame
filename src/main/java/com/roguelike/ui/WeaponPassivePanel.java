package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;

/**
 * 左上角武器/被动物品面板：第一行8个武器，第二行6个被动。
 */
public final class WeaponPassivePanel extends StackPane {
    private final GridPane grid = new GridPane();

    public WeaponPassivePanel() {
        setPickOnBounds(false);
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(6));
        grid.setHgap(6);
        grid.setVgap(6);
        getChildren().add(grid);

        // 先绘制空槽（8+6）
        for (int i = 0; i < 8; i++) grid.add(makeSlot(null, null), i, 0);
        for (int i = 0; i < 6; i++) grid.add(makeSlot(null, null), i, 1);

        // 监听事件刷新（任意武器/被动变化时重绘）。
        GameEvent.listen(GameEvent.Type.WEAPON_UPGRADED, e -> javafx.application.Platform.runLater(this::refresh));
        GameEvent.listen(GameEvent.Type.PASSIVE_ACQUIRED, e -> javafx.application.Platform.runLater(this::refresh));
        GameEvent.listen(GameEvent.Type.PASSIVE_UPGRADED, e -> javafx.application.Platform.runLater(this::refresh));

        refresh();
    }

    private Node makeSlot(String path, String text) {
        StackPane cell = new StackPane();
        cell.setPrefSize(36, 36);
        Rectangle bg = new Rectangle(36, 36, Color.color(0,0,0,0.3));
        bg.setStroke(Color.GOLD);
        bg.setStrokeWidth(2.0);
        cell.getChildren().add(bg);
        if (path != null) {
            Image fxImage = loadImageFromClasspath(path);
            if (fxImage != null && fxImage.getWidth() > 0 && !fxImage.isError()) {
                ImageView iv = new ImageView(fxImage);
                iv.setFitWidth(32); iv.setFitHeight(32);
                iv.setPreserveRatio(true); iv.setSmooth(true);
                cell.getChildren().add(iv);
            }
        }
        if (text != null && !text.isEmpty()) {
            Label lv = new Label(text);
            lv.setTextFill(Color.WHITE);
            lv.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.6); -fx-padding: 1 3 1 3;");
            StackPane.setAlignment(lv, Pos.BOTTOM_RIGHT);
            cell.getChildren().add(lv);
        }
        return cell;
    }

    private Image loadImageFromClasspath(String path) {
        // 尝试多种类加载器与路径组合
        InputStream is = null;
        try {
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null) is = ctx.getResourceAsStream(path);
            if (is == null) is = WeaponPassivePanel.class.getClassLoader().getResourceAsStream(path);
            if (is == null) is = WeaponPassivePanel.class.getResourceAsStream("/" + path);
            if (is == null) is = ClassLoader.getSystemResourceAsStream(path);
            if (is != null) {
                Image img = new Image(is);
                try { is.close(); } catch (Exception ignored) {}
                if (img != null && img.getWidth() > 0 && !img.isError()) return img;
            }
        } catch (Exception ignored) {
            try { if (is != null) is.close(); } catch (Exception ignore2) {}
        }

        // 回退：ImageIO（支持 webp）
        try {
            javax.imageio.ImageIO.scanForPlugins();
            java.net.URL url = null;
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null) url = ctx.getResource(path);
            if (url == null) url = WeaponPassivePanel.class.getClassLoader().getResource(path);
            if (url == null) url = WeaponPassivePanel.class.getResource("/" + path);
            if (url == null) url = ClassLoader.getSystemResource(path);
            if (url != null) {
                java.awt.image.BufferedImage awtImg = javax.imageio.ImageIO.read(url);
                if (awtImg != null) {
                    return javafx.embed.swing.SwingFXUtils.toFXImage(awtImg, null);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void refresh() {
        grid.getChildren().clear();
        // 武器图标：按获得顺序展示（从 WeaponManager 读取，初版：01 总在首位，其余按获得事件顺序缓存）
        java.util.List<String> order = ObtainedWeaponsOrder.INSTANCE.getOrder();
        int col = 0;
        for (String idx : order) {
            String icon = WeaponIconProvider.pathFor(idx);
            // 获取武器等级
            com.roguelike.entities.weapons.WeaponId wid = com.roguelike.entities.weapons.WeaponId.valueOf("W" + idx);
            int level = 0;
            try {
                Object wmObj = com.almasb.fxgl.dsl.FXGL.geto("weaponManager");
                if (wmObj instanceof com.roguelike.entities.weapons.WeaponManager wm) {
                    level = wm.getLevel(wid);
                }
            } catch (Throwable ignored) {}
            String levelText = level > 0 ? String.valueOf(level) : null;
            grid.add(makeSlot(icon, levelText), Math.min(col, 7), 0);
            col++;
            if (col >= 8) break;
        }
        // 若不足8格，用占位补齐
        while (col < 8) { grid.add(makeSlot(null, null), col, 0); col++; }
        // 被动物品图标：按获得顺序展示（首次到1级时会触发 PASSIVE_ACQUIRED）
        java.util.List<PassiveId> pOrder = ObtainedPassivesOrder.INSTANCE.getOrder();
        int pcol = 0;
        for (PassiveId pid : pOrder) {
            String icon = PassiveIconProvider.pathFor(pid);
            // 获取被动物品等级
            int level = 0;
            try {
                Object pmObj = com.almasb.fxgl.dsl.FXGL.geto("passiveManager");
                if (pmObj instanceof com.roguelike.ui.PassiveItemManager pm) {
                    level = pm.getLevel(pid);
                }
            } catch (Throwable ignored) {}
            String levelText = level > 0 ? String.valueOf(level) : null;
            grid.add(makeSlot(icon, levelText), Math.min(pcol, 7), 1);
            pcol++;
            if (pcol >= 6) break;
        }
        while (pcol < 6) { grid.add(makeSlot(null, null), pcol, 1); pcol++; }
    }

    public void attachToScene() {
        setTranslateX(6);
        // 放到经验条下方一点：按窗口高度的约6%
        try {
            setTranslateY(FXGL.getAppHeight() * 0.085);
        } catch (Throwable t) {
            setTranslateY(64); // 兜底
        }
        FXGL.getGameScene().addUINode(this);
    }
}


