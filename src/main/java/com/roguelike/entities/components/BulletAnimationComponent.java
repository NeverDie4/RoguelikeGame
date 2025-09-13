package com.roguelike.entities.components;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;
import javafx.animation.Animation;
// removed unused imports
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

/**
 * 子弹动画组件，支持帧动画
 */
public class BulletAnimationComponent extends Component {
    
    private static final boolean DEBUG = false; // 关闭调试日志

    private List<Texture> animationFrames;
    private List<Image> rawImages;
    private Animation animation;
    private int currentFrame = 0;
    private double frameDuration = 0.1; // 每帧持续时间（秒）
    private boolean isLooping = true;
    private boolean isPlaying = false;
    private ImageView spriteView;
    // 视觉放大倍数（不改变碰撞盒），用于统一放大所有子弹外观
    private double visualScale = 1.5;
    // 视觉旋转角度（度），发射时定死
    private double visualRotationDegrees = 0.0;
    // 播放完是否移除实体（仅在非循环时生效）
    private boolean removeOnFinish = false;
    
    public BulletAnimationComponent() {
        this.animationFrames = new ArrayList<>();
        this.rawImages = new ArrayList<>();
    }
    
    // 移除未使用字段，避免警告
    
    /**
     * 从资源路径加载动画帧
     */
    public void loadAnimationFrames(String basePath, int frameCount) {
        animationFrames.clear();
        rawImages.clear();

        String normalized = basePath == null ? "" : basePath.trim();
        // 统一分隔符
        normalized = normalized.replace('\\', '/');
        // 去掉所有前导斜杠
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        // 去掉 assets/ 与 textures/ 前缀（兼容多种书写）
        if (normalized.startsWith("assets/textures/")) {
            normalized = normalized.substring("assets/textures/".length());
        } else if (normalized.startsWith("assets/")) {
            normalized = normalized.substring("assets/".length());
            if (normalized.startsWith("textures/")) {
                normalized = normalized.substring("textures/".length());
            }
        } else if (normalized.startsWith("textures/")) {
            normalized = normalized.substring("textures/".length());
        }
        // 移除末尾斜杠
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        for (int i = 0; i < frameCount; i++) {
            String framePath = String.format("%s/1_%03d.png", normalized, i);
            String cpPath = "assets/textures/" + framePath;

            boolean loaded = false;
            // 仅使用类路径同步加载，避免异步导致的闪烁与 null 图像
            try {
                java.io.InputStream is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(cpPath);
                if (is == null) {
                    is = getClass().getResourceAsStream("/" + cpPath);
                }
                if (is != null) {
                    Image img = new Image(is);
                    is.close();
                    Texture t = new Texture(img);
                    animationFrames.add(t);
                    rawImages.add(img);
                    if (DEBUG) {
                        System.out.println("[BulletAnim] Loaded(frame=" + i + ") from CP: " + cpPath +
                                ", size=" + img.getWidth() + "x" + img.getHeight());
                    }
                    loaded = true;
                }
            } catch (Exception ignored) { }

            // 备选：同目录去掉前缀 1_ 的编号（如 000.png）
            if (!loaded) {
                try {
                    String altFile = String.format("%s/%03d.png", normalized, i);
                    String altCpPath = "assets/textures/" + altFile;
                    java.io.InputStream is2 = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(altCpPath);
                    if (is2 == null) {
                        is2 = getClass().getResourceAsStream("/" + altCpPath);
                    }
                    if (is2 != null) {
                        Image img2 = new Image(is2);
                        is2.close();
                        Texture t2 = new Texture(img2);
                        animationFrames.add(t2);
                        rawImages.add(img2);
                        if (DEBUG) {
                            System.out.println("[BulletAnim] Loaded ALT(frame=" + i + ") from CP: " + altCpPath +
                                    ", size=" + img2.getWidth() + "x" + img2.getHeight());
                        }
                        loaded = true;
                    }
                } catch (Exception ignored2) { }
            }

            if (!loaded) {
                System.out.println("跳过无法加载的帧: " + cpPath);
            }
        }

        if (!animationFrames.isEmpty()) {
            if (DEBUG) {
                System.out.println("[BulletAnim] Total frames loaded: " + animationFrames.size());
            }
            // 预创建视图并显示首帧，减少首次闪烁
            if (spriteView == null) {
                spriteView = new ImageView();
            }
            if (currentFrame < 0 || currentFrame >= rawImages.size()) {
                currentFrame = 0;
            }
            if (!rawImages.isEmpty()) {
                spriteView.setImage(rawImages.get(currentFrame));
            }
            createAnimation();
            play();
        }
    }
    
