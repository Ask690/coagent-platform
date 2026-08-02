package com.coagent.agent;

import com.coagent.agent.tools.OrderTool;
import com.coagent.rag.RetrievedChunk;
import com.coagent.rag.Retriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多 Agent 链路编排（Multi-Step Chain）。
 *
 * <p>当单个专精 Agent 不足以回答（例如「我的订单能不能退」——既需要订单数据，
 * 又需要退货政策）时，编排器走多步链路：
 * <pre>
 *   Step1 业务查询Agent：查订单当前状态         (工具调用)
 *   Step2 知识库Agent  ：检索退换货/退款政策     (RAG)
 *   Step3 汇总Agent    ：综合订单状态+政策给出结论 (生成)
 * </pre>
 * 每步的输入输出结构化传递，全程通过 SSE 事件可观测。
 */
@Service
public class AgentChainService {

    private static final Pattern ORDER_NO = Pattern.compile("(JD\\d{6,})");

    private static final String SUMMARY_SYSTEM_PROMPT = """
            你是优购商城智能客服「汇总Agent」。
            请根据【订单数据】与【政策资料】，给用户一个综合结论，分三点：
            1. 订单当前状态
            2. 结合政策，说明该订单是否满足退换货/退款条件
            3. 给出下一步具体操作建议
            语气亲切专业，结论明确，不要模棱两可。
            """;

    private final ChatClient chatClient;
    private final OrderTool orderTool;
    private final Retriever retriever;
    private final int topK;

    public AgentChainService(ChatClient chatClient,
                             OrderTool orderTool,
                             Retriever retriever,
                             @Value("${coagent.rag.top-k:3}") int topK) {
        this.chatClient = chatClient;
        this.orderTool = orderTool;
        this.retriever = retriever;
        this.topK = topK;
    }

    public Flux<ChatEvent> runChain(String userMessage, String history) {
        return Flux.defer(() -> {
            // ===== Step1：业务查询 Agent（同步取数，运行在 boundedElastic） =====
            String orderNo = extractOrderNo(userMessage);
            String orderData = orderNo == null
                    ? "未在消息中识别到订单号，无法查询具体订单状态。"
                    : orderTool.queryOrder(orderNo);

            Flux<ChatEvent> step1 = Flux.concat(
                    Flux.just(ChatEvent.agentStart("业务查询Agent", "Step1 查询订单当前状态")),
                    Flux.just(ChatEvent.toolCall("订单查询", Map.of("orderNo", orderNo == null ? "" : orderNo))),
                    Flux.just(ChatEvent.toolResult("订单查询", orderData)),
                    Flux.just(ChatEvent.agentEnd("业务查询Agent"))
            );

            // ===== Step2：知识库 Agent（RAG 检索政策） =====
            List<RetrievedChunk> hits = retriever.retrieve(userMessage, topK);
            String policy = hits.isEmpty()
                    ? "（知识库暂未检索到相关退换货/退款政策）"
                    : hits.stream().map(h -> "「" + h.docName() + "」\n" + h.text())
                        .reduce((a, b) -> a + "\n\n" + b).orElse("");

            String candidates = hits.stream()
                    .map(h -> h.docName() + "：「" + truncate(h.text(), 50) + "」")
                    .reduce((a, b) -> a + "；" + b)
                    .orElse("无命中");

            Flux<ChatEvent> step2 = Flux.concat(
                    Flux.just(ChatEvent.agentStart("知识库Agent", "Step2 检索退换货 / 退款政策")),
                    Flux.just(ChatEvent.toolCall("知识库检索", Map.of("query", userMessage, "topK", topK, "hitCount", hits.size()))),
                    Flux.just(ChatEvent.toolResult("知识库检索", candidates)),
                    Flux.just(ChatEvent.agentEnd("知识库Agent"))
            );

            // ===== Step3：汇总 Agent（综合两路结果生成结论） =====
            String summaryInput = "【用户问题】\n" + userMessage
                    + "\n\n【订单数据】\n" + orderData + "\n【/订单数据】"
                    + "\n\n【政策资料】\n" + policy + "\n【/政策资料】"
                    + (history == null || history.isBlank() ? "" : "\n\n【对话历史】\n" + history);

            Flux<ChatEvent> step3 = Flux.concat(
                    Flux.just(ChatEvent.agentStart("汇总Agent", "Step3 综合分析订单 + 政策，给出结论")),
                    chatClient.prompt()
                            .system(SUMMARY_SYSTEM_PROMPT)
                            .user(summaryInput)
                            .stream().content()
                            .map(ChatEvent::token),
                    Flux.just(ChatEvent.agentEnd("汇总Agent"))
            );

            return Flux.concat(step1, step2, step3);
        });
    }

    private String extractOrderNo(String msg) {
        Matcher m = ORDER_NO.matcher(msg);
        return m.find() ? m.group(1) : null;
    }

    private String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
