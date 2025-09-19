package it.tao.io.test01.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * POJO per rappresentare una richiesta di chat completion a OpenRouter
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenRouterRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<OpenRouterMessage> messages;

    @JsonProperty("temperature")
    private double temperature;

    @JsonProperty("max_tokens")
    private int maxTokens;

    @JsonProperty("stream")
    private boolean stream;
}
