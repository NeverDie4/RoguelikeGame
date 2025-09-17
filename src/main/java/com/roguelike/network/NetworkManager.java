package com.roguelike.network;

import com.roguelike.core.GameState;
import com.roguelike.entities.Player;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.application.Platform;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 网络管理器 - 负责UDP通信、主机发现、房间管理和游戏状态同步
 */
public class NetworkManager {
    
    // 网络配置
    private static final int DISCOVERY_PORT = 8888;
    private static final int GAME_PORT = 8889;
    private static final String DISCOVERY_MESSAGE = "ROGUELIKE_DISCOVERY";
    private static final String DISCOVERY_RESPONSE = "ROGUELIKE_RESPONSE";
    private static final int MAX_PLAYERS = 4;
    private static final int HEARTBEAT_INTERVAL = 1000; // 1秒心跳
    private static final int DISCOVERY_INTERVAL = 2000; // 2秒发现间隔
    
    // 网络状态
    private boolean isHost = false;
    private boolean isClient = false;
    private boolean isConnected = false;
    private boolean isShuttingDown = false; // 添加关闭标志
    private String playerName = "Player" + (int)(Math.random() * 1000);
    private String roomName = "Default Room";
    
    // 网络组件
    private DatagramSocket discoverySocket;
    private DatagramSocket gameSocket;
    private InetAddress broadcastAddress;
    private ScheduledExecutorService executor;
    
    // 房间和玩家管理
    private final Map<String, GameRoom> discoveredRooms = new ConcurrentHashMap<>();
    private final Map<String, NetworkPlayer> connectedPlayers = new ConcurrentHashMap<>();
    private GameRoom currentRoom;
    private NetworkPlayer localPlayer;
    
    // 游戏状态同步
    private final Map<String, PlayerState> playerStates = new ConcurrentHashMap<>();
    private long lastSyncTime = 0;
    private static final int SYNC_INTERVAL = 50; // 50ms同步间隔
    private volatile boolean spawnAligned = false; // 是否已与房主出生点对齐（客户端）
    private volatile long spawnAlignedAtMs = 0; // 对齐发生的时间戳
    private static final int SYNC_START_DELAY_MS = 800; // 对齐后延迟开始发送的时间

    // X轴镜像修正（用于排查左右颠倒）
    private volatile boolean mirrorXEnabled = true; // 可运行时开关，必要时可在菜单或控制台置为false

    /**
     * 若开启镜像修正，则围绕权威出生点X进行镜像：x' = 2*axis - x
     * 优先使用 FXGL 全局的 authoritativeSpawnX；若不可用则直接返回原值。
     */
    private double applyXMirrorIfNeeded(double x) {
        if (!mirrorXEnabled) return x;
        try {
            Object sx = com.almasb.fxgl.dsl.FXGL.geto("authoritativeSpawnX");
            if (sx instanceof Double axis) {
                double fixed = 2 * axis - x;
                System.out.println("[X-MIRROR] axis=" + axis + ", rawX=" + x + ", fixedX=" + fixed);
                return fixed;
            }
        } catch (Throwable ignored) {}
        return x;
    }
    
    // 回调接口
    private NetworkEventListener eventListener;
    
    public interface NetworkEventListener {
        void onRoomDiscovered(GameRoom room);
        void onRoomLost(String roomId);
        void onPlayerJoined(NetworkPlayer player);
        void onPlayerLeft(String playerId);
        void onGameStateReceived(PlayerState state);
        void onConnectionLost();
    }
    
    private static NetworkManager instance;
    
    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
    
