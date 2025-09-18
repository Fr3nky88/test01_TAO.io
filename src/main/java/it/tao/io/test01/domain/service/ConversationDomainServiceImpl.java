package it.tao.io.test01.domain.service;

import it.tao.io.test01.domain.model.ConversationMessage;
import it.tao.io.test01.domain.repository.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementazione del Domain Service per la gestione delle conversazioni
 */
@Service
public class ConversationDomainServiceImpl implements ConversationDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationDomainServiceImpl.class);
    private static final int AVERAGE_TOKENS_PER_CHAR = 4;

    private final ConversationMessageRepository repository;

    public ConversationDomainServiceImpl(ConversationMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<ConversationMessage> addMessage(String channelId, String role, String content) {
        logger.debug("Aggiunta messaggio: canale={}, ruolo={}, lunghezza={}", channelId, role, content.length());

        ConversationMessage message = "user".equals(role)
            ? ConversationMessage.createUserMessage(channelId, content)
            : ConversationMessage.createAssistantMessage(channelId, content);

        return repository.save(message)
            .doOnSuccess(saved -> logger.debug("Messaggio salvato con ID: {}", saved.getId()))
            .doOnError(error -> logger.error("Errore nel salvataggio del messaggio", error));
    }

    @Override
    public Flux<ConversationMessage> getConversationHistory(String channelId) {
        logger.debug("Recupero cronologia per canale: {}", channelId);
        return repository.findByChannelIdOrderByTimestamp(channelId)
            .doOnComplete(() -> logger.debug("Cronologia recuperata per canale: {}", channelId));
    }

    @Override
    public List<Map<String, String>> convertToOpenRouterFormat(List<ConversationMessage> messages) {
        return messages.stream()
            .map(msg -> Map.of(
                "role", msg.getRole(),
                "content", msg.getContent()
            ))
            .toList();
    }

    @Override
    public List<Map<String, String>> manageTokenLimit(List<Map<String, String>> messages, int maxTokens) {
        List<Map<String, String>> managedMessages = new ArrayList<>(messages);

        int currentTokenCount = messages.stream()
            .mapToInt(msg -> estimateTokens(msg.get("content")))
            .sum();

        int removedCount = 0;
        while (currentTokenCount > maxTokens && !managedMessages.isEmpty()) {
            Map<String, String> removed = managedMessages.removeFirst();
            currentTokenCount -= estimateTokens(removed.get("content"));
            removedCount++;
        }

        if (removedCount > 0) {
            logger.info("Rimossi {} messaggi per gestire il limite di token. Token attuali: {}",
                       removedCount, currentTokenCount);
        }

        return managedMessages;
    }

    @Override
    public Mono<Void> clearChannelHistory(String channelId) {
        logger.info("Cancellazione cronologia per canale: {}", channelId);
        return repository.deleteByChannelId(channelId)
            .doOnSuccess(v -> logger.info("Cronologia cancellata per canale: {}", channelId));
    }

    @Override
    public Mono<Long> countChannelMessages(String channelId) {
        return repository.countByChannelId(channelId);
    }

    @Override
    public int estimateTokens(String text) {
        return text.length() / AVERAGE_TOKENS_PER_CHAR;
    }
}
