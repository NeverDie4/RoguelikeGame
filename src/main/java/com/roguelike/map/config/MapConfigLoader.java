package com.roguelike.map.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 地图配置加载器：从 resources/configs/maps/map_config.json 加载。
 */
public class MapConfigLoader {

    private static final String CONFIG_PATH = "/configs/maps/map_config.json";
    private static MapConfig cached;

    public static MapConfig load() {
        if (cached != null) {
            return cached;
        }
        try {
            InputStream in = MapConfigLoader.class.getResourceAsStream(CONFIG_PATH);
            if (in == null) {
                throw new IllegalStateException("未找到配置文件: " + CONFIG_PATH);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                Gson gson = new GsonBuilder().create();
                cached = gson.fromJson(reader, MapConfig.class);
                if (cached == null) {
                    throw new IllegalStateException("配置解析为空: " + CONFIG_PATH);
                }
                return cached;
            }
        } catch (Exception e) {
            throw new RuntimeException("加载地图配置失败: " + e.getMessage(), e);
        }
    }
}


