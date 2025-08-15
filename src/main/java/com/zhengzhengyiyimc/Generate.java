package com.zhengzhengyiyimc;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.response.OllamaResult;

import java.io.IOException;

public class Generate {
    public static OllamaResult generate(String prompt) throws OllamaBaseException, IOException, InterruptedException {

        String host = "http://localhost:11434/";

        OllamaAPI ollamaAPI = new OllamaAPI(host);

        OllamaResult result =
                ollamaAPI.generate("Sweaterdog/Andy-4:micro-q3_k_m", prompt, null);

        return result;
    }
}
