# TAO Discord Bot

Un bot Discord intelligente che utilizza AI per conversazioni naturali, implementato con architettura a layer seguendo le best practices Java enterprise.

## 🚀 Caratteristiche

- **Conversazioni AI** tramite OpenRouter
- **Persistenza MongoDB** in tempo reale
- **Architettura Clean** a layer (Domain-Driven Design)
- **Gestione messaggi lunghi** con suddivisione automatica
- **Retry intelligente** per chiamate API
- **Logging strutturato** con MDC
- **Configurazione flessibile** tramite variabili d'ambiente

## 🏗️ Architettura

Il progetto segue i principi di **Clean Architecture** e **Domain-Driven Design** con separazione in layer:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ├── listener/DiscordMessageListener.java                  │
│  └── Gestione eventi Discord e interfaccia utente          │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│  ├── service/ChatBotApplicationService.java                │
│  └── Orchestrazione use case e coordinamento layer         │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│  ├── model/ConversationMessage.java                        │
│  ├── repository/ConversationMessageRepository.java         │
│  ├── service/ConversationDomainService.java                │
│  └── Logica di business e regole del dominio               │
├─────────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                       │
│  ├── client/OpenRouterClient.java                          │
│  ├── repository/MongoConversationMessageRepository.java    │
│  ├── repository/ConversationMessageRepositoryAdapter.java  │
│  └── Integrazione servizi esterni e persistenza           │
├─────────────────────────────────────────────────────────────┤
│                   Configuration Layer                       │
│  ├── JdaConfiguration.java                                 │
│  ├── WebClientConfiguration.java                           │
│  └── AppProperties.java                                    │
└─────────────────────────────────────────────────────────────┘
```

### Flusso delle Dipendenze

```
Presentation → Application → Domain ← Infrastructure
                                ↑
                         Configuration
```

## 🛠️ Tecnologie

- **Java 17+**
- **Spring Boot 3.x**
- **Spring WebFlux** (Reactive)
- **MongoDB** (Reactive)
- **Discord JDA**
- **OpenRouter AI API**
- **Gson** per JSON parsing
- **SLF4J + Logback** per logging

## 📋 Prerequisiti

- Java 17 o superiore
- MongoDB in esecuzione
- Token Discord Bot
- API Key OpenRouter

## 🔧 Configurazione

### 1. Variabili d'Ambiente

Crea un file `.env` o imposta le seguenti variabili:

```bash
# Discord Bot Configuration
DISCORD_BOT_TOKEN=your_discord_bot_token_here

# OpenRouter AI Configuration
OPENROUTER_API_KEY=your_openrouter_api_key_here
OPENROUTER_MODEL_NAME=deepseek/deepseek-chat-v3.1:free

# MongoDB Configuration
MONGODB_URI=mongodb://localhost:27017/test01_tao
MONGO_ENABLED=true

# Logging Configuration
LOG_FILE_PATH=./logs/test01-tao.log
LOG_FILE_MAX_SIZE=10MB
LOG_FILE_MAX_HISTORY=30
```

### 2. Configurazione MongoDB

Assicurati che MongoDB sia in esecuzione:

```bash
# Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest

# Locale
mongod --dbpath /path/to/data
```

### 3. Configurazione Discord Bot

1. Vai su [Discord Developer Portal](https://discord.com/developers/applications)
2. Crea una nuova applicazione
3. Vai su "Bot" e crea un bot
4. Copia il token e impostalo in `DISCORD_BOT_TOKEN`
5. Abilita i seguenti **Privileged Gateway Intents**:
   - Message Content Intent
6. Invita il bot al server con le autorizzazioni:
   - Read Messages
   - Send Messages
   - Read Message History

## 🚀 Avvio

### Sviluppo

```bash
# Clona il repository
git clone <repository-url>
cd test01_TAO.io

# Imposta le variabili d'ambiente
cp .env.example .env
# Modifica .env con i tuoi valori

# Avvia l'applicazione
./mvnw spring-boot:run
```

### Produzione

```bash
# Build dell'applicazione
./mvnw clean package

# Avvia con Java
java -jar target/test01-tao-*.jar
```

### Docker

```bash
# Build dell'immagine
docker build -t tao-discord-bot .

# Avvia il container
docker run -d --name tao-bot \
  -e DISCORD_BOT_TOKEN=your_token \
  -e OPENROUTER_API_KEY=your_key \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/test01_tao \
  tao-discord-bot
```

## 📖 Utilizzo

### Comandi Discord

Il bot risponde quando viene **menzionato** in un canale:

```
@TaoBot Ciao, come stai?
@TaoBot Spiegami la fisica quantistica
@TaoBot Aiutami con questo codice Python
```

### Funzionalità

- **Conversazioni contestuali**: Il bot mantiene la cronologia per canale
- **Messaggi lunghi**: Gestione automatica di risposte oltre 2000 caratteri
- **Gestione errori**: Retry automatico e messaggi di errore user-friendly
- **Persistenza**: Tutte le conversazioni sono salvate su MongoDB

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

I log sono disponibili in:
- **Console**: Output strutturato per sviluppo
- **File**: `./logs/test01-tao.log` con rotazione automatica

## 🧪 Testing

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# Test con profilo specifico
./mvnw test -Dspring.profiles.active=test
```

## 🔧 Configurazioni Avanzate

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

### Limiti

- **Token Context**: 120,000 token massimi per conversazione
- **Message Length**: 2000 caratteri per messaggio Discord
- **Rate Limiting**: Gestito automaticamente da JDA e OpenRouter

### Ottimizzazioni

- **Reactive Streams**: Non-blocking I/O per scalabilità
- **Connection Pooling**: Riutilizzo connessioni MongoDB e HTTP
- **Memory Management**: Gestione automatica memoria conversazioni

## 🤝 Contribuire

1. Fork del repository
2. Crea un branch feature (`git checkout -b feature/nuova-funzionalita`)
3. Commit delle modifiche (`git commit -am 'Aggiunge nuova funzionalità'`)
4. Push del branch (`git push origin feature/nuova-funzionalita`)
5. Crea una Pull Request

## 📄 Licenza

Questo progetto è sotto licenza MIT. Vedi il file `LICENSE` per dettagli.

## 🆘 Supporto

- **Issues**: [GitHub Issues](../../issues)
- **Documentation**: [Wiki](../../wiki)
- **Discord**: [Support Server](link-to-discord)

## 📝 Changelog

### v2.0.0 - 2025-09-19
- ✨ **Nuova architettura a layer** (Clean Architecture)
- ✨ **Domain-Driven Design** implementation
- ✨ **Reactive MongoDB** integration
- 🔧 **Migliorata gestione errori**
- 🔧 **Logging strutturato** con MDC
- 🚀 **Performance** ottimizzate

### v1.0.0 - Initial Release
- ✨ Bot Discord base
- ✨ Integrazione OpenRouter
- ✨ Persistenza file (deprecated)

---

**Sviluppato con ❤️ per la community Discord**
