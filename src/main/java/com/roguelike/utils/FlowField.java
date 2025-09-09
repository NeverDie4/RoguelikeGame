package com.roguelike.utils;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.*;

/**
 * 流场寻路算法核心类
 * 从 FlowFieldPathfinding.java 中提取，用于游戏中的敌人AI寻路
 */
public class FlowField {
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
    
    public void setTargetFromWorldPos(double worldX, double worldY) {
        int x = (int) (worldX / cellSize);
        int y = (int) (worldY / cellSize);
        setTarget(x, y);
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
    
    public void setObstacleFromWorldPos(double worldX, double worldY, boolean isObstacle) {
        int x = (int) (worldX / cellSize);
        int y = (int) (worldY / cellSize);
        setObstacle(x, y, isObstacle);
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
                
                // 使用梯度计算获得更平滑的方向
                Vector2D gradient = calculateGradient(x, y);
                flowField[y][x] = gradient.normalize();
            }
        }
    }
    
    private Vector2D calculateGradient(int x, int y) {
        // 计算集成场的梯度
        double dx = 0, dy = 0;
        
        // 使用中心差分法计算梯度
        if (x > 0 && x < gridSize - 1) {
            dx = (integrationField[y][x + 1] - integrationField[y][x - 1]) / 2.0;
        } else if (x == 0) {
            dx = integrationField[y][x + 1] - integrationField[y][x];
        } else if (x == gridSize - 1) {
            dx = integrationField[y][x] - integrationField[y][x - 1];
        }
        
        if (y > 0 && y < gridSize - 1) {
            dy = (integrationField[y + 1][x] - integrationField[y - 1][x]) / 2.0;
        } else if (y == 0) {
            dy = integrationField[y + 1][x] - integrationField[y][x];
        } else if (y == gridSize - 1) {
            dy = integrationField[y][x] - integrationField[y - 1][x];
        }
        
        // 梯度指向成本增加最快的方向，我们需要相反方向
        return new Vector2D(-dx, -dy);
    }
    
    public Vector2D getVector(int x, int y) {
        if (isValidCell(x, y)) {
            return flowField[y][x];
        }
        return new Vector2D(0, 0);
    }
    
    public Vector2D getVectorAtWorldPos(double worldX, double worldY) {
        // 使用双线性插值获得更平滑的向量
        return getInterpolatedVector(worldX, worldY);
    }
    
    private Vector2D getInterpolatedVector(double worldX, double worldY) {
        // 计算精确的网格坐标
        double gridX = worldX / cellSize;
        double gridY = worldY / cellSize;
        
        // 获取四个相邻格子的坐标
        int x1 = (int) Math.floor(gridX);
        int y1 = (int) Math.floor(gridY);
        int x2 = x1 + 1;
        int y2 = y1 + 1;
        
        // 计算插值权重
        double fx = gridX - x1;
        double fy = gridY - y1;
        
        // 获取四个角点的向量
        Vector2D v11 = getVector(x1, y1);
        Vector2D v12 = getVector(x1, y2);
        Vector2D v21 = getVector(x2, y1);
        Vector2D v22 = getVector(x2, y2);
        
        // 双线性插值
        Vector2D v1 = interpolateVectors(v11, v12, fy);
        Vector2D v2 = interpolateVectors(v21, v22, fy);
        Vector2D result = interpolateVectors(v1, v2, fx);
        
        return result.normalize();
    }
    
    private Vector2D interpolateVectors(Vector2D v1, Vector2D v2, double t) {
        // 向量插值，考虑向量的方向性
        if (v1.length() == 0 && v2.length() == 0) {
            return new Vector2D(0, 0);
        }
        
        // 使用球面线性插值（SLERP）的简化版本
        double x = v1.x + t * (v2.x - v1.x);
        double y = v1.y + t * (v2.y - v1.y);
        
        return new Vector2D(x, y);
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
        
        int arrowSpacing = 2; // 减少箭头密度以提高性能
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
        
        public Vector2D multiply(double scalar) {
            return new Vector2D(x * scalar, y * scalar);
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
