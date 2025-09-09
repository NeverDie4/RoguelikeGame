package com.roguelike.entities;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.BoundingBoxComponent;
import com.almasb.fxgl.entity.components.TransformComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

public class EntityBase extends Entity {

    public EntityBase() {
        getTransformComponent();
        if (getBoundingBoxComponent() == null) {
            addComponent(new BoundingBoxComponent());
        }
    }

    public Rectangle2D getCollisionBox() {
        BoundingBoxComponent box = getBoundingBoxComponent();
        if (box == null) return new Rectangle2D(getX(), getY(), getWidth(), getHeight());
        return new Rectangle2D(box.getMinXWorld(), box.getMinYWorld(), box.getWidth(), box.getHeight());
    }

    public <T> T addAndGetComponent(T component) {
        addComponent((com.almasb.fxgl.entity.component.Component) component);
        return component;
    }

    public void setSize(double w, double h) {
        getBoundingBoxComponent().clearHitBoxes();
        getBoundingBoxComponent().addHitBox(new HitBox(BoundingShape.box(w, h)));
    }

    public Point2D getGamePosition() {
        TransformComponent tc = getTransformComponent();
        return new Point2D(tc.getX(), tc.getY());
    }
}


