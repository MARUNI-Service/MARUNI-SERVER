package com.anyang.maruni.domain.voice_chat.domain.repository;

import com.anyang.maruni.domain.voice_chat.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUser_UserId(Long userId);
}
