package it.tao.io.test01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    /** Limite massimo token nel contesto */
    @Min(1000)
    private int maxContextTokens = 120000;

    /** Limite caratteri messaggio Discord */
    @Min(100)
    private int discordMessageLimit = 2000;

    // getters/setters
    public int getMaxContextTokens() { return maxContextTokens; }
    public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }
    public int getDiscordMessageLimit() { return discordMessageLimit; }
    public void setDiscordMessageLimit(int discordMessageLimit) { this.discordMessageLimit = discordMessageLimit; }
}
