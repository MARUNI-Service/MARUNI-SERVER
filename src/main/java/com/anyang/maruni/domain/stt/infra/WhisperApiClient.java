package com.anyang.maruni.domain.stt.infra;

import com.anyang.maruni.domain.stt.application.port.SttClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhisperApiClient implements SttClient {

    private final WebClient openAiWebClient;

    @Override
    public String transcribe(MultipartFile audioFile) {
        File tempFile = convertMultipartToFile(audioFile);

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile));
            body.add("model", "whisper-1");
            body.add("language", "ko");

            String response = openAiWebClient.post()
                    .uri("/audio/transcriptions")
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Whisper API response: {}", response);

            return response;

        } finally {
            tempFile.delete();
        }
    }

    private File convertMultipartToFile(MultipartFile file) {
        try {
            File convFile = File.createTempFile("audio", ".wav");
            try (FileOutputStream fos = new FileOutputStream(convFile)) {
                fos.write(file.getBytes());
            }
            return convFile;
        } catch (IOException e) {
            throw new RuntimeException("파일 변환 실패", e);
        }
    }
}
