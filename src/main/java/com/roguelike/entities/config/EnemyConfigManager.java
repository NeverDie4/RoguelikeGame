package com.roguelike.entities.config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 敌人配置管理器
 * 负责管理所有敌人配置，提供查询和缓存功能
 */
public class EnemyConfigManager {
    
    private static EnemyConfigManager instance;
    private Map<String, EnemyConfig> enemyConfigs;
    private List<EnemyConfig> allConfigs;
    private boolean initialized = false;
    
    private EnemyConfigManager() {
        enemyConfigs = new ConcurrentHashMap<>();
        allConfigs = new ArrayList<>();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized EnemyConfigManager getInstance() {
        if (instance == null) {
            instance = new EnemyConfigManager();
        }
        return instance;
    }
    
    /**
     * 初始化配置管理器
     */
    public void initialize() {
        if (initialized) {
            System.out.println("⚠️ 敌人配置管理器已经初始化过了");
            return;
        }
        
        System.out.println("🎯 初始化敌人配置管理器...");
        
        try {
            // 加载所有敌人配置
            allConfigs = EnemyConfigLoader.loadEnemyConfigs();
            
            // 验证并缓存配置
            for (EnemyConfig config : allConfigs) {
                if (EnemyConfigLoader.validateConfig(config)) {
                    enemyConfigs.put(config.getId(), config);
                    System.out.println("✅ 缓存敌人配置: " + config.getId());
                } else {
                    System.err.println("❌ 跳过无效配置: " + config.getId());
                }
            }
            
            initialized = true;
            System.out.println("🎯 敌人配置管理器初始化完成，共加载 " + enemyConfigs.size() + " 个有效配置");
            
        } catch (Exception e) {
            System.err.println("❌ 敌人配置管理器初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 根据ID获取敌人配置
     * @param id 敌人ID
     * @return 敌人配置，如果不存在返回null
     */
    public EnemyConfig getEnemyConfig(String id) {
        if (!initialized) {
            System.err.println("❌ 配置管理器未初始化，请先调用initialize()");
            return null;
        }
        
        return enemyConfigs.get(id);
    }
    
    /**
     * 获取所有敌人配置
     * @return 所有敌人配置列表
     */
    public List<EnemyConfig> getAllEnemyConfigs() {
        if (!initialized) {
            System.err.println("❌ 配置管理器未初始化，请先调用initialize()");
            return new ArrayList<>();
        }
        
        return new ArrayList<>(allConfigs);
    }
    
    /**
     * 获取所有敌人ID
     * @return 敌人ID列表
     */
    public List<String> getAllEnemyIds() {
        if (!initialized) {
            System.err.println("❌ 配置管理器未初始化，请先调用initialize()");
            return new ArrayList<>();
        }
        
        return new ArrayList<>(enemyConfigs.keySet());
    }
    
    /**
     * 检查敌人配置是否存在
     * @param id 敌人ID
     * @return 是否存在
     */
    public boolean hasEnemyConfig(String id) {
        if (!initialized) {
            return false;
        }
        
        return enemyConfigs.containsKey(id);
    }
    
    /**
     * 根据名称搜索敌人配置
     * @param name 敌人名称
     * @return 匹配的敌人配置列表
     */
    public List<EnemyConfig> searchEnemyConfigsByName(String name) {
        if (!initialized) {
            System.err.println("❌ 配置管理器未初始化，请先调用initialize()");
            return new ArrayList<>();
        }
        
        List<EnemyConfig> results = new ArrayList<>();
        for (EnemyConfig config : allConfigs) {
            if (config.getName().toLowerCase().contains(name.toLowerCase())) {
                results.add(config);
            }
        }
        return results;
    }
    
    /**
     * 根据血量范围筛选敌人配置
     * @param minHP 最小血量
     * @param maxHP 最大血量
     * @return 符合条件的敌人配置列表
     */
    public List<EnemyConfig> filterEnemyConfigsByHP(int minHP, int maxHP) {
        if (!initialized) {
            System.err.println("❌ 配置管理器未初始化，请先调用initialize()");
            return new ArrayList<>();
        }
        
        List<EnemyConfig> results = new ArrayList<>();
        for (EnemyConfig config : allConfigs) {
            int hp = config.getStats().getMaxHP();
            if (hp >= minHP && hp <= maxHP) {
                results.add(config);
            }
        }
        return results;
    }
    
    /**
     * 根据攻击力范围筛选敌人配置
     * @param minAttack 最小攻击力
     * @param maxAttack 最大攻击力
     * @return 符合条件的敌人配置列表
     */
    public List<EnemyConfig> filterEnemyConfigsByAttack(int minAttack, int maxAttack) {
        if (!initialized) {
            System.err.println("❌ 配置管理器未初始化，请先调用initialize()");
            return new ArrayList<>();
        }
        
        List<EnemyConfig> results = new ArrayList<>();
        for (EnemyConfig config : allConfigs) {
            int attack = config.getStats().getAttack();
            if (attack >= minAttack && attack <= maxAttack) {
                results.add(config);
            }
        }
        return results;
    }
    
    /**
     * 随机获取一个敌人配置
     * @return 随机敌人配置，如果没有配置返回null
     */
    public EnemyConfig getRandomEnemyConfig() {
        if (!initialized || allConfigs.isEmpty()) {
            return null;
        }
        
        Random random = new Random();
        int index = random.nextInt(allConfigs.size());
        return allConfigs.get(index);
    }
    
    /**
     * 获取配置统计信息
     * @return 统计信息字符串
     */
    public String getStatistics() {
        if (!initialized) {
            return "配置管理器未初始化";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("🎯 敌人配置管理器统计信息:\n");
        stats.append("   总配置数量: ").append(allConfigs.size()).append("\n");
        stats.append("   有效配置数量: ").append(enemyConfigs.size()).append("\n");
        
        if (!allConfigs.isEmpty()) {
            // 计算血量范围
            int minHP = allConfigs.stream().mapToInt(c -> c.getStats().getMaxHP()).min().orElse(0);
            int maxHP = allConfigs.stream().mapToInt(c -> c.getStats().getMaxHP()).max().orElse(0);
            stats.append("   血量范围: ").append(minHP).append(" - ").append(maxHP).append("\n");
            
            // 计算攻击力范围
            int minAttack = allConfigs.stream().mapToInt(c -> c.getStats().getAttack()).min().orElse(0);
            int maxAttack = allConfigs.stream().mapToInt(c -> c.getStats().getAttack()).max().orElse(0);
            stats.append("   攻击力范围: ").append(minAttack).append(" - ").append(maxAttack).append("\n");
            
            // 计算速度范围
            double minSpeed = allConfigs.stream().mapToDouble(c -> c.getStats().getSpeed()).min().orElse(0);
            double maxSpeed = allConfigs.stream().mapToDouble(c -> c.getStats().getSpeed()).max().orElse(0);
            stats.append("   速度范围: ").append(String.format("%.1f", minSpeed)).append(" - ").append(String.format("%.1f", maxSpeed)).append("\n");
        }
        
        return stats.toString();
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        System.out.println("🔄 重新加载敌人配置...");
        enemyConfigs.clear();
        allConfigs.clear();
        initialized = false;
        initialize();
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        enemyConfigs.clear();
        allConfigs.clear();
        initialized = false;
        System.out.println("🗑️ 敌人配置缓存已清理");
    }
    
    /**
     * 检查是否已初始化
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}
