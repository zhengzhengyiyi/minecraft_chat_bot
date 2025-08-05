package com.zhengzhengyiyimc;

import net.fabricmc.api.ModInitializer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zhengzhengyiyimc.command.AskAi;
import com.zhengzhengyiyimc.network.WebSocketClient;
import com.zhengzhengyiyimc.network.WebSocketServer;

public class Minecraft_chatbot implements ModInitializer {
	public static final String MOD_ID = "minecraft_chatbot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static WebSocketServer WSSERVER;
	public static WebSocketClient WSCLIENT;

	private static class Message_handler implements WebSocketClient.MessageHandler {
		public void onMessage(String message) {

		}
        public void onOpen() {

		}
        public void onError(String error) {

		}
        public void onClose(int code, String reason) {

		}
	}

	static {
		try {
			WSSERVER = new WebSocketServer(8081);
			WSCLIENT = new WebSocketClient("ws://localhost:8081", new Message_handler());
		} catch (IOException exception) {
			LOGGER.error(exception.getStackTrace().toString());
		}
	}

	@Override
	public void onInitialize() {
		AskAi.register();
		preloadModel();

		WSCLIENT.sendMessage("hello world");
	}

	public static void preloadModel() {
		new Thread(() -> {
			try {
				new ProcessBuilder("ollama", "pull", "qwen3:1.7b")
					.start()
					.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
}
