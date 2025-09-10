package com.roguelike.entities;

import com.almasb.fxgl.dsl.components.ProjectileComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.roguelike.core.GameEvent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;

import static com.almasb.fxgl.dsl.FXGL.*;

public class Player extends EntityBase {

    private final double speed = 200;
    private Rectangle hpBar;
    private Rectangle hpBarBackground;
    private StackPane hpBarContainer;
    private int maxHP = 100;
    private int currentHP = 100;

    public Player() {
        Rectangle view = new Rectangle(32, 32, Color.DODGERBLUE);
        getViewComponent().addChild(view);
        addComponent(new CollidableComponent(true));
        setSize(32, 32);
        
        // 初始化血条
        initHealthBar();
    }
    
    private void initHealthBar() {
        // 根据角色大小计算血条尺寸
        double characterWidth = getWidth();
        double characterHeight = getHeight();
        
        // 血条宽度为角色宽度的1.2倍，高度为角色高度的0.2倍
        double hpBarWidth = characterWidth * 1.2;
        double hpBarHeight = characterHeight * 0.2;
        
        // 血条背景 - 使用圆角矩形
        hpBarBackground = new Rectangle(hpBarWidth, hpBarHeight, Color.color(0, 0, 0, 0.8));
        hpBarBackground.setArcWidth(hpBarHeight * 0.5); // 圆角半径为高度的一半
        hpBarBackground.setArcHeight(hpBarHeight * 0.5);
        hpBarBackground.setStroke(Color.color(1, 1, 1, 0.9));
        hpBarBackground.setStrokeWidth(1.5);
        
        // 血条 - 使用圆角矩形
        hpBar = new Rectangle(hpBarWidth, hpBarHeight, Color.LIMEGREEN);
        hpBar.setArcWidth(hpBarHeight * 0.5);
        hpBar.setArcHeight(hpBarHeight * 0.5);
        
        // 血条容器
        hpBarContainer = new StackPane();
        hpBarContainer.getChildren().addAll(hpBarBackground, hpBar);
        
        // 将血条添加到实体的视图组件中
        getViewComponent().addChild(hpBarContainer);
        
        // 设置血条位置（在角色脚下，保持适当距离）
        double distanceFromCharacter = characterHeight * 0.3; // 距离为角色高度的30%
        hpBarContainer.setTranslateY(characterHeight / 2 + distanceFromCharacter); // 角色中心下方
        hpBarContainer.setTranslateX(-(hpBarWidth - characterWidth) / 2); // 居中对齐
        
        // 监听血量变化事件
        GameEvent.listen(GameEvent.Type.PLAYER_HURT, e -> updateHealthBar());
    }
    
    public void updateHealthBar() {
        double ratio = maxHP <= 0 ? 0 : (double) currentHP / (double) maxHP;
        hpBar.setWidth(44 * Math.max(0, Math.min(1, ratio)));
        
        // 根据血量改变颜色，使用更丰富的颜色渐变
        if (ratio > 0.7) {
            hpBar.setFill(Color.LIMEGREEN);
        } else if (ratio > 0.4) {
            hpBar.setFill(Color.ORANGE);
        } else if (ratio > 0.2) {
            hpBar.setFill(Color.color(1.0, 0.5, 0.0)); // 深橙色
        } else {
            hpBar.setFill(Color.RED);
        }
    }
    
    public void takeDamage(int damage) {
        currentHP = Math.max(0, currentHP - damage);
        updateHealthBar();
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_HURT));
    }
    
    public void heal(int amount) {
        currentHP = Math.min(maxHP, currentHP + amount);
        updateHealthBar();
    }
    
    public int getCurrentHP() {
        return currentHP;
    }
    
    public int getMaxHP() {
        return maxHP;
    }
    
    // 测试方法：模拟受到伤害
    public void testTakeDamage() {
        takeDamage(20);
    }
    
    // 测试方法：模拟治疗
    public void testHeal() {
        heal(15);
    }

    public void move(double dx, double dy) {
        translate(dx, dy);
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_MOVE));
    }

    public void attack() { //这个函数有问题，后面新建一个子弹类用于区分友方子弹和敌方子弹，再传入entityBuilder()里
        // 简单攻击：发射一个向右的投射体
        Entity bullet = entityBuilder()
                // 从玩家位置出发（基于玩家中心调整）
                .at(getCenter().subtract(0, 2))
                .viewWithBBox(new Rectangle(8, 4, Color.ORANGE))
                .at(getCenter().subtract(0, 2))
                .with(new CollidableComponent(true))
                .with(new ProjectileComponent(new Point2D(1, 0), 500))
                .buildAndAttach();
    }

    public Point2D getPositionVec() {
        return getPosition();
    }

    public static class Types {
        public static final String PLAYER = "PLAYER";
    }
}


