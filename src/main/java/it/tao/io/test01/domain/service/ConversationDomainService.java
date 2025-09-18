package it.tao.io.test01.domain.service;

import it.tao.io.test01.domain.model.ConversationMessage;
import it.tao.io.test01.domain.repository.ConversationMessageRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Domain Service per la gestione della logica di business delle conversazioni
 */
public interface ConversationDomainService {

    /**
     * Aggiunge un messaggio alla conversazione
     */
    Mono<ConversationMessage> addMessage(String channelId, String role, String content);

    /**
     * Recupera la cronologia di conversazione per un canale
     */
    Flux<ConversationMessage> getConversationHistory(String channelId);

    /**
     * Converte i messaggi in formato compatibile con OpenRouter
     */
    List<Map<String, String>> convertToOpenRouterFormat(List<ConversationMessage> messages);

    /**
     * Gestisce il limite di token rimuovendo i messaggi più vecchi se necessario
     */
    List<Map<String, String>> manageTokenLimit(List<Map<String, String>> messages, int maxTokens);

    /**
     * Cancella la cronologia di un canale
     */
    Mono<Void> clearChannelHistory(String channelId);

    /**
     * Conta i messaggi di un canale
     */
    Mono<Long> countChannelMessages(String channelId);

    /**
     * Stima il numero di token in un testo
     */
    int estimateTokens(String text);
}
