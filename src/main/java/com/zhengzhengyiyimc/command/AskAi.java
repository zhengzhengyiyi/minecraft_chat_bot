package com.zhengzhengyiyimc.command;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.zhengzhengyiyimc.PythonHandler;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

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

                                CompletableFuture.runAsync(() -> {
                                    String ai_message = PythonHandler.runPythonAiScript("asking_ai.py", version, prompt);
                                    context.getSource().sendMessage(Text.of(ai_message));
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
