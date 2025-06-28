package com.anyang.maruni.domain.voice_chat.application.service;

import com.anyang.maruni.domain.voice_chat.application.port.LlmClient;
import com.anyang.maruni.domain.voice_chat.application.port.SttClient;
import com.anyang.maruni.domain.voice_chat.domain.entity.Conversation;
import com.anyang.maruni.domain.voice_chat.domain.repository.ConversationRepository;
import com.anyang.maruni.domain.voice_chat.application.port.TtsClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class SttService {

    private final SttClient sttClient;
    private final LlmClient llmClient;
    private final TtsClient ttsClient;
    private final ConversationRepository conversationRepository;

    public Conversation processAudio(MultipartFile audioFile) {

        // 1️ STT 변환
        String sttText = sttClient.transcribe(audioFile);

        // 2️ LLM 응답 생성
        String llmResponse = llmClient.chat(sttText);

        // 2 Conversation 저장
        Conversation conversation = Conversation.builder()
                .sttText(sttText)
                .gptResponse(llmResponse)
                .build();

        return conversationRepository.save(conversation);
    }

    // NEW: STT → GPT → TTS → mp3 byte[] 리턴
    public byte[] processAudioAndSynthesize(MultipartFile audioFile) {

        // 1️ STT 변환
        String sttText = sttClient.transcribe(audioFile);

        // 2️ LLM 응답 생성
        String llmResponse = llmClient.chat(sttText);

        // 3️ TTS 음성 합성
        return ttsClient.synthesizeSpeech(llmResponse);
    }
}
