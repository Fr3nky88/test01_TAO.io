package it.tao.io.test01;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class DiscordListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DiscordListener.class);

    private final OpenRouterService openRouterService;
    private final ObjectMapper objectMapper;
    private Map<String, List<Map<String, String>>> conversationHistories = new ConcurrentHashMap<>();

    @Value("${app.conversation.history.path}")
    private String historyFilePath;

    @Value("${app.conversation.auto-save.enabled:true}")
    private boolean autoSaveEnabled;

    @Value("${app.conversation.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${app.conversation.backup.path:./data/backups}")
    private String backupPath;

    @Value("${app.conversation.backup.max-files:10}")
    private int maxBackupFiles;

    private static final int MAX_CONTEXT_TOKENS = 120000;
    private static final int DISCORD_MESSAGE_LIMIT = 2000;

    public DiscordListener(OpenRouterService openRouterService, ObjectMapper objectMapper) {
        this.openRouterService = openRouterService;
        this.objectMapper = objectMapper;
        logger.info("DiscordListener inizializzato");
    }

    @PostConstruct
    public void loadHistory() {
        logger.info("Inizializzazione del sistema di cronologia conversazioni");
        try {
            // Crea la directory se non esiste
            Path filePath = Paths.get(historyFilePath);
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                logger.info("Directory creata: {}", parentDir);
            }

            // Crea la directory di backup se abilitata
            if (backupEnabled) {
                Path backupDir = Paths.get(backupPath);
                if (!Files.exists(backupDir)) {
                    Files.createDirectories(backupDir);
                    logger.info("Directory backup creata: {}", backupDir);
                }
            }

            File historyFile = new File(historyFilePath);
            if (historyFile.exists()) {
                // Legge la mappa dal file JSON se esiste
                conversationHistories = objectMapper.readValue(historyFile, new TypeReference<ConcurrentHashMap<String, List<Map<String, String>>>>() {});
                logger.info("Cronologia conversazioni caricata da: {} - {} canali trovati", historyFilePath, conversationHistories.size());

                // Log dettagliato delle conversazioni caricate
                conversationHistories.forEach((channelId, history) ->
                    logger.debug("Canale {}: {} messaggi caricati", channelId, history.size())
                );
            } else {
                logger.info("File di cronologia non trovato, sarà creato al primo salvataggio: {}", historyFilePath);
            }
        } catch (IOException e) {
            logger.error("Impossibile caricare la cronologia delle conversazioni", e);
        }
    }

    @PreDestroy
    public void saveHistory() {
        logger.info("Salvataggio finale della cronologia conversazioni");
        saveHistoryToFile();
    }

    @Scheduled(fixedRateString = "${app.conversation.auto-save.interval:30000}")
    public void autoSaveHistory() {
        if (autoSaveEnabled) {
            logger.debug("Esecuzione salvataggio automatico della cronologia");
            saveHistoryToFile();
        }
    }

    private void saveHistoryToFile() {
        try {
            // Crea backup se abilitato
            if (backupEnabled && Files.exists(Paths.get(historyFilePath))) {
                createBackup();
            }

            // Salva la mappa corrente nel file JSON
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(historyFilePath), conversationHistories);
            logger.debug("Cronologia conversazioni salvata in: {} - {} canali", historyFilePath, conversationHistories.size());

            // Log statistiche dettagliate
            int totalMessages = conversationHistories.values().stream()
                .mapToInt(List::size)
                .sum();
            logger.debug("Statistiche salvataggio: {} canali, {} messaggi totali", conversationHistories.size(), totalMessages);

        } catch (IOException e) {
            logger.error("Impossibile salvare la cronologia delle conversazioni", e);
        }
    }

    private void createBackup() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "conversation_history_" + timestamp + ".json";
            Path backupFilePath = Paths.get(backupPath, backupFileName);

            Files.copy(Paths.get(historyFilePath), backupFilePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Backup creato: {}", backupFilePath);

            // Pulisce i vecchi backup
            cleanOldBackups();

        } catch (IOException e) {
            logger.error("Errore durante la creazione del backup", e);
        }
    }

    private void cleanOldBackups() {
        try {
            Path backupDir = Paths.get(backupPath);
            try (var stream = Files.list(backupDir)) {
                List<Path> backupFiles = stream
                    .filter(path -> path.getFileName().toString().startsWith("conversation_history_"))
                    .sorted((p1, p2) -> p2.getFileName().compareTo(p1.getFileName())) // Ordine decrescente per data
                    .toList();

                if (backupFiles.size() > maxBackupFiles) {
                    for (int i = maxBackupFiles; i < backupFiles.size(); i++) {
                        Files.delete(backupFiles.get(i));
                        logger.debug("Backup obsoleto rimosso: {}", backupFiles.get(i));
                    }
                    logger.info("Pulizia backup completata. Mantenuti {} file su {}", maxBackupFiles, backupFiles.size());
                }
            }
        } catch (IOException e) {
            logger.error("Errore durante la pulizia dei backup", e);
        }
    }

    private int estimateTokens(String text) {
        return text.length() / 4;
    }

    private void sendLongMessage(MessageReceivedEvent event, String message) {
        logger.debug("Invio messaggio lungo. Lunghezza: {} caratteri", message.length());

        if (message.length() <= DISCORD_MESSAGE_LIMIT) {
            event.getChannel().sendMessage(message).queue();
            logger.debug("Messaggio inviato direttamente (sotto il limite)");
            return;
        }

        // Divide il messaggio in parti più piccole
        List<String> parts = new ArrayList<>();
        String remaining = message;

        while (remaining.length() > DISCORD_MESSAGE_LIMIT) {
            // Cerca un punto di interruzione naturale (spazio, punto, virgola) prima del limite
            int breakPoint = DISCORD_MESSAGE_LIMIT;
            for (int i = DISCORD_MESSAGE_LIMIT - 1; i > DISCORD_MESSAGE_LIMIT - 200; i--) {
                char c = remaining.charAt(i);
                if (c == ' ' || c == '\n' || c == '.' || c == ',' || c == ';' || c == '!' || c == '?') {
                    breakPoint = i + 1;
                    break;
                }
            }

            parts.add(remaining.substring(0, breakPoint).trim());
            remaining = remaining.substring(breakPoint);
        }

        if (!remaining.trim().isEmpty()) {
            parts.add(remaining.trim());
        }

        logger.info("Messaggio diviso in {} parti", parts.size());

        // Invia ogni parte come messaggio separato
        for (int i = 0; i < parts.size(); i++) {
            event.getChannel().sendMessage(parts.get(i)).queue();
            logger.debug("Parte {}/{} inviata", i + 1, parts.size());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        User selfUser = event.getJDA().getSelfUser();
        String selfId = selfUser.getId();
        String rawContent = event.getMessage().getContentRaw();
        String channelId = event.getChannel().getId();
        String userId = event.getAuthor().getId();
        String username = event.getAuthor().getName();

        logger.debug("Messaggio ricevuto - Canale: {}, Utente: {} ({}), Contenuto: {}",
                    channelId, username, userId, rawContent);

        // Controlla se il bot è stato menzionato direttamente
        boolean isMentioned = rawContent.contains("<@" + selfId + ">") || rawContent.contains("<@!" + selfId + ">");

        // Se non è menzionato direttamente e il messaggio proviene da un server,
        // controlla se è stato menzionato tramite un ruolo.
        if (!isMentioned && event.isFromGuild()) {
            Member selfMember = event.getGuild().getMember(selfUser);
            if (selfMember != null) {
                // Ottiene i ruoli del bot e i ruoli menzionati
                List<Role> botRoles = selfMember.getRoles();
                List<Role> mentionedRoles = event.getMessage().getMentions().getRoles();
                // Controlla se c'è almeno un ruolo in comune
                if (!java.util.Collections.disjoint(botRoles, mentionedRoles)) {
                    isMentioned = true;
                    logger.debug("Bot menzionato tramite ruolo nel canale: {}", channelId);
                }
            }
        }

        if (isMentioned) {
            logger.info("Bot menzionato - Elaborazione messaggio per canale: {}, utente: {}", channelId, username);

            // Mostra l'indicatore "sta scrivendo..." nel canale
            event.getChannel().sendTyping().queue();

            // Rimuove tutte le menzioni (sia utente che ruolo) dal messaggio
            String userMessageContent = event.getMessage().getContentRaw()
                    .replaceAll("<@!?\\d+>", "") // Rimuove menzioni utente
                    .replaceAll("<@&\\d+>", "")   // Rimuove menzioni ruolo
                    .trim();

            if (userMessageContent.isEmpty()) {
                logger.warn("Messaggio vuoto dopo rimozione menzioni - Canale: {}", channelId);
                return;
            }

            logger.debug("Messaggio pulito: {}", userMessageContent);

            List<Map<String, String>> history = conversationHistories.computeIfAbsent(
                    channelId,
                    k -> {
                        logger.info("Nuova cronologia creata per il canale: {}", channelId);
                        return new ArrayList<>();
                    }
            );

            history.add(Map.of("role", "user", "content", userMessageContent));
            logger.debug("Messaggio aggiunto alla cronologia - Canale: {}, Messaggi totali: {}", channelId, history.size());

            // Gestione del limite di token
            int currentTokenCount = 0;
            for (Map<String, String> message : history) {
                currentTokenCount += estimateTokens(message.get("content"));
            }

            int removedMessages = 0;
            while (currentTokenCount > MAX_CONTEXT_TOKENS && !history.isEmpty()) {
                Map<String, String> removedMessage = history.removeFirst();
                currentTokenCount -= estimateTokens(removedMessage.get("content"));
                removedMessages++;
            }

            if (removedMessages > 0) {
                logger.info("Rimossi {} messaggi dalla cronologia per gestire il limite di token - Canale: {}",
                           removedMessages, channelId);
            }

            logger.debug("Token stimati per il contesto: {} - Canale: {}", currentTokenCount, channelId);

            // Mantiene l'indicatore "sta scrivendo..." attivo durante l'elaborazione della richiesta a OpenRouter
            try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                ScheduledFuture<?> typingIndicator = executor.scheduleAtFixedRate(() -> {
                    event.getChannel().sendTyping().queue();
                }, 0, 2, TimeUnit.SECONDS);

                openRouterService.getChatCompletion(history)
                        .subscribe(
                                botResponse -> {
                                    logger.info("Risposta ricevuta da OpenRouter - Canale: {}, Lunghezza: {} caratteri",
                                               channelId, botResponse.length());
                                    sendLongMessage(event, botResponse);
                                    history.add(Map.of("role", "assistant", "content", botResponse));
                                    logger.debug("Risposta aggiunta alla cronologia - Canale: {}, Messaggi totali: {}",
                                               channelId, history.size());
                                },
                                error -> {
                                    logger.error("Errore durante la chiamata a OpenRouter - Canale: {}", channelId, error);

                                    String userMessage;
                                    if (error.getMessage() != null && error.getMessage().contains("temporaneamente non disponibile")) {
                                        userMessage = "⚠️ Il servizio AI è temporaneamente non disponibile. Riprova tra qualche minuto.";
                                        logger.warn("Servizio OpenRouter non disponibile dopo retry - Canale: {}", channelId);
                                    } else if (error.getCause() != null && error.getCause().getMessage() != null &&
                                              error.getCause().getMessage().contains("Failed to resolve")) {
                                        userMessage = "🌐 Problema di connessione di rete. Riprova tra poco.";
                                        logger.warn("Errore DNS per OpenRouter - Canale: {}", channelId);
                                    } else {
                                        userMessage = "🤖 Oops! Qualcosa è andato storto. Riprova tra poco.";
                                    }

                                    event.getChannel().sendMessage(userMessage).queue();
                                },
                                () -> {
                                    // Ferma l'indicatore "sta scrivendo..." una volta completata l'elaborazione
                                    typingIndicator.cancel(false);
                                    executor.shutdown();
                                }
                        );
            } catch (Exception e) {
                logger.error("Errore nell'elaborazione del messaggio - Canale: {}", channelId, e);
                event.getChannel().sendMessage("🤖 Errore durante l'elaborazione. Riprova tra poco.").queue();
            }
        }
    }

}
