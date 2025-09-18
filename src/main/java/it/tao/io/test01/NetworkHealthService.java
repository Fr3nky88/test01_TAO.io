package it.tao.io.test01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class NetworkHealthService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkHealthService.class);

    @Value("${network.health.check.enabled:true}")
    private boolean healthCheckEnabled;

    @Value("${network.health.check.interval:300000}") // 5 minuti
    private long healthCheckInterval;

    @Value("${network.health.check.timeout:5000}")
    private int timeoutMs;

    private boolean discordReachable = true;
    private boolean openRouterReachable = true;
    private boolean dnsWorking = true;

    private LocalDateTime lastDiscordCheck;
    private LocalDateTime lastOpenRouterCheck;
    private LocalDateTime lastDnsCheck;

    @Scheduled(fixedDelayString = "${network.health.check.interval:300000}")
    public void performHealthCheck() {
        if (!healthCheckEnabled) {
            return;
        }

        logger.debug("Avvio controllo stato rete");

        // Controlla DNS
        checkDnsResolution();

        // Controlla Discord
        checkDiscordConnectivity();

        // Controlla OpenRouter
        checkOpenRouterConnectivity();

        logNetworkStatus();
    }

    private void checkDnsResolution() {
        try {
            CompletableFuture<Void> dnsCheck = CompletableFuture.runAsync(() -> {
                try {
                    InetAddress.getByName("discord.com");
                    InetAddress.getByName("openrouter.ai");
                    InetAddress.getByName("google.com");
                } catch (UnknownHostException e) {
                    throw new RuntimeException("DNS resolution failed", e);
                }
            });

            dnsCheck.get(timeoutMs, TimeUnit.MILLISECONDS);

            if (!dnsWorking) {
                logger.info("✅ Risoluzione DNS ripristinata");
            }
            dnsWorking = true;
            lastDnsCheck = LocalDateTime.now();

        } catch (Exception e) {
            if (dnsWorking) {
                logger.warn("❌ Problemi risoluzione DNS rilevati: {}", e.getMessage());
            }
            dnsWorking = false;
        }
    }

    private void checkDiscordConnectivity() {
        boolean previousState = discordReachable;

        try {
            CompletableFuture<Void> discordCheck = CompletableFuture.runAsync(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("discord.com", 443), timeoutMs);
                } catch (IOException e) {
                    throw new RuntimeException("Discord connection failed", e);
                }
            });

            discordCheck.get(timeoutMs, TimeUnit.MILLISECONDS);
            discordReachable = true;
            lastDiscordCheck = LocalDateTime.now();

            if (!previousState) {
                logger.info("✅ Connettività Discord ripristinata");
            }

        } catch (Exception e) {
            discordReachable = false;

            if (previousState) {
                logger.warn("❌ Discord non raggiungibile: {}", e.getMessage());
            }
        }
    }

    private void checkOpenRouterConnectivity() {
        boolean previousState = openRouterReachable;

        try {
            CompletableFuture<Void> openRouterCheck = CompletableFuture.runAsync(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("openrouter.ai", 443), timeoutMs);
                } catch (IOException e) {
                    throw new RuntimeException("OpenRouter connection failed", e);
                }
            });

            openRouterCheck.get(timeoutMs, TimeUnit.MILLISECONDS);
            openRouterReachable = true;
            lastOpenRouterCheck = LocalDateTime.now();

            if (!previousState) {
                logger.info("✅ Connettività OpenRouter ripristinata");
            }

        } catch (Exception e) {
            openRouterReachable = false;

            if (previousState) {
                logger.warn("❌ OpenRouter non raggiungibile: {}", e.getMessage());
            }
        }
    }

    private void logNetworkStatus() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        logger.debug("📊 Stato rete - DNS: {} ({}), Discord: {} ({}), OpenRouter: {} ({})",
                dnsWorking ? "✅" : "❌",
                lastDnsCheck != null ? lastDnsCheck.format(formatter) : "mai",
                discordReachable ? "✅" : "❌",
                lastDiscordCheck != null ? lastDiscordCheck.format(formatter) : "mai",
                openRouterReachable ? "✅" : "❌",
                lastOpenRouterCheck != null ? lastOpenRouterCheck.format(formatter) : "mai");

        // Log warning se ci sono problemi
        if (!dnsWorking || !discordReachable || !openRouterReachable) {
            logger.warn("⚠️ Problemi di connettività rilevati - Verificare configurazione DNS e connessione internet");
        }
    }

    // Metodi pubblici per altre classi
    public boolean isDiscordReachable() {
        return discordReachable;
    }

    public boolean isOpenRouterReachable() {
        return openRouterReachable;
    }

    public boolean isDnsWorking() {
        return dnsWorking;
    }

    public String getNetworkStatusSummary() {
        return String.format("DNS: %s, Discord: %s, OpenRouter: %s",
                dnsWorking ? "OK" : "ERROR",
                discordReachable ? "OK" : "ERROR",
                openRouterReachable ? "OK" : "ERROR");
    }

    // Metodo per forzare un controllo immediato
    public void forceHealthCheck() {
        logger.info("🔄 Controllo stato rete forzato");
        performHealthCheck();
    }
}
