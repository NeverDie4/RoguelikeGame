package com.roguelike.entities.components;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.Texture;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色动画组件 - 支持GIF动画
 * 用于处理角色的多帧动画播放
 */
public class CharacterAnimationComponent extends Component {
    
    // 多方向动画支持
    public enum Direction {
        RIGHT,  // 向右（原有动画）
        LEFT    // 向左（反转动画）
    }
    
    private List<Texture> rightAnimationFrames = new ArrayList<>();
    private List<Texture> leftAnimationFrames = new ArrayList<>();
    private Timeline animationTimeline;
    private int currentFrameIndex = 0;
    private double frameDuration = 0.2; // 默认每帧200毫秒
    private boolean isLooping = true;
    private Direction currentDirection = Direction.RIGHT; // 默认向右
    // 使用 JSON 的 animationWidth/animationHeight 或实体尺寸进行渲染，不再使用视觉缩放倍数
    
    // 动画类型和参数
    private AnimationType animationType = AnimationType.GIF;
    private String basePath = "";
    private String filenamePattern = "";
    private int frameCount = 0;
    private double animationWidth = 32;
    private double animationHeight = 32;
    
    // 死亡动画相关已移除，改用粒子效果
    
    public enum AnimationType {
        GIF,    // GIF动画（玩家）
        PNG     // PNG动画（敌人）
    }
    
    @Override
    public void onAdded() {
        //System.out.println("CharacterAnimationComponent已添加到实体");
        if (!getCurrentAnimationFrames().isEmpty()) {
            startAnimation();
        }
    }
    
    /**
     * 获取当前方向的动画帧列表
     */
    private List<Texture> getCurrentAnimationFrames() {
        if (currentDirection == Direction.RIGHT) {
            return rightAnimationFrames;
        }
        // 当左向帧未加载时，回退使用右向帧（渲染时再水平翻转）
        return leftAnimationFrames.isEmpty() ? rightAnimationFrames : leftAnimationFrames;
    }
    
    /**
     * 加载GIF动画帧（向右方向）
     * @param basePath 基础路径
     * @param frameCount 帧数
     */
    public void loadGifAnimationFrames(String basePath, int frameCount) {
        // 设置动画类型和参数
        this.animationType = AnimationType.GIF;
        this.basePath = basePath;
        this.filenamePattern = "player_%03d.gif";
        this.frameCount = frameCount;
        
        rightAnimationFrames.clear();
        
        for (int i = 0; i < frameCount; i++) {
            String filename = String.format("player_%03d.gif", i);
            String resourcePath = basePath + "/" + filename;
            
            try {
                java.io.InputStream inputStream = getClass().getResourceAsStream("/" + resourcePath);
                if (inputStream != null) {
                    byte[] imageData = inputStream.readAllBytes();
                    inputStream.close();
                    
                    Image image = new Image(new java.io.ByteArrayInputStream(imageData));
                    if (image != null && !image.isError()) {
                        Texture frame = new Texture(image);
                        if (frame != null && frame.getImage() != null) {
                            rightAnimationFrames.add(frame);
                            System.out.println("✅ 成功加载: " + filename);
                        } else {
                            System.err.println("❌ 纹理创建失败: " + filename);
                        }
                    } else {
                        System.err.println("❌ 图像加载失败: " + filename);
                    }
                } else {
                    System.err.println("❌ 资源文件未找到: " + resourcePath);
                }
            } catch (Exception e) {
                System.err.println("❌ 加载失败: " + filename + " - " + e.getMessage());
            }
        }
        
        if (!rightAnimationFrames.isEmpty()) {
            System.out.println("✅ 玩家动画加载完成: " + rightAnimationFrames.size() + "帧");
            currentFrameIndex = 0;
            updateFrame();
            startAnimation();
        } else {
            System.err.println("❌ 玩家动画加载失败！");
        }
    }
    
    /**
     * 加载PNG动画帧（用于敌人等）
     * @param basePath 基础路径
     * @param frameCount 帧数
     * @param filenamePattern 文件名模式，如 "enemy_walk_%02d.png"
     */
    public void loadPngAnimationFrames(String basePath, int frameCount, String filenamePattern) {
        loadPngAnimationFrames(basePath, frameCount, filenamePattern, 32, 32);
    }
    
