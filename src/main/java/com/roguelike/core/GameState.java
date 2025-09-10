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
    private int currentExp = 0;
    private int maxExp = 100; // 第一级需要100经验
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
        // 每级最大经验值略微增加（参考吸血鬼幸存者）
        maxExp = (int) (100 * Math.pow(1.1, level - 1));
        currentExp = 0;
        // 升级时恢复满血
        playerHP = playerMaxHP;
        GameEvent.post(new GameEvent(GameEvent.Type.LEVEL_UP));
    }

    // 经验值相关方法
    public int getCurrentExp() {
        return currentExp;
    }

    public int getMaxExp() {
        return maxExp;
    }

    public void addExp(int exp) {
        if (exp <= 0) return;
        currentExp += exp;
        GameEvent.post(new GameEvent(GameEvent.Type.EXP_CHANGED));

        // 检查是否升级
        while (currentExp >= maxExp) {
            nextLevel();
        }
    }

    public void setPlayerMaxHP(int maxHP) {
        if (maxHP <= 0) return;
        this.playerMaxHP = maxHP;
        // 如果当前血量超过新的最大血量，调整当前血量
        if (playerHP > maxHP) {
            playerHP = maxHP;
        }
        GameEvent.post(new GameEvent(GameEvent.Type.PLAYER_HP_CHANGED));
    }

    public void addItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return;
        collectedItems.add(itemId);
    }

    public List<String> getCollectedItems() {
        return Collections.unmodifiableList(collectedItems);
    }
}


