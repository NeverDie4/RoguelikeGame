package com.roguelike.entities.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Rectangle2D;

/**
 * 当实体超出当前视口外扩边界时自动销毁。
 * 外扩像素默认 200。
 */
public class OutOfViewportDestroyComponent extends Component {

    private double margin = 200.0;

    public OutOfViewportDestroyComponent() {}

    public OutOfViewportDestroyComponent(double margin) {
        this.margin = margin;
    }

    @Override
    public void onUpdate(double tpf) {
        Rectangle2D view = FXGL.getGameScene().getViewport().getVisibleArea();
        Rectangle2D expanded = new Rectangle2D(
                view.getMinX() - margin,
                view.getMinY() - margin,
                view.getWidth() + margin * 2,
                view.getHeight() + margin * 2
        );

        double x = entity.getX();
        double y = entity.getY();
        double w = entity.getWidth();
        double h = entity.getHeight();

        if (!expanded.intersects(x, y, w, h)) {
            entity.removeFromWorld();
        }
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = Math.max(0.0, margin);
    }
}


