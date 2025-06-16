import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Professional Real-Time Chat Logger with WebSocket Support
 * No simulation - pure user-initiated chat system
 */
public class ProfessionalChatLogger {
    
    private static final int WEBSOCKET_PORT = 8081;
    private static ServerSocket webSocketServer;
    private static final Set<WebSocketClient> connectedClients = ConcurrentHashMap.newKeySet();
    private static ExecutorService webSocketExecutor;
    
    // Thread-safe file writing
    private static final String LOG_FILE = "chat_logs.txt";
    private static final ReentrantLock fileLock = new ReentrantLock();
    private static FileWriter logWriter;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static volatile boolean isRunning = true;
    
    /**
     * WebSocket client representation
     */
    static class WebSocketClient {
        private final Socket socket;
        private final OutputStream out;
        private final InputStream in;
        private final String clientId;
        private String username;
        
        public WebSocketClient(Socket socket) throws IOException {
            this.socket = socket;
            this.out = socket.getOutputStream();
            this.in = socket.getInputStream();
            this.clientId = UUID.randomUUID().toString().substring(0, 8);
            this.username = "User" + clientId;
        }
        
        public void sendMessage(String message) {
            try {
                sendWebSocketFrame(message.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("Error sending message to client " + clientId + ": " + e.getMessage());
            }
        }
        
        private void sendWebSocketFrame(byte[] data) throws IOException {
            // Simple WebSocket text frame (FIN=1, opcode=1)
            out.write(0x81);
            
            // Payload length
            if (data.length < 126) {
                out.write(data.length);
            } else if (data.length < 65536) {
                out.write(126);
                out.write((data.length >> 8) & 0xFF);
                out.write(data.length & 0xFF);
            } else {
                out.write(127);
                for (int i = 7; i >= 0; i--) {
                    out.write((data.length >> (8 * i)) & 0xFF);
                }
            }
            
            // Payload data
            out.write(data);
            out.flush();
        }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getClientId() { return clientId; }
        
        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client " + clientId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * WebSocket server
     */
    static class WebSocketServer implements Runnable {
        @Override
        public void run() {
            try {
                webSocketServer = new ServerSocket(WEBSOCKET_PORT);
                logMessage("SYSTEM", "WebSocket server started on port " + WEBSOCKET_PORT);
                
                while (isRunning) {
                    try {
                        Socket clientSocket = webSocketServer.accept();
                        webSocketExecutor.submit(new ClientHandler(clientSocket));
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Error accepting client: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error starting WebSocket server: " + e.getMessage());
            }
        }
    }
    
    /**
     * Client handler with proper WebSocket handshake
     */
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            WebSocketClient client = null;
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                
                // Read HTTP headers
                Map<String, String> headers = new HashMap<>();
                String line;
                
                // Read request line
                String requestLine = in.readLine();
                if (requestLine == null || !requestLine.contains("HTTP/1.1")) {
                    clientSocket.close();
                    return;
                }
                
                // Read headers
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String key = line.substring(0, colonIndex).trim().toLowerCase();
                        String value = line.substring(colonIndex + 1).trim();
                        headers.put(key, value);
                    }
                }
                
                // Check for WebSocket upgrade
                String upgrade = headers.get("upgrade");
                String connection = headers.get("connection");
                String webSocketKey = headers.get("sec-websocket-key");
                
                if ("websocket".equalsIgnoreCase(upgrade) && 
                    connection != null && connection.toLowerCase().contains("upgrade") &&
                    webSocketKey != null) {
                    
                    // Generate accept key
                    String acceptKey = generateWebSocketAcceptKey(webSocketKey);
                    
                    // Send handshake response
                    out.write("HTTP/1.1 101 Switching Protocols\r\n");
                    out.write("Upgrade: websocket\r\n");
                    out.write("Connection: Upgrade\r\n");
                    out.write("Sec-WebSocket-Accept: " + acceptKey + "\r\n");
                    out.write("\r\n");
                    out.flush();
                    
                    // Create client and handle messages
                    client = new WebSocketClient(clientSocket);
                    connectedClients.add(client);
                    
                    logMessage("SYSTEM", "Client connected: " + client.getClientId() + " (Total: " + connectedClients.size() + ")");
                    
                    handleWebSocketMessages(client);
                } else {
                    // Not a WebSocket request
                    out.write("HTTP/1.1 400 Bad Request\r\n\r\n");
                }
                
            } catch (Exception e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                if (client != null) {
                    connectedClients.remove(client);
                    logMessage("SYSTEM", "Client disconnected: " + client.getClientId() + " (" + client.getUsername() + ") (Total: " + connectedClients.size() + ")");
                    client.close();
                }
            }
        }
        
