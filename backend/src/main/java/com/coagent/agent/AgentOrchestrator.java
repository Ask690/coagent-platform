package com.coagent.agent;

import com.coagent.domain.ChatMessage;
import com.coagent.domain.ChatSession;
import com.coagent.repository.ChatMessageRepository;
import com.coagent.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 多智能体编排器（Supervisor Pattern）。
 *
 * <p>每轮对话流程：
 * <pre>
 *  编排者(路由决策) -> 专精Agent(知识库/业务/工单/直接回复) -> 保存消息
 * </pre>
 * 各 Agent 拥有独立职责与提示词；工具调用由编排层驱动并全程可观测（SSE 事件）。
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final Router router;
    private final KnowledgeAgent knowledgeAgent;
    private final BusinessAgent businessAgent;
    private final TicketAgent ticketAgent;
    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public AgentOrchestrator(Router router,
                             KnowledgeAgent knowledgeAgent,
                             BusinessAgent businessAgent,
                             TicketAgent ticketAgent,
                             ChatClient chatClient,
                             ChatSessionRepository sessionRepository,
                             ChatMessageRepository messageRepository) {
        this.router = router;
        this.knowledgeAgent = knowledgeAgent;
        this.businessAgent = businessAgent;
        this.ticketAgent = ticketAgent;
        this.chatClient = chatClient;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * 处理一轮对话，返回 SSE 事件流。
     * 订阅时开始执行（controller 返回 Flux 后由 MVC 触发订阅）。
     */
    public Flux<ChatEvent> chat(String sessionId, String userMessage) {
        return Flux.defer(() -> {
            ChatSession session = ensureSession(sessionId, userMessage);
            String history = loadHistory(session.getSessionId());
            saveMessage(session.getSessionId(), ChatMessage.Role.USER, userMessage);

            // 编排者统一路由一次，供展示与分派复用
            RoutingDecision decision = router.route(userMessage);
            log.info("[编排者] session={} intent={} reason={}", session.getSessionId(),
                    decision.intent(), decision.reason());

            AtomicReference<StringBuilder> assistantAcc = new AtomicReference<>(new StringBuilder());

            return Flux.concat(
                            Flux.just(ChatEvent.session(session.getSessionId())),
                            supervisorEvents(decision),
                            dispatch(decision, userMessage, history),
                            Flux.just(ChatEvent.done(session.getSessionId()))
                    )
                    .doOnNext(ev -> {
                        if ("token".equals(ev.type()) && ev.data() != null) {
                            Object text = ev.data().get("text");
                            if (text != null) {
                                assistantAcc.get().append(text);
                            }
                        }
                    })
                    .doOnComplete(() ->
                            saveMessage(session.getSessionId(), ChatMessage.Role.ASSISTANT, assistantAcc.get().toString()))
                    .onErrorResume(e -> {
                        log.error("[编排器] 对话处理异常", e);
                        saveMessage(session.getSessionId(), ChatMessage.Role.ASSISTANT, assistantAcc.get().toString());
                        return Flux.just(ChatEvent.error(e.getMessage() == null ? "系统异常" : e.getMessage()));
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 编排者事件：开始 -> 路由决策 -> 结束 */
    private Flux<ChatEvent> supervisorEvents(RoutingDecision decision) {
        return Flux.just(
                ChatEvent.agentStart("编排者", "意图识别 / 任务调度"),
                ChatEvent.route(decision.intent(), decision.reason()),
                ChatEvent.agentEnd("编排者")
        );
    }

    /** 按路由结果分派给对应专精 Agent */
    private Flux<ChatEvent> dispatch(RoutingDecision decision, String userMessage, String history) {
        return switch (decision.intent()) {
            case BUSINESS -> wrap("业务查询Agent", "调用工具查询订单 / 物流",
                    businessAgent.handle(userMessage, history));
            case TICKET -> wrap("工单Agent", "自动创建工单并流转处理",
                    ticketAgent.handle(userMessage, history));
            case DIRECT -> wrap("客服Agent", "直接友好回复",
                    directAnswer(userMessage));
            case KNOWLEDGE -> wrap("知识库Agent", "RAG 检索知识库后作答",
                    knowledgeAgent.handle(userMessage, history));
        };
    }

    private Flux<ChatEvent> wrap(String name, String title, Flux<ChatEvent> inner) {
        return Flux.concat(
                Flux.just(ChatEvent.agentStart(name, title)),
                inner,
                Flux.just(ChatEvent.agentEnd(name))
        );
    }

    private Flux<ChatEvent> directAnswer(String userMessage) {
        return chatClient.prompt()
                .system("你是优购商城智能客服，简短友好地回应用户问候，并引导其咨询订单物流、退换货政策、工单投诉等问题。")
                .user(userMessage)
                .stream().content()
                .map(ChatEvent::token);
    }

    // ================= 会话与消息持久化 =================

    private ChatSession ensureSession(String sessionId, String userMessage) {
        String sid = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
        Optional<ChatSession> existing = sessionRepository.findBySessionId(sid);
        if (existing.isPresent()) {
            ChatSession s = existing.get();
            s.setUpdatedAt(LocalDateTime.now());
            return sessionRepository.save(s);
        }
        ChatSession session = new ChatSession();
        session.setSessionId(sid);
        String title = userMessage == null ? "新会话" : userMessage.replaceAll("\\s+", " ");
        session.setTitle(title.length() > 30 ? title.substring(0, 30) : title);
        return sessionRepository.save(session);
    }

    private void saveMessage(String sessionId, ChatMessage.Role role, String content) {
        if (content == null) {
            content = "";
        }
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        messageRepository.save(message);
    }

    /** 取最近 6 条历史（不含本轮用户消息），格式化为可读文本 */
    private String loadHistory(String sessionId) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (messages.isEmpty()) {
            return "";
        }
        return messages.stream()
                .skip(Math.max(0, messages.size() - 6L))
                .map(m -> (m.getRole() == ChatMessage.Role.USER ? "用户" : "客服") + "：" + m.getContent())
                .collect(Collectors.joining("\n"));
    }
}
