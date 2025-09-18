package it.tao.io.test01.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Entità di dominio che rappresenta un messaggio di conversazione
 */
@Document(collection = "conversation_messages")
public class ConversationMessage {

    @Id
    private String id;

    private String channelId;
    private String role; // "user" o "assistant"
    private String content;
    private Instant timestamp;

    // Costruttore vuoto per MongoDB
    public ConversationMessage() {
    }

    // Costruttore completo
    public ConversationMessage(String channelId, String role, String content, Instant timestamp) {
        this.channelId = channelId;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Metodo factory per creare un messaggio utente
    public static ConversationMessage createUserMessage(String channelId, String content) {
        return new ConversationMessage(channelId, "user", content, Instant.now());
    }

    // Metodo factory per creare un messaggio assistente
    public static ConversationMessage createAssistantMessage(String channelId, String content) {
        return new ConversationMessage(channelId, "assistant", content, Instant.now());
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ConversationMessage{" +
                "id='" + id + '\'' +
                ", channelId='" + channelId + '\'' +
                ", role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
