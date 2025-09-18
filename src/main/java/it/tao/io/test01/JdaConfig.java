package it.tao.io.test01;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class JdaConfig {

    private static final Logger logger = LoggerFactory.getLogger(JdaConfig.class);

    @Value("${discord.bot.token}")
    private String discordToken;

    @Value("${discord.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${discord.connection.timeout:30000}")
    private long connectionTimeoutMs;

    @Value("${discord.read.timeout:60000}")
    private long readTimeoutMs;

    @Bean
    public JDA jda(DiscordListener discordListener) throws LoginException {
        logger.info("Inizializzazione JDA con retry configurati: {} tentativi, timeout connessione: {}ms, timeout lettura: {}ms",
                   maxRetryAttempts, connectionTimeoutMs, readTimeoutMs);

        // Configura OkHttpClient con retry e timeout personalizzati
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectionTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(30000, TimeUnit.MILLISECONDS)
                .callTimeout(Duration.ofMillis(readTimeoutMs + 10000))
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true) // Abilita retry automatici per errori di connessione
                .addInterceptor(chain -> {
                    var request = chain.request();
                    var response = chain.proceed(request);

                    // Log per monitorare le chiamate
                    if (!response.isSuccessful()) {
                        logger.warn("Richiesta Discord fallita - URL: {}, Codice: {}",
                                   request.url(), response.code());
                    }

                    return response;
                })
                .build();

        try {
            JDA jda = JDABuilder.createDefault(discordToken)
                    .addEventListeners(discordListener)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .setHttpClient(httpClient)
                    .setCompression(Compression.ZLIB) // Riduce il traffico di rete
                    .setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER))
                    .setLargeThreshold(50) // Ottimizza per server piccoli/medi
                    .setMaxReconnectDelay(32)
                    .build();

            logger.info("JDA inizializzato con successo - Bot: {}", jda.getSelfUser().getName());
            return jda;

        } catch (Exception e) {
            logger.error("Errore durante l'inizializzazione di JDA", e);

            if (e.getMessage() != null && e.getMessage().contains("UnknownHostException")) {
                logger.error("Errore DNS per discord.com - Verificare la connessione internet e le impostazioni DNS");
                throw new RuntimeException("Impossibile raggiungere Discord: problema di connettività di rete", e);
            }

            throw e;
        }
    }
}
