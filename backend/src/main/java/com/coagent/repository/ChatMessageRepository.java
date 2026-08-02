package com.coagent.repository;

import com.coagent.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /** 删除某会话的所有消息（级联删除用）；需要显式事务，否则 em.remove 报 TransactionRequiredException */
    @Transactional
    void deleteBySessionId(String sessionId);
}
