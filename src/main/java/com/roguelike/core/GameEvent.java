package com.roguelike.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 简化的事件封装：本地发布/订阅。
 */
public class GameEvent {

    public enum Type {
        PLAYER_MOVE,
        PLAYER_HURT,
        PLAYER_DEATH,
        PLAYER_HP_CHANGED,
        ENEMY_DEATH,
        ENEMY_HP_CHANGED,
        SCORE_CHANGED,
        MAP_LOADED,
        COINS_CHANGED,
        TIME_CHANGED,
        EXPERIENCE_CHANGED,
        LEVEL_UP,
        // 碰撞相关事件
        PLAYER_HIT_WALL,
        ENEMY_HIT_WALL,
        ENTITY_COLLISION_BLOCKED,
        MOVEMENT_SLIDING
    }

    private static final Map<Type, List<Consumer<GameEvent>>> LISTENERS = new EnumMap<>(Type.class);

    static {
        for (Type t : Type.values()) {
            LISTENERS.put(t, new ArrayList<>());
        }
    }

    private final Type type;

    public GameEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static void post(GameEvent event) {
        List<Consumer<GameEvent>> handlers = LISTENERS.get(event.getType());
        if (handlers == null) return;
        for (Consumer<GameEvent> h : List.copyOf(handlers)) {
            h.accept(event);
        }
    }

    public static void listen(Type type, Consumer<GameEvent> handler) {
        LISTENERS.get(type).add(handler);
    }
}


