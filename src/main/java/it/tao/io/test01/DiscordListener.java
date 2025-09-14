package it.tao.io.test01;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class DiscordListener extends ListenerAdapter {

    private final OpenRouterService openRouterService;

    public DiscordListener(OpenRouterService openRouterService) {
        this.openRouterService = openRouterService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignora i messaggi dei bot
        if (event.getAuthor().isBot()) {
            return;
        }

        String userMessage = event.getMessage().getContentRaw();

        openRouterService.getChatCompletion(userMessage)
                .subscribe(
                        botResponse -> {
                            if (botResponse == null || botResponse.isEmpty()) {
                                return;
                            }

                            final int LIMIT = 2000;
                            int length = botResponse.length();

                            if (length <= LIMIT) {
                                event.getChannel().sendMessage(botResponse).queue();
                            } else {
                                // Spezza in chunk da massimo LIMIT caratteri e invia ciascuno
                                for (int start = 0; start < length; start += LIMIT) {
                                    int end = Math.min(length, start + LIMIT);
                                    String part = botResponse.substring(start, end);
                                    event.getChannel().sendMessage(part).queue();
                                }
                            }
                        },
                        error -> {
                            System.err.println("Errore durante la chiamata a OpenRouter: " + error.getMessage());
                            event.getChannel().sendMessage("Oops! Qualcosa è andato storto.").queue();
                        }
                );
    }
}
