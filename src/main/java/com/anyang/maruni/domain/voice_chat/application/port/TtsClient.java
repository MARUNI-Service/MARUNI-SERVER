package com.anyang.maruni.domain.voice_chat.application.port;

public interface TtsClient {
    byte[] synthesizeSpeech(String text);
}
