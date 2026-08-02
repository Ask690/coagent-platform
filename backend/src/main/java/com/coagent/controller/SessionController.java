package com.coagent.controller;

import com.coagent.domain.ChatMessage;
import com.coagent.domain.ChatSession;
import com.coagent.repository.ChatMessageRepository;
import com.coagent.repository.ChatSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        return sessionRepository.findAllByOrderByPinnedDescUpdatedAtDesc();
    }

    @GetMapping("/{sessionId}/messages")
    public List<ChatMessage> messages(@PathVariable String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /** 删除会话：同时删除该会话下的所有消息（同一事务，保证原子性） */
    @Transactional
    @DeleteMapping("/{sessionId}")
    public Map<String, Object> delete(@PathVariable String sessionId) {
        boolean existed = sessionRepository.findBySessionId(sessionId).isPresent();
        if (existed) {
            messageRepository.deleteBySessionId(sessionId);
            sessionRepository.findBySessionId(sessionId).ifPresent(sessionRepository::delete);
        }
        return Map.of("deleted", existed, "sessionId", sessionId);
    }

    /** 置顶 / 取消置顶会话 */
    @PutMapping("/{sessionId}/pin")
    public Map<String, Object> pin(@PathVariable String sessionId,
                                   @RequestBody(required = false) Map<String, Boolean> body) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));
        boolean pinned = body != null && Boolean.TRUE.equals(body.get("pinned"));
        session.setPinned(pinned);
        sessionRepository.save(session);
        return Map.of("sessionId", sessionId, "pinned", pinned);
    }
}
