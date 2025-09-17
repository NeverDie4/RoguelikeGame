package com.roguelike.map.fxgl;

import com.almasb.fxgl.app.scene.GameView;
import com.almasb.fxgl.dsl.FXGL;
import com.roguelike.map.*;
import javafx.application.Platform;
import javafx.scene.Group;

/**
 * 基于 FXGL Tile/TMX 的 Provider 骨架实现。
 * 当前直接复用现有 MapChunk 的 TMX 数据结构(TiledMap/Tileset/Layer)，
 * 渲染生成一个 Group 包装到 GameView，通行性以两套系统合并（属性/碰撞层）。
 */
public class FXGLTileMapProvider implements TileMapProvider {

    private final MapChunk ownerChunk;
    private final TiledMap tiledMap;
    private final int tileSize; // 约定为 32，来自 TMX
    private final int chunkWidthTiles;
    private final int chunkHeightTiles;
    private final double worldOffsetX;
    private final double worldOffsetY;
    private final String mapName; // 用于定位资源目录

    private GameView gameView;
    private CollisionMap collisionMapOr; // 合并后的 OR 结果
    private static final java.util.Map<String, javafx.scene.image.Image> IMAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public FXGLTileMapProvider(MapChunk ownerChunk,
                               TiledMap tiledMap,
                               int tileSize,
                               int chunkWidthTiles,
                               int chunkHeightTiles,
                               double worldOffsetX,
                               double worldOffsetY,
                               String mapName) {
        this.ownerChunk = ownerChunk;
        this.tiledMap = tiledMap;
        this.tileSize = tileSize;
        this.chunkWidthTiles = chunkWidthTiles;
        this.chunkHeightTiles = chunkHeightTiles;
        this.worldOffsetX = worldOffsetX;
        this.worldOffsetY = worldOffsetY;
        this.mapName = mapName;
        buildCollisionORMap();
        buildView();
    }

    private void buildCollisionORMap() {
        // 基于属性/碰撞层 OR 合并
        CollisionMap fromProps = CollisionMap.fromTiledMap(tiledMap);
        // 额外：如果存在名含 "collision" 的图层，则直接标记为不可通行
        CollisionMap fromLayer = new CollisionMap(chunkWidthTiles, chunkHeightTiles);
        for (Layer layer : tiledMap.getLayers()) {
            String name = layer.getName() != null ? layer.getName().toLowerCase() : "";
            if (name.contains("collision")) {
                for (int y = 0; y < layer.getHeight(); y++) {
                    for (int x = 0; x < layer.getWidth(); x++) {
                        int gid = layer.getData().get(y * layer.getWidth() + x);
                        if (gid > 0) {
                            fromLayer.setCollision(x, y, true);
                        }
                    }
                }
            }
        }
        // 合并 OR
        collisionMapOr = new CollisionMap(chunkWidthTiles, chunkHeightTiles);
        for (int y = 0; y < chunkHeightTiles; y++) {
            for (int x = 0; x < chunkWidthTiles; x++) {
                boolean collides = !fromProps.isPassable(x, y) || fromLayer.isUnaccessible(x, y);
                collisionMapOr.setCollision(x, y, collides);
            }
        }
    }

    private void buildView() {
        Group layerGroup = new Group();
        // 基于 tileset 图像切分绘制所有可见瓦片（GPU 渲染 ImageView/Viewport）
        for (Layer tileLayer : tiledMap.getLayers()) {
            Group g = new Group();
            for (int y = 0; y < tileLayer.getHeight(); y++) {
                for (int x = 0; x < tileLayer.getWidth(); x++) {
                    int gid = tileLayer.getData().get(y * tileLayer.getWidth() + x);
                    if (gid <= 0) continue;
                    Tileset tileset = findTilesetForGid(gid);
                    if (tileset == null) continue;
                    int localId = gid - tileset.getFirstgid();
                    int cols = Math.max(1, tileset.getColumns());
                    int sx = (localId % cols) * tileSize;
                    int sy = (localId / cols) * tileSize;
                    // 加载 tileset 图像
                    javafx.scene.image.Image img = loadTilesetImage(mapName, tileset.getSource());
                    if (img == null) continue;
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
                    iv.setImage(img); // 已同步加载
                    iv.setViewport(new javafx.geometry.Rectangle2D(sx, sy, tileSize, tileSize));
                    iv.setTranslateX(x * tileSize);
                    iv.setTranslateY(y * tileSize);
                    g.getChildren().add(iv);
                }
            }
            layerGroup.getChildren().add(g);
        }
        layerGroup.setTranslateX(worldOffsetX);
        layerGroup.setTranslateY(worldOffsetY);
        this.gameView = new GameView(layerGroup, -1);
    }

