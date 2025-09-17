package com.roguelike.map;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.app.scene.GameView;
import com.roguelike.map.fxgl.FXGLTileMapProvider;
import javafx.scene.Group;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * 地图区块类，管理单个区块的数据和渲染
 */
public class MapChunk {
    
    private int chunkX;                    // 区块X坐标
    private int chunkY;                    // 区块Y坐标
    private String mapName;                // 地图名称
    private TiledMap tiledMap;             // 区块的地图数据
    private CollisionMap collisionMap;     // 区块的碰撞数据
    private GameView mapView;              // 区块的渲染视图
    private boolean isLoaded;              // 是否已加载
    private double worldOffsetX;           // 世界坐标X偏移
    private double worldOffsetY;           // 世界坐标Y偏移
    private Map<String, Image> tilesetImages = new HashMap<>(); // 瓦片集图像缓存
    // 新增：Provider（双实现开关）
    private TileMapProvider tileMapProvider;
    
    // 静态缓存，避免重复解析相同的地图文件
    // 基于mapName的缓存，所有区块共享相同的地图数据，但独立计算世界偏移
    private static Map<String, TiledMap> cachedTiledMaps = new HashMap<>();
    private static Map<String, Map<String, Image>> cachedTilesetImagesMap = new HashMap<>();
    private static final Object cacheLock = new Object();
    
    // 地图常量
    private static final int TILE_SIZE = 32;     // 瓦片尺寸
    private int chunkWidth;   // 区块宽度（瓦片数）- 动态获取
    private int chunkHeight;  // 区块高度（瓦片数）- 动态获取
    
    public MapChunk(int chunkX, int chunkY, String mapName) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.mapName = mapName;
        
        // 使用MapChunkFactory获取正确的地图尺寸
        int[] dimensions = MapChunkFactory.getMapDimensions(mapName);
        this.chunkWidth = dimensions[0];
        this.chunkHeight = dimensions[1];
        
