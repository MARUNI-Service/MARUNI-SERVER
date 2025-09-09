package com.anyang.maruni.domain.voice_chat.application.service;

import com.anyang.maruni.domain.voice_chat.domain.entity.Conversation;
import com.anyang.maruni.domain.voice_chat.domain.repository.ConversationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public Conversation save(String sttText, String response) {
        Conversation conversation = Conversation.builder()
                .sttText(sttText)
                .gptResponse(response)
                .build();
        return conversationRepository.save(conversation);
    }
}

