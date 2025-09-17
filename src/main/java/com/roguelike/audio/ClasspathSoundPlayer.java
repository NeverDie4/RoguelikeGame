package com.roguelike.audio;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;

/**
 * 仅使用类路径加载并播放音效的工具类（不依赖 FXGL 资源系统）。
 */
public final class ClasspathSoundPlayer {

	private ClasspathSoundPlayer() {}

	/**
	 * 从类路径播放一次音效。
	 * @param resourcePath 类路径资源，例如 "assets/sounds/dies/die.mp3"
	 */
	public static void playOnce(String resourcePath) {
		try {
			URL url = ClasspathSoundPlayer.class.getClassLoader().getResource(resourcePath);
			if (url == null) {
				System.err.println("未找到音效资源: " + resourcePath);
				return;
			}

			Runnable playTask = () -> {
				try {
					Media media = new Media(url.toExternalForm());
					MediaPlayer player = new MediaPlayer(media);

					// 应用全局音量（若可用），否则保持默认音量
					double volume = 1.0;
					try {
						com.roguelike.audio.AudioManager am = com.roguelike.audio.AudioManager.getInstance();
						volume = Math.max(0.0, Math.min(1.0, am.getMasterVolume() * am.getSoundEffectsVolume()));
					} catch (Throwable ignored) {}
					player.setVolume(volume);

					player.setOnEndOfMedia(player::dispose);
					player.setOnError(player::dispose);
					player.play();
				} catch (Throwable e) {
					System.err.println("播放音效失败: " + e.getMessage());
				}
			};

			if (Platform.isFxApplicationThread()) {
				playTask.run();
			} else {
				Platform.runLater(playTask);
			}
		} catch (Throwable e) {
			System.err.println("初始化音效播放失败: " + e.getMessage());
		}
	}
}


