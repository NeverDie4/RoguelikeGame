package com.roguelike.entities.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * 围绕玩家的光环：显示一圈动画/贴图，按间隔对范围内敌人造成伤害。
 * 参数从 WeaponManager 读取（半径、伤害、间隔、等级）。
 */
public class AuraDamageComponent extends Component {
    private ImageView view;
    private Timeline tickTimer;
    private com.almasb.fxgl.entity.Entity target; // 目标（玩家）
    private Timeline animTimeline; // 循环播放光环动画
    private java.util.List<Image> frames;
    private double frameDurationSec = 0.07; // 默认值，会被动态计算覆盖
    private javafx.scene.effect.ColorAdjust colorAdjust;

    @Override
    public void onAdded() {
        ensureView();
        ensureTimer();
    }

    @Override
    public void onUpdate(double tpf) {
        if (com.roguelike.core.TimeService.isPaused()) return;
        // 跟随目标到中心
        if (target != null) {
            entity.getTransformComponent().setPosition(target.getCenter().getX(), target.getCenter().getY());
        }
        // 每帧更新尺寸与居中
        updateVisual();
        // 动态更新动画播放速度（当武器等级变化时）
        updateAnimationSpeed();
    }

    private void ensureView() {
        if (view != null) return;
        view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        // 加载 bullets/04 帧序列并启动循环动画
        loadFrames();
        startAnimation();
        // 调整为红黄色调：提升对比、降低亮度、提升饱和度
        colorAdjust = new javafx.scene.effect.ColorAdjust();
        colorAdjust.setContrast(0.3);
        colorAdjust.setBrightness(-0.1);
        colorAdjust.setSaturation(0.4);
        // 调整为暖色调（红黄色）
        colorAdjust.setHue(0.1);
        view.setEffect(colorAdjust);
        entity.getViewComponent().addChild(view);
        updateVisual();
    }

    private void updateVisual() {
        double r = com.roguelike.entities.weapons.WeaponManager.getWeapon04Radius();
        if (r <= 0) {
            if (view != null) view.setVisible(false);
            return;
        }
        if (view != null) view.setVisible(true);
        double size = r * 2.0;
        view.setFitWidth(size);
        view.setFitHeight(size);
        // 居中到实体位置（假设实体绑定在玩家上）
        view.setTranslateX(-size / 2.0);
        view.setTranslateY(-size / 2.0);
        // 提高可见度：更高透明度（不透明度）
        int lv = com.roguelike.entities.weapons.WeaponManager.getWeapon04Level();
        view.setOpacity(Math.min(1.0, 0.75 + lv * 0.06));
    }

    private void ensureTimer() {
        if (tickTimer != null) tickTimer.stop();
        double interval = com.roguelike.entities.weapons.WeaponManager.getWeapon04TickInterval();
        tickTimer = new Timeline(new KeyFrame(Duration.seconds(Math.max(0.05, interval)), e -> doDamageTick()));
        tickTimer.setCycleCount(Timeline.INDEFINITE);
        tickTimer.play();
    }

    private void doDamageTick() {
        double r = com.roguelike.entities.weapons.WeaponManager.getWeapon04Radius();
        if (r <= 0) return;
        int dmg = com.roguelike.entities.weapons.WeaponManager.getWeapon04Damage();
        double cx = (target != null ? target.getCenter().getX() : entity.getCenter().getX());
        double cy = (target != null ? target.getCenter().getY() : entity.getCenter().getY());
        com.almasb.fxgl.dsl.FXGL.getGameWorld().getEntitiesByType().stream()
                .filter(e -> e instanceof com.roguelike.entities.Enemy)
                .map(e -> (com.roguelike.entities.Enemy) e)
                .filter(en -> en.getCenter().distance(cx, cy) <= r)
                .forEach(en -> en.takeDamage(dmg));
    }

    public void setTarget(com.almasb.fxgl.entity.Entity target) {
        this.target = target;
    }

    private void loadFrames() {
        frames = new java.util.ArrayList<>();
        // 尝试加载 0..29 帧，允许资源缺失时停止
        for (int i = 0; i < 30; i++) {
            String p1 = String.format("assets/textures/bullets/04/1_%03d.png", i);
            Image img = null;
            try (java.io.InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(p1)) {
                if (is != null) img = new Image(is);
            } catch (Exception ignored) {}
            if (img == null) {
                // 备用：不含前缀 1_
                String p2 = String.format("assets/textures/bullets/04/%03d.png", i);
                try (java.io.InputStream is2 = Thread.currentThread().getContextClassLoader().getResourceAsStream(p2)) {
                    if (is2 != null) img = new Image(is2);
                } catch (Exception ignored) {}
            }
            if (img == null) {
                // 若已至少有1帧，遇到缺帧则停止
                if (!frames.isEmpty()) break;
            } else {
                frames.add(img);
            }
        }
        // 保底：若没有帧，尝试首帧
        if (frames.isEmpty()) {
            try (java.io.InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/textures/bullets/04/1_000.png")) {
                if (is != null) frames.add(new Image(is));
            } catch (Exception ignored) {}
        }
        if (!frames.isEmpty()) {
            view.setImage(frames.get(0));
        }
    }

    private void startAnimation() {
        if (animTimeline != null) {
            animTimeline.stop();
        }
        if (frames == null || frames.isEmpty()) return;
        
        // 动态计算帧间隔：与攻击频率成比例且更慢
        updateFrameDuration();
        
        animTimeline = new Timeline();
        animTimeline.setCycleCount(Timeline.INDEFINITE);
        // 使用依次递进的 KeyFrame 循环播放
        for (int i = 0; i < frames.size(); i++) {
            Image img = frames.get(i);
            animTimeline.getKeyFrames().add(new KeyFrame(
                    Duration.seconds(frameDurationSec * (i + 1)),
                    e -> view.setImage(img)
            ));
        }
        animTimeline.play();
    }
    
    /**
     * 动态计算帧间隔：让动画播放速度与攻击频率成比例且更慢
     * 一个完整的动画循环时间 = 攻击间隔 × 2
     * 帧间隔 = (攻击间隔 × 2) ÷ 帧数
     */
    private void updateFrameDuration() {
        double attackInterval = com.roguelike.entities.weapons.WeaponManager.getWeapon04TickInterval();
        int frameCount = frames != null ? frames.size() : 4; // 默认4帧
        // 让动画播放更慢：完整循环时间 = 攻击间隔 × 2
        frameDurationSec = (attackInterval * 2.0) / frameCount;
    }
    
    /**
     * 动态更新动画播放速度（当武器等级变化时）
     */
    private void updateAnimationSpeed() {
        if (animTimeline == null || frames == null || frames.isEmpty()) return;
        
        // 计算新的帧间隔
        double newFrameDuration = calculateNewFrameDuration();
        
        // 如果帧间隔发生变化，重新启动动画
        if (Math.abs(newFrameDuration - frameDurationSec) > 0.001) {
            frameDurationSec = newFrameDuration;
            startAnimation(); // 重新启动动画以应用新的帧间隔
        }
    }
    
    /**
     * 计算新的帧间隔
     */
    private double calculateNewFrameDuration() {
        double attackInterval = com.roguelike.entities.weapons.WeaponManager.getWeapon04TickInterval();
        int frameCount = frames != null ? frames.size() : 4;
        return (attackInterval * 2.0) / frameCount;
    }

    @Override
    public void onRemoved() {
        if (tickTimer != null) tickTimer.stop();
        if (animTimeline != null) animTimeline.stop();
    }
}


