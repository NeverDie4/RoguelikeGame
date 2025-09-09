package com.roguelike.map;

import com.almasb.fxgl.entity.level.Level;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.almasb.fxgl.app.scene.GameView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;
import java.util.logging.Logger;

/**
 * Tiled地图渲染器：解析TMX文件并创建对应的视觉表示。
 * 支持JDK21和JavaFX21，集成PNG瓦片集支持。
 */
public class MapRenderer {

    private static final Logger logger = Logger.getLogger(MapRenderer.class.getName());

    private Level currentLevel;
    private String mapPath;
    private GameView mapView;
    private boolean useFallbackBackground = false;
    
    // TMX文件解析相关
    private TiledMap tiledMap;
    private Map<String, Image> tilesetImages = new HashMap<>();

    public MapRenderer() {
        this.mapPath = "grass.tmx";
        this.tiledMap = new TiledMap();
    }

    public MapRenderer(String mapPath) {
        this.mapPath = mapPath;
        this.tiledMap = new TiledMap();
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
        System.out.println("地图尺寸: " + tiledMap.getWidth() + "x" + tiledMap.getHeight());
        System.out.println("瓦片尺寸: " + tiledMap.getTilewidth() + "x" + tiledMap.getTileheight());
    }

    /**
     * 解析TMX文件
     */
    private boolean parseTMXFile() {
        try {
            // 从固定的assets/maps目录加载TMX文件
            String resourcePath = "assets/maps/" + mapPath;
            InputStream inputStream = getClass().getResourceAsStream("/" + resourcePath);

            if (inputStream == null) {
                System.err.println("❌ 无法找到TMX文件: /" + resourcePath);
                return false;
            }
            
            System.out.println("✅ 找到TMX文件: /" + resourcePath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            
            Element mapElement = document.getDocumentElement();
            
            // 解析地图属性
            tiledMap.setWidth(Integer.parseInt(mapElement.getAttribute("width")));
            tiledMap.setHeight(Integer.parseInt(mapElement.getAttribute("height")));
            tiledMap.setTilewidth(Integer.parseInt(mapElement.getAttribute("tilewidth")));
            tiledMap.setTileheight(Integer.parseInt(mapElement.getAttribute("tileheight")));

            System.out.println("📊 地图信息: " + tiledMap.getWidth() + "x" + tiledMap.getHeight() +
                             ", 瓦片: " + tiledMap.getTilewidth() + "x" + tiledMap.getTileheight());

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
            
            Tileset tileset = new Tileset();
            tileset.setFirstgid(Integer.parseInt(tilesetElement.getAttribute("firstgid")));
            tileset.setName(tilesetElement.getAttribute("name"));
            tileset.setTilewidth(Integer.parseInt(tilesetElement.getAttribute("tilewidth")));
            tileset.setTileheight(Integer.parseInt(tilesetElement.getAttribute("tileheight")));
            tileset.setTilecount(Integer.parseInt(tilesetElement.getAttribute("tilecount")));
            tileset.setColumns(Integer.parseInt(tilesetElement.getAttribute("columns")));

            // 解析图像信息
            Element imageElement = (Element) tilesetElement.getElementsByTagName("image").item(0);
            if (imageElement != null) {
                tileset.setImage(imageElement.getAttribute("source"));
                tileset.setImagewidth(Integer.parseInt(imageElement.getAttribute("width")));
                tileset.setImageheight(Integer.parseInt(imageElement.getAttribute("height")));

                // 加载瓦片集图像
                loadTilesetImage(tileset);
            }

            tiledMap.getTilesets().add(tileset);

            System.out.println("🎨 瓦片集: " + tileset.getName() + " (GID: " + tileset.getFirstgid() +
                             "-" + (tileset.getFirstgid() + tileset.getTilecount() - 1) +
                             ", 图像: " + tileset.getImage() + ")");
        }
    }

    /**
     * 加载瓦片集图像
     * 根据tmx文件名构建图片路径：如果tmx为a.tmx，则图片为a1.png, a2.png等
     */
    private void loadTilesetImage(Tileset tileset) {
        try {
            // 从tmx文件名中提取基础名称（去掉.tmx扩展名）
            String baseName = mapPath;
            if (baseName.endsWith(".tmx")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }

            // 根据瓦片集名称构建图片文件名
            // 如果瓦片集名称为"2dmap"，则图片为baseName + "1.png"
            // 如果瓦片集名称为"2dmap2"，则图片为baseName + "2.png"
            String imageFileName = buildImageFileName(baseName, tileset.getName());

            // 从固定的assets/maps目录加载图片文件
            String imagePath = "assets/maps/" + imageFileName;
            InputStream imageStream = getClass().getResourceAsStream("/" + imagePath);

            if (imageStream != null) {
                Image image = new Image(imageStream);
                tilesetImages.put(tileset.getName(), image);
                logger.info("🖼️ 成功加载瓦片集图像: /" + imagePath + " (原始路径: " + tileset.getImage() + ")");
                System.out.println("🖼️ 成功加载瓦片集图像: /" + imagePath + " (原始路径: " + tileset.getImage() + ")");
            } else {
                logger.warning("❌ 无法加载瓦片集图像: /" + imagePath + " (原始路径: " + tileset.getImage() + ")");
                System.err.println("❌ 无法加载瓦片集图像: /" + imagePath + " (原始路径: " + tileset.getImage() + ")");
            }
        } catch (Exception e) {
            System.err.println("❌ 加载瓦片集图像失败: " + e.getMessage());
        }
    }

    /**
     * 根据tmx基础名称和瓦片集名称构建图片文件名
     * 例如：baseName="grass", tilesetName="2dmap" -> "grass1.png"
     *      baseName="grass", tilesetName="2dmap2" -> "grass2.png"
     */
    private String buildImageFileName(String baseName, String tilesetName) {
        // 从瓦片集名称中提取数字后缀
        String suffix = "1"; // 默认后缀

        if (tilesetName.endsWith("2")) {
            suffix = "2";
        } else if (tilesetName.endsWith("3")) {
            suffix = "3";
        } else if (tilesetName.endsWith("4")) {
            suffix = "4";
        } else if (tilesetName.endsWith("5")) {
            suffix = "5";
        } else if (tilesetName.endsWith("6")) {
            suffix = "6";
        } else if (tilesetName.endsWith("7")) {
            suffix = "7";
        } else if (tilesetName.endsWith("8")) {
            suffix = "8";
        } else if (tilesetName.endsWith("9")) {
            suffix = "9";
        }

        String imageFileName = baseName + suffix + ".png";
        logger.info("🔧 构建图片文件名: baseName=" + baseName + ", tilesetName=" + tilesetName + " -> " + imageFileName);
        System.out.println("🔧 构建图片文件名: baseName=" + baseName + ", tilesetName=" + tilesetName + " -> " + imageFileName);

        return imageFileName;
    }
    
    /**
     * 解析瓦片层
     */
    private void parseTileLayers(Element mapElement) {
        NodeList layerNodes = mapElement.getElementsByTagName("layer");
        for (int i = 0; i < layerNodes.getLength(); i++) {
            Element layerElement = (Element) layerNodes.item(i);
            
            Layer layer = new Layer();
            layer.setName(layerElement.getAttribute("name"));
            layer.setWidth(Integer.parseInt(layerElement.getAttribute("width")));
            layer.setHeight(Integer.parseInt(layerElement.getAttribute("height")));

            // 解析瓦片数据
            Element dataElement = (Element) layerElement.getElementsByTagName("data").item(0);
            String dataText = dataElement.getTextContent().trim();
            
            // 解析CSV格式的瓦片数据
            String[] dataStrings = dataText.split(",");
            List<Integer> data = new ArrayList<>();
            for (String dataString : dataStrings) {
                data.add(Integer.parseInt(dataString.trim()));
            }
            layer.setData(data);

            tiledMap.getLayers().add(layer);

            System.out.println("🗺️ 瓦片层: " + layer.getName() + " (" + layer.getWidth() + "x" + layer.getHeight() + ")");
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
        for (Layer tileLayer : tiledMap.getLayers()) {
            System.out.println("创建瓦片层: " + tileLayer.getName());

            for (int y = 0; y < tileLayer.getHeight(); y++) {
                for (int x = 0; x < tileLayer.getWidth(); x++) {
                    int index = y * tileLayer.getWidth() + x;
                    int gid = tileLayer.getData().get(index);

                    if (gid > 0) {
                        // 找到对应的瓦片集
                        Tileset tileset = findTilesetForGid(gid);
                        if (tileset != null) {
                            // 尝试使用PNG瓦片集，如果失败则使用颜色
                            if (tilesetImages.containsKey(tileset.getName())) {
                                createTileFromImage(layer, x, y, gid, tileset);
                            } else {
                                createTileFromColor(layer, x, y, gid, tileset);
                            }
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
     * 从图像创建瓦片
     */
    private void createTileFromImage(Group layer, int x, int y, int gid, Tileset tileset) {
        try {
            Image tilesetImage = tilesetImages.get(tileset.getName());
            int localId = gid - tileset.getFirstgid();

            // 计算瓦片在瓦片集中的位置
            int tileX = (localId % tileset.getColumns()) * tileset.getTilewidth();
            int tileY = (localId / tileset.getColumns()) * tileset.getTileheight();

            // 创建ImageView显示瓦片
            ImageView tileView = new ImageView(tilesetImage);
            tileView.setViewport(new javafx.geometry.Rectangle2D(tileX, tileY,
                                                               tileset.getTilewidth(),
                                                               tileset.getTileheight()));
            tileView.setTranslateX(x * tiledMap.getTilewidth());
            tileView.setTranslateY(y * tiledMap.getTileheight());

            layer.getChildren().add(tileView);
        } catch (Exception e) {
            System.err.println("创建图像瓦片失败: " + e.getMessage());
            // 回退到颜色瓦片
            createTileFromColor(layer, x, y, gid, tileset);
        }
    }

    /**
     * 从颜色创建瓦片
     */
    private void createTileFromColor(Group layer, int x, int y, int gid, Tileset tileset) {
        int localId = gid - tileset.getFirstgid();
        Color tileColor = getTileColor(localId, tileset);

        Rectangle tile = new Rectangle(tiledMap.getTilewidth(), tiledMap.getTileheight());
        tile.setFill(tileColor);
        tile.setStroke(Color.web("#2a5c39"));
        tile.setStrokeWidth(0.5);
        tile.setTranslateX(x * tiledMap.getTilewidth());
        tile.setTranslateY(y * tiledMap.getTileheight());
        
        layer.getChildren().add(tile);
    }
    
    /**
     * 根据GID找到对应的瓦片集
     */
    private Tileset findTilesetForGid(int gid) {
        for (Tileset tileset : tiledMap.getTilesets()) {
            if (gid >= tileset.getFirstgid() && gid < tileset.getFirstgid() + tileset.getTilecount()) {
                return tileset;
            }
        }
        return null;
    }
    
    /**
     * 根据瓦片ID和瓦片集获取颜色
     */
    private Color getTileColor(int localId, Tileset tileset) {
        // 根据瓦片集名称和瓦片ID生成不同的颜色
        switch (tileset.getName()) {
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
                Rectangle r = new Rectangle(32, 32);

                // 使用伪随机算法生成多样化的草地图案
                int colorIndex = (x * 7 + y * 11) % grassColors.length;
                r.setFill(grassColors[colorIndex]);
                
                // 添加细微的边框
                r.setStroke(Color.web("#2a5c39"));
                r.setStrokeWidth(0.5);
                
                r.setTranslateX(x * 32);
                r.setTranslateY(y * 32);
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

    /**
     * 检查指定位置是否可通行
     */
    public boolean isPassable(int x, int y) {
        // 这里可以根据瓦片属性判断是否可通行
        // 目前简单返回true，您可以根据需要扩展
        return true;
    }

    /**
     * 获取地图尺寸
     */
    public int getMapWidth() {
        return tiledMap.getWidth();
    }

    public int getMapHeight() {
        return tiledMap.getHeight();
    }

    public int getTileWidth() {
        return tiledMap.getTilewidth();
    }

    public int getTileHeight() {
        return tiledMap.getTileheight();
    }

    /**
     * 设置瓦片尺寸
     */
    public void setTileSize(int width, int height) {
        // 这个方法可以用于动态调整瓦片尺寸
        // 目前瓦片尺寸从TMX文件读取
    }
}