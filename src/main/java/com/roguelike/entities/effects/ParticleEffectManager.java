package com.roguelike.entities.effects;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import javafx.geometry.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * 粒子效果管理器
 * 负责创建和管理各种粒子效果
 */
public class ParticleEffectManager {
    
    private static ParticleEffectManager instance;
    private Map<String, ParticleEffectConfig> effectConfigs;
    private Random random;
    
    private ParticleEffectManager() {
        effectConfigs = new HashMap<>();
        random = new Random();
        initializeDefaultEffects();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized ParticleEffectManager getInstance() {
        if (instance == null) {
            instance = new ParticleEffectManager();
        }
        return instance;
    }
    
    /**
     * 初始化默认粒子效果
     */
    private void initializeDefaultEffects() {
        // 爆炸效果
        ParticleEffectConfig explosion = new ParticleEffectConfig();
        explosion.setType("explosion");
        explosion.setParticleCount(20);
        explosion.setDuration(1.0);
        explosion.setColors(java.util.Arrays.asList("#FF0000", "#FFA500", "#FFFF00"));
        explosion.setSize(4.0);
        explosion.setSpeed(150.0);
        explosion.setGravity(50.0);
        explosion.setSpread(Math.PI * 2); // 360度扩散
        explosion.setFadeOut(true);
        explosion.setFadeOutDuration(0.7);
        effectConfigs.put("explosion", explosion);
        
        // 烟雾效果
        ParticleEffectConfig smoke = new ParticleEffectConfig();
        smoke.setType("smoke");
        smoke.setParticleCount(15);
        smoke.setDuration(2.0);
        smoke.setColors(java.util.Arrays.asList("#808080", "#A0A0A0", "#C0C0C0"));
        smoke.setSize(6.0);
        smoke.setSpeed(80.0);
        smoke.setGravity(-30.0); // 向上飘
        smoke.setSpread(Math.PI * 1.5); // 270度扩散
        smoke.setFadeOut(true);
        smoke.setFadeOutDuration(1.5);
        effectConfigs.put("smoke", smoke);
        
        // 闪光效果
        ParticleEffectConfig sparkle = new ParticleEffectConfig();
        sparkle.setType("sparkle");
        sparkle.setParticleCount(12);
        sparkle.setDuration(0.8);
        sparkle.setColors(java.util.Arrays.asList("#FFFFFF", "#FFFF00", "#FFD700"));
        sparkle.setSize(3.0);
        sparkle.setSpeed(200.0);
        sparkle.setGravity(0.0);
        sparkle.setSpread(Math.PI * 2);
        sparkle.setFadeOut(true);
        sparkle.setFadeOutDuration(0.5);
        effectConfigs.put("sparkle", sparkle);
        
        // 血液效果
        ParticleEffectConfig blood = new ParticleEffectConfig();
        blood.setType("blood");
        blood.setParticleCount(25);
        blood.setDuration(1.2);
        blood.setColors(java.util.Arrays.asList("#8B0000", "#DC143C", "#FF0000"));
        blood.setSize(3.0);
        blood.setSpeed(120.0);
        blood.setGravity(100.0); // 向下落
        blood.setSpread(Math.PI * 1.2); // 216度扩散
        blood.setFadeOut(true);
        blood.setFadeOutDuration(0.8);
        effectConfigs.put("blood", blood);
        
        // 魔法效果
        ParticleEffectConfig magic = new ParticleEffectConfig();
        magic.setType("magic");
        magic.setParticleCount(18);
        magic.setDuration(1.5);
        magic.setColors(java.util.Arrays.asList("#8A2BE2", "#9370DB", "#BA55D3", "#DA70D6"));
        magic.setSize(5.0);
        magic.setSpeed(100.0);
        magic.setGravity(-20.0); // 轻微向上
        magic.setSpread(Math.PI * 2);
        magic.setFadeOut(true);
        magic.setFadeOutDuration(1.0);
        effectConfigs.put("magic", magic);
        
        System.out.println("🎆 粒子效果管理器初始化完成，加载了 " + effectConfigs.size() + " 种效果");
    }
    
    /**
     * 创建粒子效果
     * @param effectType 效果类型
     * @param x X坐标
     * @param y Y坐标
     */
    public void createEffect(String effectType, double x, double y) {
        ParticleEffectConfig config = effectConfigs.get(effectType);
        if (config == null) {
            System.err.println("❌ 未知的粒子效果类型: " + effectType);
            return;
        }
        
        createEffect(config, x, y);
    }
    
    /**
     * 创建粒子效果
     * @param config 效果配置
     * @param x X坐标
     * @param y Y坐标
     */
    public void createEffect(ParticleEffectConfig config, double x, double y) {
        if (config == null) {
            System.err.println("❌ 粒子效果配置为空");
            return;
        }
        
        System.out.println("🎆 创建粒子效果: " + config.getType() + " 在位置 (" + x + ", " + y + ")");
        
        // 创建粒子
        for (int i = 0; i < config.getParticleCount(); i++) {
            createParticle(config, x, y);
        }
    }
    
    /**
     * 创建单个粒子
     */
    private void createParticle(ParticleEffectConfig config, double x, double y) {
        // 计算随机方向
        double angle = random.nextDouble() * config.getSpread() - config.getSpread() / 2;
        double speed = config.getSpeed() * (0.5 + random.nextDouble() * 0.5); // 速度变化
        
        double velocityX = Math.cos(angle) * speed;
        double velocityY = Math.sin(angle) * speed;
        Point2D velocity = new Point2D(velocityX, velocityY);
        
        // 创建粒子实体
        Entity particleEntity = spawn("particle", new SpawnData(x, y));
        
        // 添加粒子组件
        Particle particle = new Particle();
        particle.setVelocity(velocity);
        particle.setMaxLife(config.getDuration());
        particle.setLife(config.getDuration());
        particle.setColor(config.getRandomColor());
        particle.setSize(config.getSize() * (0.7 + random.nextDouble() * 0.6)); // 大小变化
        particle.setGravity(config.getGravity());
        particle.setFadeOut(config.isFadeOut());
        // 确保fadeOutStart在有效范围内
        double fadeOutStart = config.getDuration() > 0 ? 
            Math.max(0.0, Math.min(1.0, config.getFadeOutDuration() / config.getDuration())) : 0.3;
        particle.setFadeOutStart(fadeOutStart);
        
        particleEntity.addComponent(particle);
    }
    
    /**
     * 注册自定义效果配置
     */
    public void registerEffect(String type, ParticleEffectConfig config) {
        effectConfigs.put(type, config);
        System.out.println("✅ 注册自定义粒子效果: " + type);
    }
    
    /**
     * 获取效果配置
     */
    public ParticleEffectConfig getEffectConfig(String type) {
        return effectConfigs.get(type);
    }
    
    /**
     * 获取所有效果类型
     */
    public java.util.Set<String> getAvailableEffects() {
        return effectConfigs.keySet();
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("🎆 粒子效果管理器统计:\n");
        stats.append("   可用效果数量: ").append(effectConfigs.size()).append("\n");
        stats.append("   效果类型: ").append(String.join(", ", effectConfigs.keySet())).append("\n");
        
        return stats.toString();
    }
}
