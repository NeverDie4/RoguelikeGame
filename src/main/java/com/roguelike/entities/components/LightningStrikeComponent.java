package com.roguelike.entities.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * 一次性落雷：播放 bullets/06 动画一遍，并对圆形区域敌人造成一次伤害。
 * 颜色偏冷并提高显眼度；总动画时长=1.2s。
 */
public class LightningStrikeComponent extends Component {
    private final double radius;
    private final int damage;
    private final double visualScale;
    private ImageView view;
    private Timeline anim;

    public LightningStrikeComponent(double radius, int damage, double visualScale) {
        this.radius = Math.max(1.0, radius);
        this.damage = Math.max(0, damage);
        this.visualScale = Math.max(0.1, visualScale);
    }

    @Override
    public void onAdded() {
        view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);

        // 偏冷且更显眼：加深饱和、轻微提亮、增加亮斑与辉光
        ColorAdjust ca = new ColorAdjust();
        ca.setHue(-0.45);       // 冷色调
        ca.setSaturation(0.55); // 更鲜明
        ca.setBrightness(0.08); // 稍亮

        Glow glow = new Glow(0.6);
        Bloom bloom = new Bloom(0.3);
        glow.setInput(ca);
        bloom.setInput(glow);
        view.setEffect(bloom);
        view.setOpacity(0.95);

        entity.getViewComponent().addChild(view);
        playOnce();
        applyDamageOnce();
    }

    private void playOnce() {
        if (com.roguelike.core.TimeService.isPaused()) return;
        java.util.List<Image> frames = new java.util.ArrayList<>();
        // 尝试加载最多30帧，允许缺帧
        for (int i = 0; i < 30; i++) {
            String p1 = String.format("assets/textures/bullets/06/1_%03d.png", i);
            Image img = null;
            try (java.io.InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(p1)) {
                if (is != null) img = new Image(is);
            } catch (Exception ignored) {}
            if (img == null) {
                if (!frames.isEmpty()) break;
            } else {
                frames.add(img);
            }
        }
        if (frames.isEmpty()) return;

        // 适配尺寸
        view.setImage(frames.get(0));
        double size = radius * 2.0 * visualScale;
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setTranslateX(-size / 2.0);
        view.setTranslateY(-size / 2.0);

        // 动画：总时长 1.2 秒，平均分配到每帧
        final double total = 1.2;
        double dt = total / frames.size();
        anim = new Timeline();
        anim.setCycleCount(1);
        for (int i = 0; i < frames.size(); i++) {
            Image img = frames.get(i);
            anim.getKeyFrames().add(new KeyFrame(Duration.seconds(dt * (i + 1)), e -> view.setImage(img)));
        }
        anim.setOnFinished(e -> entity.removeFromWorld());
        anim.play();

        // 守护计时，避免偶发未结束
        new Timeline(new KeyFrame(Duration.seconds(total + 0.05), e -> {
            if (entity != null && entity.isActive()) entity.removeFromWorld();
        })).play();
    }

    private void applyDamageOnce() {
        double cx = entity.getCenter().getX();
        double cy = entity.getCenter().getY();
        com.almasb.fxgl.dsl.FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .map(e -> (com.roguelike.entities.Enemy) e)
                .filter(en -> en.getCenter().distance(cx, cy) <= radius)
                .forEach(en -> en.takeDamage(damage));
    }
}


