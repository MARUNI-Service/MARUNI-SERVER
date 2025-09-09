package com.anyang.maruni.domain.voice_chat.application.service;

import com.anyang.maruni.domain.voice_chat.application.port.LlmClient;
import com.anyang.maruni.domain.voice_chat.application.port.SttClient;
import com.anyang.maruni.domain.voice_chat.application.port.TtsClient;
import com.anyang.maruni.domain.voice_chat.domain.entity.Conversation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AudioProcessingService {

    private final SttClient sttClient;
    private final LlmClient llmClient;
    private final TtsClient ttsClient;
    private final ConversationService conversationService;

    public Conversation processAudio(MultipartFile audioFile) {
        String sttText = sttClient.transcribe(audioFile);
        String llmResponse = llmClient.chat(sttText);
        return conversationService.save(sttText, llmResponse);
    }

    public byte[] processAudioAndSynthesize(MultipartFile audioFile) {
        String sttText = sttClient.transcribe(audioFile);
        String llmResponse = llmClient.chat(sttText);
        conversationService.save(sttText, llmResponse);
        return ttsClient.synthesizeSpeech(llmResponse);
    }
}
