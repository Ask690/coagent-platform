package com.coagent.repository;

import com.coagent.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);

    /** 置顶优先，其次按更新时间倒序 */
    List<ChatSession> findAllByOrderByPinnedDescUpdatedAtDesc();
}
