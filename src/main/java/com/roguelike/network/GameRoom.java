package com.roguelike.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 游戏房间类 - 表示一个多人游戏房间
 */
public class GameRoom {
    private String id;
    private String name;
    private String hostAddress;
    private String hostName;
    private int maxPlayers;
    private int playerCount;
    private List<NetworkPlayer> players;
    private long lastSeen;
    
    public GameRoom(String name, String hostAddress, int maxPlayers) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.hostAddress = hostAddress;
        this.maxPlayers = maxPlayers;
        this.playerCount = 0;
        this.players = new ArrayList<>();
        this.lastSeen = System.currentTimeMillis();
    }
    
    public void addPlayer(NetworkPlayer player) {
        if (!players.contains(player) && players.size() < maxPlayers) {
            players.add(player);
            updatePlayerCount();
        }
    }
    
    public void removePlayer(NetworkPlayer player) {
        players.remove(player);
        updatePlayerCount();
    }
    
    public void removePlayer(String playerId) {
        players.removeIf(p -> p.getId().equals(playerId));
        updatePlayerCount();
    }
    
    private void updatePlayerCount() {
        this.playerCount = players.size();
    }
    
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
    
    public boolean isEmpty() {
        return players.isEmpty();
    }
    
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
    
    public boolean isExpired(long timeout) {
        return System.currentTimeMillis() - lastSeen > timeout;
    }
    
    // Getter和Setter方法
    public String getId() { return id; }
    public String getName() { return name; }
    public String getHostAddress() { return hostAddress; }
    public String getHostName() { return hostName; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getPlayerCount() { return playerCount; }
    public List<NetworkPlayer> getPlayers() { return new ArrayList<>(players); }
    public long getLastSeen() { return lastSeen; }
    
    public void setName(String name) { this.name = name; }
    public void setHostAddress(String hostAddress) { this.hostAddress = hostAddress; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %d/%d", name, hostName, playerCount, maxPlayers);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GameRoom room = (GameRoom) obj;
        return id.equals(room.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

