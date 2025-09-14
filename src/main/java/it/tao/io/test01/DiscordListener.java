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
                        // Azione da eseguire in caso di successo
                        botResponse -> event.getChannel().sendMessage(botResponse).queue(),
                        // Azione da eseguire in caso di errore
                        error -> {
                            System.err.println("Errore durante la chiamata a OpenRouter: " + error.getMessage());
                            event.getChannel().sendMessage("Oops! Qualcosa è andato storto.").queue();
                        }
                );
    }
}