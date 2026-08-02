package com.coagent.repository;

import com.coagent.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /** 删除某会话的所有消息（级联删除用） */
    void deleteBySessionId(String sessionId);
}
