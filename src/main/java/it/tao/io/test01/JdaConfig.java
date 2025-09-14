package it.tao.io.test01;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;

@Configuration
public class JdaConfig {

    @Value("${discord.bot.token}")
    private String discordToken;

    @Bean
    public JDA jda(DiscordListener discordListener) throws LoginException {
        return JDABuilder.createDefault(discordToken)
                .addEventListeners(discordListener)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT) // Abilita l'intento per leggere il contenuto dei messaggi
                .build();
    }
}
