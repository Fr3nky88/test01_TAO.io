# TAO Discord Bot

An intelligent Discord bot that uses AI for natural conversations, implemented with a layered architecture following Java enterprise best practices.

## 🚀 Features

- **AI Conversations** via OpenRouter
- **Real-time MongoDB Persistence**
- **Clean Architecture** with layers (Domain-Driven Design)
- **Long Message Handling** with automatic splitting
- **Smart Retry** for API calls
- **Structured Logging** with MDC
- **Flexible Configuration** via environment variables

## 🏗️ Architecture

The project follows the principles of **Clean Architecture** and **Domain-Driven Design** with a clear separation into layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ├── listener/DiscordMessageListener.java                  │
│  └── Handles Discord events and user interface             │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│  ├── service/ChatBotApplicationService.java                │
│  └── Orchestrates use cases and coordinates layers         │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│  ├── model/ConversationMessage.java                        │
│  ├── repository/ConversationMessageRepository.java         │
│  ├── service/ConversationDomainService.java                │
│  └── Contains business logic and domain rules              │
├─────────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                       │
│  ├── client/OpenRouterClient.java                          │
│  ├── repository/MongoConversationMessageRepository.java    │
│  ├── repository/ConversationMessageRepositoryAdapter.java  │
│  └── Integrates with external services and handles persistence │
├─────────────────────────────────────────────────────────────┤
│                   Configuration Layer                       │
│  ├── JdaConfiguration.java                                 │
│  ├── WebClientConfiguration.java                           │
│  └── AppProperties.java                                    │
└─────────────────────────────────────────────────────────────┘
```

### Dependency Flow

```
Presentation → Application → Domain ← Infrastructure
                                ↑
                         Configuration
```

## 🛠️ Technologies

- **Java 17+**
- **Spring Boot 3.x**
- **Spring WebFlux** (Reactive)
- **MongoDB** (Reactive)
- **Discord JDA**
- **OpenRouter AI API**
- **Gson** for JSON parsing
- **SLF4J + Logback** for logging

## 📋 Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Discord Bot Token
- OpenRouter API Key

## 🔧 Configuration

### 1. Environment Variables

Create a `.env` file in the root of the project with the following content:

```bash
# Discord Bot Configuration
DISCORD_BOT_TOKEN=your_discord_bot_token_here

# OpenRouter AI Configuration
OPENROUTER_API_KEY=your_openrouter_api_key_here
OPENROUTER_MODEL_NAME=deepseek/deepseek-chat-v3.1:free

# MongoDB Configuration
MONGODB_URI=mongodb://mongodb:27017/test01_tao
MONGO_ENABLED=true

# Logging Configuration
LOG_FILE_PATH=./logs/test01-tao.log
LOG_FILE_MAX_SIZE=10MB
LOG_FILE_MAX_HISTORY=30
```

### 2. Docker Compose

A `docker-compose.yml` file is provided to run the application and a MongoDB database.

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: test01-tao-app
    restart: unless-stopped
    env_file:
      - .env
    ports:
      - "8080:8080"
    depends_on:
      - mongodb
    networks:
      - tao-network

  mongodb:
    image: mongo:latest
    container_name: test01-tao-mongodb
    restart: unless-stopped
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db
    networks:
      - tao-network

networks:
  tao-network:
    driver: bridge

volumes:
  mongo-data:
```

## 🚀 Running the Application

1.  **Build the project:**
    ```bash
    ./mvnw clean install
    ```

2.  **Run with Docker Compose:**
    Make sure your `.env` file is configured correctly, then run:
    ```bash
    docker-compose up --build
    ```

The bot will start, connect to Discord, and be ready to receive messages.

## 📖 Usage

### Discord Commands

The bot responds when mentioned in a channel:

```
@TaoBot Hi, how are you?
@TaoBot Explain quantum physics to me
@TaoBot Help me with this Python code
```

### Features

- **Contextual Conversations**: The bot maintains history per channel
- **Long Messages**: Automatic handling of responses over 2000 characters
- **Error Management**: Automatic retry and user-friendly error messages
- **Persistence**: All conversations are saved to MongoDB

## 🔍 Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

### Logs

Logs are available in:
- **Console**: Structured output for development
- **File**: `./logs/test01-tao.log` with automatic rotation

## 🧪 Testing

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# Test with specific profile
./mvnw test -Dspring.profiles.active=test
```

## 🔧 Advanced Configurations

### Tuning OpenRouter

```properties
# Retry configuration
openrouter.retry.max-attempts=3
openrouter.retry.base-delay=1000

# Model selection
openrouter.model.name=deepseek/deepseek-chat-v3.1:free
```

### Tuning Discord

```properties
# Connection tuning
discord.retry.max-attempts=3
discord.connection.timeout=30000
discord.read.timeout=60000
```

### Tuning MongoDB

```properties
# Connection pool
spring.data.mongodb.option.max-connection-pool-size=20
spring.data.mongodb.option.min-connection-pool-size=5
```

## 📊 Performance

### Limits

- **Token Context**: 120,000 max tokens per conversation
- **Message Length**: 2000 characters per Discord message
- **Rate Limiting**: Automatically managed by JDA and OpenRouter

### Optimizations

- **Reactive Streams**: Non-blocking I/O for scalability
- **Connection Pooling**: Reuse MongoDB and HTTP connections
- **Memory Management**: Automatic memory management for conversations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## 🆘 Support

- **Issues**: [GitHub Issues](../../issues)
- **Documentation**: [Wiki](../../wiki)
- **Discord**: [Support Server](link-to-discord)

## 📝 Changelog

### v2.0.0 - 2025-09-19
- ✨ **New layered architecture** (Clean Architecture)
- ✨ **Domain-Driven Design** implementation
- ✨ **Reactive MongoDB** integration
- 🔧 **Improved error handling**
- 🔧 **Structured logging** with MDC
- 🚀 **Optimized performance**

### v1.0.0 - Initial Release
- ✨ Basic Discord bot
- ✨ OpenRouter integration
- ✨ File persistence (deprecated)

---

**Developed with ❤️ for the Discord community**
