package com.roguelike.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 玩家状态类 - 用于网络同步玩家状态
 */
public class PlayerState {
    private String playerId;
    private String playerName;
    private double x;
    private double y;
    private int hp;
    private int maxHp;
    private int level;
    private int score;
    private long timestamp;
    
    public PlayerState() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public PlayerState(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 序列化玩家状态为字节数组
     */
    public byte[] serialize() {
        try {
            // 计算所需缓冲区大小
            int nameBytes = playerName.getBytes(StandardCharsets.UTF_8).length;
            int idBytes = playerId.getBytes(StandardCharsets.UTF_8).length;
            int totalSize = 4 + idBytes + 4 + nameBytes + 8 + 8 + 4 + 4 + 4 + 4 + 8; // 所有字段的大小
            
            ByteBuffer buffer = ByteBuffer.allocate(totalSize);
            
            // 写入字符串长度和字符串
            buffer.putInt(idBytes);
            buffer.put(playerId.getBytes(StandardCharsets.UTF_8));
            buffer.putInt(nameBytes);
            buffer.put(playerName.getBytes(StandardCharsets.UTF_8));
            
            // 写入数值
            buffer.putDouble(x);
            buffer.putDouble(y);
            buffer.putInt(hp);
            buffer.putInt(maxHp);
            buffer.putInt(level);
            buffer.putInt(score);
            buffer.putLong(timestamp);
            
            return buffer.array();
        } catch (Exception e) {
            System.err.println("序列化玩家状态失败: " + e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * 从字节数组反序列化玩家状态
     */
    public static PlayerState deserialize(byte[] data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            PlayerState state = new PlayerState();
            
            // 读取字符串
            int idLength = buffer.getInt();
            byte[] idBytes = new byte[idLength];
            buffer.get(idBytes);
            state.playerId = new String(idBytes, StandardCharsets.UTF_8);
            
            int nameLength = buffer.getInt();
            byte[] nameBytes = new byte[nameLength];
            buffer.get(nameBytes);
            state.playerName = new String(nameBytes, StandardCharsets.UTF_8);
            
            // 读取数值
            state.x = buffer.getDouble();
            state.y = buffer.getDouble();
            state.hp = buffer.getInt();
            state.maxHp = buffer.getInt();
            state.level = buffer.getInt();
            state.score = buffer.getInt();
            state.timestamp = buffer.getLong();
            
            return state;
        } catch (Exception e) {
            System.err.println("反序列化玩家状态失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查状态是否过期
     */
    public boolean isExpired(long timeout) {
        return System.currentTimeMillis() - timestamp > timeout;
    }
    
    /**
     * 计算与另一个状态的距离
     */
    public double distanceTo(PlayerState other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    // Getter和Setter方法
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getLevel() { return level; }
    public int getScore() { return score; }
    public long getTimestamp() { return timestamp; }
    
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setHp(int hp) { this.hp = hp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public void setLevel(int level) { this.level = level; }
    public void setScore(int score) { this.score = score; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return String.format("PlayerState{id='%s', name='%s', pos=(%.1f,%.1f), hp=%d/%d, level=%d, score=%d}", 
                playerId, playerName, x, y, hp, maxHp, level, score);
    }
}

