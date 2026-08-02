package com.coagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 大模型结构化路由：让 DeepSeek 判断用户意图并输出 JSON。
 * 输出不合法时降级为默认知识库路由，保证系统稳定。
 */
@Component
@ConditionalOnProperty(name = "coagent.ai.mock", havingValue = "false")
public class LlmRouter implements Router {

    private static final Logger log = LoggerFactory.getLogger(LlmRouter.class);

    private static final String SYSTEM_PROMPT = """
            你是智能客服系统的调度器（Supervisor Agent）。
            根据用户消息判断意图，只输出一个 JSON 对象，不要输出任何其他内容，格式：
            {"intent":"KNOWLEDGE","reason":"一句话原因"}
            可选意图：
            KNOWLEDGE=知识/政策类问题（退换货、发票、保修、会员等）
            BUSINESS=查询订单/物流/配送等业务数据
            TICKET=投诉、不满或需人工介入（应创建工单）
            DIRECT=寒暄问候或闲聊，直接友好回复即可
            CHAIN=同时涉及"具体订单数据"与"售后政策/流程"的组合问题（如"我这个订单能退吗"），需先查订单再结合政策综合答复
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public LlmRouter(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public RoutingDecision route(String userMessage) {
        try {
            String content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();
            String json = stripCodeFence(content);
            JsonNode node = objectMapper.readTree(json);
            Intent intent = parseIntent(node.path("intent").asText("KNOWLEDGE"));
            String reason = node.path("reason").asText("");
            log.info("[编排者] 路由决策 intent={}, reason={}", intent, reason);
            return RoutingDecision.of(intent, reason);
        } catch (Exception e) {
            log.warn("[编排者] LLM 路由失败，降级知识库: {}", e.getMessage());
            return RoutingDecision.of(Intent.KNOWLEDGE, "LLM 路由异常，降级知识库兜底");
        }
    }

    private Intent parseIntent(String s) {
        try {
            return Intent.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return Intent.KNOWLEDGE;
        }
    }

    private String stripCodeFence(String content) {
        if (content == null) {
            return "";
        }
        String s = content.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        int idx = s.indexOf("{");
        if (idx > 0) {
            s = s.substring(idx);
        }
        return s;
    }
}
