package it.tao.io.test01.config;

import it.tao.io.test01.presentation.listener.DiscordMessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione per l'integrazione con Discord JDA
 */
@Configuration
public class JdaConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JdaConfiguration.class);

    @Value("${discord.bot.token}")
    private String discordToken;

    @Bean
    public JDA jda(DiscordMessageListener discordMessageListener) {
        try {
            logger.info("Inizializzazione bot Discord...");

            JDA jda = JDABuilder.createDefault(discordToken)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS
                    )
                    .addEventListeners(discordMessageListener)
                    .build()
                    .awaitReady();

            logger.info("Bot Discord inizializzato con successo. Bot: {}",
                       jda.getSelfUser().getAsTag());

            return jda;

        } catch (Exception e) {
            logger.error("Errore durante l'inizializzazione del bot Discord", e);
            throw new RuntimeException("Impossibile inizializzare il bot Discord", e);
        }
    }
}
