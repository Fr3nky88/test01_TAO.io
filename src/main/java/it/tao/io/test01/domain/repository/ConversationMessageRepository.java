package it.tao.io.test01.domain.repository;

import it.tao.io.test01.domain.model.ConversationMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface per la gestione dei messaggi di conversazione
 * Definisce il contratto per la persistenza, implementato nell'infrastructure layer
 * I metodi base (save, findAll, ecc.) sono forniti da ReactiveMongoRepository
 */
public interface ConversationMessageRepository {

    /**
     * Trova tutti i messaggi per un canale specifico, ordinati per timestamp
     */
    Flux<ConversationMessage> findByChannelIdOrderByTimestamp(String channelId);

    /**
     * Elimina tutti i messaggi di un canale specifico
     */
    Mono<Void> deleteByChannelId(String channelId);

    /**
     * Conta il numero di messaggi per un canale specifico
     */
    Mono<Long> countByChannelId(String channelId);
}
