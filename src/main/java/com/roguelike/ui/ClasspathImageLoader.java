package com.roguelike.ui;

import javafx.scene.image.Image;

/**
 * 仅使用类路径加载图片（支持 webp，依赖 TwelveMonkeys）。
 */
public final class ClasspathImageLoader {
    private ClasspathImageLoader() {}

    public static Image load(String path) {
        if (path == null || path.isEmpty()) return null;
        java.io.InputStream is = null;
        try {
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null) is = ctx.getResourceAsStream(path);
            if (is == null) is = ClasspathImageLoader.class.getClassLoader().getResourceAsStream(path);
            if (is == null) is = ClasspathImageLoader.class.getResourceAsStream("/" + path);
            if (is == null) is = ClassLoader.getSystemResourceAsStream(path);
            if (is != null) {
                Image img = new Image(is);
                try { is.close(); } catch (Exception ignored) {}
                if (img != null && img.getWidth() > 0 && !img.isError()) return img;
            }
        } catch (Exception ignored) {
            try { if (is != null) is.close(); } catch (Exception ignore2) {}
        }

        // 回退：ImageIO（支持 webp）
        try {
            javax.imageio.ImageIO.scanForPlugins();
            java.net.URL url = null;
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            if (ctx != null) url = ctx.getResource(path);
            if (url == null) url = ClasspathImageLoader.class.getClassLoader().getResource(path);
            if (url == null) url = ClasspathImageLoader.class.getResource("/" + path);
            if (url == null) url = ClassLoader.getSystemResource(path);
            if (url != null) {
                java.awt.image.BufferedImage awtImg = javax.imageio.ImageIO.read(url);
                if (awtImg != null) {
                    return javafx.embed.swing.SwingFXUtils.toFXImage(awtImg, null);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}


