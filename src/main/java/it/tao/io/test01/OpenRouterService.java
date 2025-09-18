package it.tao.io.test01;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterService.class);

    private final WebClient webClient;
    private final Gson gson = new Gson();

    @Value("${openrouter.model.name}")
    private String modelName;

    @Value("${openrouter.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${openrouter.retry.base-delay:1000}")
    private long baseDelayMs;

    public OpenRouterService(WebClient.Builder webClientBuilder, @Value("${openrouter.api.key}") String openRouterApiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + openRouterApiKey)
                .build();

        logger.info("OpenRouterService inizializzato con modello: {}, retry max: {}, delay base: {}ms",
                modelName, maxRetryAttempts, baseDelayMs);
    }

    // MODIFICATO: Accetta una lista di messaggi (la cronologia)
    public Mono<String> getChatCompletion(List<Map<String, String>> conversationHistory) {
        logger.debug("Preparazione richiesta a OpenRouter - Messaggi nella cronologia: {}", conversationHistory.size());

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", conversationHistory
        );

        logger.debug("Invio richiesta a OpenRouter con modello: {}", modelName);

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gson.toJson(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(baseDelayMs))
                        .filter(throwable -> {
                            if (throwable instanceof WebClientRequestException ex) {
                                // Retry per errori DNS e di connessione
                                boolean shouldRetry = ex.getMessage().contains("Failed to resolve") ||
                                        ex.getMessage().contains("Connection refused") ||
                                        ex.getMessage().contains("timeout") ||
                                        ex.getMessage().contains("No route to host");

                                if (shouldRetry) {
                                    logger.warn("Errore di rete rilevato, tentativo di retry: {}", ex.getMessage());
                                    return true;
                                }
                            }
                            return false;
                        })
                        .doBeforeRetry(retrySignal -> {
                            logger.info("Retry #{} per chiamata OpenRouter - Tentativo: {}",
                                    retrySignal.totalRetries() + 1,
                                    retrySignal.totalRetriesInARow() + 1);
                        })
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.error("Esauriti tutti i tentativi di retry ({}) per OpenRouter", maxRetryAttempts);
                            return new RuntimeException("Servizio OpenRouter temporaneamente non disponibile dopo " +
                                    maxRetryAttempts + " tentativi", retrySignal.failure());
                        })
                )
                .doOnNext(response -> logger.debug("Risposta ricevuta da OpenRouter - Lunghezza: {} caratteri", response.length()))
                .doOnError(error -> {
                    if (error instanceof WebClientRequestException ex) {
                        if (ex.getMessage().contains("Failed to resolve")) {
                            logger.error("Errore DNS permanente per openrouter.ai - Verificare connessione internet", error);
                        } else {
                            logger.error("Errore di rete nella chiamata a OpenRouter", error);
                        }
                    } else {
                        logger.error("Errore nella chiamata a OpenRouter", error);
                    }
                })
                .map(this::extractContentFromResponse);
    }

    private String extractContentFromResponse(String responseBody) {
        try {
            Map<String, Object> bodyAsMap = gson.fromJson(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) bodyAsMap.get("choices");

            if (choices == null || choices.isEmpty()) {
                logger.warn("Nessuna scelta trovata nella risposta di OpenRouter");
                return "Nessuna risposta ricevuta dal modello.";
            }

            Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
            String content = (String) message.get("content");

            logger.debug("Contenuto estratto dalla risposta - Lunghezza: {} caratteri", content.length());

            return content;
        } catch (Exception e) {
            logger.error("Errore nell'estrazione del contenuto dalla risposta OpenRouter", e);
            return "Errore nell'elaborazione della risposta.";
        }
    }
}