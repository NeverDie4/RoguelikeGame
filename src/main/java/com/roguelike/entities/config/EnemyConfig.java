package com.roguelike.entities.config;

/**
 * 敌人配置数据类
 * 包含敌人的所有属性配置
 */
public class EnemyConfig {
    
    private String id;
    private String name;
    private EnemyStats stats;
    private EnemySize size;
    private EnemyAnimations animations;
    private EnemyCollision collision;
    private EnemyDeathEffect deathEffect;
    
    // 构造函数
    public EnemyConfig() {}
    
    public EnemyConfig(String id, String name, EnemyStats stats, EnemySize size, 
                      EnemyAnimations animations, EnemyCollision collision, EnemyDeathEffect deathEffect) {
        this.id = id;
        this.name = name;
        this.stats = stats;
        this.size = size;
        this.animations = animations;
        this.collision = collision;
        this.deathEffect = deathEffect;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public EnemyStats getStats() { return stats; }
    public void setStats(EnemyStats stats) { this.stats = stats; }
    
    public EnemySize getSize() { return size; }
    public void setSize(EnemySize size) { this.size = size; }
    
    public EnemyAnimations getAnimations() { return animations; }
    public void setAnimations(EnemyAnimations animations) { this.animations = animations; }
    
    public EnemyCollision getCollision() { return collision; }
    public void setCollision(EnemyCollision collision) { this.collision = collision; }
    
    public EnemyDeathEffect getDeathEffect() { return deathEffect; }
    public void setDeathEffect(EnemyDeathEffect deathEffect) { this.deathEffect = deathEffect; }
    
    /**
     * 敌人属性配置
     */
    public static class EnemyStats {
        private int maxHP;
        private int attack;
        private int defense;
        private int accuracy;
        private double speed;
        private int expReward;
        
        public EnemyStats() {}
        
        public EnemyStats(int maxHP, int attack, int defense, int accuracy, double speed, int expReward) {
            this.maxHP = maxHP;
            this.attack = attack;
            this.defense = defense;
            this.accuracy = accuracy;
            this.speed = speed;
            this.expReward = expReward;
        }
        
        // Getters and Setters
        public int getMaxHP() { return maxHP; }
        public void setMaxHP(int maxHP) { this.maxHP = maxHP; }
        
        public int getAttack() { return attack; }
        public void setAttack(int attack) { this.attack = attack; }
        
        public int getDefense() { return defense; }
        public void setDefense(int defense) { this.defense = defense; }
        
        public int getAccuracy() { return accuracy; }
        public void setAccuracy(int accuracy) { this.accuracy = accuracy; }
        
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        
        public int getExpReward() { return expReward; }
        public void setExpReward(int expReward) { this.expReward = expReward; }
    }
    
    /**
     * 敌人实体大小配置
     */
    public static class EnemySize {
        private double width;
        private double height;
        
        public EnemySize() {}
        
        public EnemySize(double width, double height) {
            this.width = width;
            this.height = height;
        }
        
        // Getters and Setters
        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }
        
        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }
    }
    
    /**
     * 敌人动画配置
     */
    public static class EnemyAnimations {
        private int walkFrames;
        private double frameDuration;
        private String texturePath;
        private String walkPattern;
        private double animationWidth;
        private double animationHeight;
        
        public EnemyAnimations() {}
        
        public EnemyAnimations(int walkFrames, double frameDuration, String texturePath, 
                              String walkPattern, double animationWidth, double animationHeight) {
            this.walkFrames = walkFrames;
            this.frameDuration = frameDuration;
            this.texturePath = texturePath;
            this.walkPattern = walkPattern;
            this.animationWidth = animationWidth;
            this.animationHeight = animationHeight;
        }
        
        // Getters and Setters
        public int getWalkFrames() { return walkFrames; }
        public void setWalkFrames(int walkFrames) { this.walkFrames = walkFrames; }
        
        public double getFrameDuration() { return frameDuration; }
        public void setFrameDuration(double frameDuration) { this.frameDuration = frameDuration; }
        
        public String getTexturePath() { return texturePath; }
        public void setTexturePath(String texturePath) { this.texturePath = texturePath; }
        
        public String getWalkPattern() { return walkPattern; }
        public void setWalkPattern(String walkPattern) { this.walkPattern = walkPattern; }
        
        public double getAnimationWidth() { return animationWidth; }
        public void setAnimationWidth(double animationWidth) { this.animationWidth = animationWidth; }
        
        public double getAnimationHeight() { return animationHeight; }
        public void setAnimationHeight(double animationHeight) { this.animationHeight = animationHeight; }

        // 视觉缩放已移除，统一使用 animationWidth/animationHeight 或实体尺寸
    }
    
    /**
     * 敌人碰撞箱配置
     */
    public static class EnemyCollision {
        private double width;
        private double height;
        private double offsetX;
        private double offsetY;
        
        public EnemyCollision() {}
        
        public EnemyCollision(double width, double height, double offsetX, double offsetY) {
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
        
        // Getters and Setters
        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }
        
        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }
        
        public double getOffsetX() { return offsetX; }
        public void setOffsetX(double offsetX) { this.offsetX = offsetX; }
        
        public double getOffsetY() { return offsetY; }
        public void setOffsetY(double offsetY) { this.offsetY = offsetY; }
    }
    
    /**
     * 敌人死亡效果配置
     */
    public static class EnemyDeathEffect {
        private String effectType;
        private int particleCount;
        private double duration;
        private java.util.List<String> colors;
        private double size;
        private double speed;
        private double gravity;
        private double spread;
        private boolean fadeOut;
        private double fadeOutDuration;
        
        public EnemyDeathEffect() {}
        
        public EnemyDeathEffect(String effectType, int particleCount, double duration, 
                              java.util.List<String> colors, double size, double speed) {
            this.effectType = effectType;
            this.particleCount = particleCount;
            this.duration = duration;
            this.colors = colors;
            this.size = size;
            this.speed = speed;
            this.gravity = 0.0;
            this.spread = Math.PI * 2; // 默认360度扩散
            this.fadeOut = true;
            this.fadeOutDuration = duration * 0.7;
        }
        
        // Getters and Setters
        public String getEffectType() { return effectType; }
        public void setEffectType(String effectType) { this.effectType = effectType; }
        
        public int getParticleCount() { return particleCount; }
        public void setParticleCount(int particleCount) { this.particleCount = particleCount; }
        
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
        
        public java.util.List<String> getColors() { return colors; }
        public void setColors(java.util.List<String> colors) { this.colors = colors; }
        
        public double getSize() { return size; }
        public void setSize(double size) { this.size = size; }
        
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        
        public double getGravity() { return gravity; }
        public void setGravity(double gravity) { this.gravity = gravity; }
        
        public double getSpread() { return spread; }
        public void setSpread(double spread) { this.spread = spread; }
        
        public boolean isFadeOut() { return fadeOut; }
        public void setFadeOut(boolean fadeOut) { this.fadeOut = fadeOut; }
        
        public double getFadeOutDuration() { return fadeOutDuration; }
        public void setFadeOutDuration(double fadeOutDuration) { this.fadeOutDuration = fadeOutDuration; }
    }
    
    @Override
    public String toString() {
        return "EnemyConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", stats=" + stats +
                ", size=" + size +
                ", animations=" + animations +
                ", collision=" + collision +
                '}';
    }
}
