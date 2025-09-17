package com.roguelike.map;

import com.almasb.fxgl.app.scene.GameView;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

    private String mapName; // 地图名称，不包含.tmx扩展名
    private GameView mapView;

    // TMX文件解析相关
    private TiledMap tiledMap;
    private Map<String, Image> tilesetImages = new HashMap<>();
    private CollisionMap collisionMap;

    public MapRenderer() {
        this.mapName = "grass";
        this.tiledMap = new TiledMap();
    }

    public MapRenderer(String mapName) {
        this.mapName = mapName;
        this.tiledMap = new TiledMap();
    }

    /**
     * 将地图名称映射到实际的目录名称
     * test -> map1, square -> map2, dungeon -> map3
     * 支持特殊地图：test_door -> map1, square_door -> map2, dungeon_door -> map3
     */
    private String getMapDirectoryName(String mapName) {
        try {
            com.roguelike.map.config.MapConfig cfg = com.roguelike.map.config.MapConfigLoader.load();
            if (cfg != null && cfg.maps != null) {
                // 首先查找完全匹配的条目
                for (java.util.Map.Entry<String, com.roguelike.map.config.MapConfig.SingleMapConfig> e : cfg.maps.entrySet()) {
                    com.roguelike.map.config.MapConfig.SingleMapConfig m = e.getValue();
                    if (m != null && m.dimensions != null && m.dimensions.containsKey(mapName)) {
                        // 若 SingleMapConfig 提供 assetsDir，优先用它
                        java.lang.reflect.Field f = null;
                        try {
                            f = com.roguelike.map.config.MapConfig.SingleMapConfig.class.getDeclaredField("assetsDir");
                            f.setAccessible(true);
                            Object v = f.get(m);
                            if (v instanceof String s && !s.isEmpty()) {
                                return s;
                            }
                        } catch (Throwable ignored) {}
                        // 回退：根据基础地图键名
                        return e.getKey();
                    }
                }
                // 回退：若 maps 中有与 mapName 同名的基础键
                if (cfg.maps.containsKey(mapName)) {
                    com.roguelike.map.config.MapConfig.SingleMapConfig m = cfg.maps.get(mapName);
                    try {
                        java.lang.reflect.Field f = com.roguelike.map.config.MapConfig.SingleMapConfig.class.getDeclaredField("assetsDir");
                        f.setAccessible(true);
                        Object v = f.get(m);
                        if (v instanceof String s && !s.isEmpty()) {
                            return s;
                        }
                    } catch (Throwable ignored) {}
                    return mapName;
                }
            }
        } catch (Throwable ignored) {}
        return mapName;
    }

    public void init() {
        System.out.println("🎮 初始化地图渲染器");
        System.out.println("📁 尝试解析地图: " + mapName);

        try {
            // 尝试解析TMX文件
            if (parseTMXFile()) {
                System.out.println("✅ TMX文件解析成功");
                createMapFromTMX();
                
                // 构建碰撞地图
                buildCollisionMap();
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
            // 从assets/maps/{实际目录名}/目录加载TMX文件
            String actualDirName = getMapDirectoryName(mapName);
            String resourcePath = "assets/maps/" + actualDirName + "/" + mapName + ".tmx";
            InputStream inputStream = getClass().getResourceAsStream("/" + resourcePath);

            if (inputStream == null) {
                System.err.println("❌ 无法找到TMX文件: /" + resourcePath + "，尝试目录内回退查找 .tmx 文件");
                // 回退1：开发环境路径扫描 src/main/resources
                java.io.File devDir = new java.io.File("src/main/resources/assets/maps/" + actualDirName);
                java.io.File fsDir = new java.io.File("assets/maps/" + actualDirName);
                java.io.File dirToUse = devDir.exists() ? devDir : (fsDir.exists() ? fsDir : null);
                if (dirToUse != null && dirToUse.isDirectory()) {
                    java.io.File[] tmx = dirToUse.listFiles((d, name) -> name.toLowerCase().endsWith(".tmx"));
                    if (tmx != null && tmx.length > 0) {
                        java.util.Arrays.sort(tmx, java.util.Comparator.comparing(java.io.File::getName));
                        System.out.println("🔎 回退使用TMX文件: " + tmx[0].getAbsolutePath());
                        inputStream = new java.io.FileInputStream(tmx[0]);
                    }
                }
                if (inputStream == null) {
                    System.err.println("❌ 目录内未找到任何 .tmx 文件");
                    return false;
                }
            }

            System.out.println("✅ 找到TMX文件: /" + resourcePath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            try { inputStream.close(); } catch (Exception ignored) {}

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

            // 解析瓦片属性
            parseTileProperties(tilesetElement, tileset);

            tiledMap.getTilesets().add(tileset);

            System.out.println("🎨 瓦片集: " + tileset.getName() + " (GID: " + tileset.getFirstgid() +
                             "-" + (tileset.getFirstgid() + tileset.getTilecount() - 1) +
                             ", 图像: " + tileset.getImage() + ")");
        }
    }

    /**
     * 加载瓦片集图像
     * 根据瓦片集名称映射到对应的hyptosis_tile-art-batch-$.png文件
     */
    private void loadTilesetImage(Tileset tileset) {
        try {
            // 根据瓦片集名称确定对应的图像文件
            String imageFileName = getImageFileNameForTileset(tileset.getName());
            String imagePath = "assets/maps/" + mapName + "/" + imageFileName;
            InputStream imageStream = getClass().getResourceAsStream("/" + imagePath);
            
            if (imageStream != null) {
                Image image = new Image(imageStream);
                tilesetImages.put(tileset.getName(), image);
                logger.info("🖼️ 成功加载瓦片集图像: /" + imagePath + " (瓦片集: " + tileset.getName() + ")");
                System.out.println("🖼️ 成功加载瓦片集图像: /" + imagePath + " (瓦片集: " + tileset.getName() + ")");
            } else {
                logger.warning("❌ 无法加载瓦片集图像: /" + imagePath + " (瓦片集: " + tileset.getName() + ")");
                System.err.println("❌ 无法加载瓦片集图像: /" + imagePath + " (瓦片集: " + tileset.getName() + ")");
            }
        } catch (Exception e) {
            System.err.println("❌ 加载瓦片集图像失败: " + e.getMessage());
        }
    }

    /**
     * 解析瓦片属性
     */
    private void parseTileProperties(Element tilesetElement, Tileset tileset) {
        NodeList tileNodes = tilesetElement.getElementsByTagName("tile");
        int unaccessibleCount = 0;
        
        for (int i = 0; i < tileNodes.getLength(); i++) {
            Element tileElement = (Element) tileNodes.item(i);
            int tileId = Integer.parseInt(tileElement.getAttribute("id"));
            
            // 解析瓦片属性
            TileProperty tileProperty = new TileProperty();
            NodeList propertyNodes = tileElement.getElementsByTagName("property");
            
            for (int j = 0; j < propertyNodes.getLength(); j++) {
                Element propertyElement = (Element) propertyNodes.item(j);
                String propertyName = propertyElement.getAttribute("name");
                String propertyType = propertyElement.getAttribute("type");
                String propertyValue = propertyElement.getAttribute("value");
                
                // 根据属性类型转换值
                Object value = convertPropertyValue(propertyType, propertyValue);
                tileProperty.addProperty(propertyName, value);
                
                // 统计unaccessible属性
                if ("unaccessible".equals(propertyName) && Boolean.TRUE.equals(value)) {
                    unaccessibleCount++;
                }
            }
            
            // 将瓦片属性添加到瓦片集
            if (!tileProperty.getAllProperties().isEmpty()) {
                tileset.addTileProperty(tileId, tileProperty);
            }
        }
        
        if (unaccessibleCount > 0) {
            System.out.println("🚧 瓦片集 " + tileset.getName() + " 中有 " + unaccessibleCount + " 个不可通行的瓦片");
        }
    }
    
    /**
     * 转换属性值类型
     */
    private Object convertPropertyValue(String type, String value) {
        if (type == null || type.isEmpty()) {
            return value; // 默认为字符串
        }
        
        switch (type.toLowerCase()) {
            case "bool":
            case "boolean":
                return Boolean.parseBoolean(value);
            case "int":
            case "integer":
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return 0;
                }
            case "float":
                try {
                    return Float.parseFloat(value);
                } catch (NumberFormatException e) {
                    return 0.0f;
                }
            case "string":
            default:
                return value;
        }
    }

    /**
     * 根据瓦片集名称获取对应的图像文件名
     * 映射规则：1对应1，2对应2，3对应3
     * - 2dmap1 -> hyptosis_tile-art-batch-1.png
     * - 2dmap2 -> hyptosis_tile-art-batch-2.png  
     * - 2dmap3 -> hyptosis_tile-art-batch-3.png
     */
    private String getImageFileNameForTileset(String tilesetName) {
        // 从瓦片集名称中提取数字后缀
        if (tilesetName.endsWith("1")) {
            return "hyptosis_tile-art-batch-1.png";
        } else if (tilesetName.endsWith("2")) {
            return "hyptosis_tile-art-batch-2.png";
        } else if (tilesetName.endsWith("3")) {
            return "hyptosis_tile-art-batch-3.png";
        } else {
            // 默认情况，尝试从名称中提取数字
            return "hyptosis_tile-art-batch-1.png";
        }
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
                            if (hasTilesetImage(tileset)) {
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
     * 检查是否有瓦片集图像
     */
    private boolean hasTilesetImage(Tileset tileset) {
        return tilesetImages.containsKey(tileset.getName());
    }

    /**
     * 从图像创建瓦片
     */
    private void createTileFromImage(Group layer, int x, int y, int gid, Tileset tileset) {
        try {
            // 直接通过瓦片集名称获取图像
            Image tilesetImage = tilesetImages.get(tileset.getName());
            
            if (tilesetImage == null) {
                throw new Exception("未找到瓦片集图像: " + tileset.getName());
            }
            
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
        System.out.println("🎨 创建优化的草地背景");

        Group layer = new Group();

        // 生成一个多样化的草地背景，模拟grass.tmx的尺寸
        int cols = 30; // 默认宽度（列）
        int rows = 30; // 默认高度（行）

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

        // 确保在回退场景下也提供非零的地图与瓦片尺寸，供路径/碰撞等系统使用
        try {
            if (tiledMap != null) {
                tiledMap.setWidth(cols);
                tiledMap.setHeight(rows);
                tiledMap.setTilewidth(32);
                tiledMap.setTileheight(32);
            }
        } catch (Exception ignored) {}
    }

    public void onUpdate(double tpf) {
        // 视口跟随已在 GameApp 中绑定
        // Tiled地图的更新由FXGL自动处理
    }

    /**
     * 构建碰撞地图
     */
    private void buildCollisionMap() {
        if (tiledMap != null) {
            collisionMap = CollisionMap.fromTiledMap(tiledMap);
            System.out.println("🗺️ 碰撞地图构建完成: " + collisionMap.getWidth() + "x" + collisionMap.getHeight());
            
            // 可选：打印碰撞地图用于调试
            if (logger.isLoggable(java.util.logging.Level.FINE)) {
                collisionMap.printCollisionMap();
            }
        } else {
            System.err.println("❌ 无法构建碰撞地图：TiledMap为空");
        }
    }

    /**
     * 检查指定位置是否可通行
     */
    public boolean isPassable(int x, int y) {
        if (collisionMap != null) {
            return collisionMap.isPassable(x, y);
        }
        // 如果没有碰撞地图，默认返回true（可通行）
        return true;
    }
    
    /**
     * 检查指定位置是否不可通行
     */
    public boolean isUnaccessible(int x, int y) {
        if (collisionMap != null) {
            return collisionMap.isUnaccessible(x, y);
        }
        // 如果没有碰撞地图，默认返回false（可通行）
        return false;
    }
    
    /**
     * 获取碰撞地图
     */
    public CollisionMap getCollisionMap() {
        return collisionMap;
    }
    
    /**
     * 调试方法：打印碰撞地图信息
     */
    public void printCollisionInfo() {
        if (collisionMap != null) {
            System.out.println("🗺️ 碰撞地图信息:");
            System.out.println("   尺寸: " + collisionMap.getWidth() + "x" + collisionMap.getHeight());
            
            // 统计不可通行的瓦片数量
            int unaccessibleCount = 0;
            for (int y = 0; y < collisionMap.getHeight(); y++) {
                for (int x = 0; x < collisionMap.getWidth(); x++) {
                    if (collisionMap.isUnaccessible(x, y)) {
                        unaccessibleCount++;
                    }
                }
            }
            System.out.println("   不可通行瓦片数量: " + unaccessibleCount);
            System.out.println("   可通行瓦片数量: " + (collisionMap.getWidth() * collisionMap.getHeight() - unaccessibleCount));
        } else {
            System.out.println("❌ 碰撞地图未初始化");
        }
    }
    
    /**
     * 调试方法：检查指定区域的通行性
     */
    public void checkAreaPassability(int startX, int startY, int width, int height) {
        System.out.println("🔍 检查区域通行性 (" + startX + "," + startY + ") 到 (" + 
                         (startX + width - 1) + "," + (startY + height - 1) + "):");
        
        for (int y = startY; y < startY + height && y < getMapHeight(); y++) {
            StringBuilder line = new StringBuilder();
            for (int x = startX; x < startX + width && x < getMapWidth(); x++) {
                line.append(isPassable(x, y) ? "." : "X");
            }
            System.out.println("   " + line.toString());
        }
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