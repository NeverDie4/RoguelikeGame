package com.roguelike.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.network.NetworkManager;
import com.roguelike.network.NetworkPlayer;
import com.roguelike.network.PlayerState;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

/**
 * 网络HUD组件 - 显示多人游戏中的其他玩家信息
 */
public class NetworkHUD {
    
    private VBox networkContainer;
    private NetworkManager networkManager;
    private boolean isVisible = false;
    
    public NetworkHUD() {
        // 延迟初始化NetworkManager，避免在游戏启动时自动触发网络功能
        // networkManager = NetworkManager.getInstance();
        createNetworkHUD();
    }
    
    /**
     * 创建网络HUD界面
     */
    private void createNetworkHUD() {
        networkContainer = new VBox(5);
        networkContainer.setAlignment(Pos.TOP_RIGHT);
        networkContainer.setPadding(new Insets(10));
        networkContainer.setPrefWidth(250);
        networkContainer.setMaxWidth(250);
        
        // 设置背景样式
        networkContainer.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.7); " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: rgba(45, 212, 191, 0.8); " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 10;"
        );
        
        // 标题
        Label title = new Label("在线玩家");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        title.setTextFill(Color.WHITE);
        title.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(45, 212, 191, 0.8), 5, 0, 0, 2);"
        );
        
        networkContainer.getChildren().add(title);
        
        // 注意：不在初始化时调用updatePlayerList()，避免自动触发网络功能
    }
    
    /**
     * 显示网络HUD
     */
    public void show() {
        if (isVisible || networkContainer == null) {
            return;
        }
        
        FXGL.getGameScene().getContentRoot().getChildren().add(networkContainer);
        isVisible = true;
        
        // 设置位置（右上角）
        networkContainer.setLayoutX(FXGL.getAppWidth() - networkContainer.getPrefWidth() - 20);
        networkContainer.setLayoutY(20);
        
        System.out.println("网络HUD已显示");
    }
    
    /**
     * 隐藏网络HUD
     */
    public void hide() {
        if (!isVisible || networkContainer == null) {
            return;
        }
        
        if (FXGL.getGameScene().getContentRoot().getChildren().contains(networkContainer)) {
            FXGL.getGameScene().getContentRoot().getChildren().remove(networkContainer);
        }
        
        isVisible = false;
        System.out.println("网络HUD已隐藏");
    }
    
    /**
     * 更新玩家列表
     */
    public void updatePlayerList() {
        if (networkContainer == null) {
            return;
        }
        
        Platform.runLater(() -> {
            // 清除现有玩家信息（保留标题）
            if (networkContainer.getChildren().size() > 1) {
                networkContainer.getChildren().remove(1, networkContainer.getChildren().size());
            }
            
            // 延迟初始化NetworkManager
            if (networkManager == null) {
                networkManager = NetworkManager.getInstance();
            }
            
            if (!networkManager.isConnected()) {
                return;
            }
            
            Map<String, NetworkPlayer> players = networkManager.getConnectedPlayers();
            Map<String, PlayerState> states = networkManager.getPlayerStates();
            
            for (NetworkPlayer player : players.values()) {
                if (player.isLocal()) {
                    continue; // 跳过本地玩家
                }
                
                VBox playerInfo = createPlayerInfoBox(player, states.get(player.getId()));
                networkContainer.getChildren().add(playerInfo);
            }
            
            // 如果没有其他玩家，显示提示
            if (players.size() <= 1) {
                Label noPlayersLabel = new Label("等待其他玩家...");
                noPlayersLabel.setFont(Font.font("Consolas", 12));
                noPlayersLabel.setTextFill(Color.LIGHTGRAY);
                noPlayersLabel.setStyle("-fx-font-style: italic;");
                networkContainer.getChildren().add(noPlayersLabel);
            }
        });
    }
    
    /**
     * 创建玩家信息框
     */
    private VBox createPlayerInfoBox(NetworkPlayer player, PlayerState state) {
        VBox playerBox = new VBox(3);
        playerBox.setPadding(new Insets(5));
        playerBox.setStyle(
            "-fx-background-color: rgba(30, 30, 35, 0.8); " +
            "-fx-background-radius: 5; " +
            "-fx-border-color: rgba(60, 60, 65, 0.5); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 5;"
        );
        
        // 玩家名称
        Label nameLabel = new Label(player.getName());
        nameLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.WHITE);
        
        // 连接状态
        Label statusLabel = new Label("在线");
        statusLabel.setFont(Font.font("Consolas", 10));
        statusLabel.setTextFill(Color.LIGHTGREEN);
        
        playerBox.getChildren().addAll(nameLabel, statusLabel);
        
        // 如果有状态信息，显示详细信息
        if (state != null) {
            HBox infoRow = new HBox(10);
            
            // 血量信息
            Label hpLabel = new Label("HP: " + state.getHp() + "/" + state.getMaxHp());
            hpLabel.setFont(Font.font("Consolas", 10));
            hpLabel.setTextFill(Color.LIGHTBLUE);
            
            // 等级信息
            Label levelLabel = new Label("Lv." + state.getLevel());
            levelLabel.setFont(Font.font("Consolas", 10));
            levelLabel.setTextFill(Color.YELLOW);
            
            infoRow.getChildren().addAll(hpLabel, levelLabel);
            playerBox.getChildren().add(infoRow);
            
            // 位置信息（可选，用于调试）
            // 注意：FXGL的isDeveloperMode()方法可能不存在，这里注释掉调试信息
            // if (FXGL.getGameController().isDeveloperMode()) {
            //     Label posLabel = new Label(String.format("位置: (%.0f, %.0f)", state.getX(), state.getY()));
            //     posLabel.setFont(Font.font("Consolas", 9));
            //     posLabel.setTextFill(Color.GRAY);
            //     playerBox.getChildren().add(posLabel);
            // }
        }
        
        return playerBox;
    }
    
    /**
     * 更新特定玩家的状态
     */
    public void updatePlayerState(String playerId) {
        if (!isVisible) {
            return;
        }
        
        updatePlayerList();
    }
    
    /**
     * 检查HUD是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * 设置网络管理器
     */
    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        hide();
        networkContainer = null;
        networkManager = null;
    }
}

