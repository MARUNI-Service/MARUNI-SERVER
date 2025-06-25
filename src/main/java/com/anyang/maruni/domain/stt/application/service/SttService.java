package com.anyang.maruni.domain.stt.application.service;

import com.anyang.maruni.domain.llm.application.port.LlmClient;
import com.anyang.maruni.domain.stt.application.port.SttClient;
import com.anyang.maruni.domain.stt.domain.entity.Conversation;
import com.anyang.maruni.domain.stt.domain.repository.ConversationRepository;
import com.anyang.maruni.domain.tts.application.port.TtsClient;
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

        // TODO: audio 저장 (S3 or local) → 지금은 임시로 null
        String audioUrl = "TODO: audio url";

        // 2 Conversation 저장
        Conversation conversation = Conversation.builder()
                .originalAudioUrl(audioUrl)
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
