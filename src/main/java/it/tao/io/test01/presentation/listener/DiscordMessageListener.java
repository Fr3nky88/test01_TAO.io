package it.tao.io.test01.presentation.listener;

import it.tao.io.test01.application.service.ChatBotApplicationService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Presentation Layer - Listener per gli eventi Discord
 * Gestisce l'interfaccia utente Discord e delega la logica di business all'Application Service
 */
@Component
public class DiscordMessageListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DiscordMessageListener.class);
    private static final int DISCORD_MESSAGE_LIMIT = 2000;

    private final ChatBotApplicationService chatBotService;

    public DiscordMessageListener(ChatBotApplicationService chatBotService) {
        this.chatBotService = chatBotService;
        logger.info("DiscordMessageListener inizializzato con architettura a layer");
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

        // Imposta il contesto MDC per il logging
        MDC.put("channelId", channelId);
        MDC.put("userId", userId);

        try {
            logger.debug("Messaggio ricevuto - Canale: {}, Utente: {} ({}), Contenuto: {}",
                        channelId, username, userId, rawContent);

            if (isBotMentioned(event, selfUser, selfId, rawContent)) {
                logger.info("Bot menzionato - Elaborazione messaggio per canale: {}, utente: {}", channelId, username);
                processMessage(event, channelId);
            }
        } finally {
            MDC.remove("channelId");
            MDC.remove("userId");
        }
    }

    private boolean isBotMentioned(MessageReceivedEvent event, User selfUser, String selfId, String rawContent) {
        // Controlla se il bot è stato menzionato direttamente
        boolean isMentioned = rawContent.contains("<@" + selfId + ">") || rawContent.contains("<@!" + selfId + ">");

        // Se non è menzionato direttamente e il messaggio proviene da un server,
        // controlla se è stato menzionato tramite un ruolo.
        if (!isMentioned && event.isFromGuild()) {
            Member selfMember = event.getGuild().getMember(selfUser);
            if (selfMember != null) {
                List<Role> botRoles = selfMember.getRoles();
                List<Role> mentionedRoles = event.getMessage().getMentions().getRoles();
                if (!java.util.Collections.disjoint(botRoles, mentionedRoles)) {
                    isMentioned = true;
                    logger.debug("Bot menzionato tramite ruolo nel canale: {}", event.getChannel().getId());
                }
            }
        }

        return isMentioned;
    }

    private void processMessage(MessageReceivedEvent event, String channelId) {
        // Mostra l'indicatore "sta scrivendo..." nel canale
        event.getChannel().sendTyping().queue();

        // Pulisce il messaggio rimuovendo le menzioni
        String cleanMessage = cleanMessage(event.getMessage().getContentRaw());

        if (cleanMessage.isEmpty()) {
            logger.warn("Messaggio vuoto dopo rimozione menzioni - Canale: {}", channelId);
            return;
        }

        logger.debug("Messaggio pulito: {}", cleanMessage);

        // Mantiene l'indicatore "sta scrivendo..." attivo durante l'elaborazione
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            ScheduledFuture<?> typingIndicator = executor.scheduleAtFixedRate(() ->
                event.getChannel().sendTyping().queue(), 0, 2, TimeUnit.SECONDS);

            // Delega la logica di business all'Application Service
            chatBotService.processUserMessage(channelId, cleanMessage)
                .subscribe(
                    botResponse -> {
                        logger.info("Risposta ricevuta per canale: {}, lunghezza: {} caratteri",
                                   channelId, botResponse.length());
                        sendLongMessage(event, botResponse);
                    },
                    error -> {
                        logger.error("Errore durante l'elaborazione messaggio - Canale: {}", channelId, error);
                        handleError(event, error, channelId);
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

    private String cleanMessage(String rawContent) {
        return rawContent
            .replaceAll("<@!?\\d+>", "") // Rimuove menzioni utente
            .replaceAll("<@&\\d+>", "")   // Rimuove menzioni ruolo
            .trim();
    }

    private void handleError(MessageReceivedEvent event, Throwable error, String channelId) {
        String userMessage;
        String errorMsg = error.getMessage() != null ? error.getMessage() : "";

        if (errorMsg.contains("temporaneamente non disponibile")) {
            userMessage = "⚠️ Il servizio AI è temporaneamente non disponibile. Riprova tra qualche minuto.";
            logger.warn("Servizio OpenRouter non disponibile - Canale: {}", channelId);
        } else if (error.getCause() != null && error.getCause().getMessage() != null &&
                  error.getCause().getMessage().contains("Failed to resolve")) {
            userMessage = "🌐 Problema di connessione di rete. Riprova tra poco.";
            logger.warn("Errore DNS per OpenRouter - Canale: {}", channelId);
        } else {
            userMessage = "🤖 Oops! Qualcosa è andato storto. Riprova tra poco.";
        }

        event.getChannel().sendMessage(userMessage).queue();
    }

    private void sendLongMessage(MessageReceivedEvent event, String message) {
        logger.debug("Invio messaggio lungo. Lunghezza: {} caratteri", message.length());

        if (message.length() <= DISCORD_MESSAGE_LIMIT) {
            event.getChannel().sendMessage(message).queue();
            logger.debug("Messaggio inviato direttamente (sotto il limite)");
            return;
        }

        // Divide il messaggio in parti più piccole
        List<String> parts = splitLongMessage(message);
        logger.info("Messaggio diviso in {} parti", parts.size());

        // Invia ogni parte come messaggio separato
        for (int i = 0; i < parts.size(); i++) {
            event.getChannel().sendMessage(parts.get(i)).queue();
            logger.debug("Parte {}/{} inviata", i + 1, parts.size());
        }
    }

    private List<String> splitLongMessage(String message) {
        List<String> parts = new ArrayList<>();
        String remaining = message;

        while (remaining.length() > DISCORD_MESSAGE_LIMIT) {
            // Cerca un punto di interruzione naturale prima del limite
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

        return parts;
    }
}
