package com.roguelike.utils;

import java.util.*;

/**
 * A*寻路算法实现类
 * 用于在敌人数量较少时提供流畅的寻路功能
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class AStarPathfinder {
    
    /**
     * 寻路节点类
     */
    public static class Node {
        public int x, y;
        public double gCost; // 从起点到当前节点的实际代价
        public double hCost; // 从当前节点到终点的启发式代价
        public double fCost; // 总代价 (gCost + hCost)
        public Node parent; // 父节点，用于路径重构
        
        public Node(int x, int y) {
            this.x = x;
            this.y = y;
            this.gCost = 0;
            this.hCost = 0;
            this.fCost = 0;
            this.parent = null;
        }
        
        /**
         * 计算总代价
         */
        public void calculateFCost() {
            this.fCost = this.gCost + this.hCost;
        }
        
        /**
         * 计算到另一个节点的欧几里得距离
         */
        public double distanceTo(Node other) {
            int dx = this.x - other.x;
            int dy = this.y - other.y;
            return Math.sqrt(dx * dx + dy * dy);
        }
        
        /**
         * 计算到另一个节点的曼哈顿距离
         */
        public double manhattanDistanceTo(Node other) {
            return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Node node = (Node) obj;
            return x == node.x && y == node.y;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
        
        @Override
        public String toString() {
            return "Node(" + x + ", " + y + ")";
        }
    }
    
    /**
     * 地图接口，用于定义地图的障碍物检查
     */
    public interface MapInterface {
        /**
         * 检查指定位置是否为可通行区域
         * @param x X坐标
         * @param y Y坐标
         * @return true表示可通行，false表示有障碍物
         */
        boolean isWalkable(int x, int y);
        
        /**
         * 获取地图宽度
         * @return 地图宽度
         */
        int getMapWidth();
        
        /**
         * 获取地图高度
         * @return 地图高度
         */
        int getMapHeight();
    }
    
    private final MapInterface map;
    private final boolean allowDiagonal; // 是否允许对角线移动
    private final double diagonalCost; // 对角线移动的代价倍数
    
    /**
     * 构造函数
     * @param map 地图接口实现
     * @param allowDiagonal 是否允许对角线移动
     */
    public AStarPathfinder(MapInterface map, boolean allowDiagonal) {
        this.map = map;
        this.allowDiagonal = allowDiagonal;
        this.diagonalCost = Math.sqrt(2); // 对角线移动代价约为1.414
    }
    
    /**
     * 构造函数，默认允许对角线移动
     * @param map 地图接口实现
     */
    public AStarPathfinder(MapInterface map) {
        this(map, true);
    }
    
    /**
     * 使用A*算法寻找从起点到终点的路径
     * @param startX 起点X坐标
     * @param startY 起点Y坐标
     * @param endX 终点X坐标
     * @param endY 终点Y坐标
     * @return 路径节点列表，如果找不到路径则返回空列表
     */
    public List<Node> findPath(int startX, int startY, int endX, int endY) {
        // 检查起点和终点是否有效
        if (!isValidPosition(startX, startY) || !isValidPosition(endX, endY)) {
            return new ArrayList<>();
        }
        
        // 如果起点和终点相同，直接返回
        if (startX == endX && startY == endY) {
            List<Node> path = new ArrayList<>();
            path.add(new Node(startX, startY));
            return path;
        }
        
        // 初始化开放列表和关闭列表
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(node -> node.fCost));
        Set<Node> closedList = new HashSet<>();
        
        // 创建起点和终点节点
        Node startNode = new Node(startX, startY);
        Node endNode = new Node(endX, endY);
        
        // 计算起点的启发式代价
        startNode.hCost = calculateHeuristic(startNode, endNode);
        startNode.calculateFCost();
        
        openList.add(startNode);
        
        while (!openList.isEmpty()) {
            // 从开放列表中选择fCost最小的节点
            Node currentNode = openList.poll();
            closedList.add(currentNode);
            
            // 如果到达终点，重构路径
            if (currentNode.equals(endNode)) {
                return reconstructPath(currentNode);
            }
            
            // 检查所有相邻节点
            for (Node neighbor : getNeighbors(currentNode)) {
                // 跳过已经在关闭列表中的节点或不可通行的节点
                if (closedList.contains(neighbor) || !map.isWalkable(neighbor.x, neighbor.y)) {
                    continue;
                }
                
                // 计算从起点到相邻节点的代价
                double tentativeGCost = currentNode.gCost + calculateDistance(currentNode, neighbor);
                
                // 如果相邻节点不在开放列表中，或者找到更好的路径
                if (!openList.contains(neighbor) || tentativeGCost < neighbor.gCost) {
                    neighbor.parent = currentNode;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = calculateHeuristic(neighbor, endNode);
                    neighbor.calculateFCost();
                    
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }
        
        // 没有找到路径
        return new ArrayList<>();
    }
    
    /**
     * 获取指定节点的所有相邻节点
     * @param node 当前节点
     * @return 相邻节点列表
     */
    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        
        // 四个基本方向
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        for (int i = 0; i < 4; i++) {
            int newX = node.x + dx[i];
            int newY = node.y + dy[i];
            
            if (isValidPosition(newX, newY)) {
                neighbors.add(new Node(newX, newY));
            }
        }
        
        // 如果允许对角线移动，添加对角线方向
        if (allowDiagonal) {
            int[] diagonalDx = {-1, -1, 1, 1};
            int[] diagonalDy = {-1, 1, -1, 1};
            
            for (int i = 0; i < 4; i++) {
                int newX = node.x + diagonalDx[i];
                int newY = node.y + diagonalDy[i];
                
                if (isValidPosition(newX, newY)) {
                    neighbors.add(new Node(newX, newY));
                }
            }
        }
        
        return neighbors;
    }
    
    /**
     * 计算两个节点之间的距离
     * @param from 起始节点
     * @param to 目标节点
     * @return 距离
     */
    private double calculateDistance(Node from, Node to) {
        double distance = from.distanceTo(to);
        
        // 如果是对角线移动，应用对角线代价
        if (allowDiagonal && Math.abs(from.x - to.x) == 1 && Math.abs(from.y - to.y) == 1) {
            distance *= diagonalCost;
        }
        
        return distance;
    }
    
    /**
     * 计算启发式代价（使用欧几里得距离）
     * @param from 起始节点
     * @param to 目标节点
     * @return 启发式代价
     */
    private double calculateHeuristic(Node from, Node to) {
        return from.distanceTo(to);
    }
    
    /**
     * 检查位置是否有效
     * @param x X坐标
     * @param y Y坐标
     * @return true表示有效，false表示无效
     */
    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < map.getMapWidth() && y >= 0 && y < map.getMapHeight();
    }
    
    /**
     * 重构路径
     * @param endNode 终点节点
     * @return 从起点到终点的路径
     */
    private List<Node> reconstructPath(Node endNode) {
        List<Node> path = new ArrayList<>();
        Node currentNode = endNode;
        
        while (currentNode != null) {
            path.add(0, currentNode); // 在列表开头插入，确保路径顺序正确
            currentNode = currentNode.parent;
        }
        
        return path;
    }
    
    /**
     * 将路径转换为坐标点列表
     * @param path 节点路径
     * @return 坐标点列表
     */
    public static List<Point2D> pathToPoints(List<Node> path) {
        List<Point2D> points = new ArrayList<>();
        for (Node node : path) {
            points.add(new Point2D(node.x, node.y));
        }
        return points;
    }
    
    /**
     * 简单的2D点类
     */
    public static class Point2D {
        public final double x, y;
        
        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "Point2D(" + x + ", " + y + ")";
        }
    }
    
    /**
     * 获取路径的平滑度评分
     * @param path 路径
     * @return 平滑度评分（0-1，1表示最平滑）
     */
    public static double calculatePathSmoothness(List<Node> path) {
        if (path.size() < 3) {
            return 1.0; // 短路径认为是平滑的
        }
        
        int directionChanges = 0;
        int totalMoves = path.size() - 1;
        
        for (int i = 1; i < path.size() - 1; i++) {
            Node prev = path.get(i - 1);
            Node curr = path.get(i);
            Node next = path.get(i + 1);
            
            // 计算方向向量
            int dx1 = curr.x - prev.x;
            int dy1 = curr.y - prev.y;
            int dx2 = next.x - curr.x;
            int dy2 = next.y - curr.y;
            
            // 如果方向改变，增加方向变化计数
            if (dx1 != dx2 || dy1 != dy2) {
                directionChanges++;
            }
        }
        
        return 1.0 - (double) directionChanges / totalMoves;
    }
    
    /**
     * 优化路径，移除不必要的中间节点
     * @param path 原始路径
     * @return 优化后的路径
     */
    public List<Node> optimizePath(List<Node> path) {
        if (path.size() < 3) {
            return new ArrayList<>(path);
        }
        
        List<Node> optimizedPath = new ArrayList<>();
        optimizedPath.add(path.get(0)); // 添加起点
        
        for (int i = 1; i < path.size() - 1; i++) {
            Node prev = optimizedPath.get(optimizedPath.size() - 1);
            Node curr = path.get(i);
            Node next = path.get(i + 1);
            
            // 如果从prev到next是直线且没有障碍物，跳过curr
            if (!hasObstacleBetween(prev, next)) {
                continue;
            }
            
            optimizedPath.add(curr);
        }
        
        optimizedPath.add(path.get(path.size() - 1)); // 添加终点
        return optimizedPath;
    }
    
    /**
     * 检查两点之间是否有障碍物
     * @param from 起始点
     * @param to 目标点
     * @return true表示有障碍物，false表示无障碍物
     */
    private boolean hasObstacleBetween(Node from, Node to) {
        // 简单的直线检查，可以进一步优化
        int steps = Math.max(Math.abs(to.x - from.x), Math.abs(to.y - from.y));
        
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(from.x + t * (to.x - from.x));
            int y = (int) Math.round(from.y + t * (to.y - from.y));
            
            if (!map.isWalkable(x, y)) {
                return true;
            }
        }
        
        return false;
    }
}
