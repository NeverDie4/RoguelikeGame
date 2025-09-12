package com.roguelike.entities.components;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.image.Image;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 子弹动画组件，支持帧动画
 */
public class BulletAnimationComponent extends Component {
    
    private List<Texture> animationFrames;
    private Animation animation;
    private int currentFrame = 0;
    private double frameDuration = 0.1; // 每帧持续时间（秒）
    private boolean isLooping = true;
    private boolean isPlaying = false;
    
    public BulletAnimationComponent() {
        this.animationFrames = new ArrayList<>();
    }
    
    // 简单的静态缓存，避免重复 IO（保留但不强依赖）
    private static final java.util.Map<String, List<Texture>> CACHE = new java.util.HashMap<>();
    private static boolean LOGGED_FXGL_FAIL_ONCE = false;
    
    /**
     * 从资源路径加载动画帧
     */
    public void loadAnimationFrames(String basePath, int frameCount) {
        animationFrames.clear();

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

            // 1) 优先：类路径读取
            boolean loaded = false;
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
                    loaded = true;
                    //System.out.println("[BulletAnimation] 使用classpath优先加载成功: /" + cpPath);
                }
            } catch (Exception ignored) { }

            // 2) 备选：FXGL AssetLoader（若类路径未命中）
            if (!loaded) {
                try {
                    Texture texture = com.almasb.fxgl.dsl.FXGL.getAssetLoader().loadTexture(framePath);
                    animationFrames.add(texture);
                    loaded = true;
                } catch (Exception e) {
                    System.err.println("无法加载动画帧 (两种方式均失败): FXGL='" + framePath + "', CLASSPATH='/" + cpPath + "'");
                }
            }
        }

        if (!animationFrames.isEmpty()) {
            createAnimation();
            play();
        }
    }
    
    /**
     * 创建动画
     */
    private void createAnimation() {
        if (animationFrames.isEmpty()) return;
        
        animation = new Transition() {
            {
                setCycleDuration(Duration.seconds(frameDuration * animationFrames.size()));
                setInterpolator(Interpolator.LINEAR);
            }
            
            @Override
            protected void interpolate(double frac) {
                if (animationFrames.isEmpty()) return;
                
                int frameIndex = (int) (frac * animationFrames.size());
                if (frameIndex >= animationFrames.size()) {
                    frameIndex = animationFrames.size() - 1;
                }
                
                if (frameIndex != currentFrame) {
                    currentFrame = frameIndex;
                    updateTexture();
                }
            }
        };
        
        if (isLooping) {
            animation.setCycleCount(Animation.INDEFINITE);
        } else {
            animation.setCycleCount(1);
        }
    }
    
    /**
     * 更新当前显示的纹理
     */
    private void updateTexture() {
        if (currentFrame < animationFrames.size() && entity != null) {
            Texture currentTexture = animationFrames.get(currentFrame);
            
            // 清除旧的视图并添加新的纹理
            entity.getViewComponent().clearChildren();
            entity.getViewComponent().addChild(currentTexture);
        }
    }
    
    /**
     * 播放动画
     */
    public void play() {
        if (animation != null && !isPlaying) {
            animation.play();
            isPlaying = true;
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
            createAnimation();
        }
    }
    
    /**
     * 设置是否循环播放
     */
    public void setLooping(boolean looping) {
        this.isLooping = looping;
        if (animation != null) {
            createAnimation();
        }
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
