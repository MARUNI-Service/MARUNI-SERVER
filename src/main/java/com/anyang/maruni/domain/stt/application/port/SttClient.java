package com.anyang.maruni.domain.stt.application.port;

import org.springframework.web.multipart.MultipartFile;

public interface SttClient {
    String transcribe(MultipartFile audioFile);
}
