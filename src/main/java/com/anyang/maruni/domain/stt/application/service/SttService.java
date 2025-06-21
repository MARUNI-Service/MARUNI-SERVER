package com.anyang.maruni.domain.stt.application.service;

import com.anyang.maruni.domain.stt.application.port.SttClient;
import com.anyang.maruni.domain.stt.domain.entity.Conversation;
import com.anyang.maruni.domain.stt.domain.repository.ConversationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class SttService {

    private final SttClient sttClient;
    private final ConversationRepository conversationRepository;

    public Conversation processAudio(MultipartFile audioFile) {

        // 1️ STT 변환
        String sttText = sttClient.transcribe(audioFile);

        // TODO: ChatGPT 호출 후 gptResponse 받아오기 → 지금은 일단 null 넣기
        String gptResponse = "TODO: GPT response";

        // TODO: audio 저장 (S3 or local) → 지금은 임시로 null
        String audioUrl = "TODO: audio url";

        // 2 Conversation 저장
        Conversation conversation = Conversation.builder()
                .originalAudioUrl(audioUrl)
                .sttText(sttText)
                .gptResponse(gptResponse)
                .build();

        return conversationRepository.save(conversation);
    }
}
