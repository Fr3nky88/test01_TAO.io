# test01_TAO.io

Applicazione Spring Boot che integra un bot Discord con funzionalità di AI tramite OpenRouter. Il bot risponde ai messaggi su Discord utilizzando modelli di intelligenza artificiale configurabili tramite variabili d'ambiente. Ideale per creare assistenti virtuali, chatbot o sistemi di automazione su server Discord.

## Funzionalità principali
- Bot Discord configurabile tramite token
- Integrazione con API OpenRouter per risposte AI
- Modello AI selezionabile tramite variabile d'ambiente
- Deploy semplice e sicuro tramite Docker

## Avvio del progetto con Docker

### Prerequisiti
- Docker installato

### Costruzione dell'immagine

Esegui dalla root del progetto:

```
docker build -t test01-tao .
```

### Avvio del container

Il progetto richiede alcune variabili d'ambiente da impostare per funzionare correttamente:
- `DISCORD_BOT_TOKEN`: il token del bot Discord
- `OPENROUTER_API_KEY`: la chiave API di OpenRouter
- `OPENROUTER_MODEL_NAME`: (opzionale) il nome del modello OpenRouter, default: `deepseek/deepseek-chat-v3.1:free`
- `SPRING_APPLICATION_NAME`: (opzionale) nome dell'applicazione, default: `test01`

Esempio di avvio:

```
docker run -e DISCORD_BOT_TOKEN=your_token \
           -e OPENROUTER_API_KEY=your_openrouter_key \
           -e OPENROUTER_MODEL_NAME=deepseek/deepseek-chat-v3.1:free \
           -p 8080:8080 test01-tao
```

Sostituisci `your_token` e `your_openrouter_key` con i tuoi valori reali.

### Note
- Le variabili d'ambiente sono obbligatorie per il corretto funzionamento.
- La porta esposta è la 8080.
