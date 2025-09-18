package it.tao.io.test01.infrastructure.repository;

import it.tao.io.test01.domain.model.ConversationMessage;
import it.tao.io.test01.domain.repository.ConversationMessageRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementazione MongoDB del repository per i messaggi di conversazione
 */
@Repository
public interface MongoConversationMessageRepository extends ReactiveMongoRepository<ConversationMessage, String>, ConversationMessageRepository {

    @Override
    Flux<ConversationMessage> findByChannelIdOrderByTimestamp(String channelId);

    @Override
    Mono<Void> deleteByChannelId(String channelId);

    @Override
    Mono<Long> countByChannelId(String channelId);
}
