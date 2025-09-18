package it.tao.io.test01.infrastructure.client;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client per l'integrazione con il servizio OpenRouter AI
 */
@Component
public class OpenRouterClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterClient.class);

    private final WebClient webClient;
    private final Gson gson = new Gson();

    @Value("${openrouter.model.name}")
    private String modelName;

    @Value("${openrouter.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${openrouter.retry.base-delay:1000}")
    private long baseDelayMs;

    public OpenRouterClient(WebClient.Builder webClientBuilder, @Value("${openrouter.api.key}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", "https://localhost:8080")
                .defaultHeader("X-Title", "TAO Discord Bot")
                .build();

        logger.info("OpenRouterClient inizializzato con modello: {}", modelName);
    }

    /**
     * Invia una richiesta di chat completion a OpenRouter
     */
    public Mono<String> getChatCompletion(List<Map<String, String>> messages) {
        logger.debug("Invio richiesta a OpenRouter con {} messaggi", messages.size());

        var requestBody = Map.of(
                "model", modelName,
                "messages", messages,
                "temperature", 0.7,
                "max_tokens", 4000,
                "stream", false
        );

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToString()
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(baseDelayMs))
                        .filter(this::isRetryableException)
                        .doBeforeRetry(retrySignal ->
                            logger.warn("Tentativo {} per OpenRouter", retrySignal.totalRetries() + 1)))
                .map(this::extractContentFromResponse)
                .doOnSuccess(response ->
                    logger.debug("Risposta ricevuta da OpenRouter: {} caratteri", response.length()))
                .doOnError(error ->
                    logger.error("Errore nella chiamata a OpenRouter", error));
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientRequestException) {
            return true;
        }
        return throwable.getMessage() != null &&
               (throwable.getMessage().contains("timeout") ||
                throwable.getMessage().contains("connection"));
    }

    private String extractContentFromResponse(String jsonResponse) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = gson.fromJson(jsonResponse, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

                if (message != null) {
                    return (String) message.get("content");
                }
            }

            logger.warn("Formato risposta OpenRouter non valido: {}", jsonResponse);
            return "Risposta non valida dal servizio AI.";

        } catch (Exception e) {
            logger.error("Errore nel parsing della risposta OpenRouter", e);
            return "Errore nell'elaborazione della risposta AI.";
        }
    }
}
