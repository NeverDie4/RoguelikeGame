package com.roguelike.entities.factory;

import com.roguelike.entities.Bullet;
import com.roguelike.entities.bullets.StraightBullet;
import com.roguelike.entities.bullets.CurveBullet;
import com.roguelike.entities.components.OutOfViewportDestroyComponent;
import com.roguelike.entities.configs.BulletSpec;
import javafx.geometry.Point2D;

/**
 * 根据 BulletSpec 生成具体子弹实例，并应用资源/尺寸/生命周期等。
 */
public class BulletFactory {

    public static Bullet create(Bullet.Faction faction, Point2D direction, BulletSpec spec) {
        if (spec == null) return null;

        Bullet bullet;
        switch (spec.getBulletType()) {
            case CURVE:
                bullet = new CurveBullet(
                        faction,
                        direction,
                        spec.getBaseDamage(),
                        spec.isPiercing(),
                        spec.getBaseSpeed(),
                        30.0 // 默认曲线因子（可扩展入 spec）
                );
                break;
            case STRAIGHT:
            default:
                bullet = new StraightBullet(
                        faction,
                        direction,
                        spec.getBaseDamage(),
                        spec.isPiercing(),
                        spec.getBaseSpeed()
                );
                break;
        }

        // 应用尺寸
        if (spec.getRadius() > 0) {
            bullet.setSize(spec.getRadius() * 2, spec.getRadius() * 2);
        } else if (spec.getWidth() > 0 && spec.getHeight() > 0) {
            bullet.setSize(spec.getWidth(), spec.getHeight());
        }

        // 应用动画（若有资源）
        if (bullet instanceof StraightBullet) {
            StraightBullet sb = (StraightBullet) bullet;
            if (spec.getAnimationBasePath() != null && spec.getFrameCount() > 0) {
                sb.enableAnimation();
                if (sb.getAnimationComponent() != null) {
                    // 先设置动画参数，再加载帧，避免重建后不再播放/只播放一轮
                    sb.setAnimationLooping(true);
                    sb.setAnimationFrameDuration(spec.getFrameDuration());
                    sb.getAnimationComponent().loadAnimationFrames(spec.getAnimationBasePath(), spec.getFrameCount());
                    // 应用视觉缩放
                    sb.getAnimationComponent().setVisualScale(spec.getVisualScale());
                }
            }
        } else if (bullet instanceof CurveBullet) {
            CurveBullet cb = (CurveBullet) bullet;
            if (spec.getAnimationBasePath() != null && spec.getFrameCount() > 0) {
                cb.enableAnimation();
                if (cb.getAnimationComponent() != null) {
                    // 先设置动画参数，再加载帧，避免重建后不再播放/只播放一轮
                    cb.setAnimationLooping(true);
                    cb.setAnimationFrameDuration(spec.getFrameDuration());
                    cb.getAnimationComponent().loadAnimationFrames(spec.getAnimationBasePath(), spec.getFrameCount());
                    // 应用视觉缩放
                    cb.getAnimationComponent().setVisualScale(spec.getVisualScale());
                }
            }
        }

        // 应用寿命或越界销毁（统一在工厂层添加，避免重复添加）
        if (spec.getLifetimeSeconds() > 0) {
            bullet.applyLifetime(spec.getLifetimeSeconds());
        } else {
            // 仅当实体当前没有该组件时再添加；扩大边界以延长存活
            if (bullet.getComponentOptional(OutOfViewportDestroyComponent.class).isEmpty()) {
                bullet.addComponent(new OutOfViewportDestroyComponent(600));
            }
        }

        return bullet;
    }
}


