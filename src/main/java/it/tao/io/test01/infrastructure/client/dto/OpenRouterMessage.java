package it.tao.io.test01.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POJO per rappresentare un messaggio nel formato OpenRouter
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenRouterMessage {

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;
}
