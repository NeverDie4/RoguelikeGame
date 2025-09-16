package com.roguelike.ui;

import com.roguelike.core.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 记录武器获得的顺序（用于左上角面板展示顺位）。
 */
public final class ObtainedWeaponsOrder {
    public static final ObtainedWeaponsOrder INSTANCE = new ObtainedWeaponsOrder();
    private final List<String> order = new ArrayList<>(); // 元素为 "01".."08"

    private ObtainedWeaponsOrder() {
        GameEvent.listen(GameEvent.Type.WEAPON_UPGRADED, e -> {
            Object data = e.getData();
            if (data instanceof String idx && idx.matches("\\d{2}")) {
                if (!order.contains(idx)) order.add(idx);
            }
        });
        // 初始保证 01 在列表首位（玩家初始拥有）
        if (!order.contains("01")) order.add("01");
    }

    public List<String> getOrder() {
        return Collections.unmodifiableList(order);
    }
    
    /**
     * 重置武器获得顺序，用于新游戏开始
     */
    public void reset() {
        order.clear();
        // 重新添加初始武器01
        if (!order.contains("01")) order.add("01");
    }
}


