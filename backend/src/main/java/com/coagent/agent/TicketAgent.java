package com.coagent.agent;

import com.coagent.agent.tools.TicketTool;
import com.coagent.domain.Ticket;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工单 Agent：识别投诉/售后/保修意图 -> 自动创建工单 -> 基于工单号流式安抚回复。
 */
@Component
public class TicketAgent {

    private static final Pattern ORDER_NO = Pattern.compile("(JD\\d{6,})");

    private static final String SYSTEM_PROMPT = """
            你是优购商城智能客服「工单Agent」。
            用户的问题已自动创建工单。请告知用户工单编号、类型，并安抚说明客服团队会跟进处理。
            语气友善、有同理心。
            """;

    private final ChatClient chatClient;
    private final TicketTool ticketTool;

    public TicketAgent(ChatClient chatClient, TicketTool ticketTool) {
        this.chatClient = chatClient;
        this.ticketTool = ticketTool;
    }

    public Flux<ChatEvent> handle(String userMessage, String history) {
        return Flux.defer(() -> {
            Ticket.Type type = detectType(userMessage);
            String orderNo = extractOrderNo(userMessage);
            String ticketNo = ticketTool.createTicket(type.name(), orderNo, userMessage);

            Flux<ChatEvent> tool = Flux.just(
                    ChatEvent.toolCall("创建工单", Map.of("type", type.name(),
                            "orderNo", orderNo == null ? "" : orderNo, "title", truncate(userMessage, 40))),
                    ChatEvent.toolResult("创建工单", "工单 " + ticketNo + " 创建成功（类型：" + type + "，状态：OPEN）")
            );

            Flux<ChatEvent> tokens = chatClient.prompt()
                    .system(SYSTEM_PROMPT + historyBlock(history))
                    .user(userMessage + "\n\n【工具结果】\n工单编号：" + ticketNo + "，类型：" + type + "\n【/工具结果】")
                    .stream().content()
                    .map(ChatEvent::token);

            return Flux.concat(tool, tokens);
        });
    }

    private Ticket.Type detectType(String msg) {
        if (msg.contains("投诉") || msg.contains("举报") || msg.contains("不满")) {
            return Ticket.Type.COMPLAINT;
        }
        if (msg.contains("保修")) {
            return Ticket.Type.WARRANTY;
        }
        if (msg.contains("退货") || msg.contains("退款") || msg.contains("换货") || msg.contains("售后")) {
            return Ticket.Type.AFTER_SALES;
        }
        return Ticket.Type.CONSULT;
    }

    private String extractOrderNo(String msg) {
        Matcher m = ORDER_NO.matcher(msg);
        return m.find() ? m.group(1) : null;
    }

    private String historyBlock(String history) {
        return history == null || history.isBlank() ? "" : "\n\n【对话历史】\n" + history;
    }

    private String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
