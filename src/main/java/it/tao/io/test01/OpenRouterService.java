package it.tao.io.test01;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    private final WebClient webClient;
    private final Gson gson = new Gson();

    @Value("${openrouter.model.name}")
    private String modelName;

    public OpenRouterService(WebClient.Builder webClientBuilder, @Value("${openrouter.api.key}") String openRouterApiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + openRouterApiKey)
                .build();
    }

    // MODIFICATO: Accetta una lista di messaggi (la cronologia)
    public Mono<String> getChatCompletion(List<Map<String, String>> conversationHistory) {
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", conversationHistory
        );

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gson.toJson(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractContentFromResponse);
    }

    private String extractContentFromResponse(String responseBody) {
        Map<String, Object> bodyAsMap = gson.fromJson(responseBody, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) bodyAsMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            return "Nessuna risposta ricevuta dal modello.";
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}