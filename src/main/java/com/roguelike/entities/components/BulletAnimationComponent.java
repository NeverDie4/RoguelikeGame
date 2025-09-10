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
    
    /**
     * 从资源路径加载动画帧
     */
    public void loadAnimationFrames(String basePath, int frameCount) {
        animationFrames.clear();
        
        for (int i = 0; i < frameCount; i++) {
            String framePath = String.format("%s/1_%03d.png", basePath, i);
            try {
                Image image = new Image(framePath);
                Texture texture = new Texture(image);
                animationFrames.add(texture);
            } catch (Exception e) {
                System.err.println("无法加载动画帧: " + framePath + " - " + e.getMessage());
            }
        }
        
        if (!animationFrames.isEmpty()) {
            createAnimation();
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
