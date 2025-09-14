package com.roguelike.entities.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * 实体被移除时，原地播放一次性爆炸动画，并对范围内敌人造成 AOE 伤害。
 */
public class ExplosionOnDestroyComponent extends Component {
    private final String animBasePath; // 资源基路径，如 assets/textures/bullets/09
    private final int frameCount;
    private final double frameDuration;
    private final double visualScale;
    private final double radius;
    private final int damage;

    public ExplosionOnDestroyComponent(String animBasePath, int frameCount, double frameDuration,
                                       double visualScale, double radius, int damage) {
        this.animBasePath = animBasePath;
        this.frameCount = Math.max(1, frameCount);
        this.frameDuration = Math.max(0.01, frameDuration);
        this.visualScale = Math.max(0.1, visualScale);
        this.radius = Math.max(1.0, radius);
        this.damage = Math.max(0, damage);
    }

    @Override
    public void onRemoved() {
        // 记录中心
        double cx = entity.getCenter().getX();
        double cy = entity.getCenter().getY();

        // AOE 伤害：在半径内的敌人扣血
        com.almasb.fxgl.dsl.FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .map(e -> (com.roguelike.entities.Enemy) e)
                .filter(en -> en.getCenter().distance(cx, cy) <= radius)
                .forEach(en -> en.takeDamage(damage));

        // 爆炸动画实体
        com.almasb.fxgl.entity.Entity explosion = new com.roguelike.entities.EntityBase();
        explosion.getTransformComponent().setPosition(cx, cy);

        // 载入帧
        java.util.List<Image> imgs = new java.util.ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            String path = String.format("%s/1_%03d.png", animBasePath, i);
            String cp = path.startsWith("assets/") ? path : ("assets/textures/" + path);
            try (java.io.InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(cp)) {
                if (is != null) imgs.add(new Image(is));
            } catch (Exception ignored) {}
        }
        if (imgs.isEmpty()) return;

        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setImage(imgs.get(0));
        // 以爆炸半径近似设定渲染大小
        double size = radius * 2 * visualScale;
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setTranslateX(-size / 2.0);
        view.setTranslateY(-size / 2.0);
        explosion.getViewComponent().addChild(view);

        com.almasb.fxgl.dsl.FXGL.getGameWorld().addEntity(explosion);

        Timeline tl = new Timeline();
        // 只播放一遍
        tl.setCycleCount(1);
        for (int i = 0; i < imgs.size(); i++) {
            Image img = imgs.get(i);
            tl.getKeyFrames().add(new KeyFrame(Duration.seconds(frameDuration * (i + 1)), e -> view.setImage(img)));
        }
        tl.setOnFinished(e -> explosion.removeFromWorld());
        tl.play();

        // 保险：到达总时长后强制移除（避免偶发的 onFinished 未触发）
        double total = frameDuration * imgs.size();
        Timeline guard = new Timeline(new KeyFrame(Duration.seconds(total + 0.05), e -> {
            if (explosion.isActive()) {
                explosion.removeFromWorld();
            }
        }));
        guard.setCycleCount(1);
        guard.play();
    }
}


