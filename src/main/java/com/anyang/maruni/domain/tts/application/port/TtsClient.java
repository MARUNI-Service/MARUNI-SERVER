package com.anyang.maruni.domain.tts.application.port;

public interface TtsClient {
    byte[] synthesizeSpeech(String text);
}
