package com.zhengzhengyiyimc.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class WebSocketClient {
    private final String uri;
    private final MessageHandler messageHandler;
    private WebSocket webSocket;

    public interface MessageHandler {
        void onMessage(String message);
        void onOpen();
        void onError(String error);
        void onClose(int code, String reason);
    }

    public WebSocketClient(String uri, MessageHandler handler) {
        this.uri = uri;
        this.messageHandler = handler;
    }

    public void start() {
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> future = client.newWebSocketBuilder()
            .buildAsync(URI.create(uri), new ClientListener());
        future.whenComplete((webSocket, throwable) -> {
            if (throwable != null) {
                messageHandler.onError(throwable.getMessage());
            } else {
                this.webSocket = webSocket;
                System.out.println("WebSocket connected to: " + uri);
            }
        });
    }

    public void sendMessage(String message) {
        if (webSocket != null && !webSocket.isOutputClosed()) {
            webSocket.sendText(message, true);
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "normal close");
        }
    }

    private class ClientListener implements Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            messageHandler.onOpen();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageHandler.onMessage(data.toString());
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            messageHandler.onError(error.getMessage());
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            messageHandler.onClose(statusCode, reason);
            return CompletableFuture.completedFuture(null);
        }
    }
}