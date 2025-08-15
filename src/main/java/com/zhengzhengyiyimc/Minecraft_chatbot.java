package com.zhengzhengyiyimc;

import net.fabricmc.api.ModInitializer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.response.OllamaResult;

import com.zhengzhengyiyimc.command.AskAi;

public class Minecraft_chatbot implements ModInitializer {
	public static final String MOD_ID = "minecraft_chatbot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AskAi.register();
		OllamaResult result;
		try {
			result = Generate.generate("hi");
		} catch (OllamaBaseException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
