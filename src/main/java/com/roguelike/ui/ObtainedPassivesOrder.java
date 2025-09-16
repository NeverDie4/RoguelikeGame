package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.core.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 记录被动获得顺序，用于 WeaponPassivePanel 第二行展示。
 * 若监听器注册前已有已获得被动，则在首次读取时从 PassiveItemManager 补齐。
 */
public final class ObtainedPassivesOrder {
    public static final ObtainedPassivesOrder INSTANCE = new ObtainedPassivesOrder();
    private final List<PassiveId> order = new ArrayList<>();
    private boolean seededFromManager = false;

    private ObtainedPassivesOrder() {
        GameEvent.listen(GameEvent.Type.PASSIVE_ACQUIRED, e -> {
            Object data = e.getData();
            if (data instanceof PassiveId id) {
                if (!order.contains(id)) order.add(id);
            }
        });
    }

    private void seedFromManagerIfNeeded() {
        if (seededFromManager) return;
        try {
            Object pmObj = FXGL.geto("passiveManager");
            if (pmObj instanceof PassiveItemManager pm) {
                for (PassiveId id : PassiveId.values()) {
                    if (pm.getLevel(id) > 0 && !order.contains(id)) {
                        order.add(id);
                    }
                }
            }
        } catch (Throwable ignored) {}
        seededFromManager = true;
    }

    public List<PassiveId> getOrder() {
        seedFromManagerIfNeeded();
        return Collections.unmodifiableList(order);
    }
    
    /**
     * 重置被动物品获得顺序，用于新游戏开始
     */
    public void reset() {
        order.clear();
        seededFromManager = false; // 重置种子状态
    }
}


