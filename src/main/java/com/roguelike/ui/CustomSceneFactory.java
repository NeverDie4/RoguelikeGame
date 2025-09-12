package com.roguelike.ui;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;

/**
 * 自定义场景工厂，使用美化后的菜单系统
 */
public class CustomSceneFactory extends SceneFactory {

    @Override
    public FXGLMenu newMainMenu() {
        return new CustomMainMenu();
    }

    @Override
    public FXGLMenu newGameMenu() {
        return new CustomGameMenu();
    }
}

