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
                    // 06 子弹仅播放一遍，其它循环
                    if ("straight_06".equals(spec.getId())) {
                        sb.setAnimationLooping(false);
                        sb.getAnimationComponent().setRemoveOnFinish(true);
                    } else {
                        sb.setAnimationLooping(true);
                    }
                    sb.setAnimationFrameDuration(spec.getFrameDuration());
                    sb.getAnimationComponent().loadAnimationFrames(spec.getAnimationBasePath(), spec.getFrameCount());
                    // 应用视觉缩放
                    sb.getAnimationComponent().setVisualScale(spec.getVisualScale());
                    // 针对 01/05/06：发射时按方向旋转动画
                    if (("straight_06".equals(spec.getId()) || "straight_01".equals(spec.getId()) || "straight_05".equals(spec.getId())) && direction != null) {
                        javafx.geometry.Point2D dir = direction.normalize();
                        double deg = Math.toDegrees(Math.atan2(dir.getY(), dir.getX()));
                        // 右=0，上=90，左=180，下=270
                        double base = 360.0 - deg;

                        // 斜向精确旋转需求：
                        // 左上 -> 顺时针 90°；左下 -> 逆时针 90°；右下 -> 顺时针 90°；右上 -> 逆时针 90°。
                        // 依据象限对 base 进行±90°校正，同时保证左右/上下完全在直线上。
                        double visualDeg = base;
                        boolean isHorizontal = Math.abs(dir.getY()) < 1e-6;
                        boolean isVertical = Math.abs(dir.getX()) < 1e-6;
                        if (!isHorizontal && !isVertical) {
                            if (dir.getX() < 0 && dir.getY() < 0) { // 左上
                                visualDeg = base + 90.0; // 顺时针90
                            } else if (dir.getX() < 0 && dir.getY() > 0) { // 左下
                                visualDeg = base - 90.0; // 逆时针90
                            } else if (dir.getX() > 0 && dir.getY() > 0) { // 右下
                                visualDeg = base + 90.0; // 顺时针90
                            } else if (dir.getX() > 0 && dir.getY() < 0) { // 右上
                                visualDeg = base - 90.0; // 逆时针90
                            }
                        } else if (isVertical) {
                            // 上/下方向额外加 180° 以修正颠倒
                            visualDeg = (base + 180.0);
                        }

                        while (visualDeg < 0) visualDeg += 360.0;
                        while (visualDeg >= 360.0) visualDeg -= 360.0;
                        sb.getAnimationComponent().setVisualRotationDegrees(visualDeg);
                    }
                }
            }
            // 03：销毁时爆炸（bullets/09），AOE=当前伤害，半径与爆炸缩放取自 WeaponManager
            if ("straight_03".equals(spec.getId())) {
                int dmg = spec.getBaseDamage();
                double radius = com.roguelike.entities.weapons.WeaponManager.getWeapon03ExplosionRadius();
                double scale = com.roguelike.entities.weapons.WeaponManager.getWeapon03ExplosionScale();
                sb.addComponent(new com.roguelike.entities.components.ExplosionOnDestroyComponent(
                        "assets/textures/bullets/09", 10, 0.06,
                        scale, radius, dmg
                ));
            }
            // 07：与03相同，但爆炸资源改为 bullets/10
            if ("straight_07".equals(spec.getId())) {
                int dmg = spec.getBaseDamage();
                double radius = com.roguelike.entities.weapons.WeaponManager.getWeapon07ExplosionRadius();
                double scale = com.roguelike.entities.weapons.WeaponManager.getWeapon07ExplosionScale();
                sb.addComponent(new com.roguelike.entities.components.ExplosionOnDestroyComponent(
                        "assets/textures/bullets/10", 10, 0.09,
                        scale, radius, dmg
                ));
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

        // 临时：将 straight_03 的寿命设为 1s，以便可见爆炸动画
        if ("straight_03".equals(spec.getId())) {
            bullet.applyLifetime(1.0);
        }
        // 临时：将 straight_07 的寿命设为 1s，以便可见爆炸动画
        if ("straight_07".equals(spec.getId())) {
            bullet.applyLifetime(1.0);
        }

        return bullet;
    }
}