        private void handleWebSocketMessages(WebSocketClient client) {
            try {
                InputStream is = clientSocket.getInputStream();
                
                while (isRunning && !clientSocket.isClosed()) {
                    // Read frame header
                    int firstByte = is.read();
                    if (firstByte == -1) break;
                    
                    int secondByte = is.read();
                    if (secondByte == -1) break;
                    
                    // Check if frame is masked (from client)
                    boolean masked = (secondByte & 0x80) != 0;
                    int payloadLength = secondByte & 0x7F;
                    
                    // Read extended payload length if needed
                    if (payloadLength == 126) {
                        int byte1 = is.read();
                        int byte2 = is.read();
                        payloadLength = (byte1 << 8) | byte2;
                    } else if (payloadLength == 127) {
                        // Skip 8 bytes for simplicity (we don't expect huge messages)
                        for (int i = 0; i < 8; i++) is.read();
                        payloadLength = 0; // Reset to avoid issues
                        continue;
                    }
                    
                    // Read mask if present
                    byte[] mask = new byte[4];
                    if (masked) {
                        for (int i = 0; i < 4; i++) {
                            mask[i] = (byte) is.read();
                        }
                    }
                    
                    // Read payload
                    if (payloadLength > 0 && payloadLength < 4096) { // Reasonable size limit
                        byte[] payload = new byte[payloadLength];
                        int bytesRead = 0;
                        while (bytesRead < payloadLength) {
                            int read = is.read(payload, bytesRead, payloadLength - bytesRead);
                            if (read == -1) break;
                            bytesRead += read;
                        }
                        
                        // Unmask payload
                        if (masked) {
                            for (int i = 0; i < payloadLength; i++) {
                                payload[i] ^= mask[i % 4];
                            }
                        }
                        
                        String message = new String(payload, StandardCharsets.UTF_8);
                        handleMessage(client, message);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading WebSocket messages: " + e.getMessage());
            }
        }
        
        private void handleMessage(WebSocketClient client, String message) {
            try {
                // Simple JSON parsing
                String username = extractJsonValue(message, "username");
                String messageText = extractJsonValue(message, "message");
                
                if (username != null && messageText != null) {
                    client.setUsername(username);
                    logMessage(username, messageText, client.getClientId());
                    // Broadcast to all OTHER clients (not the sender)
                    broadcastMessage(username, messageText, client.getClientId());
                }
            } catch (Exception e) {
                System.err.println("Error handling message: " + e.getMessage());
            }
        }
        
        private String extractJsonValue(String json, String key) {
            try {
                Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(json);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                System.err.println("Error parsing JSON: " + e.getMessage());
            }
            return null;
        }
        
        private String generateWebSocketAcceptKey(String webSocketKey) {
            try {
                String concatenated = webSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                byte[] hash = sha1.digest(concatenated.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                System.err.println("Error generating WebSocket accept key: " + e.getMessage());
                return "";
            }
        }
    }
    
    /**
     * Broadcast message to all connected clients except sender
     */
    public static void broadcastMessage(String username, String message, String senderClientId) {
        String jsonMessage = String.format(
            "{\"type\":\"message\",\"username\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            username, message.replace("\"", "\\\""), LocalDateTime.now().format(TIME_FORMATTER)
        );
        
        Iterator<WebSocketClient> iterator = connectedClients.iterator();
        while (iterator.hasNext()) {
            WebSocketClient client = iterator.next();
            if (!client.getClientId().equals(senderClientId)) {
                try {
                    client.sendMessage(jsonMessage);
                } catch (Exception e) {
                    System.err.println("Error broadcasting to client: " + e.getMessage());
                    iterator.remove();
                    client.close();
                }
            }
        }
    }
    
    /**
     * Thread-safe message logging
     */
    public static void logMessage(String username, String message) {
        logMessage(username, message, null);
    }
    
    public static void logMessage(String username, String message, String senderClientId) {
        fileLock.lock();
        try {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String logEntry = String.format("[%s] %s: %s%n", timestamp, username, message);
            
            // Write to file
            if (logWriter != null) {
                logWriter.write(logEntry);
                logWriter.flush();
            }
            
            // Display to console
            System.out.print(logEntry);
            
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Initialize logger
     */
    private static void initializeLogger() throws IOException {
        logWriter = new FileWriter(LOG_FILE, true);
        logMessage("SYSTEM", "=== Professional Chat session started ===");
    }
    
    /**
     * Close logger
     */
    private static void closeLogger() {
        fileLock.lock();
        try {
            if (logWriter != null) {
                logMessage("SYSTEM", "=== Chat session ended ===");
                logWriter.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing log file: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== Professional Real-Time Chat Logger Started ===");
            System.out.println("Log file: " + LOG_FILE);
            System.out.println("WebSocket server: ws://localhost:" + WEBSOCKET_PORT);
            System.out.println("Press Ctrl+C to stop");
            System.out.println("Waiting for clients to connect...");
            System.out.println();
            
            // Initialize logger
            initializeLogger();
            
            // Start WebSocket server
            webSocketExecutor = Executors.newCachedThreadPool();
            webSocketExecutor.submit(new WebSocketServer());
            
            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down gracefully...");
                isRunning = false;
                
                // Close server
                if (webSocketServer != null) {
                    try {
                        webSocketServer.close();
                    } catch (IOException e) {
                        System.err.println("Error closing server: " + e.getMessage());
                    }
                }
                
                // Close all clients
                connectedClients.forEach(WebSocketClient::close);
                
                // Shutdown executor
                webSocketExecutor.shutdown();
                
                try {
                    if (!webSocketExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                        webSocketExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    webSocketExecutor.shutdownNow();
                }
                
                closeLogger();
                System.out.println("Chat logger stopped.");
            }));
            
            // Keep the server running indefinitely
            // The server will only stop when interrupted (Ctrl+C)
            while (isRunning) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeLogger();
            System.out.println("Server shutdown complete.");
        }
    }
}