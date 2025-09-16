# test01_TAO.io

Applicazione Spring Boot che integra un bot Discord con funzionalità di AI tramite OpenRouter. Il bot risponde ai messaggi su Discord utilizzando modelli di intelligenza artificiale configurabili tramite variabili d'ambiente. Include sistema di logging avanzato, backup automatici e persistenza delle conversazioni.

## Funzionalità principali
- **Bot Discord intelligente**: Risponde quando menzionato direttamente o tramite ruoli
- **Integrazione OpenRouter AI**: Supporto per modelli AI configurabili
- **Cronologia conversazioni**: Persistenza automatica delle conversazioni per canale
- **Logging dettagliato**: Sistema di log completo con rotazione automatica
- **Backup automatici**: Backup periodici della cronologia con pulizia automatica
- **Monitoring**: Endpoint Actuator per monitoraggio stato applicazione
- **Deploy Docker**: Containerizzazione completa con volumi persistenti

## Architettura e Componenti

### Gestione Conversazioni
- Cronologia separata per ogni canale Discord
- Gestione automatica del limite di token (120k)
- Salvataggio automatico ogni 30 secondi
- Backup con timestamp e pulizia file obsoleti

### Sistema di Logging
- **Console**: Output formattato per sviluppo
- **File principale**: Log completi con rotazione
- **File errori**: Log separato per errori e eccezioni
- **Livelli configurabili**: DEBUG per il codice applicativo, WARN per librerie

### Monitoring
- Endpoint `/actuator/health` per controllo stato
- Metriche applicazione tramite Spring Actuator
- Log statistiche utilizzo (canali, messaggi, token)

## Configurazione

### Variabili d'ambiente principali
```bash
# Obbligatorie
DISCORD_BOT_TOKEN=your_discord_bot_token
OPENROUTER_API_KEY=your_openrouter_api_key

# Opzionali con valori di default
OPENROUTER_MODEL_NAME=deepseek/deepseek-chat-v3.1:free
SPRING_APPLICATION_NAME=test01
CONVERSATION_HISTORY_PATH=/app/data/conversation_history.json
LOG_FILE_PATH=/app/logs/test01-tao.log
```

### Configurazioni avanzate
```bash
# Logging
LOG_FILE_MAX_SIZE=10MB
LOG_FILE_MAX_HISTORY=30
LOG_FILE_TOTAL_SIZE=100MB

# Backup e salvataggio
AUTO_SAVE_ENABLED=true
AUTO_SAVE_INTERVAL=30000
BACKUP_ENABLED=true
BACKUP_PATH=/app/data/backups
BACKUP_MAX_FILES=10
```

## Avvio del progetto

### Con Docker (Raccomandato)

#### 1. Costruzione dell'immagine
```bash
docker build -t test01-tao .
```

#### 2. Avvio con configurazione base
```bash
docker run -d \
  --name test01-tao \
  -e DISCORD_BOT_TOKEN=your_token \
  -e OPENROUTER_API_KEY=your_openrouter_key \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  -p 8080:8080 \
  test01-tao
```

#### 3. Avvio con configurazione completa
```bash
docker run -d \
  --name test01-tao \
  -e DISCORD_BOT_TOKEN=your_token \
  -e OPENROUTER_API_KEY=your_openrouter_key \
  -e OPENROUTER_MODEL_NAME=deepseek/deepseek-chat-v3.1:free \
  -e LOG_FILE_MAX_SIZE=20MB \
  -e BACKUP_MAX_FILES=15 \
  -e AUTO_SAVE_INTERVAL=60000 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  -p 8080:8080 \
  test01-tao
```

#### 4. Docker Compose (file di esempio)
```yaml
services:
  test01-tao:
    build: .
    environment:
      - DISCORD_BOT_TOKEN=your_token
      - OPENROUTER_API_KEY=your_openrouter_key
      - OPENROUTER_MODEL_NAME=deepseek/deepseek-chat-v3.1:free
      - AUTO_SAVE_INTERVAL=30000
      - BACKUP_ENABLED=true
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
    ports:
      - "8080:8080"
    restart: unless-stopped
```

### Sviluppo locale

#### Prerequisiti
- Java 21+
- Maven 3.6+

#### Avvio
```bash
# Imposta le variabili d'ambiente
export DISCORD_BOT_TOKEN=your_token
export OPENROUTER_API_KEY=your_openrouter_key

# Esegui l'applicazione
mvn spring-boot:run
```

## Struttura dei file

```
/app/
├── data/                           # Dati persistenti
│   ├── conversation_history.json   # Cronologia conversazioni
│   └── backups/                    # Backup automatici
│       ├── conversation_history_20240917_143022.json
│       └── ...
├── logs/                           # File di log
│   ├── test01-tao.log             # Log principale
│   ├── test01-tao-errors.log      # Log errori
│   └── archived/                   # Log archiviati
└── app.jar                         # Applicazione
```

## Uso del Bot

### Attivazione
Il bot risponde quando viene:
- **Menzionato direttamente**: `@NomeBot ciao come stai?`
- **Menzionato tramite ruolo**: Se il bot ha un ruolo che viene menzionato

### Funzionalità
- **Conversazioni contestuali**: Mantiene la cronologia per canale
- **Messaggi lunghi**: Divisione automatica dei messaggi oltre 2000 caratteri
- **Gestione errori**: Messaggi di errore user-friendly
- **Typing indicator**: Mostra quando il bot sta elaborando

## Monitoring e Manutenzione

### Endpoint di monitoraggio
```bash
# Stato applicazione
curl http://localhost:8080/actuator/health

# Informazioni applicazione  
curl http://localhost:8080/actuator/info

# Metriche
curl http://localhost:8080/actuator/metrics
```

### Log analysis
```bash
# Visualizza log in tempo reale
docker logs -f test01-tao

# Solo errori
docker exec test01-tao tail -f /app/logs/test01-tao-errors.log

# Statistiche conversazioni
docker exec test01-tao grep "Statistiche salvataggio" /app/logs/test01-tao.log
```

### Backup management
```bash
# Lista backup disponibili
docker exec test01-tao ls -la /app/data/backups/

# Ripristino da backup
docker exec test01-tao cp /app/data/backups/conversation_history_TIMESTAMP.json /app/data/conversation_history.json
```

## Risoluzione problemi

### Bot non risponde
1. Verifica che il bot sia online su Discord
2. Controlla i log: `docker logs test01-tao`
3. Verifica le menzioni: il bot deve essere menzionato esplicitamente
4. Controlla i permessi del bot nel canale

### Errori API OpenRouter
1. Verifica la validità della chiave API
2. Controlla il modello specificato
3. Monitora i log errori per dettagli specifici

### Problemi di persistenza
1. Verifica che i volumi Docker siano montati correttamente
2. Controlla i permessi delle directory
3. Verifica lo spazio disco disponibile

## Contribuire

1. Fork del repository
2. Crea un branch per la feature
3. Implementa le modifiche
4. Aggiungi test se necessario
5. Crea una Pull Request

## Licenza

Questo progetto è distribuito sotto licenza MIT.
