package com.zhengzhengyiyimc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;

public class PythonHandler {
    public static String runPythonAiScript(String scriptName, String version, String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        Thread workerThread = new Thread(() -> {
            Process process = null;
            try {
                Path scriptPath = FabricLoader.getInstance().getGameDir()
                    .resolve("mods/minecraft_chatbot/scripts/" + scriptName);
                    // .resolve("resources/assets/minecraft_chatbot/scripts/" + scriptName);

                if (!PythonScriptManager.checkScriptExists(scriptPath)) {
                    PythonScriptManager.ensurePythonScriptExists(scriptPath);

                    future.complete(null);
                }
                
                process = new ProcessBuilder("python", scriptPath.toString(), 
                    "--version", version, "--prompt", prompt)
                    .redirectErrorStream(true)
                    .start();

                new Thread(() -> {
                    try {
                        Process pipInstall = new ProcessBuilder("pip", "install", "ollama")
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start();
                        pipInstall.waitFor();
                    } catch (Exception e) {
                        System.err.println("error at install dependency \"ollama\": " + e.getMessage());
                    }
                }).start();

                StringBuilder output = new StringBuilder();
                try (InputStream inputStream = process.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    
                    char[] buffer = new char[1024];
                    int bytesRead;
                    while ((bytesRead = reader.read(buffer)) != -1) {
                        output.append(buffer, 0, bytesRead);

                        System.out.println("[Python] " + new String(buffer, 0, bytesRead));
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    future.complete(output.toString());
                } else {
                    future.completeExceptionally(new RuntimeException("exception: " + exitCode));
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        });
        
        workerThread.start();

        try {
            String result = future.get();
            System.out.println("finished: " + result);
            return result;
        } catch (Exception e) {
            Minecraft_chatbot.LOGGER.error("on error" + e.getMessage());
            workerThread.interrupt();
            return "Error: " + e.getMessage();
        }
    }
}