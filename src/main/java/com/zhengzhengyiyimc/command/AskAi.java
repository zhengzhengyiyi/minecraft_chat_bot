package com.zhengzhengyiyimc.command;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;

import io.github.ollama4j.exceptions.OllamaBaseException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import com.zhengzhengyiyimc.Generate;

public class AskAi {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("ask_ai")
                    .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                        .executes(context -> {
                            try {
                                String version = context.getSource().getServer().getVersion();
                                String prompt = StringArgumentType.getString(context, "prompt");

                                context.getSource().sendFeedback(() -> Text.of("loading ai message, Minecraft version: " + version + " sent message: " + prompt), true);

                                CompletableFuture.supplyAsync(() -> {
                                    try {
                                        return Generate.generate(prompt);
                                    } catch (OllamaBaseException | IOException | InterruptedException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                }).thenAccept(result -> {
                                    context.getSource().getServer().execute(() -> {
                                        if (result != null) {
                                            context.getSource().sendFeedback(() -> Text.of(result.getResponse()), true);
                                        } else {
                                            context.getSource().sendError(Text.of("faild to use ai"));
                                        }
                                    });
                                });
                                
                                return 1;
                            } catch (Exception e) {
                                context.getSource().sendError(Text.of("error: " + e.getMessage()));
                                return 0;
                            }
                        })
                    )
            );
        });
    }
}
