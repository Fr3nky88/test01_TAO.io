package it.tao.io.test01.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * POJO per rappresentare una risposta di OpenRouter
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenRouterResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("created")
    private long created;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<Choice> choices;

    @JsonProperty("usage")
    private Usage usage;

    /**
     * Classe per rappresentare una scelta nella risposta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        @JsonProperty("index")
        private int index;

        @JsonProperty("message")
        private OpenRouterMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * Classe per rappresentare l'utilizzo di token
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
