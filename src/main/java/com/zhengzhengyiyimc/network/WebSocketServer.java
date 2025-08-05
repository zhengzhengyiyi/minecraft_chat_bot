package com.zhengzhengyiyimc.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketServer {
    private final HttpServer server;
    public static final Logger LOGGER = LoggerFactory.getLogger("WebSocketServer");
    private final Map<String, WebSocketConnection> connections = new HashMap<>();

    public WebSocketServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/ws", new WsHandler());
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        WebSocketServer.LOGGER.info("ws server started on port " + server.getAddress().getPort());
    }

    public void broadcast(String message) {
        connections.values().forEach(conn -> conn.send(message));
    }

    private class WsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleUpgrade(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }

        private void handleUpgrade(HttpExchange exchange) throws IOException {
            String key = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
            if (key == null) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            String responseKey = generateKey(key);
            exchange.getResponseHeaders().set("Upgrade", "websocket");
            exchange.getResponseHeaders().set("Connection", "Upgrade");
            exchange.getResponseHeaders().set("Sec-WebSocket-Accept", responseKey);
            exchange.sendResponseHeaders(101, -1);

            WebSocketConnection conn = new WebSocketConnection(exchange);
            connections.put(conn.id, conn);
            CompletableFuture.runAsync(conn::startReading);
        }

        private String generateKey(String key) {
            String guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            String combined = key + guid;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] hash = md.digest(combined.getBytes());
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class WebSocketConnection {
        private final HttpExchange exchange;
        private final String id = java.util.UUID.randomUUID().toString();
        private volatile boolean open = true;

        public WebSocketConnection(HttpExchange exchange) {
            this.exchange = exchange;
        }

        public void send(String text) {
            if (!open) return;
            byte[] payload = toWebSocketFrame(text.getBytes());
            try {
                exchange.getResponseBody().write(payload);
                exchange.getResponseBody().flush();
            } catch (IOException e) {
                close();
            }
        }

        public void startReading() {
            try {
                while (open) {
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int read = exchange.getRequestBody().read(buffer.array());
                    if (read <= 0) break;
                    buffer.limit(read);
                    String message = extractTextFromFrame(buffer);
                    if (message != null) {
                        WebSocketServer.LOGGER.info("Received: " + message);
                        send("Echo: " + message);
                    }
                }
            } catch (IOException e) {
            } finally {
                close();
            }
        }

        private void close() {
            open = false;
            connections.remove(id);
            try { exchange.close(); } catch (Exception e) { }
        }

        private byte[] toWebSocketFrame(byte[] data) {
            int size = data.length;
            byte[] header;
            if (size <= 125) {
                header = new byte[]{(byte) 0x81, (byte) size};
            } else if (size <= 65535) {
                header = new byte[]{(byte) 0x81, (byte) 126, (byte) (size >> 8), (byte) (size & 0xFF)};
            } else {
                header = new byte[]{(byte) 0x81, (byte) 127, 0, 0, 0, 0, (byte) (size >> 24), (byte) (size >> 16), (byte) (size >> 8), (byte) (size & 0xFF)};
            }
            byte[] frame = new byte[header.length + size];
            System.arraycopy(header, 0, frame, 0, header.length);
            System.arraycopy(data, 0, frame, header.length, size);
            return frame;
        }

        private String extractTextFromFrame(ByteBuffer buffer) {
            byte b = buffer.get();
            @SuppressWarnings("unused")
            boolean isFinal = (b & 0x80) != 0;

            byte opcode = (byte) (b & 0x0F);
            if (opcode == 0x8) return null;
            if (opcode == 0x9) return null;
            if (opcode != 0x1) return null;
            b = buffer.get();
            boolean hasMask = (b & 0x80) != 0;
            int length = b & 0x7F;
            if (length == 126) length = ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
            if (length == 127) {
                for (int i = 0; i < 8; i++) buffer.get();
                length = (int) (buffer.get() & 0xFF);
            }
            byte[] mask = hasMask ? new byte[]{buffer.get(), buffer.get(), buffer.get(), buffer.get()} : null;
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                byte d = buffer.get();
                if (hasMask) d = (byte) (d ^ mask[i % 4]);
                data[i] = d;
            }
            return new String(data);
        }
    }
}
