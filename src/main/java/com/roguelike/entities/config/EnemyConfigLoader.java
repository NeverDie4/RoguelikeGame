package com.roguelike.entities.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 敌人配置JSON加载器
 * 负责从JSON文件加载敌人配置数据
 */
public class EnemyConfigLoader {
    
    private static final String DEFAULT_CONFIG_PATH = "configs/enemies/enemy_types.json";
    
    /**
     * 从默认路径加载敌人配置
     */
    public static List<EnemyConfig> loadEnemyConfigs() {
        return loadEnemyConfigs(DEFAULT_CONFIG_PATH);
    }
    
    /**
     * 从指定路径加载敌人配置
     * @param configPath 配置文件路径
     * @return 敌人配置列表
     */
    public static List<EnemyConfig> loadEnemyConfigs(String configPath) {
        List<EnemyConfig> configs = new ArrayList<>();
        
        try {
            System.out.println("🎯 开始加载敌人配置: " + configPath);
            
            // 从资源文件加载JSON
            InputStream inputStream = EnemyConfigLoader.class.getResourceAsStream("/" + configPath);
            if (inputStream == null) {
                System.err.println("❌ 无法找到配置文件: " + configPath);
                return configs;
            }
            
            // 解析JSON
            JsonElement jsonElement = JsonParser.parseReader(new InputStreamReader(inputStream));
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            
            // 获取enemyTypes数组
            JsonArray enemyTypesArray = jsonObject.getAsJsonArray("enemyTypes");
            if (enemyTypesArray == null) {
                System.err.println("❌ JSON文件中没有找到enemyTypes数组");
                return configs;
            }
            
            // 解析每个敌人配置
            for (JsonElement element : enemyTypesArray) {
                try {
                    EnemyConfig config = parseEnemyConfig(element.getAsJsonObject());
                    if (config != null) {
                        configs.add(config);
                        System.out.println("✅ 成功加载敌人配置: " + config.getId() + " - " + config.getName());
                    }
                } catch (Exception e) {
                    System.err.println("❌ 解析敌人配置失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            inputStream.close();
            System.out.println("🎯 敌人配置加载完成，共加载 " + configs.size() + " 个敌人类型");
            
        } catch (Exception e) {
            System.err.println("❌ 加载敌人配置文件失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return configs;
    }
    
    /**
     * 解析单个敌人配置
     */
    private static EnemyConfig parseEnemyConfig(JsonObject jsonObject) {
        try {
            EnemyConfig config = new EnemyConfig();
            
            // 基本信息
            config.setId(getStringValue(jsonObject, "id"));
            config.setName(getStringValue(jsonObject, "name"));
            
            // 解析stats
            JsonObject statsObject = jsonObject.getAsJsonObject("stats");
            if (statsObject != null) {
                EnemyConfig.EnemyStats stats = new EnemyConfig.EnemyStats();
                stats.setMaxHP(getIntValue(statsObject, "maxHP"));
                stats.setAttack(getIntValue(statsObject, "attack"));
                stats.setDefense(getIntValue(statsObject, "defense"));
                stats.setAccuracy(getIntValue(statsObject, "accuracy"));
                stats.setSpeed(getDoubleValue(statsObject, "speed"));
                stats.setExpReward(getIntValue(statsObject, "expReward"));
                config.setStats(stats);
            }
            
            // 解析size
            JsonObject sizeObject = jsonObject.getAsJsonObject("size");
            if (sizeObject != null) {
                EnemyConfig.EnemySize size = new EnemyConfig.EnemySize();
                size.setWidth(getDoubleValue(sizeObject, "width"));
                size.setHeight(getDoubleValue(sizeObject, "height"));
                config.setSize(size);
            }
            
            // 解析animations
            JsonObject animationsObject = jsonObject.getAsJsonObject("animations");
            if (animationsObject != null) {
                EnemyConfig.EnemyAnimations animations = new EnemyConfig.EnemyAnimations();
                animations.setWalkFrames(getIntValue(animationsObject, "walkFrames"));
                animations.setFrameDuration(getDoubleValue(animationsObject, "frameDuration"));
                animations.setTexturePath(getStringValue(animationsObject, "texturePath"));
                animations.setWalkPattern(getStringValue(animationsObject, "walkPattern"));
                animations.setAnimationWidth(getDoubleValue(animationsObject, "animationWidth"));
                animations.setAnimationHeight(getDoubleValue(animationsObject, "animationHeight"));
                config.setAnimations(animations);
            }
            
            // 解析collision
            JsonObject collisionObject = jsonObject.getAsJsonObject("collision");
            if (collisionObject != null) {
                EnemyConfig.EnemyCollision collision = new EnemyConfig.EnemyCollision();
                collision.setWidth(getDoubleValue(collisionObject, "width"));
                collision.setHeight(getDoubleValue(collisionObject, "height"));
                collision.setOffsetX(getDoubleValue(collisionObject, "offsetX", 0.0));
                collision.setOffsetY(getDoubleValue(collisionObject, "offsetY", 0.0));
                config.setCollision(collision);
            }
            
            // 解析deathEffect
            JsonObject deathEffectObject = jsonObject.getAsJsonObject("deathEffect");
            if (deathEffectObject != null) {
                EnemyConfig.EnemyDeathEffect deathEffect = new EnemyConfig.EnemyDeathEffect();
                deathEffect.setEffectType(getStringValue(deathEffectObject, "effectType"));
                deathEffect.setParticleCount(getIntValue(deathEffectObject, "particleCount"));
                deathEffect.setDuration(getDoubleValue(deathEffectObject, "duration"));
                deathEffect.setSize(getDoubleValue(deathEffectObject, "size"));
                deathEffect.setSpeed(getDoubleValue(deathEffectObject, "speed"));
                deathEffect.setGravity(getDoubleValue(deathEffectObject, "gravity", 0.0));
                deathEffect.setSpread(getDoubleValue(deathEffectObject, "spread", Math.PI * 2));
                deathEffect.setFadeOut(getBooleanValue(deathEffectObject, "fadeOut", true));
                deathEffect.setFadeOutDuration(getDoubleValue(deathEffectObject, "fadeOutDuration", 0.7));
                
                // 解析颜色数组
                JsonArray colorsArray = deathEffectObject.getAsJsonArray("colors");
                if (colorsArray != null) {
                    java.util.List<String> colors = new java.util.ArrayList<>();
                    for (JsonElement colorElement : colorsArray) {
                        colors.add(colorElement.getAsString());
                    }
                    deathEffect.setColors(colors);
                }
                
                config.setDeathEffect(deathEffect);
            }
            
            return config;
            
        } catch (Exception e) {
            System.err.println("❌ 解析敌人配置时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取字符串值
     */
    private static String getStringValue(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        return element != null ? element.getAsString() : "";
    }
    
    /**
     * 获取整数值
     */
    private static int getIntValue(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        return element != null ? element.getAsInt() : 0;
    }
    
    /**
     * 获取双精度值
     */
    private static double getDoubleValue(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        return element != null ? element.getAsDouble() : 0.0;
    }
    
    /**
     * 获取双精度值（带默认值）
     */
    private static double getDoubleValue(JsonObject jsonObject, String key, double defaultValue) {
        JsonElement element = jsonObject.get(key);
        return element != null ? element.getAsDouble() : defaultValue;
    }
    
    /**
     * 获取布尔值（带默认值）
     */
    private static boolean getBooleanValue(JsonObject jsonObject, String key, boolean defaultValue) {
        JsonElement element = jsonObject.get(key);
        return element != null ? element.getAsBoolean() : defaultValue;
    }
    
    /**
     * 验证配置完整性
     */
    public static boolean validateConfig(EnemyConfig config) {
        if (config == null) {
            System.err.println("❌ 配置为空");
            return false;
        }
        
        if (config.getId() == null || config.getId().isEmpty()) {
            System.err.println("❌ 敌人ID不能为空");
            return false;
        }
        
        if (config.getName() == null || config.getName().isEmpty()) {
            System.err.println("❌ 敌人名称不能为空");
            return false;
        }
        
        if (config.getStats() == null) {
            System.err.println("❌ 敌人属性配置不能为空");
            return false;
        }
        
        if (config.getSize() == null) {
            System.err.println("❌ 敌人大小配置不能为空");
            return false;
        }
        
        if (config.getAnimations() == null) {
            System.err.println("❌ 敌人动画配置不能为空");
            return false;
        }
        
        if (config.getCollision() == null) {
            System.err.println("❌ 敌人碰撞配置不能为空");
            return false;
        }
        
        // 验证数值合理性
        if (config.getStats().getMaxHP() <= 0) {
            System.err.println("❌ 敌人血量必须大于0");
            return false;
        }
        
        if (config.getSize().getWidth() <= 0 || config.getSize().getHeight() <= 0) {
            System.err.println("❌ 敌人大小必须大于0");
            return false;
        }
        
        if (config.getAnimations().getWalkFrames() <= 0) {
            System.err.println("❌ 行走动画帧数必须大于0");
            return false;
        }
        
        if (config.getCollision().getWidth() <= 0 || config.getCollision().getHeight() <= 0) {
            System.err.println("❌ 碰撞箱大小必须大于0");
            return false;
        }
        
        return true;
    }
}
