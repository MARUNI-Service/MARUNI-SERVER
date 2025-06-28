package com.anyang.maruni.domain.voice_chat.domain.repository;

import com.anyang.maruni.domain.voice_chat.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
