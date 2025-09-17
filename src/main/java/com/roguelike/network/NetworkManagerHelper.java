package com.roguelike.network;

import com.roguelike.core.GameState;
import com.roguelike.entities.Player;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.application.Platform;

import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NetworkManager的辅助类 - 包含网络通信的具体实现方法
 */
public class NetworkManagerHelper {
    
    private static final int DISCOVERY_PORT = 8888;
    private static final int GAME_PORT = 8889;
    private static final String DISCOVERY_MESSAGE = "ROGUELIKE_DISCOVERY";
    private static final String DISCOVERY_RESPONSE = "ROGUELIKE_RESPONSE";
    private static final int MAX_PLAYERS = 4;
    
    /**
     * 发现监听线程
     */
    public static void discoveryListener(DatagramSocket discoverySocket, 
                                       NetworkManager networkManager,
                                       Map<String, GameRoom> discoveredRooms,
                                       boolean isShuttingDown) {
        byte[] buffer = new byte[1024];
        System.out.println("发现监听线程已启动，监听端口: " + discoverySocket.getLocalPort());
        
        while (discoverySocket != null && !discoverySocket.isClosed() && !isShuttingDown) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                discoverySocket.receive(packet);
                
                // 检查是否正在关闭
                if (isShuttingDown) {
                    System.out.println("发现监听线程收到关闭信号，停止监听");
                    break;
                }
                
                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();
                
                System.out.println("收到消息来自 " + senderAddress + ": " + message);
                
                if (message.startsWith(DISCOVERY_MESSAGE)) {
                    // 收到发现请求，发送响应
                    if (networkManager.isHost() && networkManager.getCurrentRoom() != null && !isShuttingDown) {
                        // 主机响应发现请求
                        sendDiscoveryResponse(discoverySocket, packet.getAddress(), packet.getPort(), networkManager);
                        System.out.println("收到发现请求，发送房间信息: " + senderAddress);
                    } else {
                        System.out.println("收到发现请求，但当前不是主机: " + senderAddress);
                    }
                } else if (message.startsWith(DISCOVERY_RESPONSE)) {
                    // 收到发现响应，解析房间信息
                    System.out.println("收到发现响应: " + message);
                    parseDiscoveryResponse(message, senderAddress, discoveredRooms, networkManager);
                } else {
                    System.out.println("收到未知消息: " + message);
                }
                
            } catch (Exception e) {
                if (discoverySocket != null && !discoverySocket.isClosed() && !isShuttingDown) {
                    System.err.println("发现监听错误: " + e.getMessage());
                }
            }
        }
        System.out.println("发现监听线程已停止");
    }
    
    /**
     * 广播发现消息
     */
    public static void broadcastDiscovery(DatagramSocket discoverySocket,
                                        NetworkManager networkManager,
                                        boolean isShuttingDown) {
        if (discoverySocket == null || discoverySocket.isClosed() || isShuttingDown) {
            System.out.println("发现Socket未初始化或已关闭，跳过广播");
            return;
        }
        
        try {
            String message;
            // 如果是主机，直接广播房间信息
            if (networkManager.isHost() && networkManager.getCurrentRoom() != null) {
                message = DISCOVERY_RESPONSE + ":" + networkManager.getRoomName() + ":" + 
                         networkManager.getPlayerName() + ":" + networkManager.getConnectedPlayers().size() + "/" + MAX_PLAYERS;
                System.out.println("主机广播房间信息: " + networkManager.getRoomName() + 
                                 " (" + networkManager.getConnectedPlayers().size() + "/" + MAX_PLAYERS + ")");
            } else {
                // 如果不是主机，发送发现请求
                message = DISCOVERY_MESSAGE + ":" + networkManager.getPlayerName();
                System.out.println("发送发现请求: " + networkManager.getPlayerName());
            }
            
            byte[] data = message.getBytes();
            
            // 1. 向全局广播地址发送
            try {
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, DISCOVERY_PORT);
                discoverySocket.send(packet);
                System.out.println("向全局广播地址发送: " + broadcastAddress.getHostAddress() + ":" + DISCOVERY_PORT);
            } catch (Exception e) {
                System.err.println("向全局广播地址发送失败: " + e.getMessage());
            }
            
            // 2. 向所有网络接口广播，提高发现成功率
            broadcastToAllInterfaces(discoverySocket, message);
            
        } catch (Exception e) {
            System.err.println("广播发现消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 向所有网络接口广播消息
     */
    private static void broadcastToAllInterfaces(DatagramSocket discoverySocket, String message) {
        try {
            System.out.println("开始向所有网络接口广播...");
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            int interfaceCount = 0;
            int broadcastCount = 0;
            
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                interfaceCount++;
                
                if (ni.isUp() && !ni.isLoopback()) {
                    System.out.println("检查接口: " + ni.getName() + " (" + ni.getDisplayName() + ")");
                    
                    try {
                        java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            java.net.InetAddress addr = addresses.nextElement();
                            if (addr instanceof java.net.Inet4Address) {
                                System.out.println("  发现IPv4地址: " + addr.getHostAddress());
                                
                                // 计算子网广播地址
                                String broadcastAddr = calculateBroadcastAddress(addr, ni);
                                if (broadcastAddr != null) {
                                    try {
                                        java.net.InetAddress broadcast = java.net.InetAddress.getByName(broadcastAddr);
                                        byte[] data = message.getBytes();
                                        DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, DISCOVERY_PORT);
                                        discoverySocket.send(packet);
                                        System.out.println("  ✅ 向接口 " + ni.getName() + " 广播到: " + broadcastAddr);
                                        broadcastCount++;
                                    } catch (Exception e) {
                                        System.err.println("  ❌ 向接口 " + ni.getName() + " 广播失败: " + e.getMessage());
                                    }
                                } else {
                                    System.out.println("  ⚠️ 无法计算广播地址: " + addr.getHostAddress());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("  ❌ 处理接口 " + ni.getName() + " 失败: " + e.getMessage());
                    }
                } else {
                    System.out.println("跳过接口: " + ni.getName() + " (未启用或回环接口)");
                }
            }
            
            System.out.println("网络接口广播完成 - 总接口: " + interfaceCount + ", 成功广播: " + broadcastCount);
            
        } catch (Exception e) {
            System.err.println("向所有接口广播失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 计算广播地址
     */
    private static String calculateBroadcastAddress(java.net.InetAddress addr, java.net.NetworkInterface ni) {
        try {
            String ip = addr.getHostAddress();
            System.out.println("    计算广播地址: " + ip);
            
            // 对于常见的私有网络，直接使用.255作为广播地址
            if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                String broadcast = ip.substring(0, ip.lastIndexOf('.')) + ".255";
                System.out.println("    私有网络广播地址: " + broadcast);
                return broadcast;
            } else if (ip.startsWith("172.")) {
                String broadcast = ip.substring(0, ip.lastIndexOf('.')) + ".255";
                System.out.println("    私有网络广播地址: " + broadcast);
                return broadcast;
            } else if (ip.startsWith("169.254.")) {
                // 链路本地地址
                String broadcast = ip.substring(0, ip.lastIndexOf('.')) + ".255";
                System.out.println("    链路本地广播地址: " + broadcast);
                return broadcast;
            } else {
                // 尝试使用网络接口的子网掩码计算
                try {
                    java.util.List<java.net.InterfaceAddress> interfaceAddresses = ni.getInterfaceAddresses();
                    for (java.net.InterfaceAddress ia : interfaceAddresses) {
                        if (ia.getAddress().equals(addr)) {
                            java.net.InetAddress broadcast = ia.getBroadcast();
                            if (broadcast != null) {
                                System.out.println("    接口广播地址: " + broadcast.getHostAddress());
                                return broadcast.getHostAddress();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("    无法获取接口广播地址: " + e.getMessage());
                }
                
                // 如果无法计算，尝试使用.255
                String broadcast = ip.substring(0, ip.lastIndexOf('.')) + ".255";
                System.out.println("    默认广播地址: " + broadcast);
                return broadcast;
            }
        } catch (Exception e) {
            System.err.println("计算广播地址失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 发送发现响应
     */
    private static void sendDiscoveryResponse(DatagramSocket discoverySocket, InetAddress address, int remotePort, NetworkManager networkManager) {
        try {
            String response = DISCOVERY_RESPONSE + ":" + networkManager.getRoomName() + ":" + 
                            networkManager.getPlayerName() + ":" + networkManager.getConnectedPlayers().size() + "/" + MAX_PLAYERS;
            byte[] data = response.getBytes();
            // 将响应发回请求包的来源端口，避免对方使用临时端口时收不到
            DatagramPacket packet = new DatagramPacket(data, data.length, address, remotePort);
            discoverySocket.send(packet);
        } catch (Exception e) {
            System.err.println("发送发现响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析发现响应
     */
    private static void parseDiscoveryResponse(String message, String address, 
                                            Map<String, GameRoom> discoveredRooms, 
                                            NetworkManager networkManager) {
        try {
            String[] parts = message.split(":");
            if (parts.length >= 4) {
                String roomName = parts[1];
                String hostName = parts[2];
                String playerCountStr = parts[3];
                
                // 检查是否是房主自己的房间，如果是则忽略
                if (networkManager.isHost() && networkManager.getCurrentRoom() != null && 
                    roomName.equals(networkManager.getRoomName()) && hostName.equals(networkManager.getPlayerName())) {
                    System.out.println("忽略自己的房间广播: " + roomName);
                    return;
                }
                
                GameRoom room = new GameRoom(roomName, address, MAX_PLAYERS);
                room.setHostName(hostName);
                try {
                    // 解析玩家数量，格式可能是 "1" 或 "1/4"
                    int playerCount;
                    if (playerCountStr.contains("/")) {
                        playerCount = Integer.parseInt(playerCountStr.split("/")[0]);
                    } else {
                        playerCount = Integer.parseInt(playerCountStr);
                    }
                    room.setPlayerCount(playerCount);
                } catch (NumberFormatException e) {
                    room.setPlayerCount(0);
                }
                
                discoveredRooms.put(address, room);
                
                System.out.println("发现房间: " + roomName + " (" + hostName + ") - " + playerCountStr);
                
                if (networkManager.getEventListener() != null) {
                    Platform.runLater(() -> networkManager.getEventListener().onRoomDiscovered(room));
                }
            } else {
                System.err.println("发现响应格式错误: " + message);
            }
        } catch (Exception e) {
            System.err.println("解析发现响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 游戏通信监听线程
     */
    public static void gameListener(DatagramSocket gameSocket, 
                                  NetworkManager networkManager,
                                  boolean isShuttingDown) {
        byte[] buffer = new byte[1024];
        System.out.println("游戏通信监听线程已启动");
        
        while (gameSocket != null && !gameSocket.isClosed() && !isShuttingDown) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                gameSocket.receive(packet);
                
                // 检查是否正在关闭
                if (isShuttingDown) {
                    System.out.println("游戏监听线程收到关闭信号，停止监听");
                    break;
                }
                
                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();
                
                // 解析消息类型
                if (message.startsWith("JOIN:")) {
                    handleJoinRequest(message, senderAddress, networkManager, gameSocket);
                } else if (message.startsWith("JOIN_RESPONSE:")) {
                    handleJoinResponse(message, senderAddress, networkManager);
                } else if (message.startsWith("LEAVE:")) {
                    handleLeaveMessage(message, senderAddress, networkManager);
                } else if (message.startsWith("HEARTBEAT:")) {
                    handleHeartbeat(message, senderAddress, networkManager);
                } else if (message.startsWith("STATE:")) {
                    handleGameState(message, senderAddress, networkManager);
                } else if (message.startsWith("GAME_START:")) {
                    handleGameStart(message, senderAddress, networkManager);
                } else if (message.startsWith("REQUEST_INITIAL_STATE:")) {
                    handleInitialStateRequest(message, senderAddress, networkManager, gameSocket);
                } else if (message.startsWith("INITIAL_STATE:")) {
                    handleInitialState(message, senderAddress, networkManager);
                } else if (message.startsWith("AUTHORITATIVE_SPAWN:")) {
                    handleAuthoritativeSpawn(message, networkManager);
                } else if (message.startsWith("SPAWN_ENEMY:")) {
                    handleSpawnEnemy(message);
                }
                
            } catch (Exception e) {
                if (gameSocket != null && !gameSocket.isClosed() && !isShuttingDown) {
                    System.err.println("游戏通信监听错误: " + e.getMessage());
                }
            }
        }
        System.out.println("游戏通信监听线程已停止");
    }

    private static long parseStartAt(String startAtStr) {
        try {
            return Long.parseLong(startAtStr);
        } catch (Exception e) {
            return 0L;
        }
    }
    
    // 处理各种消息的方法
    private static void handleJoinRequest(String message, String senderAddress, 
                                        NetworkManager networkManager, DatagramSocket gameSocket) {
        if (!networkManager.isHost()) {
            System.out.println("收到加入请求，但当前不是房主: " + senderAddress);
            return;
        }
        
        try {
            System.out.println("房主收到加入请求: " + message + " 来自: " + senderAddress);
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                String playerName = parts[1];
                String clientPort = parts.length >= 3 ? parts[2] : String.valueOf(GAME_PORT);
                
                System.out.println("解析玩家名称: " + playerName);
                System.out.println("解析客户端端口: " + clientPort);
                System.out.println("当前房间玩家数量: " + networkManager.getConnectedPlayers().size() + "/" + MAX_PLAYERS);
                
                // 检查房间是否已满
                if (networkManager.getConnectedPlayers().size() >= MAX_PLAYERS) {
                    System.out.println("房间已满，拒绝玩家加入: " + playerName);
                    sendJoinResponse(senderAddress, Integer.parseInt(clientPort), false, "房间已满", gameSocket);
                    return;
                }
                
                // 创建新玩家
                NetworkPlayer newPlayer = new NetworkPlayer(playerName, senderAddress, false);
                try { newPlayer.setPort(Integer.parseInt(clientPort)); } catch (Exception ignored) {}
                networkManager.getConnectedPlayers().put(newPlayer.getId(), newPlayer);
                networkManager.getCurrentRoom().addPlayer(newPlayer);
                
                System.out.println("新玩家已添加到房间: " + newPlayer.getName() + " (ID: " + newPlayer.getId() + ")");
                System.out.println("当前连接的玩家列表: " + networkManager.getConnectedPlayers().keySet());
                
                // 发送加入响应到客户端的实际端口
                sendJoinResponse(senderAddress, Integer.parseInt(clientPort), true, "加入成功", gameSocket);
                System.out.println("已发送加入成功响应给: " + senderAddress + ":" + clientPort);
                
                // 通知其他玩家
                broadcastPlayerJoined(newPlayer, networkManager, gameSocket);
                
                if (networkManager.getEventListener() != null) {
                    Platform.runLater(() -> networkManager.getEventListener().onPlayerJoined(newPlayer));
                }
                
                System.out.println("玩家加入完成: " + playerName + " (" + senderAddress + ":" + clientPort + ")");
            } else {
                System.err.println("加入请求格式错误: " + message);
            }
        } catch (Exception e) {
            System.err.println("处理加入请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void handleJoinResponse(String message, String senderAddress, NetworkManager networkManager) {
        if (!networkManager.isClient()) {
            return; // 只有客户端需要处理加入响应
        }
        
        try {
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String status = parts[1];
                String responseMessage = parts[2];
                
                if ("SUCCESS".equals(status)) {
                    System.out.println("成功加入房间: " + responseMessage);
                    
                    // 关键修复：客户端需要将房主添加到connectedPlayers中
                    if (networkManager.getCurrentRoom() != null) {
                        // 使用房主的实际信息创建NetworkPlayer
                        String hostName = networkManager.getCurrentRoom().getHostName() != null ? 
                                        networkManager.getCurrentRoom().getHostName() : "Host";
                        NetworkPlayer hostPlayer = new NetworkPlayer(hostName, networkManager.getCurrentRoom().getHostAddress(), false);
                        hostPlayer.setPort(GAME_PORT);
                        hostPlayer.setHost(true);
                        networkManager.getConnectedPlayers().put(hostPlayer.getId(), hostPlayer);
                        System.out.println("客户端已添加房主到连接列表: " + hostPlayer.getName() + " (" + hostPlayer.getAddress() + ")");
                        System.out.println("客户端当前连接的玩家数量: " + networkManager.getConnectedPlayers().size());
                    }
                    
                    // 客户端已成功加入房间，可以开始游戏
                } else {
                    System.err.println("加入房间失败: " + responseMessage);
                    // 加入失败，可能需要重新尝试或显示错误信息
                }
            }
        } catch (Exception e) {
            System.err.println("处理加入响应失败: " + e.getMessage());
        }
    }
    
    private static void handleLeaveMessage(String message, String senderAddress, NetworkManager networkManager) {
        try {
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                String playerId = parts[1];
                
                NetworkPlayer player = networkManager.getConnectedPlayers().remove(playerId);
                if (player != null) {
                    networkManager.getCurrentRoom().removePlayer(player);
                    
                    // 通知其他玩家
                    broadcastPlayerLeft(player, networkManager, null);
                    
                    if (networkManager.getEventListener() != null) {
                        Platform.runLater(() -> networkManager.getEventListener().onPlayerLeft(playerId));
                    }
                    
                    System.out.println("玩家离开: " + player.getName() + " (" + senderAddress + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("处理离开消息失败: " + e.getMessage());
        }
    }
    
    private static void handleHeartbeat(String message, String senderAddress, NetworkManager networkManager) {
        // 更新玩家在线状态
        for (NetworkPlayer player : networkManager.getConnectedPlayers().values()) {
            if (player.getAddress().equals(senderAddress)) {
                player.updateLastSeen();
                break;
            }
        }
    }
    
    private static void handleGameState(String message, String senderAddress, NetworkManager networkManager) {
        try {
            // 解析状态数据
            String data = message.substring(6); // 移除"STATE:"前缀
            byte[] stateData = data.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            PlayerState state = PlayerState.deserialize(stateData);
            
            if (state != null) {
                networkManager.getPlayerStates().put(state.getPlayerId(), state);
                System.out.println("收到玩家状态: " + state.getPlayerName() + 
                                 " 位置: (" + state.getX() + ", " + state.getY() + ") 来自: " + senderAddress);
                
                if (networkManager.getEventListener() != null) {
                    Platform.runLater(() -> networkManager.getEventListener().onGameStateReceived(state));
                }
            } else {
                System.err.println("解析玩家状态失败，消息: " + message);
            }
        } catch (Exception e) {
            System.err.println("处理游戏状态失败: " + e.getMessage());
        }
    }
    
    private static void handleGameStart(String message, String senderAddress, NetworkManager networkManager) {
        if (networkManager.isHost()) {
            return; // 房主不需要处理自己的消息
        }
        
        try {
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String mapName = parts[1];
                final String startAtStr = parts[2];
                final long startAt = parseStartAt(startAtStr);
                System.out.println("收到房主开始游戏通知: map=" + mapName + ", startAt=" + startAt + " 来自: " + senderAddress);
                System.out.println("客户端当前状态 - isClient: " + networkManager.isClient() + ", isConnected: " + networkManager.isConnected());
                
                // 客户端开始游戏
                Platform.runLater(() -> {
                    try {
                        System.out.println("客户端开始执行游戏开始逻辑...");
                        
                        // 隐藏网络菜单的等待界面
                        com.roguelike.ui.NetworkMenu.getInstance().hide();
                        System.out.println("网络菜单已隐藏");
                        
                        // 关键顺序：先写入地图与权威时间，再启动新游戏
                        System.out.println("客户端创建新游戏实例...");
                        // 将地图名与权威开始时间写入全局
                        try { FXGL.set("selectedMapName", mapName); } catch (Exception ignored) {}
                        try { FXGL.set("authoritativeStartTime", startAt); } catch (Exception ignored) {}
                        FXGL.getGameController().startNewGame();
                        System.out.println("客户端游戏实例创建完成");
                        
                        // 延迟启动游戏状态同步，确保游戏实例完全创建
                        FXGL.runOnce(() -> {
                            try {
                                // 先请求出生点与初始状态，再开启发送循环
                                try { networkManager.requestInitialGameStateFromHost(); } catch (Throwable ignored) {}
                                System.out.println("已向房主请求初始游戏状态");
                                // 再启动同步接收/发送
                                networkManager.startGameStateSync();
                                System.out.println("客户端开始游戏成功，已启动状态同步");
                            } catch (Exception e) {
                                System.err.println("启动状态同步失败: " + e.getMessage());
                            }
                        }, javafx.util.Duration.seconds(2));
                    } catch (Exception e) {
                        System.err.println("客户端开始游戏失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                System.err.println("游戏开始消息格式错误: " + message);
            }
        } catch (Exception e) {
            System.err.println("处理游戏开始消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void handleInitialStateRequest(String message, String senderAddress, 
                                                NetworkManager networkManager, DatagramSocket gameSocket) {
        if (!networkManager.isHost()) {
            return;
        }
        
        try {
            System.out.println("收到初始状态请求: " + message + " 来自: " + senderAddress);
            sendInitialGameStateToAllClients(networkManager, gameSocket);
        } catch (Exception e) {
            System.err.println("处理初始状态请求失败: " + e.getMessage());
        }
    }
    
    private static void handleInitialState(String message, String senderAddress, NetworkManager networkManager) {
        if (!networkManager.isClient()) {
            return;
        }
        
        try {
            System.out.println("收到房主初始状态: " + message + " 来自: " + senderAddress);
            
            // 解析状态数据
            String data = message.substring(13); // 移除"INITIAL_STATE:"前缀
            byte[] stateData = data.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            PlayerState state = PlayerState.deserialize(stateData);
            
            if (state != null) {
                networkManager.getPlayerStates().put(state.getPlayerId(), state);
                System.out.println("已保存房主初始状态: " + state.getPlayerName() + 
                                 " 位置: (" + state.getX() + ", " + state.getY() + ")");
            }
        } catch (Exception e) {
            System.err.println("处理初始状态失败: " + e.getMessage());
        }
    }

    private static void handleAuthoritativeSpawn(String message, NetworkManager networkManager) {
        if (!networkManager.isClient()) {
            return;
        }
        try {
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                System.out.println("收到权威出生点: (" + x + ", " + y + ")");
                javafx.application.Platform.runLater(() -> {
                    try {
                        // 如果游戏世界尚未准备，就存入全局变量，待玩家生成后读取
                        try { FXGL.set("authoritativeSpawnX", x); } catch (Throwable ignored) {}
                        try { FXGL.set("authoritativeSpawnY", y); } catch (Throwable ignored) {}
                        // 若玩家已存在，立即迁移至权威出生点
                        try {
                            com.almasb.fxgl.entity.Entity p = FXGL.getGameWorld().getEntitiesByType().stream().filter(e -> e instanceof com.roguelike.entities.Player).findFirst().orElse(null);
                            if (p instanceof com.roguelike.entities.Player) {
                                p.setX(x);
                                p.setY(y);
                                System.out.println("已将本地玩家移动至权威出生点");
                            }
                        } catch (Throwable ignored) {}
                        // 标记已经对齐，允许开始发送自身状态
                        try { com.roguelike.network.NetworkManager.getInstance().markSpawnAligned(); } catch (Throwable ignored) {}
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Exception e) {
            System.err.println("处理权威出生点失败: " + e.getMessage());
        }
    }

    private static void handleSpawnEnemy(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length >= 4) {
                String enemyId = parts[1];
                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                javafx.application.Platform.runLater(() -> {
                    try {
                        com.almasb.fxgl.entity.SpawnData data = new com.almasb.fxgl.entity.SpawnData(x, y).put("enemyId", enemyId);
                        com.almasb.fxgl.dsl.FXGL.spawn("enemy", data);
                    } catch (Throwable e) {
                        System.err.println("客户端生成敌人失败: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("处理SPAWN_ENEMY失败: " + e.getMessage());
        }
    }
    
    // 辅助方法
    private static void sendJoinResponse(String address, int port, boolean success, String message, DatagramSocket gameSocket) {
        try {
            String response = "JOIN_RESPONSE:" + (success ? "SUCCESS" : "FAILED") + ":" + message;
            byte[] data = response.getBytes();
            InetAddress inetAddress = InetAddress.getByName(address);
            
            // 向客户端的实际端口发送响应
            DatagramPacket packet = new DatagramPacket(data, data.length, inetAddress, port);
            
            System.out.println("房主发送加入响应到: " + address + ":" + port);
            System.out.println("响应内容: " + response);
            System.out.println("房主Socket端口: " + gameSocket.getLocalPort());
            
            gameSocket.send(packet);
            System.out.println("加入响应发送成功");
        } catch (Exception e) {
            System.err.println("发送加入响应失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void broadcastPlayerJoined(NetworkPlayer newPlayer, NetworkManager networkManager, DatagramSocket gameSocket) {
        try {
            String message = "PLAYER_JOINED:" + newPlayer.getId() + ":" + newPlayer.getName();
            byte[] data = message.getBytes();
            
            for (NetworkPlayer player : networkManager.getConnectedPlayers().values()) {
                if (!player.isLocal() && !player.equals(newPlayer)) {
                    InetAddress address = InetAddress.getByName(player.getAddress());
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, GAME_PORT);
                    gameSocket.send(packet);
                }
            }
        } catch (Exception e) {
            System.err.println("广播玩家加入失败: " + e.getMessage());
        }
    }
    
    private static void broadcastPlayerLeft(NetworkPlayer leftPlayer, NetworkManager networkManager, DatagramSocket gameSocket) {
        try {
            String message = "PLAYER_LEFT:" + leftPlayer.getId();
            byte[] data = message.getBytes();
            
            for (NetworkPlayer player : networkManager.getConnectedPlayers().values()) {
                if (!player.isLocal()) {
                    InetAddress address = InetAddress.getByName(player.getAddress());
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, GAME_PORT);
                    if (gameSocket != null) {
                        gameSocket.send(packet);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("广播玩家离开失败: " + e.getMessage());
        }
    }
    
    // 删除requestInitialGameState，改为实例方法在NetworkManager中实现
    
    private static void sendInitialGameStateToAllClients(NetworkManager networkManager, DatagramSocket gameSocket) {
        if (!networkManager.isHost()) {
            return;
        }
        
        try {
            // 获取房主的游戏状态 - 使用反射或公共方法
            PlayerState hostState = getLocalPlayerStateInternal(networkManager);
            if (hostState == null) {
                System.out.println("房主状态为空，跳过初始状态发送");
                return;
            }
            
            // 序列化状态
            byte[] stateData = hostState.serialize();
            String message = "INITIAL_STATE:" + new String(stateData, java.nio.charset.StandardCharsets.ISO_8859_1);
            byte[] data = message.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            
            // 发送给所有客户端
            System.out.println("房主开始发送初始状态，当前连接的玩家数量: " + networkManager.getConnectedPlayers().size());
            for (NetworkPlayer player : networkManager.getConnectedPlayers().values()) {
                if (!player.isLocal()) {
                    try {
                        InetAddress address = InetAddress.getByName(player.getAddress());
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, GAME_PORT);
                        gameSocket.send(packet);
                        System.out.println("已发送初始状态给: " + player.getName() + " (" + player.getAddress() + ")");
                    } catch (Exception e) {
                        System.err.println("发送初始状态失败: " + player.getName() + " - " + e.getMessage());
                    }
                } else {
                    System.out.println("跳过本地玩家: " + player.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("发送初始游戏状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取本地玩家状态（供NetworkManagerHelper使用）
     */
    private static PlayerState getLocalPlayerStateInternal(NetworkManager networkManager) {
        try {
            // 获取本地玩家实体（排除网络玩家）
            Entity playerEntity = FXGL.getGameWorld().getEntitiesByType().stream()
                    .filter(e -> e instanceof Player)
                    .filter(e -> {
                        if (e instanceof com.roguelike.entities.Player) {
                            return !((com.roguelike.entities.Player) e).isNetworkPlayer();
                        }
                        return true; // 如果不是Player类型，可能是本地玩家
                    })
                    .findFirst()
                    .orElse(null);
            
            if (playerEntity == null) {
                return null;
            }
            
            Player player = (Player) playerEntity;
            GameState gameState = FXGL.geto("gameState");
            
            if (gameState == null) {
                return null;
            }
            
            // 创建玩家状态
            PlayerState state = new PlayerState();
            state.setPlayerId(networkManager.getLocalPlayer().getId());
            state.setPlayerName(networkManager.getLocalPlayer().getName());
            state.setX(player.getX());
            state.setY(player.getY());
            state.setHp(gameState.getPlayerHP());
            state.setMaxHp(gameState.getPlayerMaxHP());
            state.setLevel(gameState.getLevel());
            state.setScore(gameState.getScore());
            state.setTimestamp(System.currentTimeMillis());
            
            return state;
            
        } catch (Exception e) {
            System.err.println("获取本地玩家状态失败: " + e.getMessage());
            return null;
        }
    }
}
