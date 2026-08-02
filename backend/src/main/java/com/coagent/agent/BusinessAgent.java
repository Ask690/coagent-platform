package com.coagent.agent;

import com.coagent.agent.tools.OrderTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务查询 Agent：识别订单号 -> 调用订单工具 -> 基于工具结果流式生成回复。
 * 工具调用由编排层驱动，前端可见完整的「调用 -> 结果」过程。
 */
@Component
public class BusinessAgent {

    private static final Pattern ORDER_NO = Pattern.compile("(JD\\d{6,})");

    private static final String SYSTEM_PROMPT = """
            你是优购商城智能客服「业务查询Agent」。
            根据【工具结果】中的订单数据，用亲切专业的口吻向用户说明订单状态与物流信息。
            若工具提示未找到订单，请引导用户核对订单号（格式如 JD2025001）。
            """;

    private final ChatClient chatClient;
    private final OrderTool orderTool;

    public BusinessAgent(ChatClient chatClient, OrderTool orderTool) {
        this.chatClient = chatClient;
        this.orderTool = orderTool;
    }

    public Flux<ChatEvent> handle(String userMessage, String history) {
        Matcher matcher = ORDER_NO.matcher(userMessage);
        if (!matcher.find()) {
            // 未识别到订单号：让模型引导用户提供
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT + historyBlock(history))
                    .user(userMessage)
                    .stream().content()
                    .map(ChatEvent::token);
        }

        String orderNo = matcher.group(1);
        return Flux.defer(() -> {
            String result = orderTool.queryOrder(orderNo);
            Flux<ChatEvent> tool = Flux.just(
                    ChatEvent.toolCall("订单查询", Map.of("orderNo", orderNo)),
                    ChatEvent.toolResult("订单查询", result)
            );
            Flux<ChatEvent> tokens = chatClient.prompt()
                    .system(SYSTEM_PROMPT + historyBlock(history))
                    .user(userMessage + "\n\n【工具结果】\n" + result + "\n【/工具结果】")
                    .stream().content()
                    .map(ChatEvent::token);
            return Flux.concat(tool, tokens);
        });
    }

    private String historyBlock(String history) {
        return history == null || history.isBlank() ? "" : "\n\n【对话历史】\n" + history;
    }
}
