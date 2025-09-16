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
    private int experience = 0;
    private int experienceToNextLevel = 100;
    private int coins = 0;
    private long gameStartTime = System.currentTimeMillis();
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
        experienceToNextLevel = (int) (100 * Math.pow(1.1, level - 1));
        experience = 0;
        // 升级时恢复满血
        playerHP = playerMaxHP;
        GameEvent.post(new GameEvent(GameEvent.Type.LEVEL_UP));
        try { com.roguelike.ui.SoundService.playOnce("levelups/levelup.wav", 3.0); } catch (Throwable ignored) {} // 调高升级音效音量
    }

    // 经验值相关方法
    public int getCurrentExp() {
        return experience;
    }

    public int getMaxExp() {
        return experienceToNextLevel;
    }

    public void addExp(int exp) {
        if (exp <= 0) return;
        experience += exp;
        GameEvent.post(new GameEvent(GameEvent.Type.EXPERIENCE_CHANGED));

        // 检查是否升级
        while (experience >= experienceToNextLevel) {
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

    // 金币相关方法
    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        if (amount <= 0) return;
        this.coins += amount;
        GameEvent.post(new GameEvent(GameEvent.Type.COINS_CHANGED));
    }

    public boolean spendCoins(int amount) {
        if (amount <= 0 || coins < amount) return false;
        this.coins -= amount;
        GameEvent.post(new GameEvent(GameEvent.Type.COINS_CHANGED));
        return true;
    }

    // 时间相关方法
    public long getGameStartTime() {
        return gameStartTime;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - gameStartTime;
    }

    public String getFormattedTime() {
        long elapsed = getElapsedTime();
        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void resetGameTime() {
        this.gameStartTime = System.currentTimeMillis();
        GameEvent.post(new GameEvent(GameEvent.Type.TIME_CHANGED));
    }

    // 经验值和等级相关方法
    public int getExperience() {
        return experience;
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    public int getCurrentLevel() {
        return level;
    }

    public double getExperienceProgress() {
        return experienceToNextLevel <= 0 ? 1.0 : (double) experience / (double) experienceToNextLevel;
    }

    public void addExperience(int amount) {
        if (amount <= 0) return;

        this.experience += amount;

        // 检查是否升级
        while (experience >= experienceToNextLevel) {
            levelUp();
        }

        GameEvent.post(new GameEvent(GameEvent.Type.EXPERIENCE_CHANGED));
    }

    private void levelUp() {
        experience -= experienceToNextLevel;
        level++;

        // 每升一级，下一级所需经验增加20%
        experienceToNextLevel = (int) (experienceToNextLevel * 1.2);

        // 升级时恢复部分血量
        int healAmount = playerMaxHP / 4; // 恢复25%血量
        healPlayer(healAmount);

        GameEvent.post(new GameEvent(GameEvent.Type.LEVEL_UP));
        try { com.roguelike.ui.SoundService.playOnce("levelups/levelup.wav", 3.0); } catch (Throwable ignored) {} // 调高升级音效音量
    }

    public void setLevel(int newLevel) {
        if (newLevel < 1) newLevel = 1;
        this.level = newLevel;
        this.experience = 0;
        this.experienceToNextLevel = 100;

        // 重新计算升级所需经验
        for (int i = 1; i < newLevel; i++) {
            experienceToNextLevel = (int) (experienceToNextLevel * 1.2);
        }

        GameEvent.post(new GameEvent(GameEvent.Type.EXPERIENCE_CHANGED));
    }
}