        this.worldOffsetX = chunkX * chunkWidth * TILE_SIZE;
        this.worldOffsetY = chunkY * chunkHeight * TILE_SIZE;
        this.isLoaded = false;
    }

    /**
     * 将地图名称映射到实际的目录名称
     * test -> map1, square -> map2, dungeon -> map3
     * 支持特殊地图：test_door -> map1, square_door -> map2, dungeon_door -> map3
     */
    private String getMapDirectoryName(String mapName) {
        // 处理特殊地图名称（_door, _boss）
        if (mapName.endsWith("_door") || mapName.endsWith("_boss")) {
            String baseName = mapName.substring(0, mapName.lastIndexOf("_"));
            switch (baseName) {
                case "test":
                    return "map1";
                case "square":
                    return "map2";
                case "dungeon":
                    return "map3";
                default:
                    return baseName; // 如果不在映射中，使用原名称
            }
        }
        
        // 处理基础地图名称
        switch (mapName) {
            case "test":
                return "map1";
            case "square":
                return "map2";
            case "dungeon":
                return "map3";
            default:
                return mapName; // 如果不在映射中，使用原名称
        }
    }
    
    /**
     * 加载区块
     */
    public void load() {
        if (isLoaded) {
            return;
        }
        
        try {
            // 加载基础地图（所有区块使用相同的地图文件）
            loadBaseMap();
            
            // Provider 开关：默认启用
            if (MapRuntimeConfig.useFxglTileMap()) {
                tileMapProvider = new FXGLTileMapProvider(
                    this,
                    cachedTiledMaps.get(mapName),
                    TILE_SIZE,
                    chunkWidth,
                    chunkHeight,
                    worldOffsetX,
                    worldOffsetY,
                    mapName
                );
                tileMapProvider.addToScene();
            } else {
                // 回退到旧渲染
                createMapView();
                if (mapView != null) FXGL.getGameScene().addGameView(mapView);
            }
            // 构建碰撞（保留旧方式作为回退与辅助数据）
            buildCollisionMap();
            
        isLoaded = true;
        System.out.println("🗺️ 区块 (" + chunkX + "," + chunkY + ") 加载完成 (偏移: " + worldOffsetX + "," + worldOffsetY + ")");
        System.out.println("   地图名称: " + mapName);
        System.out.println("   区块尺寸: " + chunkWidth + "x" + chunkHeight);
        System.out.println("   瓦片集数量: " + tiledMap.getTilesets().size());
        System.out.println("   图层数量: " + tiledMap.getLayers().size());
        System.out.println("   图像缓存: " + tilesetImages.size() + " 个");
        
        // 打印瓦片集信息
        for (Tileset tileset : tiledMap.getTilesets()) {
            System.out.println("   瓦片集: " + tileset.getName() + " (GID: " + tileset.getFirstgid() + "-" + (tileset.getFirstgid() + tileset.getTilecount() - 1) + ")");
        }
            
        } catch (Exception e) {
            System.err.println("❌ 区块 " + chunkX + " 加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 卸载区块
     */
    public void unload() {
        if (!isLoaded) {
            return;
        }
        
        // 从场景中移除视图
        if (mapView != null) {
            FXGL.getGameScene().removeGameView(mapView);
        }
        if (tileMapProvider != null) {
            try { tileMapProvider.removeFromScene(); } catch (Throwable ignored) {}
        }
        
        // 清理资源
        tiledMap = null;
        collisionMap = null;
        mapView = null;
        tileMapProvider = null;
        isLoaded = false;
        
        System.out.println("🗑️ 区块 (" + chunkX + "," + chunkY + ") 已卸载");
    }
    
    /**
     * 加载基础地图数据（使用缓存避免重复解析）
     * 支持基于mapName的缓存，每个区块独立计算世界偏移
     */
    private void loadBaseMap() throws Exception {
        synchronized (cacheLock) {
            // 生成缓存键：只基于mapName，因为所有区块使用相同的地图文件
            String cacheKey = mapName;
            
            // 如果该地图的缓存不存在，则解析地图文件
            if (!cachedTiledMaps.containsKey(cacheKey)) {
                System.out.println("📋 首次解析地图文件 " + mapName + "，创建缓存...");
                
                // 使用配置的地图名称，映射到实际目录
                String actualDirName = getMapDirectoryName(mapName);
                String resourcePath = "assets/maps/" + actualDirName + "/" + mapName + ".tmx";
                InputStream inputStream = getClass().getResourceAsStream("/" + resourcePath);
                
                if (inputStream == null) {
                    throw new Exception("无法找到地图文件: /" + resourcePath);
                }
                
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(inputStream);
                
                Element mapElement = document.getDocumentElement();
                
                // 创建TiledMap对象
                TiledMap newTiledMap = new TiledMap();
                newTiledMap.setWidth(Integer.parseInt(mapElement.getAttribute("width")));
                newTiledMap.setHeight(Integer.parseInt(mapElement.getAttribute("height")));
                newTiledMap.setTilewidth(Integer.parseInt(mapElement.getAttribute("tilewidth")));
                newTiledMap.setTileheight(Integer.parseInt(mapElement.getAttribute("tileheight")));
                
                // 解析瓦片集和图层（使用临时变量）
                Map<String, Image> tempTilesetImages = new HashMap<>();
                
                // 临时设置实例变量用于解析
                TiledMap originalTiledMap = this.tiledMap;
                Map<String, Image> originalTilesetImages = this.tilesetImages;
                
                this.tiledMap = newTiledMap;
                this.tilesetImages = tempTilesetImages;
                
                parseTilesets(mapElement);
                parseTileLayers(mapElement);
                
                // 恢复实例变量
                this.tiledMap = originalTiledMap;
                this.tilesetImages = originalTilesetImages;
                
                // 缓存地图数据和瓦片集图像
                cachedTiledMaps.put(cacheKey, newTiledMap);
                cachedTilesetImagesMap.put(cacheKey, new HashMap<>(tempTilesetImages));
                
                inputStream.close();
                System.out.println("✅ 地图缓存创建完成: " + cacheKey);
            }
            
            // 使用缓存的地图数据
            tiledMap = cachedTiledMaps.get(cacheKey);
            tilesetImages = new HashMap<>(cachedTilesetImagesMap.get(cacheKey));
            
            // 更新区块尺寸（从缓存的地图数据获取）
            this.chunkWidth = tiledMap.getWidth();
            this.chunkHeight = tiledMap.getHeight();
            
            // 计算世界偏移（每个区块独立计算）
            this.worldOffsetX = chunkX * chunkWidth * TILE_SIZE;
            this.worldOffsetY = chunkY * chunkHeight * TILE_SIZE;
            
            System.out.println("🔧 MapChunk尺寸更新: " + mapName + " -> " + chunkWidth + "x" + chunkHeight + " 瓦片 (" + (chunkWidth * TILE_SIZE) + "x" + (chunkHeight * TILE_SIZE) + " 像素)");
            System.out.println("   区块(" + chunkX + "," + chunkY + ") 世界偏移: (" + worldOffsetX + "," + worldOffsetY + ")");
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
            
            // 解析图像源
            NodeList imageNodes = tilesetElement.getElementsByTagName("image");
            if (imageNodes.getLength() > 0) {
                Element imageElement = (Element) imageNodes.item(0);
                String imageSource = imageElement.getAttribute("source");
                tileset.setSource(imageSource);
                
                // 加载瓦片集图像
                loadTilesetImage(tileset.getName(), imageSource);
            }
            
            // 解析瓦片属性
            parseTileProperties(tilesetElement, tileset);
            
            tiledMap.getTilesets().add(tileset);
        }
    }
    
    /**
     * 加载瓦片集图像
     */
    private void loadTilesetImage(String tilesetName, String imageSource) {
        try {
            String imagePath;
            
            // 处理相对路径（如 ../dungeon/hyptosis_tile-art-batch-1.png）
            if (imageSource.startsWith("../")) {
                // 相对路径：从当前地图目录的上级目录开始
                String relativePath = imageSource.substring(3); // 移除 "../"
                imagePath = "assets/maps/" + relativePath;
            } else {
                // 绝对路径：在当前地图目录中
                String actualDirName = getMapDirectoryName(mapName);
                imagePath = "assets/maps/" + actualDirName + "/" + imageSource;
            }
            
            InputStream imageStream = getClass().getResourceAsStream("/" + imagePath);
            
            if (imageStream != null) {
                Image image = new Image(imageStream);
                tilesetImages.put(tilesetName, image);
                System.out.println("✅ 成功加载瓦片集图像: " + imageSource + " -> " + imagePath);
            } else {
                System.err.println("❌ 无法找到瓦片集图像: " + imagePath + " (原始路径: " + imageSource + ")");
            }
        } catch (Exception e) {
            System.err.println("❌ 加载瓦片集图像失败: " + imageSource + " - " + e.getMessage());
        }
    }
    
    /**
     * 解析瓦片属性
     */
    private void parseTileProperties(Element tilesetElement, Tileset tileset) {
        NodeList tileNodes = tilesetElement.getElementsByTagName("tile");
        for (int i = 0; i < tileNodes.getLength(); i++) {
            Element tileElement = (Element) tileNodes.item(i);
            int tileId = Integer.parseInt(tileElement.getAttribute("id"));
            
            // 解析瓦片属性
            NodeList propertyNodes = tileElement.getElementsByTagName("property");
            if (propertyNodes.getLength() > 0) {
                TileProperty tileProperty = new TileProperty();
                
                for (int j = 0; j < propertyNodes.getLength(); j++) {
                    Element propertyElement = (Element) propertyNodes.item(j);
                    String name = propertyElement.getAttribute("name");
                    String value = propertyElement.getAttribute("value");
                    String type = propertyElement.getAttribute("type");
                    
                    // 根据类型转换值
                    Object convertedValue = convertPropertyValue(value, type);
                    tileProperty.addProperty(name, convertedValue);
                }
                
                // 将瓦片属性添加到瓦片集
                tileset.addTileProperty(tileId, tileProperty);
            }
        }
    }
    
    /**
     * 转换属性值类型
     */
    private Object convertPropertyValue(String value, String type) {
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
            default:
                return value; // 默认为字符串
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
            
            // 解析数据
            NodeList dataNodes = layerElement.getElementsByTagName("data");
            if (dataNodes.getLength() > 0) {
                Element dataElement = (Element) dataNodes.item(0);
                String data = dataElement.getTextContent().trim();
                String[] values = data.split(",");
                
                List<Integer> tileData = new ArrayList<>();
                for (int j = 0; j < values.length; j++) {
                    tileData.add(Integer.parseInt(values[j].trim()));
                }
                layer.setData(tileData);
            }
            
            tiledMap.getLayers().add(layer);
        }
    }
    
    /**
     * 创建地图渲染视图
     */
    private void createMapView() {
        Group layer = new Group();
        
        // 为每个瓦片层创建瓦片
        for (Layer tileLayer : tiledMap.getLayers()) {
            for (int y = 0; y < tileLayer.getHeight(); y++) {
                for (int x = 0; x < tileLayer.getWidth(); x++) {
                    int index = y * tileLayer.getWidth() + x;
                    int gid = tileLayer.getData().get(index);
                    
                    if (gid > 0) {
                        // 尝试使用图像创建瓦片，如果失败则使用颜色
                        if (createTileFromImage(layer, x, y, gid)) {
                            // 成功使用图像创建
                        } else {
                            // 回退到颜色创建
                            createTileFromColor(layer, x, y, gid);
                        }
                    }
                }
            }
        }
        
        // 设置Group的偏移
        layer.setTranslateX(worldOffsetX);
        layer.setTranslateY(worldOffsetY);
        
        // 创建GameView，设置渲染层级为背景层（负值表示在背景）
        mapView = new GameView(layer, -1);
        
        // 注意：不在这里添加到场景，而是在主线程中添加
    }
    
    /**
     * 将地图视图添加到场景（必须在FX Application Thread中调用）
     */
    public void addToScene() {
        if (mapView != null) {
            FXGL.getGameScene().addGameView(mapView);
        }
        // 暂不添加 provider 的视图，避免重复；后续切换渲染时启用。
    }
    
    /**
     * 从图像创建瓦片
     */
    private boolean createTileFromImage(Group layer, int x, int y, int gid) {
        // 找到对应的瓦片集
        Tileset tileset = findTilesetForGid(gid);
        if (tileset == null || !tilesetImages.containsKey(tileset.getName())) {
            return false; // 无法使用图像创建
        }
        
        try {
            Image tilesetImage = tilesetImages.get(tileset.getName());
            
            // 计算瓦片在瓦片集中的位置
            int localId = gid - tileset.getFirstgid();
            int tilesPerRow = tileset.getColumns();
            int tileX = localId % tilesPerRow;
            int tileY = localId / tilesPerRow;
            
            // 创建ImageView来显示瓦片
            javafx.scene.image.ImageView tileView = new javafx.scene.image.ImageView(tilesetImage);
            
            // 设置视口来显示特定的瓦片
            tileView.setViewport(new javafx.geometry.Rectangle2D(
                tileX * TILE_SIZE, tileY * TILE_SIZE, TILE_SIZE, TILE_SIZE
            ));
            
            // 设置位置
            tileView.setTranslateX(x * TILE_SIZE);
            tileView.setTranslateY(y * TILE_SIZE);
            
            layer.getChildren().add(tileView);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 从图像创建瓦片失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 找到对应GID的瓦片集
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
     * 从颜色创建瓦片（改进版本，支持更多瓦片类型）
     */
    private void createTileFromColor(Group layer, int x, int y, int gid) {
        javafx.scene.shape.Rectangle tile = new javafx.scene.shape.Rectangle(TILE_SIZE, TILE_SIZE);
        
        // 根据GID设置颜色和样式
        if (gid == 1) {
            tile.setFill(javafx.scene.paint.Color.web("#4a7c59")); // 草地
            tile.setStroke(javafx.scene.paint.Color.web("#2a5c39"));
        } else if (gid == 2) {
            tile.setFill(javafx.scene.paint.Color.web("#8B4513")); // 泥土
            tile.setStroke(javafx.scene.paint.Color.web("#654321"));
        } else if (gid == 3) {
            tile.setFill(javafx.scene.paint.Color.web("#696969")); // 石头
            tile.setStroke(javafx.scene.paint.Color.web("#2F2F2F"));
        } else if (gid == 4) {
            tile.setFill(javafx.scene.paint.Color.web("#4169E1")); // 水
            tile.setStroke(javafx.scene.paint.Color.web("#0000CD"));
        } else if (gid == 5) {
            tile.setFill(javafx.scene.paint.Color.web("#228B22")); // 深绿草地
            tile.setStroke(javafx.scene.paint.Color.web("#006400"));
        } else {
            // 默认草地，使用更丰富的颜色变化
            int colorVariation = (x + y) % 3;
            switch (colorVariation) {
                case 0:
                    tile.setFill(javafx.scene.paint.Color.web("#4a7c59")); // 深绿
                    break;
                case 1:
                    tile.setFill(javafx.scene.paint.Color.web("#5a8c69")); // 中绿
                    break;
                case 2:
                    tile.setFill(javafx.scene.paint.Color.web("#6a9c79")); // 浅绿
                    break;
            }
            tile.setStroke(javafx.scene.paint.Color.web("#2a5c39"));
        }
        
        tile.setStrokeWidth(0.5);
        
        tile.setTranslateX(x * TILE_SIZE);
        tile.setTranslateY(y * TILE_SIZE);
        layer.getChildren().add(tile);
    }
    
    /**
     * 构建碰撞地图
     */
    private void buildCollisionMap() {
        if (tiledMap != null) {
            collisionMap = buildCollisionMapFromTiledMap(tiledMap);
        }
    }
    
    /**
     * 从TiledMap构建碰撞地图（支持瓦片属性）
     */
    private CollisionMap buildCollisionMapFromTiledMap(TiledMap tiledMap) {
        CollisionMap collisionMap = new CollisionMap(tiledMap.getWidth(), tiledMap.getHeight());
        
        // 遍历所有图层
        for (Layer layer : tiledMap.getLayers()) {
            // 只处理碰撞层（通常命名为"collision"或"collision_layer"）
            if (layer.getName().toLowerCase().contains("collision")) {
                for (int y = 0; y < layer.getHeight(); y++) {
                    for (int x = 0; x < layer.getWidth(); x++) {
                        int index = y * layer.getWidth() + x;
                        int gid = layer.getData().get(index);
                        
                        if (gid > 0) {
                            // 检查瓦片是否不可通行
                            boolean isUnaccessible = isTileUnaccessible(gid);
                            collisionMap.setCollision(x, y, isUnaccessible);
                        }
                    }
                }
            } else {
                // 对于非碰撞层，检查瓦片属性
                for (int y = 0; y < layer.getHeight(); y++) {
                    for (int x = 0; x < layer.getWidth(); x++) {
                        int index = y * layer.getWidth() + x;
                        int gid = layer.getData().get(index);
                        
                        if (gid > 0) {
                            // 检查瓦片是否不可通行
                            boolean isUnaccessible = isTileUnaccessible(gid);
                            // 如果当前位置还没有设置碰撞，则设置
                            if (!collisionMap.hasCollision(x, y)) {
                                collisionMap.setCollision(x, y, isUnaccessible);
                            }
                        }
                    }
                }
            }
        }
        
        return collisionMap;
    }
    
    /**
     * 检查瓦片是否不可通行
     */
    private boolean isTileUnaccessible(int gid) {
        // 找到对应的瓦片集
        Tileset tileset = findTilesetForGid(gid);
        if (tileset != null) {
            int localId = gid - tileset.getFirstgid();
            TileProperty tileProperty = tileset.getTileProperty(localId);
            if (tileProperty != null) {
                return tileProperty.isUnaccessible();
            }
        }
        return false; // 默认可通行
    }
    
    /**
     * 检查指定位置是否可通行
     */
    public boolean isPassable(double worldX, double worldY) {
        if (!isLoaded) {
            return false; // 未加载时默认为不可通行，确保碰撞检测准确性
        }
        // 优先走新 Provider（合并属性/碰撞层）
        if (tileMapProvider != null) {
            return tileMapProvider.isPassable(worldX, worldY);
        }
        
        // 转换为区块内坐标
        int localX = (int) ((worldX - worldOffsetX) / TILE_SIZE);
        int localY = (int) ((worldY - worldOffsetY) / TILE_SIZE);
        
        // 检查是否在区块范围内
        if (localX < 0 || localX >= chunkWidth || localY < 0 || localY >= chunkHeight) {
            return false; // 超出区块范围时默认为不可通行，防止越界移动
        }
        
        return collisionMap != null && collisionMap.isPassable(localX, localY);
    }
    
    /**
     * 检查指定位置是否可通行（支持跨区块检测）
     * 用于无限地图的跨区块寻路
     */
    public boolean isPassableCrossChunk(double worldX, double worldY, InfiniteMapManager infiniteMapManager) {
        if (!isLoaded || collisionMap == null) {
            return false; // 未加载时默认为不可通行
        }
        
        // 转换为区块内坐标
        int localX = (int) ((worldX - worldOffsetX) / TILE_SIZE);
        int localY = (int) ((worldY - worldOffsetY) / TILE_SIZE);
        
        // 检查是否在区块范围内
        if (localX < 0 || localX >= chunkWidth || localY < 0 || localY >= chunkHeight) {
            // 超出当前区块范围，检查是否在邻近区块内
            return infiniteMapManager.isPassable(worldX, worldY);
        }
        
        return collisionMap.isPassable(localX, localY);
    }
    
    /**
     * 检查指定位置是否不可通行
     */
    public boolean isUnaccessible(double worldX, double worldY) {
        return !isPassable(worldX, worldY);
    }
    
    /**
     * 使指定瓦片位置变为可通行（用于定时器瓦片）
     * @param tileX 瓦片X坐标
     * @param tileY 瓦片Y坐标
     */
    public void makeTilePassable(int tileX, int tileY) {
        if (tileMapProvider != null) {
            tileMapProvider.setTilePassable(tileX, tileY, true);
            System.out.println("✅ 瓦片位置(" + tileX + "," + tileY + ") 已变为可通行 (provider)");
            return;
        }
        if (collisionMap != null && 
            tileX >= 0 && tileX < collisionMap.getWidth() && 
            tileY >= 0 && tileY < collisionMap.getHeight()) {
            collisionMap.setCollision(tileX, tileY, false);
            System.out.println("✅ 瓦片位置(" + tileX + "," + tileY + ") 已变为可通行");
        }
    }
    
    /**
     * 使指定瓦片位置变为不可通行
     * @param tileX 瓦片X坐标
     * @param tileY 瓦片Y坐标
     */
    public void makeTileUnpassable(int tileX, int tileY) {
        if (tileMapProvider != null) {
            tileMapProvider.setTilePassable(tileX, tileY, false);
            System.out.println("🚫 瓦片位置(" + tileX + "," + tileY + ") 已变为不可通行 (provider)");
            return;
        }
        if (collisionMap != null && 
            tileX >= 0 && tileX < collisionMap.getWidth() && 
            tileY >= 0 && tileY < collisionMap.getHeight()) {
            collisionMap.setCollision(tileX, tileY, true);
            System.out.println("🚫 瓦片位置(" + tileX + "," + tileY + ") 已变为不可通行");
        }
    }
    
    /**
     * 检查指定瓦片位置是否可通行
     * @param tileX 瓦片X坐标
     * @param tileY 瓦片Y坐标
     * @return 是否可通行
     */
    public boolean isTilePassable(int tileX, int tileY) {
        if (collisionMap != null && 
            tileX >= 0 && tileX < collisionMap.getWidth() && 
            tileY >= 0 && tileY < collisionMap.getHeight()) {
            
            return collisionMap.isPassable(tileX, tileY);
        }
        
        return false;
    }
    
    // Getter方法
    public int getChunkX() { return chunkX; }
    public int getChunkY() { return chunkY; }
    public int getChunkWidth() { return chunkWidth; }
    public int getChunkHeight() { return chunkHeight; }
    public TiledMap getTiledMap() { return tiledMap; }
    public CollisionMap getCollisionMap() { return collisionMap; }
    public GameView getMapView() { return mapView; }
    public boolean isLoaded() { return isLoaded; }
    public double getWorldOffsetX() { return worldOffsetX; }
    public double getWorldOffsetY() { return worldOffsetY; }
    
    /**
     * 世界坐标转区块坐标（使用默认尺寸）
     * @deprecated 使用MapChunkFactory.worldToChunkX(worldX, mapName)替代
     */
    @Deprecated
    public static int worldToChunkX(double worldX) {
        return (int) Math.floor(worldX / (96 * TILE_SIZE)); // 默认96x54
    }
    
    /**
     * 区块坐标转世界坐标（使用默认尺寸）
     * @deprecated 使用MapChunkFactory.chunkToWorldX(chunkX, mapName)替代
     */
    @Deprecated
    public static double chunkToWorldX(int chunkX) {
        return chunkX * 96 * TILE_SIZE; // 默认96x54
    }
    
    /**
     * 世界坐标转区块Y坐标（使用默认尺寸）
     * @deprecated 使用MapChunkFactory.worldToChunkY(worldY, mapName)替代
     */
    @Deprecated
    public static int worldToChunkY(double worldY) {
        return (int) Math.floor(worldY / (54 * TILE_SIZE)); // 默认96x54
    }
    
    /**
     * 区块Y坐标转世界坐标（使用默认尺寸）
     * @deprecated 使用MapChunkFactory.chunkToWorldY(chunkY, mapName)替代
     */
    @Deprecated
    public static double chunkToWorldY(int chunkY) {
        return chunkY * 54 * TILE_SIZE; // 默认96x54
    }
    
    /**
     * 获取区块宽度（像素）- 实例方法
     */
    public int getChunkWidthPixels() {
        return chunkWidth * TILE_SIZE;
    }
    
    /**
     * 获取区块高度（像素）- 实例方法
     */
    public int getChunkHeightPixels() {
        return chunkHeight * TILE_SIZE;
    }
    
    /**
     * 清理所有缓存（用于内存管理）
     */
    public static void clearCache() {
        synchronized (cacheLock) {
            cachedTiledMaps.clear();
            cachedTilesetImagesMap.clear();
            System.out.println("🗑️ 地图缓存已清理");
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        synchronized (cacheLock) {
            return "缓存的地图数量: " + cachedTiledMaps.size() + 
                   ", 缓存的瓦片集数量: " + cachedTilesetImagesMap.size();
        }
    }
}
