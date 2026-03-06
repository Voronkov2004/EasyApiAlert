package io.github.voronkov.easyapialert;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class TelegramNotifier {

    private final RestClient rest;
    private final TelegramBotProperties props;

    public void send(String text) {
        String token = props.token();
        String chatId = props.chatId();
        if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) return;

        String url = "https://api.telegram.org/bot" + token + "/sendMessage";
        rest.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SendMessage(chatId, text))
                .retrieve()
                .toBodilessEntity();
    }

    record SendMessage(String chat_id, String text) {}
}
