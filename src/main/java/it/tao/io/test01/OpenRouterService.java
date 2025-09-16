package it.tao.io.test01;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterService.class);

    private final WebClient webClient;
    private final Gson gson = new Gson();

    @Value("${openrouter.model.name}")
    private String modelName;

    public OpenRouterService(WebClient.Builder webClientBuilder, @Value("${openrouter.api.key}") String openRouterApiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + openRouterApiKey)
                .build();

        logger.info("OpenRouterService inizializzato con modello: {}", modelName);
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
                .doOnNext(response -> logger.debug("Risposta ricevuta da OpenRouter - Lunghezza: {} caratteri", response.length()))
                .doOnError(error -> logger.error("Errore nella chiamata a OpenRouter", error))
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

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            logger.debug("Contenuto estratto dalla risposta - Lunghezza: {} caratteri", content.length());

            return content;
        } catch (Exception e) {
            logger.error("Errore nell'estrazione del contenuto dalla risposta OpenRouter", e);
            return "Errore nell'elaborazione della risposta.";
        }
    }
}