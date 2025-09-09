package com.roguelike.utils;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.*;

public class FlowFieldPathfinding extends Application {
    
    // 可配置参数
    private static final int GRID_SIZE = 800;
    private static final int CELL_SIZE = 20;
    private static final int NUM_CELLS = GRID_SIZE / CELL_SIZE;
    
    private Canvas canvas;
    private GraphicsContext gc;
    private FlowField flowField;
    
    // 代理（Agent）类代表游戏中的移动单位
    private List<Agent> agents = new ArrayList<>();
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Flow Field Pathfinding");
        
        BorderPane root = new BorderPane();
        canvas = new Canvas(GRID_SIZE, GRID_SIZE);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);
        
        // 初始化流场
        flowField = new FlowField(NUM_CELLS, CELL_SIZE);
        
        // 设置鼠标事件
        setupMouseEvents();
        
        Scene scene = new Scene(root, GRID_SIZE, GRID_SIZE);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 启动动画循环
        startAnimation();
    }
    
    private void setupMouseEvents() {
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            int x = (int) (event.getX() / CELL_SIZE);
            int y = (int) (event.getY() / CELL_SIZE);
            
            if (event.getButton() == MouseButton.PRIMARY) {
                // 左键设置目标点
                flowField.setTarget(x, y);
                flowField.updateFlowField();
                
                // 添加代理
                if (agents.size() < 100) {
                    agents.add(new Agent(
                        (int) (Math.random() * NUM_CELLS), 
                        (int) (Math.random() * NUM_CELLS),
                        CELL_SIZE
                    ));
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                // 右键切换障碍物
                flowField.toggleObstacle(x, y);
                flowField.updateFlowField();
            }
        });
        
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            int x = (int) (event.getX() / CELL_SIZE);
            int y = (int) (event.getY() / CELL_SIZE);
            
            if (event.getButton() == MouseButton.SECONDARY) {
                // 拖拽设置障碍物
                flowField.setObstacle(x, y, true);
                flowField.updateFlowField();
            }
        });
    }
    
    private void startAnimation() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                render();
                update();
            }
        }.start();
    }
    
    private void render() {
        // 清空画布
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, GRID_SIZE, GRID_SIZE);
        
        // 绘制流场
        flowField.draw(gc);
        
        // 绘制代理
        for (Agent agent : agents) {
            agent.draw(gc);
        }
    }
    
    private void update() {
        // 更新代理位置
        for (Agent agent : agents) {
            agent.follow(flowField);
        }
    }
    
    /**
     * FlowField 类封装了流场算法
     * 你可以将这个类直接复制到你的游戏代码中使用
     */
    public static class FlowField {
        private int gridSize;
        private int cellSize;
        private int targetX, targetY;
        private int[][] costField;
        private int[][] integrationField;
        private Vector2D[][] flowField;
        private boolean[][] obstacles;
        
        public FlowField(int gridSize, int cellSize) {
            this.gridSize = gridSize;
            this.cellSize = cellSize;
            this.costField = new int[gridSize][gridSize];
            this.integrationField = new int[gridSize][gridSize];
            this.flowField = new Vector2D[gridSize][gridSize];
            this.obstacles = new boolean[gridSize][gridSize];
            
            // 初始化流场
            initialize();
        }
        
        private void initialize() {
            // 初始化成本场（所有单元格成本为1）
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    costField[y][x] = 1;
                    integrationField[y][x] = Integer.MAX_VALUE;
                    flowField[y][x] = new Vector2D(0, 0);
                }
            }
            
            // 设置默认目标
            setTarget(gridSize / 2, gridSize / 2);
            updateFlowField();
        }
        
        public void setTarget(int x, int y) {
            if (isValidCell(x, y)) {
                this.targetX = x;
                this.targetY = y;
            }
        }
        
        public void toggleObstacle(int x, int y) {
            if (isValidCell(x, y)) {
                obstacles[y][x] = !obstacles[y][x];
                costField[y][x] = obstacles[y][x] ? 255 : 1;
            }
        }
        
        public void setObstacle(int x, int y, boolean isObstacle) {
            if (isValidCell(x, y)) {
                obstacles[y][x] = isObstacle;
                costField[y][x] = isObstacle ? 255 : 1;
            }
        }
        
        public void updateFlowField() {
            calculateIntegrationField();
            calculateFlowField();
        }
        
        private void calculateIntegrationField() {
            // 重置集成场
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    integrationField[y][x] = Integer.MAX_VALUE;
                }
            }
            
            // 使用Dijkstra算法计算集成场
            PriorityQueue<Cell> queue = new PriorityQueue<>();
            integrationField[targetY][targetX] = 0;
            queue.add(new Cell(targetX, targetY, 0));
            
            while (!queue.isEmpty()) {
                Cell current = queue.poll();
                
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        
                        int nx = current.x + dx;
                        int ny = current.y + dy;
                        
                        if (isValidCell(nx, ny) && !obstacles[ny][nx]) {
                            // 对角线移动成本更高（约1.4倍）
                            int moveCost = (dx != 0 && dy != 0) ? 
                                (int)(costField[ny][nx] * 1.4) : costField[ny][nx];
                            
                            int newCost = integrationField[current.y][current.x] + moveCost;
                            
                            if (newCost < integrationField[ny][nx]) {
                                integrationField[ny][nx] = newCost;
                                queue.add(new Cell(nx, ny, newCost));
                            }
                        }
                    }
                }
            }
        }
        
        private void calculateFlowField() {
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    if (obstacles[y][x] || integrationField[y][x] == Integer.MAX_VALUE) {
                        flowField[y][x] = new Vector2D(0, 0);
                        continue;
                    }
                    
                    int bestDX = 0, bestDY = 0;
                    int bestCost = Integer.MAX_VALUE;
                    
                    // 检查所有8个方向
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            
                            int nx = x + dx;
                            int ny = y + dy;
                            
                            if (isValidCell(nx, ny) && integrationField[ny][nx] < bestCost) {
                                bestCost = integrationField[ny][nx];
                                bestDX = dx;
                                bestDY = dy;
                            }
                        }
                    }
                    
                    flowField[y][x] = new Vector2D(bestDX, bestDY).normalize();
                }
            }
        }
        
        public Vector2D getVector(int x, int y) {
            if (isValidCell(x, y)) {
                return flowField[y][x];
            }
            return new Vector2D(0, 0);
        }
        
        public Vector2D getVectorAtWorldPos(double worldX, double worldY) {
            int x = (int) (worldX / cellSize);
            int y = (int) (worldY / cellSize);
            return getVector(x, y);
        }
        
        private boolean isValidCell(int x, int y) {
            return x >= 0 && x < gridSize && y >= 0 && y < gridSize;
        }
        
        public void draw(GraphicsContext gc) {
            // 绘制集成场（背景色）
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    if (obstacles[y][x]) {
                        gc.setFill(Color.BLACK);
                        gc.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                    } else {
                        // 根据集成场值设置颜色
                        double intensity = 1.0 - Math.min(integrationField[y][x] / 50.0, 1.0);
                        gc.setFill(Color.color(intensity, intensity, 1.0));
                        gc.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                    }
                }
            }
            
            // 绘制流场方向（箭头）
            gc.setStroke(Color.RED);
            gc.setLineWidth(1);
            
            int arrowSpacing = 1; // 每格都绘制箭头
            for (int y = 0; y < gridSize; y += arrowSpacing) {
                for (int x = 0; x < gridSize; x += arrowSpacing) {
                    if (!obstacles[y][x] && integrationField[y][x] != Integer.MAX_VALUE) {
                        Vector2D vec = flowField[y][x];
                        if (vec.length() > 0) {
                            double centerX = x * cellSize + cellSize / 2;
                            double centerY = y * cellSize + cellSize / 2;
                            
                            double endX = centerX + vec.x * cellSize * 0.4;
                            double endY = centerY + vec.y * cellSize * 0.4;
                            
                            gc.strokeLine(centerX, centerY, endX, endY);
                            
                            // 绘制箭头头部
                            double angle = Math.atan2(vec.y, vec.x);
                            double arrowLength = cellSize * 0.2;
                            
                            double arrowX1 = endX - arrowLength * Math.cos(angle - Math.PI / 6);
                            double arrowY1 = endY - arrowLength * Math.sin(angle - Math.PI / 6);
                            double arrowX2 = endX - arrowLength * Math.cos(angle + Math.PI / 6);
                            double arrowY2 = endY - arrowLength * Math.sin(angle + Math.PI / 6);
                            
                            gc.strokeLine(endX, endY, arrowX1, arrowY1);
                            gc.strokeLine(endX, endY, arrowX2, arrowY2);
                        }
                    }
                }
            }
            
            // 绘制目标点
            gc.setFill(Color.GREEN);
            gc.fillRect(targetX * cellSize, targetY * cellSize, cellSize, cellSize);
        }
    }
    
    /**
     * 代理类，代表游戏中的移动单位
     */
    public static class Agent {
        private double x, y;
        private int cellSize;
        private double speed;
        
        public Agent(int gridX, int gridY, int cellSize) {
            this.x = gridX * cellSize + cellSize / 2;
            this.y = gridY * cellSize + cellSize / 2;
            this.cellSize = cellSize;
            this.speed = 2.0;
        }
        
        public void follow(FlowField flowField) {
            Vector2D direction = flowField.getVectorAtWorldPos(x, y);
            
            if (direction.length() > 0) {
                x += direction.x * speed;
                y += direction.y * speed;
            }
        }
        
        public void draw(GraphicsContext gc) {
            gc.setFill(Color.BLUE);
            gc.fillOval(x - 5, y - 5, 10, 10);
        }
    }
    
    /**
     * 二维向量类
     */
    public static class Vector2D {
        public double x, y;
        
        public Vector2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double length() {
            return Math.sqrt(x * x + y * y);
        }
        
        public Vector2D normalize() {
            double len = length();
            if (len > 0) {
                return new Vector2D(x / len, y / len);
            }
            return new Vector2D(0, 0);
        }
    }
    
    /**
     * 用于优先级队列的单元格类
     */
    private static class Cell implements Comparable<Cell> {
        public int x, y;
        public int cost;
        
        public Cell(int x, int y, int cost) {
            this.x = x;
            this.y = y;
            this.cost = cost;
        }
        
        @Override
        public int compareTo(Cell other) {
            return Integer.compare(this.cost, other.cost);
        }
    }
}