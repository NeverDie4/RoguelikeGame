package com.roguelike.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 提供关卡（地图）元数据以供选择界面使用。
 * 约定：MapRenderer 将使用 id 作为 mapName 加载
 * 资源路径优先：assets/maps/<id>/<id>.tmx；
 * 若文件名不同，MapRenderer 内部具备回退方案，会在目录下自动寻找 .tmx。
 */
public final class MapRegistry {

    public static final class MapInfo {
        public final String id;        // mapName / 目录名
        public final String title;     // 中文标题
        public final String stage;     // Stage_x 文本
        public final String desc;      // 描述
        public final String preview;   // 预览图相对类路径，例如 assets/maps/<id>/hyptosis_tile-art-batch-1.png

        public MapInfo(String id, String title, String stage, String desc, String preview) {
            this.id = id;
            this.title = title;
            this.stage = stage;
            this.desc = desc;
            this.preview = preview;
        }
    }

    private static final List<MapInfo> MAPS;

    static {
        List<MapInfo> list = new ArrayList<>();
        list.add(new MapInfo(
                "test",
                "疯狂森林",
                "Stage-1",
                "魔堡是一个谎言，但这里有免费的烤鸡，所以也挺好。",
                "assets/maps/map1/preview.png"
        ));
        list.add(new MapInfo(
                "square",
                "锦锻书库",
                "Stage-2",
                "这个安静的长条形图书馆适合休息，冥想和寻找烤鸡，但这里为什么会有石面具？",
                "assets/maps/map2/preview.png"
        ));
        list.add(new MapInfo(
                "dungeon",
                "乳品厂",
                "Stage-3",
                "在这个隐秘的神奇地图上，我们可能会找到关于吸血鬼的线索，或者至少找到更多的烤鸡。",
                "assets/maps/map3/preview.png"
        ));
        MAPS = Collections.unmodifiableList(list);
    }

    public static List<MapInfo> getMaps() {
        return MAPS;
    }
}


