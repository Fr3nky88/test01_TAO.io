# Fase 1: Build dell'applicazione
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Fase 2: Immagine runtime leggera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Installa tzdata e imposta il fuso orario Europe/Rome
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Europe/Rome /etc/localtime && \
    echo "Europe/Rome" > /etc/timezone && \
    apk del tzdata

# Crea le directory per i dati persistenti, log e backup
RUN mkdir -p /app/data /app/logs /app/data/backups

# Copia il JAR dall'immagine di build
COPY --from=build /app/target/*.jar app.jar

# Crea volumi per i dati persistenti, log e backup
VOLUME ["/app/data", "/app/logs"]

# Espone la porta (se necessario)
EXPOSE 8080

# Imposta le variabili d'ambiente di default
ENV CONVERSATION_HISTORY_PATH=/app/data/conversation_history.json \
    LOG_FILE_PATH=/app/logs/test01-tao.log \
    BACKUP_PATH=/app/data/backups \
    AUTO_SAVE_ENABLED=true \
    BACKUP_ENABLED=true \
    TZ=Europe/Rome

ENTRYPOINT ["java","-jar","app.jar"]
