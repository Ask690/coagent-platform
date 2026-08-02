package com.coagent.controller;

import com.coagent.agent.AgentOrchestrator;
import com.coagent.agent.ChatEvent;
import com.coagent.dto.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/** 对话接口：POST /api/chat/stream 返回 SSE 流式事件 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    public ChatController(AgentOrchestrator orchestrator, ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody ChatRequest request) {
        return orchestrator.chat(request.sessionId(), request.message())
                .map(event -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(writeJson(event))
                        .build());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "app", "coagent-backend");
    }

    private String writeJson(ChatEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"data\":{\"message\":\"事件序列化失败\"}}";
        }
    }
}
