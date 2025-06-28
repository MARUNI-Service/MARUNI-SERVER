package com.anyang.maruni.domain.voice_chat.application.port;

public interface LlmClient {
    String chat(String prompt);
}
