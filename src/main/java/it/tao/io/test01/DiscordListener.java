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
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DiscordListener extends ListenerAdapter {

    private final OpenRouterService openRouterService;
    private final ObjectMapper objectMapper;
    private Map<String, List<Map<String, String>>> conversationHistories = new ConcurrentHashMap<>();

    private static final String HISTORY_FILE_PATH = "conversation_history.json";
    private static final int MAX_CONTEXT_TOKENS = 120000;
    private static final int DISCORD_MESSAGE_LIMIT = 2000;

    public DiscordListener(OpenRouterService openRouterService, ObjectMapper objectMapper) {
        this.openRouterService = openRouterService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadHistory() {
        File historyFile = new File(HISTORY_FILE_PATH);
        if (historyFile.exists()) {
            try {
                // Legge la mappa dal file JSON se esiste
                conversationHistories = objectMapper.readValue(historyFile, new TypeReference<ConcurrentHashMap<String, List<Map<String, String>>>>() {});
                System.out.println("Cronologia conversazioni caricata da " + HISTORY_FILE_PATH);
            } catch (IOException e) {
                System.err.println("Impossibile caricare la cronologia delle conversazioni: " + e.getMessage());
            }
        }
    }

    @PreDestroy
    public void saveHistory() {
        try {
            // Salva la mappa corrente nel file JSON
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(HISTORY_FILE_PATH), conversationHistories);
            System.out.println("Cronologia conversazioni salvata in " + HISTORY_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Impossibile salvare la cronologia delle conversazioni: " + e.getMessage());
        }
    }

    private int estimateTokens(String text) {
        return text.length() / 4;
    }

    private void sendLongMessage(MessageReceivedEvent event, String message) {
        if (message.length() <= DISCORD_MESSAGE_LIMIT) {
            event.getChannel().sendMessage(message).queue();
            return;
        }

        // Divide il messaggio in parti più piccole
        List<String> parts = new ArrayList<>();
        String remaining = message;

        while (remaining.length() > DISCORD_MESSAGE_LIMIT) {
            // Cerca un punto di interruzione naturale (spazio, punto, virgola) prima del limite
            int breakPoint = DISCORD_MESSAGE_LIMIT;
            for (int i = DISCORD_MESSAGE_LIMIT - 1; i > DISCORD_MESSAGE_LIMIT - 200 && i >= 0; i--) {
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

        // Invia ogni parte come messaggio separato
        for (String part : parts) {
            event.getChannel().sendMessage(part).queue();
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
                }
            }
        }

        if (isMentioned) {
            // Mostra l'indicatore "sta scrivendo..." nel canale
            event.getChannel().sendTyping().queue();

            String channelId = event.getChannel().getId();

            // Rimuove tutte le menzioni (sia utente che ruolo) dal messaggio
            String userMessageContent = event.getMessage().getContentRaw()
                    .replaceAll("<@!?\\d+>", "") // Rimuove menzioni utente
                    .replaceAll("<@&\\d+>", "")   // Rimuove menzioni ruolo
                    .trim();

            if (userMessageContent.isEmpty()) {
                return;
            }

            List<Map<String, String>> history = conversationHistories.computeIfAbsent(
                    channelId,
                    k -> new ArrayList<>()
            );

            history.add(Map.of("role", "user", "content", userMessageContent));

            int currentTokenCount = 0;
            for (Map<String, String> message : history) {
                currentTokenCount += estimateTokens(message.get("content"));
            }

            while (currentTokenCount > MAX_CONTEXT_TOKENS && !history.isEmpty()) {
                Map<String, String> removedMessage = history.removeFirst();
                currentTokenCount -= estimateTokens(removedMessage.get("content"));
            }

            openRouterService.getChatCompletion(history)
                    .subscribe(
                            botResponse -> {
                                sendLongMessage(event, botResponse);
                                history.add(Map.of("role", "assistant", "content", botResponse));
                            },
                            error -> {
                                System.err.println("Errore durante la chiamata a OpenRouter: " + error.getMessage());
                                event.getChannel().sendMessage("Oops! Qualcosa è andato storto.").queue();
                            }
                    );
        }
    }

}
