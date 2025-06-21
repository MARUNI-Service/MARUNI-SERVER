package com.anyang.maruni.domain.stt.infra;

import com.anyang.maruni.domain.stt.application.port.SttClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhisperApiClient implements SttClient {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    @Override
    public String transcribe(MultipartFile audioFile) {

        File tempFile = convertMultipartToFile(audioFile);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openAiApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("model", "whisper-1");
            body.add("language", "ko");
            body.add("file", new FileSystemResource(tempFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<WhisperResponse> response = restTemplate.exchange(
                    WHISPER_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    WhisperResponse.class
            );

            return response.getBody() != null ? response.getBody().text() : null;

        } finally {
            tempFile.delete();
        }
    }

    private File convertMultipartToFile(MultipartFile file) {
        try {
            File convFile = File.createTempFile("audio", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(convFile)) {
                fos.write(file.getBytes());
            }
            return convFile;
        } catch (IOException e) {
            throw new RuntimeException("파일 변환 실패", e);
        }
    }

    public static record WhisperResponse(String text) {}
}
