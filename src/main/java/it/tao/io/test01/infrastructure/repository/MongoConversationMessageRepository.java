package it.tao.io.test01.infrastructure.repository;

import it.tao.io.test01.domain.model.ConversationMessage;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository MongoDB per i messaggi di conversazione
 * Spring Data crea automaticamente l'implementazione di questa interfaccia
 */
@Repository
public interface MongoConversationMessageRepository extends ReactiveMongoRepository<ConversationMessage, String> {

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
