package com.coagent.agent;

import com.coagent.rag.RetrievedChunk;
import com.coagent.rag.Retriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 知识库 Agent：RAG 检索 + 生成式回答。
 * 先调用 Retriever 检索相关知识块，再让大模型基于上下文生成回答。
 */
@Component
public class KnowledgeAgent {

    private static final String SYSTEM_PROMPT = """
            你是优购商城智能客服「知识库Agent」。
            请严格基于【知识库上下文】中提供的资料回答用户问题。
            资料中没有的内容，请如实说明"暂未收录"，不要编造。
            语气亲切专业，回答简洁、分点清晰。
            """;

    private final ChatClient chatClient;
    private final Retriever retriever;
    private final int topK;

    public KnowledgeAgent(ChatClient chatClient,
                          Retriever retriever,
                          @Value("${coagent.rag.top-k:3}") int topK) {
        this.chatClient = chatClient;
        this.retriever = retriever;
        this.topK = topK;
    }

    public Flux<ChatEvent> handle(String userMessage, String history) {
        List<RetrievedChunk> hits = retriever.retrieve(userMessage, topK);
        String context = hits.isEmpty()
                ? "（知识库暂无相关文档）"
                : hits.stream()
                    .map(h -> "「" + h.docName() + "」\n" + h.text())
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");

        String candidates = hits.stream()
                .map(h -> h.docName() + "：「" + truncate(h.text(), 60) + "」")
                .reduce((a, b) -> a + "；" + b)
                .orElse("无命中");

        Flux<ChatEvent> retrieval = Flux.just(
                ChatEvent.toolCall("知识库检索", Map.of("query", userMessage, "topK", topK, "hitCount", hits.size())),
                ChatEvent.toolResult("知识库检索", candidates)
        );

        Flux<ChatEvent> tokens = chatClient.prompt()
                .system(SYSTEM_PROMPT + historyBlock(history))
                .user(userMessage + "\n\n【知识库上下文】\n" + context + "\n【/知识库上下文】")
                .stream().content()
                .map(ChatEvent::token);

        return Flux.concat(retrieval, tokens);
    }

    private String historyBlock(String history) {
        return history == null || history.isBlank() ? "" : "\n\n【对话历史】\n" + history;
    }

    private String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
