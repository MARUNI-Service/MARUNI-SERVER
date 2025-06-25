package com.anyang.maruni.domain.llm.application.port;

public interface LlmClient {
    String chat(String prompt);
}
