# Fase 1: Build dell'applicazione
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Fase 2: Immagine runtime leggera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crea la directory per i dati persistenti
RUN mkdir -p /app/data

# Copia il JAR dall'immagine di build
COPY --from=build /app/target/*.jar app.jar

# Crea un volume per i dati persistenti
VOLUME ["/app/data"]

# Espone la porta (se necessario)
EXPOSE 8080

# Imposta le variabili d'ambiente di default
ENV CONVERSATION_HISTORY_PATH=/app/data/conversation_history.json

ENTRYPOINT ["java","-jar","app.jar"]
