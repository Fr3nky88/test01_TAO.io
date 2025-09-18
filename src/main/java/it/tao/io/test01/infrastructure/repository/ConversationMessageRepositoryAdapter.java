package it.tao.io.test01.infrastructure.repository;

import it.tao.io.test01.domain.model.ConversationMessage;
import it.tao.io.test01.domain.repository.ConversationMessageRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter che implementa l'interfaccia del domain repository
 * delegando al repository MongoDB concreto
 */
@Repository
public class ConversationMessageRepositoryAdapter implements ConversationMessageRepository {

    private final MongoConversationMessageRepository mongoRepository;

    public ConversationMessageRepositoryAdapter(MongoConversationMessageRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Mono<ConversationMessage> save(ConversationMessage message) {
        return mongoRepository.save(message);
    }

    @Override
    public Flux<ConversationMessage> findByChannelIdOrderByTimestamp(String channelId) {
        return mongoRepository.findByChannelIdOrderByTimestamp(channelId);
    }

    @Override
    public Flux<ConversationMessage> findAll() {
        return mongoRepository.findAll();
    }

    @Override
    public Mono<Void> deleteByChannelId(String channelId) {
        return mongoRepository.deleteByChannelId(channelId);
    }

    @Override
    public Mono<Long> countByChannelId(String channelId) {
        return mongoRepository.countByChannelId(channelId);
    }
}