    private Tileset findTilesetForGid(int gid) {
        for (Tileset t : tiledMap.getTilesets()) {
            if (gid >= t.getFirstgid() && gid < t.getFirstgid() + t.getTilecount()) return t;
        }
        return null;
    }

    private javafx.scene.image.Image loadTilesetImage(String mapName, String imageSource) {
        try {
            String path;
            if (imageSource == null) return null;
            if (imageSource.startsWith("../")) {
                String relativePath = imageSource.substring(3);
                path = "/assets/maps/" + relativePath;
            } else {
                String dir = mapNameToAssetsDir(mapName);
                path = "/assets/maps/" + dir + "/" + imageSource;
            }
            // 使用静态缓存，避免为每个瓦片重复解码整张 tileset
            javafx.scene.image.Image cached = IMAGE_CACHE.get(path);
            if (cached != null) return cached;
            java.net.URL url = FXGLTileMapProvider.class.getResource(path);
            if (url == null) return null;
            // 同步加载以规避 JavaFX 背景加载回调的监听器 NPE 问题
            javafx.scene.image.Image img = new javafx.scene.image.Image(url.toExternalForm(), false);
            IMAGE_CACHE.put(path, img);
            return img;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String mapNameToAssetsDir(String mapName) {
        if (mapName.endsWith("_door") || mapName.endsWith("_boss")) {
            String base = mapName.substring(0, mapName.lastIndexOf("_"));
            return switch (base) { case "test" -> "map1"; case "square" -> "map2"; case "dungeon" -> "map3"; default -> base; };
        }
        return switch (mapName) { case "test" -> "map1"; case "square" -> "map2"; case "dungeon" -> "map3"; default -> mapName; };
    }

    @Override
    public void addToScene() {
        if (gameView == null) return;
        if (Platform.isFxApplicationThread()) {
            FXGL.getGameScene().addGameView(gameView);
        } else {
            Platform.runLater(() -> FXGL.getGameScene().addGameView(gameView));
        }
    }

    @Override
    public void removeFromScene() {
        if (gameView == null) return;
        if (Platform.isFxApplicationThread()) {
            FXGL.getGameScene().removeGameView(gameView);
        } else {
            Platform.runLater(() -> FXGL.getGameScene().removeGameView(gameView));
        }
    }

    @Override
    public boolean isPassable(double worldX, double worldY) {
        int localX = (int) ((worldX - worldOffsetX) / tileSize);
        int localY = (int) ((worldY - worldOffsetY) / tileSize);
        if (localX < 0 || localX >= chunkWidthTiles || localY < 0 || localY >= chunkHeightTiles) {
            return false;
        }
        return collisionMapOr.isPassable(localX, localY);
    }

    @Override
    public int getChunkWidthPixels() {
        return chunkWidthTiles * tileSize;
    }

    @Override
    public int getChunkHeightPixels() {
        return chunkHeightTiles * tileSize;
    }

    @Override
    public int getTileSize() {
        return tileSize;
    }

    @Override
    public GameView getGameView() {
        return gameView;
    }

    @Override
    public void scanSpecialTiles() {
        // 暂留：由上层 TimerTileManager/TeleportManager 继续调用 MapChunk 扫描，或未来迁移至此
    }

    @Override
    public void setTilePassable(int tileX, int tileY, boolean passable) {
        if (tileX < 0 || tileX >= chunkWidthTiles || tileY < 0 || tileY >= chunkHeightTiles) return;
        collisionMapOr.setCollision(tileX, tileY, !passable);
    }
}


