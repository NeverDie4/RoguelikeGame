package com.roguelike.map;

import com.almasb.fxgl.entity.level.Level;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import com.almasb.fxgl.app.scene.GameView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * Tiled地图渲染器：解析TMX文件并创建对应的视觉表示。
 * 支持JDK21和JavaFX21，不依赖外部图像文件。
 */
public class MapRenderer {

    private Level currentLevel;
    private String mapPath;
    private int tileWidth = 32;  // 默认瓦片宽度
    private int tileHeight = 32; // 默认瓦片高度
    private GameView mapView;
    private boolean useFallbackBackground = false;
    
    // TMX文件解析相关
    private int mapWidth = 30;
    private int mapHeight = 30;
    private List<TileLayer> tileLayers = new ArrayList<>();
    private List<Tileset> tilesets = new ArrayList<>();
    
    // 瓦片层数据结构
    private static class TileLayer {
        String name;
        int width;
        int height;
        int[] data;
        
        TileLayer(String name, int width, int height, int[] data) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.data = data;
        }
    }
    
    // 瓦片集数据结构
    private static class Tileset {
        String name;
        int firstGid;
        int tileWidth;
        int tileHeight;
        int tileCount;
        int columns;
        
        Tileset(String name, int firstGid, int tileWidth, int tileHeight, int tileCount, int columns) {
            this.name = name;
            this.firstGid = firstGid;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.tileCount = tileCount;
            this.columns = columns;
        }
    }

    public MapRenderer() {
        this.mapPath = "grass.tmx";
    }

    public MapRenderer(String mapPath) {
        this.mapPath = mapPath;
    }

    public void init() {
        System.out.println("🎮 初始化地图渲染器");
        System.out.println("📁 尝试解析TMX文件: " + mapPath);
        
        try {
            // 尝试解析TMX文件
            if (parseTMXFile()) {
                System.out.println("✅ TMX文件解析成功");
                createMapFromTMX();
            } else {
                throw new Exception("TMX文件解析失败");
            }
        } catch (Exception e) {
            System.err.println("❌ TMX文件解析失败: " + e.getMessage());
            System.out.println("🔄 使用回退背景方案");
            initFallbackBackground();
        }
        
        System.out.println("✅ 地图渲染器初始化完成");
        System.out.println("地图尺寸: " + mapWidth + "x" + mapHeight);
        System.out.println("瓦片尺寸: " + tileWidth + "x" + tileHeight);
    }

    /**
     * 解析TMX文件
     */
    private boolean parseTMXFile() {
        try {
            // 从资源文件加载TMX文件
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("assets/levels/" + mapPath);
            if (inputStream == null) {
                System.err.println("无法找到TMX文件: assets/levels/" + mapPath);
                return false;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            
            Element mapElement = document.getDocumentElement();
            
            // 解析地图属性
            mapWidth = Integer.parseInt(mapElement.getAttribute("width"));
            mapHeight = Integer.parseInt(mapElement.getAttribute("height"));
            tileWidth = Integer.parseInt(mapElement.getAttribute("tilewidth"));
            tileHeight = Integer.parseInt(mapElement.getAttribute("tileheight"));
            
            System.out.println("📊 地图信息: " + mapWidth + "x" + mapHeight + ", 瓦片: " + tileWidth + "x" + tileHeight);
            
            // 解析瓦片集
            parseTilesets(mapElement);
            
            // 解析瓦片层
            parseTileLayers(mapElement);
            
            inputStream.close();
            return true;
            
        } catch (Exception e) {
            System.err.println("TMX文件解析错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 解析瓦片集
     */
    private void parseTilesets(Element mapElement) {
        NodeList tilesetNodes = mapElement.getElementsByTagName("tileset");
        for (int i = 0; i < tilesetNodes.getLength(); i++) {
            Element tilesetElement = (Element) tilesetNodes.item(i);
            
            String name = tilesetElement.getAttribute("name");
            int firstGid = Integer.parseInt(tilesetElement.getAttribute("firstgid"));
            int tileWidth = Integer.parseInt(tilesetElement.getAttribute("tilewidth"));
            int tileHeight = Integer.parseInt(tilesetElement.getAttribute("tileheight"));
            int tileCount = Integer.parseInt(tilesetElement.getAttribute("tilecount"));
            int columns = Integer.parseInt(tilesetElement.getAttribute("columns"));
            
            Tileset tileset = new Tileset(name, firstGid, tileWidth, tileHeight, tileCount, columns);
            tilesets.add(tileset);
            
            System.out.println("🎨 瓦片集: " + name + " (GID: " + firstGid + "-" + (firstGid + tileCount - 1) + ")");
        }
    }
    
    /**
     * 解析瓦片层
     */
    private void parseTileLayers(Element mapElement) {
        NodeList layerNodes = mapElement.getElementsByTagName("layer");
        for (int i = 0; i < layerNodes.getLength(); i++) {
            Element layerElement = (Element) layerNodes.item(i);
            
            String name = layerElement.getAttribute("name");
            int width = Integer.parseInt(layerElement.getAttribute("width"));
            int height = Integer.parseInt(layerElement.getAttribute("height"));
            
            // 解析瓦片数据
            Element dataElement = (Element) layerElement.getElementsByTagName("data").item(0);
            String dataText = dataElement.getTextContent().trim();
            
            // 解析CSV格式的瓦片数据
            String[] dataStrings = dataText.split(",");
            int[] data = new int[dataStrings.length];
            for (int j = 0; j < dataStrings.length; j++) {
                data[j] = Integer.parseInt(dataStrings[j].trim());
            }
            
            TileLayer layer = new TileLayer(name, width, height, data);
            tileLayers.add(layer);
            
            System.out.println("🗺️ 瓦片层: " + name + " (" + width + "x" + height + ")");
        }
    }
    
    /**
     * 根据TMX数据创建地图
     */
    private void createMapFromTMX() {
        useFallbackBackground = false;
        System.out.println("🎨 根据TMX数据创建地图");
        
        Group layer = new Group();
        
        // 为每个瓦片层创建瓦片
        for (TileLayer tileLayer : tileLayers) {
            System.out.println("创建瓦片层: " + tileLayer.name);
            
            for (int y = 0; y < tileLayer.height; y++) {
                for (int x = 0; x < tileLayer.width; x++) {
                    int index = y * tileLayer.width + x;
                    int gid = tileLayer.data[index];
                    
                    if (gid > 0) {
                        // 找到对应的瓦片集
                        Tileset tileset = findTilesetForGid(gid);
                        if (tileset != null) {
                            Color tileColor = getTileColor(gid, tileset);
                            
                            Rectangle tile = new Rectangle(tileWidth, tileHeight);
                            tile.setFill(tileColor);
                            tile.setStroke(Color.web("#2a5c39"));
                            tile.setStrokeWidth(0.5);
                            tile.setTranslateX(x * tileWidth);
                            tile.setTranslateY(y * tileHeight);
                            
                            layer.getChildren().add(tile);
                        }
                    }
                }
            }
        }
        
        mapView = new GameView(layer, 0);
        getGameScene().addGameView(mapView);
        System.out.println("✅ TMX地图创建完成");
    }
    
    /**
     * 根据GID找到对应的瓦片集
     */
    private Tileset findTilesetForGid(int gid) {
        for (Tileset tileset : tilesets) {
            if (gid >= tileset.firstGid && gid < tileset.firstGid + tileset.tileCount) {
                return tileset;
            }
        }
        return null;
    }
    
    /**
     * 根据瓦片ID和瓦片集获取颜色
     */
    private Color getTileColor(int gid, Tileset tileset) {
        // 根据瓦片集名称和瓦片ID生成不同的颜色
        int localId = gid - tileset.firstGid;
        
        // 为不同的瓦片集使用不同的颜色方案
        switch (tileset.name) {
            case "2dmap":
                return getGrassColor(localId);
            case "2dmap2":
                return getStoneColor(localId);
            case "2dmap3":
                return getWaterColor(localId);
            default:
                return getDefaultColor(localId);
        }
    }
    
    /**
     * 草地颜色方案
     */
    private Color getGrassColor(int localId) {
        Color[] grassColors = {
            Color.web("#4a7c59"), // 深绿
            Color.web("#5a8c69"), // 中绿
            Color.web("#6a9c79"), // 浅绿
            Color.web("#3a6c49"), // 暗绿
            Color.web("#7aac89")  // 亮绿
        };
        return grassColors[localId % grassColors.length];
    }
    
    /**
     * 石头颜色方案
     */
    private Color getStoneColor(int localId) {
        Color[] stoneColors = {
            Color.web("#8a8a8a"), // 深灰
            Color.web("#9a9a9a"), // 中灰
            Color.web("#aaaaaa"), // 浅灰
            Color.web("#7a7a7a"), // 暗灰
            Color.web("#bababa")  // 亮灰
        };
        return stoneColors[localId % stoneColors.length];
    }
    
    /**
     * 水颜色方案
     */
    private Color getWaterColor(int localId) {
        Color[] waterColors = {
            Color.web("#4a7c9a"), // 深蓝
            Color.web("#5a8caa"), // 中蓝
            Color.web("#6a9cba"), // 浅蓝
            Color.web("#3a6c8a"), // 暗蓝
            Color.web("#7aacca")  // 亮蓝
        };
        return waterColors[localId % waterColors.length];
    }
    
    /**
     * 默认颜色方案
     */
    private Color getDefaultColor(int localId) {
        Color[] defaultColors = {
            Color.web("#6a6a6a"), // 中性灰
            Color.web("#7a7a7a"), // 中灰
            Color.web("#8a8a8a"), // 浅灰
            Color.web("#5a5a5a"), // 暗灰
            Color.web("#9a9a9a")  // 亮灰
        };
        return defaultColors[localId % defaultColors.length];
    }

    /**
     * 回退方案：如果Tiled地图加载失败，使用简单的网格背景
     */
    private void initFallbackBackground() {
        useFallbackBackground = true;
        System.out.println("🎨 创建优化的草地背景");
        
        Group layer = new Group();
        
        // 生成一个多样化的草地背景，模拟grass.tmx的尺寸
        int cols = 30; // 根据grass.tmx的宽度
        int rows = 30; // 根据grass.tmx的高度
        
        // 定义多种草地颜色，增加视觉多样性
        Color[] grassColors = {
            Color.web("#4a7c59"), // 深绿
            Color.web("#5a8c69"), // 中绿
            Color.web("#6a9c79"), // 浅绿
            Color.web("#3a6c49"), // 暗绿
            Color.web("#7aac89")  // 亮绿
        };
        
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Rectangle r = new Rectangle(tileWidth, tileHeight);
                
                // 使用伪随机算法生成多样化的草地图案
                int colorIndex = (x * 7 + y * 11) % grassColors.length;
                r.setFill(grassColors[colorIndex]);
                
                // 添加细微的边框
                r.setStroke(Color.web("#2a5c39"));
                r.setStrokeWidth(0.5);
                
                r.setTranslateX(x * tileWidth);
                r.setTranslateY(y * tileHeight);
                layer.getChildren().add(r);
            }
        }
        
        mapView = new GameView(layer, 0);
        getGameScene().addGameView(mapView);
        System.out.println("✅ 优化的草地背景创建完成");
    }

    public void onUpdate(double tpf) {
        // 视口跟随已在 GameApp 中绑定
        // Tiled地图的更新由FXGL自动处理
    }

    public void render(Object graphics) {
        // FXGL 使用场景图渲染，这里保留接口
    }

    /**
     * 获取当前地图的关卡对象
     */
    public Level getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 检查指定坐标是否可通行
     * @param x 世界坐标X
     * @param y 世界坐标Y
     * @return 是否可通行
     */
    public boolean isPassable(double x, double y) {
        if (currentLevel == null) {
            return true; // 如果没有地图，默认可通行
        }
        
        // 将世界坐标转换为瓦片坐标
        int tileX = (int) (x / tileWidth);
        int tileY = (int) (y / tileHeight);
        
        // 检查边界
        if (tileX < 0 || tileY < 0 || tileX >= currentLevel.getWidth() || tileY >= currentLevel.getHeight()) {
            return false;
        }
        
        // 这里可以根据瓦片属性判断是否可通行
        // 目前简单返回true，您可以根据需要扩展
        return true;
    }

    /**
     * 获取地图尺寸
     */
    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    /**
     * 设置瓦片尺寸
     */
    public void setTileSize(int width, int height) {
        this.tileWidth = width;
        this.tileHeight = height;
    }
}


