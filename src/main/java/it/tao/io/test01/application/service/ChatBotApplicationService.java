package it.tao.io.test01.application.service;

import it.tao.io.test01.domain.service.ConversationDomainService;
import it.tao.io.test01.infrastructure.client.OpenRouterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Application Service che orchestra i use case del bot Discord
 */
@Service
public class ChatBotApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ChatBotApplicationService.class);
    private static final int MAX_CONTEXT_TOKENS = 120000;

    private final ConversationDomainService conversationService;
    private final OpenRouterClient openRouterClient;

    public ChatBotApplicationService(ConversationDomainService conversationService,
                                   OpenRouterClient openRouterClient) {
        this.conversationService = conversationService;
        this.openRouterClient = openRouterClient;
    }

    /**
     * Processa un messaggio utente e genera una risposta AI
     */
    public Mono<String> processUserMessage(String channelId, String userMessage) {
        logger.debug("Processamento messaggio per canale: {}", channelId);

        return conversationService.addMessage(channelId, "user", userMessage)
            .then(conversationService.getConversationHistory(channelId).collectList())
            .map(messages -> {
                // Converte i messaggi in formato OpenRouter
                List<Map<String, String>> openRouterMessages =
                    conversationService.convertToOpenRouterFormat(messages);

                // Gestisce il limite di token
                return conversationService.manageTokenLimit(openRouterMessages, MAX_CONTEXT_TOKENS);
            })
            .flatMap(managedMessages -> {
                logger.debug("Invio {} messaggi a OpenRouter", managedMessages.size());
                return openRouterClient.getChatCompletion(managedMessages);
            })
            .flatMap(aiResponse -> {
                // Salva la risposta AI nella cronologia
                return conversationService.addMessage(channelId, "assistant", aiResponse)
                    .map(savedMessage -> aiResponse);
            })
            .doOnSuccess(response ->
                logger.info("Risposta AI generata per canale: {} (lunghezza: {})",
                           channelId, response.length()))
            .doOnError(error ->
                logger.error("Errore nel processamento messaggio per canale: {}", channelId, error));
    }

    /**
     * Cancella la cronologia di conversazione per un canale
     */
    public Mono<Void> clearChannelHistory(String channelId) {
        logger.info("Cancellazione cronologia per canale: {}", channelId);
        return conversationService.clearChannelHistory(channelId);
    }

    /**
     * Ottiene statistiche della conversazione per un canale
     */
    public Mono<ConversationStats> getChannelStats(String channelId) {
        return conversationService.countChannelMessages(channelId)
            .map(messageCount -> new ConversationStats(channelId, messageCount));
    }

    /**
     * Record per le statistiche della conversazione
     */
    public record ConversationStats(String channelId, Long messageCount) {}
}
