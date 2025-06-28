package com.anyang.maruni.domain.voice_chat.presentation.controller;

import com.anyang.maruni.domain.voice_chat.domain.entity.Conversation;
import com.anyang.maruni.domain.voice_chat.domain.repository.ConversationRepository;
import com.anyang.maruni.domain.voice_chat.presentation.dto.response.ConversationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationRepository conversationRepository;

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getAllConversations() {
        List<Conversation> conversations = conversationRepository.findAll();
        List<ConversationResponse> responses = conversations.stream()
                .map(ConversationResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationResponse>> getConversationsByUserId(@PathVariable Long userId) {
        List<Conversation> conversations = conversationRepository.findByUser_UserId(userId);
        List<ConversationResponse> responses = conversations.stream()
                .map(ConversationResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
