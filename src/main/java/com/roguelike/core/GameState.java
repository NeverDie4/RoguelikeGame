package com.roguelike.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 游戏全局状态。
 */
public class GameState {
    private int score = 0;
    private int playerHP = 100;
    private int playerMaxHP = 100;
    private int level = 1;
    private final List<String> collectedItems = new ArrayList<>();

    public int getPlayerHP() {
        return playerHP;
    }

    public int getPlayerMaxHP() {
        return playerMaxHP;
    }

    public void setPlayerHP(int hp) {
        if (hp < 0) hp = 0;
        if (hp > playerMaxHP) hp = playerMaxHP;
        this.playerHP = hp;
    }

    public void damagePlayer(int damage) {
        if (damage < 0) return;
        setPlayerHP(playerHP - damage);
    }

    public void healPlayer(int value) {
        if (value < 0) return;
        setPlayerHP(playerHP + value);
    }

    public void addScore(int delta) {
        if (delta <= 0) return;
        this.score += delta;
        GameEvent.post(new GameEvent(GameEvent.Type.SCORE_CHANGED));
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public void nextLevel() {
        level++;
    }

    public void addItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return;
        collectedItems.add(itemId);
    }

    public List<String> getCollectedItems() {
        return Collections.unmodifiableList(collectedItems);
    }
}