    /**
     * 创建动画
     */
    private void createAnimation() {
        if (animationFrames.isEmpty()) return;

        // 使用 Timeline 的双 KeyFrame（0s 与 frameDuration）来保证周期稳定重复
        Timeline timeline = new Timeline();

        KeyFrame kfStart = new KeyFrame(Duration.ZERO, e -> {
            if (animationFrames.isEmpty()) return;
            // 确保每个周期开始时渲染当前帧
            updateTexture();
        });

        KeyFrame kfStep = new KeyFrame(Duration.seconds(frameDuration), e -> {
            if (animationFrames.isEmpty()) return;
            int next = currentFrame + 1;
            // 非循环模式：播放到最后一帧后停止在最后一帧
            if (!isLooping && next >= animationFrames.size()) {
                currentFrame = animationFrames.size() - 1;
                updateTexture();
                if (animation != null) {
                    animation.stop();
                    isPlaying = false;
                }
                if (removeOnFinish && entity != null && entity.isActive()) {
                    entity.removeFromWorld();
                }
                return;
            }
            if (next >= animationFrames.size()) {
                next = 0;
            }
            if (next != currentFrame) {
                currentFrame = next;
                updateTexture();
            }
        });

        timeline.getKeyFrames().setAll(kfStart, kfStep);
        // 始终使用无限循环，由 kfStep 内逻辑在非循环模式下自动停止
        timeline.setCycleCount(Animation.INDEFINITE);
        if (DEBUG) {
            System.out.println("[BulletAnim] createAnimation: frames=" + animationFrames.size() +
                    ", frameDuration=" + frameDuration + ", looping=" + isLooping);
        }

        // 显示第一帧
        if (currentFrame < 0 || currentFrame >= animationFrames.size()) {
            currentFrame = 0;
        }
        updateTexture();

        animation = timeline;
    }
    
    /**
     * 更新当前显示的纹理
     */
    private void updateTexture() {
        if (entity == null || entity.getViewComponent() == null) return;
        if (rawImages.isEmpty()) return;
        if (currentFrame < 0 || currentFrame >= rawImages.size()) return;

        if (spriteView == null) {
            spriteView = new ImageView();
        }

        // 首次添加：只维护一个视图节点，避免每帧清空/添加引发闪烁
        if (!entity.getViewComponent().getChildren().contains(spriteView)) {
            entity.getViewComponent().clearChildren();
            entity.getViewComponent().addChild(spriteView);
        }

        Image img = rawImages.get(currentFrame);
        spriteView.setImage(img);
        // 自适应缩放到实体尺寸并按视觉倍数放大，避免不同帧分辨率产生大小跳变
        spriteView.setPreserveRatio(true);
        spriteView.setSmooth(true);
        double targetW = Math.max(1.0, entity.getWidth()) * visualScale;
        double targetH = Math.max(1.0, entity.getHeight()) * visualScale;
        spriteView.setFitWidth(targetW);
        spriteView.setFitHeight(targetH);
        // 居中对齐：放大后向四周溢出，保持实体中心不变
        spriteView.setTranslateX((entity.getWidth() - targetW) / 2.0);
        spriteView.setTranslateY((entity.getHeight() - targetH) / 2.0);
        // 以中心为轴进行旋转
        spriteView.getTransforms().setAll(new Rotate(visualRotationDegrees, targetW / 2.0, targetH / 2.0));

        if (DEBUG) {
            System.out.println("[BulletAnim] show frame=" + currentFrame +
                    ", size=" + (img != null ? (img.getWidth() + "x" + img.getHeight()) : "null") +
                    ", childrenCount=" + entity.getViewComponent().getChildren().size());
        }
    }

    /**
     * 设置视觉放大倍数（仅影响渲染，不改变碰撞盒）。
     */
    public void setVisualScale(double scale) {
        this.visualScale = Math.max(0.1, scale);
        // 立刻应用到当前帧
        updateTexture();
    }

    /**
     * 设置视觉旋转角度（度）。发射时调用一次即可。
     */
    public void setVisualRotationDegrees(double degrees) {
        this.visualRotationDegrees = degrees;
        updateTexture();
    }
    
    /**
     * 播放动画
     */
    public void play() {
        if (animation != null && !isPlaying) {
            animation.play();
            isPlaying = true;
            if (DEBUG) {
                System.out.println("[BulletAnim] play()");
            }
        }
    }
    
    /**
     * 停止动画
     */
    public void stop() {
        if (animation != null && isPlaying) {
            animation.stop();
            isPlaying = false;
        }
    }
    
    /**
     * 暂停动画
     */
    public void pauseAnimation() {
        if (animation != null && isPlaying) {
            animation.pause();
            isPlaying = false;
        }
    }
    
    /**
     * 设置帧持续时间
     */
    public void setFrameDuration(double duration) {
        this.frameDuration = duration;
        if (animation != null) {
            // 保存当前循环与播放状态，重建后恢复
            boolean wasLooping = this.isLooping;
            boolean wasPlaying = this.isPlaying;
            createAnimation();
            animation.setCycleCount(wasLooping ? Animation.INDEFINITE : 1);
            if (wasPlaying) {
                animation.play();
                this.isPlaying = true;
            }
        }
    }
    
    /**
     * 设置是否循环播放
     */
    public void setLooping(boolean looping) {
        this.isLooping = looping;
        if (animation != null) {
            // 同步循环次数；若需要从单次切换为循环或相反，直接应用
            animation.setCycleCount(looping ? Animation.INDEFINITE : 1);
        }
    }

    /**
     * 设置在非循环模式下，播放结束是否移除实体。
     */
    public void setRemoveOnFinish(boolean remove) {
        this.removeOnFinish = remove;
    }
    
    /**
     * 获取当前帧索引
     */
    public int getCurrentFrame() {
        return currentFrame;
    }
    
    /**
     * 获取总帧数
     */
    public int getFrameCount() {
        return animationFrames.size();
    }
    
    /**
     * 检查动画是否正在播放
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    @Override
    public void onAdded() {
        // 组件添加到实体时自动播放动画
        play();
    }
}
