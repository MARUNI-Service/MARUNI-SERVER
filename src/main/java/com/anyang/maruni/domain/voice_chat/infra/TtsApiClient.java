package com.anyang.maruni.domain.voice_chat.infra;

import com.anyang.maruni.domain.voice_chat.application.port.TtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TtsApiClient implements TtsClient {

    private final WebClient openAiWebClient;

    @Override
    public byte[] synthesizeSpeech(String text) {

        return openAiWebClient.post()
                .uri("/audio/speech")
                .bodyValue(Map.of(
                        "model", "tts-1",
                        "input", text,
                        "voice", "nova",
                        "speed", 1.0
                ))
                .retrieve()
                .bodyToMono(byte[].class)  // binary response
                .block();
    }
}