    private NetworkManager() {
        try {
            // 获取广播地址
            broadcastAddress = InetAddress.getByName("255.255.255.255");
            executor = Executors.newScheduledThreadPool(4);
            System.out.println("网络管理器初始化完成，等待用户操作");
            
            // 打印网络接口信息
            printNetworkInterfaces();
        } catch (Exception e) {
            System.err.println("网络管理器初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 打印网络接口信息，用于调试
     */
    private void printNetworkInterfaces() {
        try {
            System.out.println("=== 网络接口信息 ===");
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    System.out.println("接口: " + ni.getName() + " - " + ni.getDisplayName());
                    System.out.println("  支持广播: " + ni.supportsMulticast());
                    java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        java.net.InetAddress addr = addresses.nextElement();
                        System.out.println("  IP: " + addr.getHostAddress());
                    }
                }
            }
            System.out.println("==================");
        } catch (Exception e) {
            System.err.println("打印网络接口信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 开始主机发现
     */
    public void startDiscovery() {
        if (discoverySocket != null) {
            System.out.println("主机发现已在运行中");
            return;
        }
        
        try {
            System.out.println("正在启动主机发现...");
            try {
                // 优先尝试绑定固定端口（方便同网段其他设备发现）
                discoverySocket = new DatagramSocket(DISCOVERY_PORT);
                discoverySocket.setBroadcast(true);
                System.out.println("发现Socket已创建，端口: " + discoverySocket.getLocalPort());
            } catch (Exception bindEx) {
                System.out.println("固定端口" + DISCOVERY_PORT + "被占用，回退到临时端口: " + bindEx.getMessage());
                // 使用系统分配的临时端口，仍可接受房主按来源端口返回的响应
                discoverySocket = new DatagramSocket();
                discoverySocket.setBroadcast(true);
                System.out.println("发现Socket已创建（临时端口）: " + discoverySocket.getLocalPort());
            }
            
            // 启动发现监听线程
            executor.submit(this::discoveryListener);
            System.out.println("发现监听线程已提交");
            
            // 启动发现广播线程
            executor.scheduleAtFixedRate(this::broadcastDiscovery, 0, DISCOVERY_INTERVAL, TimeUnit.MILLISECONDS);
            System.out.println("发现广播线程已启动，间隔: " + DISCOVERY_INTERVAL + "ms");
            
            System.out.println("主机发现已成功启动");
        } catch (Exception e) {
            System.err.println("启动主机发现失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 停止主机发现
     */
    public void stopDiscovery() {
        if (discoverySocket != null) {
            discoverySocket.close();
            discoverySocket = null;
        }
        discoveredRooms.clear();
        System.out.println("主机发现已停止");
    }
    
    /**
     * 创建游戏房间
     */
    public boolean createRoom(String roomName, String playerName) {
        if (isHost || isClient) {
            return false;
        }
        
        try {
            this.roomName = roomName;
            this.playerName = playerName;
            
            // 创建游戏Socket
            gameSocket = new DatagramSocket(GAME_PORT);
            
            // 获取本机IP地址
            String localIP = getLocalIPAddress();
            
            // 创建本地玩家
            localPlayer = new NetworkPlayer(playerName, localIP, true);
            // 房主固定监听游戏端口
            localPlayer.setPort(GAME_PORT);
            localPlayer.setHost(true);
            connectedPlayers.put(localPlayer.getId(), localPlayer);
            
            // 创建房间
            currentRoom = new GameRoom(roomName, localIP, MAX_PLAYERS);
            currentRoom.setHostName(playerName);
            currentRoom.addPlayer(localPlayer);
            
            isHost = true;
            isConnected = true;
            
            // 启动游戏通信监听
            executor.submit(this::gameListener);
            
            // 启动心跳
            executor.scheduleAtFixedRate(this::sendHeartbeat, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
            
            System.out.println("房间创建成功: " + roomName);
            return true;
            
        } catch (Exception e) {
            System.err.println("创建房间失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 加入游戏房间
     */
    public boolean joinRoom(GameRoom room, String playerName) {
        if (isHost || isClient) {
            return false;
        }
        
        try {
            this.playerName = playerName;
            this.roomName = room.getName();
            
            // 创建游戏Socket - 客户端优先使用GAME_PORT，如被占用则向后尝试100个端口
            int clientPort = GAME_PORT;
            while (clientPort < GAME_PORT + 100) {
                try {
                    gameSocket = new DatagramSocket(clientPort);
                    System.out.println("客户端Socket创建成功，端口: " + clientPort);
                    break;
                } catch (Exception e) {
                    clientPort++;
                    if (clientPort >= GAME_PORT + 100) {
                        throw new Exception("无法找到可用端口");
                    }
                }
            }
            
            // 获取本机IP地址
            String localIP = getLocalIPAddress();
            
            // 创建本地玩家
            localPlayer = new NetworkPlayer(playerName, localIP, false);
            // 记录客户端实际监听端口
            localPlayer.setPort(gameSocket.getLocalPort());
            connectedPlayers.put(localPlayer.getId(), localPlayer);
            
            // 设置当前房间
            currentRoom = room;
            currentRoom.addPlayer(localPlayer);
            
            isClient = true;
            isConnected = true;
            
            // 启动游戏通信监听
            executor.submit(this::gameListener);
            
            // 启动心跳
            executor.scheduleAtFixedRate(this::sendHeartbeat, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
            
            // 发送加入请求
            sendJoinRequest(room.getHostAddress());
            
            System.out.println("加入房间成功: " + room.getName());
            return true;
            
        } catch (Exception e) {
            System.err.println("加入房间失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 离开当前房间
     */
    public void leaveRoom() {
        if (!isConnected) {
            return;
        }
        
        try {
            System.out.println("=== 开始离开房间 ===");
            
            // 发送离开消息
            if (isClient && currentRoom != null) {
                sendLeaveMessage(currentRoom.getHostAddress());
            }
            
            // 清理游戏Socket（但不关闭发现Socket，因为可能还需要发现功能）
            if (gameSocket != null && !gameSocket.isClosed()) {
                gameSocket.close();
                gameSocket = null;
                System.out.println("游戏Socket已关闭");
            }
            
            // 重置状态
            isHost = false;
            isClient = false;
            isConnected = false;
            currentRoom = null;
            localPlayer = null;
            connectedPlayers.clear();
            playerStates.clear();
            
            System.out.println("已离开房间");
            
        } catch (Exception e) {
            System.err.println("离开房间时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 启动游戏状态同步
     */
    public void startGameStateSync() {
        if (!isConnected) {
            System.out.println("未连接，无法启动游戏状态同步");
            return;
        }
        
        System.out.println("启动游戏状态同步，连接状态: " + isConnected);
        
        // 启动定期状态同步
        executor.scheduleAtFixedRate(this::syncGameState, 0, SYNC_INTERVAL, TimeUnit.MILLISECONDS);
        
        // 启动定期更新其他玩家实体
        executor.scheduleAtFixedRate(this::updateOtherPlayers, 100, 100, TimeUnit.MILLISECONDS);
        
        System.out.println("游戏状态同步已启动");
    }
    
    /**
     * 发送游戏状态同步
     */
    public void syncGameState() {
        if (!isConnected || gameSocket == null) {
            return;
        }
        // 客户端：等待与权威出生点对齐后再开始发送状态，避免早期(0,0)或左上角漂移
        if (!spawnAligned) {
            // 若尚未对齐，尝试检测当前位置有效性，满足条件后开启同步
            PlayerState probe = getLocalPlayerState();
            if (probe == null) return;
            if (probe.getX() >= 64 && probe.getY() >= 64) {
                spawnAligned = true;
                spawnAlignedAtMs = System.currentTimeMillis();
                System.out.println("本端已对齐出生点，进入延迟窗口: (" + probe.getX() + ", " + probe.getY() + ")");
            } else {
                return;
            }
        }
        // 对齐后延迟窗口，进一步压制早发包
        if (System.currentTimeMillis() - spawnAlignedAtMs < SYNC_START_DELAY_MS) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime < SYNC_INTERVAL) {
            return;
        }
        lastSyncTime = currentTime;
        
        try {
            // 获取本地玩家状态
            PlayerState state = getLocalPlayerState();
            if (state == null) {
                return;
            }
            // 保护：若坐标仍接近左上角（<64像素），则跳过发送，避免把对方拉回左上角
            if (state.getX() < 64 && state.getY() < 64) {
                return;
            }
            
            // 关键修复：将本地玩家状态也添加到playerStates中，这样其他玩家能看到本地玩家
            playerStates.put(state.getPlayerId(), state);
            System.out.println("本地玩家状态已更新: " + state.getPlayerName() + 
                             " 位置: (" + state.getX() + ", " + state.getY() + ")");
            
            // 序列化状态并添加消息头
            byte[] stateData = state.serialize();
            String message = "STATE:" + new String(stateData, java.nio.charset.StandardCharsets.ISO_8859_1);
            byte[] data = message.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            
            // 发送给所有连接的玩家
            System.out.println("发送状态同步，当前连接的玩家数量: " + connectedPlayers.size());
            for (NetworkPlayer player : connectedPlayers.values()) {
                if (!player.isLocal()) {
                    try {
                        InetAddress address = InetAddress.getByName(player.getAddress());
                        int port = player.getPort() > 0 ? player.getPort() : GAME_PORT;
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                        gameSocket.send(packet);
                        System.out.println("已发送状态同步给: " + player.getName() + " (" + player.getAddress() + ")");
                    } catch (Exception e) {
                        System.err.println("发送状态同步失败: " + player.getName() + " - " + e.getMessage());
                    }
                } else {
                    System.out.println("跳过本地玩家: " + player.getName());
                }
            }
            
        } catch (Exception e) {
            System.err.println("同步游戏状态失败: " + e.getMessage());
        }
    }

    /** 标记客户端已与权威出生点对齐 */
    public void markSpawnAligned() {
        this.spawnAligned = true;
        this.spawnAlignedAtMs = System.currentTimeMillis();
        System.out.println("收到权威出生点对齐标记，开始延迟计时: " + spawnAlignedAtMs);
    }
    
    /**
     * 获取本地玩家状态
     */
    private PlayerState getLocalPlayerState() {
        try {
            // 使用Platform.runLater确保在JavaFX线程中执行
            final PlayerState[] result = {null};
            
            if (Platform.isFxApplicationThread()) {
                result[0] = getLocalPlayerStateInternal();
            } else {
                Platform.runLater(() -> {
                    result[0] = getLocalPlayerStateInternal();
                });
                
                // 等待结果（最多100ms）
                long startTime = System.currentTimeMillis();
                while (result[0] == null && (System.currentTimeMillis() - startTime) < 100) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            
            return result[0];
            
        } catch (Exception e) {
            System.err.println("获取本地玩家状态失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 内部获取本地玩家状态方法（在JavaFX线程中执行）
     */
    private PlayerState getLocalPlayerStateInternal() {
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
            state.setPlayerId(localPlayer.getId());
            state.setPlayerName(localPlayer.getName());
            // 本地上报不做镜像：保持权威坐标
            state.setX(player.getX());
            state.setY(player.getY());
            state.setHp(gameState.getPlayerHP());
            state.setMaxHp(gameState.getPlayerMaxHP());
            state.setLevel(gameState.getLevel());
            state.setScore(gameState.getScore());
            state.setTimestamp(System.currentTimeMillis());
            
            return state;
            
        } catch (Exception e) {
            System.err.println("内部获取本地玩家状态失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 基于现有 PlayerState 复制一份并替换X坐标。
     */
    private PlayerState cloneWithX(PlayerState src, double newX) {
        PlayerState dst = new PlayerState();
        dst.setPlayerId(src.getPlayerId());
        dst.setPlayerName(src.getPlayerName());
        dst.setX(newX);
        dst.setY(src.getY());
        dst.setHp(src.getHp());
        dst.setMaxHp(src.getMaxHp());
        dst.setLevel(src.getLevel());
        dst.setScore(src.getScore());
        dst.setTimestamp(src.getTimestamp());
        return dst;
    }
    
    // Getter和Setter方法
    public boolean isHost() { return isHost; }
    public boolean isClient() { return isClient; }
    public boolean isConnected() { return isConnected; }
    public String getPlayerName() { return playerName; }
    public String getRoomName() { return roomName; }
    public Map<String, GameRoom> getDiscoveredRooms() { return discoveredRooms; }
    public Map<String, NetworkPlayer> getConnectedPlayers() { return connectedPlayers; }
    public GameRoom getCurrentRoom() { return currentRoom; }
    public NetworkPlayer getLocalPlayer() { return localPlayer; }
    public Map<String, PlayerState> getPlayerStates() { return playerStates; }
    
    public void setEventListener(NetworkEventListener listener) {
        this.eventListener = listener;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    /** 开关：启用/禁用X轴镜像修正 */
    public void setMirrorXEnabled(boolean enabled) {
        this.mirrorXEnabled = enabled;
        System.out.println("[X-MIRROR] mirrorXEnabled=" + enabled);
    }
    
    /**
     * 获取本机IP地址
     */
    private String getLocalIPAddress() {
        try {
            // 优先选择非回环、已启用、IPv4、私有网段地址
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
            // 回退：第一条非回环IPv4
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            // 最终回退
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            System.err.println("获取本机IP地址失败: " + e.getMessage());
            return "127.0.0.1";
        }
    }

    /**
     * 使用游戏Socket向所有客户端广播游戏消息
     */
    public void broadcastToClients(String message) {
        if (!isHost || gameSocket == null) return;
        try {
            byte[] data = message.getBytes();
            for (NetworkPlayer player : connectedPlayers.values()) {
                if (!player.isLocal()) {
                    try {
                        InetAddress address = InetAddress.getByName(player.getAddress());
                        int port = player.getPort() > 0 ? player.getPort() : GAME_PORT;
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                        gameSocket.send(packet);
                    } catch (Exception e) {
                        System.err.println("广播消息失败: " + player.getName() + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("广播消息异常: " + e.getMessage());
        }
    }

    /**
     * 房主以指定坐标发送权威出生点
     */
    public void sendAuthoritativeSpawn(double x, double y) {
        if (!isHost) return;
        try {
            String message = "AUTHORITATIVE_SPAWN:" + x + ":" + y;
            System.out.println("房主发送权威出生点(指定): (" + x + ", " + y + ") 给 " + (connectedPlayers.size() - 1) + " 名客户端");
            broadcastToClients(message);
        } catch (Exception e) {
            System.err.println("发送指定权威出生点失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        try {
            System.out.println("=== 开始清理网络管理器 ===");
            
            // 设置关闭标志，停止所有网络活动
            isShuttingDown = true;
            System.out.println("设置关闭标志，停止网络活动");
            
            // 离开房间
            leaveRoom();
            
            // 停止发现
            stopDiscovery();
            
            // 关闭所有Socket
            if (discoverySocket != null && !discoverySocket.isClosed()) {
                discoverySocket.close();
                discoverySocket = null;
                System.out.println("发现Socket已关闭");
            }
            
            if (gameSocket != null && !gameSocket.isClosed()) {
                gameSocket.close();
                gameSocket = null;
                System.out.println("游戏Socket已关闭");
            }
            
            // 关闭线程池
            if (executor != null && !executor.isShutdown()) {
                System.out.println("正在关闭线程池...");
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)) {
                        System.out.println("线程池未在3秒内关闭，强制关闭");
                        executor.shutdownNow();
                        // 再等待1秒确保强制关闭完成
                        if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                            System.out.println("警告：线程池强制关闭可能未完成");
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("线程池关闭被中断，强制关闭");
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                executor = null;
                System.out.println("线程池已关闭");
            }
            
            // 清理状态
            isHost = false;
            isClient = false;
            isConnected = false;
            currentRoom = null;
            localPlayer = null;
            connectedPlayers.clear();
            playerStates.clear();
            discoveredRooms.clear();
            
            System.out.println("网络管理器清理完成");
        } catch (Exception e) {
            System.err.println("清理网络管理器时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发现监听线程
     */
    private void discoveryListener() {
        NetworkManagerHelper.discoveryListener(discoverySocket, this, discoveredRooms, isShuttingDown);
    }
    
    /**
     * 广播发现消息
     */
    private void broadcastDiscovery() {
        NetworkManagerHelper.broadcastDiscovery(discoverySocket, this, isShuttingDown);
    }
    
    /**
     * 游戏通信监听线程
     */
    private void gameListener() {
        NetworkManagerHelper.gameListener(gameSocket, this, isShuttingDown);
    }
    
    /**
     * 获取事件监听器
     */
    public NetworkEventListener getEventListener() {
        return eventListener;
    }
    
    /**
     * 发送加入请求
     */
    private void sendJoinRequest(String hostAddress) {
        try {
            // 在加入请求中包含客户端的端口信息
            String message = "JOIN:" + playerName + ":" + gameSocket.getLocalPort();
            byte[] data = message.getBytes();
            InetAddress address = InetAddress.getByName(hostAddress);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, GAME_PORT);
            
            System.out.println("客户端发送加入请求到: " + hostAddress + ":" + GAME_PORT);
            System.out.println("消息内容: " + message);
            System.out.println("客户端Socket端口: " + gameSocket.getLocalPort());
            
            gameSocket.send(packet);
            System.out.println("加入请求发送成功");
        } catch (Exception e) {
            System.err.println("发送加入请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送离开消息
     */
    private void sendLeaveMessage(String hostAddress) {
        try {
            String message = "LEAVE:" + localPlayer.getId();
            byte[] data = message.getBytes();
            InetAddress address = InetAddress.getByName(hostAddress);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, GAME_PORT);
            gameSocket.send(packet);
        } catch (Exception e) {
            System.err.println("发送离开消息失败: " + e.getMessage());
        }
    }

    /**
     * 客户端请求房主发送初始游戏状态
     */
    public void requestInitialGameStateFromHost() {
        if (!isClient || currentRoom == null || gameSocket == null) {
            return;
        }
        try {
            String message = "REQUEST_INITIAL_STATE:" + (localPlayer != null ? localPlayer.getName() : "");
            byte[] data = message.getBytes();
            InetAddress address = InetAddress.getByName(currentRoom.getHostAddress());
            DatagramPacket packet = new DatagramPacket(data, data.length, address, GAME_PORT);
            gameSocket.send(packet);
            System.out.println("已向房主请求初始游戏状态");
        } catch (Exception e) {
            System.err.println("请求初始游戏状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeat() {
        if (!isConnected || gameSocket == null) {
            return;
        }
        
        try {
            String message = "HEARTBEAT:" + localPlayer.getId();
            byte[] data = message.getBytes();
            
            for (NetworkPlayer player : connectedPlayers.values()) {
                if (!player.isLocal()) {
                    int port = player.getPort() > 0 ? player.getPort() : GAME_PORT;
                    InetAddress address = InetAddress.getByName(player.getAddress());
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                    gameSocket.send(packet);
                }
            }
        } catch (Exception e) {
            System.err.println("发送心跳失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新其他玩家实体
     */
    private void updateOtherPlayers() {
        if (!isConnected) {
            return;
        }
        
        try {
            // 在JavaFX线程中更新UI
            Platform.runLater(() -> {
                try {
                    // 检查游戏是否已初始化
                    if (FXGL.getGameScene() == null || FXGL.getGameWorld() == null) {
                        System.out.println("游戏未初始化，跳过其他玩家实体更新");
                        return;
                    }
                    
                    // 为每个远程玩家创建或更新实体
                    System.out.println("更新其他玩家实体，当前playerStates数量: " + playerStates.size());
                    System.out.println("本地玩家ID: " + localPlayer.getId());
                    
                    for (PlayerState state : playerStates.values()) {
                        System.out.println("处理玩家状态: " + state.getPlayerName() + " (ID: " + state.getPlayerId() + ")");
                        
                        if (state.getPlayerId().equals(localPlayer.getId())) {
                            System.out.println("跳过本地玩家: " + state.getPlayerName());
                            continue; // 跳过本地玩家
                        }
                        
                        // 应用X轴镜像修正（若开启且有权威出生点X）
                        PlayerState adjusted = state;
                        try {
                            double rawX = state.getX();
                            double fixedX = applyXMirrorIfNeeded(rawX);
                            if (fixedX != rawX) {
                                adjusted = cloneWithX(state, fixedX);
                            }
                        } catch (Throwable ignored) {}

                        // 查找或创建其他玩家实体
                        Entity otherPlayerEntity = findOrCreateOtherPlayerEntity(adjusted);
                        if (otherPlayerEntity != null) {
                            // 更新其他玩家的位置和状态
                            if (!(adjusted.getX() < 64 && adjusted.getY() < 64)) {
                                otherPlayerEntity.setX(adjusted.getX());
                                otherPlayerEntity.setY(adjusted.getY());
                            }
                            
                            // 可以在这里添加其他状态更新，如血量、等级等
                            System.out.println("更新其他玩家: " + adjusted.getPlayerName() + 
                                             " 位置: (" + adjusted.getX() + ", " + adjusted.getY() + ")");
                        } else {
                            System.out.println("无法创建或找到其他玩家实体: " + state.getPlayerName());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("更新其他玩家实体失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("更新其他玩家失败: " + e.getMessage());
        }
    }
    
    // 存储其他玩家实体的映射
    private final Map<String, Entity> otherPlayerEntities = new ConcurrentHashMap<>();
    
    /**
     * 查找或创建其他玩家实体
     */
    private Entity findOrCreateOtherPlayerEntity(PlayerState state) {
        try {
            // 查找现有的其他玩家实体
            Entity existingEntity = otherPlayerEntities.get(state.getPlayerId());
            
            if (existingEntity != null) {
                return existingEntity;
            }
            
            // 检查游戏世界是否可用
            if (FXGL.getGameWorld() == null) {
                System.out.println("游戏世界未初始化，无法创建其他玩家实体");
                return null;
            }
            
            // 过滤明显无效坐标，避免初始化阶段在(0,0)生成看不见的实体
            if (Double.isNaN(state.getX()) || Double.isNaN(state.getY())) {
                System.out.println("忽略无效坐标的玩家状态: " + state.getPlayerName());
                return null;
            }
            // 启动初期可能短暂上报(0,0)，等待下一次有效同步（阈值放宽到64像素）
            if (state.getX() < 64 && state.getY() < 64) {
                System.out.println("暂不为左上角近原点创建玩家实体: " + state.getPlayerName());
                return null;
            }

            // 创建新的其他玩家实体
            Entity otherPlayer;
            try {
                // 尝试使用EntityFactory创建玩家实体
                otherPlayer = FXGL.getGameWorld().spawn("player", state.getX(), state.getY());
                System.out.println("成功创建其他玩家实体: " + state.getPlayerName());
                
                // 设置其他玩家为网络玩家
                if (otherPlayer instanceof com.roguelike.entities.Player) {
                    ((com.roguelike.entities.Player) otherPlayer).setNetworkPlayer(true);
                }
                
                // 添加名字标签，便于确认远程玩家是否出现
                try {
                    javafx.scene.control.Label nameTag = new javafx.scene.control.Label(state.getPlayerName());
                    nameTag.setStyle("-fx-font-size: 12px; -fx-text-fill: yellow; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 6, 0, 0, 1);");
                    nameTag.setTranslateY(-40);
                    otherPlayer.getViewComponent().addChild(nameTag);
                } catch (Throwable ignored) {}

                // 确保可见与图层在前（避免被地图或HUD遮挡）
                try {
                    otherPlayer.getViewComponent().setVisible(true);
                    otherPlayer.setZIndex(2000);
                    otherPlayer.setOpacity(1.0);
                    System.out.println("远程玩家视图子节点数量: " + otherPlayer.getViewComponent().getChildren().size());
                } catch (Throwable ignored) {}

                // 如果暂时没有任何可见子节点（动画未及时加载），添加一个临时占位符
                try {
                    if (otherPlayer.getViewComponent().getChildren().isEmpty()) {
                        javafx.scene.shape.Rectangle placeholder = new javafx.scene.shape.Rectangle(24, 24, javafx.scene.paint.Color.LIGHTGREEN);
                        placeholder.setOpacity(0.8);
                        placeholder.setArcWidth(6);
                        placeholder.setArcHeight(6);
                        placeholder.setTranslateX(-12);
                        placeholder.setTranslateY(-12);
                        otherPlayer.getViewComponent().addChild(placeholder);
                        System.out.println("为远程玩家添加占位渲染（等待动画加载）: " + state.getPlayerName());
                    }
                } catch (Throwable ignored) {}
                
                // 禁用其他玩家的输入控制 - 通过设置网络玩家标识来实现
                // InputComponent在FXGL中可能不存在，我们通过Player类的isNetworkPlayer标识来控制
                
                // 存储到映射中
                otherPlayerEntities.put(state.getPlayerId(), otherPlayer);
                
                System.out.println("其他玩家实体创建完成: " + state.getPlayerName() + 
                                 " 位置: (" + state.getX() + ", " + state.getY() + ")");
                
                return otherPlayer;
                
            } catch (Exception e) {
                System.out.println("EntityFactory创建失败: " + e.getMessage());
                System.out.println("跳过创建其他玩家实体，等待游戏完全初始化");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("查找或创建其他玩家实体失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 开始游戏（房主功能）
     */
    public void startGame() {
        if (!isHost) {
            System.err.println("只有房主可以开始游戏");
            return;
        }
        
        try {
            // 多人模式：固定使用默认地图，并设置权威开始时间
            String mapName = "test";
            long startAt = System.currentTimeMillis() + 2000;
            try { FXGL.set("authoritativeStartTime", startAt); } catch (Exception ignored) {}
            // 广播游戏开始消息，包含地图名与权威开始时间
            String message = "GAME_START:" + mapName + ":" + startAt;
            byte[] data = message.getBytes();
            
            System.out.println("房主开始游戏，当前连接的玩家数量: " + connectedPlayers.size());
            System.out.println("连接的玩家列表: " + connectedPlayers.keySet());
            
            int notifiedCount = 0;
            for (NetworkPlayer player : connectedPlayers.values()) {
                if (!player.isLocal()) {
                    try {
                        InetAddress address = InetAddress.getByName(player.getAddress());
                        int port = player.getPort() > 0 ? player.getPort() : GAME_PORT;
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                        gameSocket.send(packet);
                        notifiedCount++;
                        System.out.println("已通知玩家: " + player.getName() + " (" + player.getAddress() + ")");
                    } catch (Exception e) {
                        System.err.println("通知玩家失败: " + player.getName() + " - " + e.getMessage());
                    }
                }
            }
            
            System.out.println("房主开始游戏，已通知 " + notifiedCount + " 个玩家");
            
            // 房主也开始游戏
            Platform.runLater(() -> {
                try {
                    // 触发游戏开始
                    FXGL.getGameController().startNewGame();
                    // 尽快发送权威出生点，减少竞态
                    FXGL.runOnce(() -> {
                        try {
                            sendAuthoritativeSpawnToAllClients();
                        } catch (Exception e) {
                            System.err.println("初次发送权威出生点失败: " + e.getMessage());
                        }
                    }, javafx.util.Duration.seconds(0.3));
                    
                    // 延迟启动游戏状态同步，确保游戏实例完全创建
                    FXGL.runOnce(() -> {
                        try {
                            startGameStateSync();
                            System.out.println("房主开始游戏成功，已启动状态同步");
                            
                            // 房主主动发送初始游戏状态给所有客户端
                            sendInitialGameStateToAllClients();

                            // 发送权威出生点给所有客户端，确保玩家出生在同一区域
                            sendAuthoritativeSpawnToAllClients();
                        } catch (Exception e) {
                            System.err.println("房主启动状态同步失败: " + e.getMessage());
                        }
                    }, javafx.util.Duration.seconds(2));
                } catch (Exception e) {
                    System.err.println("房主开始游戏失败: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            System.err.println("开始游戏失败: " + e.getMessage());
        }
    }
    
    /**
     * 房主发送初始游戏状态给所有客户端
     */
    private void sendInitialGameStateToAllClients() {
        if (!isHost) {
            return;
        }
        
        try {
            // 获取房主的游戏状态
            PlayerState hostState = getLocalPlayerState();
            if (hostState == null) {
                System.out.println("房主状态为空，跳过初始状态发送");
                return;
            }
            
            // 序列化状态
            byte[] stateData = hostState.serialize();
            String message = "INITIAL_STATE:" + new String(stateData, java.nio.charset.StandardCharsets.ISO_8859_1);
            byte[] data = message.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            
            // 发送给所有客户端
            System.out.println("房主开始发送初始状态，当前连接的玩家数量: " + connectedPlayers.size());
            for (NetworkPlayer player : connectedPlayers.values()) {
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
     * 房主发送权威出生点给所有客户端
     */
    private void sendAuthoritativeSpawnToAllClients() {
        if (!isHost) {
            return;
        }
        try {
            PlayerState hostState = getLocalPlayerState();
            if (hostState == null) {
                System.out.println("房主出生点不可用，跳过发送");
                return;
            }
            String message = "AUTHORITATIVE_SPAWN:" + hostState.getX() + ":" + hostState.getY();
            System.out.println("准备发送权威出生点给客户端: (" + hostState.getX() + ", " + hostState.getY() + ") - 客户端数: " + connectedPlayers.size());
            byte[] data = message.getBytes();
            for (NetworkPlayer player : connectedPlayers.values()) {
                if (!player.isLocal()) {
                    try {
                        InetAddress address = InetAddress.getByName(player.getAddress());
                        int port = player.getPort() > 0 ? player.getPort() : GAME_PORT;
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                        gameSocket.send(packet);
                        System.out.println("已发送权威出生点给: " + player.getName() + " -> " + player.getAddress() + ":" + port);
                    } catch (Exception e) {
                        System.err.println("发送权威出生点失败: " + player.getName() + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("发送权威出生点异常: " + e.getMessage());
        }
    }
}
