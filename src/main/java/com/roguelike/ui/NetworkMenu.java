package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.network.GameRoom;
import com.roguelike.network.NetworkManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Map;

/**
 * 网络菜单 - 负责房间发现、创建和加入功能
 */
public class NetworkMenu {
    
    private static NetworkMenu instance;
    private StackPane menuContainer;
    private StackPane overlay;
    private NetworkManager networkManager;
    private ListView<GameRoom> roomListView;
    private TextField playerNameField;
    private TextField roomNameField;
    private Label statusLabel;
    private Button refreshButton;
    private Button createRoomButton;
    private Button joinRoomButton;
    private Button backButton;
    private boolean isVisible = false;
    private boolean isUpdatingRooms = false; // 避免刷新房间列表时触发选中变更造成按钮闪烁
    private String lastSelectedRoomId = null; // 记录上次选中的房间ID，刷新后恢复选中
    
    private NetworkMenu() {
        networkManager = NetworkManager.getInstance();
        createMenu();
        setupNetworkListener();
    }
    
    public static NetworkMenu getInstance() {
        if (instance == null) {
            instance = new NetworkMenu();
        }
        return instance;
    }
    
    /**
     * 创建网络菜单界面
     */
    private void createMenu() {
        menuContainer = new StackPane();
        menuContainer.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.9); " +
            "-fx-background-radius: 15; " +
            "-fx-border-color: rgba(45, 212, 191, 0.8); " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 15;"
        );
        menuContainer.setPrefSize(600, 500);
        
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(30));
        
        // 标题
        Label title = new Label("多人游戏频道");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.8), 10, 0, 0, 3); " +
            "-fx-text-alignment: center;"
        );
        
        // 玩家名称输入
        HBox playerNameContainer = new HBox(10);
        playerNameContainer.setAlignment(Pos.CENTER);
        Label playerNameLabel = new Label("玩家名称:");
        playerNameLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        playerNameLabel.setTextFill(Color.WHITE);
        
        playerNameField = new TextField("Player" + (int)(Math.random() * 1000));
        playerNameField.setPrefWidth(200);
        playerNameField.setFont(Font.font("Consolas", 14));
        playerNameField.setStyle(
            "-fx-background-color: rgba(30, 30, 35, 0.9); " +
            "-fx-text-fill: white; " +
            "-fx-border-color: rgba(45, 212, 191, 0.6); " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5;"
        );
        playerNameContainer.getChildren().addAll(playerNameLabel, playerNameField);
        
        // 房间创建区域
        VBox createRoomContainer = new VBox(10);
        createRoomContainer.setAlignment(Pos.CENTER);
        
        Label createRoomLabel = new Label("创建房间");
        createRoomLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        createRoomLabel.setTextFill(Color.LIGHTBLUE);
        
        HBox roomNameContainer = new HBox(10);
        roomNameContainer.setAlignment(Pos.CENTER);
        Label roomNameLabel = new Label("房间名称:");
        roomNameLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        roomNameLabel.setTextFill(Color.WHITE);
        
        roomNameField = new TextField("我的房间");
        roomNameField.setPrefWidth(200);
        roomNameField.setFont(Font.font("Consolas", 14));
        roomNameField.setStyle(
            "-fx-background-color: rgba(30, 30, 35, 0.9); " +
            "-fx-text-fill: white; " +
            "-fx-border-color: rgba(45, 212, 191, 0.6); " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5;"
        );
        roomNameContainer.getChildren().addAll(roomNameLabel, roomNameField);
        
        createRoomButton = createStyledButton("创建房间", this::createRoom);
        createRoomContainer.getChildren().addAll(createRoomLabel, roomNameContainer, createRoomButton);
        
        // 房间列表区域
        VBox roomListContainer = new VBox(10);
        roomListContainer.setAlignment(Pos.CENTER);
        
        Label roomListLabel = new Label("可用房间");
        roomListLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        roomListLabel.setTextFill(Color.LIGHTGREEN);
        
        roomListView = new ListView<>();
        roomListView.setPrefSize(400, 150);
        roomListView.setStyle(
            "-fx-background-color: rgba(30, 30, 35, 0.9); " +
            "-fx-border-color: rgba(45, 212, 191, 0.6); " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5;"
        );
        
        // 设置房间列表的单元格工厂
        roomListView.setCellFactory(listView -> new ListCell<GameRoom>() {
            @Override
            protected void updateItem(GameRoom room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // 检查是否是房主自己的房间
                    boolean isMyRoom = networkManager.isHost() && 
                                     networkManager.getCurrentRoom() != null &&
                                     room.getId().equals(networkManager.getCurrentRoom().getId());
                    
                    String roomText;
                    if (isMyRoom) {
                        roomText = String.format("🏠 %s (我的房间) - %d/%d 玩家", 
                            room.getName(), room.getPlayerCount(), room.getMaxPlayers());
                    } else {
                        roomText = String.format("%s (%s) - %d/%d 玩家", 
                            room.getName(), room.getHostName(), room.getPlayerCount(), room.getMaxPlayers());
                    }
                    
                    setText(roomText);
                    setFont(Font.font("Consolas", 12));
                    
                    if (isMyRoom) {
                        // 房主房间特殊样式
                        setTextFill(Color.LIGHTGREEN);
                        setStyle(
                            "-fx-background-color: rgba(30, 80, 30, 0.9); " +
                            "-fx-border-color: rgba(45, 212, 191, 1.0); " +
                            "-fx-border-width: 2; " +
                            "-fx-padding: 5; " +
                            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.5), 3, 0, 0, 1);"
                        );
                    } else {
                        // 普通房间样式
                        setTextFill(Color.WHITE);
                        setStyle(
                            "-fx-background-color: rgba(40, 40, 45, 0.8); " +
                            "-fx-border-color: rgba(60, 60, 65, 0.5); " +
                            "-fx-border-width: 1; " +
                            "-fx-padding: 5;"
                        );
                    }
                }
            }
        });
        
        refreshButton = createStyledButton("刷新房间", this::refreshRooms);
        joinRoomButton = createStyledButton("加入房间", this::joinRoom);
        joinRoomButton.setDisable(true);
        
        HBox roomButtonContainer = new HBox(10);
        roomButtonContainer.setAlignment(Pos.CENTER);
        roomButtonContainer.getChildren().addAll(refreshButton, joinRoomButton);
        
        roomListContainer.getChildren().addAll(roomListLabel, roomListView, roomButtonContainer);
        
        // 状态标签
        statusLabel = new Label("准备就绪");
        statusLabel.setFont(Font.font("Consolas", 12));
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setStyle("-fx-font-style: italic;");
        
        // 返回按钮
        backButton = createStyledButton("返回主菜单", this::safeExitToMainMenu);
        
        mainContainer.getChildren().addAll(
            title, playerNameContainer, createRoomContainer, 
            roomListContainer, statusLabel, backButton
        );
        
        menuContainer.getChildren().add(mainContainer);
        
        // 监听房间选择
        roomListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingRooms) {
                // 刷新列表时的临时选中变化忽略，防止按钮明暗闪烁
                return;
            }
            if (newVal != null) {
                lastSelectedRoomId = newVal.getId();
                joinRoomButton.setDisable(false);
            } else {
                joinRoomButton.setDisable(true);
            }
        });
    }
    
    /**
     * 设置网络事件监听
     */
    private void setupNetworkListener() {
        networkManager.setEventListener(new NetworkManager.NetworkEventListener() {
            @Override
            public void onRoomDiscovered(GameRoom room) {
                Platform.runLater(() -> {
                    updateRoomList();
                    updateStatus("发现新房间: " + room.getName());
                });
            }
            
            @Override
            public void onRoomLost(String roomId) {
                Platform.runLater(() -> {
                    updateRoomList();
                    updateStatus("房间已断开: " + roomId);
                });
            }
            
            @Override
            public void onPlayerJoined(com.roguelike.network.NetworkPlayer player) {
                Platform.runLater(() -> {
                    updateStatus("玩家加入: " + player.getName());
                });
            }
            
            @Override
            public void onPlayerLeft(String playerId) {
                Platform.runLater(() -> {
                    updateStatus("玩家离开: " + playerId);
                });
            }
            
            @Override
            public void onGameStateReceived(com.roguelike.network.PlayerState state) {
                // 游戏状态更新在游戏内处理
            }
            
            @Override
            public void onConnectionLost() {
                Platform.runLater(() -> {
                    updateStatus("连接丢失，请重新连接");
                    hide();
                });
            }
        });
    }
    
    /**
     * 创建房间
     */
    private void createRoom() {
        String playerName = playerNameField.getText().trim();
        String roomName = roomNameField.getText().trim();
        
        if (playerName.isEmpty() || roomName.isEmpty()) {
            updateStatus("请输入玩家名称和房间名称");
            return;
        }
        
        updateStatus("正在创建房间...");
        
        if (networkManager.createRoom(roomName, playerName)) {
            updateStatus("房间创建成功！等待其他玩家加入...");
            // 启动房间发现，让其他玩家能找到这个房间
            networkManager.startDiscovery();
            
            // 房主创建房间后，添加"开始游戏"按钮
            addStartGameButton();
            
            // 立即更新房间列表，显示房主自己的房间
            updateRoomList();
        } else {
            updateStatus("房间创建失败，请重试");
        }
    }
    
    /**
     * 添加开始游戏按钮（仅房主可见）
     */
    private void addStartGameButton() {
        try {
            System.out.println("=== 添加开始游戏按钮 ===");
            
            // 检查是否已经有开始游戏按钮
            boolean hasStartButton = false;
            for (var child : menuContainer.getChildren()) {
                if (child instanceof Button && ((Button) child).getText().contains("开始游戏")) {
                    hasStartButton = true;
                    break;
                }
            }
            
            if (!hasStartButton) {
                Button startGameButton = createStyledButton("开始游戏", this::startGame);
                
                // 将开始游戏按钮添加到菜单容器的最后（返回按钮之前）
                VBox mainContainer = (VBox) menuContainer.getChildren().get(0);
                int insertIndex = mainContainer.getChildren().size() - 1; // 在返回按钮之前插入
                mainContainer.getChildren().add(insertIndex, startGameButton);
                
                updateStatus("房间已创建，点击'开始游戏'开始多人游戏");
                System.out.println("开始游戏按钮已添加");
            } else {
                System.out.println("开始游戏按钮已存在");
            }
        } catch (Exception e) {
            System.err.println("添加开始游戏按钮失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 开始游戏（房主功能）
     */
    private void startGame() {
        if (networkManager.isHost()) {
            updateStatus("房主开始游戏...");
            hide();
            // 房主开始游戏，所有玩家都会收到通知并开始游戏
            networkManager.startGame();
        } else {
            updateStatus("只有房主可以开始游戏");
        }
    }
    
    /**
     * 加入房间
     */
    private void joinRoom() {
        GameRoom selectedRoom = roomListView.getSelectionModel().getSelectedItem();
        String playerName = playerNameField.getText().trim();
        
        if (selectedRoom == null) {
            updateStatus("请选择一个房间");
            return;
        }
        
        if (playerName.isEmpty()) {
            updateStatus("请输入玩家名称");
            return;
        }
        
        updateStatus("正在加入房间...");
        
        if (networkManager.joinRoom(selectedRoom, playerName)) {
            updateStatus("成功加入房间！等待房主开始游戏...");
            // 客户端加入房间后，显示等待界面而不是隐藏菜单
            showWaitingInterface();
        } else {
            updateStatus("加入房间失败，请重试");
        }
    }
    
    /**
     * 显示等待界面（客户端加入房间后）
     */
    private void showWaitingInterface() {
        try {
            System.out.println("=== 显示等待界面 ===");
            
            // 清空菜单内容
            menuContainer.getChildren().clear();
            
            // 创建等待界面
            VBox waitingContainer = new VBox(20);
            waitingContainer.setAlignment(Pos.CENTER);
            waitingContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-padding: 40;");
            
            // 标题
            Label title = new Label("等待房主开始游戏");
            title.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");
            
            // 房间信息
            Label roomInfo = new Label("房间: " + networkManager.getCurrentRoom().getName());
            roomInfo.setStyle("-fx-font-size: 16px; -fx-text-fill: lightblue;");
            
            // 玩家信息
            Label playerInfo = new Label("玩家: " + networkManager.getLocalPlayer().getName());
            playerInfo.setStyle("-fx-font-size: 16px; -fx-text-fill: lightgreen;");
            
            // 等待提示
            Label waitingLabel = new Label("请等待房主点击'开始游戏'...");
            waitingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: yellow;");
            
            // 离开房间按钮
            Button leaveButton = createStyledButton("离开房间", this::leaveRoom);
            
            waitingContainer.getChildren().addAll(title, roomInfo, playerInfo, waitingLabel, leaveButton);
            menuContainer.getChildren().add(waitingContainer);
            
            System.out.println("等待界面已显示");
        } catch (Exception e) {
            System.err.println("显示等待界面失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 离开房间（从等待界面）
     */
    private void leaveRoom() {
        try {
            System.out.println("=== 客户端离开房间 ===");
            
            // 离开房间
            networkManager.leaveRoom();
            
            // 隐藏菜单
            hide();
            
            System.out.println("客户端已离开房间");
        } catch (Exception e) {
            System.err.println("离开房间失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 刷新房间列表
     */
    private void refreshRooms() {
        updateStatus("正在搜索房间...");
        networkManager.startDiscovery();
        
        // 延迟更新状态
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                updateRoomList();
                if (networkManager.getDiscoveredRooms().isEmpty()) {
                    updateStatus("未发现任何房间，请确保其他玩家已创建房间");
                } else {
                    updateStatus("发现 " + networkManager.getDiscoveredRooms().size() + " 个房间");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * 更新房间列表
     */
    private void updateRoomList() {
        isUpdatingRooms = true;
        // 记录当前选中项
        GameRoom selected = roomListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            lastSelectedRoomId = selected.getId();
        }

        roomListView.getItems().clear();
        Map<String, GameRoom> rooms = networkManager.getDiscoveredRooms();
        
        // 添加发现的房间
        for (GameRoom room : rooms.values()) {
            roomListView.getItems().add(room);
            System.out.println("添加发现的房间: " + room.getName() + " (" + room.getHostAddress() + ")");
        }
        
        // 如果房主创建了房间，也要显示自己的房间
        if (networkManager.isHost() && networkManager.getCurrentRoom() != null) {
            GameRoom myRoom = networkManager.getCurrentRoom();
            // 检查是否已经在列表中（避免重复）
            boolean alreadyExists = roomListView.getItems().stream()
                .anyMatch(room -> room.getId().equals(myRoom.getId()));
            
            if (!alreadyExists) {
                roomListView.getItems().add(myRoom);
                System.out.println("添加房主自己的房间到列表: " + myRoom.getName());
            } else {
                System.out.println("房主房间已存在于列表中，跳过重复添加");
            }
        }
        
        // 刷新后恢复原先选中，避免按钮禁用闪烁
        if (lastSelectedRoomId != null) {
            for (GameRoom r : roomListView.getItems()) {
                if (lastSelectedRoomId.equals(r.getId())) {
                    roomListView.getSelectionModel().select(r);
                    break;
                }
            }
        }

        // 若仍未选中项，则根据是否有房间决定按钮状态
        if (roomListView.getSelectionModel().getSelectedItem() == null) {
            joinRoomButton.setDisable(roomListView.getItems().isEmpty());
        }

        isUpdatingRooms = false;

        System.out.println("房间列表已更新，共 " + roomListView.getItems().size() + " 个房间");
        
        // 打印所有房间的详细信息用于调试
        for (int i = 0; i < roomListView.getItems().size(); i++) {
            GameRoom room = roomListView.getItems().get(i);
            System.out.println("房间 " + (i+1) + ": " + room.getName() + " (" + room.getHostAddress() + ") - " + room.getPlayerCount() + "/" + room.getMaxPlayers());
        }
    }
    
    /**
     * 更新状态信息
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
        System.out.println("[网络菜单] " + message);
    }
    
    /**
     * 创建样式化按钮
     */
    private Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        button.setPrefWidth(120);
        button.setPrefHeight(35);
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(45, 212, 191, 0.8), rgba(30, 150, 140, 0.8)); " +
            "-fx-text-fill: white; " +
            "-fx-border-color: rgba(45, 212, 191, 1.0); " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 5, 0, 0, 2);"
        );
        
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(45, 212, 191, 1.0), rgba(30, 150, 140, 1.0)); " +
                "-fx-text-fill: white; " +
                "-fx-border-color: rgba(45, 212, 191, 1.0); " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 8, 0, 0, 3);"
            );
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(45, 212, 191, 0.8), rgba(30, 150, 140, 0.8)); " +
                "-fx-text-fill: white; " +
                "-fx-border-color: rgba(45, 212, 191, 1.0); " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 5, 0, 0, 2);"
            );
        });
        
        button.setOnAction(e -> action.run());
        
        return button;
    }
    
    /**
     * 显示网络菜单
     */
    public void show() {
        if (isVisible) {
            return;
        }
        
        try {
            System.out.println("=== 开始显示网络菜单 ===");
            
            // 检测当前场景类型 - 参考OptionsMenu的实现
            boolean isInGameScene = false;
            try {
                if (FXGL.getGameScene() != null && 
                    FXGL.getPrimaryStage().getScene() != null) {
                    if (FXGL.getGameScene().getRoot().getChildren().size() > 0) {
                        if (FXGL.getPrimaryStage().getScene().getRoot().getChildrenUnmodifiable().contains(FXGL.getGameScene().getRoot())) {
                            isInGameScene = true;
                            System.out.println("检测到游戏场景：游戏场景根节点在主舞台中");
                        } else {
                            System.out.println("检测到主菜单场景：游戏场景根节点不在主舞台中");
                        }
                    } else {
                        System.out.println("检测到主菜单场景：游戏场景根节点为空");
                    }
                } else {
                    System.out.println("检测到主菜单场景：游戏场景或主舞台为空");
                }
            } catch (Exception e) {
                System.out.println("检测场景类型时出错: " + e.getMessage());
                isInGameScene = false;
            }
            
            if (!isInGameScene) {
                System.out.println("当前在主菜单场景，使用主菜单显示方式");
                showInMainMenu();
                return;
            }
            
            System.out.println("当前在游戏场景，使用游戏场景显示方式");
            showInGameScene();
            
        } catch (Exception e) {
            System.err.println("显示网络菜单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 在主菜单场景中显示网络菜单
     */
    private void showInMainMenu() {
        try {
            // 创建覆盖层
            overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
            overlay.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
            overlay.setMaxSize(FXGL.getAppWidth(), FXGL.getAppHeight());
            overlay.setMinSize(FXGL.getAppWidth(), FXGL.getAppHeight());
            
            // 添加点击背景关闭功能
            overlay.setOnMouseClicked(e -> {
                if (e.getTarget() == overlay) {
                    hide();
                }
            });
            
            // 将菜单容器添加到覆盖层
            overlay.getChildren().add(menuContainer);
            
            // 添加到主舞台场景根节点
            if (FXGL.getPrimaryStage().getScene() != null && 
                FXGL.getPrimaryStage().getScene().getRoot() instanceof javafx.scene.layout.Pane) {
                ((javafx.scene.layout.Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().add(overlay);
                System.out.println("覆盖层已添加到主舞台场景");
            }
            
            // 确保覆盖层在最顶层
            overlay.setViewOrder(-2000);
            overlay.toFront();
            System.out.println("覆盖层已设置到最顶层");
            
            isVisible = true;
            updateStatus("网络菜单已打开");
            
            // 自动刷新房间列表
            refreshRooms();
            
            System.out.println("网络菜单在主菜单中显示成功");
        } catch (Exception e) {
            System.err.println("在主菜单中显示网络菜单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 在游戏场景中显示网络菜单
     */
    private void showInGameScene() {
        try {
            // 创建覆盖层
            overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
            overlay.setPrefSize(FXGL.getAppWidth(), FXGL.getAppHeight());
            overlay.setMaxSize(FXGL.getAppWidth(), FXGL.getAppHeight());
            overlay.setMinSize(FXGL.getAppWidth(), FXGL.getAppHeight());
            
            // 添加点击背景关闭功能
            overlay.setOnMouseClicked(e -> {
                if (e.getTarget() == overlay) {
                    hide();
                }
            });
            
            // 将菜单容器添加到覆盖层
            overlay.getChildren().add(menuContainer);
            
            // 添加到游戏场景根节点
            FXGL.getGameScene().getRoot().getChildren().add(overlay);
            System.out.println("覆盖层已添加到游戏场景");
            
            // 确保覆盖层在最顶层
            overlay.setViewOrder(-2000);
            overlay.toFront();
            System.out.println("覆盖层已设置到最顶层");
            
            isVisible = true;
            updateStatus("网络菜单已打开");
            
            // 自动刷新房间列表
            refreshRooms();
            
            System.out.println("网络菜单在游戏场景中显示成功");
        } catch (Exception e) {
            System.err.println("在游戏场景中显示网络菜单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 隐藏网络菜单
     */
    public void hide() {
        if (!isVisible) {
            return;
        }
        
        try {
            System.out.println("=== 开始隐藏网络菜单 ===");
            
            if (overlay != null) {
                // 尝试从主舞台场景移除
                if (FXGL.getPrimaryStage().getScene() != null && 
                    FXGL.getPrimaryStage().getScene().getRoot() instanceof javafx.scene.layout.Pane) {
                    ((javafx.scene.layout.Pane) FXGL.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
                    System.out.println("覆盖层已从主舞台场景移除");
                }
                
                // 尝试从游戏场景移除
                if (FXGL.getGameScene() != null && FXGL.getGameScene().getRoot() != null) {
                    FXGL.getGameScene().getRoot().getChildren().remove(overlay);
                    System.out.println("覆盖层已从游戏场景移除");
                }
            }
            
            overlay = null;
            isVisible = false;
            updateStatus("网络菜单已关闭");
            
            System.out.println("网络菜单隐藏成功");
        } catch (Exception e) {
            System.err.println("隐藏网络菜单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查菜单是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * 强制清理菜单
     */
    public static void forceCleanup() {
        if (instance != null) {
            if (instance.isVisible) {
                instance.hide();
            }
            // 清理网络资源
            if (instance.networkManager != null) {
                instance.networkManager.cleanup();
            }
            System.out.println("NetworkMenu强制清理完成");
        }
    }
    
    /**
     * 安全退出到主菜单
     */
    public void safeExitToMainMenu() {
        try {
            System.out.println("=== 安全退出到主菜单 ===");
            
            // 如果当前是主机或客户端，先离开房间
            if (networkManager.isConnected()) {
                System.out.println("正在离开房间...");
                networkManager.leaveRoom();
            }
            
            // 停止发现
            if (networkManager != null) {
                networkManager.stopDiscovery();
            }
            
            // 隐藏菜单
            hide();
            
            System.out.println("安全退出到主菜单完成");
        } catch (Exception e) {
            System.err.println("安全退出到主菜单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

