 # Professional Chat Logger

A modern, real-time chat application with comprehensive logging capabilities built with Java WebSocket backend and a beautiful HTML/CSS/JavaScript frontend.

## 🚀 Features

### Backend Features
- **Real-time WebSocket Communication**: Multi-threaded WebSocket server supporting concurrent connections
- **Thread-safe Message Logging**: All messages are logged to `chat_logs.txt` with timestamps
- **Professional WebSocket Implementation**: Full RFC 6455 compliant WebSocket handshake and frame handling
- **Concurrent Client Management**: Handles multiple users simultaneously with proper connection lifecycle management
- **Graceful Shutdown**: Clean server shutdown with proper resource cleanup
- **Message Broadcasting**: Messages are broadcast to all connected clients except the sender

### Frontend Features
- **Modern UI/UX**: Beautiful, responsive design with dark/light mode support
- **Real-time Messaging**: Instant message delivery and display
- **Connection Status Monitoring**: Visual indicators for connection status
- **Message Export**: Export chat logs as text files
- **Quick Messages**: Pre-defined quick message buttons
- **Session Statistics**: Live tracking of messages, users, and session time
- **Typing Indicators**: Visual feedback for active conversations
- **Automatic Reconnection**: Attempts to reconnect on connection loss

## 📋 Prerequisites

- **Java**: JDK 8 or higher
- **Web Browser**: Modern browser with WebSocket support (Chrome, Firefox, Safari, Edge)
- **Network**: Localhost access (default port: 8081)

## 🛠️ Installation & Setup

### 1. Compile the Backend

```bash
# Navigate to the project directory
cd /path/to/your/project

# Compile the Java file
javac ProfessionalChatLogger.java

# Run the server
java ProfessionalChatLogger
```

### 2. Start the Frontend

Simply open the `index.html` file in your web browser:

```bash
# Open directly in browser
open index.html

```

## 🏃‍♂️ Usage

### Starting the System

1. **Start the Backend Server**:
   ```bash
   java ProfessionalChatLogger
   ```
   You should see:
   ```
   === Professional Real-Time Chat Logger Started ===
   Log file: chat_logs.txt
   WebSocket server: ws://localhost:8081
   Press Ctrl+C to stop
   Waiting for clients to connect...
   ```

2. **Open the Frontend**:
   - Open `index.html` in one or more browser windows/tabs
   - Each window represents a different user

3. **Start Chatting**:
   - Enter your name in the "Your name" field
   - Type your message and press Enter or click Send
   - Messages will appear in real-time across all connected clients

### Key Controls

- **Send Message**: Enter key or Send button
- **Clear Chat**: Trash icon in chat header
- **Export Messages**: Export button in header
- **Dark Mode**: Moon/Sun toggle in header
- **Quick Messages**: Pre-defined message buttons in sidebar

## 🏗️ Architecture

### Backend Architecture

```
ProfessionalChatLogger.java
├── WebSocketServer (Thread)
│   ├── Accepts client connections
│   └── Spawns ClientHandler threads
├── ClientHandler (Thread per client)
│   ├── WebSocket handshake
│   ├── Message parsing
│   └── Frame handling
├── WebSocketClient (Client representation)
│   ├── Connection management
│   └── Message sending
└── Thread-safe logging system
```

### Frontend Architecture

```
index.html
├── ProfessionalChatUI (Main class)
│   ├── WebSocket connection management
│   ├── Message handling
│   ├── UI state management
│   └── Statistics tracking
├── Responsive design system
├── Dark mode support
└── Real-time updates
```

## 📁 File Structure

```
professional-chat-logger/
├── ProfessionalChatLogger.java    # Backend WebSocket server
├── index.html                     # Frontend application
├── chat_logs.txt                  # Generated log file
└── README.md                      # This file
```

## 🔧 Configuration

### Backend Configuration

Edit these constants in `ProfessionalChatLogger.java`:

```java
private static final int WEBSOCKET_PORT = 8081;        // Server port
private static final String LOG_FILE = "chat_logs.txt"; // Log file name
```

### Frontend Configuration

The frontend automatically connects to `ws://localhost:8081`. To change the server address, modify the WebSocket URL in the `connectWebSocket()` method:

```javascript
this.websocket = new WebSocket('ws://your-server:8081');
```

## 📊 Message Format

Messages are exchanged in JSON format:

### Client to Server
```json
{
    "type": "message",
    "username": "JohnDoe",
    "message": "Hello everyone!"
}
```

### Server to Client
```json
{
    "type": "message",
    "username": "JohnDoe",
    "message": "Hello everyone!",
    "timestamp": "2024-01-15 10:30:45.123"
}
```

## 📝 Log File Format

Chat logs are saved in the following format:

```
[2024-01-15 10:30:45.123] SYSTEM: === Professional Chat session started ===
[2024-01-15 10:30:52.456] SYSTEM: Client connected: abc12345 (Total: 1)
[2024-01-15 10:31:05.789] JohnDoe: Hello everyone!
[2024-01-15 10:31:12.012] JaneSmith: Hi John! How are you?
```

## 🐛 Troubleshooting

### Common Issues

1. **Connection Failed**
   - Ensure the Java server is running
   - Check if port 8081 is available
   - Verify firewall settings

2. **Messages Not Appearing**
   - Check browser console for WebSocket errors
   - Ensure both username and message fields are filled
   - Verify server is accepting connections

3. **Server Won't Start**
   - Check if port 8081 is already in use
   - Ensure Java is properly installed
   - Verify file permissions for log file creation

### Debug Mode

Enable debug logging by checking the browser console (F12) for detailed connection and message information.

## 🔒 Security Considerations

- **Local Network Only**: Currently configured for localhost only
- **No Authentication**: Users can set any username
- **No Message Validation**: Messages are not sanitized (suitable for trusted environments)
- **Plaintext Logging**: Messages are logged in plaintext

For production use, consider implementing:
- User authentication
- Message encryption
- Input validation and sanitization
- Rate limiting
- SSL/TLS support

## 🎨 Customization

### Styling
The frontend uses Tailwind CSS classes. Modify the HTML classes to change the appearance:
- Colors: Change `bg-blue-500` to `bg-green-500` for different color schemes
- Layout: Adjust grid classes for different layouts
- Animations: Modify animation classes for different effects

### Backend Modifications
- **Port Change**: Modify `WEBSOCKET_PORT` constant
- **Log Format**: Customize the `logMessage()` method
- **Message Processing**: Extend the `handleMessage()` method

## 📈 Performance

### Backend Performance
- Supports multiple concurrent connections
- Thread-safe message broadcasting
- Efficient WebSocket frame handling
- Minimal memory footprint per connection

### Frontend Performance
- Lightweight vanilla JavaScript (no frameworks)
- Efficient DOM updates
- Automatic reconnection on failure
- Responsive design for all screen sizes

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is released under the MIT License. Feel free to use, modify, and distribute as needed.

## 🆘 Support

For issues and questions:
1. Check the troubleshooting section
2. Review browser console for errors
3. Ensure server logs don't show connection issues
4. Verify all prerequisites are met

## 🔮 Future Enhancements

Potential improvements:
- [ ] User authentication system
- [ ] Private messaging
- [ ] File sharing capabilities
- [ ] Message encryption
- [ ] Database integration
- [ ] Mobile app support
- [ ] Voice/video chat integration
- [ ] Message reactions and emojis
- [ ] Chat rooms/channels
- [ ] Message history persistence

---

**Built using Java and modern web technologies**