    /**
     * 加载PNG动画帧（用于敌人等，支持自定义动画大小）
     * @param basePath 基础路径
     * @param frameCount 帧数
     * @param filenamePattern 文件名模式，如 "enemy_walk_%02d.png"
     * @param animationWidth 动画宽度
     * @param animationHeight 动画高度
     */
    public void loadPngAnimationFrames(String basePath, int frameCount, String filenamePattern, 
                                     double animationWidth, double animationHeight) {
        // 设置动画类型和参数
        this.animationType = AnimationType.PNG;
        this.basePath = basePath;
        this.filenamePattern = filenamePattern;
        this.frameCount = frameCount;
        this.animationWidth = animationWidth;
        this.animationHeight = animationHeight;
        
        rightAnimationFrames.clear();
        leftAnimationFrames.clear();
        
        for (int i = 1; i <= frameCount; i++) { // 敌人动画从01开始
            String filename = String.format(filenamePattern, i);
            String resourcePath = basePath + "/" + filename;
            
            try {
                java.io.InputStream inputStream = getClass().getResourceAsStream("/" + resourcePath);
                if (inputStream != null) {
                    byte[] imageData = inputStream.readAllBytes();
                    inputStream.close();
                    
                    Image image = new Image(new java.io.ByteArrayInputStream(imageData));
                    if (image != null && !image.isError()) {
                        Texture frame = new Texture(image);
                        if (frame != null && frame.getImage() != null) {
                            rightAnimationFrames.add(frame);
                            
                            // 为向左方向创建相同的纹理（翻转将在显示时动态设置）
                            Texture leftFrame = new Texture(image);
                            leftAnimationFrames.add(leftFrame);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ 加载失败: " + filename);
            }
        }
        
        if (!rightAnimationFrames.isEmpty()) {
            //System.out.println("✅ 敌人动画加载完成: " + rightAnimationFrames.size() + "帧");
            currentFrameIndex = 0;
            updateFrame();
            startAnimation();
        } else {
            System.err.println("❌ 敌人动画加载失败！");
        }
    }
    
    // 死亡动画加载方法已移除，改用粒子效果
    
    /**
     * 加载向左方向的GIF动画帧
     * @param basePath 基础路径
     * @param frameCount 帧数
     */
    public void loadLeftGifAnimationFrames(String basePath, int frameCount) {
        // 确保动画类型已设置
        this.animationType = AnimationType.GIF;
        this.basePath = basePath;
        this.filenamePattern = "player_left_%03d.gif";
        this.frameCount = frameCount;
        
        leftAnimationFrames.clear();
        
        for (int i = 0; i < frameCount; i++) {
            String filename;
            if (i == 0) {
                // 特殊处理第一个文件，因为文件名中有空格
                filename = "player_left_000 .gif";
            } else {
                filename = String.format("player_left_%03d.gif", i);
            }
            String resourcePath = basePath + "/" + filename;
            
            try {
                java.io.InputStream inputStream = getClass().getResourceAsStream("/" + resourcePath);
                if (inputStream != null) {
                    byte[] imageData = inputStream.readAllBytes();
                    inputStream.close();
                    
                    Image image = new Image(new java.io.ByteArrayInputStream(imageData));
                    if (image != null && !image.isError()) {
                        Texture frame = new Texture(image);
                        if (frame != null && frame.getImage() != null) {
                            leftAnimationFrames.add(frame);
                            System.out.println("✅ 成功加载: " + filename);
                        } else {
                            System.err.println("❌ 纹理创建失败: " + filename);
                        }
                    } else {
                        System.err.println("❌ 图像加载失败: " + filename);
                    }
                } else {
                    System.err.println("❌ 资源文件未找到: " + resourcePath);
                }
            } catch (Exception e) {
                System.err.println("❌ 加载失败: " + filename + " - " + e.getMessage());
            }
        }
        
        if (!leftAnimationFrames.isEmpty()) {
            System.out.println("✅ 玩家向左动画加载完成: " + leftAnimationFrames.size() + "帧");
        } else {
            System.err.println("❌ 玩家向左动画加载失败！");
        }
    }
    
    /**
     * 开始动画播放
     */
    private void startAnimation() {
        if (com.roguelike.core.TimeService.isPaused()) return;
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
        
        animationTimeline = new Timeline();
        
        List<Texture> currentFrames = getCurrentAnimationFrames();
        if (currentFrames.isEmpty()) {
            System.err.println("❌ 当前方向没有动画帧，无法开始动画");
            return;
        }
        
        // 使用简单的循环逻辑：每frameDuration秒切换到下一帧
        KeyFrame keyFrame = new KeyFrame(
            Duration.seconds(frameDuration),
            e -> {
                currentFrameIndex = (currentFrameIndex + 1) % currentFrames.size();
                
                // 检查目标帧是否有效
                if (currentFrameIndex < currentFrames.size()) {
                    Texture targetTexture = currentFrames.get(currentFrameIndex);
                    if (targetTexture != null && targetTexture.getImage() != null) {
                        updateFrame();
                    } else {
                        // 尝试下一帧
                        currentFrameIndex = (currentFrameIndex + 1) % currentFrames.size();
                        if (currentFrameIndex < currentFrames.size()) {
                            updateFrame();
                        }
                    }
                }
            }
        );
        animationTimeline.getKeyFrames().add(keyFrame);
        
        // 设置循环播放
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        
        //System.out.println("✅ 动画开始播放: " + currentFrames.size() + "帧 (方向: " + currentDirection + ")");
        animationTimeline.play();
    }
    
    /**
     * 更新当前帧显示
     */
    private void updateFrame() {
        List<Texture> currentFrames = getCurrentAnimationFrames();
        
        if (!currentFrames.isEmpty() && currentFrameIndex < currentFrames.size()) {
            Texture currentTexture = currentFrames.get(currentFrameIndex);
            
            if (currentTexture != null && currentTexture.getImage() != null) {
                if (getEntity() != null && getEntity().getViewComponent() != null) {
                    // 保存血条等UI元素
                    javafx.scene.Node healthBar = null;
                    if (getEntity() instanceof com.roguelike.entities.Player) {
                        com.roguelike.entities.Player player = (com.roguelike.entities.Player) getEntity();
                        healthBar = player.getHealthBarContainer();
                    }
                    
                    getEntity().getViewComponent().clearChildren();
                    
                    // 统一的左右翻转与尺寸居中逻辑（兼容 GIF 与 PNG）
                    Image image = currentTexture.getImage();
                    if (image != null) {
                        double imgW = image.getWidth();
                        double imgH = image.getHeight();
                        // 渲染目标尺寸优先使用动画尺寸，否则使用实体尺寸
                        double baseW = animationWidth > 0 ? animationWidth : getEntity().getWidth();
                        double baseH = animationHeight > 0 ? animationHeight : getEntity().getHeight();
                        double targetW = Math.max(1.0, baseW);
                        double targetH = Math.max(1.0, baseH);
                        double scaleXAbs = targetW / Math.max(1.0, imgW);
                        double scaleY = targetH / Math.max(1.0, imgH);
                        // 左向使用负 scaleX 实现水平翻转；若GIF已提供独立左向帧则不再翻转
                        boolean shouldFlip = currentDirection == Direction.LEFT &&
                                (animationType == AnimationType.PNG || leftAnimationFrames.isEmpty());
                        double scaleX = shouldFlip ? -scaleXAbs : scaleXAbs;
                        currentTexture.setScaleX(scaleX);
                        currentTexture.setScaleY(scaleY);
                        // 实际渲染尺寸（考虑了翻转后的绝对宽度）
                        double realW = Math.abs(scaleX) * imgW;
                        double realH = Math.abs(scaleY) * imgH;
                        // 居中对齐到实体中心
                        currentTexture.setTranslateX((baseW - realW) / 2.0);
                        currentTexture.setTranslateY((baseH - realH) / 2.0);
                    }
                    
                    getEntity().getViewComponent().addChild(currentTexture);
                    
                    // 重新添加血条等UI元素
                    if (healthBar != null) {
                        getEntity().getViewComponent().addChild(healthBar);
                    }
                }
            } else {
                // 尝试重新加载这一帧
                reloadFrame(currentFrameIndex);
            }
        }
    }

    
    /**
     * 重新加载指定帧
     */
    private void reloadFrame(int frameIndex) {
        String filename;
        String resourcePath;
        List<Texture> targetFrames;
        
        if (animationType == AnimationType.GIF) {
            // GIF动画（玩家）
            boolean useRight = (currentDirection == Direction.RIGHT) || leftAnimationFrames.isEmpty();
            if (useRight) {
                filename = String.format("player_%03d.gif", frameIndex);
                resourcePath = basePath + "/" + filename;
                targetFrames = rightAnimationFrames;
            } else {
                filename = String.format("player_left_%03d.gif", frameIndex);
                resourcePath = basePath + "/" + filename;
                targetFrames = leftAnimationFrames;
            }
        } else {
            // PNG动画（敌人）
            if (currentDirection == Direction.RIGHT) {
                filename = String.format(filenamePattern, frameIndex + 1); // PNG动画从1开始
                resourcePath = basePath + "/" + filename;
                targetFrames = rightAnimationFrames;
            } else {
                filename = String.format(filenamePattern, frameIndex + 1); // PNG动画从1开始
                resourcePath = basePath + "/" + filename;
                targetFrames = leftAnimationFrames;
            }
        }
        
        try {
            java.io.InputStream inputStream = getClass().getResourceAsStream("/" + resourcePath);
            if (inputStream != null) {
                byte[] imageData = inputStream.readAllBytes();
                inputStream.close();
                
                Image image = new Image(new java.io.ByteArrayInputStream(imageData));
                if (image != null && !image.isError()) {
                    Texture frame = new Texture(image);
                    if (frame != null && frame.getImage() != null) {
                        targetFrames.set(frameIndex, frame);
                        updateFrame(); // 重新尝试显示
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理重新加载失败
        }
    }
    
    /**
     * 设置动画方向
     * @param direction 新的方向
     */
    public void setDirection(Direction direction) {
        if (this.currentDirection != direction) {
            this.currentDirection = direction;
            currentFrameIndex = 0; // 重置帧索引
            
            // 重新开始动画
            if (!getCurrentAnimationFrames().isEmpty()) {
                startAnimation();
            }
        }
    }
    
    /**
     * 获取当前方向
     */
    public Direction getCurrentDirection() {
        return currentDirection;
    }
    
    /**
     * 设置帧持续时间
     * @param duration 持续时间（秒）
     */
    public void setFrameDuration(double duration) {
        this.frameDuration = duration;
        if (!getCurrentAnimationFrames().isEmpty()) {
            startAnimation();
        }
    }
    
    /**
     * 测试方法：强制显示指定帧
     * @param frameIndex 帧索引
     */
    public void testShowFrame(int frameIndex) {
        List<Texture> currentFrames = getCurrentAnimationFrames();
        if (frameIndex >= 0 && frameIndex < currentFrames.size()) {
            currentFrameIndex = frameIndex;
            //System.out.println("🧪 测试显示第" + frameIndex + "帧 (方向: " + currentDirection + ")");
            updateFrame();
        }
    }
    
    /**
     * 设置是否循环播放
     * @param looping 是否循环
     */
    public void setLooping(boolean looping) {
        this.isLooping = looping;
    }
    
    // 死亡动画相关方法已移除，改用粒子效果

    /**
     * 设置动画渲染目标尺寸（像素）。若设置为0或以下，表示退回使用实体尺寸。
     */
    public void setAnimationSize(double width, double height) {
        this.animationWidth = width;
        this.animationHeight = height;
        updateFrame();
    }

    /** 获取动画渲染目标宽度（像素），0 表示使用实体宽度 */
    public double getAnimationWidth() {
        return animationWidth;
    }

    /** 获取动画渲染目标高度（像素），0 表示使用实体高度 */
    public double getAnimationHeight() {
        return animationHeight;
    }
}
