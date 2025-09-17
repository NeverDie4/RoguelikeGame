package com.roguelike.network;

import java.util.UUID;

/**
 * 网络玩家类 - 表示一个网络连接的玩家
 */
public class NetworkPlayer {
    private String id;
    private String name;
    private String address;
    private int port; // 对端接收端口
    private boolean isLocal;
    private boolean isHost;
    private long lastSeen;
    private long joinTime;
    
    public NetworkPlayer(String name, String address, boolean isLocal) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.address = address;
        this.port = 0;
        this.isLocal = isLocal;
        this.isHost = false;
        this.lastSeen = System.currentTimeMillis();
        this.joinTime = System.currentTimeMillis();
    }
    
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
    
    public boolean isExpired(long timeout) {
        return System.currentTimeMillis() - lastSeen > timeout;
    }
    
    public long getOnlineTime() {
        return System.currentTimeMillis() - joinTime;
    }
    
    // Getter和Setter方法
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public int getPort() { return port; }
    public boolean isLocal() { return isLocal; }
    public boolean isHost() { return isHost; }
    public long getLastSeen() { return lastSeen; }
    public long getJoinTime() { return joinTime; }
    
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setPort(int port) { this.port = port; }
    public void setLocal(boolean isLocal) { this.isLocal = isLocal; }
    public void setHost(boolean isHost) { this.isHost = isHost; }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", name, isLocal ? "本地" : "远程");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NetworkPlayer player = (NetworkPlayer) obj;
        return id.equals(player.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

