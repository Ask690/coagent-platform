package com.coagent.controller;

import com.coagent.domain.ChatMessage;
import com.coagent.domain.ChatSession;
import com.coagent.repository.ChatMessageRepository;
import com.coagent.repository.ChatSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 会话管理：创建 / 列表 / 历史消息 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public SessionController(ChatSessionRepository sessionRepository,
                             ChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    /** 新建会话，返回会话号 */
    @PostMapping
    public Map<String, String> create() {
        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setTitle("新会话");
        sessionRepository.save(session);
        return Map.of("sessionId", session.getSessionId());
    }

    @GetMapping
    public List<ChatSession> list() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc();
    }

    @GetMapping("/{sessionId}/messages")
    public List<ChatMessage> messages(@PathVariable String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
