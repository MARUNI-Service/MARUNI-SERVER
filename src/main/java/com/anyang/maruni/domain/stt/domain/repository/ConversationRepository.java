package com.anyang.maruni.domain.stt.domain.repository;

import com.anyang.maruni.domain.stt.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
