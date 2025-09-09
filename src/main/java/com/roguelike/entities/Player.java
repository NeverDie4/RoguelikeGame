package com.roguelike.entities;

import com.almasb.fxgl.entity.components.CollidableComponent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public class Player extends EntityBase {

    private final double speed = 200;

    public Player() {
        Rectangle view = new Rectangle(32, 32, Color.DODGERBLUE);
        getViewComponent().addChild(view);
        addComponent(new CollidableComponent(true));
        setSize(32, 32);
    }

    public void move(double dx, double dy) {
        translate(dx, dy);
        // 移除重复的事件发送，避免在输入处理中重复触发
    }

    // 子弹发射逻辑将在后续添加


    public Point2D getPositionVec() {
        return getPosition();
    }

    public static class Types {
        public static final String PLAYER = "PLAYER";
    }
}


