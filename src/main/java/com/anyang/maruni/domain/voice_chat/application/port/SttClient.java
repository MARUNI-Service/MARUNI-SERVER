package com.anyang.maruni.domain.voice_chat.application.port;

import org.springframework.web.multipart.MultipartFile;

public interface SttClient {
    String transcribe(MultipartFile audioFile);
}
