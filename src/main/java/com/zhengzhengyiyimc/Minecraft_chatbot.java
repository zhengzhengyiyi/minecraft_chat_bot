package com.zhengzhengyiyimc;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zhengzhengyiyimc.command.AskAi;

public class Minecraft_chatbot implements ModInitializer {
	public static final String MOD_ID = "minecraft_chatbot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AskAi.register();
		preloadModel();
	}

	public static void preloadModel() {
		new Thread(() -> {
			try {
				new ProcessBuilder("ollama", "pull", "tinyllama")
					.start()
					.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
}
